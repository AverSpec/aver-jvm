package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OtlpReceiverTest {

    @Test
    fun `receiver starts and stops`() {
        val receiver = OtlpReceiver(port = 0).start()
        assertTrue(receiver.actualPort > 0)
        receiver.close()
    }

    @Test
    fun `receiver accepts json traces`() {
        val receiver = OtlpReceiver(port = 0).start()
        try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${receiver.endpoint}/v1/traces"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""{"resourceSpans":[]}"""))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            assertEquals(200, response.statusCode())
            assertEquals(1, receiver.getReceivedBodies().size)
        } finally {
            receiver.close()
        }
    }

    @Test
    fun `receiver rejects unsupported content type`() {
        val receiver = OtlpReceiver(port = 0).start()
        try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${receiver.endpoint}/v1/traces"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("hello"))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            assertEquals(415, response.statusCode())
        } finally {
            receiver.close()
        }
    }

    @Test
    fun `receiver reset clears bodies`() {
        val receiver = OtlpReceiver(port = 0).start()
        try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${receiver.endpoint}/v1/traces"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build()
            client.send(request, HttpResponse.BodyHandlers.ofString())
            assertEquals(1, receiver.getReceivedBodies().size)
            receiver.reset()
            assertTrue(receiver.getReceivedBodies().isEmpty())
        } finally {
            receiver.close()
        }
    }

    @Test
    fun `receiver endpoint returns correct url`() {
        val receiver = OtlpReceiver(port = 0).start()
        try {
            assertTrue(receiver.endpoint.startsWith("http://localhost:"))
        } finally {
            receiver.close()
        }
    }
}
