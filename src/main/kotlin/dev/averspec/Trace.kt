package dev.averspec

data class TraceEntry(
    val kind: String,
    val category: String,
    val name: String,
    val payload: Any? = null,
    val status: String = "pass",
    val durationMs: Double = 0.0,
    val result: Any? = null,
    val error: String? = null
)
