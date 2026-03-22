package dev.averspec

data class TraceEntry(
    val kind: String,
    val category: String,
    val name: String,
    val payload: Any? = null,
    val status: String = "pass",
    val durationMs: Double = 0.0,
    val result: Any? = null,
    val error: String? = null,
    val telemetry: TelemetryMatchResult? = null
)

/**
 * Format a trace for display, optionally including telemetry annotations.
 */
fun formatTrace(entries: List<TraceEntry>, includeTelemetry: Boolean = false): String {
    val sb = StringBuilder()
    entries.forEachIndexed { i, entry ->
        val status = if (entry.status == "pass") "PASS" else "FAIL"
        sb.append("${i + 1}. [${entry.category}] ${entry.name} ($status)")
        if (entry.durationMs > 0) {
            sb.append(" ${String.format("%.1f", entry.durationMs)}ms")
        }
        if (includeTelemetry && entry.telemetry != null) {
            val tel = entry.telemetry
            val matchStr = if (tel.matched) "matched" else "MISSING"
            sb.append(" | telemetry: ${tel.expected.span} ($matchStr)")
        }
        if (entry.error != null) {
            sb.append("\n   Error: ${entry.error}")
        }
        sb.appendLine()
    }
    return sb.toString()
}
