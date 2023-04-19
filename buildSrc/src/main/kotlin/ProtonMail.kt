import org.gradle.api.JavaVersion

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

/**
 * Params for the Application and various modules
 * @author Davide Farella
 */
object ProtonMail {

    const val versionName = "3.0.14"
    const val versionCode = 936

    const val compileSdk = 33
    const val targetSdk = 31
    const val minSdk = 23

    val jvmTarget = JavaVersion.VERSION_11
}
