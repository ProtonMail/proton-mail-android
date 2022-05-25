/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
@file:Suppress("PropertyName")

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


// Command
val HELP = "help"
val DASH_HELP = "-$HELP"
val FLAVOUR_LONG = "-flavour"
val FLAVOUR_SHORT = "-f"
val FLAVOURS_ONLY = "-flavours-only"
val WITH_VERSION_LONG = "-withVersion"
val WITH_VERSION_SHORT = "-v"
val FILTER_LONG = "-filter"
val FILTER_NOT_LONG = "-filterNot"
val MERGE_LONG = "-merge"
val MERGE_SHORT = "-m"



// Arguments
val help = hasArg(HELP, DASH_HELP)
val flavour = getArg(FLAVOUR_LONG, FLAVOUR_SHORT)
val flavoursOnly = hasArg(FLAVOURS_ONLY)
val withVersion = hasArg(WITH_VERSION_LONG, WITH_VERSION_SHORT)
val filters = getArg(FILTER_LONG)?.split(",")
val filterNots = getArg(FILTER_NOT_LONG)?.split(",")
val merge = hasArg(MERGE_LONG, MERGE_SHORT)



// Help
if (help) {
    println("""
        
        This script will parse dependencies for 'app' module.
        Dependencies will be extracted from Gradle into a temporary file that will be cached for one day.

        '$FLAVOUR_SHORT' | '$FLAVOUR_LONG'        followed by the name of the flavour we want to inspect
        '$FLAVOURS_ONLY'         for print only available flavours
        '$WITH_VERSION_SHORT' | '$WITH_VERSION_LONG'    for include dependencies versions
        '$FILTER_LONG'                to pick only dependencies containing the declared queries, separated by comma
        '$FILTER_NOT_LONG'             to skip dependencies containing the declared queries, separated by comma
        '$MERGE_LONG' | '$MERGE_SHORT'          for merge dependencies from all the flavours
        
    """.trimIndent())

    exitProcess(0)
}


// Setup
/** Find the root dir of our project. `` **./proton-mail-android `` */
val ROOT_DIR = with(StringBuilder(File("").absolutePath)) {
    if (last() == File.separatorChar) deleteCharAt(lastIndex)

    var current = ""
    while (current != "proton-mail-android") {
        val lastSeparatorIndex = indexOfLast { it == File.separatorChar }
        current = substring(lastSeparatorIndex + 1..lastIndex)
        delete(lastSeparatorIndex, length)
    }

    val path = toString() + File.separator + current
    return@with File(path)
}
val INPUT = File(ROOT_DIR,"scripts/extract_dependencies/raw_dependencies.txt")
if (!INPUT.exists() || INPUT.lastModified() < System.currentTimeMillis() - 24 * 60 * 60 * 1000) { // one day cache
    "./gradlew :app:dependencies > scripts/extract_dependencies/raw_dependencies.txt".runCommand(
        // Using root directory
        ROOT_DIR
    )
}
val ROOT_IDENTIFIER = "+--- "
val FLAVOR_DIVIDER = "-"
val START_FROM = flavour ?: " "


// Parse
// Key is flavor name
val dependencies = mutableMapOf<String, MutableSet<Dependency>>()
var canStart = flavour == null
var close = false
var lastFlavor = ""
INPUT.forEachLine {

    if (!close) {

        // Start if we reach 'START_FROM'
        @Suppress("ControlFlowWithEmptyBody")
        if (!canStart && it.startsWith(START_FROM)) {
            canStart = true

            // Stop if we have a declared 'flavor' and line is empty
        } else if (flavour != null && it.isBlank()) {
            // Close if started, else skip
            if (canStart) {
                close = true
                return@forEachLine
            }

            // Parse if we can stat
        } else if (canStart) {

            // Identify dependency
            if (it.startsWith(ROOT_IDENTIFIER)) {
                val dep = it.substringAfter(ROOT_IDENTIFIER)
                if (!dep.startsWith("project") && dep.matchFilter(filters) && dep.matchNotFilter(filterNots)) {
                    if (lastFlavor !in dependencies) dependencies[lastFlavor] = mutableSetOf()
                    dependencies[lastFlavor]!! += Dependency(dep)
                }

                // Identify flavor
            } else if (!it.startsWith("|")) {
                lastFlavor = it.substringBefore(FLAVOR_DIVIDER)
            }
        }
    }
}


// Print output
println()
if (flavoursOnly) {
    (dependencies.keys + "").forEach(::println)

} else if (merge) {
    out(dependencies.flatMap { it.value })

} else {
    for ((flavour, deps) in dependencies) {
        println(flavour)
        out(deps)
    }
}

fun out(deps: Collection<Dependency>) {
    for (dep in deps.sorted().withVersion(withVersion))
        println(dep.toString())
    println()
}


// = = = = = = = = = = Tools = = = = = = = = = = //

data class Dependency(val group: String, val module: String, val version: String) : Comparable<Dependency> {

    companion object {
        operator fun invoke(rawString: String): Dependency {
            return try {
                val (group, module, version) = rawString.split(":")
                Dependency(group, module, version)
            } catch (e: Throwable) {
                Dependency("Cannot parse: '$rawString'", "", "")
            }
        }
    }

    override fun compareTo(other: Dependency): Int =
        group.compareTo(other.group) * 10000 +
            module.compareTo(other.module) * 100 +
            version.compareTo(other.version)

    override fun toString() = "$group:$module:$version"
}

fun List<Dependency>.withVersion(b: Boolean) = if (b) this else ignoreVersions()
fun List<Dependency>.ignoreVersions() = map { NoVersionDependency(it.group, it.module) }.toSet()

data class NoVersionDependency(val group: String, val module: String) : Comparable<NoVersionDependency> {

    override fun compareTo(other: NoVersionDependency): Int =
        group.compareTo(other.group) * 10000 +
            module.compareTo(other.module) * 100

    override fun toString() = "$group:$module"
}

fun String.matchFilter(filters: Collection<String>?) = filters?.any { it in this } ?: true
fun String.matchNotFilter(filters: Collection<String>?) = filters?.none { it in this } ?: true

fun hasArg(vararg keys: String) = keys.any { it in args }

fun getArg(vararg keys: String) = args
    .indexOfFirst { it in keys }
    .takeIf { it >= 0 }
    ?.let {
        try {
            args[it + 1]
        } catch (t: Throwable) {
            println("No argument found for ${args[it]}, use '$HELP' for info")
            null
        }
    }

fun String.runCommand(workingDir: File = File("")) {
    val parts = split(" > ")
    ProcessBuilder(parts[0].split("\\s".toRegex()))
        .directory(workingDir).apply {
            if (parts.size > 1) redirectOutput(File(parts[1]))
            else redirectOutput(Redirect.INHERIT)
        }
        .redirectError(Redirect.INHERIT)
        .start()
        .waitFor(10, TimeUnit.MINUTES)
}
