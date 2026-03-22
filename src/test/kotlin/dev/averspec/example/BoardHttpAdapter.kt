package dev.averspec.example

import dev.averspec.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * HTTP adapter context: holds the server and an HTTP client.
 */
data class HttpBoardCtx(
    val server: BoardHttpServer,
    val client: HttpClient = HttpClient.newHttpClient(),
    var lastCreatedCardId: String? = null
) {
    val baseUrl: String get() = "http://localhost:${server.actualPort}"

    fun get(path: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .GET()
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    fun post(path: String, body: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    fun put(path: String, body: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    fun delete(path: String): Int {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .DELETE()
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()
    }
}

/**
 * HTTP adapter: exercises the Board through its HTTP API.
 */
fun buildBoardHttpAdapter(): Adapter {
    val dom = BoardDomain
    val protocol = object : Protocol<HttpBoardCtx> {
        override val name = "http"
        override fun setup(): HttpBoardCtx {
            val server = BoardHttpServer(port = 0).start()
            return HttpBoardCtx(server = server)
        }
        override fun teardown(ctx: HttpBoardCtx) {
            ctx.server.close()
        }
    }
    return implement<HttpBoardCtx>(dom.d, protocol) {
        onAction(dom.addCard) { ctx: HttpBoardCtx, title: String ->
            val response = ctx.post("/cards", """{"title":"$title"}""")
            val id = extractField(response, "id")
            ctx.lastCreatedCardId = id
        }
        onAction(dom.moveCard) { ctx: HttpBoardCtx, payload: MoveCardPayload ->
            ctx.put("/cards/${payload.cardId}/move", """{"column":"${payload.column}"}""")
        }
        onAction(dom.removeCard) { ctx: HttpBoardCtx, cardId: String ->
            ctx.delete("/cards/$cardId")
        }
        onQuery(dom.getCard) { ctx: HttpBoardCtx, cardId: String ->
            val response = ctx.get("/cards/$cardId")
            if (response.isBlank()) null
            else parseCard(response)
        }
        onQuery(dom.listCards) { ctx: HttpBoardCtx, _: Unit ->
            val response = ctx.get("/cards")
            parseCardList(response)
        }
        onQuery(dom.columnCount) { ctx: HttpBoardCtx, column: String ->
            val response = ctx.get("/columns/$column/count")
            val countStr = extractField(response, "count")
            countStr.toInt()
        }
        onAssertion(dom.hasTotalCards) { ctx: HttpBoardCtx, expected: Int ->
            val response = ctx.get("/cards")
            val cards = parseCardList(response)
            if (cards.size != expected) {
                throw AssertionError("Expected $expected total cards, got ${cards.size}")
            }
        }
        onAssertion(dom.cardIsInColumn) { ctx: HttpBoardCtx, check: CardColumnCheck ->
            val response = ctx.get("/cards/${check.cardId}")
            val card = parseCard(response)
            if (card.column != check.column) {
                throw AssertionError("Card '${check.cardId}' is in '${card.column}', expected '${check.column}'")
            }
        }
    }
}

private fun extractField(json: String, field: String): String {
    val pattern = Regex(""""$field"\s*:\s*"?([^",}\]]+)"?""")
    return pattern.find(json)?.groupValues?.get(1) ?: ""
}

private fun parseCard(json: String): Card {
    val id = extractField(json, "id")
    val title = extractField(json, "title")
    val column = extractField(json, "column")
    return Card(id = id, title = title, column = column)
}

private fun parseCardList(json: String): List<Card> {
    if (json.trim() == "[]") return emptyList()
    // Split by },{ pattern
    val stripped = json.trim().removePrefix("[").removeSuffix("]")
    if (stripped.isBlank()) return emptyList()
    return stripped.split("},").map { part ->
        val clean = part.trim().removePrefix("{").removeSuffix("}")
        parseCard("{$clean}")
    }
}
