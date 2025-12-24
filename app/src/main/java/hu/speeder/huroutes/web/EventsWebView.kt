package hu.speeder.huroutes.web

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import androidx.core.net.toUri
import hu.speeder.huroutes.web.HuroutesWebView.HuroutesDownloadListener

/**
 * A customized WebView that contains specializations for the events page.
 */
@SuppressLint("SetJavaScriptEnabled")
class EventsWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
): CarappsWebView(
    "https://sp3eder.github.io/autosesemenyek".toUri(),
    context, attrs, defStyleAttr) {

    private val _client = EventsWebViewClient()
    override val client get() = _client

    init {
        webViewClient = _client
        _client.setHandleUriCallback { uri -> handleUri(uri) }
    }

}