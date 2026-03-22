package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ExtensionTest {

    @Test
    fun `extend creates child domain with parent markers`() {
        val parent = domain("parent") {
            action<String>("base_action")
            query<Unit, Int>("base_query")
        }
        val child = parent.extend("child") {
            action<String>("child_action")
        }
        assertTrue(child.markers.containsKey("base_action"))
        assertTrue(child.markers.containsKey("base_query"))
        assertTrue(child.markers.containsKey("child_action"))
    }

    @Test
    fun `extended domain has correct name`() {
        val parent = domain("parent") {}
        val child = parent.extend("child") {}
        assertEquals("child", child.name)
    }

    @Test
    fun `extended domain tracks parent`() {
        val parent = domain("parent") {}
        val child = parent.extend("child") {}
        assertNotNull(child.parent)
        assertEquals("parent", child.parent!!.name)
    }

    @Test
    fun `child markers have child domain name`() {
        val parent = domain("parent") {
            action<String>("base")
        }
        val child = parent.extend("child") {
            action<String>("extra")
        }
        assertEquals("child", child.markers["extra"]!!.domainName)
    }

    @Test
    fun `parent markers keep parent domain name`() {
        val parent = domain("parent") {
            action<String>("base")
        }
        val child = parent.extend("child") {}
        assertEquals("parent", child.markers["base"]!!.domainName)
    }

    @Test
    fun `extended domain marker count is correct`() {
        val parent = domain("parent") {
            action<String>("a1")
            query<Unit, Int>("q1")
        }
        val child = parent.extend("child") {
            action<String>("a2")
            assertion<Int>("c1")
        }
        assertEquals(4, child.markers.size)
    }

    @Test
    fun `multiple levels of extension`() {
        val grandparent = domain("gp") { action<String>("gp_action") }
        val parent = grandparent.extend("parent") { action<String>("p_action") }
        val child = parent.extend("child") { action<String>("c_action") }
        assertEquals(3, child.markers.size)
        assertEquals("parent", child.parent!!.name)
        assertEquals("gp", child.parent!!.parent!!.name)
    }
}
