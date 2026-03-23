package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test

/**
 * Port of test_action_trace.py: trace recording across operations.
 */
class ActionTraceTest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `records complete trace across multiple operations`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "trace-full",
            actions = listOf("setup_data"),
            queries = listOf("fetch_data"),
            assertions = listOf("data_valid")
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.When(AverCoreDomain.callOperation, OperationCall(markerName = "setup_data", payload = "seed"))
        ctx.When(AverCoreDomain.callOperation, OperationCall(markerName = "fetch_data", payload = "key"))
        ctx.When(AverCoreDomain.callOperation, OperationCall(markerName = "data_valid", payload = "check"))
        ctx.Then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 3))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 0, kind = "action", category = "when", status = "pass"))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 1, kind = "query", category = "query", status = "pass"))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 2, kind = "assertion", category = "then", status = "pass"))
    }

    @Test
    fun `records failure status when assertion fails`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "trace-fail",
            actions = listOf("prepare"),
            queries = emptyList(),
            assertions = listOf("check_result")
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.When(AverCoreDomain.callOperation, OperationCall(markerName = "prepare", payload = "data"))
        ctx.When(AverCoreDomain.executeFailingAssertion, FailingAssertionSpec(
            markerName = "check_result", payload = "bad"
        ))
        ctx.Then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 2))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 0, kind = "action", category = "when", status = "pass"))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 1, kind = "assertion", category = "then", status = "fail"))
    }

    @Test
    fun `records categorized trace with given when then`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "trace-categorized",
            actions = listOf("seed_state", "perform_action"),
            queries = emptyList(),
            assertions = listOf("verify_outcome")
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.When(AverCoreDomain.callThroughProxy, ProxyCall(
            proxyName = "given", markerName = "seed_state", payload = "initial"
        ))
        ctx.When(AverCoreDomain.callThroughProxy, ProxyCall(
            proxyName = "when", markerName = "perform_action", payload = "go"
        ))
        ctx.When(AverCoreDomain.callThroughProxy, ProxyCall(
            proxyName = "then", markerName = "verify_outcome", payload = "ok"
        ))
        ctx.Then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 3))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 0, kind = "action", category = "given", status = "pass"))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 1, kind = "action", category = "when", status = "pass"))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 2, kind = "assertion", category = "then", status = "pass"))
    }

    @Test
    fun `trace is empty before any operations`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "trace-empty",
            actions = listOf("noop"),
            queries = emptyList(),
            assertions = emptyList()
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.Then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 0))
    }
}
