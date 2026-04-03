package dev.averspec.core

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

class ContractTest {

    @Test
    fun `extract contract from trace entries`() {
        val entries = listOf(
            TraceEntry(
                kind = "action", category = "when", name = "login",
                telemetry = TelemetryMatchResult(
                    expected = TelemetryExpectation(span = "auth.login", attributes = mapOf("role" to "admin")),
                    matched = true
                )
            )
        )
        val collector = InMemoryCollector()
        val contract = extractContract("test-domain", entries, collector)
        assertEquals("test-domain", contract.domain)
        assertEquals(1, contract.entries.size)
        assertEquals("login", contract.entries[0].testName)
        assertEquals("auth.login", contract.entries[0].spans[0].name)
    }

    @Test
    fun `extract contract with parameterized attributes`() {
        val entries = listOf(
            TraceEntry(
                kind = "action", category = "when", name = "signup",
                telemetry = TelemetryMatchResult(
                    expected = TelemetryExpectation(span = "user.signup", attributes = mapOf("email" to "\$email")),
                    matched = true
                )
            )
        )
        val collector = InMemoryCollector()
        val contract = extractContract("test", entries, collector, parameterized = true)
        val binding = contract.entries[0].spans[0].attributes["email"]!!
        assertEquals("correlated", binding.kind)
        assertEquals("\$email", binding.symbol)
    }

    @Test
    fun `verify contract passes on matching traces`() {
        val contract = BehavioralContract(
            domain = "test",
            entries = listOf(
                ContractEntry("login", listOf(SpanExpectation("auth.login")))
            )
        )
        val trace = ProductionTrace(listOf(
            ProductionSpan(name = "auth.login")
        ))
        val result = verifyContract(contract, trace)
        assertTrue(result.passed)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun `verify contract fails on missing span`() {
        val contract = BehavioralContract(
            domain = "test",
            entries = listOf(
                ContractEntry("login", listOf(SpanExpectation("auth.login"))),
                ContractEntry("charge", listOf(SpanExpectation("payment.charge")))
            )
        )
        val trace = ProductionTrace(listOf(
            ProductionSpan(name = "auth.login")
        ))
        val result = verifyContract(contract, trace)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.kind == "missing-span" })
    }

    @Test
    fun `verify contract fails on literal mismatch`() {
        val contract = BehavioralContract(
            domain = "test",
            entries = listOf(
                ContractEntry("cancel", listOf(
                    SpanExpectation("order.cancel", mapOf(
                        "status" to AttributeBinding(kind = "literal", value = "cancelled")
                    ))
                ))
            )
        )
        val trace = ProductionTrace(listOf(
            ProductionSpan(name = "order.cancel", attributes = mapOf("status" to "canceled"))
        ))
        val result = verifyContract(contract, trace)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.kind == "literal-mismatch" })
    }

    @Test
    fun `verify contract fails on correlation violation`() {
        val contract = BehavioralContract(
            domain = "test",
            entries = listOf(
                ContractEntry("login", listOf(
                    SpanExpectation("auth.login", mapOf(
                        "email" to AttributeBinding(kind = "correlated", symbol = "\$email")
                    ))
                )),
                ContractEntry("session", listOf(
                    SpanExpectation("auth.session", mapOf(
                        "email" to AttributeBinding(kind = "correlated", symbol = "\$email")
                    ))
                ))
            )
        )
        val trace = ProductionTrace(listOf(
            ProductionSpan(name = "auth.login", attributes = mapOf("email" to "alice@co.com")),
            ProductionSpan(name = "auth.session", attributes = mapOf("email" to "bob@co.com"))
        ))
        val result = verifyContract(contract, trace)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.kind == "correlation-violation" })
    }

    @Test
    fun `write and read contract round trip`() {
        val contract = BehavioralContract(
            domain = "roundtrip",
            entries = listOf(
                ContractEntry("op", listOf(
                    SpanExpectation("service.op", mapOf(
                        "key" to AttributeBinding(kind = "literal", value = "value")
                    ))
                ))
            )
        )
        @Suppress("DEPRECATION")
        val dir = kotlin.io.createTempDir("aver-contract-test-")
        try {
            val files = writeContract(contract, dir)
            assertEquals(1, files.size)
            assertTrue(files[0].exists())
            assertTrue(files[0].path.endsWith("roundtrip/op.contract.json"))
            val loaded = readContract(dir)
            assertEquals("roundtrip", loaded.domain)
            assertEquals(1, loaded.entries.size)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `slugify generates correct slugs`() {
        assertEquals("create-a-task", slugify("create a task"))
        assertEquals("hello-world", slugify("Hello World"))
        assertEquals("test-123", slugify("test 123"))
        assertEquals("no-special-chars", slugify("no special chars!@#"))
        assertEquals("collapse-hyphens", slugify("collapse   hyphens"))
        assertEquals("trim-edges", slugify(" trim edges "))
    }

    @Test
    fun `write creates per-entry files`() {
        val contract = BehavioralContract(
            domain = "task-board",
            entries = listOf(
                ContractEntry("create a task", listOf(SpanExpectation("task.create"))),
                ContractEntry("delete a task", listOf(SpanExpectation("task.delete")))
            )
        )
        @Suppress("DEPRECATION")
        val dir = kotlin.io.createTempDir("aver-contract-test-")
        try {
            val files = writeContract(contract, dir)
            assertEquals(2, files.size)
            assertTrue(files[0].name == "create-a-task.contract.json")
            assertTrue(files[1].name == "delete-a-task.contract.json")
            // Verify file content has standard structure
            val text = files[0].readText()
            assertTrue(text.contains("\"version\": 1"))
            assertTrue(text.contains("\"domain\": \"task-board\""))
            assertTrue(text.contains("\"testName\": \"create a task\""))
            assertTrue(text.contains("\"entry\":"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `no matching traces produces violation`() {
        val contract = BehavioralContract(
            domain = "test",
            entries = listOf(
                ContractEntry("op", listOf(SpanExpectation("expected.span")))
            )
        )
        val trace = ProductionTrace(listOf(
            ProductionSpan(name = "unrelated.span")
        ))
        val result = verifyContract(contract, trace)
        assertFalse(result.passed)
        assertTrue(result.violations.any { it.kind == "no-matching-traces" || it.kind == "missing-span" })
    }

    @Test
    fun `correlation passes when symbols are consistent`() {
        val contract = BehavioralContract(
            domain = "test",
            entries = listOf(
                ContractEntry("login", listOf(
                    SpanExpectation("auth.login", mapOf(
                        "email" to AttributeBinding(kind = "correlated", symbol = "\$email")
                    ))
                )),
                ContractEntry("session", listOf(
                    SpanExpectation("auth.session", mapOf(
                        "email" to AttributeBinding(kind = "correlated", symbol = "\$email")
                    ))
                ))
            )
        )
        val trace = ProductionTrace(listOf(
            ProductionSpan(name = "auth.login", attributes = mapOf("email" to "alice@co.com")),
            ProductionSpan(name = "auth.session", attributes = mapOf("email" to "alice@co.com"))
        ))
        val result = verifyContract(contract, trace)
        assertTrue(result.passed)
    }

    @Test
    fun `empty contract passes`() {
        val contract = BehavioralContract(domain = "test", entries = emptyList())
        val result = verifyContract(contract, ProductionTrace(emptyList()))
        assertTrue(result.passed)
    }
}
