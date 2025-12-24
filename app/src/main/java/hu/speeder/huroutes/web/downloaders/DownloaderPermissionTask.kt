package hu.speeder.huroutes.web.downloaders

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import hu.speeder.huroutes.MainActivity
import hu.speeder.huroutes.web.downloaders.DownloadNotificationPermissionTask
import hu.speeder.huroutes.utils.PermissionTask

/**
 * A file downloading task that requires permissions.
 * This task must be executed when the required permissions are granted.
 */
class DownloaderPermissionTask(
    private val context: Context,
    uri: String,
    @Suppress("UNUSED_PARAMETER") userAgent: String?,
    @Suppress("UNUSED_PARAMETER") contentDisposition: String?,
    @Suppress("UNUSED_PARAMETER") mimeType: String?,
    @Suppress("UNUSED_PARAMETER") contentLength: Long): PermissionTask {

    private val downloader = getDownloaderFor(context, Uri.parse(uri))

    override val permissionsNeeded get() = downloader.permissionsNeeded

    override fun run() {
        try {
            val (uri, fileName) = downloader.saveTo(Environment.DIRECTORY_DOWNLOADS)
            showDownloadNotification(uri, fileName)
        }
        catch (e: Exception) {
            Log.w(LOG_TAG, e.message.toString())
        }
    }

    private fun showDownloadNotification(uri: Uri, fileName: String) {
        val notificationTask = DownloadNotificationPermissionTask(context, uri, fileName)
        val activity = context as? MainActivity
        activity?.runTaskWithPermission(notificationTask)
    }

    override fun error() {}

    companion object {
        private const val LOG_TAG = "webViewClient"
    }
}