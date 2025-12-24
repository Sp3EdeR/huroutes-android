package hu.speeder.huroutes.web.downloaders

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import hu.speeder.huroutes.utils.DownloadNotification
import hu.speeder.huroutes.utils.PermissionTask

/**
 * Permission-gated task that shows a download notification once POST_NOTIFICATIONS is granted.
 */
class DownloadNotificationPermissionTask(
    private val context: Context,
    private val intentUri: Uri,
    private val fileName: String,
): PermissionTask {

    override val permissionsNeeded: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        else emptyArray()

    override fun run() {
        DownloadNotification(context)
            .setIntentUri(intentUri)
            .showText(fileName)
    }

    override fun error() {
        // User denied notification permission; nothing else to do.
    }
}