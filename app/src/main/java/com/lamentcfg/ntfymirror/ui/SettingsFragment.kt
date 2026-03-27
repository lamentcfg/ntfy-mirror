package com.lamentcfg.ntfymirror.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.lamentcfg.ntfymirror.BuildConfig
import com.lamentcfg.ntfymirror.R
import com.lamentcfg.ntfymirror.data.SettingsRepository

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        settingsRepository = SettingsRepository.getInstance()

        setupPreferences()
    }

    private fun setupPreferences() {
        // Forwarding enabled toggle
        val forwardingEnabledPref = findPreference<SwitchPreferenceCompat>("forwarding_enabled")
        forwardingEnabledPref?.isChecked = settingsRepository.isForwardingEnabled()
        forwardingEnabledPref?.setOnPreferenceChangeListener { _, newValue ->
            settingsRepository.setForwardingEnabled(newValue as Boolean)
            true
        }

        // Forward ongoing notifications toggle
        val forwardOngoingPref = findPreference<SwitchPreferenceCompat>("forward_ongoing")
        forwardOngoingPref?.isChecked = settingsRepository.shouldForwardOngoing()
        forwardOngoingPref?.setOnPreferenceChangeListener { _, newValue ->
            settingsRepository.setForwardOngoing(newValue as Boolean)
            true
        }

        // Include body toggle
        val includeBodyPref = findPreference<SwitchPreferenceCompat>("include_body")
        includeBodyPref?.isChecked = settingsRepository.shouldIncludeBody()
        includeBodyPref?.setOnPreferenceChangeListener { _, newValue ->
            settingsRepository.setIncludeBody(newValue as Boolean)
            true
        }

        // Battery optimization
        val batteryPref = findPreference<Preference>("battery_optimization")
        updateBatteryOptimizationSummary(batteryPref)
        batteryPref?.setOnPreferenceClickListener {
            requestIgnoreBatteryOptimizations()
            true
        }

        // Version info
        val versionPref = findPreference<Preference>("app_version")
        versionPref?.summary = getString(R.string.pref_version_summary, BuildConfig.VERSION_NAME)

        // Donate button
        val donatePref = findPreference<Preference>("donate")
        donatePref?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lamentcfg.com/donate"))
            startActivity(intent)
            true
        }

        // Source Code button
        val sourceCodePref = findPreference<Preference>("source_code")
        sourceCodePref?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lamentcfg/ntfy-mirror"))
            startActivity(intent)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        // Update battery optimization summary when returning from settings
        val batteryPref = findPreference<Preference>("battery_optimization")
        updateBatteryOptimizationSummary(batteryPref)
    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = requireContext().getSystemService<PowerManager>()!!
        return powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
    }

    private fun updateBatteryOptimizationSummary(preference: Preference?) {
        preference?.summary = if (isBatteryOptimizationDisabled()) {
            getString(R.string.pref_battery_optimization_disabled)
        } else {
            getString(R.string.pref_battery_optimization_enabled)
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (!isBatteryOptimizationDisabled()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startActivity(intent)
        }
    }
}
