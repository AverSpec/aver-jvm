package dev.averspec.example

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

/**
 * Embedded HTTP server for the task board.
 */
class BoardHttpServer(val port: Int = 0) : AutoCloseable {
    private var server: HttpServer? = null
    private val board = Board()
    var actualPort: Int = port
        private set

    fun start(): BoardHttpServer {
        val srv = HttpServer.create(InetSocketAddress(port), 0)
        actualPort = srv.address.port

        srv.createContext("/cards") { exchange ->
            when (exchange.requestMethod) {
                "GET" -> {
                    val column = exchange.requestURI.query?.let {
                        it.split("&").find { p -> p.startsWith("column=") }?.substringAfter("column=")
                    }
                    val cards = board.listCards(column)
                    val json = "[${cards.joinToString(",") { cardToJson(it) }}]"
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, json.length.toLong())
                    exchange.responseBody.write(json.toByteArray())
                    exchange.responseBody.close()
                }
                "POST" -> {
                    val body = exchange.requestBody.bufferedReader().readText()
                    val title = extractJsonField(body, "title")
                    val card = board.addCard(title)
                    val json = cardToJson(card)
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(201, json.length.toLong())
                    exchange.responseBody.write(json.toByteArray())
                    exchange.responseBody.close()
                }
                else -> {
                    exchange.sendResponseHeaders(405, 0)
                    exchange.responseBody.close()
                }
            }
        }

        srv.createContext("/cards/") { exchange ->
            val path = exchange.requestURI.path
            val cardId = path.substringAfter("/cards/").split("/")[0]

            when {
                exchange.requestMethod == "GET" -> {
                    val card = board.getCard(cardId)
                    if (card != null) {
                        val json = cardToJson(card)
                        exchange.responseHeaders.set("Content-Type", "application/json")
                        exchange.sendResponseHeaders(200, json.length.toLong())
                        exchange.responseBody.write(json.toByteArray())
                    } else {
                        exchange.sendResponseHeaders(404, 0)
                    }
                    exchange.responseBody.close()
                }
                exchange.requestMethod == "PUT" && path.endsWith("/move") -> {
                    val body = exchange.requestBody.bufferedReader().readText()
                    val column = extractJsonField(body, "column")
                    board.moveCard(cardId, column)
                    val card = board.getCard(cardId)!!
                    val json = cardToJson(card)
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, json.length.toLong())
                    exchange.responseBody.write(json.toByteArray())
                    exchange.responseBody.close()
                }
                exchange.requestMethod == "DELETE" -> {
                    try {
                        board.removeCard(cardId)
                        exchange.sendResponseHeaders(204, -1)
                    } catch (e: IllegalArgumentException) {
                        exchange.sendResponseHeaders(404, 0)
                    }
                    exchange.responseBody.close()
                }
                else -> {
                    exchange.sendResponseHeaders(405, 0)
                    exchange.responseBody.close()
                }
            }
        }

        srv.createContext("/columns/") { exchange ->
            val column = exchange.requestURI.path.substringAfter("/columns/").split("/")[0]
            when (exchange.requestMethod) {
                "GET" -> {
                    val count = board.columnCount(column)
                    val json = """{"column":"$column","count":$count}"""
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, json.length.toLong())
                    exchange.responseBody.write(json.toByteArray())
                    exchange.responseBody.close()
                }
                else -> {
                    exchange.sendResponseHeaders(405, 0)
                    exchange.responseBody.close()
                }
            }
        }

        srv.executor = null
        srv.start()
        server = srv
        return this
    }

    override fun close() {
        server?.stop(0)
    }

    private fun cardToJson(card: Card): String =
        """{"id":"${card.id}","title":"${card.title}","column":"${card.column}"}"""

    private fun extractJsonField(json: String, field: String): String {
        val pattern = Regex(""""$field"\s*:\s*"([^"]+)"""")
        return pattern.find(json)?.groupValues?.get(1) ?: ""
    }
}
