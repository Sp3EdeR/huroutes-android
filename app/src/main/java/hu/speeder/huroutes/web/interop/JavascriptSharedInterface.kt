package hu.speeder.huroutes.web.interop

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import hu.speeder.huroutes.MainActivity

class ShareData(
    val title: String?,
    val text: String?,
    val url: String?
)

class JavaScriptSharedInterface(
    val context: Context,
) {
    @JavascriptInterface
    fun share(shareDataJson: String) {
        val data = shareDataAdapter.fromJson(shareDataJson)
        var text = ""
        if (data?.text != null)
            text += data.text
        if (data?.url != null)
            if (text != "")
                text += " "
            text += data!!.url

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, data.title))
    }

    @JavascriptInterface
    fun setLang(lang: String) {
        var language: String? = null
        if (lang != "undefined") {
            language = lang
        }
        (context as MainActivity).onLanguageUpdated(language)
    }

    companion object {
        private val moshi: Moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        var shareDataAdapter: JsonAdapter<ShareData> = moshi.adapter(ShareData::class.java)
    }
}