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

import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Setup Kotlin for whole Project.
 * It will setup Kotlin compile options to sub-projects
 *
 * @param filter filter [Project.subprojects] to configure
 *
 *
 * @author Davide Farella
 */
fun Project.setupKotlin(filter: (Project) -> Boolean = { true }) {

    // Configure sub-projects
    for (sub in subprojects.filter(filter)) {

        // Options for Kotlin
        sub.tasks.withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = freeCompilerArgs +
                    "-XXLanguage:+NewInference" +
                    "-Xuse-experimental=kotlin.Experimental" +
                    "-XXLanguage:+InlineClasses"
            }
        }

        // Disable JavaDoc
        sub.tasks.withType<Javadoc> { enabled = false }
    }
}
