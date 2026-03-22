package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test

/**
 * Gap B: Extended domain end-to-end in suite.
 * Test that an extended domain can be implemented with an adapter
 * and used in a suite to dispatch operations and record trace.
 */
class ExtendedDomainE2ETest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `extended domain end-to-end in suite`() {
        s.test("ext e2e") { ctx ->
            // Define a parent domain
            ctx.given(AverCoreDomain.defineDomain, DomainSpec(
                name = "e2e-parent",
                actions = listOf("create_task", "delete_task"),
                queries = emptyList(),
                assertions = emptyList()
            ))
            // Extend with child markers
            ctx.`when`(AverCoreDomain.extendDomain, ExtensionSpec(
                childName = "e2e-child",
                newActions = listOf("show_spinner", "hide_spinner"),
                newQueries = emptyList(),
                newAssertions = emptyList()
            ))
            // Verify total marker count (parent + child)
            ctx.then(AverCoreDomain.extensionMarkerCountIs, MarkerCountCheckPayload(expected = 4))
            // Verify parent is tracked
            ctx.then(AverCoreDomain.hasParentDomain, "e2e-parent")
            // Create adapter for the parent domain
            ctx.given(AverCoreDomain.createAdapter, AdapterSpec())
            // Call operations through the adapter
            ctx.`when`(AverCoreDomain.callOperation, OperationCall(markerName = "create_task"))
            // Verify trace recorded the operation
            ctx.then(AverCoreDomain.traceHasLength, TraceLengthCheck(expected = 1))
        }
    }
}
