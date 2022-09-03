package hu.speeder.huroutes

import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import hu.speeder.huroutes.databinding.ActivityMainBinding
import hu.speeder.huroutes.utils.PermissionTask
import hu.speeder.huroutes.utils.Permissions
import hu.speeder.huroutes.utils.Permissions.Companion.stillNeeded
import hu.speeder.huroutes.utils.launch

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var _onBackPressedCallback: (() -> Boolean)? = null
    fun setOnBackPressedCallback(callback: (() -> Boolean)?) {
        _onBackPressedCallback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onBackPressed() {
        _onBackPressedCallback?.also {
            if (it()) return
        }

        super.onBackPressed()
    }

    private var permissionRequests: MutableMap<String, Int> = mutableMapOf()
    private val requestTasks: MutableMap<Int, MutableList<PermissionTask>> = mutableMapOf()

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
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getUniqueTaskCode(): Int {
        val range = (0..Int.MAX_VALUE)
        var value = range.random()
        while (requestTasks.containsKey(value))
            value = range.random()
        return value
    }
}