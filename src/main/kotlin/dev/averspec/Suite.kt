package dev.averspec

class Suite(val domain: Domain, val adapter: Adapter) {
    fun test(name: String, block: (TestContext) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val protocol = adapter.protocol as Protocol<Any>
        val ctx = protocol.setup()
        val testCtx = TestContext(domain, adapter, ctx)
        try {
            block(testCtx)
        } finally {
            protocol.teardown(ctx)
        }
    }
}

fun suite(domain: Domain, adapter: Adapter): Suite {
    return Suite(domain, adapter)
}
