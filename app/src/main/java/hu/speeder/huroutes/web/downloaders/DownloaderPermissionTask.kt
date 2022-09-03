package hu.speeder.huroutes.web.downloaders

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import hu.speeder.huroutes.utils.DownloadNotification
import hu.speeder.huroutes.utils.PermissionTask

class DownloaderPermissionTask(
    private val context: Context,
    uri: String,
    @Suppress("UNUSED_PARAMETER") userAgent: String?,
    @Suppress("UNUSED_PARAMETER") contentDisposition: String?,
    @Suppress("UNUSED_PARAMETER") mimeType: String?,
    @Suppress("UNUSED_PARAMETER") contentLength: Long): PermissionTask {

    private val downloader = getDownloaderFor(Uri.parse(uri)).setContext(context)

    override val permissionsNeeded get() = downloader.permissionsNeeded

    override fun run() {
        try {
            val (uri, fileName) = downloader.saveTo(Environment.DIRECTORY_DOWNLOADS)
            DownloadNotification(context)
                .setIntentUri(uri)
                .showText(fileName)
        }
        catch (e: Exception) {
            Log.w(LOG_TAG, e.message.toString())
        }
    }

    companion object {
        private const val LOG_TAG = "webViewClient"
    }
}