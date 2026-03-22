package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test

/**
 * Port of test_contract_verification.py: extract, write, read, verify contracts.
 */
class ContractVerificationTest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `extract contract from passing test with static telemetry`() {
        s.test("cv static") { ctx ->
            ctx.given(AverCoreDomain.setupContractWorkbench, Unit)
            ctx.given(AverCoreDomain.defineContractDomain, ContractDomainSpecPayload(
                domainName = "cv-static",
                actions = listOf("login"),
                spanNames = listOf("auth.login"),
                spanAttributes = listOf(mapOf("user.role" to "admin" as Any))
            ))
            ctx.given(AverCoreDomain.createContractAdapter, ContractTraceSpecPayload(
                spans = listOf(mapOf("name" to "auth.login" as Any, "attributes" to mapOf("user.role" to "admin") as Any))
            ))
            ctx.`when`(AverCoreDomain.runContractOperations, Unit)
            ctx.`when`(AverCoreDomain.extractAndWriteContract, Unit)
            ctx.`when`(AverCoreDomain.loadAndVerifyContract, ContractTraceSpecPayload(
                spans = listOf(mapOf(
                    "name" to "auth.login" as Any,
                    "attributes" to mapOf("user.role" to "admin") as Any,
                    "trace_id" to "t1" as Any,
                    "span_id" to "s1" as Any
                ))
            ))
            ctx.then(AverCoreDomain.contractPasses, Unit)
        }
    }

    @Test
    fun `extract contract from parameterized telemetry`() {
        s.test("cv param") { ctx ->
            ctx.given(AverCoreDomain.setupContractWorkbench, Unit)
            ctx.given(AverCoreDomain.defineContractDomain, ContractDomainSpecPayload(
                domainName = "cv-param",
                actions = listOf("signup"),
                spanNames = listOf("user.signup"),
                spanAttributes = listOf(mapOf("user.email" to "\$email" as Any)),
                parameterized = true
            ))
            ctx.given(AverCoreDomain.createContractAdapter, ContractTraceSpecPayload(
                spans = listOf(mapOf("name" to "user.signup" as Any, "attributes" to mapOf("user.email" to "alice@test.com") as Any))
            ))
            ctx.`when`(AverCoreDomain.runContractOperations, Unit)
            ctx.`when`(AverCoreDomain.extractAndWriteContract, Unit)
            ctx.`when`(AverCoreDomain.loadAndVerifyContract, ContractTraceSpecPayload(
                spans = listOf(mapOf(
                    "name" to "user.signup" as Any,
                    "attributes" to mapOf("user.email" to "bob@test.com") as Any,
                    "trace_id" to "t1" as Any,
                    "span_id" to "s1" as Any
                ))
            ))
            ctx.then(AverCoreDomain.contractPasses, Unit)
        }
    }

    @Test
    fun `verify passes on matching production traces`() {
        s.test("cv match") { ctx ->
            ctx.given(AverCoreDomain.setupContractWorkbench, Unit)
            ctx.given(AverCoreDomain.defineContractDomain, ContractDomainSpecPayload(
                domainName = "cv-match",
                actions = listOf("checkout"),
                spanNames = listOf("order.checkout"),
                spanAttributes = listOf(mapOf("amount" to 100 as Any))
            ))
            ctx.given(AverCoreDomain.createContractAdapter, ContractTraceSpecPayload(
                spans = listOf(mapOf("name" to "order.checkout" as Any, "attributes" to mapOf("amount" to 100) as Any))
            ))
            ctx.`when`(AverCoreDomain.runContractOperations, Unit)
            ctx.`when`(AverCoreDomain.extractAndWriteContract, Unit)
            ctx.`when`(AverCoreDomain.loadAndVerifyContract, ContractTraceSpecPayload(
                spans = listOf(mapOf(
                    "name" to "order.checkout" as Any,
                    "attributes" to mapOf("amount" to 100) as Any,
                    "trace_id" to "t1" as Any,
                    "span_id" to "s1" as Any
                ))
            ))
            ctx.then(AverCoreDomain.contractPasses, Unit)
            ctx.then(AverCoreDomain.violationCountIs, ViolationCountCheckPayload(expected = 0))
        }
    }

    @Test
    fun `verify fails on missing span`() {
        s.test("cv missing") { ctx ->
            ctx.given(AverCoreDomain.setupContractWorkbench, Unit)
            ctx.given(AverCoreDomain.defineContractDomain, ContractDomainSpecPayload(
                domainName = "cv-missing",
                actions = listOf("start", "charge"),
                spanNames = listOf("checkout.start", "payment.charge")
            ))
            ctx.given(AverCoreDomain.createContractAdapter, ContractTraceSpecPayload(
                spans = listOf(
                    mapOf("name" to "checkout.start" as Any),
                    mapOf("name" to "payment.charge" as Any)
                )
            ))
            ctx.`when`(AverCoreDomain.runContractOperations, Unit)
            ctx.`when`(AverCoreDomain.extractAndWriteContract, Unit)
            ctx.`when`(AverCoreDomain.loadAndVerifyContract, ContractTraceSpecPayload(
                spans = listOf(mapOf(
                    "name" to "checkout.start" as Any,
                    "trace_id" to "t1" as Any,
                    "span_id" to "s1" as Any
                ))
            ))
            ctx.then(AverCoreDomain.contractHasViolations, Unit)
            ctx.then(AverCoreDomain.violationIncludes, "missing-span")
        }
    }

    @Test
    fun `verify fails on literal attribute mismatch`() {
        s.test("cv literal") { ctx ->
            ctx.given(AverCoreDomain.setupContractWorkbench, Unit)
            ctx.given(AverCoreDomain.defineContractDomain, ContractDomainSpecPayload(
                domainName = "cv-literal",
                actions = listOf("cancel"),
                spanNames = listOf("order.cancel"),
                spanAttributes = listOf(mapOf("order.status" to "cancelled" as Any))
            ))
            ctx.given(AverCoreDomain.createContractAdapter, ContractTraceSpecPayload(
                spans = listOf(mapOf("name" to "order.cancel" as Any, "attributes" to mapOf("order.status" to "cancelled") as Any))
            ))
            ctx.`when`(AverCoreDomain.runContractOperations, Unit)
            ctx.`when`(AverCoreDomain.extractAndWriteContract, Unit)
            ctx.`when`(AverCoreDomain.loadAndVerifyContract, ContractTraceSpecPayload(
                spans = listOf(mapOf(
                    "name" to "order.cancel" as Any,
                    "attributes" to mapOf("order.status" to "canceled") as Any,
                    "trace_id" to "t1" as Any,
                    "span_id" to "s1" as Any
                ))
            ))
            ctx.then(AverCoreDomain.contractHasViolations, Unit)
            ctx.then(AverCoreDomain.violationIncludes, "literal-mismatch")
        }
    }

    @Test
    fun `verify fails on correlation violation`() {
        s.test("cv corr") { ctx ->
            ctx.given(AverCoreDomain.setupContractWorkbench, Unit)
            ctx.given(AverCoreDomain.defineContractDomain, ContractDomainSpecPayload(
                domainName = "cv-corr",
                actions = listOf("login", "session"),
                spanNames = listOf("auth.login", "auth.session"),
                spanAttributes = listOf(
                    mapOf("user.email" to "\$email" as Any),
                    mapOf("user.email" to "\$email" as Any)
                ),
                parameterized = true
            ))
            ctx.given(AverCoreDomain.createContractAdapter, ContractTraceSpecPayload(
                spans = listOf(
                    mapOf("name" to "auth.login" as Any, "attributes" to mapOf("user.email" to "alice@co.com") as Any),
                    mapOf("name" to "auth.session" as Any, "attributes" to mapOf("user.email" to "alice@co.com") as Any)
                )
            ))
            ctx.`when`(AverCoreDomain.runContractOperations, Unit)
            ctx.`when`(AverCoreDomain.extractAndWriteContract, Unit)
            ctx.`when`(AverCoreDomain.loadAndVerifyContract, ContractTraceSpecPayload(
                spans = listOf(
                    mapOf("name" to "auth.login" as Any, "attributes" to mapOf("user.email" to "alice@co.com") as Any, "trace_id" to "t1" as Any, "span_id" to "s1" as Any),
                    mapOf("name" to "auth.session" as Any, "attributes" to mapOf("user.email" to "bob@co.com") as Any, "trace_id" to "t1" as Any, "span_id" to "s2" as Any)
                )
            ))
            ctx.then(AverCoreDomain.contractHasViolations, Unit)
            ctx.then(AverCoreDomain.violationIncludes, "correlation-violation")
        }
    }

    @Test
    fun `contract write and read round trip`() {
        s.test("cv roundtrip") { ctx ->
            ctx.given(AverCoreDomain.setupContractWorkbench, Unit)
            ctx.given(AverCoreDomain.defineContractDomain, ContractDomainSpecPayload(
                domainName = "cv-roundtrip",
                actions = listOf("op_one"),
                spanNames = listOf("service.op_one"),
                spanAttributes = listOf(mapOf("key" to "value" as Any))
            ))
            ctx.given(AverCoreDomain.createContractAdapter, ContractTraceSpecPayload(
                spans = listOf(mapOf("name" to "service.op_one" as Any, "attributes" to mapOf("key" to "value") as Any))
            ))
            ctx.`when`(AverCoreDomain.runContractOperations, Unit)
            ctx.`when`(AverCoreDomain.extractAndWriteContract, Unit)
            ctx.`when`(AverCoreDomain.loadAndVerifyContract, ContractTraceSpecPayload(
                spans = listOf(mapOf(
                    "name" to "service.op_one" as Any,
                    "attributes" to mapOf("key" to "value") as Any,
                    "trace_id" to "t1" as Any,
                    "span_id" to "s1" as Any
                ))
            ))
            ctx.then(AverCoreDomain.contractPasses, Unit)
        }
    }

    @Test
    fun `no matching traces produces violation`() {
        s.test("cv no match") { ctx ->
            ctx.given(AverCoreDomain.setupContractWorkbench, Unit)
            ctx.given(AverCoreDomain.defineContractDomain, ContractDomainSpecPayload(
                domainName = "cv-no-match",
                actions = listOf("expected_op"),
                spanNames = listOf("expected.span")
            ))
            ctx.given(AverCoreDomain.createContractAdapter, ContractTraceSpecPayload(
                spans = listOf(mapOf("name" to "expected.span" as Any))
            ))
            ctx.`when`(AverCoreDomain.runContractOperations, Unit)
            ctx.`when`(AverCoreDomain.extractAndWriteContract, Unit)
            ctx.`when`(AverCoreDomain.loadAndVerifyContract, ContractTraceSpecPayload(
                spans = listOf(mapOf(
                    "name" to "unrelated.span" as Any,
                    "trace_id" to "t1" as Any,
                    "span_id" to "s1" as Any
                ))
            ))
            ctx.then(AverCoreDomain.contractHasViolations, Unit)
            ctx.then(AverCoreDomain.violationIncludes, "no-matching-traces")
        }
    }
}
