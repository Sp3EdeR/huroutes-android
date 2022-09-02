package hu.speeder.huroutes

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import hu.speeder.huroutes.databinding.ActivityMainBinding
import hu.speeder.huroutes.databinding.FragmentWebviewBinding

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var _onBackPressedCallback: (() -> Boolean)? = null
    fun setOnBackPressedCallback(callback: (() -> Boolean)?) {
        _onBackPressedCallback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onBackPressed() {
        _onBackPressedCallback?.also {
            if (it()) return
        }

        super.onBackPressed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}