package io.github.corbym.dokker

class DokkerRunCommandBuilder(
    override val process: String,
    override val name: String,
    override val networks: List<String>,
    override var expose: List<String>,
    override var env: List<String>,
    override var publishedPorts: List<String>,
    override var image: String,
    override var version: String? = null,
    override var command: String? = null,
    override var memory: String? = null,
    override var hostname: String? = null,
    override var user: String? = null,
    override vararg val options: Option,
) : DokkerProperties {
    fun buildRunCommand(): String = "$process run${buildOptions()} ${buildImage()}${command?.prefix(" ") ?: ""}"
    private fun buildOptions() = listOf(
        listOf(buildFlags()),
        buildName(),
        buildExpose(),
        buildPorts(),
        buildEnv(),
        buildNetworks(),
        buildMemoryLimit(),
        buildHostName(),
        buildUser(),
    ).flatten()
        .filter { it.isNotEmpty() }
        .joinToString(separator = " ", prefix = " ")

    private fun buildMemoryLimit(): List<String> =
        if (memory != null)
            listOf("--memory $memory")
        else emptyList()

    private fun buildHostName(): List<String> =
        if (hostname != null)
            listOf("--hostname $hostname")
        else emptyList()

    private fun buildImage() = "$image${version?.prefix(":") ?: ""}"

    private fun buildExpose(): List<String> = expose.map { "--expose $it" }
    private fun buildEnv(): List<String> = env.map { "--env $it" }

    private fun buildName(): List<String> = listOf("--name $name")

    private fun buildNetworks(): List<String> = networks.map { "--network $it" }

    private fun buildFlags(): String {
        val enabledOptions = options.filter { it.enabled }
        return if (enabledOptions.isNotEmpty()) enabledOptions.joinToString(
            prefix = "-",
            separator = ""
        ) { it.type.option } else ""
    }

    private fun buildPorts() = publishedPorts.map { "-p $it" }

    private fun buildUser(): List<String> = if (user != null) listOf("--user $user") else emptyList()
}

fun String?.prefix(prefix: String): String = "$prefix$this"