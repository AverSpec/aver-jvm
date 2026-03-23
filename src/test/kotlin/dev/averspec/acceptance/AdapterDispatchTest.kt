package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test

/**
 * Port of test_adapter_dispatch.py: suite proxy dispatches operations correctly.
 */
class AdapterDispatchTest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `dispatches actions through suite proxy`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "dispatch-action",
            actions = listOf("submit_order"),
            queries = emptyList(),
            assertions = emptyList()
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.When(AverCoreDomain.callOperation, OperationCall(markerName = "submit_order", payload = "order-1"))
        ctx.Then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 1))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(
            index = 0, kind = "action", category = "when", status = "pass"
        ))
    }

    @Test
    fun `dispatches queries and returns typed results`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "dispatch-query",
            actions = emptyList(),
            queries = listOf("get_status"),
            assertions = emptyList()
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.When(AverCoreDomain.callOperation, OperationCall(markerName = "get_status", payload = "item-1"))
        ctx.Then(AverCoreDomain.queryReturnedValue, QueryResultCheck(
            markerName = "get_status", expected = "result-get_status"
        ))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(
            index = 0, kind = "query", category = "query", status = "pass"
        ))
    }

    @Test
    fun `dispatches assertions through suite proxy`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "dispatch-assertion",
            actions = emptyList(),
            queries = emptyList(),
            assertions = listOf("is_valid")
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.When(AverCoreDomain.callOperation, OperationCall(markerName = "is_valid", payload = "check"))
        ctx.Then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 1))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(
            index = 0, kind = "assertion", category = "then", status = "pass"
        ))
    }

    @Test
    fun `failing assertion with no prior trace`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "dispatch-fail-only",
            actions = emptyList(),
            queries = emptyList(),
            assertions = listOf("must_pass")
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.When(AverCoreDomain.executeFailingAssertion, FailingAssertionSpec(
            markerName = "must_pass", payload = "nope"
        ))
        ctx.Then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 1))
        ctx.Then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(
            index = 0, kind = "assertion", category = "then", status = "fail"
        ))
    }

    @Test
    fun `multiple adapters registered for same domain`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "multi-adapter",
            actions = listOf("do_work"),
            queries = emptyList(),
            assertions = emptyList()
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec(protocolName = "unit"))
        ctx.When(AverCoreDomain.registerSecondAdapter, AdapterSpec(protocolName = "http"))
        ctx.Then(AverCoreDomain.adapterCountIs, 2)
    }

    @Test
    fun `query returns typed result value`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "dispatch-typed-query",
            actions = emptyList(),
            queries = listOf("get_count"),
            assertions = emptyList()
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.When(AverCoreDomain.callOperation, OperationCall(markerName = "get_count", payload = "x"))
        ctx.Then(AverCoreDomain.queryReturnedValue, QueryResultCheck(
            markerName = "get_count", expected = "result-get_count"
        ))
        ctx.Then(AverCoreDomain.queryResultTypeIs, QueryResultTypeCheckPayload(
            markerName = "get_count", expectedType = "str"
        ))
    }

    @Test
    fun `parent chain lookup finds parent adapter`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineDomain, DomainSpec(
            name = "parent-chain-dispatch",
            actions = listOf("base_op"),
            queries = emptyList(),
            assertions = emptyList()
        ))
        ctx.Given(AverCoreDomain.createAdapter, AdapterSpec())
        ctx.When(AverCoreDomain.extendDomain, ExtensionSpec(
            childName = "child-chain-dispatch",
            newActions = listOf("child_op"),
            newQueries = emptyList(),
            newAssertions = emptyList()
        ))
        ctx.Then(AverCoreDomain.hasParentDomain, "parent-chain-dispatch")
    }
}
