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
    fun buildRunCommand(): List<String> =
        listOf(process, "run") + buildOptions() + listOf(buildImage()) +
            // Command is split on spaces for backwards compatibility; arguments with embedded
            // spaces must be passed via execWithSpacedParameter or exec(command, parameter).
            (command?.split(" ") ?: emptyList())

    private fun buildOptions(): List<String> = listOf(
        buildFlags(),
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

    private fun buildMemoryLimit(): List<String> =
        if (memory != null) listOf("--memory", memory!!) else emptyList()

    private fun buildHostName(): List<String> =
        if (hostname != null) listOf("--hostname", hostname!!) else emptyList()

    private fun buildImage() = "$image${version?.prefix(":") ?: ""}"

    private fun buildExpose(): List<String> = expose.flatMap { listOf("--expose", it) }
    private fun buildEnv(): List<String> = env.flatMap { listOf("--env", it) }

    private fun buildName(): List<String> = listOf("--name", name)

    private fun buildNetworks(): List<String> = networks.flatMap { listOf("--network", it) }

    private fun buildFlags(): List<String> {
        val enabledOptions = options.filter { it.enabled }
        return if (enabledOptions.isNotEmpty())
            listOf(enabledOptions.joinToString(prefix = "-", separator = "") { it.type.option })
        else emptyList()
    }

    private fun buildPorts(): List<String> = publishedPorts.flatMap { listOf("-p", it) }

    private fun buildUser(): List<String> = if (user != null) listOf("--user", user!!) else emptyList()
}

fun String?.prefix(prefix: String): String = "$prefix$this"