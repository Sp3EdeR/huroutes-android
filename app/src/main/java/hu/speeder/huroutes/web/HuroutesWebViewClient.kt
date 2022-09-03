package hu.speeder.huroutes.web

import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class HuroutesWebViewClient: WebViewClient() {

    @Volatile
    private var _hasError = false
    val hasError get() = _hasError

    private var _errorCallback: ((WebResourceError?) -> Unit)? = null
    val hasErrorCallback get() = _errorCallback != null
    fun setErrorCallback(callback: (WebResourceError?) -> Unit) {
        _errorCallback = callback
    }

    private var _loadedCallback: (() -> Unit)? = null
    fun setLoadedCallback(callback: () -> Unit) {
        _loadedCallback = callback
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ||
            request == null || !request.isForMainFrame) {
            return
        }

        _hasError = true
        _errorCallback?.also {
            it(error)
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        _hasError = false
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)

        _loadedCallback?.also {
            it()
        }
    }
}