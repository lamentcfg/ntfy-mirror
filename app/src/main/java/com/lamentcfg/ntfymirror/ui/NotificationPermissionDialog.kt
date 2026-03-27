package com.lamentcfg.ntfymirror.ui

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.lamentcfg.ntfymirror.NtfyNotificationListenerService
import com.lamentcfg.ntfymirror.R

/**
 * Full-screen modal dialog that prompts the user to enable notification listener access.
 * This dialog is non-cancelable and blocks the UI until permission is granted.
 */
class NotificationPermissionDialog : DialogFragment() {

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

        val view = View.inflate(requireContext(), R.layout.dialog_notification_permission, null)
        dialog.setContentView(view)

        view.findViewById<MaterialButton>(R.id.openSettingsButton).setOnClickListener {
            openNotificationListenerSettings()
        }
    }

    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    companion object {
        const val TAG = "NotificationPermissionDialog"

        /**
         * Check if the notification listener service is enabled.
         */
        fun isNotificationListenerEnabled(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false

            return flat.contains(packageName)
        }
    }
}
