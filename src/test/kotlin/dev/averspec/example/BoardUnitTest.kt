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
        ctx.Given(BoardDomain.addCard, "Buy groceries")
        ctx.Then(BoardDomain.hasTotalCards, 1)
    }

    @Test
    fun `new card starts in todo column`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Write tests")
        val cards = ctx.Query(BoardDomain.listCards)
        val card = cards.first()
        ctx.Then(BoardDomain.cardIsInColumn, CardColumnCheck(card.id, "todo"))
    }

    @Test
    fun `move card between columns`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Deploy service")
        val cards = ctx.Query(BoardDomain.listCards)
        val card = cards.first()
        ctx.When(BoardDomain.moveCard, MoveCardPayload(card.id, "doing"))
        ctx.Then(BoardDomain.cardIsInColumn, CardColumnCheck(card.id, "doing"))
    }

    @Test
    fun `remove card from board`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Temp task")
        val cards = ctx.Query(BoardDomain.listCards)
        ctx.When(BoardDomain.removeCard, cards.first().id)
        ctx.Then(BoardDomain.hasTotalCards, 0)
    }

    @Test
    fun `query card by id`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Important task")
        val cards = ctx.Query(BoardDomain.listCards)
        val card = ctx.Query(BoardDomain.getCard, cards.first().id)
        assertNotNull(card)
        assertEquals("Important task", card!!.title)
    }

    @Test
    fun `column count tracks cards`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Task 1")
        ctx.Given(BoardDomain.addCard, "Task 2")
        val count = ctx.Query(BoardDomain.columnCount, "todo")
        assertEquals(2, count)
    }

    @Test
    fun `multiple cards across columns`() = s.run { ctx ->
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
    fun `trace records board operations`() = s.run { ctx ->
        ctx.Given(BoardDomain.addCard, "Traced task")
        ctx.Then(BoardDomain.hasTotalCards, 1)
        val trace = ctx.trace()
        assertEquals(2, trace.size)
        assertEquals("action", trace[0].kind)
        assertEquals("given", trace[0].category)
        assertEquals("assertion", trace[1].kind)
        assertEquals("then", trace[1].category)
    }
}
