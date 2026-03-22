package dev.averspec

class Domain(val name: String, val parent: Domain? = null) {
    val markers = mutableMapOf<String, Marker>()

    init {
        // Inherit parent markers
        parent?.markers?.forEach { (k, v) -> markers[k] = v }
    }

    inline fun <reified P> action(
        name: String,
        telemetry: TelemetryExpectation? = null
    ): ActionMarker<P> {
        val marker = ActionMarker<P>(name, this.name, telemetry)
        markers[name] = marker
        return marker
    }

    inline fun <reified P, reified R> query(
        name: String,
        telemetry: TelemetryExpectation? = null
    ): QueryMarker<P, R> {
        val marker = QueryMarker<P, R>(name, this.name, telemetry)
        markers[name] = marker
        return marker
    }

    inline fun <reified P> assertion(
        name: String,
        telemetry: TelemetryExpectation? = null
    ): AssertionMarker<P> {
        val marker = AssertionMarker<P>(name, this.name, telemetry)
        markers[name] = marker
        return marker
    }

    fun extend(childName: String, block: Domain.() -> Unit): Domain {
        return Domain(childName, parent = this).apply(block)
    }
}

fun domain(name: String, block: Domain.() -> Unit): Domain {
    return Domain(name).apply(block)
}
