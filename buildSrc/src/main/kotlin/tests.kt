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

import org.gradle.api.Project
import studio.forface.easygradle.dsl.*

/**
 * Setup Tests for whole Project.
 * It will create a Gradle Task called "allTest" that will invoke "test" for jvm modules and "testDebugUnitTest" for
 * Android ones
 *
 * @param filter filter [Project.subprojects] to attach Publishing to
 *
 *
 * @author Davide Farella
 */
fun Project.setupTests(filter: (Project) -> Boolean = { true }) {

    // Configure sub-projects
    for (sub in subprojects.filter(filter)) {
        sub.afterEvaluate {
            tasks.register("allTest") {
                if (isAndroid) dependsOn(tasks.findByName("testDebugUnitTest")
                    ?: tasks.getByName("testBetaDebugUnitTest"))
                else if (isJvm) dependsOn(tasks.getByName("test"))
            }
        }
    }
}

val Project.isJvm get() =
    plugins.hasPlugin("java-library")
