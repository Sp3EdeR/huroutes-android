package hu.speeder.huroutes.controls

import android.content.Context
import android.util.AttributeSet
import hu.speeder.huroutes.web.CarappsWebView
import hu.speeder.huroutes.web.HuroutesWebView

class HuroutesWebViewFragment : WebViewFragment() {
    override fun makeWebView(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int): CarappsWebView {
        return HuroutesWebView(ctx, attrs, defStyleAttr)
    }
}