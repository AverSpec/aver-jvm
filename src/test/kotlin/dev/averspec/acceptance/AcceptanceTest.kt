package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test

/**
 * Self-dogfood tests: aver-jvm tests itself through its own domain.
 * All assertions go through ctx.then, never raw assertEquals.
 */
class SelfTest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `domain collects markers`() {
        s.test("domain collects markers") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "test-domain",
                actions = listOf("do_thing"),
                queries = listOf("get_thing"),
                assertions = listOf("check_thing")
            ))
            ctx.then(AverCoreDomain.domainHasMarker, MarkerCheck(name = "do_thing", kind = "action"))
            ctx.then(AverCoreDomain.domainHasMarker, MarkerCheck(name = "get_thing", kind = "query"))
            ctx.then(AverCoreDomain.domainHasMarker, MarkerCheck(name = "check_thing", kind = "assertion"))
        }
    }

    @Test
    fun `domain markers queryable`() {
        s.test("domain markers queryable") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "queryable",
                actions = listOf("create", "update"),
                queries = emptyList(),
                assertions = listOf("exists")
            ))
            ctx.then(AverCoreDomain.markersHaveNames, MarkerNamesCheckPayload(
                expectedNames = listOf("create", "update", "exists")
            ))
        }
    }

    @Test
    fun `complete adapter builds`() {
        s.test("complete adapter builds") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "complete",
                actions = listOf("act_a"),
                queries = listOf("query_a"),
                assertions = listOf("assert_a")
            ))
            ctx.`when`(AverCoreDomain.createAdapter, AdapterSpec(protocolName = "unit"))
            ctx.then(AverCoreDomain.adapterIsComplete, CompletenessCheck(missing = emptyList()))
        }
    }

    @Test
    fun `incomplete adapter reports missing`() {
        s.test("incomplete adapter reports missing") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "incomplete",
                actions = listOf("act_a", "act_b"),
                queries = emptyList(),
                assertions = listOf("assert_a")
            ))
            ctx.`when`(AverCoreDomain.createAdapter, AdapterSpec(protocolName = "unit"))
            ctx.then(AverCoreDomain.adapterIsComplete, CompletenessCheck(missing = listOf("act_b")))
        }
    }

    @Test
    fun `action dispatch records trace`() {
        s.test("action dispatch records trace") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "traced",
                actions = listOf("do_thing"),
                queries = emptyList(),
                assertions = emptyList()
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.`when`(AverCoreDomain.callOperation, OperationCall(markerName = "do_thing", payload = "hello"))
            ctx.then(AverCoreDomain.traceHasEntry, TraceCheck(index = 0, category = "when", status = "pass"))
        }
    }

    @Test
    fun `multiple operations build trace`() {
        s.test("multiple operations build trace") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "multi-trace",
                actions = listOf("step_one", "step_two"),
                queries = emptyList(),
                assertions = emptyList()
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.`when`(AverCoreDomain.callOperation, OperationCall(markerName = "step_one"))
            ctx.`when`(AverCoreDomain.callOperation, OperationCall(markerName = "step_two"))
            ctx.then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 2))
            ctx.then(AverCoreDomain.traceNameAtIndex, TraceNameCheckPayload(index = 0, expectedName = "multi-trace.step_one"))
            ctx.then(AverCoreDomain.traceNameAtIndex, TraceNameCheckPayload(index = 1, expectedName = "multi-trace.step_two"))
        }
    }

    @Test
    fun `when rejects assertions`() {
        s.test("when rejects assertions") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "proxy-test",
                actions = listOf("do_thing"),
                queries = emptyList(),
                assertions = listOf("check_thing")
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.then(AverCoreDomain.proxyRejectsWrongKind, ProxyRestrictionCheck(
                proxyName = "when",
                markerName = "check_thing"
            ))
        }
    }

    @Test
    fun `then rejects actions`() {
        s.test("then rejects actions") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "proxy-test-2",
                actions = listOf("do_thing"),
                queries = emptyList(),
                assertions = listOf("check_thing")
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.then(AverCoreDomain.proxyRejectsWrongKind, ProxyRestrictionCheck(
                proxyName = "then",
                markerName = "do_thing"
            ))
        }
    }

    @Test
    fun `query rejects actions`() {
        s.test("query rejects actions") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "proxy-test-3",
                actions = listOf("do_thing"),
                queries = listOf("get_thing"),
                assertions = emptyList()
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.then(AverCoreDomain.proxyRejectsWrongKind, ProxyRestrictionCheck(
                proxyName = "query",
                markerName = "do_thing"
            ))
        }
    }
}
