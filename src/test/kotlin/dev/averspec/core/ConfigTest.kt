package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ConfigTest {

    @Test
    fun `default config has empty adapters`() {
        val config = defineConfig {}
        assertTrue(config.adapters.isEmpty())
    }

    @Test
    fun `default coverage mode is WARN`() {
        val config = defineConfig {}
        assertEquals(CoverageMode.WARN, config.coverage)
    }

    @Test
    fun `default teardown failure mode is WARN`() {
        val config = defineConfig {}
        assertEquals(TeardownFailureMode.WARN, config.teardownFailureMode)
    }

    @Test
    fun `config accepts adapters`() {
        val d = domain("test") {}
        val protocol = UnitProtocol { Unit }
        val adapter = implement<Unit>(d, protocol) {}
        val config = defineConfig {
            adapter(adapter)
        }
        assertEquals(1, config.adapters.size)
    }

    @Test
    fun `config allows overriding coverage mode`() {
        val config = defineConfig {
            coverage = CoverageMode.ERROR
        }
        assertEquals(CoverageMode.ERROR, config.coverage)
    }

    @Test
    fun `config allows overriding teardown failure mode`() {
        val config = defineConfig {
            teardownFailureMode = TeardownFailureMode.SILENT
        }
        assertEquals(TeardownFailureMode.SILENT, config.teardownFailureMode)
    }
}
