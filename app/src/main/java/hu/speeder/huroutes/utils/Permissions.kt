package hu.speeder.huroutes.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class Permissions {
    companion object {
        fun stillNeeded(context: Context, permissions: Array<String>): Array<String> {
            return permissions.filter {
                ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()
        }

        // This calls back in Activity::onRequestPermissionsResult
        fun request(context: Activity, permissions: Array<String>, requestCode: Int) {
            ActivityCompat.requestPermissions(context, permissions, requestCode)
        }
    }
}