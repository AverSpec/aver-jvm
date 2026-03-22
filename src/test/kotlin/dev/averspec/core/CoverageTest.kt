package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CoverageTest {

    @Test
    fun `full coverage when all markers called`() {
        val d = domain("todo") {
            action<String>("add item")
            assertion<Int>("has count")
        }
        val report = checkCoverage(d, setOf("add item", "has count"))
        assertTrue(report.complete)
        assertEquals(100.0, report.percentage)
        assertTrue(report.missing.isEmpty())
    }

    @Test
    fun `partial coverage reports missing`() {
        val d = domain("todo") {
            action<String>("add item")
            assertion<Int>("has count")
        }
        val report = checkCoverage(d, setOf("add item"))
        assertFalse(report.complete)
        assertEquals(50.0, report.percentage)
        assertTrue(report.missing.contains("has count"))
    }

    @Test
    fun `zero coverage on empty calls`() {
        val d = domain("todo") {
            action<String>("add item")
        }
        val report = checkCoverage(d, emptySet())
        assertEquals(0.0, report.percentage)
        assertEquals(1, report.missing.size)
    }

    @Test
    fun `empty domain is always complete`() {
        val d = domain("empty") {}
        val report = checkCoverage(d, emptySet())
        assertTrue(report.complete)
        assertEquals(100.0, report.percentage)
    }
}
