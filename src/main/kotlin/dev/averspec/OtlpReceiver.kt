package dev.averspec

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lightweight OTLP HTTP receiver for testing.
 * Accepts POST /v1/traces and collects the raw JSON bodies.
 */
class OtlpReceiver(
    val port: Int = 0
) : AutoCloseable {
    private var server: HttpServer? = null
    private val receivedBodies = CopyOnWriteArrayList<String>()
    var actualPort: Int = port
        private set

    fun start(): OtlpReceiver {
        val srv = HttpServer.create(InetSocketAddress(port), 0)
        actualPort = srv.address.port

        srv.createContext("/v1/traces") { exchange ->
            if (exchange.requestMethod == "POST") {
                val contentType = exchange.requestHeaders.getFirst("Content-Type") ?: ""
                if (contentType.contains("application/json") || contentType.contains("application/x-protobuf")) {
                    val body = exchange.requestBody.bufferedReader().readText()
                    receivedBodies.add(body)
                    val response = """{"partialSuccess":{}}"""
                    exchange.sendResponseHeaders(200, response.length.toLong())
                    exchange.responseBody.write(response.toByteArray())
                    exchange.responseBody.close()
                } else {
                    exchange.sendResponseHeaders(415, 0)
                    exchange.responseBody.close()
                }
            } else {
                exchange.sendResponseHeaders(405, 0)
                exchange.responseBody.close()
            }
        }

        srv.executor = null
        srv.start()
        server = srv
        return this
    }

    fun getReceivedBodies(): List<String> = receivedBodies.toList()

    fun reset() = receivedBodies.clear()

    val endpoint: String get() = "http://localhost:$actualPort"

    override fun close() {
        server?.stop(0)
    }
}
