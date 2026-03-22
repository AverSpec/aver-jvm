package dev.averspec.acceptance

import dev.averspec.*

// --- Payload data classes ---

data class DomainSpec(
    val name: String,
    val actions: List<String>,
    val queries: List<String>,
    val assertions: List<String>
)

data class AdapterSpec(val protocolName: String = "unit")

data class OperationCall(val markerName: String, val payload: Any? = null)

data class ProxyCall(val proxyName: String, val markerName: String, val payload: Any? = null)

data class MarkerCheck(val name: String, val kind: String)

data class TraceCheck(val index: Int, val category: String, val status: String = "pass")

data class TraceEntryCheck(val index: Int, val kind: String, val category: String, val status: String = "pass")

data class TraceLengthCheck(val expected: Int)

data class QueryResultCheck(val markerName: String, val expected: Any)

data class VocabularyCheck(val actions: Int, val queries: Int, val assertions: Int)

data class ProxyRestrictionCheck(val proxyName: String, val markerName: String)

data class CompletenessCheck(val missing: List<String>)

data class FailingAssertionSpec(val markerName: String, val payload: Any? = null)

data class CoverageCheckPayload(val expectedPercentage: Int)

data class TelemetryDomainSpec(val name: String, val actions: List<String>, val spanNames: List<String>)

data class TelemetryAdapterSpecPayload(val dummy: Unit = Unit)

data class TelemetrySpanCheckPayload(val index: Int, val expectedSpan: String, val matched: Boolean)

data class ExtensionSpec(
    val childName: String,
    val newActions: List<String>,
    val newQueries: List<String>,
    val newAssertions: List<String>
)

data class ExtensionMarkerCheckPayload(
    val parentMarkerNames: List<String>,
    val childMarkerNames: List<String>
)

data class ContractDomainSpecPayload(
    val domainName: String,
    val actions: List<String>,
    val spanNames: List<String>,
    val spanAttributes: List<Map<String, Any>> = emptyList(),
    val parameterized: Boolean = false
)

data class ContractTraceSpecPayload(
    val spans: List<Map<String, Any>> = emptyList()
)

data class CoverageBreakdownCheckPayload(
    val actionsCalled: Int,
    val actionsTotal: Int,
    val queriesCalled: Int,
    val queriesTotal: Int,
    val assertionsCalled: Int,
    val assertionsTotal: Int
)

data class MarkerNamesCheckPayload(val expectedNames: List<String>)

data class MarkerKindMapCheckPayload(val expected: Map<String, String>)

data class MarkerCountCheckPayload(val expected: Int)

data class TraceNameCheckPayload(val index: Int, val expectedName: String)

data class ViolationCountCheckPayload(val expected: Int)

data class MissingMarkerErrorCheckPayload(
    val proxyName: String,
    val markerName: String,
    val expectedMatch: String
)

data class QueryResultTypeCheckPayload(val markerName: String, val expectedType: String)

// --- Domain definition ---

object AverCoreDomain {
    val d = Domain("aver-core")

    // Actions
    val defineDomain = ActionMarker<DomainSpec>("define domain", "aver-core")
    val createAdapter = ActionMarker<AdapterSpec>("create adapter", "aver-core")
    val callOperation = ActionMarker<OperationCall>("call operation", "aver-core")
    val callThroughProxy = ActionMarker<ProxyCall>("call through proxy", "aver-core")
    val executeFailingAssertion = ActionMarker<FailingAssertionSpec>("execute failing assertion", "aver-core")

    // Coverage actions
    val defineDomainForCoverage = ActionMarker<DomainSpec>("define domain for coverage", "aver-core")
    val createAdapterForCoverage = ActionMarker<AdapterSpec>("create adapter for coverage", "aver-core")
    val callCoverageOperation = ActionMarker<OperationCall>("call coverage operation", "aver-core")

    // Telemetry actions
    val defineTelemetryDomain = ActionMarker<TelemetryDomainSpec>("define telemetry domain", "aver-core")
    val createTelemetryAdapter = ActionMarker<TelemetryAdapterSpecPayload>("create telemetry adapter", "aver-core")
    val callTelemetryOperation = ActionMarker<OperationCall>("call telemetry operation", "aver-core")

    // Extension actions
    val extendDomain = ActionMarker<ExtensionSpec>("extend domain", "aver-core")

    // Multi-adapter
    val registerSecondAdapter = ActionMarker<AdapterSpec>("register second adapter", "aver-core")

    // Contract verification
    val setupContractWorkbench = ActionMarker<Unit>("setup contract workbench", "aver-core")
    val defineContractDomain = ActionMarker<ContractDomainSpecPayload>("define contract domain", "aver-core")
    val createContractAdapter = ActionMarker<ContractTraceSpecPayload>("create contract adapter", "aver-core")
    val runContractOperations = ActionMarker<Unit>("run contract operations", "aver-core")
    val extractAndWriteContract = ActionMarker<Unit>("extract and write contract", "aver-core")
    val loadAndVerifyContract = ActionMarker<ContractTraceSpecPayload>("load and verify contract", "aver-core")

    // Queries
    val getMarkers = QueryMarker<Unit, List<Map<String, String>>>("get markers", "aver-core")
    val getTrace = QueryMarker<Unit, List<TraceEntry>>("get trace", "aver-core")
    val getQueryResult = QueryMarker<String, Any?>("get query result", "aver-core")
    val getCoveragePercentage = QueryMarker<Unit, Int>("get coverage percentage", "aver-core")
    val getExtensionMarkers = QueryMarker<Unit, List<Map<String, String>>>("get extension markers", "aver-core")
    val getAdapterCount = QueryMarker<Unit, Int>("get adapter count", "aver-core")
    val getCoverageDetail = QueryMarker<Unit, Map<String, Any>>("get coverage detail", "aver-core")
    val getContractViolations = QueryMarker<Unit, Int>("get contract violations", "aver-core")
    val getViolationDetails = QueryMarker<Unit, List<ContractViolation>>("get violation details", "aver-core")

    // Assertions
    val domainHasMarker = AssertionMarker<MarkerCheck>("domain has marker", "aver-core")
    val traceHasEntry = AssertionMarker<TraceCheck>("trace has entry", "aver-core")
    val traceEntryMatches = AssertionMarker<TraceEntryCheck>("trace entry matches", "aver-core")
    val traceHasLength = AssertionMarker<TraceLengthCheck>("trace has length", "aver-core")
    val queryReturnedValue = AssertionMarker<QueryResultCheck>("query returned value", "aver-core")
    val hasVocabulary = AssertionMarker<VocabularyCheck>("has vocabulary", "aver-core")
    val adapterIsComplete = AssertionMarker<CompletenessCheck>("adapter is complete", "aver-core")
    val proxyRejectsWrongKind = AssertionMarker<ProxyRestrictionCheck>("proxy rejects wrong kind", "aver-core")
    val coverageIs = AssertionMarker<CoverageCheckPayload>("coverage is", "aver-core")
    val telemetrySpanMatched = AssertionMarker<TelemetrySpanCheckPayload>("telemetry span matched", "aver-core")
    val extensionHasMarkers = AssertionMarker<ExtensionMarkerCheckPayload>("extension has markers", "aver-core")
    val contractPasses = AssertionMarker<Unit>("contract passes", "aver-core")
    val contractHasViolations = AssertionMarker<Unit>("contract has violations", "aver-core")
    val violationIncludes = AssertionMarker<String>("violation includes", "aver-core")
    val hasParentDomain = AssertionMarker<String>("has parent domain", "aver-core")
    val adapterCountIs = AssertionMarker<Int>("adapter count is", "aver-core")
    val coverageBreakdownMatches = AssertionMarker<CoverageBreakdownCheckPayload>("coverage breakdown matches", "aver-core")
    val markersHaveNames = AssertionMarker<MarkerNamesCheckPayload>("markers have names", "aver-core")
    val markerKindsMatch = AssertionMarker<MarkerKindMapCheckPayload>("marker kinds match", "aver-core")
    val extensionMarkerCountIs = AssertionMarker<MarkerCountCheckPayload>("extension marker count is", "aver-core")
    val extensionMarkerNamesEqual = AssertionMarker<MarkerNamesCheckPayload>("extension marker names equal", "aver-core")
    val traceNameAtIndex = AssertionMarker<TraceNameCheckPayload>("trace name at index", "aver-core")
    val violationCountIs = AssertionMarker<ViolationCountCheckPayload>("violation count is", "aver-core")
    val missingMarkerRaisesError = AssertionMarker<MissingMarkerErrorCheckPayload>("missing marker raises error", "aver-core")
    val queryResultTypeIs = AssertionMarker<QueryResultTypeCheckPayload>("query result type is", "aver-core")

    init {
        // Register all markers in the domain
        val allMarkers: List<Marker> = listOf(
            defineDomain, createAdapter, callOperation, callThroughProxy, executeFailingAssertion,
            defineDomainForCoverage, createAdapterForCoverage, callCoverageOperation,
            defineTelemetryDomain, createTelemetryAdapter, callTelemetryOperation,
            extendDomain, registerSecondAdapter,
            setupContractWorkbench, defineContractDomain, createContractAdapter,
            runContractOperations, extractAndWriteContract, loadAndVerifyContract,
            getMarkers, getTrace, getQueryResult, getCoveragePercentage, getExtensionMarkers,
            getAdapterCount, getCoverageDetail, getContractViolations, getViolationDetails,
            domainHasMarker, traceHasEntry, traceEntryMatches, traceHasLength,
            queryReturnedValue, hasVocabulary, adapterIsComplete, proxyRejectsWrongKind,
            coverageIs, telemetrySpanMatched, extensionHasMarkers,
            contractPasses, contractHasViolations, violationIncludes,
            hasParentDomain, adapterCountIs, coverageBreakdownMatches,
            markersHaveNames, markerKindsMatch, extensionMarkerCountIs,
            extensionMarkerNamesEqual, traceNameAtIndex, violationCountIs,
            missingMarkerRaisesError, queryResultTypeIs
        )
        allMarkers.forEach { d.markers[it.name] = it }
    }
}
