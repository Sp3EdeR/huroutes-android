package hu.speeder.huroutes.web

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val SITE_URI = "https://sp3eder.github.io/huroutes/"

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
        settings.apply {
            domStorageEnabled = true // localStorage
            displayZoomControls = false // No zoom controls (only pinch)
            javaScriptEnabled = true // Javascript
            javaScriptCanOpenWindowsAutomatically = true // window.open
            if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
                forceDark = WebSettings.FORCE_DARK_OFF // Day mode always
            }
        }
    }

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

}