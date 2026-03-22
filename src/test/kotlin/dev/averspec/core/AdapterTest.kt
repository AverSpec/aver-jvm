package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AdapterTest {

    data class Ctx(val items: MutableList<String> = mutableListOf())

    private val protocol = UnitProtocol { Ctx() }

    private fun todoDomain(): Triple<Domain, ActionMarker<String>, AssertionMarker<Int>> {
        lateinit var addItem: ActionMarker<String>
        lateinit var hasCount: AssertionMarker<Int>
        val d = domain("todo") {
            addItem = action("add item")
            hasCount = assertion("has count")
        }
        return Triple(d, addItem, hasCount)
    }

    @Test
    fun `builds adapter with all handlers`() {
        val (d, addItem, hasCount) = todoDomain()
        val adapter = implement<Ctx>(d, protocol) {
            onAction(addItem) { ctx: Ctx, text: String -> ctx.items.add(text) }
            onAssertion(hasCount) { ctx: Ctx, expected: Int ->
                assertEquals(expected, ctx.items.size)
            }
        }
        assertNotNull(adapter)
        assertEquals(d, adapter.domain)
    }

    @Test
    fun `throws when handler is missing`() {
        val (d, addItem, _) = todoDomain()
        val err = assertThrows(IllegalArgumentException::class.java) {
            implement<Ctx>(d, protocol) {
                onAction(addItem) { _: Ctx, _: String -> }
            }
        }
        assertTrue(err.message!!.contains("Missing handlers"))
        assertTrue(err.message!!.contains("has count"))
    }

    @Test
    fun `adapt alias works`() {
        val (d, addItem, hasCount) = todoDomain()
        val adapter = adapt<Ctx>(d, protocol) {
            onAction(addItem) { ctx: Ctx, text: String -> ctx.items.add(text) }
            onAssertion(hasCount) { ctx: Ctx, expected: Int ->
                assertEquals(expected, ctx.items.size)
            }
        }
        assertNotNull(adapter)
    }

    @Test
    fun `adapter holds protocol reference`() {
        val (d, addItem, hasCount) = todoDomain()
        val adapter = implement<Ctx>(d, protocol) {
            onAction(addItem) { _: Ctx, _: String -> }
            onAssertion(hasCount) { _: Ctx, _: Int -> }
        }
        assertSame(protocol, adapter.protocol)
    }

    @Test
    fun `handler map has correct size`() {
        val (d, addItem, hasCount) = todoDomain()
        val adapter = implement<Ctx>(d, protocol) {
            onAction(addItem) { _: Ctx, _: String -> }
            onAssertion(hasCount) { _: Ctx, _: Int -> }
        }
        assertEquals(2, adapter.handlers.size)
    }

    @Test
    fun `query handler is stored`() {
        lateinit var listItems: QueryMarker<Unit, List<String>>
        val d = domain("todo") {
            listItems = query("list items")
        }
        val adapter = implement<Ctx>(d, protocol) {
            onQuery(listItems) { ctx: Ctx, _: Unit -> ctx.items.toList() }
        }
        assertTrue(adapter.handlers.containsKey("list items"))
    }

    @Test
    fun `missing multiple handlers lists all names`() {
        lateinit var addItem: ActionMarker<String>
        lateinit var removeItem: ActionMarker<String>
        lateinit var hasCount: AssertionMarker<Int>
        val d = domain("todo") {
            addItem = action("add item")
            removeItem = action("remove item")
            hasCount = assertion("has count")
        }
        val err = assertThrows(IllegalArgumentException::class.java) {
            implement<Ctx>(d, protocol) {
                onAction(addItem) { _: Ctx, _: String -> }
            }
        }
        assertTrue(err.message!!.contains("remove item"))
        assertTrue(err.message!!.contains("has count"))
    }

    @Test
    fun `empty domain builds with no handlers`() {
        val d = domain("empty") {}
        val adapter = implement<Ctx>(d, protocol) {}
        assertTrue(adapter.handlers.isEmpty())
    }
}
