package hu.speeder.huroutes.web

import android.webkit.WebView

class HuroutesWebViewClient : CarappsWebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        // Inject custom Javascript:
        // This code implements the WebShare API for URLs.
        val injectCssJs = """
            navigator.share=shData=>android.share(JSON.stringify(shData));
            android.setLang(huroutes.opt.l10n.providers.Google.getCurrentLang());
        """.trimIndent()
        view?.evaluateJavascript(injectCssJs, null)
    }
}