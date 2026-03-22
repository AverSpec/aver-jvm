package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TelemetryTest {

    @Test
    fun `telemetry expectation holds span name`() {
        val tel = TelemetryExpectation(span = "order.create")
        assertEquals("order.create", tel.span)
    }

    @Test
    fun `telemetry expectation holds attributes`() {
        val tel = TelemetryExpectation(span = "order.create", attributes = mapOf("user" to "alice"))
        assertEquals(mapOf("user" to "alice"), tel.attributes)
    }

    @Test
    fun `collected span records trace and span ids`() {
        val span = CollectedSpan(traceId = "t1", spanId = "s1", name = "order.create")
        assertEquals("t1", span.traceId)
        assertEquals("s1", span.spanId)
    }

    @Test
    fun `in-memory collector stores spans`() {
        val collector = InMemoryCollector()
        collector.addSpan(CollectedSpan("t1", "s1", "test.span"))
        assertEquals(1, collector.getSpans().size)
        assertEquals("test.span", collector.getSpans()[0].name)
    }

    @Test
    fun `in-memory collector reset clears spans`() {
        val collector = InMemoryCollector()
        collector.addSpan(CollectedSpan("t1", "s1", "test.span"))
        collector.reset()
        assertTrue(collector.getSpans().isEmpty())
    }

    @Test
    fun `match telemetry finds matching span`() {
        val collector = InMemoryCollector()
        collector.addSpan(CollectedSpan("t1", "s1", "order.create", mapOf("user" to "alice")))
        val result = matchTelemetry(
            TelemetryExpectation(span = "order.create", attributes = mapOf("user" to "alice")),
            collector
        )
        assertTrue(result.matched)
        assertNotNull(result.matchedSpan)
    }

    @Test
    fun `match telemetry returns false on missing span`() {
        val collector = InMemoryCollector()
        val result = matchTelemetry(
            TelemetryExpectation(span = "order.create"),
            collector
        )
        assertFalse(result.matched)
        assertNull(result.matchedSpan)
    }

    @Test
    fun `match telemetry returns false on attribute mismatch`() {
        val collector = InMemoryCollector()
        collector.addSpan(CollectedSpan("t1", "s1", "order.create", mapOf("user" to "bob")))
        val result = matchTelemetry(
            TelemetryExpectation(span = "order.create", attributes = mapOf("user" to "alice")),
            collector
        )
        assertFalse(result.matched)
    }

    @Test
    fun `marker carries telemetry expectation`() {
        val d = domain("test") {
            action<String>("submit", telemetry = TelemetryExpectation(span = "order.submit"))
        }
        val marker = d.markers["submit"]!!
        assertNotNull(marker.telemetry)
        assertEquals("order.submit", marker.telemetry!!.span)
    }

    @Test
    fun `telemetry match result carries expected span`() {
        val result = TelemetryMatchResult(
            expected = TelemetryExpectation(span = "order.create"),
            matched = true
        )
        assertEquals("order.create", result.expected.span)
    }

    @Test
    fun `trace entry carries telemetry result`() {
        val telResult = TelemetryMatchResult(
            expected = TelemetryExpectation(span = "order.create"),
            matched = true
        )
        val entry = TraceEntry(
            kind = "action",
            category = "when",
            name = "submit",
            telemetry = telResult
        )
        assertNotNull(entry.telemetry)
        assertTrue(entry.telemetry!!.matched)
    }

    @Test
    fun `telemetry matching through narrative proxy`() {
        val collector = InMemoryCollector()
        lateinit var submit: ActionMarker<String>
        val d = domain("test") {
            submit = action("submit", telemetry = TelemetryExpectation(span = "order.submit"))
        }
        val protocol = UnitProtocol { mutableListOf<String>() }
        val adapter = implement(d, protocol) {
            onAction<MutableList<String>, String>(submit) { ctx, payload ->
                ctx.add(payload)
                collector.addSpan(CollectedSpan("t1", "s1", "order.submit"))
            }
        }
        val s = suite(d, adapter)
        s.test("tel proxy") { ctx ->
            // Need a context with collector
            val telCtx = TestContext(d, adapter, protocol.setup(), collector)
            telCtx.act(submit, "order-1")
            val trace = telCtx.trace()
            assertEquals(1, trace.size)
            assertNotNull(trace[0].telemetry)
            assertTrue(trace[0].telemetry!!.matched)
        }
    }
}
