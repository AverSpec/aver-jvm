package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

class ApprovalsTest {

    @Test
    fun `approve passes when content matches`() {
        @Suppress("DEPRECATION")
        val dir = kotlin.io.createTempDir("aver-approvals-")
        try {
            val approved = File(dir, "test.approved.txt")
            approved.writeText("hello world")
            assertDoesNotThrow { approve("hello world", approved) }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `approve fails when content differs`() {
        @Suppress("DEPRECATION")
        val dir = kotlin.io.createTempDir("aver-approvals-")
        try {
            val approved = File(dir, "test.approved.txt")
            approved.writeText("hello world")
            val err = assertThrows(AssertionError::class.java) {
                approve("hello mars", approved)
            }
            assertTrue(err.message!!.contains("Approval mismatch"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `approve creates received file on first run`() {
        @Suppress("DEPRECATION")
        val dir = kotlin.io.createTempDir("aver-approvals-")
        try {
            val approved = File(dir, "test.approved.txt")
            // No approved file exists
            val err = assertThrows(AssertionError::class.java) {
                approve("new output", approved)
            }
            assertTrue(err.message!!.contains("No approved file found"))
            val received = File(dir, "test.received.txt")
            assertTrue(received.exists())
            assertEquals("new output", received.readText())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `scrubber removes UUIDs`() {
        val text = "Order id=550e8400-e29b-41d4-a716-446655440000 created"
        val scrubbed = Scrubbers.UUID(text)
        assertEquals("Order id=<UUID> created", scrubbed)
    }

    @Test
    fun `scrubber removes timestamps`() {
        val text = "Created at 2024-01-15T10:30:00Z"
        val scrubbed = Scrubbers.TIMESTAMP(text)
        assertEquals("Created at <TIMESTAMP>", scrubbed)
    }

    @Test
    fun `combined scrubber removes both`() {
        val text = "id=550e8400-e29b-41d4-a716-446655440000 at 2024-01-15T10:30:00Z"
        val scrubbed = Scrubbers.combine(Scrubbers.UUID, Scrubbers.TIMESTAMP)(text)
        assertEquals("id=<UUID> at <TIMESTAMP>", scrubbed)
    }

    @Test
    fun `approve with scrubber normalizes content`() {
        @Suppress("DEPRECATION")
        val dir = kotlin.io.createTempDir("aver-approvals-")
        try {
            val approved = File(dir, "test.approved.txt")
            approved.writeText("id=<UUID> created")
            assertDoesNotThrow {
                approve("id=550e8400-e29b-41d4-a716-446655440000 created", approved, scrub = Scrubbers.UUID)
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `characterize is alias for approve`() {
        @Suppress("DEPRECATION")
        val dir = kotlin.io.createTempDir("aver-approvals-")
        try {
            val approved = File(dir, "test.approved.txt")
            approved.writeText("hello")
            assertDoesNotThrow { characterize("hello", approved) }
        } finally {
            dir.deleteRecursively()
        }
    }
}
