package dev.averspec

interface Protocol<Ctx> {
    val name: String
    fun setup(): Ctx
    fun teardown(ctx: Ctx) {}
}

class UnitProtocol<Ctx>(
    override val name: String = "unit",
    val factory: () -> Ctx
) : Protocol<Ctx> {
    override fun setup(): Ctx = factory()
}
