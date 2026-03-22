package dev.averspec

data class CoverageReport(
    val total: Int,
    val covered: Int,
    val missing: Set<String>,
    val percentage: Double
) {
    val complete: Boolean get() = missing.isEmpty()
}

fun checkCoverage(domain: Domain, calledMarkers: Set<String>): CoverageReport {
    val allMarkers = domain.markers.keys
    val missing = allMarkers - calledMarkers
    val covered = allMarkers.size - missing.size
    val percentage = if (allMarkers.isEmpty()) 100.0 else (covered.toDouble() / allMarkers.size) * 100.0
    return CoverageReport(
        total = allMarkers.size,
        covered = covered,
        missing = missing,
        percentage = percentage
    )
}
