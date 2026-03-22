package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class HttpProtocolTest {

    @Test
    fun `http protocol has default name`() {
        val p = HttpProtocol(baseUrl = "http://localhost:8080")
        assertEquals("http", p.name)
    }

    @Test
    fun `http protocol accepts custom name`() {
        val p = HttpProtocol(name = "api", baseUrl = "http://localhost:8080")
        assertEquals("api", p.name)
    }

    @Test
    fun `http protocol setup returns context with base url`() {
        val p = HttpProtocol(baseUrl = "http://localhost:8080")
        val ctx = p.setup()
        assertEquals("http://localhost:8080", ctx.baseUrl)
    }

    @Test
    fun `http context has client`() {
        val ctx = HttpContext(baseUrl = "http://localhost:8080")
        assertNotNull(ctx.client)
    }
}
