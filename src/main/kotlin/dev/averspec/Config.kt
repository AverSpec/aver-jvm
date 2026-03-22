package dev.averspec

data class AverConfig(
    val adapters: List<Adapter> = emptyList(),
    val coverage: CoverageMode = CoverageMode.WARN,
    val teardownFailureMode: TeardownFailureMode = TeardownFailureMode.WARN
)

enum class CoverageMode { OFF, WARN, ERROR }
enum class TeardownFailureMode { WARN, ERROR, SILENT }

fun defineConfig(block: ConfigBuilder.() -> Unit): AverConfig {
    return ConfigBuilder().apply(block).build()
}

class ConfigBuilder {
    private val adapters = mutableListOf<Adapter>()
    var coverage: CoverageMode = CoverageMode.WARN
    var teardownFailureMode: TeardownFailureMode = TeardownFailureMode.WARN

    fun adapter(adapter: Adapter) {
        adapters.add(adapter)
    }

    fun build(): AverConfig {
        return AverConfig(
            adapters = adapters.toList(),
            coverage = coverage,
            teardownFailureMode = teardownFailureMode
        )
    }
}
