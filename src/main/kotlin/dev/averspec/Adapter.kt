package dev.averspec

class Adapter(
    val domain: Domain,
    val protocol: Protocol<*>,
    val handlers: Map<String, Any>
)

class AdapterBuilder(val domain: Domain, val protocol: Protocol<*>) {
    val handlers = mutableMapOf<String, Any>()

    fun <Ctx, P> onAction(marker: ActionMarker<P>, handler: (Ctx, P) -> Unit) {
        handlers[marker.name] = handler as Any
    }

    fun <Ctx, P, R> onQuery(marker: QueryMarker<P, R>, handler: (Ctx, P) -> R) {
        handlers[marker.name] = handler as Any
    }

    fun <Ctx, P> onAssertion(marker: AssertionMarker<P>, handler: (Ctx, P) -> Unit) {
        handlers[marker.name] = handler as Any
    }

    fun build(): Adapter {
        val missing = domain.markers.keys - handlers.keys
        require(missing.isEmpty()) { "Missing handlers: $missing" }
        return Adapter(domain, protocol, handlers.toMap())
    }
}

fun <Ctx> implement(domain: Domain, protocol: Protocol<Ctx>, block: AdapterBuilder.() -> Unit): Adapter {
    return AdapterBuilder(domain, protocol).apply(block).build()
}

fun <Ctx> adapt(domain: Domain, protocol: Protocol<Ctx>, block: AdapterBuilder.() -> Unit): Adapter {
    return implement(domain, protocol, block)
}
