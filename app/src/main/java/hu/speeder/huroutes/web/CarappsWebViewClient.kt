package hu.speeder.huroutes.web

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * A WebViewClient specialization for the huroutes functionality.
 */
open class CarappsWebViewClient: WebViewClient() {

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

    private var _handleUriCallback: ((Uri) -> Boolean)? = null
    fun setHandleUriCallback(callback: (Uri) -> Boolean) {
        _handleUriCallback = callback
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

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return if (handleUri(request?.url)) true else super.shouldOverrideUrlLoading(view, request)
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

    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        if (view.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            handler.proceed()
        } else {
            handler.cancel()
        }
    }

    private fun handleUri(maybeUri: Uri?): Boolean {
        if (maybeUri == null || _handleUriCallback == null) {
            return false
        }

        return _handleUriCallback!!(maybeUri)
    }
}