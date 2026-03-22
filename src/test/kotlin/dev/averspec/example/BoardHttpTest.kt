package dev.averspec.example

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Task board tests through the HTTP adapter.
 * Same tests as unit, different protocol.
 */
class BoardHttpTest {

    private val adapter = buildBoardHttpAdapter()
    private val s = suite(BoardDomain.d, adapter)

    @Test
    fun `add a card via http`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Buy groceries")
        ctx.then(BoardDomain.hasTotalCards, 1)
    }

    @Test
    fun `new card starts in todo via http`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Write tests")
        val cards = ctx.query(BoardDomain.listCards)
        val card = cards.first()
        ctx.then(BoardDomain.cardIsInColumn, CardColumnCheck(card.id, "todo"))
    }

    @Test
    fun `move card via http`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Deploy service")
        val cards = ctx.query(BoardDomain.listCards)
        val card = cards.first()
        ctx.act(BoardDomain.moveCard, MoveCardPayload(card.id, "doing"))
        ctx.then(BoardDomain.cardIsInColumn, CardColumnCheck(card.id, "doing"))
    }

    @Test
    fun `remove card via http`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Temp task")
        val cards = ctx.query(BoardDomain.listCards)
        ctx.act(BoardDomain.removeCard, cards.first().id)
        ctx.then(BoardDomain.hasTotalCards, 0)
    }

    @Test
    fun `query card by id via http`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Important task")
        val cards = ctx.query(BoardDomain.listCards)
        val card = ctx.query(BoardDomain.getCard, cards.first().id)
        assertNotNull(card)
        assertEquals("Important task", card!!.title)
    }

    @Test
    fun `column count via http`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Task 1")
        ctx.given(BoardDomain.addCard, "Task 2")
        val count = ctx.query(BoardDomain.columnCount, "todo")
        assertEquals(2, count)
    }

    @Test
    fun `multiple cards across columns via http`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Task A")
        ctx.given(BoardDomain.addCard, "Task B")
        val cards = ctx.query(BoardDomain.listCards)
        ctx.act(BoardDomain.moveCard, MoveCardPayload(cards[0].id, "doing"))
        val todoCount = ctx.query(BoardDomain.columnCount, "todo")
        val doingCount = ctx.query(BoardDomain.columnCount, "doing")
        assertEquals(1, todoCount)
        assertEquals(1, doingCount)
    }

    @Test
    fun `trace records http operations`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Traced task")
        ctx.then(BoardDomain.hasTotalCards, 1)
        val trace = ctx.trace()
        assertEquals(2, trace.size)
        assertEquals("action", trace[0].kind)
        assertEquals("assertion", trace[1].kind)
    }
}
