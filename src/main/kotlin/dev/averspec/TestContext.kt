package dev.averspec

open class TestContext(
    val domain: Domain,
    val adapter: Adapter,
    val protocolCtx: Any,
    val telemetryCollector: TelemetryCollector? = null
) {
    private val traceEntries = mutableListOf<TraceEntry>()
    private val calledMarkers = mutableSetOf<String>()

    val given = NarrativeProxy(
        domain, adapter, protocolCtx, traceEntries,
        "given", setOf(MarkerKind.ACTION, MarkerKind.ASSERTION), calledMarkers, telemetryCollector
    )
    val `when` = NarrativeProxy(
        domain, adapter, protocolCtx, traceEntries,
        "when", setOf(MarkerKind.ACTION), calledMarkers, telemetryCollector
    )
    val then = NarrativeProxy(
        domain, adapter, protocolCtx, traceEntries,
        "then", setOf(MarkerKind.ASSERTION), calledMarkers, telemetryCollector
    )
    val query = NarrativeProxy(
        domain, adapter, protocolCtx, traceEntries,
        "query", setOf(MarkerKind.QUERY), calledMarkers, telemetryCollector
    )

    open fun trace(): List<TraceEntry> = traceEntries.toList()

    fun calledMarkerNames(): Set<String> = calledMarkers.toSet()
}
