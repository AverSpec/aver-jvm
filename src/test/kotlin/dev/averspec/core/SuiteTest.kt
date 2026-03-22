package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SuiteTest {

    data class TodoCtx(val items: MutableList<String> = mutableListOf())

    private fun buildTodoSuite(): Triple<Suite, ActionMarker<String>, AssertionMarker<Int>> {
        lateinit var addItem: ActionMarker<String>
        lateinit var hasCount: AssertionMarker<Int>
        val d = domain("todo") {
            addItem = action("add item")
            hasCount = assertion("has count")
        }
        val protocol = UnitProtocol { TodoCtx() }
        val adapter = implement<TodoCtx>(d, protocol) {
            onAction(addItem) { ctx: TodoCtx, text: String -> ctx.items.add(text) }
            onAssertion(hasCount) { ctx: TodoCtx, expected: Int ->
                assertEquals(expected, ctx.items.size)
            }
        }
        return Triple(suite(d, adapter), addItem, hasCount)
    }

    @Test
    fun `suite runs a test`() {
        val (s, addItem, hasCount) = buildTodoSuite()
        var ran = false
        s.test("add an item") { ctx ->
            ctx.given(addItem, "buy milk")
            ctx.then(hasCount, 1)
            ran = true
        }
        assertTrue(ran)
    }

    @Test
    fun `given dispatches action`() {
        val (s, addItem, hasCount) = buildTodoSuite()
        s.test("dispatch") { ctx ->
            ctx.given(addItem, "item 1")
            ctx.then(hasCount, 1)
        }
    }

    @Test
    fun `when dispatches action`() {
        val (s, addItem, hasCount) = buildTodoSuite()
        s.test("when dispatch") { ctx ->
            ctx.act(addItem, "item 1")
            ctx.then(hasCount, 1)
        }
    }

    @Test
    fun `trace records entries`() {
        val (s, addItem, hasCount) = buildTodoSuite()
        s.test("trace test") { ctx ->
            ctx.given(addItem, "first")
            ctx.then(hasCount, 1)
            val trace = ctx.trace()
            assertEquals(2, trace.size)
            assertEquals("given", trace[0].category)
            assertEquals("add item", trace[0].name)
            assertEquals("then", trace[1].category)
            assertEquals("has count", trace[1].name)
        }
    }

    @Test
    fun `trace records pass status`() {
        val (s, addItem, _) = buildTodoSuite()
        s.test("status") { ctx ->
            ctx.given(addItem, "x")
            assertEquals("pass", ctx.trace()[0].status)
        }
    }

    @Test
    fun `trace records fail status on error`() {
        val (s, _, hasCount) = buildTodoSuite()
        s.test("fail status") { ctx ->
            try {
                ctx.then(hasCount, 99)
            } catch (_: Throwable) {}
            assertEquals("fail", ctx.trace()[0].status)
        }
    }

    @Test
    fun `trace records duration`() {
        val (s, addItem, _) = buildTodoSuite()
        s.test("duration") { ctx ->
            ctx.given(addItem, "x")
            assertTrue(ctx.trace()[0].durationMs >= 0.0)
        }
    }

    @Test
    fun `then rejects action markers`() {
        lateinit var addItem: ActionMarker<String>
        val d = domain("todo") {
            addItem = action("add item")
        }
        val protocol = UnitProtocol { TodoCtx() }
        val adapter = implement<TodoCtx>(d, protocol) {
            onAction(addItem) { ctx: TodoCtx, text: String -> ctx.items.add(text) }
        }
        val s = suite(d, adapter)
        s.test("reject") { ctx ->
            assertThrows(IllegalArgumentException::class.java) {
                ctx.then(addItem, "nope")
            }
        }
    }

    @Test
    fun `when rejects assertion markers`() {
        lateinit var hasCount: AssertionMarker<Int>
        val d = domain("todo") {
            hasCount = assertion("has count")
        }
        val protocol = UnitProtocol { TodoCtx() }
        val adapter = implement<TodoCtx>(d, protocol) {
            onAssertion(hasCount) { _: TodoCtx, _: Int -> }
        }
        val s = suite(d, adapter)
        s.test("reject") { ctx ->
            assertThrows(IllegalArgumentException::class.java) {
                ctx.act(hasCount, 1)
            }
        }
    }

    @Test
    fun `query proxy dispatches queries`() {
        lateinit var listItems: QueryMarker<Unit, List<String>>
        lateinit var addItem: ActionMarker<String>
        val d = domain("todo") {
            addItem = action("add item")
            listItems = query("list items")
        }
        val protocol = UnitProtocol { TodoCtx() }
        val adapter = implement<TodoCtx>(d, protocol) {
            onAction(addItem) { ctx: TodoCtx, text: String -> ctx.items.add(text) }
            onQuery(listItems) { ctx: TodoCtx, _: Unit -> ctx.items.toList() }
        }
        val s = suite(d, adapter)
        s.test("query") { ctx ->
            ctx.given(addItem, "milk")
            val items = ctx.query(listItems, Unit)
            assertEquals(listOf("milk"), items)
        }
    }

    @Test
    fun `each test gets fresh context`() {
        val (s, addItem, hasCount) = buildTodoSuite()
        s.test("first") { ctx ->
            ctx.given(addItem, "a")
            ctx.then(hasCount, 1)
        }
        s.test("second") { ctx ->
            ctx.then(hasCount, 0)
        }
    }

    @Test
    fun `trace includes payload`() {
        val (s, addItem, _) = buildTodoSuite()
        s.test("payload") { ctx ->
            ctx.given(addItem, "buy eggs")
            assertEquals("buy eggs", ctx.trace()[0].payload)
        }
    }

    @Test
    fun `trace records kind for action`() {
        val (s, addItem, _) = buildTodoSuite()
        s.test("kind") { ctx ->
            ctx.given(addItem, "x")
            assertEquals("action", ctx.trace()[0].kind)
        }
    }

    @Test
    fun `trace records kind for assertion`() {
        val (s, _, hasCount) = buildTodoSuite()
        s.test("assertion kind") { ctx ->
            ctx.then(hasCount, 0)
            assertEquals("assertion", ctx.trace()[0].kind)
        }
    }

    @Test
    fun `query proxy rejects action markers`() {
        lateinit var addItem: ActionMarker<String>
        val d = domain("todo") {
            addItem = action("add item")
        }
        val protocol = UnitProtocol { TodoCtx() }
        val adapter = implement<TodoCtx>(d, protocol) {
            onAction(addItem) { ctx: TodoCtx, text: String -> ctx.items.add(text) }
        }
        val s = suite(d, adapter)
        s.test("reject") { ctx ->
            assertThrows(IllegalArgumentException::class.java) {
                ctx.query(addItem, "nope")
            }
        }
    }

    @Test
    fun `given allows assertion markers`() {
        val (s, _, hasCount) = buildTodoSuite()
        s.test("given assertion") { ctx ->
            assertDoesNotThrow { ctx.given(hasCount, 0) }
        }
    }

    @Test
    fun `calledMarkerNames tracks invocations`() {
        val (s, addItem, hasCount) = buildTodoSuite()
        s.test("called markers") { ctx ->
            ctx.given(addItem, "x")
            ctx.then(hasCount, 1)
            assertTrue(ctx.calledMarkerNames().contains("add item"))
            assertTrue(ctx.calledMarkerNames().contains("has count"))
        }
    }

    @Test
    fun `teardown runs after test`() {
        data class Ctx(val items: MutableList<String> = mutableListOf())
        var tornDown = false
        val protocol = object : Protocol<Ctx> {
            override val name = "test"
            override fun setup() = Ctx()
            override fun teardown(ctx: Ctx) { tornDown = true }
        }
        val d = domain("td") {}
        val adapter = implement<Ctx>(d, protocol) {}
        val s = suite(d, adapter)
        s.test("t") { _ -> }
        assertTrue(tornDown)
    }

    @Test
    fun `teardown runs even on failure`() {
        data class Ctx(val items: MutableList<String> = mutableListOf())
        var tornDown = false
        val protocol = object : Protocol<Ctx> {
            override val name = "test"
            override fun setup() = Ctx()
            override fun teardown(ctx: Ctx) { tornDown = true }
        }
        lateinit var check: AssertionMarker<Int>
        val d = domain("td") { check = assertion("check") }
        val adapter = implement<Ctx>(d, protocol) {
            onAssertion(check) { _: Ctx, _: Int -> throw AssertionError("fail") }
        }
        val s = suite(d, adapter)
        try {
            s.test("t") { ctx -> ctx.then(check, 1) }
        } catch (_: Throwable) {}
        assertTrue(tornDown)
    }
}
