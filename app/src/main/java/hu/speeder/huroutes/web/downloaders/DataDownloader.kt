package hu.speeder.huroutes.web.downloaders

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.webkit.MimeTypeMap
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Regex for the media type format used by `data` URIs.
 */
const val MEDIA_TYPE_SCHEME = """(?<mime>[^;,]+)(?<params>(?:;(?:charset=(?<charset>[^;,]+)|(?<param>[^=]+)=(?<val>[^;,]+)))+)"""
/**
 * Regex for `data` URIs.
 */
const val DATA_URI_SCHEME = """data:(?:$MEDIA_TYPE_SCHEME)?(?<b64>;base64)?,(?<data>.*)"""

/**
 * The file names to be generated.
 * Unfortunately `WebView` hides the `<a download="...">` attribute...
 */
const val FILE_NAME = "huroutes_track"
/**
 * The file name's timestamp format
 */
const val FILE_TIME_FORMAT = "yyyyMMdd_HHmmss"

/**
 * A downloader for URIs with the `data` scheme.
 */
class DataDownloader(private val uri: Uri): DownloaderBase() {

    private var content: ByteArray? = null
    private var mimeType: String? = null
    private lateinit var extension: String

    init {
        parseUri()
    }

    /**
     * Parses the `data` URI to be downloaded
     */
    private fun parseUri() {
        val parseResult = dataParser.matchEntire(uri.toString()) ?: return

        val mimeMatch = parseResult.groups["mime"]
        mimeType = mimeMatch?.value
        extension = if (mimeType == null) "" else getExtensionFromMimetype(mimeType!!)
        val data = parseResult.groups["data"]!!.value
        content = if (parseResult.groups["b64"] == null) {
            val charset = parseResult.groups["charset"]?.value ?: "utf-8"
            URLDecoder.decode(data, charset).toByteArray(Charsets.UTF_8)
        } else {
            Base64.decode(data, Base64.DEFAULT)
        }
    }

    /**
     * Specifies that this downloader
     */
    override val permissionsNeeded: Array<String> =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) arrayOf(WRITE_EXTERNAL_STORAGE) else emptyArray()

    override fun saveTo(directory: String): Pair<Uri, String> {
        val fileName = "$FILE_NAME-${dateFormatter.format(Date())}.$extension"
        val (uri, stream) = createMediaFileStream(directory, fileName, mimeType)
        stream.write(content)
        stream.close()
        return Pair(uri, fileName)
    }

    companion object {
        private val dataParser = Regex(DATA_URI_SCHEME)
        private val dateFormatter = SimpleDateFormat(FILE_TIME_FORMAT, Locale.getDefault())

        /**
         * MIME type to extension resolver, that extends the system's resolver capabilities.
         */
        private fun getExtensionFromMimetype(mime: String): String {
            return when (mime) {
                "application/gpx+xml" -> "gpx"
                "application/vnd.google-earth.kml+xml" -> "kml"
                else -> null
            } ?:
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: ""
        }
    }
}