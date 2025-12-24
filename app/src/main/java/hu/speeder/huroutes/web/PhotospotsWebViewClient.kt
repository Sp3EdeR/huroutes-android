package hu.speeder.huroutes.web

import android.graphics.Bitmap
import android.webkit.WebView
import org.json.JSONObject

class PhotospotsWebViewClient: CarappsWebViewClient() {
    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?,
    ) {
        super.onPageStarted(view, url, favicon)

        val css = """
            #map-canvas > div:has(div[data-tooltip*="Autós"]) {
                display: none !important;
            }
        """.trimIndent()
        val escapedCss = JSONObject.quote(css)
        val injectCssJs = """
            (function() {
                function applyCss() {
                    var style = document.createElement('style');
                    (document.head || document.documentElement).appendChild(style);
                    style.innerHTML = $escapedCss;
                }

                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', applyCss, { once: true });
                } else {
                    applyCss();
                }
            })();
        """.trimIndent()

        view?.evaluateJavascript(injectCssJs, null)
    }
}