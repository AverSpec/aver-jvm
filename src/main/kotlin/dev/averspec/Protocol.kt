package dev.averspec

interface Protocol<Ctx> {
    val name: String
    fun setup(): Ctx
    fun teardown(ctx: Ctx) {}
    fun onTestStart(ctx: Ctx) {}
    fun onTestEnd(ctx: Ctx) {}
    fun onTestFail(ctx: Ctx, error: Throwable) {}
}

class UnitProtocol<Ctx>(
    override val name: String = "unit",
    val factory: () -> Ctx
) : Protocol<Ctx> {
    override fun setup(): Ctx = factory()
}

/**
 * Wrap a protocol with before/after hooks.
 */
fun <Ctx> withFixture(
    protocol: Protocol<Ctx>,
    beforeSetup: (() -> Unit)? = null,
    afterSetup: ((Ctx) -> Unit)? = null,
    beforeTeardown: ((Ctx) -> Unit)? = null,
    afterTeardown: (() -> Unit)? = null
): Protocol<Ctx> {
    return object : Protocol<Ctx> {
        override val name = protocol.name

        override fun setup(): Ctx {
            beforeSetup?.invoke()
            val ctx = protocol.setup()
            afterSetup?.invoke(ctx)
            return ctx
        }

        override fun teardown(ctx: Ctx) {
            beforeTeardown?.invoke(ctx)
            protocol.teardown(ctx)
            afterTeardown?.invoke()
        }

        override fun onTestStart(ctx: Ctx) = protocol.onTestStart(ctx)
        override fun onTestEnd(ctx: Ctx) = protocol.onTestEnd(ctx)
        override fun onTestFail(ctx: Ctx, error: Throwable) = protocol.onTestFail(ctx, error)
    }
}
