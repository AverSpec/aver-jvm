package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TraceFormatTest {

    @Test
    fun `formatTrace shows entries`() {
        val entries = listOf(
            TraceEntry(kind = "action", category = "given", name = "add item", status = "pass"),
            TraceEntry(kind = "assertion", category = "then", name = "has count", status = "pass")
        )
        val output = formatTrace(entries)
        assertTrue(output.contains("add item"))
        assertTrue(output.contains("has count"))
        assertTrue(output.contains("PASS"))
    }

    @Test
    fun `formatTrace shows failure`() {
        val entries = listOf(
            TraceEntry(kind = "assertion", category = "then", name = "check", status = "fail", error = "Expected 1 got 0")
        )
        val output = formatTrace(entries)
        assertTrue(output.contains("FAIL"))
        assertTrue(output.contains("Expected 1 got 0"))
    }

    @Test
    fun `formatTrace with telemetry annotations`() {
        val entries = listOf(
            TraceEntry(
                kind = "action", category = "when", name = "submit",
                telemetry = TelemetryMatchResult(
                    expected = TelemetryExpectation(span = "order.submit"),
                    matched = true
                )
            )
        )
        val output = formatTrace(entries, includeTelemetry = true)
        assertTrue(output.contains("order.submit"))
        assertTrue(output.contains("matched"))
    }

    @Test
    fun `formatTrace without telemetry skips annotations`() {
        val entries = listOf(
            TraceEntry(
                kind = "action", category = "when", name = "submit",
                telemetry = TelemetryMatchResult(
                    expected = TelemetryExpectation(span = "order.submit"),
                    matched = true
                )
            )
        )
        val output = formatTrace(entries, includeTelemetry = false)
        assertFalse(output.contains("order.submit"))
    }

    @Test
    fun `error enhancement includes trace in message`() {
        lateinit var addItem: ActionMarker<String>
        lateinit var hasCount: AssertionMarker<Int>
        val d = domain("test") {
            addItem = action("add item")
            hasCount = assertion("has count")
        }
        val protocol = UnitProtocol { mutableListOf<String>() }
        val adapter = implement(d, protocol) {
            onAction<MutableList<String>, String>(addItem) { ctx, text -> ctx.add(text) }
            onAssertion<MutableList<String>, Int>(hasCount) { ctx, expected ->
                if (ctx.size != expected) throw AssertionError("Expected $expected items, got ${ctx.size}")
            }
        }
        val s = suite(d, adapter)
        val err = assertThrows(Throwable::class.java) {
            s.test("t") { ctx ->
                ctx.Given(addItem, "milk")
                ctx.Then(hasCount, 99)
            }
        }
        assertTrue(err.message!!.contains("Test steps"))
        assertTrue(err.message!!.contains("add item"))
    }
}
