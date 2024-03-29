package hu.speeder.huroutes

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import hu.speeder.huroutes.databinding.FragmentWebviewBinding

/**
 * A fragment that contains the main WebView control.
 */
class WebViewFragment : Fragment() {

    private var _binding: FragmentWebviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.apply {
            client.setLoadedCallback {
                binding.swipeRefresh.isRefreshing = false
                updateOfflineMode() // Re-enable cache after a refresh
            }

            setCoroutineScope(lifecycleScope)
            loadUrl(getStartUri().toString())

            (activity as? MainActivity)?.setOnBackPressedCallback {
                canGoBack() && goBack() == Unit
            }
        }

        binding.swipeRefresh.apply {
            setCanChildScrollUpCallback {
                binding.webView.scrollable.y || !binding.webView.isNetworkAvailable()
            }
            setOnRefreshListener {
                binding.webView.also { vw ->
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
        (activity as? MainActivity)?.setOnBackPressedCallback(null)
        super.onDestroyView()
    }

    /**
     * Returns the initial URI to be loaded in the WebView.
     *
     * The URI can be received from an intent used to launch the app.
     */
    private fun getStartUri(): Uri {
        var uri = binding.webView.startUri
        try {
            val inUri = Uri.parse(requireActivity().intent!!.data!!.toString())
            Log.d(LOG_TAG, "Received URI in intent: $inUri")
            if (binding.webView.validateUri(inUri)) {
                uri = inUri
                Log.i(LOG_TAG, "Accepted URI in intent: $uri")
            }
        }
        catch (_: Exception) {
        }
        return uri
    }

    companion object {
        private const val LOG_TAG = "webViewFragment"
    }
}