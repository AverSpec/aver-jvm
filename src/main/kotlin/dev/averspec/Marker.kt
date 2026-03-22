package dev.averspec

enum class MarkerKind { ACTION, QUERY, ASSERTION }

sealed class Marker(val name: String, val domainName: String, val kind: MarkerKind)

class ActionMarker<P>(name: String, domainName: String) : Marker(name, domainName, MarkerKind.ACTION)

class QueryMarker<P, R>(name: String, domainName: String) : Marker(name, domainName, MarkerKind.QUERY)

class AssertionMarker<P>(name: String, domainName: String) : Marker(name, domainName, MarkerKind.ASSERTION)
