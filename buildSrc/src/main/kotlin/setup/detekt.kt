/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonMail.
 * 
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package setup

import `detekt id`
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import studio.forface.easygradle.dsl.*
import java.io.BufferedWriter
import java.io.File

/**
 * Setup Detekt for whole Project.
 * It will:
 * * apply Detekt plugin to sub-projects
 * * configure Detekt Extension
 * * add accessor Detekt dependencies
 * * register [MergeDetektReports] Task, in order to generate an unique json report for all the
 *   module
 *
 * @param filter filter [Project.subprojects] to attach Detekt to
 *
 *
 * @author Davide Farella
 */
fun Project.setupDetekt(filter: (Project) -> Boolean = { true }) {

    val detektRootDir = File("$rootDir/detekt")
    val detektReportsDir = File(detektRootDir, "reports")

    // Configure sub-projects
    for (sub in subprojects.filter(filter)) {

        sub.apply(plugin = `detekt id`)
        sub.extensions.configure<DetektExtension> {

            failFast = false
            config = files(File(detektRootDir, "config.yml"))

            reports {
                xml.enabled = false
                html.enabled = false
                txt.enabled = false
                custom {
                    reportId = "DetektQualityOutputReport"
                    destination = File(detektReportsDir, "${sub.name}.json")
                }
            }
        }
        sub.dependencies {
            add("detekt", `detekt-cli`)
            add("detektPlugins", `detekt-code-analysis`)
            add("detektPlugins", `detekt-formatting`)
        }
    }

    tasks.register<MergeDetektReports>("multiModuleDetekt") {
        reportsDir = detektReportsDir

        // Execute after 'detekt' is completed for sub-projects
        val subTasks = subprojects.flatMap { getTasksByName("detekt", true) }
        dependsOn(subTasks)
    }

}

internal open class MergeDetektReports : DefaultTask() {
    @InputDirectory lateinit var reportsDir: File
    @Input var outputName: String = "mergedReport.json"

    @TaskAction
    fun run() {
        val mergedReport = File(reportsDir, outputName)
            .apply { if (exists()) writeText("") }

        mergedReport.bufferedWriter().use { writer ->
            val reportFiles = reportsDir
                // Take json files, excluding the merged report
                .listFiles { _, name -> name.endsWith(".json") && name != outputName }
                ?.filterNotNull()
                // Skip modules without issues
                ?.filter {
                    it.bufferedReader().use {  reader ->
                        return@filter reader.readLine() != "[]"
                    }
                }
                // Return if no file is found
                ?.takeIf { it.isNotEmpty() } ?: return

            // Open array
            writer.append("[")

            // Write body
            writer.handleFile(reportFiles.first())
            reportFiles.drop(1).forEach {
                writer.append(",")
                writer.handleFile(it)
            }

            // Close array
            writer.newLine()
            writer.append("]")
        }
    }

    private fun BufferedWriter.handleFile(file: File) {
        val allLines = file.bufferedReader().lineSequence()

        // Drop first and write 'prev' in order to skip array open and close
        var prev: String? = null
        allLines.drop(1).forEach { s ->
            prev?.let {
                newLine()
                append(it)
            }
            prev = s
        }
    }
}
