package dev.averspec

/**
 * Global registry for domains and adapters with snapshot/restore support.
 */
object Registry {
    private val domains = mutableMapOf<String, Domain>()
    private val adapters = mutableListOf<Adapter>()

    fun registerDomain(domain: Domain) {
        domains[domain.name] = domain
    }

    fun registerAdapter(adapter: Adapter) {
        adapters.add(adapter)
    }

    fun getDomain(name: String): Domain? = domains[name]

    fun getAdaptersForDomain(domainName: String): List<Adapter> =
        adapters.filter { it.domain.name == domainName }

    fun allDomains(): Map<String, Domain> = domains.toMap()

    fun allAdapters(): List<Adapter> = adapters.toList()

    data class Snapshot(
        val domains: Map<String, Domain>,
        val adapters: List<Adapter>
    )

    fun snapshot(): Snapshot = Snapshot(
        domains = domains.toMap(),
        adapters = adapters.toList()
    )

    fun restore(snapshot: Snapshot) {
        domains.clear()
        domains.putAll(snapshot.domains)
        adapters.clear()
        adapters.addAll(snapshot.adapters)
    }

    fun clear() {
        domains.clear()
        adapters.clear()
    }
}
