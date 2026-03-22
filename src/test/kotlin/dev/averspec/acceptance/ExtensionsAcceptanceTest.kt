package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test

/**
 * Port of test_extensions_acceptance.py: domain extension through itself.
 */
class ExtensionsAcceptanceTest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `extension inherits parent markers`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "ext-parent",
            actions = listOf("base_action"),
            queries = listOf("base_query"),
            assertions = listOf("base_check")
        ))
        ctx.act(AverCoreDomain.extendDomain, ExtensionSpec(
            childName = "ext-child",
            newActions = listOf("child_action"),
            newQueries = emptyList(),
            newAssertions = listOf("child_check")
        ))
        ctx.then(AverCoreDomain.extensionHasMarkers, ExtensionMarkerCheckPayload(
            parentMarkerNames = listOf("base_action", "base_query", "base_check"),
            childMarkerNames = listOf("child_action", "child_check")
        ))
    }

    @Test
    fun `extension with only new actions`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "ext-action-only",
            actions = listOf("original"),
            queries = emptyList(),
            assertions = emptyList()
        ))
        ctx.act(AverCoreDomain.extendDomain, ExtensionSpec(
            childName = "ext-action-child",
            newActions = listOf("added_one", "added_two"),
            newQueries = emptyList(),
            newAssertions = emptyList()
        ))
        ctx.then(AverCoreDomain.extensionMarkerNamesEqual, MarkerNamesCheckPayload(
            expectedNames = listOf("original", "added_one", "added_two")
        ))
    }

    @Test
    fun `extension preserves marker count`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "ext-count",
            actions = listOf("a1"),
            queries = listOf("q1"),
            assertions = listOf("c1")
        ))
        ctx.act(AverCoreDomain.extendDomain, ExtensionSpec(
            childName = "ext-count-child",
            newActions = listOf("a2"),
            newQueries = listOf("q2"),
            newAssertions = listOf("c2")
        ))
        ctx.then(AverCoreDomain.extensionMarkerCountIs, MarkerCountCheckPayload(expected = 6))
    }

    @Test
    fun `extension tracks parent`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "ext-parent-track",
            actions = emptyList(),
            queries = emptyList(),
            assertions = emptyList()
        ))
        ctx.act(AverCoreDomain.extendDomain, ExtensionSpec(
            childName = "ext-child-track",
            newActions = listOf("extra"),
            newQueries = emptyList(),
            newAssertions = emptyList()
        ))
        ctx.then(AverCoreDomain.hasParentDomain, "ext-parent-track")
    }
}
