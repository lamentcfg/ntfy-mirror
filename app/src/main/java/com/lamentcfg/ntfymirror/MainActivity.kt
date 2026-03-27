package com.lamentcfg.ntfymirror

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar
import com.lamentcfg.ntfymirror.databinding.ActivityMainBinding
import com.lamentcfg.ntfymirror.ui.BatteryOptimizationDialog
import com.lamentcfg.ntfymirror.ui.NotificationPermissionDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var permissionDialog: NotificationPermissionDialog? = null
    private var batteryDialog: BatteryOptimizationDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (!NotificationPermissionDialog.isNotificationListenerEnabled(this)) {
            // Notification permission not granted - show notification permission dialog
            if (permissionDialog?.isAdded != true) {
                permissionDialog = NotificationPermissionDialog()
                permissionDialog?.show(supportFragmentManager, NotificationPermissionDialog.TAG)
            }
            // Dismiss battery dialog if showing
            batteryDialog?.dismiss()
            batteryDialog = null
        } else {
            // Notification permission granted
            permissionDialog?.dismiss()
            permissionDialog = null

            // Check if we should show battery optimization dialog
            checkBatteryOptimization()
        }
    }

    private fun checkBatteryOptimization() {
        val settingsRepository = com.lamentcfg.ntfymirror.data.SettingsRepository.getInstance()
        val alreadyPrompted = settingsRepository.wasBatteryOptimizationPromptShown()
        val isOptimized = !BatteryOptimizationDialog.isBatteryOptimizationDisabled(this)

        // Only show if: not already prompted AND battery optimization is still enabled
        if (!alreadyPrompted && isOptimized) {
            if (batteryDialog?.isAdded != true) {
                batteryDialog = BatteryOptimizationDialog()
                batteryDialog?.show(supportFragmentManager, BatteryOptimizationDialog.TAG)
            }
        } else {
            batteryDialog?.dismiss()
            batteryDialog = null
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(R.id.serverSettingsFragment, R.id.appListFragment, R.id.settingsFragment)
        )

        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setupWithNavController(navController)
    }
}
