package hu.speeder.huroutes.web

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.webkit.DownloadListener
import android.webkit.WebView
import hu.speeder.huroutes.MainActivity
import androidx.core.net.toUri
import hu.speeder.huroutes.web.downloaders.DownloaderPermissionTask

/**
 * The huroutes website root URI
 */
private const val SITE_LANGUAGE = "hu"
private const val GOOGLE_TRANSLATE_LINK_BASE = "https://translate.google.com/translate"
private const val SITE_TRANSLATE_TEMPLATE = "${GOOGLE_TRANSLATE_LINK_BASE}?sl=${SITE_LANGUAGE}&tl=%s&u=%s"
private const val SITE_TRANSLATE_URI_TEMPLATE = "https://sp3eder-github-io.translate.goog%s"

/**
 * A customized WebView that contains huroutes specializations.
 */
@SuppressLint("SetJavaScriptEnabled")
class HuroutesWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
): CarappsWebView(
    "https://sp3eder.github.io/huroutes".toUri(),
    context, attrs, defStyleAttr) {

    private val _client = HuroutesWebViewClient()
    override val client get() = _client

    init {
        webChromeClient = GeolocationPermissionWebChromeClient(context)
        webViewClient = _client
        _client.setHandleUriCallback { uri -> handleUri(uri) }
        setDownloadListener(HuroutesDownloadListener(context))
    }

    private var _startLanguage: String? = null

    override val startUri: Uri get() {
        if (_startLanguage == null) {
            _startLanguage = (context as MainActivity).preferencesStore.languageIso2 ?:
                SITE_LANGUAGE
        }
        return (if (_startLanguage!! == SITE_LANGUAGE) defaultUri.toString() else
            SITE_TRANSLATE_TEMPLATE.format(_startLanguage, defaultUri.toString())).toUri()
    }

    override fun validateUri(uri: Uri?): Uri? {
        if (uri == null) return null

        val uris = arrayOf(
            defaultUri,
            SITE_TRANSLATE_URI_TEMPLATE.format(defaultUri.path).toUri(),
            GOOGLE_TRANSLATE_LINK_BASE.toUri()
        )
        for (u in uris) {
            if (u.host == uri.host && uri.path?.startsWith(u.path!!) == true) {
                return uri
            }
        }
        return null
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
        private const val LOG_TAG = "HuroutesWebView"
    }
}