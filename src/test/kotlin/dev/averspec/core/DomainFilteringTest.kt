package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DomainFilteringTest {

    data class Ctx(val items: MutableList<String> = mutableListOf())

    private fun buildSuite(envLookup: (String) -> String? = { null }): Triple<Suite, ActionMarker<String>, AssertionMarker<Int>> {
        lateinit var addItem: ActionMarker<String>
        lateinit var hasCount: AssertionMarker<Int>
        val d = domain("todo") {
            addItem = action("add item")
            hasCount = assertion("has count")
        }
        val protocol = UnitProtocol { Ctx() }
        val adapter = implement<Ctx>(d, protocol) {
            onAction(addItem) { ctx: Ctx, text: String -> ctx.items.add(text) }
            onAssertion(hasCount) { ctx: Ctx, expected: Int ->
                assertEquals(expected, ctx.items.size)
            }
        }
        return Triple(Suite(d, adapter, envLookup = envLookup), addItem, hasCount)
    }

    @Test
    fun `AVER_DOMAIN skips non-matching domain`() {
        val env = mapOf("AVER_DOMAIN" to "other")
        val (s, addItem, _) = buildSuite { env[it] }
        var ran = false
        s.test("should skip") {
            ran = true
        }
        assertFalse(ran, "test should have been skipped by AVER_DOMAIN filter")
    }

    @Test
    fun `AVER_DOMAIN runs matching domain`() {
        val env = mapOf("AVER_DOMAIN" to "todo")
        val (s, addItem, hasCount) = buildSuite { env[it] }
        var ran = false
        s.test("should run") { ctx ->
            ctx.Given(addItem, "milk")
            ctx.Then(hasCount, 1)
            ran = true
        }
        assertTrue(ran, "test should have run because AVER_DOMAIN matches")
    }

    @Test
    fun `AVER_ADAPTER skips non-matching adapter`() {
        val env = mapOf("AVER_ADAPTER" to "http")
        val (s, _, _) = buildSuite { env[it] }
        var ran = false
        s.test("should skip") {
            ran = true
        }
        assertFalse(ran, "test should have been skipped by AVER_ADAPTER filter")
    }

    @Test
    fun `AVER_ADAPTER runs matching adapter`() {
        val env = mapOf("AVER_ADAPTER" to "unit")
        val (s, addItem, hasCount) = buildSuite { env[it] }
        var ran = false
        s.test("should run") { ctx ->
            ctx.Given(addItem, "milk")
            ctx.Then(hasCount, 1)
            ran = true
        }
        assertTrue(ran, "test should have run because AVER_ADAPTER matches")
    }

    @Test
    fun `no env filters runs all tests`() {
        val (s, addItem, hasCount) = buildSuite { null }
        var ran = false
        s.test("should run") { ctx ->
            ctx.Given(addItem, "milk")
            ctx.Then(hasCount, 1)
            ran = true
        }
        assertTrue(ran, "test should run when no filters are set")
    }
}
