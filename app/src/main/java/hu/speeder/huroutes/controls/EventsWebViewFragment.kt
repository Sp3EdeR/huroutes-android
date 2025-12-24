package hu.speeder.huroutes.controls

import android.content.Context
import android.util.AttributeSet
import hu.speeder.huroutes.web.CarappsWebView
import hu.speeder.huroutes.web.EventsWebView

class EventsWebViewFragment : WebViewFragment() {
    override fun makeWebView(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int): CarappsWebView {
        return EventsWebView(ctx, attrs, defStyleAttr)
    }
}