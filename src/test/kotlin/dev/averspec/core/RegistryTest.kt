package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class RegistryTest {

    @BeforeEach
    fun setup() {
        Registry.clear()
    }

    @Test
    fun `register and retrieve domain`() {
        val d = domain("test") {}
        Registry.registerDomain(d)
        assertEquals(d, Registry.getDomain("test"))
    }

    @Test
    fun `register and retrieve adapter`() {
        val d = domain("test") {}
        val protocol = UnitProtocol { Unit }
        val adapter = implement<Unit>(d, protocol) {}
        Registry.registerAdapter(adapter)
        assertEquals(1, Registry.getAdaptersForDomain("test").size)
    }

    @Test
    fun `snapshot captures current state`() {
        val d = domain("test") {}
        Registry.registerDomain(d)
        val snap = Registry.snapshot()
        assertEquals(1, snap.domains.size)
    }

    @Test
    fun `restore resets to snapshot state`() {
        val d1 = domain("d1") {}
        Registry.registerDomain(d1)
        val snap = Registry.snapshot()

        val d2 = domain("d2") {}
        Registry.registerDomain(d2)
        assertEquals(2, Registry.allDomains().size)

        Registry.restore(snap)
        assertEquals(1, Registry.allDomains().size)
        assertNotNull(Registry.getDomain("d1"))
        assertNull(Registry.getDomain("d2"))
    }

    @Test
    fun `clear empties registry`() {
        val d = domain("test") {}
        Registry.registerDomain(d)
        Registry.clear()
        assertTrue(Registry.allDomains().isEmpty())
        assertTrue(Registry.allAdapters().isEmpty())
    }

    @Test
    fun `multiple adapters for same domain`() {
        val d = domain("test") {}
        val p1 = UnitProtocol(name = "p1") { Unit }
        val p2 = UnitProtocol(name = "p2") { Unit }
        Registry.registerAdapter(implement<Unit>(d, p1) {})
        Registry.registerAdapter(implement<Unit>(d, p2) {})
        assertEquals(2, Registry.getAdaptersForDomain("test").size)
    }
}
