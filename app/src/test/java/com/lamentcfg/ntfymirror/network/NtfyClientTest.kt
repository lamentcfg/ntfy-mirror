package com.lamentcfg.ntfymirror.network

import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for NtfyClient.
 * Tests the HTTP communication with ntfy server using mocked OkHttp client.
 */
class NtfyClientTest {

    private lateinit var mockHttpClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var ntfyClient: NtfyClient

    @Before
    fun setUp() {
        mockHttpClient = mock()
        mockCall = mock()
        ntfyClient = NtfyClient(mockHttpClient)
    }

    // publish() Tests

    @Test
    fun `publish returns success on HTTP 200`() = runTest {
        val mockResponse = createMockResponse(200, "{}")
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        val result = ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "Test Title",
            message = "Test message"
        )

        assertTrue(result.isSuccess)
        verify(mockHttpClient).newCall(any())
    }

    @Test
    fun `publish returns failure on HTTP 400`() = runTest {
        val mockResponse = createMockResponse(400, "Bad request")
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        val result = ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "Test",
            message = "Message"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("400") == true)
    }

    @Test
    fun `publish returns failure on HTTP 500`() = runTest {
        val mockResponse = createMockResponse(500, "Internal server error")
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        val result = ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "Test",
            message = "Message"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
    }

    @Test
    fun `publish with credentials adds Authorization header`() = runTest {
        val mockResponse = createMockResponse(200, "{}")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "Test",
            message = "Message",
            username = "testuser",
            password = "testpass"
        )

        assertNotNull(capturedRequest?.header("Authorization"))
        assertTrue(capturedRequest?.header("Authorization")!!.startsWith("Basic "))
    }

    @Test
    fun `publish without credentials has no Authorization header`() = runTest {
        val mockResponse = createMockResponse(200, "{}")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "Test",
            message = "Message"
        )

        assertNull(capturedRequest?.header("Authorization"))
    }

    @Test
    fun `publish with empty username does not add Authorization header`() = runTest {
        val mockResponse = createMockResponse(200, "{}")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "Test",
            message = "Message",
            username = "",
            password = "testpass"
        )

        assertNull(capturedRequest?.header("Authorization"))
    }

    @Test
    fun `publish with empty password does not add Authorization header`() = runTest {
        val mockResponse = createMockResponse(200, "{}")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "Test",
            message = "Message",
            username = "testuser",
            password = ""
        )

        assertNull(capturedRequest?.header("Authorization"))
    }

    @Test
    fun `publish builds correct URL with topic`() = runTest {
        val mockResponse = createMockResponse(200, "{}")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "my-secret-topic",
            title = "Test",
            message = "Message"
        )

        val url = capturedRequest?.url?.toString()
        assertTrue(url?.endsWith("/my-secret-topic") == true)
    }

    @Test
    fun `publish handles server URL with trailing slash`() = runTest {
        val mockResponse = createMockResponse(200, "{}")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.publish(
            serverUrl = "https://ntfy.sh/",
            topic = "topic",
            title = "Test",
            message = "Message"
        )

        val url = capturedRequest?.url?.toString()
        assertTrue(url?.endsWith("/topic") == true)
        assertFalse(url?.contains("//topic") == true)
    }

    @Test
    fun `publish uses POST method`() = runTest {
        val mockResponse = createMockResponse(200, "{}")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "Test",
            message = "Message"
        )

        assertEquals("POST", capturedRequest?.method)
    }

    @Test
    fun `publish returns failure on network exception`() = runTest {
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenThrow(RuntimeException("Network error"))

        val result = ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "Test",
            message = "Message"
        )

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    // testConnection() Tests

    @Test
    fun `testConnection returns success on HTTP 200`() = runTest {
        val mockResponse = createMockResponse(200, "[]")
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        val result = ntfyClient.testConnection(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic"
        )

        assertTrue(result.isSuccess)
        verify(mockHttpClient).newCall(any())
    }

    @Test
    fun `testConnection returns success on HTTP 404`() = runTest {
        // 404 is acceptable - just means no cached messages on the topic
        val mockResponse = createMockResponse(404, "Not found")
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        val result = ntfyClient.testConnection(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic"
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `testConnection returns failure on HTTP 401`() = runTest {
        val mockResponse = createMockResponse(401, "Unauthorized")
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        val result = ntfyClient.testConnection(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Authentication failed") == true)
    }

    @Test
    fun `testConnection returns failure on HTTP 403`() = runTest {
        val mockResponse = createMockResponse(403, "Forbidden")
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        val result = ntfyClient.testConnection(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Authentication failed") == true)
    }

    @Test
    fun `testConnection with credentials adds Authorization header`() = runTest {
        val mockResponse = createMockResponse(200, "[]")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.testConnection(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            username = "user",
            password = "pass"
        )

        assertNotNull(capturedRequest?.header("Authorization"))
        assertTrue(capturedRequest?.header("Authorization")!!.startsWith("Basic "))
    }

    @Test
    fun `testConnection returns failure on HTTP 500`() = runTest {
        val mockResponse = createMockResponse(500, "Server error")
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        val result = ntfyClient.testConnection(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
    }

    @Test
    fun `testConnection returns failure on network exception`() = runTest {
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenThrow(RuntimeException("Network error"))

        val result = ntfyClient.testConnection(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic"
        )

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `testConnection uses GET method`() = runTest {
        val mockResponse = createMockResponse(200, "[]")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.testConnection(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic"
        )

        assertEquals("GET", capturedRequest?.method)
    }

    @Test
    fun `testConnection builds correct URL with topic`() = runTest {
        val mockResponse = createMockResponse(200, "[]")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.testConnection(
            serverUrl = "https://ntfy.sh",
            topic = "my-topic"
        )

        val url = capturedRequest?.url?.toString()
        assertTrue(url?.endsWith("/my-topic") == true)
    }

    @Test
    fun `publish handles empty title and message`() = runTest {
        val mockResponse = createMockResponse(200, "{}")
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        val result = ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "",
            message = ""
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `publish includes Content-Type header`() = runTest {
        val mockResponse = createMockResponse(200, "{}")
        var capturedRequest: Request? = null

        whenever(mockHttpClient.newCall(any())).thenAnswer { invocation ->
            capturedRequest = invocation.getArgument(0)
            mockCall
        }
        whenever(mockCall.execute()).thenReturn(mockResponse)

        ntfyClient.publish(
            serverUrl = "https://ntfy.sh",
            topic = "test-topic",
            title = "Test",
            message = "Message"
        )

        val contentType = capturedRequest?.body?.contentType()
        assertNotNull(contentType)
        assertTrue(contentType.toString().startsWith("text/plain"))
    }

    // Helper function to create mock Response
    private fun createMockResponse(code: Int, body: String): Response {
        val mockResponseBody: ResponseBody = mock {
            on { string() }.thenReturn(body)
            on { contentType() }.thenReturn("application/json".toMediaType())
        }
        return mock {
            on { isSuccessful }.thenReturn(code in 200..299)
            on { this.code }.thenReturn(code)
            on { this.body }.thenReturn(mockResponseBody)
        }
    }
}
