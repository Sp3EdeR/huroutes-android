package hu.speeder.huroutes.utils

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Specifies a task that requires Android permissions to be executed.
 */
interface PermissionTask {
    val permissionsNeeded: Array<String>
    fun run()
    fun error()
}

/**
 * Launches the task asynchronously on the I/O dispatcher.
 */
fun PermissionTask.launch(scope: LifecycleCoroutineScope) {
    scope.launch(Dispatchers.IO) { run() }
}

/**
 * Launches the task's error handler asynchronously on the I/O dispatcher.
 */
fun PermissionTask.launchError(scope: LifecycleCoroutineScope) {
    scope.launch(Dispatchers.IO) { error() }
}