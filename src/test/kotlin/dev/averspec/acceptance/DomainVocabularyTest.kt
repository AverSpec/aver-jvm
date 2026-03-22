package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test

/**
 * Port of test_domain_vocabulary.py: domain captures and reports markers.
 */
class DomainVocabularyTest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `captures actions queries and assertions`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "vocab-full",
            actions = listOf("create_item", "delete_item"),
            queries = listOf("get_item"),
            assertions = listOf("item_exists", "item_is_deleted")
        ))
        ctx.then(AverCoreDomain.domainHasMarker, MarkerCheck(name = "create_item", kind = "action"))
        ctx.then(AverCoreDomain.domainHasMarker, MarkerCheck(name = "delete_item", kind = "action"))
        ctx.then(AverCoreDomain.domainHasMarker, MarkerCheck(name = "get_item", kind = "query"))
        ctx.then(AverCoreDomain.domainHasMarker, MarkerCheck(name = "item_exists", kind = "assertion"))
        ctx.then(AverCoreDomain.domainHasMarker, MarkerCheck(name = "item_is_deleted", kind = "assertion"))
        ctx.then(AverCoreDomain.hasVocabulary, VocabularyCheck(actions = 2, queries = 1, assertions = 2))
    }

    @Test
    fun `allows empty vocabulary`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "vocab-empty",
            actions = emptyList(),
            queries = emptyList(),
            assertions = emptyList()
        ))
        ctx.then(AverCoreDomain.hasVocabulary, VocabularyCheck(actions = 0, queries = 0, assertions = 0))
        ctx.then(AverCoreDomain.markersHaveNames, MarkerNamesCheckPayload(expectedNames = emptyList()))
    }

    @Test
    fun `markers report correct kind`() = s.run { ctx ->
        ctx.given(AverCoreDomain.defineDomain, DomainSpec(
            name = "vocab-kinds",
            actions = listOf("do_thing"),
            queries = listOf("get_thing"),
            assertions = listOf("check_thing")
        ))
        ctx.then(AverCoreDomain.markerKindsMatch, MarkerKindMapCheckPayload(expected = mapOf(
            "do_thing" to "action",
            "get_thing" to "query",
            "check_thing" to "assertion"
        )))
    }
}
