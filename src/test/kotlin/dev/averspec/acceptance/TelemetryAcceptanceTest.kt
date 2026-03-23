package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test

/**
 * Port of test_telemetry_acceptance.py: telemetry span matching through itself.
 */
class TelemetryAcceptanceTest {

    private val adapter = buildAverCoreAdapter()
    private val s = suite(AverCoreDomain.d, adapter)

    @Test
    fun `telemetry span matched on action`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineTelemetryDomain, TelemetryDomainSpec(
            name = "tel-match",
            actions = listOf("create_order"),
            spanNames = listOf("order.create")
        ))
        ctx.Given(AverCoreDomain.createTelemetryAdapter, TelemetryAdapterSpecPayload())
        ctx.When(AverCoreDomain.callTelemetryOperation, OperationCall(
            markerName = "create_order", payload = "order-1"
        ))
        ctx.Then(AverCoreDomain.telemetrySpanMatched, TelemetrySpanCheckPayload(
            index = 0,
            expectedSpan = "order.create",
            matched = true
        ))
    }

    @Test
    fun `multiple telemetry spans matched`() = s.run { ctx ->
        ctx.Given(AverCoreDomain.defineTelemetryDomain, TelemetryDomainSpec(
            name = "tel-multi",
            actions = listOf("start_flow", "complete_flow"),
            spanNames = listOf("flow.start", "flow.complete")
        ))
        ctx.Given(AverCoreDomain.createTelemetryAdapter, TelemetryAdapterSpecPayload())
        ctx.When(AverCoreDomain.callTelemetryOperation, OperationCall(
            markerName = "start_flow", payload = "flow-1"
        ))
        ctx.When(AverCoreDomain.callTelemetryOperation, OperationCall(
            markerName = "complete_flow", payload = "flow-1"
        ))
        ctx.Then(AverCoreDomain.telemetrySpanMatched, TelemetrySpanCheckPayload(
            index = 0, expectedSpan = "flow.start", matched = true
        ))
        ctx.Then(AverCoreDomain.telemetrySpanMatched, TelemetrySpanCheckPayload(
            index = 1, expectedSpan = "flow.complete", matched = true
        ))
    }
}
