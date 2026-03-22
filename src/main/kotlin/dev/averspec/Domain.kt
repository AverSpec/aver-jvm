package dev.averspec

class Domain(val name: String) {
    val markers = mutableMapOf<String, Marker>()

    inline fun <reified P> action(name: String): ActionMarker<P> {
        val marker = ActionMarker<P>(name, this.name)
        markers[name] = marker
        return marker
    }

    inline fun <reified P, reified R> query(name: String): QueryMarker<P, R> {
        val marker = QueryMarker<P, R>(name, this.name)
        markers[name] = marker
        return marker
    }

    inline fun <reified P> assertion(name: String): AssertionMarker<P> {
        val marker = AssertionMarker<P>(name, this.name)
        markers[name] = marker
        return marker
    }
}

fun domain(name: String, block: Domain.() -> Unit): Domain {
    return Domain(name).apply(block)
}
