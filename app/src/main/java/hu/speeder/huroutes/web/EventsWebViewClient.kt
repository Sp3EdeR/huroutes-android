package hu.speeder.huroutes.web

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient

class EventsWebViewClient: CarappsWebViewClient() {
    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?,
    ) {
        super.onPageStarted(view, url, favicon)

        // Inject custom Javascript:
        // This code makes the site content fullscreen.
        val injectCssJs = """
            (function() {
                function applyClass() {
                    document.getElementsByTagName('html')[0].classList.add('fullscreen');
                }

                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', applyClass, { once: true });
                } else {
                    applyClass();
                }
            })();
        """.trimIndent()
        view?.evaluateJavascript(injectCssJs, null)
    }
}