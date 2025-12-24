package hu.speeder.huroutes.controls

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.core.content.PackageManagerCompat.LOG_TAG
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import hu.speeder.huroutes.MainActivity
import hu.speeder.huroutes.databinding.FragmentWebviewBinding
import hu.speeder.huroutes.web.CarappsWebView

/**
 * A fragment that contains the main WebView control.
 */
open abstract class WebViewFragment() : Fragment() {

    private var _binding: FragmentWebviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var webView: CarappsWebView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create and add the WebView to the fragment
        webView = makeWebView(requireContext(), null, 0).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        binding.swipeRefresh.addView(webView)

        // Apply web view integrations
        webView.apply {
            client.setLoadedCallback {
                binding.swipeRefresh.isRefreshing = false
                updateOfflineMode() // Re-enable cache after a refresh
            }

            setCoroutineScope(lifecycleScope)
            loadUrl(getStartUri().toString())
        }

        binding.swipeRefresh.apply {
            setCanChildScrollUpCallback {
                webView.scrollable.y || !webView.isNetworkAvailable()
            }
            setOnRefreshListener {
                webView.also { vw ->
                    vw.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    vw.reload()
                    isRefreshing = true
                }
            }
            isRefreshing = true
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /**
     * Returns the initial URI to be loaded in the WebView.
     *
     * The URI can be received from an intent used to launch the app.
     */
    private fun getStartUri(): Uri {
        var uri = webView.startUri
        try {
            var inUri = requireActivity().intent?.data
            if (inUri != null) {
                Log.d(LOG_TAG, "Received URI in intent: $inUri")
            }
            inUri = webView.validateUri(inUri)
            if (inUri != null) {
                uri = inUri
                (activity as? MainActivity)?.activatePage(this)
                Log.i(LOG_TAG, "Accepted URI in intent: $inUri")
            }
        }
        catch (_: Exception) {
        }
        return uri
    }

    /**
     * Handles the back button press in the WebView.
     */
    fun onBackPressed(): Boolean {
        return webView.canGoBack() && webView.goBack() == Unit
    }

    protected abstract fun makeWebView(ctx: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): CarappsWebView

    companion object {
        private const val LOG_TAG = "webViewFragment"
    }
}