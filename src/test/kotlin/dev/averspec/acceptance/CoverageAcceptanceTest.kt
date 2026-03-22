package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test

/**
 * Port of test_coverage_acceptance.py: aver-jvm verifying its own coverage tracking.
 */
class CoverageAcceptanceTest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `empty domain has full coverage`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomainForCoverage, DomainSpec(
            name = "coverage-empty",
            actions = emptyList(),
            queries = emptyList(),
            assertions = emptyList()
        ))
        ctx.given(AverCoreDomain.createAdapterForCoverage, AdapterSpec())
        ctx.then(AverCoreDomain.coverageIs, CoverageCheckPayload(expectedPercentage = 100))
    }

    @Test
    fun `partial coverage reports correct percentage`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomainForCoverage, DomainSpec(
            name = "coverage-partial",
            actions = listOf("step_one", "step_two"),
            queries = emptyList(),
            assertions = emptyList()
        ))
        ctx.given(AverCoreDomain.createAdapterForCoverage, AdapterSpec())
        ctx.act(AverCoreDomain.callCoverageOperation, OperationCall(markerName = "step_one", payload = "go"))
        ctx.then(AverCoreDomain.coverageIs, CoverageCheckPayload(expectedPercentage = 50))
    }

    @Test
    fun `full coverage after calling all markers`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomainForCoverage, DomainSpec(
            name = "coverage-full",
            actions = listOf("act"),
            queries = listOf("ask"),
            assertions = listOf("check")
        ))
        ctx.given(AverCoreDomain.createAdapterForCoverage, AdapterSpec())
        ctx.act(AverCoreDomain.callCoverageOperation, OperationCall(markerName = "act", payload = "go"))
        ctx.act(AverCoreDomain.callCoverageOperation, OperationCall(markerName = "ask", payload = "what"))
        ctx.act(AverCoreDomain.callCoverageOperation, OperationCall(markerName = "check", payload = "ok"))
        ctx.then(AverCoreDomain.coverageIs, CoverageCheckPayload(expectedPercentage = 100))
    }

    @Test
    fun `does not double count repeated calls`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomainForCoverage, DomainSpec(
            name = "coverage-dedup",
            actions = listOf("submit"),
            queries = listOf("total"),
            assertions = listOf("valid")
        ))
        ctx.given(AverCoreDomain.createAdapterForCoverage, AdapterSpec())
        ctx.act(AverCoreDomain.callCoverageOperation, OperationCall(markerName = "submit", payload = "first"))
        ctx.act(AverCoreDomain.callCoverageOperation, OperationCall(markerName = "submit", payload = "second"))
        ctx.act(AverCoreDomain.callCoverageOperation, OperationCall(markerName = "total", payload = "check"))
        ctx.then(AverCoreDomain.coverageIs, CoverageCheckPayload(expectedPercentage = 67))
    }

    @Test
    fun `reports per kind breakdown`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomainForCoverage, DomainSpec(
            name = "coverage-breakdown",
            actions = listOf("a1", "a2"),
            queries = listOf("q1"),
            assertions = listOf("c1")
        ))
        ctx.given(AverCoreDomain.createAdapterForCoverage, AdapterSpec())
        ctx.act(AverCoreDomain.callCoverageOperation, OperationCall(markerName = "a1", payload = "go"))
        ctx.then(AverCoreDomain.coverageBreakdownMatches, CoverageBreakdownCheckPayload(
            actionsCalled = 1,
            actionsTotal = 2,
            queriesCalled = 0,
            queriesTotal = 1,
            assertionsCalled = 0,
            assertionsTotal = 1
        ))
    }
}
