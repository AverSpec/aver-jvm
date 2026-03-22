package dev.averspec

class TestContext(
    val domain: Domain,
    val adapter: Adapter,
    val protocolCtx: Any
) {
    private val traceEntries = mutableListOf<TraceEntry>()
    private val calledMarkers = mutableSetOf<String>()

    val given = NarrativeProxy(
        domain, adapter, protocolCtx, traceEntries,
        "given", setOf(MarkerKind.ACTION, MarkerKind.ASSERTION), calledMarkers
    )
    val `when` = NarrativeProxy(
        domain, adapter, protocolCtx, traceEntries,
        "when", setOf(MarkerKind.ACTION), calledMarkers
    )
    val then = NarrativeProxy(
        domain, adapter, protocolCtx, traceEntries,
        "then", setOf(MarkerKind.ASSERTION), calledMarkers
    )
    val query = NarrativeProxy(
        domain, adapter, protocolCtx, traceEntries,
        "query", setOf(MarkerKind.QUERY), calledMarkers
    )

    fun trace(): List<TraceEntry> = traceEntries.toList()

    fun calledMarkerNames(): Set<String> = calledMarkers.toSet()
}
