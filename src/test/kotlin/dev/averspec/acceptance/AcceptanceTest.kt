package dev.averspec.acceptance

import dev.averspec.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Acceptance tests: Aver tests itself.
 *
 * These tests define a domain that models Aver's own core concepts,
 * then use Aver's suite/adapter machinery to verify behavior.
 * All assertions go through domain operations (ctx.then), never raw assertEquals.
 */
class AcceptanceTest {

    // --- Context: a mini in-memory model of "domain + adapter + suite" ---

    data class AverCtx(
        var domainName: String? = null,
        val markerNames: MutableList<String> = mutableListOf(),
        val markerKinds: MutableMap<String, MarkerKind> = mutableMapOf(),
        var adapterBuilt: Boolean = false,
        var adapterError: String? = null,
        var handlerCount: Int = 0,
        var suiteRan: Boolean = false,
        val traceLog: MutableList<TraceEntry> = mutableListOf(),
        var lastQueryResult: Any? = null,
        var coverageReport: CoverageReport? = null,
        var proxyError: String? = null
    )

    // --- Domain: the vocabulary of Aver's own behavior ---

    private val createDomain = ActionMarker<String>("create domain", "aver-core")
    private val addActionMarker = ActionMarker<String>("add action marker", "aver-core")
    private val addQueryMarker = ActionMarker<String>("add query marker", "aver-core")
    private val addAssertionMarker = ActionMarker<String>("add assertion marker", "aver-core")
    private val buildAdapter = ActionMarker<Unit>("build adapter", "aver-core")
    private val buildAdapterMissingHandlers = ActionMarker<Unit>("build adapter missing handlers", "aver-core")
    private val runSuiteTest = ActionMarker<Unit>("run suite test", "aver-core")
    private val dispatchGiven = ActionMarker<String>("dispatch given", "aver-core")
    private val dispatchWhen = ActionMarker<String>("dispatch when", "aver-core")
    private val dispatchQuery = ActionMarker<String>("dispatch query", "aver-core")
    private val dispatchThenOnAction = ActionMarker<Unit>("dispatch then on action", "aver-core")
    private val computeCoverage = ActionMarker<Unit>("compute coverage", "aver-core")

    private val domainHasName = AssertionMarker<String>("domain has name", "aver-core")
    private val markerExists = AssertionMarker<String>("marker exists", "aver-core")
    private val markerHasKind = AssertionMarker<Pair<String, MarkerKind>>("marker has kind", "aver-core")
    private val adapterIsBuilt = AssertionMarker<Boolean>("adapter is built", "aver-core")
    private val adapterErrorContains = AssertionMarker<String>("adapter error contains", "aver-core")
    private val suiteTestRan = AssertionMarker<Boolean>("suite test ran", "aver-core")
    private val traceHasEntries = AssertionMarker<Int>("trace has entries", "aver-core")
    private val traceEntryCategory = AssertionMarker<Pair<Int, String>>("trace entry category", "aver-core")
    private val traceEntryName = AssertionMarker<Pair<Int, String>>("trace entry name", "aver-core")
    private val queryResultEquals = AssertionMarker<Any>("query result equals", "aver-core")
    private val coverageIsComplete = AssertionMarker<Boolean>("coverage is complete", "aver-core")
    private val coveragePercentage = AssertionMarker<Double>("coverage percentage", "aver-core")
    private val proxyErrorContains = AssertionMarker<String>("proxy error contains", "aver-core")

    private val averDomain = Domain("aver-core").also { d ->
        // Register all markers
        d.markers["create domain"] = createDomain
        d.markers["add action marker"] = addActionMarker
        d.markers["add query marker"] = addQueryMarker
        d.markers["add assertion marker"] = addAssertionMarker
        d.markers["build adapter"] = buildAdapter
        d.markers["build adapter missing handlers"] = buildAdapterMissingHandlers
        d.markers["run suite test"] = runSuiteTest
        d.markers["dispatch given"] = dispatchGiven
        d.markers["dispatch when"] = dispatchWhen
        d.markers["dispatch query"] = dispatchQuery
        d.markers["dispatch then on action"] = dispatchThenOnAction
        d.markers["compute coverage"] = computeCoverage
        d.markers["domain has name"] = domainHasName
        d.markers["marker exists"] = markerExists
        d.markers["marker has kind"] = markerHasKind
        d.markers["adapter is built"] = adapterIsBuilt
        d.markers["adapter error contains"] = adapterErrorContains
        d.markers["suite test ran"] = suiteTestRan
        d.markers["trace has entries"] = traceHasEntries
        d.markers["trace entry category"] = traceEntryCategory
        d.markers["trace entry name"] = traceEntryName
        d.markers["query result equals"] = queryResultEquals
        d.markers["coverage is complete"] = coverageIsComplete
        d.markers["coverage percentage"] = coveragePercentage
        d.markers["proxy error contains"] = proxyErrorContains
    }

    private val protocol = UnitProtocol { AverCtx() }

    private val adapter = Adapter(averDomain, protocol, buildHandlers())

    private fun buildHandlers(): Map<String, Any> {
        val h = mutableMapOf<String, Any>()
        val action = { name: String, fn: (AverCtx, Any?) -> Unit -> h[name] = fn }

        action("create domain") { ctx, p -> ctx.domainName = p as String }
        action("add action marker") { ctx, p ->
            val name = p as String; ctx.markerNames.add(name); ctx.markerKinds[name] = MarkerKind.ACTION
        }
        action("add query marker") { ctx, p ->
            val name = p as String; ctx.markerNames.add(name); ctx.markerKinds[name] = MarkerKind.QUERY
        }
        action("add assertion marker") { ctx, p ->
            val name = p as String; ctx.markerNames.add(name); ctx.markerKinds[name] = MarkerKind.ASSERTION
        }
        action("build adapter") { ctx, _ ->
            ctx.adapterBuilt = true; ctx.handlerCount = ctx.markerNames.size
        }
        action("build adapter missing handlers") { ctx, _ ->
            try {
                val d = domain("temp") { action<String>("a"); assertion<Int>("b") }
                implement<Unit>(d, UnitProtocol { Unit }) {}
                ctx.adapterError = null
            } catch (e: IllegalArgumentException) {
                ctx.adapterError = e.message
            }
        }
        action("run suite test") { ctx, _ -> ctx.suiteRan = true }
        action("dispatch given") { ctx, p ->
            val markerName = p as String
            val d = domain("mini") { action<String>("item") }
            val miniProtocol = UnitProtocol { mutableListOf<String>() }
            val miniMarker = d.markers["item"] as ActionMarker<String>
            val a = implement<MutableList<String>>(d, miniProtocol) {
                onAction(miniMarker) { list: MutableList<String>, v: String -> list.add(v) }
            }
            val s = suite(d, a)
            s.test("t") { tc ->
                tc.given(miniMarker, markerName)
                ctx.traceLog.addAll(tc.trace())
            }
        }
        action("dispatch when") { ctx, p ->
            val markerName = p as String
            val d = domain("mini") { action<String>("item") }
            val miniProtocol = UnitProtocol { mutableListOf<String>() }
            val miniMarker = d.markers["item"] as ActionMarker<String>
            val a = implement<MutableList<String>>(d, miniProtocol) {
                onAction(miniMarker) { list: MutableList<String>, v: String -> list.add(v) }
            }
            val s = suite(d, a)
            s.test("t") { tc ->
                tc.`when`(miniMarker, markerName)
                ctx.traceLog.addAll(tc.trace())
            }
        }
        action("dispatch query") { ctx, _ ->
            val d = domain("mini") { query<Unit, Int>("count") }
            val miniProtocol = UnitProtocol { 42 }
            val miniMarker = d.markers["count"] as QueryMarker<Unit, Int>
            val a = implement<Int>(d, miniProtocol) {
                onQuery(miniMarker) { v: Int, _: Unit -> v }
            }
            val s = suite(d, a)
            s.test("t") { tc ->
                ctx.lastQueryResult = tc.query(miniMarker, Unit)
                ctx.traceLog.addAll(tc.trace())
            }
        }
        action("dispatch then on action") { ctx, _ ->
            val d = domain("mini") { action<String>("item") }
            val miniProtocol = UnitProtocol { mutableListOf<String>() }
            val miniMarker = d.markers["item"] as ActionMarker<String>
            val a = implement<MutableList<String>>(d, miniProtocol) {
                onAction(miniMarker) { list: MutableList<String>, v: String -> list.add(v) }
            }
            val s = suite(d, a)
            s.test("t") { tc ->
                try {
                    tc.then(miniMarker, "x")
                } catch (e: IllegalArgumentException) {
                    ctx.proxyError = e.message
                }
            }
        }
        action("compute coverage") { ctx, _ ->
            val d = domain("cov") { action<Unit>("a"); action<Unit>("b"); assertion<Unit>("c") }
            ctx.coverageReport = checkCoverage(d, setOf("a", "b"))
        }
        action("domain has name") { ctx, p ->
            val expected = p as String
            if (ctx.domainName != expected) throw AssertionError("Expected domain name '$expected', got '${ctx.domainName}'")
        }
        action("marker exists") { ctx, p ->
            val name = p as String
            if (name !in ctx.markerNames) throw AssertionError("Expected marker '$name' to exist")
        }
        action("marker has kind") { ctx, p ->
            @Suppress("UNCHECKED_CAST") val pair = p as Pair<String, MarkerKind>
            val (name, kind) = pair
            if (ctx.markerKinds[name] != kind) throw AssertionError("Expected '$name' to have kind $kind, got ${ctx.markerKinds[name]}")
        }
        action("adapter is built") { ctx, p ->
            val expected = p as Boolean
            if (ctx.adapterBuilt != expected) throw AssertionError("Expected adapterBuilt=$expected, got ${ctx.adapterBuilt}")
        }
        action("adapter error contains") { ctx, p ->
            val substr = p as String
            if (ctx.adapterError == null || substr !in ctx.adapterError!!)
                throw AssertionError("Expected adapter error to contain '$substr', got '${ctx.adapterError}'")
        }
        action("suite test ran") { ctx, p ->
            val expected = p as Boolean
            if (ctx.suiteRan != expected) throw AssertionError("Expected suiteRan=$expected, got ${ctx.suiteRan}")
        }
        action("trace has entries") { ctx, p ->
            val count = p as Int
            if (ctx.traceLog.size != count) throw AssertionError("Expected $count trace entries, got ${ctx.traceLog.size}")
        }
        action("trace entry category") { ctx, p ->
            @Suppress("UNCHECKED_CAST") val pair = p as Pair<Int, String>
            val (idx, expected) = pair
            if (ctx.traceLog[idx].category != expected)
                throw AssertionError("Expected trace[$idx].category='$expected', got '${ctx.traceLog[idx].category}'")
        }
        action("trace entry name") { ctx, p ->
            @Suppress("UNCHECKED_CAST") val pair = p as Pair<Int, String>
            val (idx, expected) = pair
            if (ctx.traceLog[idx].name != expected)
                throw AssertionError("Expected trace[$idx].name='$expected', got '${ctx.traceLog[idx].name}'")
        }
        action("query result equals") { ctx, p ->
            if (ctx.lastQueryResult != p) throw AssertionError("Expected query result=$p, got ${ctx.lastQueryResult}")
        }
        action("coverage is complete") { ctx, p ->
            val expected = p as Boolean
            if (ctx.coverageReport?.complete != expected)
                throw AssertionError("Expected coverage complete=$expected, got ${ctx.coverageReport?.complete}")
        }
        action("coverage percentage") { ctx, p ->
            val expected = p as Double
            val actual = ctx.coverageReport?.percentage
            if (actual == null || Math.abs(actual - expected) > 0.01)
                throw AssertionError("Expected coverage ${expected}%, got $actual%")
        }
        action("proxy error contains") { ctx, p ->
            val substr = p as String
            if (ctx.proxyError == null || substr !in ctx.proxyError!!)
                throw AssertionError("Expected proxy error to contain '$substr', got '${ctx.proxyError}'")
        }
        return h
    }

    private val s = suite(averDomain, adapter)

    // --- Tests ---

    @Test
    fun `domain creation sets name`() {
        s.test("domain creation") { ctx ->
            ctx.given(createDomain, "todo-app")
            ctx.then(domainHasName, "todo-app")
        }
    }

    @Test
    fun `action markers are registered with correct kind`() {
        s.test("action marker") { ctx ->
            ctx.given(addActionMarker, "submit order")
            ctx.then(markerExists, "submit order")
            ctx.then(markerHasKind, Pair("submit order", MarkerKind.ACTION))
        }
    }

    @Test
    fun `query markers are registered with correct kind`() {
        s.test("query marker") { ctx ->
            ctx.given(addQueryMarker, "get total")
            ctx.then(markerExists, "get total")
            ctx.then(markerHasKind, Pair("get total", MarkerKind.QUERY))
        }
    }

    @Test
    fun `assertion markers are registered with correct kind`() {
        s.test("assertion marker") { ctx ->
            ctx.given(addAssertionMarker, "has items")
            ctx.then(markerExists, "has items")
            ctx.then(markerHasKind, Pair("has items", MarkerKind.ASSERTION))
        }
    }

    @Test
    fun `adapter builds with all handlers`() {
        s.test("adapter build") { ctx ->
            ctx.given(addActionMarker, "do thing")
            ctx.`when`(buildAdapter, Unit)
            ctx.then(adapterIsBuilt, true)
        }
    }

    @Test
    fun `adapter rejects missing handlers`() {
        s.test("missing handlers") { ctx ->
            ctx.`when`(buildAdapterMissingHandlers, Unit)
            ctx.then(adapterErrorContains, "Missing handlers")
        }
    }

    @Test
    fun `suite runs test block`() {
        s.test("suite runs") { ctx ->
            ctx.`when`(runSuiteTest, Unit)
            ctx.then(suiteTestRan, true)
        }
    }

    @Test
    fun `given dispatches and records trace`() {
        s.test("given trace") { ctx ->
            ctx.`when`(dispatchGiven, "hello")
            ctx.then(traceHasEntries, 1)
            ctx.then(traceEntryCategory, Pair(0, "given"))
        }
    }

    @Test
    fun `when dispatches and records trace`() {
        s.test("when trace") { ctx ->
            ctx.`when`(dispatchWhen, "hello")
            ctx.then(traceHasEntries, 1)
            ctx.then(traceEntryCategory, Pair(0, "when"))
        }
    }

    @Test
    fun `query dispatches and returns value`() {
        s.test("query dispatch") { ctx ->
            ctx.`when`(dispatchQuery, "count")
            ctx.then(queryResultEquals, 42 as Any)
            ctx.then(traceHasEntries, 1)
        }
    }

    @Test
    fun `then rejects action markers`() {
        s.test("proxy restriction") { ctx ->
            ctx.`when`(dispatchThenOnAction, Unit)
            ctx.then(proxyErrorContains, "Cannot use ACTION")
        }
    }

    @Test
    fun `coverage detects uncalled markers`() {
        s.test("coverage partial") { ctx ->
            ctx.`when`(computeCoverage, Unit)
            ctx.then(coverageIsComplete, false)
        }
    }

    @Test
    fun `coverage computes correct percentage`() {
        s.test("coverage pct") { ctx ->
            ctx.`when`(computeCoverage, Unit)
            ctx.then(coveragePercentage, 66.67)
        }
    }
}
