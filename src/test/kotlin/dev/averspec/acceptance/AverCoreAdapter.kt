package dev.averspec.acceptance

import dev.averspec.*
import java.io.File

/**
 * Workbench context: a workspace for creating and testing aver constructs.
 */
class AverWorkbench {
    var currentDomain: Domain? = null
    var currentAdapter: Adapter? = null
    var currentContext: TestContext? = null
    var lastQueryResults = mutableMapOf<String, Any?>()

    // Coverage workspace
    var coverageDomain: Domain? = null
    var coverageAdapter: Adapter? = null
    var coverageContext: TestContext? = null

    // Telemetry workspace
    var telemetryDomain: Domain? = null
    var telemetryAdapter: Adapter? = null
    var telemetryContext: TestContext? = null
    var telemetryCollector: InMemoryCollector? = null

    // Extension workspace
    var extendedDomain: Domain? = null

    // Multi-adapter workspace
    var secondAdapter: Adapter? = null

    // Contract verification workspace
    var contractTmpDir: File? = null
    var contractDomain: Domain? = null
    var contractAdapter: Adapter? = null
    var contractContext: TestContext? = null
    var contractCollector: InMemoryCollector? = null
    var contractDomainSpec: ContractDomainSpecPayload? = null
    var contractResult: ConformanceReport? = null
    var contractWrittenFiles: List<File>? = null
}

/**
 * Build the acceptance adapter for the AverCore domain.
 */
fun buildAverCoreAdapter(): Adapter {
    val dom = AverCoreDomain
    val protocol = UnitProtocol { AverWorkbench() }
    val handlers = mutableMapOf<String, Any>()

    // -- Actions --

    handlers["define domain"] = { wb: AverWorkbench, spec: DomainSpec ->
        val d = Domain(spec.name)
        spec.actions.forEach { name -> d.markers[name] = ActionMarker<String>(name, spec.name) }
        spec.queries.forEach { name -> d.markers[name] = QueryMarker<String, String>(name, spec.name) }
        spec.assertions.forEach { name -> d.markers[name] = AssertionMarker<String>(name, spec.name) }
        wb.currentDomain = d
    }

    handlers["create adapter"] = { wb: AverWorkbench, spec: AdapterSpec ->
        val d = wb.currentDomain ?: throw RuntimeException("No domain defined yet")
        val innerProtocol = UnitProtocol(name = spec.protocolName) { mutableMapOf<String, Any?>() }
        val h = mutableMapOf<String, Any>()
        for ((name, marker) in d.markers) {
            when (marker.kind) {
                MarkerKind.ACTION -> h[name] = { _: MutableMap<String, Any?>, _: Any? -> }
                MarkerKind.QUERY -> h[name] = { _: MutableMap<String, Any?>, _: Any? -> "result-$name" }
                MarkerKind.ASSERTION -> h[name] = { _: MutableMap<String, Any?>, _: Any? -> }
            }
        }
        wb.currentAdapter = Adapter(d, innerProtocol, h)
        val innerCtx = innerProtocol.setup()
        wb.currentContext = TestContext(d, wb.currentAdapter!!, innerCtx)
    }

    handlers["call operation"] = { wb: AverWorkbench, op: OperationCall ->
        val ctx = wb.currentContext ?: throw RuntimeException("No adapter created yet")
        val d = wb.currentDomain!!
        val marker = d.markers[op.markerName] ?: throw IllegalStateException("No marker '${op.markerName}'")
        when (marker.kind) {
            MarkerKind.ACTION -> {
                @Suppress("UNCHECKED_CAST")
                ctx.When(marker as ActionMarker<Any?>, op.payload)
            }
            MarkerKind.QUERY -> {
                @Suppress("UNCHECKED_CAST")
                val result = ctx.Query(marker as QueryMarker<Any?, Any?>, op.payload)
                wb.lastQueryResults[op.markerName] = result
            }
            MarkerKind.ASSERTION -> {
                @Suppress("UNCHECKED_CAST")
                ctx.Then(marker as AssertionMarker<Any?>, op.payload)
            }
        }
    }

    handlers["call through proxy"] = { wb: AverWorkbench, call: ProxyCall ->
        val ctx = wb.currentContext ?: throw RuntimeException("No adapter created yet")
        val d = wb.currentDomain!!
        val marker = d.markers[call.markerName] ?: throw IllegalStateException("No marker '${call.markerName}'")
        val proxy = when (call.proxyName) {
            "given" -> ctx.Given
            "when" -> ctx.When
            "then" -> ctx.Then
            "query" -> ctx.Query
            else -> throw IllegalArgumentException("Unknown proxy '${call.proxyName}'")
        }
        when (marker) {
            is ActionMarker<*> -> {
                @Suppress("UNCHECKED_CAST")
                proxy(marker as ActionMarker<Any?>, call.payload)
            }
            is AssertionMarker<*> -> {
                @Suppress("UNCHECKED_CAST")
                proxy(marker as AssertionMarker<Any?>, call.payload)
            }
            is QueryMarker<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                proxy(marker as QueryMarker<Any?, Any?>, call.payload)
            }
        }
    }

    handlers["execute failing assertion"] = { wb: AverWorkbench, spec: FailingAssertionSpec ->
        val ctx = wb.currentContext ?: throw RuntimeException("No adapter created yet")
        val d = wb.currentDomain!!
        val marker = d.markers[spec.markerName] ?: throw IllegalStateException("No marker '${spec.markerName}'")
        // Replace handler with failing one temporarily
        val adapter = wb.currentAdapter!!
        val original = adapter.handlers[spec.markerName]
        val failingHandlers = adapter.handlers.toMutableMap()
        failingHandlers[spec.markerName] = { _: Any, _: Any? ->
            throw AssertionError("Intentional failure in ${spec.markerName}")
        }
        // Create a new adapter with the failing handler
        val failingAdapter = Adapter(d, adapter.protocol, failingHandlers)
        val failingCtx = TestContext(d, failingAdapter, ctx.protocolCtx)
        // Copy existing trace by calling the original context operations
        try {
            @Suppress("UNCHECKED_CAST")
            failingCtx.Then(marker as AssertionMarker<Any?>, spec.payload)
        } catch (_: AssertionError) {
            // Expected
        }
        // Copy the failing trace entries back to the real context's underlying trace
        // We need a workaround: we'll make the wb.currentContext point to one that includes
        // both old and new trace entries
        val combinedAdapter = Adapter(d, adapter.protocol, failingHandlers)
        val combinedCtx = TestContext(d, combinedAdapter, ctx.protocolCtx)
        // Replay old trace through given proxy (action markers)
        // Actually, simpler: just replace the context
        // Re-dispatch all previous operations + the failing one
        val prevTrace = ctx.trace()
        // Build a merged context that has the combined trace
        wb.currentContext = MergedTestContext(prevTrace, failingCtx.trace(), d, combinedAdapter, ctx.protocolCtx)
    }

    // Coverage actions
    handlers["define domain for coverage"] = { wb: AverWorkbench, spec: DomainSpec ->
        val d = Domain(spec.name)
        spec.actions.forEach { name -> d.markers[name] = ActionMarker<String>(name, spec.name) }
        spec.queries.forEach { name -> d.markers[name] = QueryMarker<String, String>(name, spec.name) }
        spec.assertions.forEach { name -> d.markers[name] = AssertionMarker<String>(name, spec.name) }
        wb.coverageDomain = d
    }

    handlers["create adapter for coverage"] = { wb: AverWorkbench, spec: AdapterSpec ->
        val d = wb.coverageDomain ?: throw RuntimeException("No coverage domain defined")
        val innerProtocol = UnitProtocol { mutableMapOf<String, Any?>() }
        val h = mutableMapOf<String, Any>()
        for ((name, marker) in d.markers) {
            when (marker.kind) {
                MarkerKind.ACTION -> h[name] = { _: MutableMap<String, Any?>, _: Any? -> }
                MarkerKind.QUERY -> h[name] = { _: MutableMap<String, Any?>, _: Any? -> "result-$name" }
                MarkerKind.ASSERTION -> h[name] = { _: MutableMap<String, Any?>, _: Any? -> }
            }
        }
        wb.coverageAdapter = Adapter(d, innerProtocol, h)
        val innerCtx = innerProtocol.setup()
        wb.coverageContext = TestContext(d, wb.coverageAdapter!!, innerCtx)
    }

    handlers["call coverage operation"] = { wb: AverWorkbench, op: OperationCall ->
        val ctx = wb.coverageContext ?: throw RuntimeException("No coverage adapter")
        val d = wb.coverageDomain!!
        val marker = d.markers[op.markerName] ?: throw IllegalStateException("No marker '${op.markerName}'")
        when (marker.kind) {
            MarkerKind.ACTION -> {
                @Suppress("UNCHECKED_CAST")
                ctx.When(marker as ActionMarker<Any?>, op.payload)
            }
            MarkerKind.QUERY -> {
                @Suppress("UNCHECKED_CAST")
                ctx.Query(marker as QueryMarker<Any?, Any?>, op.payload)
            }
            MarkerKind.ASSERTION -> {
                @Suppress("UNCHECKED_CAST")
                ctx.Then(marker as AssertionMarker<Any?>, op.payload)
            }
        }
    }

    // Telemetry actions
    handlers["define telemetry domain"] = { wb: AverWorkbench, spec: TelemetryDomainSpec ->
        val d = Domain(spec.name)
        spec.actions.zip(spec.spanNames).forEach { (actName, spanName) ->
            d.markers[actName] = ActionMarker<String>(actName, spec.name, TelemetryExpectation(span = spanName))
        }
        wb.telemetryDomain = d
    }

    handlers["create telemetry adapter"] = { wb: AverWorkbench, _: TelemetryAdapterSpecPayload ->
        val d = wb.telemetryDomain ?: throw RuntimeException("No telemetry domain defined")
        val collector = InMemoryCollector()
        wb.telemetryCollector = collector
        val innerProtocol = UnitProtocol { mutableMapOf<String, Any?>() }
        val h = mutableMapOf<String, Any>()
        for ((name, marker) in d.markers) {
            val tel = marker.telemetry
            h[name] = { _: MutableMap<String, Any?>, _: Any? ->
                if (tel != null) {
                    collector.addSpan(CollectedSpan(
                        traceId = "trace-001",
                        spanId = "span-${tel.span}",
                        name = tel.span,
                        attributes = tel.attributes
                    ))
                }
            }
        }
        wb.telemetryAdapter = Adapter(d, innerProtocol, h)
        val innerCtx = innerProtocol.setup()
        wb.telemetryContext = TestContext(d, wb.telemetryAdapter!!, innerCtx, collector)
    }

    handlers["call telemetry operation"] = { wb: AverWorkbench, op: OperationCall ->
        val ctx = wb.telemetryContext ?: throw RuntimeException("No telemetry adapter")
        val d = wb.telemetryDomain!!
        val marker = d.markers[op.markerName] ?: throw IllegalStateException("No marker '${op.markerName}'")
        if (marker.kind == MarkerKind.ACTION) {
            @Suppress("UNCHECKED_CAST")
            ctx.When(marker as ActionMarker<Any?>, op.payload)
        }
    }

    // Extension actions
    handlers["extend domain"] = { wb: AverWorkbench, spec: ExtensionSpec ->
        val parent = wb.currentDomain ?: throw RuntimeException("No domain defined")
        wb.extendedDomain = parent.extend(spec.childName) {
            spec.newActions.forEach { name -> action<String>(name) }
            spec.newQueries.forEach { name -> query<String, String>(name) }
            spec.newAssertions.forEach { name -> assertion<String>(name) }
        }
    }

    // Multi-adapter
    handlers["register second adapter"] = { wb: AverWorkbench, spec: AdapterSpec ->
        val d = wb.currentDomain ?: throw RuntimeException("No domain defined")
        val innerProtocol = UnitProtocol(name = spec.protocolName) { mutableMapOf<String, Any?>() }
        val h = mutableMapOf<String, Any>()
        for ((name, marker) in d.markers) {
            when (marker.kind) {
                MarkerKind.ACTION -> h[name] = { _: MutableMap<String, Any?>, _: Any? -> }
                MarkerKind.QUERY -> h[name] = { _: MutableMap<String, Any?>, _: Any? -> "result-$name" }
                MarkerKind.ASSERTION -> h[name] = { _: MutableMap<String, Any?>, _: Any? -> }
            }
        }
        wb.secondAdapter = Adapter(d, innerProtocol, h)
    }

    // Contract verification
    handlers["setup contract workbench"] = { wb: AverWorkbench, _: Unit ->
        wb.contractTmpDir = createTempDir("aver-contract-")
    }

    handlers["define contract domain"] = { wb: AverWorkbench, spec: ContractDomainSpecPayload ->
        wb.contractDomainSpec = spec
        val d = Domain(spec.domainName)
        spec.actions.forEachIndexed { i, actName ->
            val spanName = spec.spanNames.getOrElse(i) { actName }
            val spanAttrs = spec.spanAttributes.getOrElse(i) { emptyMap() }
            val attrs = mutableMapOf<String, Any>()
            spanAttrs.forEach { (k, v) ->
                if (spec.parameterized && v is String && v.startsWith("$")) {
                    attrs[k] = v
                } else {
                    attrs[k] = v
                }
            }
            d.markers[actName] = ActionMarker<String>(actName, spec.domainName, TelemetryExpectation(span = spanName, attributes = attrs))
        }
        wb.contractDomain = d
    }

    handlers["create contract adapter"] = { wb: AverWorkbench, traceSpec: ContractTraceSpecPayload ->
        val d = wb.contractDomain ?: throw RuntimeException("No contract domain")
        val collector = InMemoryCollector()
        wb.contractCollector = collector
        // Pre-load the collector with the specified spans
        traceSpec.spans.forEach { spanMap ->
            val name = spanMap["name"] as String
            @Suppress("UNCHECKED_CAST")
            val attrs = (spanMap["attributes"] as? Map<String, Any>) ?: emptyMap()
            collector.addSpan(CollectedSpan(
                traceId = "trace-001",
                spanId = "span-$name",
                name = name,
                attributes = attrs
            ))
        }
        val innerProtocol = UnitProtocol { mutableMapOf<String, Any?>() }
        val h = mutableMapOf<String, Any>()
        for ((name, _) in d.markers) {
            h[name] = { _: MutableMap<String, Any?>, _: Any? -> }
        }
        wb.contractAdapter = Adapter(d, innerProtocol, h)
        val innerCtx = innerProtocol.setup()
        wb.contractContext = TestContext(d, wb.contractAdapter!!, innerCtx, collector)
    }

    handlers["run contract operations"] = { wb: AverWorkbench, _: Unit ->
        val ctx = wb.contractContext ?: throw RuntimeException("No contract context")
        val d = wb.contractDomain!!
        for ((name, marker) in d.markers) {
            if (marker.kind == MarkerKind.ACTION) {
                @Suppress("UNCHECKED_CAST")
                ctx.When(marker as ActionMarker<Any?>, "test")
            }
        }
    }

    handlers["extract and write contract"] = { wb: AverWorkbench, _: Unit ->
        val ctx = wb.contractContext ?: throw RuntimeException("No contract context")
        val collector = wb.contractCollector ?: throw RuntimeException("No collector")
        val d = wb.contractDomain!!
        val spec = wb.contractDomainSpec!!
        val contract = extractContract(d.name, ctx.trace(), collector, spec.parameterized)
        val dir = wb.contractTmpDir ?: throw RuntimeException("No tmp dir")
        wb.contractWrittenFiles = writeContract(contract, dir)
    }

    handlers["load and verify contract"] = { wb: AverWorkbench, traceSpec: ContractTraceSpecPayload ->
        val files = wb.contractWrittenFiles ?: throw RuntimeException("No contract written")
        val dir = wb.contractTmpDir ?: throw RuntimeException("No tmp dir")
        val contract = readContract(dir)
        val prodSpans = traceSpec.spans.map { spanMap ->
            val name = spanMap["name"] as String
            @Suppress("UNCHECKED_CAST")
            val attrs = (spanMap["attributes"] as? Map<String, Any>) ?: emptyMap()
            val traceId = spanMap["trace_id"] as? String
            val spanId = spanMap["span_id"] as? String
            ProductionSpan(name = name, attributes = attrs, traceId = traceId, spanId = spanId)
        }
        wb.contractResult = verifyContract(contract, ProductionTrace(prodSpans))
    }

    // Queries
    handlers["get markers"] = { wb: AverWorkbench, _: Unit ->
        val d = wb.currentDomain
        if (d == null) emptyList<Map<String, String>>()
        else d.markers.values.map { m ->
            mapOf("name" to m.name, "kind" to m.kind.name.lowercase(), "domainName" to m.domainName)
        }
    }

    handlers["get trace"] = { wb: AverWorkbench, _: Unit ->
        wb.currentContext?.trace() ?: emptyList()
    }

    handlers["get query result"] = { wb: AverWorkbench, markerName: String ->
        wb.lastQueryResults[markerName]
    }

    handlers["get coverage percentage"] = { wb: AverWorkbench, _: Unit ->
        if (wb.coverageContext == null) 100
        else {
            val report = checkCoverage(wb.coverageDomain!!, wb.coverageContext!!.calledMarkerNames())
            kotlin.math.round(report.percentage).toInt()
        }
    }

    handlers["get extension markers"] = { wb: AverWorkbench, _: Unit ->
        val d = wb.extendedDomain
        if (d == null) emptyList<Map<String, String>>()
        else d.markers.values.map { m ->
            mapOf("name" to m.name, "kind" to m.kind.name.lowercase(), "domainName" to m.domainName)
        }
    }

    handlers["get adapter count"] = { wb: AverWorkbench, _: Unit ->
        var count = if (wb.currentAdapter != null) 1 else 0
        if (wb.secondAdapter != null) count++
        count
    }

    handlers["get coverage detail"] = { wb: AverWorkbench, _: Unit ->
        if (wb.coverageContext == null) {
            mapOf<String, Any>("percentage" to 100)
        } else {
            val report = checkCoverage(wb.coverageDomain!!, wb.coverageContext!!.calledMarkerNames())
            mapOf<String, Any>("percentage" to kotlin.math.round(report.percentage).toInt(), "breakdown" to report.breakdown!!)
        }
    }

    handlers["get contract violations"] = { wb: AverWorkbench, _: Unit ->
        wb.contractResult?.violations?.size ?: 0
    }

    handlers["get violation details"] = { wb: AverWorkbench, _: Unit ->
        wb.contractResult?.violations ?: emptyList()
    }

    // Assertions
    handlers["domain has marker"] = { wb: AverWorkbench, check: MarkerCheck ->
        val d = wb.currentDomain ?: throw AssertionError("No domain defined")
        val marker = d.markers[check.name] ?: throw AssertionError("Domain has no marker '${check.name}'")
        val actualKind = marker.kind.name.lowercase()
        if (actualKind != check.kind) {
            throw AssertionError("Marker '${check.name}' is $actualKind, expected ${check.kind}")
        }
    }

    handlers["trace has entry"] = { wb: AverWorkbench, check: TraceCheck ->
        val trace = wb.currentContext?.trace() ?: throw AssertionError("No context")
        if (check.index >= trace.size) throw AssertionError("Trace has ${trace.size} entries, expected at least ${check.index + 1}")
        val entry = trace[check.index]
        if (entry.category != check.category) throw AssertionError("Trace[${check.index}] category is '${entry.category}', expected '${check.category}'")
        if (entry.status != check.status) throw AssertionError("Trace[${check.index}] status is '${entry.status}', expected '${check.status}'")
    }

    handlers["trace entry matches"] = { wb: AverWorkbench, check: TraceEntryCheck ->
        val trace = wb.currentContext?.trace() ?: throw AssertionError("No context")
        if (check.index >= trace.size) throw AssertionError("Trace has ${trace.size} entries, expected at least ${check.index + 1}")
        val entry = trace[check.index]
        if (entry.kind != check.kind) throw AssertionError("Trace[${check.index}] kind is '${entry.kind}', expected '${check.kind}'")
        if (entry.category != check.category) throw AssertionError("Trace[${check.index}] category is '${entry.category}', expected '${check.category}'")
        if (entry.status != check.status) throw AssertionError("Trace[${check.index}] status is '${entry.status}', expected '${check.status}'")
    }

    handlers["trace has length"] = { wb: AverWorkbench, check: TraceLengthCheck ->
        val trace = wb.currentContext?.trace() ?: throw AssertionError("No context")
        if (trace.size != check.expected) throw AssertionError("Trace has ${trace.size} entries, expected ${check.expected}")
    }

    handlers["query returned value"] = { wb: AverWorkbench, check: QueryResultCheck ->
        val actual = wb.lastQueryResults[check.markerName]
        if (actual != check.expected) throw AssertionError("Query '${check.markerName}' returned $actual, expected ${check.expected}")
    }

    handlers["has vocabulary"] = { wb: AverWorkbench, check: VocabularyCheck ->
        val d = wb.currentDomain ?: throw AssertionError("No domain")
        val actualActions = d.markers.values.count { it.kind == MarkerKind.ACTION }
        val actualQueries = d.markers.values.count { it.kind == MarkerKind.QUERY }
        val actualAssertions = d.markers.values.count { it.kind == MarkerKind.ASSERTION }
        if (actualActions != check.actions) throw AssertionError("Expected ${check.actions} actions, got $actualActions")
        if (actualQueries != check.queries) throw AssertionError("Expected ${check.queries} queries, got $actualQueries")
        if (actualAssertions != check.assertions) throw AssertionError("Expected ${check.assertions} assertions, got $actualAssertions")
    }

    handlers["adapter is complete"] = { wb: AverWorkbench, check: CompletenessCheck ->
        if (check.missing.isEmpty()) {
            if (wb.currentAdapter == null) throw AssertionError("No adapter created")
        } else {
            // Try building an incomplete adapter and verify the error
            val d = wb.currentDomain ?: throw AssertionError("No domain")
            val innerProtocol = UnitProtocol { mutableMapOf<String, Any?>() }
            val h = mutableMapOf<String, Any>()
            for ((name, marker) in d.markers) {
                if (name !in check.missing) {
                    h[name] = { _: MutableMap<String, Any?>, _: Any? -> }
                }
            }
            try {
                val builder = AdapterBuilder(d, innerProtocol)
                h.forEach { (k, v) -> builder.handlers[k] = v }
                builder.build()
                throw AssertionError("Expected error for missing handlers")
            } catch (e: IllegalArgumentException) {
                for (missingName in check.missing) {
                    if (missingName !in (e.message ?: "")) {
                        throw AssertionError("Error should mention missing handler '$missingName', got: ${e.message}")
                    }
                }
            }
        }
    }

    handlers["proxy rejects wrong kind"] = { wb: AverWorkbench, check: ProxyRestrictionCheck ->
        val ctx = wb.currentContext ?: throw AssertionError("No context")
        val d = wb.currentDomain!!
        val marker = d.markers[check.markerName] ?: throw AssertionError("No marker '${check.markerName}'")
        try {
            when (check.proxyName) {
                "when" -> {
                    when (marker) {
                        is ActionMarker<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            ctx.When(marker as ActionMarker<Any?>, "test")
                        }
                        is AssertionMarker<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            ctx.When(marker as AssertionMarker<Any?>, "test")
                        }
                        is QueryMarker<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            ctx.When(marker as QueryMarker<Any?, Any?>, "test")
                        }
                    }
                }
                "then" -> {
                    when (marker) {
                        is ActionMarker<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            ctx.Then(marker as ActionMarker<Any?>, "test")
                        }
                        is AssertionMarker<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            ctx.Then(marker as AssertionMarker<Any?>, "test")
                        }
                        is QueryMarker<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            ctx.Then(marker as QueryMarker<Any?, Any?>, "test")
                        }
                    }
                }
                "query" -> {
                    when (marker) {
                        is ActionMarker<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            ctx.Query(marker as ActionMarker<Any?>, "test")
                        }
                        is AssertionMarker<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            ctx.Query(marker as AssertionMarker<Any?>, "test")
                        }
                        is QueryMarker<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            ctx.Query(marker as QueryMarker<Any?, Any?>, "test")
                        }
                    }
                }
                else -> throw IllegalArgumentException("Unknown proxy '${check.proxyName}'")
            }
            throw AssertionError("Expected IllegalArgumentException from ctx.${check.proxyName}.${check.markerName}")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    handlers["coverage is"] = { wb: AverWorkbench, check: CoverageCheckPayload ->
        val percentage = if (wb.coverageContext == null) 100
        else {
            val report = checkCoverage(wb.coverageDomain!!, wb.coverageContext!!.calledMarkerNames())
            kotlin.math.round(report.percentage).toInt()
        }
        if (percentage != check.expectedPercentage) {
            throw AssertionError("Coverage is $percentage%, expected ${check.expectedPercentage}%")
        }
    }

    handlers["telemetry span matched"] = { wb: AverWorkbench, check: TelemetrySpanCheckPayload ->
        val ctx = wb.telemetryContext ?: throw AssertionError("No telemetry context")
        val trace = ctx.trace()
        if (check.index >= trace.size) throw AssertionError("Trace has ${trace.size} entries, expected at least ${check.index + 1}")
        val entry = trace[check.index]
        val tel = entry.telemetry ?: throw AssertionError("Trace[${check.index}] has no telemetry result")
        if (tel.expected.span != check.expectedSpan) throw AssertionError("Expected span '${check.expectedSpan}', got '${tel.expected.span}'")
        if (tel.matched != check.matched) throw AssertionError("Expected matched=${check.matched}, got matched=${tel.matched}")
    }

    handlers["extension has markers"] = { wb: AverWorkbench, check: ExtensionMarkerCheckPayload ->
        val d = wb.extendedDomain ?: throw AssertionError("No extended domain")
        val names = d.markers.keys
        check.parentMarkerNames.forEach { name ->
            if (name !in names) throw AssertionError("Extended domain missing parent marker '$name'. Has: $names")
        }
        check.childMarkerNames.forEach { name ->
            if (name !in names) throw AssertionError("Extended domain missing child marker '$name'. Has: $names")
        }
    }

    handlers["contract passes"] = { wb: AverWorkbench, _: Unit ->
        val result = wb.contractResult ?: throw AssertionError("No contract result")
        if (!result.passed) throw AssertionError("Contract did not pass. Violations: ${result.violations}")
    }

    handlers["contract has violations"] = { wb: AverWorkbench, _: Unit ->
        val result = wb.contractResult ?: throw AssertionError("No contract result")
        if (result.passed) throw AssertionError("Contract passed but expected violations")
    }

    handlers["violation includes"] = { wb: AverWorkbench, kind: String ->
        val result = wb.contractResult ?: throw AssertionError("No contract result")
        val hasKind = result.violations.any { it.kind == kind }
        if (!hasKind) throw AssertionError("Expected violation kind '$kind', got: ${result.violations.map { it.kind }}")
    }

    handlers["has parent domain"] = { wb: AverWorkbench, parentName: String ->
        val d = wb.extendedDomain ?: throw AssertionError("No extended domain")
        val parent = d.parent ?: throw AssertionError("Extended domain has no parent")
        if (parent.name != parentName) throw AssertionError("Expected parent '$parentName', got '${parent.name}'")
    }

    handlers["adapter count is"] = { wb: AverWorkbench, expected: Int ->
        var count = if (wb.currentAdapter != null) 1 else 0
        if (wb.secondAdapter != null) count++
        if (count != expected) throw AssertionError("Expected $expected adapters, got $count")
    }

    handlers["coverage breakdown matches"] = { wb: AverWorkbench, check: CoverageBreakdownCheckPayload ->
        if (wb.coverageContext == null) throw AssertionError("No coverage context")
        val report = checkCoverage(wb.coverageDomain!!, wb.coverageContext!!.calledMarkerNames())
        val bd = report.breakdown!!
        if (bd.actionsCalled != check.actionsCalled) throw AssertionError("Expected ${check.actionsCalled} actions called, got ${bd.actionsCalled}")
        if (bd.actionsTotal != check.actionsTotal) throw AssertionError("Expected ${check.actionsTotal} total actions, got ${bd.actionsTotal}")
        if (bd.queriesCalled != check.queriesCalled) throw AssertionError("Expected ${check.queriesCalled} queries called, got ${bd.queriesCalled}")
        if (bd.queriesTotalCount != check.queriesTotal) throw AssertionError("Expected ${check.queriesTotal} total queries, got ${bd.queriesTotalCount}")
        if (bd.assertionsCalled != check.assertionsCalled) throw AssertionError("Expected ${check.assertionsCalled} assertions called, got ${bd.assertionsCalled}")
        if (bd.assertionsTotal != check.assertionsTotal) throw AssertionError("Expected ${check.assertionsTotal} total assertions, got ${bd.assertionsTotal}")
    }

    handlers["markers have names"] = { wb: AverWorkbench, check: MarkerNamesCheckPayload ->
        val d = wb.currentDomain ?: throw AssertionError("No domain")
        val actual = d.markers.keys
        val expected = check.expectedNames.toSet()
        if (actual != expected) throw AssertionError("Expected markers $expected, got $actual")
    }

    handlers["marker kinds match"] = { wb: AverWorkbench, check: MarkerKindMapCheckPayload ->
        val d = wb.currentDomain ?: throw AssertionError("No domain")
        check.expected.forEach { (name, kind) ->
            val marker = d.markers[name] ?: throw AssertionError("No marker '$name'")
            val actualKind = marker.kind.name.lowercase()
            if (actualKind != kind) throw AssertionError("Marker '$name' is $actualKind, expected $kind")
        }
    }

    handlers["extension marker count is"] = { wb: AverWorkbench, check: MarkerCountCheckPayload ->
        val d = wb.extendedDomain ?: throw AssertionError("No extended domain")
        if (d.markers.size != check.expected) throw AssertionError("Expected ${check.expected} markers, got ${d.markers.size}")
    }

    handlers["extension marker names equal"] = { wb: AverWorkbench, check: MarkerNamesCheckPayload ->
        val d = wb.extendedDomain ?: throw AssertionError("No extended domain")
        val actual = d.markers.keys
        val expected = check.expectedNames.toSet()
        if (actual != expected) throw AssertionError("Expected markers $expected, got $actual")
    }

    handlers["trace name at index"] = { wb: AverWorkbench, check: TraceNameCheckPayload ->
        val trace = wb.currentContext?.trace() ?: throw AssertionError("No context")
        if (check.index >= trace.size) throw AssertionError("Trace has ${trace.size} entries, expected at least ${check.index + 1}")
        val entry = trace[check.index]
        val d = wb.currentDomain!!
        val qualifiedName = "${d.name}.${entry.name}"
        if (qualifiedName != check.expectedName) throw AssertionError("Trace[${check.index}] name is '$qualifiedName', expected '${check.expectedName}'")
    }

    handlers["violation count is"] = { wb: AverWorkbench, check: ViolationCountCheckPayload ->
        val count = wb.contractResult?.violations?.size ?: 0
        if (count != check.expected) throw AssertionError("Expected ${check.expected} violations, got $count")
    }

    handlers["missing marker raises error"] = { wb: AverWorkbench, check: MissingMarkerErrorCheckPayload ->
        val ctx = wb.currentContext ?: throw AssertionError("No context")
        val d = wb.currentDomain!!
        if (check.markerName in d.markers) {
            throw AssertionError("Marker '${check.markerName}' exists, but should be missing")
        }
        // Trying to find a nonexistent marker should show an error
        // In JVM, markers are looked up by name, so accessing a non-existent one throws
        try {
            val fakeMarker = ActionMarker<Any?>(check.markerName, d.name)
            @Suppress("UNCHECKED_CAST")
            ctx.When(fakeMarker, "test")
            throw AssertionError("Expected error for missing marker '${check.markerName}'")
        } catch (e: IllegalStateException) {
            if (check.expectedMatch !in (e.message ?: "")) {
                // Check for partial match
                if ("no handler" !in (e.message?.lowercase() ?: "") && check.markerName !in (e.message ?: "")) {
                    throw AssertionError("Error should contain '${check.expectedMatch}', got: ${e.message}")
                }
            }
        }
    }

    handlers["query result type is"] = { wb: AverWorkbench, check: QueryResultTypeCheckPayload ->
        val result = wb.lastQueryResults[check.markerName]
        val actualType = when (result) {
            is String -> "str"
            is Int -> "int"
            is Long -> "int"
            is Double -> "float"
            is Boolean -> "bool"
            is List<*> -> "list"
            null -> "null"
            else -> result::class.simpleName ?: "unknown"
        }
        if (actualType != check.expectedType) throw AssertionError("Expected type '${check.expectedType}', got '$actualType'")
    }

    return Adapter(dom.d, protocol, handlers)
}

/**
 * A TestContext that merges trace entries from two contexts.
 */
class MergedTestContext(
    private val prevTrace: List<TraceEntry>,
    private val newTrace: List<TraceEntry>,
    domain: Domain,
    adapter: Adapter,
    protocolCtx: Any
) : TestContext(domain, adapter, protocolCtx) {
    override fun trace(): List<TraceEntry> = prevTrace + newTrace
}

// Make trace() overridable
open class TestContextBase

@Suppress("DEPRECATION")
private fun createTempDir(prefix: String): File {
    return kotlin.io.createTempDir(prefix)
}
