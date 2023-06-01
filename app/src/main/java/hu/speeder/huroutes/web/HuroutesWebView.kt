package hu.speeder.huroutes.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.webkit.DownloadListener
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import hu.speeder.huroutes.BuildConfig
import hu.speeder.huroutes.MainActivity
import hu.speeder.huroutes.web.downloaders.DownloaderPermissionTask
import hu.speeder.huroutes.web.interop.JavaScriptSharedInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The huroutes website root URI
 */
const val SITE_LANGUAGE = "hu"
const val SITE_URI = "https://sp3eder.github.io/huroutes"
const val SITE_TRANSLATE_URI = "https://sp3eder-github-io.translate.goog/huroutes"
const val GOOGLE_TRANSLATE_LINK_BASE = "https://translate.google.com/translate"
const val SITE_TRANSLATE_TEMPLATE = "${GOOGLE_TRANSLATE_LINK_BASE}?sl=${SITE_LANGUAGE}&tl=%s&u=${SITE_URI}"

/**
 * A customized WebView that contains huroutes specializations.
 */
@SuppressLint("SetJavaScriptEnabled")
class HuroutesWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
): WebView(context, attrs, defStyleAttr) {

    class UninitializedException: Exception("HuroutesWebView requires setCoroutineScope to be called before loadUrl")

    val client = HuroutesWebViewClient()
    private var _startLanguage: String? = null
    val startUri: Uri get() {
        if (_startLanguage == null) {
            _startLanguage = (context as MainActivity).preferencesStore.languageIso2 ?:
                SITE_LANGUAGE
        }
        return Uri.parse(if (_startLanguage!! == SITE_LANGUAGE) SITE_URI else
            SITE_TRANSLATE_TEMPLATE.format(_startLanguage))
    }

    class Scrollable { var x = true; var y = true }
    val scrollable = Scrollable()

    init {
        webViewClient = client
        webChromeClient = HuroutesWebChromeClient(context)
        settings.apply {
            domStorageEnabled = true // localStorage
            displayZoomControls = false // No zoom controls (only pinch)
            javaScriptEnabled = true // Javascript
            javaScriptCanOpenWindowsAutomatically = true // window.open
            // Customized user-agent string
            userAgentString = userAgentString.replace(
                " Mobile", " huroutes/${BuildConfig.VERSION_NAME} Mobile")
            setGeolocationEnabled(true)
        }
        client.setHandleUriCallback { uri -> handleUri(uri) }
        setDownloadListener(HuroutesDownloadListener(context))
        updateOfflineMode()
        initDarkMode()
        addShareInterface()

        // Setup debugging; See https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews for reference
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            setWebContentsDebuggingEnabled(true)
        }
    }

    @Suppress("DEPRECATION")
    private fun initDarkMode() {
        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            settings.forceDark = WebSettings.FORCE_DARK_OFF // Day mode always
        }

        val nightModeFlag = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightModeFlag == Configuration.UI_MODE_NIGHT_YES) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(
                    settings,
                    WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    fun isNetworkAvailable(): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }

    /**
     * @return Whether the webView is in offline mode
     */
    fun updateOfflineMode() {
        if (isNetworkAvailable()) {
            settings.cacheMode = WebSettings.LOAD_DEFAULT
        } else {
            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
    }

	/**
	 * Adds a Javascript->Kotlin interface for exporting android features.
	 */
    private fun addShareInterface() {
        addJavascriptInterface(JavaScriptSharedInterface(context), "android");
    }

    /**
     * When called with a coroutine scope, it adds the ability to automatically refresh the page on
     * a load error.
     */
    fun setCoroutineScope(scope: LifecycleCoroutineScope) {
        client.setErrorCallback {
            scope.launch {
                if (client.hasError) {
                    if (settings.cacheMode == WebSettings.LOAD_DEFAULT) {
                        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        reload()
                    } else {
                        delay(5000)
                        reload()
                    }
                }
            }
        }
    }

    /**
     * Tests whether the given URI is a valid huroutes URL.
     */
    fun validateUri(uri: Uri): Boolean {
        val uris = arrayOf(SITE_URI, SITE_TRANSLATE_URI, GOOGLE_TRANSLATE_LINK_BASE)
        for (u in uris) {
            val testUri = Uri.parse(u)
            if (testUri.host == uri.host && uri.path?.startsWith(testUri.path!!) == true) {
                return true;
            }
        }
        return false;
    }

    /**
     * Opens some URIs with external viewers, instead of this WebView.
     */
    private fun handleUri(uri: Uri): Boolean {
        if (validateUri(uri)) {
            Log.d(LOG_TAG, "Loading URL: $uri")
            return false
        }

        Log.d(LOG_TAG, "Viewing URL with intent: $uri")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))

        return true
    }

    override fun loadUrl(url: String) {
        if (!client.hasErrorCallback)
            throw UninitializedException()

        super.loadUrl(url)
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
        if (!client.hasErrorCallback)
            throw UninitializedException()

        super.loadUrl(url, additionalHttpHeaders)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val ret = super.onTouchEvent(event)

        if (event?.actionMasked == MotionEvent.ACTION_DOWN) {
            scrollable.x = true; scrollable.y = true
        }

        return ret
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)

        scrollable.x = !clampedX
        scrollable.y = !clampedY
    }

    /**
     * A download listener for the HuroutesWebView.
     * This implements custom file downloading for huroutes.
     */
    class HuroutesDownloadListener(private val context: Context): DownloadListener {
        override fun onDownloadStart(
            uri: String?, userAgent: String?, contentDisposition: String?,
            mimeType: String?, contentLength: Long,
        ) {
            if (uri == null) {
                return
            }

            try {
                val task = DownloaderPermissionTask(
                    context, uri, userAgent, contentDisposition, mimeType, contentLength
                )
                (context as MainActivity).runTaskWithPermission(task)
            }
            catch (e: Exception)
            {
                Log.w(LOG_TAG, "Failed to download uri: $uri")
            }
        }
    }

    companion object {
        private const val LOG_TAG = "webViewClient"
    }
}