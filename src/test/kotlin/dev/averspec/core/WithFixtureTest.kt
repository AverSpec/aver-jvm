package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class WithFixtureTest {

    data class AppCtx(val items: MutableList<String> = mutableListOf())

    @Test
    fun `beforeSetup runs before protocol setup`() {
        val events = mutableListOf<String>()
        val base = UnitProtocol { events.add("setup"); AppCtx() }
        val wrapped = withFixture(base, beforeSetup = { events.add("before") })
        wrapped.setup()
        assertEquals(listOf("before", "setup"), events)
    }

    @Test
    fun `afterSetup runs after protocol setup`() {
        val events = mutableListOf<String>()
        val base = UnitProtocol { AppCtx() }
        val wrapped = withFixture(base, afterSetup = { ctx ->
            events.add("after:${ctx.items.size}")
        })
        wrapped.setup()
        assertEquals(listOf("after:0"), events)
    }

    @Test
    fun `beforeTeardown runs before protocol teardown`() {
        val events = mutableListOf<String>()
        val base = object : Protocol<AppCtx> {
            override val name = "test"
            override fun setup() = AppCtx()
            override fun teardown(ctx: AppCtx) { events.add("teardown") }
        }
        val wrapped = withFixture(base, beforeTeardown = { events.add("before-teardown") })
        val ctx = wrapped.setup()
        wrapped.teardown(ctx)
        assertEquals(listOf("before-teardown", "teardown"), events)
    }

    @Test
    fun `afterTeardown runs after protocol teardown`() {
        val events = mutableListOf<String>()
        val base = object : Protocol<AppCtx> {
            override val name = "test"
            override fun setup() = AppCtx()
            override fun teardown(ctx: AppCtx) { events.add("teardown") }
        }
        val wrapped = withFixture(base, afterTeardown = { events.add("after-teardown") })
        val ctx = wrapped.setup()
        wrapped.teardown(ctx)
        assertEquals(listOf("teardown", "after-teardown"), events)
    }

    @Test
    fun `fixture preserves protocol name`() {
        val base = UnitProtocol(name = "custom") { AppCtx() }
        val wrapped = withFixture(base)
        assertEquals("custom", wrapped.name)
    }

    @Test
    fun `all hooks compose together`() {
        val events = mutableListOf<String>()
        val base = object : Protocol<AppCtx> {
            override val name = "test"
            override fun setup(): AppCtx { events.add("setup"); return AppCtx() }
            override fun teardown(ctx: AppCtx) { events.add("teardown") }
        }
        val wrapped = withFixture(
            base,
            beforeSetup = { events.add("1-before-setup") },
            afterSetup = { events.add("2-after-setup") },
            beforeTeardown = { events.add("3-before-teardown") },
            afterTeardown = { events.add("4-after-teardown") }
        )
        val ctx = wrapped.setup()
        wrapped.teardown(ctx)
        assertEquals(
            listOf("1-before-setup", "setup", "2-after-setup", "3-before-teardown", "teardown", "4-after-teardown"),
            events
        )
    }
}
