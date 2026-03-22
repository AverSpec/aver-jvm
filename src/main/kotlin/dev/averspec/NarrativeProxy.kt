package dev.averspec

class NarrativeProxy(
    private val domain: Domain,
    private val adapter: Adapter,
    private val protocolCtx: Any,
    private val traceEntries: MutableList<TraceEntry>,
    private val category: String,
    private val allowedKinds: Set<MarkerKind>,
    private val calledMarkers: MutableSet<String>,
    private val telemetryCollector: TelemetryCollector? = null
) {
    operator fun <P> invoke(marker: ActionMarker<P>, payload: P) {
        require(marker.kind in allowedKinds) {
            "Cannot use ${marker.kind} marker '${marker.name}' in '$category' block"
        }
        val handler = adapter.handlers[marker.name]
            ?: throw IllegalStateException("No handler for '${marker.name}'")
        @Suppress("UNCHECKED_CAST")
        val fn = handler as (Any, P) -> Unit
        val start = System.nanoTime()
        var status = "pass"
        var error: String? = null
        var telemetryResult: TelemetryMatchResult? = null
        try {
            fn(protocolCtx, payload)
        } catch (e: Throwable) {
            status = "fail"
            error = e.message
            throw e
        } finally {
            val durationMs = (System.nanoTime() - start) / 1_000_000.0
            if (marker.telemetry != null && telemetryCollector != null) {
                telemetryResult = matchTelemetry(marker.telemetry!!, telemetryCollector)
            }
            traceEntries.add(TraceEntry(
                kind = marker.kind.name.lowercase(),
                category = category,
                name = marker.name,
                payload = payload,
                status = status,
                durationMs = durationMs,
                error = error,
                telemetry = telemetryResult
            ))
            calledMarkers.add(marker.name)
        }
    }

    operator fun <P> invoke(marker: AssertionMarker<P>, payload: P) {
        require(marker.kind in allowedKinds) {
            "Cannot use ${marker.kind} marker '${marker.name}' in '$category' block"
        }
        val handler = adapter.handlers[marker.name]
            ?: throw IllegalStateException("No handler for '${marker.name}'")
        @Suppress("UNCHECKED_CAST")
        val fn = handler as (Any, P) -> Unit
        val start = System.nanoTime()
        var status = "pass"
        var error: String? = null
        try {
            fn(protocolCtx, payload)
        } catch (e: Throwable) {
            status = "fail"
            error = e.message
            throw e
        } finally {
            val durationMs = (System.nanoTime() - start) / 1_000_000.0
            traceEntries.add(TraceEntry(
                kind = marker.kind.name.lowercase(),
                category = category,
                name = marker.name,
                payload = payload,
                status = status,
                durationMs = durationMs,
                error = error
            ))
            calledMarkers.add(marker.name)
        }
    }

    operator fun <P, R> invoke(marker: QueryMarker<P, R>, payload: P): R {
        require(marker.kind in allowedKinds) {
            "Cannot use ${marker.kind} marker '${marker.name}' in '$category' block"
        }
        val handler = adapter.handlers[marker.name]
            ?: throw IllegalStateException("No handler for '${marker.name}'")
        @Suppress("UNCHECKED_CAST")
        val fn = handler as (Any, P) -> R
        val start = System.nanoTime()
        var status = "pass"
        var error: String? = null
        var result: R? = null
        var telemetryResult: TelemetryMatchResult? = null
        try {
            result = fn(protocolCtx, payload)
            return result
        } catch (e: Throwable) {
            status = "fail"
            error = e.message
            throw e
        } finally {
            val durationMs = (System.nanoTime() - start) / 1_000_000.0
            if (marker.telemetry != null && telemetryCollector != null) {
                telemetryResult = matchTelemetry(marker.telemetry!!, telemetryCollector)
            }
            traceEntries.add(TraceEntry(
                kind = marker.kind.name.lowercase(),
                category = category,
                name = marker.name,
                payload = payload,
                status = status,
                durationMs = durationMs,
                result = result,
                error = error,
                telemetry = telemetryResult
            ))
            calledMarkers.add(marker.name)
        }
    }
}
