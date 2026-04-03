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
 * Write a contract to a JSON file.
 */
fun writeContract(contract: BehavioralContract, dir: File): File {
    val file = File(dir, "${contract.domain}.contract.json")
    val sb = StringBuilder()
    sb.appendLine("{")
    sb.appendLine("""  "domain": "${contract.domain}",""")
    sb.appendLine("""  "entries": [""")
    contract.entries.forEachIndexed { i, entry ->
        sb.appendLine("    {")
        sb.appendLine("""      "testName": "${entry.testName}",""")
        sb.appendLine("""      "spans": [""")
        entry.spans.forEachIndexed { j, span ->
            sb.appendLine("        {")
            sb.appendLine("""          "name": "${span.name}"""")
            if (span.attributes.isNotEmpty()) {
                sb.appendLine(",")
                sb.appendLine("""          "attributes": {""")
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
                sb.appendLine("          }")
            }
            sb.appendLine("        }" + if (j < entry.spans.size - 1) "," else "")
        }
        sb.appendLine("      ]")
        sb.appendLine("    }" + if (i < contract.entries.size - 1) "," else "")
    }
    sb.appendLine("  ]")
    sb.appendLine("}")
    file.writeText(sb.toString())
    return file
}

/**
 * Read a contract from a JSON file (simplified parser).
 */
fun readContract(file: File): BehavioralContract {
    val text = file.readText()
    // Simplified JSON parse: extract domain and entries
    val domainNameMatch = Regex(""""domain"\s*:\s*"([^"]+)"""").find(text)
        ?: throw IllegalStateException("No domain in contract file")
    val domainName = domainNameMatch.groupValues[1]

    val entries = mutableListOf<ContractEntry>()
    val entryPattern = Regex(""""testName"\s*:\s*"([^"]+)"""")
    val spanPattern = Regex(""""name"\s*:\s*"([^"]+)"""")
    val literalPattern = Regex(""""(\w[\w.]*?)"\s*:\s*\{\s*"kind"\s*:\s*"literal"\s*,\s*"value"\s*:\s*("([^"]*)"|([\d.]+))\s*\}""")
    val correlatedPattern = Regex(""""(\w[\w.]*?)"\s*:\s*\{\s*"kind"\s*:\s*"correlated"\s*,\s*"symbol"\s*:\s*"([^"]+)"\s*\}""")

    // Split by entry blocks
    val entryBlocks = text.split(""""testName"""").drop(1)
    for (block in entryBlocks) {
        val opMatch = Regex(""":\s*"([^"]+)"""").find(block) ?: continue
        val opName = opMatch.groupValues[1]

        val spanMatches = spanPattern.findAll(block).toList()
        // Skip the first match if it matches the operation name itself - get span names from "spans" blocks
        val spanNames = spanMatches.map { it.groupValues[1] }

        val spans = spanNames.map { spanName ->
            val attrs = mutableMapOf<String, AttributeBinding>()
            literalPattern.findAll(block).forEach { m ->
                val attrName = m.groupValues[1]
                if (attrName != "kind") {
                    val strVal = m.groupValues[3]
                    val numVal = m.groupValues[4]
                    val value: Any = if (strVal.isNotEmpty()) strVal
                    else numVal.toDoubleOrNull()?.let { if (it == it.toLong().toDouble()) it.toLong() else it } ?: numVal
                    attrs[attrName] = AttributeBinding(kind = "literal", value = value)
                }
            }
            correlatedPattern.findAll(block).forEach { m ->
                val attrName = m.groupValues[1]
                attrs[attrName] = AttributeBinding(kind = "correlated", symbol = m.groupValues[2])
            }
            SpanExpectation(name = spanName, attributes = attrs)
        }
        entries.add(ContractEntry(testName = opName, spans = spans))
    }

    return BehavioralContract(domain = domainName, entries = entries)
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
