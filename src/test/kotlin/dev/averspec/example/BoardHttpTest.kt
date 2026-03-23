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
        ctx.Given(BoardDomain.addCard, "Buy groceries")
        ctx.Then(BoardDomain.hasTotalCards, 1)
    }

    @Test
    fun `new card starts in todo via http`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Write tests")
        val cards = ctx.Query(BoardDomain.listCards)
        val card = cards.first()
        ctx.Then(BoardDomain.cardIsInColumn, CardColumnCheck(card.id, "todo"))
    }

    @Test
    fun `move card via http`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Deploy service")
        val cards = ctx.Query(BoardDomain.listCards)
        val card = cards.first()
        ctx.When(BoardDomain.moveCard, MoveCardPayload(card.id, "doing"))
        ctx.Then(BoardDomain.cardIsInColumn, CardColumnCheck(card.id, "doing"))
    }

    @Test
    fun `remove card via http`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Temp task")
        val cards = ctx.Query(BoardDomain.listCards)
        ctx.When(BoardDomain.removeCard, cards.first().id)
        ctx.Then(BoardDomain.hasTotalCards, 0)
    }

    @Test
    fun `query card by id via http`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Important task")
        val cards = ctx.Query(BoardDomain.listCards)
        val card = ctx.Query(BoardDomain.getCard, cards.first().id)
        assertNotNull(card)
        assertEquals("Important task", card!!.title)
    }

    @Test
    fun `column count via http`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Task 1")
        ctx.Given(BoardDomain.addCard, "Task 2")
        val count = ctx.Query(BoardDomain.columnCount, "todo")
        assertEquals(2, count)
    }

    @Test
    fun `multiple cards across columns via http`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Task A")
        ctx.Given(BoardDomain.addCard, "Task B")
        val cards = ctx.Query(BoardDomain.listCards)
        ctx.When(BoardDomain.moveCard, MoveCardPayload(cards[0].id, "doing"))
        val todoCount = ctx.Query(BoardDomain.columnCount, "todo")
        val doingCount = ctx.Query(BoardDomain.columnCount, "doing")
        assertEquals(1, todoCount)
        assertEquals(1, doingCount)
    }

    @Test
    fun `trace records http operations`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Traced task")
        ctx.Then(BoardDomain.hasTotalCards, 1)
        val trace = ctx.trace()
        assertEquals(2, trace.size)
        assertEquals("action", trace[0].kind)
        assertEquals("assertion", trace[1].kind)
    }
}
