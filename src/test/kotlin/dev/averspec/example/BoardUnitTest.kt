package dev.averspec.example

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Task board tests through the unit adapter.
 */
class BoardUnitTest {

    private val adapter = buildBoardUnitAdapter()
    private val s = suite(BoardDomain.d, adapter)

    @Test
    fun `add a card to the board`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Buy groceries")
        ctx.then(BoardDomain.hasTotalCards, 1)
    }

    @Test
    fun `new card starts in todo column`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Write tests")
        val cards = ctx.query(BoardDomain.listCards, null as String?)
        val card = cards.first()
        ctx.then(BoardDomain.cardIsInColumn, Pair(card.id, "todo"))
    }

    @Test
    fun `move card between columns`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Deploy service")
        val cards = ctx.query(BoardDomain.listCards, null as String?)
        val card = cards.first()
        ctx.act(BoardDomain.moveCard, Pair(card.id, "doing"))
        ctx.then(BoardDomain.cardIsInColumn, Pair(card.id, "doing"))
    }

    @Test
    fun `remove card from board`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Temp task")
        val cards = ctx.query(BoardDomain.listCards, null as String?)
        ctx.act(BoardDomain.removeCard, cards.first().id)
        ctx.then(BoardDomain.hasTotalCards, 0)
    }

    @Test
    fun `query card by id`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Important task")
        val cards = ctx.query(BoardDomain.listCards, null as String?)
        val card = ctx.query(BoardDomain.getCard, cards.first().id)
        assertNotNull(card)
        assertEquals("Important task", card!!.title)
    }

    @Test
    fun `column count tracks cards`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Task 1")
        ctx.given(BoardDomain.addCard, "Task 2")
        val count = ctx.query(BoardDomain.columnCount, "todo")
        assertEquals(2, count)
    }

    @Test
    fun `multiple cards across columns`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Task A")
        ctx.given(BoardDomain.addCard, "Task B")
        val cards = ctx.query(BoardDomain.listCards, null as String?)
        ctx.act(BoardDomain.moveCard, Pair(cards[0].id, "doing"))
        val todoCount = ctx.query(BoardDomain.columnCount, "todo")
        val doingCount = ctx.query(BoardDomain.columnCount, "doing")
        assertEquals(1, todoCount)
        assertEquals(1, doingCount)
    }

    @Test
    fun `trace records board operations`() = s.run { ctx ->
        ctx.given(BoardDomain.addCard, "Traced task")
        ctx.then(BoardDomain.hasTotalCards, 1)
        val trace = ctx.trace()
        assertEquals(2, trace.size)
        assertEquals("action", trace[0].kind)
        assertEquals("given", trace[0].category)
        assertEquals("assertion", trace[1].kind)
        assertEquals("then", trace[1].category)
    }
}
