package dev.averspec

class Suite(
    val domain: Domain,
    val adapter: Adapter,
    val teardownFailureMode: TeardownFailureMode = TeardownFailureMode.WARN
) {
    fun test(name: String, block: (TestContext) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val protocol = adapter.protocol as Protocol<Any>
        val ctx = protocol.setup()
        val testCtx = TestContext(domain, adapter, ctx)
        protocol.onTestStart(ctx)
        try {
            block(testCtx)
            protocol.onTestEnd(ctx)
        } catch (e: Throwable) {
            protocol.onTestFail(ctx, e)
            protocol.onTestEnd(ctx)
            throw enhanceError(e, testCtx.trace())
        } finally {
            try {
                protocol.teardown(ctx)
            } catch (teardownError: Throwable) {
                when (teardownFailureMode) {
                    TeardownFailureMode.ERROR -> throw teardownError
                    TeardownFailureMode.WARN -> System.err.println("Teardown warning: ${teardownError.message}")
                    TeardownFailureMode.SILENT -> { /* swallow */ }
                }
            }
        }
    }
}

/**
 * Enhance an error message with the test trace for debugging.
 */
private fun enhanceError(original: Throwable, trace: List<TraceEntry>): Throwable {
    if (trace.isEmpty()) return original
    val traceStr = formatTrace(trace, includeTelemetry = true)
    val enhanced = AssertionError(
        "${original.message}\n\nTest steps:\n$traceStr",
        original
    )
    enhanced.stackTrace = original.stackTrace
    return enhanced
}

fun suite(
    domain: Domain,
    adapter: Adapter,
    teardownFailureMode: TeardownFailureMode = TeardownFailureMode.WARN
): Suite {
    return Suite(domain, adapter, teardownFailureMode)
}
