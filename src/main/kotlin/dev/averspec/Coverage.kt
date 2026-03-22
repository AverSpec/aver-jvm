package dev.averspec

data class CoverageReport(
    val total: Int,
    val covered: Int,
    val missing: Set<String>,
    val percentage: Double,
    val breakdown: CoverageBreakdown? = null
) {
    val complete: Boolean get() = missing.isEmpty()
}

data class CoverageBreakdown(
    val actionsCalled: Int,
    val actionsTotal: Int,
    val queriesCalled: Int,
    val queriesTotalCount: Int,
    val assertionsCalled: Int,
    val assertionsTotal: Int
)

fun checkCoverage(domain: Domain, calledMarkers: Set<String>): CoverageReport {
    val allMarkers = domain.markers.keys
    val missing = allMarkers - calledMarkers
    val covered = allMarkers.size - missing.size
    val percentage = if (allMarkers.isEmpty()) 100.0 else (covered.toDouble() / allMarkers.size) * 100.0

    // Per-kind breakdown
    val actionMarkers = domain.markers.filter { it.value.kind == MarkerKind.ACTION }.keys
    val queryMarkers = domain.markers.filter { it.value.kind == MarkerKind.QUERY }.keys
    val assertionMarkers = domain.markers.filter { it.value.kind == MarkerKind.ASSERTION }.keys

    val breakdown = CoverageBreakdown(
        actionsCalled = (actionMarkers intersect calledMarkers).size,
        actionsTotal = actionMarkers.size,
        queriesCalled = (queryMarkers intersect calledMarkers).size,
        queriesTotalCount = queryMarkers.size,
        assertionsCalled = (assertionMarkers intersect calledMarkers).size,
        assertionsTotal = assertionMarkers.size
    )

    return CoverageReport(
        total = allMarkers.size,
        covered = covered,
        missing = missing,
        percentage = percentage,
        breakdown = breakdown
    )
}
