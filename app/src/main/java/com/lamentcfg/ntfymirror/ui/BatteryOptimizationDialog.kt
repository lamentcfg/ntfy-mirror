package com.lamentcfg.ntfymirror.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.core.content.getSystemService
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.lamentcfg.ntfymirror.R
import com.lamentcfg.ntfymirror.data.SettingsRepository

/**
 * Dialog that prompts the user to disable battery optimization.
 * Only shown once on first run after notification permission is granted.
 */
class BatteryOptimizationDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        isCancelable = false
        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar)
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        val view = View.inflate(requireContext(), R.layout.dialog_battery_optimization, null)
        dialog.setContentView(view)

        view.findViewById<MaterialButton>(R.id.openSettingsButton).setOnClickListener {
            openBatteryOptimizationSettings()
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.skipButton).setOnClickListener {
            SettingsRepository.getInstance().setBatteryOptimizationPromptShown(true)
            dismiss()
        }
    }

    private fun openBatteryOptimizationSettings() {
        SettingsRepository.getInstance().setBatteryOptimizationPromptShown(true)
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${requireContext().packageName}")
        }
        startActivity(intent)
    }

    companion object {
        const val TAG = "BatteryOptimizationDialog"

        /**
         * Check if battery optimization is disabled for this app.
         */
        fun isBatteryOptimizationDisabled(context: Context): Boolean {
            val powerManager = context.getSystemService<PowerManager>()!!
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
    }
}
