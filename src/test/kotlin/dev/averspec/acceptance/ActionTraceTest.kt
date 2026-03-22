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
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "trace-full",
            actions = listOf("setup_data"),
            queries = listOf("fetch_data"),
            assertions = listOf("data_valid")
        ))
        ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.act(AverCoreDomain.callOperation, OperationCall(markerName = "setup_data", payload = "seed"))
        ctx.act(AverCoreDomain.callOperation, OperationCall(markerName = "fetch_data", payload = "key"))
        ctx.act(AverCoreDomain.callOperation, OperationCall(markerName = "data_valid", payload = "check"))
        ctx.then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 3))
        ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 0, kind = "action", category = "when", status = "pass"))
        ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 1, kind = "query", category = "query", status = "pass"))
        ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 2, kind = "assertion", category = "then", status = "pass"))
    }

    @Test
    fun `records failure status when assertion fails`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "trace-fail",
            actions = listOf("prepare"),
            queries = emptyList(),
            assertions = listOf("check_result")
        ))
        ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.act(AverCoreDomain.callOperation, OperationCall(markerName = "prepare", payload = "data"))
        ctx.act(AverCoreDomain.executeFailingAssertion, FailingAssertionSpec(
            markerName = "check_result", payload = "bad"
        ))
        ctx.then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 2))
        ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 0, kind = "action", category = "when", status = "pass"))
        ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 1, kind = "assertion", category = "then", status = "fail"))
    }

    @Test
    fun `records categorized trace with given when then`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "trace-categorized",
            actions = listOf("seed_state", "perform_action"),
            queries = emptyList(),
            assertions = listOf("verify_outcome")
        ))
        ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.act(AverCoreDomain.callThroughProxy, ProxyCall(
            proxyName = "given", markerName = "seed_state", payload = "initial"
        ))
        ctx.act(AverCoreDomain.callThroughProxy, ProxyCall(
            proxyName = "when", markerName = "perform_action", payload = "go"
        ))
        ctx.act(AverCoreDomain.callThroughProxy, ProxyCall(
            proxyName = "then", markerName = "verify_outcome", payload = "ok"
        ))
        ctx.then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 3))
        ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 0, kind = "action", category = "given", status = "pass"))
        ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 1, kind = "action", category = "when", status = "pass"))
        ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(index = 2, kind = "assertion", category = "then", status = "pass"))
    }

    @Test
    fun `trace is empty before any operations`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "trace-empty",
            actions = listOf("noop"),
            queries = emptyList(),
            assertions = emptyList()
        ))
        ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 0))
    }
}
