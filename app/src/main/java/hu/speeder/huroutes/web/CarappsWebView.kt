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
 * A customized WebView that contains car apps specializations.
 */
@SuppressLint("SetJavaScriptEnabled")
open class CarappsWebView @JvmOverloads constructor(
    val defaultUri: Uri,
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
): WebView(context, attrs, defStyleAttr) {

    class UninitializedException: Exception("HuroutesWebView requires setCoroutineScope to be called before loadUrl")

    private val _client = CarappsWebViewClient()
    open val client get() = _client

    open val startUri: Uri get() {
        return defaultUri
    }

    class Scrollable { var x = true; var y = true }
    val scrollable = Scrollable()

    init {
        webViewClient = _client
        _client.setHandleUriCallback { uri -> handleUri(uri) }
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
        addJavascriptInterface(JavaScriptSharedInterface(context), "android")
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
     * Tests the given Uri, and returns the Uri to be loaded if so. Otherwise returns null.
     */
    open fun validateUri(uri: Uri?): Uri? {
        if (defaultUri.host == uri?.host && uri?.path?.startsWith(defaultUri.path!!) == true) {
            return uri
        }
        return null
    }

    /**
     * Opens some URIs with external viewers, instead of this WebView.
     */
    protected fun handleUri(uri: Uri): Boolean {
        if (validateUri(uri) != null) {
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

    companion object {
        private const val LOG_TAG = "CarappsWebView"
    }
}