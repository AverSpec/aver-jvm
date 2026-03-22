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
        onAction(dom.moveCard) { board: Board, payload: MoveCardPayload ->
            board.moveCard(payload.cardId, payload.column)
        }
        onAction(dom.removeCard) { board: Board, cardId: String ->
            board.removeCard(cardId)
        }
        onQuery(dom.getCard) { board: Board, cardId: String ->
            board.getCard(cardId)
        }
        onQuery(dom.listCards) { board: Board, _: Unit ->
            board.listCards()
        }
        onQuery(dom.columnCount) { board: Board, column: String ->
            board.columnCount(column)
        }
        onAssertion(dom.hasTotalCards) { board: Board, expected: Int ->
            val actual = board.listCards().size
            if (actual != expected) throw AssertionError("Expected $expected total cards, got $actual")
        }
        onAssertion(dom.cardIsInColumn) { board: Board, check: CardColumnCheck ->
            val card = board.getCard(check.cardId) ?: throw AssertionError("Card '${check.cardId}' not found")
            if (card.column != check.column) {
                throw AssertionError("Card '${check.cardId}' is in '${card.column}', expected '${check.column}'")
            }
        }
    }
}
