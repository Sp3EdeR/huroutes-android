package hu.speeder.huroutes

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class HuroutesSwipeRefreshLayout(context: Context, attrs: AttributeSet?) : SwipeRefreshLayout(context, attrs) {
    private var _canChildScrollUpCallback: (() -> Boolean)? = null
    fun setCanChildScrollUpCallback(callback: () -> Boolean) {
        _canChildScrollUpCallback = callback
    }

    override fun canChildScrollUp(): Boolean {
        return if (_canChildScrollUpCallback != null)
            _canChildScrollUpCallback!!() else super.canChildScrollUp()
    }
}