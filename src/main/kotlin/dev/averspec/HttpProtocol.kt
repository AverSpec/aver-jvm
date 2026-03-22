package dev.averspec

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * HTTP protocol context: holds a base URL and an HttpClient.
 */
data class HttpContext(
    val baseUrl: String,
    val client: HttpClient = HttpClient.newHttpClient()
) {
    fun get(path: String): HttpResult {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return HttpResult(response.statusCode(), response.body(), response.headers().map())
    }

    fun post(path: String, body: String, contentType: String = "application/json"): HttpResult {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return HttpResult(response.statusCode(), response.body(), response.headers().map())
    }

    fun delete(path: String): HttpResult {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .DELETE()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return HttpResult(response.statusCode(), response.body(), response.headers().map())
    }
}

data class HttpResult(
    val status: Int,
    val body: String,
    val headers: Map<String, List<String>> = emptyMap()
)

/**
 * HTTP protocol for testing against a running server.
 */
class HttpProtocol(
    override val name: String = "http",
    val baseUrl: String
) : Protocol<HttpContext> {
    override fun setup(): HttpContext = HttpContext(baseUrl)
}
