package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Gap C: Domain filter skip acceptance test.
 * Test that AVER_DOMAIN env var causes non-matching domain's tests to be skipped.
 */
class DomainFilterAcceptanceTest {

    @Test
    fun `AVER_DOMAIN skips non-matching domain in acceptance suite`() {
        val env = mapOf("AVER_DOMAIN" to "SomeOtherDomain")
        val adapter = buildAverCoreAdapter()
        val s = Suite(AverCoreDomain.d, adapter, envLookup = { env[it] })

        var ran = false
        s.run {
            ran = true
        }
        assertFalse(ran, "test should have been skipped by AVER_DOMAIN filter")
    }

    @Test
    fun `AVER_DOMAIN runs matching domain in acceptance suite`() {
        val env = mapOf("AVER_DOMAIN" to "aver-core")
        val adapter = buildAverCoreAdapter()
        val s = Suite(AverCoreDomain.d, adapter, envLookup = { env[it] })

        var ran = false
        s.run { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "filter-run",
                actions = listOf("ping"),
                queries = emptyList(),
                assertions = emptyList()
            ))
            ran = true
        }
        assertTrue(ran, "test should have run because AVER_DOMAIN matches")
    }
}
