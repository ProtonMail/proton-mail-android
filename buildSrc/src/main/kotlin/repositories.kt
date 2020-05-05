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
@file:Suppress("PackageDirectoryMismatch")

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

/**
 * Lambda that applies repositories required by the project
 * @author Davide Farella
 *
 * TODO clean up / comment with relative usage
 */
val repos: RepositoryHandler.() -> Unit get() = {
    google()
    jcenter()
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://s3.amazonaws.com/repo.commonsware.com")
    maven("https://oss.sonatype.org/content/repositories/releases/")
    maven("https://mint.splunk.com/gradle/")
    maven("https://jitpack.io")
}
