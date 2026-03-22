package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test

/**
 * Port of test_error_scenarios.py: clear errors on misuse.
 */
class ErrorScenariosTest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `missing marker raises error`() {
        s.test("err missing") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "err-missing",
                actions = listOf("real_action"),
                queries = emptyList(),
                assertions = emptyList()
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.then(AverCoreDomain.missingMarkerRaisesError, MissingMarkerErrorCheckPayload(
                proxyName = "when",
                markerName = "nonexistent_marker",
                expectedMatch = "nonexistent_marker"
            ))
        }
    }

    @Test
    fun `wrong proxy raises type error`() {
        s.test("err wrong proxy") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "err-wrong-proxy",
                actions = listOf("do_thing"),
                queries = emptyList(),
                assertions = listOf("verify_thing")
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.then(AverCoreDomain.proxyRejectsWrongKind, ProxyRestrictionCheck(
                proxyName = "when",
                markerName = "verify_thing"
            ))
        }
    }

    @Test
    fun `incomplete adapter detected`() {
        s.test("err incomplete") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "err-incomplete",
                actions = listOf("handle_this", "handle_that"),
                queries = listOf("fetch_status"),
                assertions = listOf("status_is_ok")
            ))
            ctx.`when`(AverCoreDomain.createAdapter, AdapterSpec())
            ctx.then(AverCoreDomain.adapterIsComplete, CompletenessCheck(
                missing = listOf("handle_that", "fetch_status")
            ))
        }
    }
}
