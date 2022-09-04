package hu.speeder.huroutes.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

/**
 * A utility class that helps with Android permission tasks.
 */
class Permissions {
    companion object {
        /**
         * Filters the permissions to the ones that are not granted to the context yet.
         */
        fun stillNeeded(context: Context, permissions: Array<String>): Array<String> {
            return permissions.filter {
                ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()
        }

        /**
         * Initiates a request for the specified permissions.
         * The result of this call is given in the `Activity::onRequestPermissionsResult` method.
         */
        fun request(context: Activity, permissions: Array<String>, requestCode: Int) {
            ActivityCompat.requestPermissions(context, permissions, requestCode)
        }
    }
}