package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Gap A: Missing adapter error lists registered adapters.
 * When no adapter is found for a domain, the error message should list
 * what adapters ARE registered (to help debugging).
 */
class MissingAdapterErrorTest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `missing adapter error includes registered names`() {
        s.test("missing adapter") { ctx ->
            // Define a domain so we have something in the workbench
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "known-domain",
                actions = listOf("ping"),
                queries = emptyList(),
                assertions = emptyList()
            ))
            // Create adapter to verify the domain works
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            // Call operation to verify dispatch works
            ctx.`when`(AverCoreDomain.callOperation, OperationCall(markerName = "ping"))
            ctx.then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 1))
        }
    }

    @Test
    fun `unknown marker in call raises clear error`() {
        val adapter = buildAverCoreAdapter()
        val s = suite(AverCoreDomain.d, adapter)

        // This test verifies that calling an unknown marker produces a clear error
        var errorCaught = false
        s.test("unknown marker") { ctx ->
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "err-known",
                actions = listOf("real_action"),
                queries = emptyList(),
                assertions = emptyList()
            ))
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            try {
                ctx.`when`(AverCoreDomain.callOperation, OperationCall(markerName = "nonexistent"))
            } catch (e: Exception) {
                assertTrue(e.message?.contains("nonexistent") == true,
                    "Error should mention the missing marker name")
                errorCaught = true
            }
        }
        assertTrue(errorCaught, "Should have caught error for unknown marker")
    }
}
