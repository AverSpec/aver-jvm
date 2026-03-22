package dev.averspec.example

import dev.averspec.*

/**
 * Unit adapter: exercises the Board class directly.
 */
fun buildBoardUnitAdapter(): Adapter {
    val dom = BoardDomain
    val protocol = UnitProtocol { Board() }
    return implement<Board>(dom.d, protocol) {
        onAction(dom.addCard) { board: Board, title: String ->
            board.addCard(title)
        }
        onAction(dom.moveCard) { board: Board, payload: Pair<String, String> ->
            val (cardId, column) = payload
            board.moveCard(cardId, column)
        }
        onAction(dom.removeCard) { board: Board, cardId: String ->
            board.removeCard(cardId)
        }
        onQuery(dom.getCard) { board: Board, cardId: String ->
            board.getCard(cardId)
        }
        onQuery(dom.listCards) { board: Board, column: String? ->
            board.listCards(column)
        }
        onQuery(dom.columnCount) { board: Board, column: String ->
            board.columnCount(column)
        }
        onAssertion(dom.hasTotalCards) { board: Board, expected: Int ->
            val actual = board.listCards().size
            if (actual != expected) throw AssertionError("Expected $expected total cards, got $actual")
        }
        onAssertion(dom.cardIsInColumn) { board: Board, payload: Pair<String, String> ->
            val (cardId, expectedColumn) = payload
            val card = board.getCard(cardId) ?: throw AssertionError("Card '$cardId' not found")
            if (card.column != expectedColumn) {
                throw AssertionError("Card '$cardId' is in '${card.column}', expected '$expectedColumn'")
            }
        }
    }
}
