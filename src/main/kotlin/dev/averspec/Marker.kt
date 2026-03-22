package dev.averspec

enum class MarkerKind { ACTION, QUERY, ASSERTION }

sealed class Marker(
    val name: String,
    val domainName: String,
    val kind: MarkerKind,
    val telemetry: TelemetryExpectation? = null
)

class ActionMarker<P>(
    name: String,
    domainName: String,
    telemetry: TelemetryExpectation? = null
) : Marker(name, domainName, MarkerKind.ACTION, telemetry)

class QueryMarker<P, R>(
    name: String,
    domainName: String,
    telemetry: TelemetryExpectation? = null
) : Marker(name, domainName, MarkerKind.QUERY, telemetry)

class AssertionMarker<P>(
    name: String,
    domainName: String,
    telemetry: TelemetryExpectation? = null
) : Marker(name, domainName, MarkerKind.ASSERTION, telemetry)
