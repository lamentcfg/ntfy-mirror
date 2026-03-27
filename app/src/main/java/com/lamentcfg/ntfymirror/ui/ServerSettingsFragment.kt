package com.lamentcfg.ntfymirror.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lamentcfg.ntfymirror.R
import com.lamentcfg.ntfymirror.data.SettingsRepository
import com.lamentcfg.ntfymirror.network.NtfyClient
import kotlinx.coroutines.launch
import java.net.URL

class ServerSettingsFragment : Fragment() {

    private lateinit var serverUrlLayout: TextInputLayout
    private lateinit var serverUrlInput: TextInputEditText
    private lateinit var topicLayout: TextInputLayout
    private lateinit var topicInput: TextInputEditText
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var testConnectionButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var statusText: TextView

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var ntfyClient: NtfyClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_server_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsRepository = SettingsRepository.getInstance()
        ntfyClient = NtfyClient.getInstance()

        bindViews(view)
        loadSettings()
        setupListeners()
    }

    private fun bindViews(view: View) {
        serverUrlLayout = view.findViewById(R.id.serverUrlLayout)
        serverUrlInput = view.findViewById(R.id.serverUrlInput)
        topicLayout = view.findViewById(R.id.topicLayout)
        topicInput = view.findViewById(R.id.topicInput)
        usernameLayout = view.findViewById(R.id.usernameLayout)
        usernameInput = view.findViewById(R.id.usernameInput)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        passwordInput = view.findViewById(R.id.passwordInput)
        testConnectionButton = view.findViewById(R.id.testConnectionButton)
        saveButton = view.findViewById(R.id.saveButton)
        statusText = view.findViewById(R.id.statusText)
    }

    private fun loadSettings() {
        serverUrlInput.setText(settingsRepository.getServerUrl())
        topicInput.setText(settingsRepository.getTopic())
        usernameInput.setText(settingsRepository.getUsername())
        passwordInput.setText(settingsRepository.getPassword())
    }

    private fun setupListeners() {
        testConnectionButton.setOnClickListener {
            if (validateInput()) {
                testConnection()
            }
        }

        saveButton.setOnClickListener {
            if (validateInput()) {
                saveSettings()
            }
        }
    }

    private fun validateInput(): Boolean {
        val serverUrl = serverUrlInput.text.toString().trim()
        val topic = topicInput.text.toString().trim()

        if (serverUrl.isEmpty()) {
            serverUrlLayout.error = getString(R.string.server_url_required)
            return false
        }
        serverUrlLayout.error = null

        if (!isValidUrl(serverUrl)) {
            serverUrlLayout.error = getString(R.string.invalid_url)
            return false
        }
        serverUrlLayout.error = null

        if (topic.isEmpty()) {
            topicLayout.error = getString(R.string.topic_required)
            return false
        }
        topicLayout.error = null

        return true
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun testConnection() {
        val serverUrl = serverUrlInput.text.toString().trim()
        val topic = topicInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        testConnectionButton.isEnabled = false
        statusText.visibility = View.VISIBLE
        statusText.text = getString(R.string.test_connection)

        lifecycleScope.launch {
            val result = ntfyClient.testConnection(serverUrl, topic, username, password)

            testConnectionButton.isEnabled = true

            result.fold(
                onSuccess = {
                    statusText.text = getString(R.string.connection_successful)
                    statusText.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                    Toast.makeText(requireContext(), R.string.connection_successful, Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    val message = getString(R.string.connection_failed, error.message)
                    statusText.text = message
                    statusText.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun saveSettings() {
        val serverUrl = serverUrlInput.text.toString().trim()
        val topic = topicInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        settingsRepository.setServerUrl(serverUrl)
        settingsRepository.setTopic(topic)
        settingsRepository.setUsername(username)
        settingsRepository.setPassword(password)

        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
    }
}
