package dev.averspec.example

import dev.averspec.*

/**
 * Domain vocabulary for the task board example.
 */
object BoardDomain {
    val d = domain("task-board") {
        action<String>("add card")
        action<Pair<String, String>>("move card")
        action<String>("remove card")
        query<String, Card?>("get card")
        query<String?, List<Card>>("list cards")
        query<String, Int>("column count")
        assertion<Int>("has total cards")
        assertion<Pair<String, String>>("card is in column")
    }

    // Typed marker references
    val addCard = d.markers["add card"] as ActionMarker<String>
    val moveCard = d.markers["move card"] as ActionMarker<Pair<String, String>>
    val removeCard = d.markers["remove card"] as ActionMarker<String>
    val getCard = d.markers["get card"] as QueryMarker<String, Card?>
    val listCards = d.markers["list cards"] as QueryMarker<String?, List<Card>>
    val columnCount = d.markers["column count"] as QueryMarker<String, Int>
    val hasTotalCards = d.markers["has total cards"] as AssertionMarker<Int>
    val cardIsInColumn = d.markers["card is in column"] as AssertionMarker<Pair<String, String>>
}
