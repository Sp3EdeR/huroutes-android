package hu.speeder.huroutes.web

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import androidx.core.net.toUri

private const val PHOTOSPOTS_SITE_URI = "https://sp3eder.github.io/autofoto"
private const val PHOTOSPOTS_START_URI =  "${PHOTOSPOTS_SITE_URI}/fullscreen"
private const val GOOGLE_MAPS_EMBED_LINK_BASE = "https://www.google.com/maps/d/embed"

/**
 * A customized WebView that contains specializations for the photospots page.
 */
@SuppressLint("SetJavaScriptEnabled")
class PhotospotsWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
): CarappsWebView(
    PHOTOSPOTS_START_URI.toUri(),
    context, attrs, defStyleAttr) {

    private val _client = PhotospotsWebViewClient()
    override val client get() = _client

    init {
        webChromeClient = GeolocationPermissionWebChromeClient(context)
        webViewClient = _client
        _client.setHandleUriCallback { uri -> handleUri(uri) }
    }

    override fun validateUri(uri: Uri?): Uri? {
        if (uri == null) return null

        val siteUri = PHOTOSPOTS_SITE_URI.toUri()
        if (siteUri.host == uri.host && uri.path?.startsWith(siteUri.path!!) == true) {
            return PHOTOSPOTS_START_URI.toUri()
        }

        val mapsUri = GOOGLE_MAPS_EMBED_LINK_BASE.toUri()
        if (mapsUri.host == uri.host && uri.path?.startsWith(mapsUri.path!!) == true) {
            return uri
        }

        return null
    }
}