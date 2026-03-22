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
    fun `dispatches actions through suite proxy`() {
        s.test("dispatch action") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "dispatch-action",
                actions = listOf("submit_order"),
                queries = emptyList(),
                assertions = emptyList()
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.`when`(AverCoreDomain.callOperation, OperationCall(markerName = "submit_order", payload = "order-1"))
            ctx.then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 1))
            ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(
                index = 0, kind = "action", category = "when", status = "pass"
            ))
        }
    }

    @Test
    fun `dispatches queries and returns typed results`() {
        s.test("dispatch query") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "dispatch-query",
                actions = emptyList(),
                queries = listOf("get_status"),
                assertions = emptyList()
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.`when`(AverCoreDomain.callOperation, OperationCall(markerName = "get_status", payload = "item-1"))
            ctx.then(AverCoreDomain.queryReturnedValue, QueryResultCheck(
                markerName = "get_status", expected = "result-get_status"
            ))
            ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(
                index = 0, kind = "query", category = "query", status = "pass"
            ))
        }
    }

    @Test
    fun `dispatches assertions through suite proxy`() {
        s.test("dispatch assertion") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "dispatch-assertion",
                actions = emptyList(),
                queries = emptyList(),
                assertions = listOf("is_valid")
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.`when`(AverCoreDomain.callOperation, OperationCall(markerName = "is_valid", payload = "check"))
            ctx.then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 1))
            ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(
                index = 0, kind = "assertion", category = "then", status = "pass"
            ))
        }
    }

    @Test
    fun `failing assertion with no prior trace`() {
        s.test("dispatch fail only") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "dispatch-fail-only",
                actions = emptyList(),
                queries = emptyList(),
                assertions = listOf("must_pass")
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.`when`(AverCoreDomain.executeFailingAssertion, FailingAssertionSpec(
                markerName = "must_pass", payload = "nope"
            ))
            ctx.then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 1))
            ctx.then(AverCoreDomain.traceEntryMatches, TraceEntryCheck(
                index = 0, kind = "assertion", category = "then", status = "fail"
            ))
        }
    }

    @Test
    fun `multiple adapters registered for same domain`() {
        s.test("multi adapter") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "multi-adapter",
                actions = listOf("do_work"),
                queries = emptyList(),
                assertions = emptyList()
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec(protocolName = "unit"))
            ctx.`when`(AverCoreDomain.registerSecondAdapter, AdapterSpec(protocolName = "http"))
            ctx.then(AverCoreDomain.adapterCountIs, 2)
        }
    }

    @Test
    fun `query returns typed result value`() {
        s.test("typed query") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "dispatch-typed-query",
                actions = emptyList(),
                queries = listOf("get_count"),
                assertions = emptyList()
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.`when`(AverCoreDomain.callOperation, OperationCall(markerName = "get_count", payload = "x"))
            ctx.then(AverCoreDomain.queryReturnedValue, QueryResultCheck(
                markerName = "get_count", expected = "result-get_count"
            ))
            ctx.then(AverCoreDomain.queryResultTypeIs, QueryResultTypeCheckPayload(
                markerName = "get_count", expectedType = "str"
            ))
        }
    }

    @Test
    fun `parent chain lookup finds parent adapter`() {
        s.test("parent chain") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "parent-chain-dispatch",
                actions = listOf("base_op"),
                queries = emptyList(),
                assertions = emptyList()
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.`when`(AverCoreDomain.extendDomain, ExtensionSpec(
                childName = "child-chain-dispatch",
                newActions = listOf("child_op"),
                newQueries = emptyList(),
                newAssertions = emptyList()
            ))
            ctx.then(AverCoreDomain.hasParentDomain, "parent-chain-dispatch")
        }
    }
}
