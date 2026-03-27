package com.lamentcfg.ntfymirror.network

import com.lamentcfg.ntfymirror.NtfyMirrorApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Client for communicating with ntfy server via HTTP API.
 * Supports both anonymous and authenticated publishing.
 */
class NtfyClient(private val okHttpClient: OkHttpClient) {

    /**
     * Publishes a message to a ntfy topic.
     *
     * @param serverUrl The ntfy server URL (e.g., "https://ntfy.sh")
     * @param topic The topic to publish to
     * @param title The message title
     * @param message The message body
     * @param username Optional username for authentication
     * @param password Optional password for authentication
     * @return Result indicating success or failure with error message
     */
    suspend fun publish(
        serverUrl: String,
        topic: String,
        title: String,
        message: String,
        username: String = "",
        password: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Build the full URL with topic
            val url = buildUrl(serverUrl, topic)

            // Build the request - ntfy expects message as body, title as header
            val requestBody = message.toRequestBody("text/plain".toMediaType())

            // Build the request
            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("X-Title", title)

            // Add authentication if credentials are provided
            if (username.isNotEmpty() && password.isNotEmpty()) {
                requestBuilder.header("Authorization", Credentials.basic(username, password))
            }

            val request = requestBuilder.build()

            // Execute the request
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.body.string()
                    Result.failure(IOException("HTTP ${response.code}: $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tests the connection to the ntfy server.
     *
     * @param serverUrl The ntfy server URL
     * @param topic The topic to test
     * @param username Optional username for authentication
     * @param password Optional password for authentication
     * @return Result indicating success or failure with error message
     */
    suspend fun testConnection(
        serverUrl: String,
        topic: String,
        username: String = "",
        password: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(serverUrl, topic)

            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                requestBuilder.header("Authorization", Credentials.basic(username, password))
            }

            val request = requestBuilder.build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404) {
                    // 404 is acceptable for GET on a topic (just means no cached messages)
                    Result.success(Unit)
                } else if (response.code == 401 || response.code == 403) {
                    Result.failure(IOException("Authentication failed"))
                } else {
                    val errorBody = response.body.string()
                    Result.failure(IOException("HTTP ${response.code}: $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildUrl(serverUrl: String, topic: String): String {
        val baseUrl = serverUrl.trimEnd('/')
        return "$baseUrl/$topic"
    }

    companion object {
        /**
         * Gets the singleton instance of NtfyClient.
         */
        fun getInstance(): NtfyClient {
            return InstanceHolder.instance
        }

        private object InstanceHolder {
            val instance = NtfyClient(NtfyMirrorApplication.ServiceLocator.getOkHttpClient())
        }
    }
}
