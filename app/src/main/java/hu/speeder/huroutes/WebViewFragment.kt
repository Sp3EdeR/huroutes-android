package hu.speeder.huroutes

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import hu.speeder.huroutes.databinding.FragmentWebviewBinding

const val LOG_TAG = "huroutes/webView"

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
                binding.hourglassView.visibility = View.GONE
            }

            setCoroutineScope(lifecycleScope)
            loadUrl(getStartUri().toString())

            (activity as? MainActivity)?.setOnBackPressedCallback {
                canGoBack() && goBack() == Unit
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        (activity as? MainActivity)?.setOnBackPressedCallback(null)
        super.onDestroyView()
    }

    private fun getStartUri(): Uri {
        var uri = binding.webView.startUri
        try {
            val inUri = Uri.parse(requireActivity().intent!!.data!!.toString())
            Log.d(LOG_TAG, "Received URI in intent: $inUri")
            if (inUri.host == uri.host && inUri.path?.startsWith(uri.path!!) == true) {
                uri = inUri
                Log.i(LOG_TAG, "Accepted URI in intent: $uri")
            }
        }
        catch (_: Exception) {
        }
        return uri
    }
}