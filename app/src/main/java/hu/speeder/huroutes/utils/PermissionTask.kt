package hu.speeder.huroutes.utils

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface PermissionTask {
    val permissionsNeeded: Array<String>
    fun run()
}

fun PermissionTask.launch(scope: LifecycleCoroutineScope) {
    scope.launch(Dispatchers.IO) { run() }
}