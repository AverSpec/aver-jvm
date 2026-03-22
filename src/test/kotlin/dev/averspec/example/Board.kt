package dev.averspec.example

/**
 * Example domain model: a task board with columns and cards.
 */
data class Card(val id: String, val title: String, var column: String = "todo")

class Board {
    private val cards = mutableMapOf<String, Card>()
    private var nextId = 1

    fun addCard(title: String): Card {
        val id = "card-${nextId++}"
        val card = Card(id = id, title = title)
        cards[id] = card
        return card
    }

    fun moveCard(cardId: String, column: String) {
        val card = cards[cardId] ?: throw IllegalArgumentException("No card with id '$cardId'")
        card.column = column
    }

    fun getCard(cardId: String): Card? = cards[cardId]

    fun listCards(column: String? = null): List<Card> {
        return if (column != null) {
            cards.values.filter { it.column == column }
        } else {
            cards.values.toList()
        }
    }

    fun removeCard(cardId: String) {
        cards.remove(cardId) ?: throw IllegalArgumentException("No card with id '$cardId'")
    }

    fun columnCount(column: String): Int = cards.values.count { it.column == column }
}
