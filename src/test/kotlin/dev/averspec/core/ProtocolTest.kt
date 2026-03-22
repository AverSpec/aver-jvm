package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ProtocolTest {

    data class AppCtx(val items: MutableList<String> = mutableListOf())

    @Test
    fun `unit protocol has default name`() {
        val p = UnitProtocol { AppCtx() }
        assertEquals("unit", p.name)
    }

    @Test
    fun `unit protocol accepts custom name`() {
        val p = UnitProtocol(name = "custom") { AppCtx() }
        assertEquals("custom", p.name)
    }

    @Test
    fun `setup returns fresh context each time`() {
        val p = UnitProtocol { AppCtx() }
        val a = p.setup()
        val b = p.setup()
        assertNotSame(a, b)
    }

    @Test
    fun `teardown is callable`() {
        val p = UnitProtocol { AppCtx() }
        val ctx = p.setup()
        assertDoesNotThrow { p.teardown(ctx) }
    }

    @Test
    fun `custom protocol with teardown`() {
        var tornDown = false
        val p = object : Protocol<AppCtx> {
            override val name = "custom"
            override fun setup() = AppCtx()
            override fun teardown(ctx: AppCtx) { tornDown = true }
        }
        val ctx = p.setup()
        p.teardown(ctx)
        assertTrue(tornDown)
    }
}
