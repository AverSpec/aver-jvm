package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class EventuallyTest {

    @Test
    fun `returns immediately on success`() {
        val result = eventually { 42 }
        assertEquals(42, result)
    }

    @Test
    fun `retries until success`() {
        var attempts = 0
        val result = eventually(timeoutMs = 2000, intervalMs = 50) {
            attempts++
            if (attempts < 3) throw AssertionError("not yet")
            "done"
        }
        assertEquals("done", result)
        assertTrue(attempts >= 3)
    }

    @Test
    fun `throws on timeout`() {
        val err = assertThrows(AssertionError::class.java) {
            eventually(timeoutMs = 200, intervalMs = 50) {
                throw AssertionError("always fails")
            }
        }
        assertTrue(err.message!!.contains("Timed out"))
    }

    @Test
    fun `preserves original error as cause`() {
        val err = assertThrows(AssertionError::class.java) {
            eventually(timeoutMs = 200, intervalMs = 50) {
                throw IllegalStateException("root cause")
            }
        }
        assertNotNull(err.cause)
        assertEquals("root cause", err.cause!!.message)
    }
}
