package dev.averspec

import java.io.File

/**
 * An attribute binding in a contract: literal value or correlated symbol.
 */
data class AttributeBinding(
    val kind: String, // "literal" or "correlated"
    val value: Any? = null,
    val symbol: String? = null
)

/**
 * A span expectation within a contract.
 */
data class SpanExpectation(
    val name: String,
    val attributes: Map<String, AttributeBinding> = emptyMap()
)

/**
 * A single contract entry: one domain operation's expected telemetry.
 */
data class ContractEntry(
    val testName: String,
    val spans: List<SpanExpectation>
)

/**
 * A behavioral contract extracted from test telemetry.
 */
data class BehavioralContract(
    val domain: String,
    val entries: List<ContractEntry>
)

/**
 * A production span for verification.
 */
data class ProductionSpan(
    val name: String,
    val attributes: Map<String, Any> = emptyMap(),
    val traceId: String? = null,
    val spanId: String? = null
)

/**
 * A production trace (collection of spans).
 */
data class ProductionTrace(
    val spans: List<ProductionSpan>
)

/**
 * A single contract violation.
 */
data class ContractViolation(
    val kind: String, // "missing-span", "literal-mismatch", "correlation-violation", "no-matching-traces"
    val message: String
)

/**
 * Result of verifying a contract against production traces.
 */
data class ConformanceReport(
    val passed: Boolean,
    val violations: List<ContractViolation>
)

/**
 * Extract a behavioral contract from test telemetry.
 */
fun extractContract(
    domainName: String,
    traceEntries: List<TraceEntry>,
    collector: TelemetryCollector,
    parameterized: Boolean = false
): BehavioralContract {
    val entries = traceEntries.mapNotNull { entry ->
        val telemetry = entry.telemetry ?: return@mapNotNull null
        val attrs = mutableMapOf<String, AttributeBinding>()
        telemetry.expected.attributes.forEach { (k, v) ->
            if (parameterized && v is String && v.startsWith("$")) {
                attrs[k] = AttributeBinding(kind = "correlated", symbol = v)
            } else {
                attrs[k] = AttributeBinding(kind = "literal", value = v)
            }
        }
        ContractEntry(
            testName = entry.name,
            spans = listOf(SpanExpectation(name = telemetry.expected.span, attributes = attrs))
        )
    }
    return BehavioralContract(domain = domainName, entries = entries)
}

/**
 * Generate a filename-safe slug from a test name.
 * Lowercase, spaces to hyphens, strip special chars, collapse consecutive hyphens.
 */
fun slugify(text: String): String {
    return text
        .lowercase()
        .replace(Regex("\\s+"), "-")
        .replace(Regex("[^a-z0-9-]"), "")
        .replace(Regex("-{2,}"), "-")
        .trim('-')
}

/**
 * Serialize an entry to a JSON string for a single contract file.
 */
private fun entryToJson(entry: ContractEntry): String {
    val sb = StringBuilder()
    sb.append("{\n")
    sb.append("""      "testName": "${entry.testName}",""")
    sb.append("\n")
    sb.append("""      "spans": [""")
    sb.append("\n")
    entry.spans.forEachIndexed { j, span ->
        sb.append("        {\n")
        sb.append("""          "name": "${span.name}"""")
        if (span.attributes.isNotEmpty()) {
            sb.append(",\n")
            sb.append("""          "attributes": {""")
            sb.append("\n")
            span.attributes.entries.forEachIndexed { k, (attrName, binding) ->
                val comma = if (k < span.attributes.size - 1) "," else ""
                if (binding.kind == "literal") {
                    val valStr = when (binding.value) {
                        is String -> "\"${binding.value}\""
                        else -> "${binding.value}"
                    }
                    sb.appendLine("""            "$attrName": {"kind": "literal", "value": $valStr}$comma""")
                } else {
                    sb.appendLine("""            "$attrName": {"kind": "correlated", "symbol": "${binding.symbol}"}$comma""")
                }
            }
            sb.append("          }\n")
        } else {
            sb.append("\n")
        }
        sb.append("        }" + if (j < entry.spans.size - 1) "," else "")
        sb.append("\n")
    }
    sb.append("      ]\n")
    sb.append("    }")
    return sb.toString()
}

/**
 * Write contract entries as individual JSON files.
 * Creates one {slug}.contract.json per entry under dir/{domain}/.
 * Returns list of written file paths.
 */
fun writeContract(contract: BehavioralContract, dir: File): List<File> {
    val domainDir = File(dir, contract.domain)
    domainDir.mkdirs()

    val now = java.time.Instant.now().toString()
    val paths = mutableListOf<File>()

    for (entry in contract.entries) {
        val slug = slugify(entry.testName)
        val file = File(domainDir, "$slug.contract.json")

        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("""  "version": 1,""")
        sb.appendLine("""  "domain": "${contract.domain}",""")
        sb.appendLine("""  "testName": "${entry.testName}",""")
        sb.appendLine("""  "extractedAt": "$now",""")
        sb.append("""  "entry": """)
        sb.appendLine(entryToJson(entry))
        sb.appendLine("}")
        file.writeText(sb.toString())
        paths.add(file)
    }

    return paths
}

/**
 * Read a single contract file and return its domain and entry.
 */
fun readContractFile(file: File): Pair<String, ContractEntry> {
    val text = file.readText()

    val versionMatch = Regex(""""version"\s*:\s*(\d+)""").find(text)
    val version = versionMatch?.groupValues?.get(1)?.toIntOrNull()
    if (version != 1) {
        throw IllegalStateException("Unsupported contract version $version in ${file.path}")
    }

    val domainMatch = Regex(""""domain"\s*:\s*"([^"]+)"""").find(text)
        ?: throw IllegalStateException("Missing domain in contract file ${file.path}")
    val domain = domainMatch.groupValues[1]

    // Find the "entry" block - everything after "entry": { ... }
    val entryStart = text.indexOf("\"entry\"")
    if (entryStart < 0) throw IllegalStateException("Missing entry in contract file ${file.path}")

    val entryText = text.substring(entryStart)

    val testNameMatch = Regex(""""testName"\s*:\s*"([^"]+)"""").find(entryText)
        ?: throw IllegalStateException("Missing entry.testName in contract file ${file.path}")
    val testName = testNameMatch.groupValues[1]

    val spanPattern = Regex(""""name"\s*:\s*"([^"]+)"""")
    val literalPattern = Regex(""""(\w[\w.]*?)"\s*:\s*\{\s*"kind"\s*:\s*"literal"\s*,\s*"value"\s*:\s*("([^"]*)"|([\d.]+))\s*\}""")
    val correlatedPattern = Regex(""""(\w[\w.]*?)"\s*:\s*\{\s*"kind"\s*:\s*"correlated"\s*,\s*"symbol"\s*:\s*"([^"]+)"\s*\}""")

    val spanNames = spanPattern.findAll(entryText).map { it.groupValues[1] }.toList()

    val spans = spanNames.map { spanName ->
        val attrs = mutableMapOf<String, AttributeBinding>()
        literalPattern.findAll(entryText).forEach { m ->
            val attrName = m.groupValues[1]
            if (attrName != "kind") {
                val strVal = m.groupValues[3]
                val numVal = m.groupValues[4]
                val value: Any = if (strVal.isNotEmpty()) strVal
                else numVal.toDoubleOrNull()?.let { if (it == it.toLong().toDouble()) it.toLong() else it } ?: numVal
                attrs[attrName] = AttributeBinding(kind = "literal", value = value)
            }
        }
        correlatedPattern.findAll(entryText).forEach { m ->
            val attrName = m.groupValues[1]
            attrs[attrName] = AttributeBinding(kind = "correlated", symbol = m.groupValues[2])
        }
        SpanExpectation(name = spanName, attributes = attrs)
    }

    return Pair(domain, ContractEntry(testName = testName, spans = spans))
}

/**
 * Read all contract files from baseDir, grouped by domain.
 * Scans baseDir/{domain}/ for .contract.json files and reconstructs BehavioralContracts.
 */
fun readContract(baseDir: File): BehavioralContract {
    val domainMap = mutableMapOf<String, MutableList<ContractEntry>>()

    if (!baseDir.exists()) {
        return BehavioralContract(domain = "", entries = emptyList())
    }

    val subdirs = baseDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: emptyList()
    for (subdir in subdirs) {
        val files = subdir.listFiles()?.filter { it.name.endsWith(".contract.json") }?.sortedBy { it.name } ?: continue
        for (file in files) {
            val (domain, entry) = readContractFile(file)
            domainMap.getOrPut(domain) { mutableListOf() }.add(entry)
        }
    }

    // Return the first (and typically only) domain's contract
    val firstDomain = domainMap.keys.firstOrNull() ?: return BehavioralContract(domain = "", entries = emptyList())
    return BehavioralContract(domain = firstDomain, entries = domainMap[firstDomain]!!)
}

/**
 * Read all contract files from baseDir, returning all domains.
 */
fun readContracts(baseDir: File): List<BehavioralContract> {
    if (!baseDir.exists()) return emptyList()

    val domainMap = mutableMapOf<String, MutableList<ContractEntry>>()

    val subdirs = baseDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: emptyList()
    for (subdir in subdirs) {
        val files = subdir.listFiles()?.filter { it.name.endsWith(".contract.json") }?.sortedBy { it.name } ?: continue
        for (file in files) {
            val (domain, entry) = readContractFile(file)
            domainMap.getOrPut(domain) { mutableListOf() }.add(entry)
        }
    }

    return domainMap.map { (domain, entries) -> BehavioralContract(domain = domain, entries = entries) }
}

/**
 * Verify a contract against production traces.
 */
fun verifyContract(
    contract: BehavioralContract,
    productionTrace: ProductionTrace
): ConformanceReport {
    val violations = mutableListOf<ContractViolation>()
    val symbolBindings = mutableMapOf<String, Any>() // symbol -> first-seen value

    // Collect all expected span names from the contract
    val allExpectedNames = contract.entries.flatMap { it.spans.map { s -> s.name } }.toSet()
    // Check if ANY expected span name appears in production traces
    val anyExpectedInProd = allExpectedNames.any { name ->
        productionTrace.spans.any { it.name == name }
    }

    for (entry in contract.entries) {
        for (spanExpectation in entry.spans) {
            // Find matching production span by name
            val prodSpan = productionTrace.spans.find { it.name == spanExpectation.name }

            if (prodSpan == null) {
                if (productionTrace.spans.isEmpty()) {
                    violations.add(ContractViolation(
                        kind = "no-matching-traces",
                        message = "No production traces found for span '${spanExpectation.name}'"
                    ))
                } else if (!anyExpectedInProd) {
                    violations.add(ContractViolation(
                        kind = "no-matching-traces",
                        message = "No matching production traces for span '${spanExpectation.name}'"
                    ))
                } else {
                    violations.add(ContractViolation(
                        kind = "missing-span",
                        message = "Expected span '${spanExpectation.name}' not found in production traces"
                    ))
                }
                continue
            }

            // Check attributes
            for ((attrName, binding) in spanExpectation.attributes) {
                val prodValue = prodSpan.attributes[attrName]
                when (binding.kind) {
                    "literal" -> {
                        if (prodValue?.toString() != binding.value?.toString()) {
                            violations.add(ContractViolation(
                                kind = "literal-mismatch",
                                message = "Span '${spanExpectation.name}' attribute '$attrName': expected '${binding.value}', got '$prodValue'"
                            ))
                        }
                    }
                    "correlated" -> {
                        val symbol = binding.symbol!!
                        if (symbol in symbolBindings) {
                            val expected = symbolBindings[symbol]
                            if (prodValue?.toString() != expected?.toString()) {
                                violations.add(ContractViolation(
                                    kind = "correlation-violation",
                                    message = "Symbol '$symbol' bound to '$expected' but span '${spanExpectation.name}' attribute '$attrName' has '$prodValue'"
                                ))
                            }
                        } else {
                            if (prodValue != null) {
                                symbolBindings[symbol] = prodValue
                            }
                        }
                    }
                }
            }
        }
    }

    return ConformanceReport(
        passed = violations.isEmpty(),
        violations = violations
    )
}
