package dev.averspec

import java.io.File

typealias Scrubber = (String) -> String

/**
 * Built-in scrubbers for non-deterministic values.
 */
object Scrubbers {
    val UUID: Scrubber = { text ->
        text.replace(
            Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"),
            "<UUID>"
        )
    }

    val TIMESTAMP: Scrubber = { text ->
        text.replace(
            Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})?"),
            "<TIMESTAMP>"
        )
    }

    fun combine(vararg scrubbers: Scrubber): Scrubber = { text ->
        scrubbers.fold(text) { acc, scrub -> scrub(acc) }
    }
}

/**
 * Approval testing: compare actual output against an approved snapshot.
 */
fun approve(
    actual: String,
    approvalFile: File,
    scrub: Scrubber? = null
) {
    val scrubbed = if (scrub != null) scrub(actual) else actual

    if (!approvalFile.exists()) {
        // First run: write the received file and fail
        val receivedFile = File(approvalFile.parentFile, approvalFile.name.replace(".approved", ".received"))
        receivedFile.writeText(scrubbed)
        throw AssertionError(
            "No approved file found. Received output written to ${receivedFile.absolutePath}. " +
            "Review and rename to ${approvalFile.name} to approve."
        )
    }

    val approved = approvalFile.readText()
    if (scrubbed != approved) {
        val receivedFile = File(approvalFile.parentFile, approvalFile.name.replace(".approved", ".received"))
        receivedFile.writeText(scrubbed)
        throw AssertionError(
            "Approval mismatch.\n" +
            "Approved: ${approvalFile.absolutePath}\n" +
            "Received: ${receivedFile.absolutePath}\n" +
            "Diff:\n${diffText(approved, scrubbed)}"
        )
    }
}

/** Alias for approve(). */
fun characterize(
    actual: String,
    approvalFile: File,
    scrub: Scrubber? = null
) = approve(actual, approvalFile, scrub)

private fun diffText(expected: String, actual: String): String {
    val expLines = expected.lines()
    val actLines = actual.lines()
    val sb = StringBuilder()
    val maxLines = maxOf(expLines.size, actLines.size)
    for (i in 0 until maxLines) {
        val exp = expLines.getOrNull(i) ?: ""
        val act = actLines.getOrNull(i) ?: ""
        if (exp != act) {
            sb.appendLine("- $exp")
            sb.appendLine("+ $act")
        }
    }
    return sb.toString()
}
