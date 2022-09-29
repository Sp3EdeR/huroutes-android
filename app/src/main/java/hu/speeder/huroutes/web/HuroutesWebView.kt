package hu.speeder.huroutes.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.webkit.DownloadListener
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import hu.speeder.huroutes.BuildConfig
import hu.speeder.huroutes.MainActivity
import hu.speeder.huroutes.web.downloaders.DownloaderPermissionTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The huroutes website root URI
 */
const val SITE_URI = "https://sp3eder.github.io/huroutes/"

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
    val startUri: Uri = Uri.parse(SITE_URI)

    init {
        webViewClient = client
        webChromeClient = HuroutesWebChromeClient(context)
        settings.apply {
            domStorageEnabled = true // localStorage
            displayZoomControls = false // No zoom controls (only pinch)
            javaScriptEnabled = true // Javascript
            javaScriptCanOpenWindowsAutomatically = true // window.open
            if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
                forceDark = WebSettings.FORCE_DARK_OFF // Day mode always
            }
            // Customized user-agent string
            userAgentString = userAgentString.replace(
                " Mobile", " huroutes/${BuildConfig.VERSION_NAME} Mobile")
            setGeolocationEnabled(true)
        }
        client.setHandleUriCallback { uri -> handleUri(uri) }
        setDownloadListener(HuroutesDownloadListener(context))
        initDarkMode()
    }

    @Suppress("DEPRECATION")
    fun initDarkMode() {
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

    /**
     * When called with a coroutine scope, it adds the ability to automatically refresh the page on
     * a load error.
     */
    fun setCoroutineScope(scope: LifecycleCoroutineScope) {
        client.setErrorCallback {
            scope.launch {
                delay(5000)
                if (client.hasError) {
                    reload()
                }
            }
        }
    }

    /**
     * Opens some URIs with external viewers, instead of this WebView.
     */
    private fun handleUri(uri: Uri): Boolean {
        if (uri.host == startUri.host && uri.path?.startsWith(startUri.path!!) == true) {
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