package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DomainTest {

    @Test
    fun `domain has a name`() {
        val d = domain("todo") {}
        assertEquals("todo", d.name)
    }

    @Test
    fun `domain starts with empty markers`() {
        val d = domain("todo") {}
        assertTrue(d.markers.isEmpty())
    }

    @Test
    fun `action marker is registered`() {
        val d = domain("todo") {
            action<String>("add item")
        }
        assertEquals(1, d.markers.size)
        assertTrue(d.markers.containsKey("add item"))
    }

    @Test
    fun `query marker is registered`() {
        val d = domain("todo") {
            query<Unit, List<String>>("list items")
        }
        assertEquals(1, d.markers.size)
        assertTrue(d.markers.containsKey("list items"))
    }

    @Test
    fun `assertion marker is registered`() {
        val d = domain("todo") {
            assertion<Int>("has count")
        }
        assertEquals(1, d.markers.size)
        assertTrue(d.markers.containsKey("has count"))
    }

    @Test
    fun `action marker has correct kind`() {
        val d = domain("todo") {
            action<String>("add item")
        }
        assertEquals(MarkerKind.ACTION, d.markers["add item"]!!.kind)
    }

    @Test
    fun `query marker has correct kind`() {
        val d = domain("todo") {
            query<Unit, List<String>>("list items")
        }
        assertEquals(MarkerKind.QUERY, d.markers["list items"]!!.kind)
    }

    @Test
    fun `assertion marker has correct kind`() {
        val d = domain("todo") {
            assertion<Int>("has count")
        }
        assertEquals(MarkerKind.ASSERTION, d.markers["has count"]!!.kind)
    }

    @Test
    fun `markers carry domain name`() {
        val d = domain("todo") {
            action<String>("add item")
        }
        assertEquals("todo", d.markers["add item"]!!.domainName)
    }

    @Test
    fun `multiple markers coexist`() {
        val d = domain("todo") {
            action<String>("add item")
            query<Unit, List<String>>("list items")
            assertion<Int>("has count")
        }
        assertEquals(3, d.markers.size)
        assertEquals(MarkerKind.ACTION, d.markers["add item"]!!.kind)
        assertEquals(MarkerKind.QUERY, d.markers["list items"]!!.kind)
        assertEquals(MarkerKind.ASSERTION, d.markers["has count"]!!.kind)
    }

    @Test
    fun `domain block returns marker references`() {
        lateinit var addItem: ActionMarker<String>
        val d = domain("todo") {
            addItem = action("add item")
        }
        assertNotNull(addItem)
        assertEquals("add item", addItem.name)
        assertSame(d.markers["add item"], addItem)
    }
}
