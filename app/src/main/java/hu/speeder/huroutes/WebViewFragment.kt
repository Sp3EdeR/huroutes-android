package hu.speeder.huroutes

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebSettings.FORCE_DARK_OFF
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import hu.speeder.huroutes.databinding.FragmentWebviewBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val LOG_TAG = "huroutes/webview"
const val SITE_PATH = "sp3eder.github.io/huroutes"

class WebViewFragment : Fragment() {

    private var _binding: FragmentWebviewBinding? = null
    private val binding get() = _binding!!

    private var _webViewClient: HuroutesWebViewClient? = null
    private val webViewClient get() = _webViewClient!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var uri = "https://${SITE_PATH}/"
        try {
            val inUri = activity?.intent?.data?.toString()!!
            Log.d(LOG_TAG, "Received URI in intent: ${inUri}")
            if (inUri.contains(SITE_PATH))
                uri = inUri
            Log.i(LOG_TAG, "Accepted URI in intent: ${uri}")
        }
        catch (_: Exception) {
        }

        _webViewClient = HuroutesWebViewClient(lifecycleScope)
        webViewClient.apply {
            setErrorCallback {
                delay(5000)
                if (webViewClient.hasError) {
                    binding.webView.reload()
                }
            }
            setLoadedCallback {
                binding.hourglassView.visibility = View.GONE
            }
        }

        binding.webView.apply {
            webViewClient = this@WebViewFragment.webViewClient
            settings.apply {
                domStorageEnabled = true // localstorage
                displayZoomControls = false // No zoom controls (only pinch)
                javaScriptEnabled = true // Javascript
                javaScriptCanOpenWindowsAutomatically = true // window.open
                if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
                    forceDark = WebSettings.FORCE_DARK_OFF // Day mode always
                }
            }
            loadUrl(uri)
        }

        (activity as? MainActivity)?.setOnBackPressedCallback {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
                true
            } else {
                false
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        _webViewClient = null
        (activity as? MainActivity)?.setOnBackPressedCallback(null)
        super.onDestroyView()
    }

}