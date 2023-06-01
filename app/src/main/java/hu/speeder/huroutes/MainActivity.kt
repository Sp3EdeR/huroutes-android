package hu.speeder.huroutes

import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import hu.speeder.huroutes.data.PreferencesStore
import hu.speeder.huroutes.databinding.ActivityMainBinding
import hu.speeder.huroutes.utils.PermissionTask
import hu.speeder.huroutes.utils.Permissions
import hu.speeder.huroutes.utils.Permissions.Companion.stillNeeded
import hu.speeder.huroutes.utils.launch
import hu.speeder.huroutes.utils.launchError
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var _preferencesStore: PreferencesStore
    val preferencesStore get() = _preferencesStore

    /**
     * Allows other components to register a handler for `onBackPressed`.
     */
    fun setOnBackPressedCallback(callback: (() -> Boolean)?) {
        _onBackPressedCallback = callback
    }
    private var _onBackPressedCallback: (() -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _preferencesStore = PreferencesStore(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        _onBackPressedCallback?.also {
            if (it()) return
        }

        super.onBackPressed()
    }

    /**
     * Stores a map from a permission-list identifier to an ID within `requestTasks`.
     *
     * This helps avoiding requesting the same permission set twice at the same time.
     */
    private var permissionRequests: MutableMap<String, Int> = mutableMapOf()

    /**
     * A container of tasks awaiting permissions to run, organized by the requestCodes.
     */
    private val requestTasks: MutableMap<Int, MutableList<PermissionTask>> = mutableMapOf()

    /**
     * Requests the needed permissions from the user.
     *
     * The permission tasks are stored while awaiting for the permissions.
     * The `onRequestPermissionsResult` callback handles the results for this request.
     */
    fun runTaskWithPermission(task: PermissionTask) {
        val permissions = task.permissionsNeeded.sortedArray()
        val neededPermissions = stillNeeded(this, permissions).sortedArray()
        if (neededPermissions.isNotEmpty()) {
            var requestCode = permissionRequests[neededPermissions.joinToString()]
            if (requestCode == null) {
                requestCode = getUniqueTaskCode()
                permissionRequests[neededPermissions.joinToString()] = requestCode
                requestTasks[requestCode] = mutableListOf()
                // Request needed permissions for this task
                Permissions.request(this, neededPermissions, requestCode)
            }
            requestTasks[requestCode]!!.add(task)
            return
        }

        task.launch(lifecycleScope)
    }

    /**
     * Processes the system response for permission requests.
     *
     * When permissions are granted, executes the tasks awaiting the permissions.
     * Otherwise it only does cleanup.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        permissionRequests = permissionRequests.filterValues { it != requestCode } as MutableMap
        val tasks = requestTasks.remove(requestCode)!!
        if (grantResults.all { it == PERMISSION_GRANTED }) {
            for (task in tasks) {
                task.launch(lifecycleScope)
            }
        } else {
            for (task in tasks) {
                task.launchError(lifecycleScope)
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun onLanguageUpdated(language: String?) {
        lifecycleScope.launch {
            preferencesStore.saveLanguageToPreferencesStore(language)
        }
    }

    /**
     * Returns a unique request code for permission requests.
     */
    private fun getUniqueTaskCode(): Int {
        val range = (0..Int.MAX_VALUE)
        var value = range.random()
        while (requestTasks.containsKey(value))
            value = range.random()
        return value
    }
}