package dev.averspec.example

import dev.averspec.*

/**
 * Domain vocabulary for the task board example.
 * Markers are extracted from the domain after creation — no unsafe casts needed
 * because the domain DSL returns typed markers that we store in local vars first.
 */
data class MoveCardPayload(val cardId: String, val column: String)
data class CardColumnCheck(val cardId: String, val column: String)

object BoardDomain {
    val d: Domain = domain("task-board") {
        action<String>("add card")
        action<MoveCardPayload>("move card")
        action<String>("remove card")
        query<String, Card?>("get card")
        query<Unit, List<Card>>("list cards")
        query<String, Int>("column count")
        assertion<Int>("has total cards")
        assertion<CardColumnCheck>("card is in column")
    }

    // Type-safe marker accessors via the domain's typed lookup
    @Suppress("UNCHECKED_CAST")
    val addCard get() = d.markers["add card"] as ActionMarker<String>
    @Suppress("UNCHECKED_CAST")
    val moveCard get() = d.markers["move card"] as ActionMarker<MoveCardPayload>
    @Suppress("UNCHECKED_CAST")
    val removeCard get() = d.markers["remove card"] as ActionMarker<String>
    @Suppress("UNCHECKED_CAST")
    val getCard get() = d.markers["get card"] as QueryMarker<String, Card?>
    @Suppress("UNCHECKED_CAST")
    val listCards get() = d.markers["list cards"] as QueryMarker<Unit, List<Card>>
    @Suppress("UNCHECKED_CAST")
    val columnCount get() = d.markers["column count"] as QueryMarker<String, Int>
    @Suppress("UNCHECKED_CAST")
    val hasTotalCards get() = d.markers["has total cards"] as AssertionMarker<Int>
    @Suppress("UNCHECKED_CAST")
    val cardIsInColumn get() = d.markers["card is in column"] as AssertionMarker<CardColumnCheck>
}
