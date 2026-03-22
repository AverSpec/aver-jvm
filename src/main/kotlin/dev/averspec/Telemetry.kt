package dev.averspec

/**
 * Declares the expected OTel span for a domain marker.
 */
data class TelemetryExpectation(
    val span: String,
    val attributes: Map<String, Any> = emptyMap()
)

/**
 * A span collected during test execution.
 */
data class CollectedSpan(
    val traceId: String,
    val spanId: String,
    val name: String,
    val attributes: Map<String, Any> = emptyMap()
)

/**
 * Result of matching a trace entry against collected spans.
 */
data class TelemetryMatchResult(
    val expected: TelemetryExpectation,
    val matched: Boolean,
    val matchedSpan: CollectedSpan? = null
)

/**
 * Collector interface for gathering spans during test execution.
 */
interface TelemetryCollector {
    fun getSpans(): List<CollectedSpan>
    fun reset()
    fun addSpan(span: CollectedSpan)
}

/**
 * In-memory telemetry collector for testing.
 */
class InMemoryCollector : TelemetryCollector {
    private val spans = mutableListOf<CollectedSpan>()

    override fun getSpans(): List<CollectedSpan> = spans.toList()
    override fun reset() = spans.clear()
    override fun addSpan(span: CollectedSpan) { spans.add(span) }
}

/**
 * Match a telemetry expectation against collected spans.
 */
fun matchTelemetry(
    expectation: TelemetryExpectation,
    collector: TelemetryCollector
): TelemetryMatchResult {
    val spans = collector.getSpans()
    val match = spans.find { span ->
        span.name == expectation.span && expectation.attributes.all { (k, v) ->
            span.attributes[k] == v
        }
    }
    return TelemetryMatchResult(
        expected = expectation,
        matched = match != null,
        matchedSpan = match
    )
}
