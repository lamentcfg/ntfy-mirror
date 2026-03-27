package com.lamentcfg.ntfymirror.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.lamentcfg.ntfymirror.R
import com.lamentcfg.ntfymirror.data.ChannelDiscoveryManager
import com.lamentcfg.ntfymirror.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment displaying a list of apps with tabs for regular apps and system apps.
 * Allows enabling/disabling forwarding per app and per channel with expandable rows.
 */
class AppListFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: AppListAdapter
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var channelDiscoveryManager: ChannelDiscoveryManager
    private var currentApps: MutableList<AppInfo> = mutableListOf()
    private var cachedUserApps: List<AppInfo> = emptyList()
    private var cachedSystemApps: List<AppInfo> = emptyList()
    private var showSystemApps: Boolean = false
    private var isDataLoaded: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsRepository = SettingsRepository.getInstance()
        channelDiscoveryManager = ChannelDiscoveryManager.getInstance()

        setupMenu()
        bindViews(view)
        setupTabs()
        setupAdapter()
        loadApps()
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.apps_tab))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.system_tab))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showSystemApps = tab?.position == 1
                if (isDataLoaded) {
                    displayCachedApps()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupMenu() {
        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.app_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_select_all -> {
                        selectAllApps(true)
                        true
                    }
                    R.id.action_deselect_all -> {
                        selectAllApps(false)
                        true
                    }
                    else -> false
                }
            }
        }
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun selectAllApps(enabled: Boolean) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                for (app in currentApps) {
                    settingsRepository.setAppEnabled(app.packageName, enabled)
                    // Also update all channels for this app
                    for (channel in app.channels) {
                        settingsRepository.setChannelEnabled(app.packageName, channel.channelId, enabled)
                    }
                }
            }
            // Reload apps to reflect changes
            loadApps()
        }
    }

    private fun bindViews(view: View) {
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.appRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupAdapter() {
        adapter = AppListAdapter(
            onAppToggle = { appInfo, isEnabled ->
                settingsRepository.setAppEnabled(appInfo.packageName, isEnabled)
                // Update the local state
                val index = currentApps.indexOfFirst { it.packageName == appInfo.packageName }
                if (index >= 0) {
                    currentApps[index] = appInfo.copy(isEnabled = isEnabled)
                }
            },
            onChannelToggle = { channelInfo, isEnabled ->
                settingsRepository.setChannelEnabled(channelInfo.packageName, channelInfo.channelId, isEnabled)
                // Update the local state
                val appIndex = currentApps.indexOfFirst { it.packageName == channelInfo.packageName }
                if (appIndex >= 0) {
                    val app = currentApps[appIndex]
                    val updatedChannels = app.channels.map { channel ->
                        if (channel.channelId == channelInfo.channelId) {
                            channel.copy(isEnabled = isEnabled)
                        } else {
                            channel
                        }
                    }
                    currentApps[appIndex] = app.copy(channels = updatedChannels)
                }
            },
            onAppExpanded = { appInfo ->
                // Load channels when app is expanded
                loadChannelsForApp(appInfo.packageName)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun loadChannelsForApp(packageName: String) {
        lifecycleScope.launch {
            val channels = loadChannelsFromSystem(packageName)
            val index = currentApps.indexOfFirst { it.packageName == packageName }
            if (index >= 0) {
                currentApps[index] = currentApps[index].copy(
                    channels = channels,
                    isExpanded = true
                )
                adapter.submitList(currentApps.toList())
            }
        }
    }

    private suspend fun loadChannelsFromSystem(packageName: String): List<ChannelInfo> = withContext(Dispatchers.IO) {
        val configuredChannels = settingsRepository.getConfiguredChannels(packageName)
        val discoveredChannels = channelDiscoveryManager.getChannelsForPackage(packageName)
        val channels = mutableListOf<ChannelInfo>()

        // Combine configured channels with discovered channels
        val allChannelIds = configuredChannels + discoveredChannels.keys

        for (channelId in allChannelIds) {
            val isEnabled = settingsRepository.isChannelEnabled(packageName, channelId)
            val discovered = discoveredChannels[channelId]

            val channelName = discovered?.channelName
                ?: channelDiscoveryManager.formatChannelIdAsName(channelId)

            channels.add(
                ChannelInfo(
                    channelId = channelId,
                    channelName = channelName,
                    packageName = packageName,
                    isEnabled = isEnabled,
                    hasOngoingNotification = discovered?.hasOngoingNotification ?: false,
                    notificationCount = discovered?.notificationCount ?: 0
                )
            )
        }

        channels.sortedBy { it.channelName.lowercase() }
    }

    private fun loadApps() {
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Load both user and system apps once
            val (userApps, systemApps) = loadAllAppsFromSystem()
            cachedUserApps = userApps
            cachedSystemApps = systemApps
            isDataLoaded = true

            progressBar.visibility = View.GONE
            displayCachedApps()
        }
    }

    private fun displayCachedApps() {
        val apps = if (showSystemApps) cachedSystemApps else cachedUserApps
        currentApps.clear()
        currentApps.addAll(apps)

        if (apps.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.text = if (showSystemApps) {
                getString(R.string.no_system_apps_found)
            } else {
                getString(R.string.no_apps_found)
            }
        } else {
            emptyView.visibility = View.GONE
            adapter.submitList(apps)
            recyclerView.visibility = View.VISIBLE
        }
    }

    private suspend fun loadAllAppsFromSystem(): Pair<List<AppInfo>, List<AppInfo>> = withContext(Dispatchers.IO) {
        val pm = requireContext().packageManager
        val configuredApps = settingsRepository.getConfiguredApps()

        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val userApps = mutableListOf<AppInfo>()
        val systemApps = mutableListOf<AppInfo>()

        for (appInfo in installedApps) {
            try {
                val packageName = appInfo.packageName
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isSystemPackage = appInfo.packageName.startsWith("com.android.") ||
                        appInfo.packageName == "android"
                val hasLauncher = pm.getLaunchIntentForPackage(packageName) != null

                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val isEnabled = if (packageName in configuredApps) {
                    settingsRepository.isAppEnabled(packageName)
                } else {
                    true
                }

                val channels = loadChannelsFromSystem(packageName)

                val app = AppInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    isEnabled = isEnabled,
                    channels = channels
                )

                if (isSystemApp || isSystemPackage) {
                    systemApps.add(app)
                } else if (hasLauncher) {
                    userApps.add(app)
                }
            } catch (e: Exception) {
                // Skip apps that fail to load
            }
        }

        Pair(
            userApps.sortedBy { it.appName.lowercase() },
            systemApps.sortedBy { it.appName.lowercase() }
        )
    }
}
