package hu.speeder.huroutes

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.tabs.TabLayout
import hu.speeder.huroutes.controls.EventsWebViewFragment
import hu.speeder.huroutes.controls.HuroutesWebViewFragment
import hu.speeder.huroutes.controls.PhotospotsWebViewFragment
import hu.speeder.huroutes.controls.ViewPagerAdapter
import hu.speeder.huroutes.data.PreferencesStore
import hu.speeder.huroutes.databinding.ActivityMainBinding
import hu.speeder.huroutes.utils.PermissionTask
import hu.speeder.huroutes.utils.Permissions
import hu.speeder.huroutes.utils.Permissions.Companion.stillNeeded
import hu.speeder.huroutes.utils.TabData
import hu.speeder.huroutes.utils.launch
import hu.speeder.huroutes.utils.launchError
import hu.speeder.huroutes.web.EventsWebView
import hu.speeder.huroutes.web.HuroutesWebView
import hu.speeder.huroutes.web.PhotospotsWebView
import kotlinx.coroutines.launch

// Define tab data with URLs and navigation rail menu IDs
private val TAB_DATA = listOf(
    TabData(R.id.nav_events) {
        EventsWebViewFragment()
    },
    TabData(R.id.nav_huroutes) {
        HuroutesWebViewFragment()
    },
    TabData(R.id.nav_photospots) {
        PhotospotsWebViewFragment()
    }
)

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var _preferencesStore: PreferencesStore
    val preferencesStore get() = _preferencesStore

    /**
     * When the main activity is created, initialize the controls and bindings.
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

        val pageAdapter = ViewPagerAdapter(this, TAB_DATA)
        binding.viewPager.adapter = pageAdapter
        // Disable swipe to swap tabs (huroutes menu uses swiping)
        binding.viewPager.isUserInputEnabled = false
        // Retain two pages to the side of the active one (all pages)
        binding.viewPager.offscreenPageLimit = 2

        setupTabLayout()
        setupNavigationRail()

        // Get the last tab position from the preferences store
        val tabPosition = preferencesStore.lastTabPosition ?: 1
        binding.viewPager.setCurrentItem(tabPosition.coerceIn(0, TAB_DATA.size - 1), false)
    }

    /**
     * Saves the activity's state into the preferences store.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        preferencesStore.lastTabPosition = binding.viewPager.currentItem
    }

    /**
     * Handles orientation and view size changes.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
            binding.tabLayout.visibility = android.view.View.GONE
            binding.navigationRail.visibility = android.view.View.VISIBLE
        } else {
            binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            binding.tabLayout.visibility = android.view.View.VISIBLE
            binding.navigationRail.visibility = android.view.View.GONE
        }
        // Scroll to the active page in the ViewPager's inner recycler view to avoid page reset
        val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView
        recyclerView?.scrollToPosition(binding.viewPager.currentItem)
    }

    /**
     * Sets up the tab layout and binds it to the view pager.
     */
    private fun setupTabLayout() {
        val tabLayout = binding.root.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
        tabLayout?.let { tabs ->
            // Visible only when portrait
            if (resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                tabs.visibility = android.view.View.VISIBLE
            } else {
                tabs.visibility = android.view.View.GONE
            }

            // Increase tab icon size
            resizeTabIcons(tabs, resources)

            // Set initial selection
            tabs.getTabAt(binding.viewPager.currentItem)?.select()

            // Handle tab selection
            tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let { binding.viewPager.currentItem = it.position }
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })

            // Handle icon activation
            binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    tabs.getTabAt(position)?.select()
                }
            })
        }
    }

    /**
     * Resizes the tab's icons by programmatically changing the inner layout.
     */
    private fun resizeTabIcons(tabLayout: TabLayout, resources: Resources, iconSizeDp: Int = 48, paddingDp: Int = 4) {
        tabLayout.post {
            val tabStrip = tabLayout.getChildAt(0) as? ViewGroup
            tabStrip?.let { strip ->
                for (i in 0 until strip.childCount) {
                    val tabView = strip.getChildAt(i)
                    if (tabView is ViewGroup) {
                        for (j in 0 until tabView.childCount) {
                            val child = tabView.getChildAt(j)
                            if (child is ImageView) {
                                val sizePx = (iconSizeDp * resources.displayMetrics.density).toInt()
                                val paddingPx = (paddingDp * resources.displayMetrics.density).toInt()
                                child.layoutParams?.let { params ->
                                    params.width = sizePx
                                    params.height = sizePx
                                    child.layoutParams = params
                                }
                                child.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                                child.scaleType = ImageView.ScaleType.FIT_CENTER
                                child.requestLayout()
                            }
                        }
                    }
                }
                strip.requestLayout()
            }
        }
    }

    /**
     * Sets up the navigation rail and binds it to the view pager.
     */
    private fun setupNavigationRail() {
        val navigationRail = binding.root.findViewById<com.google.android.material.navigationrail.NavigationRailView>(R.id.navigationRail)
        navigationRail?.let { rail ->
            // Visible only when landscape
            if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                rail.visibility = android.view.View.VISIBLE
            } else {
                rail.visibility = android.view.View.GONE
            }

            // Force icon tinting to be disabled
            binding.navigationRail.itemIconTintList = null

            applyNavigationRailBackground()

            val setSelectedItem = { position: Int ->
                rail.selectedItemId = TAB_DATA.elementAt(position).navRes
            }

            // Set initial selection
            setSelectedItem(binding.viewPager.currentItem)

            // Handle navigation item selection
            rail.setOnItemSelectedListener { item ->
                val position = TAB_DATA.indexOfFirst { it.navRes == item.itemId }
                binding.viewPager.currentItem = if (position == -1) 0 else position
                true
            }

            // Handle icon activation
            binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    setSelectedItem(position)
                }
            })
        }
    }

    /**
      * Applies the background color to the navigation rail to match the tab layout.
      */
    private fun applyNavigationRailBackground() {
        val surfaceColor = MaterialColors.getColor(binding.tabLayout, com.google.android.material.R.attr.colorSurface)
        val railBackground = MaterialShapeDrawable().apply {
            fillColor = ColorStateList.valueOf(surfaceColor)
            elevation = 0f
            shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_NEVER
        }
        binding.navigationRail.background = railBackground
    }

    // TODO: Handle back press in active webview
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
            preferencesStore.languageIso2 = language
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