package hu.speeder.huroutes.web

import android.Manifest
import android.content.Context
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import hu.speeder.huroutes.MainActivity
import hu.speeder.huroutes.utils.PermissionTask

/**
 * A custom chrome client implementation for the huroutes web view.
 */
class HuroutesWebChromeClient(val context: Context): WebChromeClient() {
    /**
     * A location requesting task that signals back to the WebView about the permission result.
     */
    class DownloaderPermissionTask(
        private val origin: String?,
        private val callback: GeolocationPermissions.Callback?
    ): PermissionTask {
        override val permissionsNeeded: Array<String> = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        override fun run() {
            callback?.invoke(origin, true, false)
        }

        override fun error() {
            callback?.invoke(origin, false, false)
        }
    }

    /**
     * The WebView calls this function when the location is requested.
     */
    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        val task = DownloaderPermissionTask(origin, callback)
        (context as MainActivity).runTaskWithPermission(task)
    }
}