package io.github.corbym.dokker

interface DokkerProperties {
    val name: String
    val networks: List<String>
    var expose: List<String>
    var env: List<String>
    var publishedPorts: List<String>
    var image: String
    var version: String?
    var command: String?
    var memory: String?
    var hostname: String?
    var user: String?
    val options: Array<out Option>
}

enum class OptionType(val option: String) {
    DETACH("d"),
    INTERACTIVE("i")
}

data class Option(val type: OptionType, val enabled: Boolean)