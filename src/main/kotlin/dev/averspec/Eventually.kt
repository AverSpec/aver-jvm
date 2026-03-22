package dev.averspec

fun <T> eventually(
    timeoutMs: Long = 5000,
    intervalMs: Long = 100,
    block: () -> T
): T {
    val deadline = System.currentTimeMillis() + timeoutMs
    var lastError: Throwable? = null
    while (System.currentTimeMillis() < deadline) {
        try {
            return block()
        } catch (e: Throwable) {
            lastError = e
            Thread.sleep(intervalMs)
        }
    }
    throw AssertionError("Timed out after ${timeoutMs}ms", lastError)
}
