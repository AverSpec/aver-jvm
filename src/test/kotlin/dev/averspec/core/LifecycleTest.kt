package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LifecycleTest {

    data class Ctx(val items: MutableList<String> = mutableListOf())

    @Test
    fun `onTestStart called before test`() {
        val events = mutableListOf<String>()
        val protocol = object : Protocol<Ctx> {
            override val name = "lifecycle"
            override fun setup() = Ctx()
            override fun onTestStart(ctx: Ctx) { events.add("start") }
            override fun onTestEnd(ctx: Ctx) { events.add("end") }
        }
        val d = domain("test") {}
        val adapter = implement<Ctx>(d, protocol) {}
        val s = suite(d, adapter)
        s.test("t") { _ -> events.add("body") }
        assertEquals(listOf("start", "body", "end"), events)
    }

    @Test
    fun `onTestFail called on error`() {
        var failCalled = false
        val protocol = object : Protocol<Ctx> {
            override val name = "lifecycle"
            override fun setup() = Ctx()
            override fun onTestFail(ctx: Ctx, error: Throwable) { failCalled = true }
            override fun onTestEnd(ctx: Ctx) {}
        }
        lateinit var check: AssertionMarker<Int>
        val d = domain("test") { check = assertion("check") }
        val adapter = implement<Ctx>(d, protocol) {
            onAssertion(check) { _: Ctx, _: Int -> throw AssertionError("fail") }
        }
        val s = suite(d, adapter)
        try { s.test("t") { ctx -> ctx.then(check, 1) } } catch (_: Throwable) {}
        assertTrue(failCalled)
    }

    @Test
    fun `onTestEnd called even on failure`() {
        var endCalled = false
        val protocol = object : Protocol<Ctx> {
            override val name = "lifecycle"
            override fun setup() = Ctx()
            override fun onTestFail(ctx: Ctx, error: Throwable) {}
            override fun onTestEnd(ctx: Ctx) { endCalled = true }
        }
        lateinit var check: AssertionMarker<Int>
        val d = domain("test") { check = assertion("check") }
        val adapter = implement<Ctx>(d, protocol) {
            onAssertion(check) { _: Ctx, _: Int -> throw AssertionError("fail") }
        }
        val s = suite(d, adapter)
        try { s.test("t") { ctx -> ctx.then(check, 1) } } catch (_: Throwable) {}
        assertTrue(endCalled)
    }

    @Test
    fun `teardown ERROR mode propagates exception`() {
        val protocol = object : Protocol<Ctx> {
            override val name = "lifecycle"
            override fun setup() = Ctx()
            override fun teardown(ctx: Ctx) { throw RuntimeException("teardown boom") }
        }
        val d = domain("test") {}
        val adapter = implement<Ctx>(d, protocol) {}
        val s = suite(d, adapter, teardownFailureMode = TeardownFailureMode.ERROR)
        assertThrows(RuntimeException::class.java) {
            s.test("t") { _ -> }
        }
    }

    @Test
    fun `teardown WARN mode does not propagate`() {
        val protocol = object : Protocol<Ctx> {
            override val name = "lifecycle"
            override fun setup() = Ctx()
            override fun teardown(ctx: Ctx) { throw RuntimeException("teardown boom") }
        }
        val d = domain("test") {}
        val adapter = implement<Ctx>(d, protocol) {}
        val s = suite(d, adapter, teardownFailureMode = TeardownFailureMode.WARN)
        assertDoesNotThrow {
            s.test("t") { _ -> }
        }
    }

    @Test
    fun `teardown SILENT mode swallows exception`() {
        val protocol = object : Protocol<Ctx> {
            override val name = "lifecycle"
            override fun setup() = Ctx()
            override fun teardown(ctx: Ctx) { throw RuntimeException("teardown boom") }
        }
        val d = domain("test") {}
        val adapter = implement<Ctx>(d, protocol) {}
        val s = suite(d, adapter, teardownFailureMode = TeardownFailureMode.SILENT)
        assertDoesNotThrow {
            s.test("t") { _ -> }
        }
    }
}
