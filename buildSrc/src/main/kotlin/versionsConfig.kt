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
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

/** Initialize Easy Gradle versions */
fun initVersions() {

    // region Kotlin
    `kotlin version` =                          "1.7.20"
    `coroutines version` =                      "1.5.2"         // Released: Sep 02, 2021
    `serialization version` =                   "1.4.1"
    // endregion

    // region Android
    `android-gradle-plugin version` =           "7.4.1"

    `android-annotation version` =              "1.1.0"         // Released: Jun 05, 2019
    `appcompat version` =                       "1.2.0"         // Released: Aug 19, 2020
    `android-arch version` =                    "2.1.0"         // Released: Sep 06, 2019
    `constraint-layout version` =               "2.0.4"         // Released: Oct 31, 2020
    `espresso version` =                        "3.5.0"
    `hilt-android version` =                    "2.44.2"
    `hilt-androidx version` =                   "1.0.0"         // Released: May 05, 2021
    `ktx version` =                             "1.9.0"
    `lifecycle version` =                       "2.4.0-alpha01" // Released: Mar 24, 2021
    `material version` =                        "1.4.0"         // Released: Jul 04, 2021
    `android-paging version` =                  "2.1.0"         // Released: Jan 26, 2019
    `android-room version` =                    "2.4.3"         // Released: Feb 23, 2022
    `android-work version` =                    "2.7.1"         // Released: Nov 17, 2021

    `android-test version` =                    "1.5.0"
    // endregion

    // region Others
    `assert4k version` =                        "0.7.1"         // Released: May 04, 2021
    `assistedInject version` =                  "0.6.0"         // Released: Sep 14, 2020
    `dagger version` =                          "2.44.2"

    `mockK version` =                           "1.13.3"
    `retrofit version` =                        "2.9.0"         // Released: May 20, 2020
    `retrofit-kotlin-serialization version` =   "0.8.0"         // Released: Oct 09, 2020
    `robolectric version` =                     "4.9.1"
    `viewStateStore version` =                  "1.4-beta-4"    // Released: Mar 02, 2020
    // endregion
}

// Proton Core
const val `Proton-core version` =               "10.0.0"

// Test
const val `aerogear version` =                  "1.0.0"         // Released: Mar 23, 2013
const val `android-test-ext version` =          "1.1.4"
const val `android-test-orchestrator version` = "1.4.1"         // Released: Dec 13, 2021
const val `assertJ version` =                   "3.13.2"        // Released: Aug 04, 2019
const val `falcon version` =                    "2.1.1"         // Released: Sep 24, 2018
const val `hamcrest version` =                  "1.3"           // Released:
const val `json-simple version` =               "1.1.1"         // Released: Mar 21, 2012
const val `browserstack-plugin version` =       "3.0.1"         // Released: Jun 10, 2020
const val `uiautomator version` =               "2.3.0-alpha01"
const val `sun-mail-android version` =          "1.5.5"         // Released: Mar 06, 2020
const val `turbine version` =                   "0.12.1"
const val `junit-ktx version` =                 "1.1.4"

// Android
const val `android-biometric version` =         "1.0.1"         // Released: Jan 23, 2020
const val `android-core-splashscreen version` = "1.0.0-beta02"  // Released: Mar 23, 2022
const val `android-fragment version` =          "1.5.4"         // Released: Oct 10, 2022
const val `android-media version` =             "1.1.0"         // Released: Sep 06, 2019
const val `android-preference version` =        "1.1.1"         // Released: Apr 15, 2020
const val `android-startup version` =           "1.1.0"         // Released: Aug 04, 2021
const val `android-webkit version` =            "1.4.0"         // Released: Dec 16, 2020
const val `flexbox version` =                   "2.0.1"         // Released: Jan 17, 2020
const val `lifecycle-extensions version` =      "2.2.0"         // Released: Jan 22, 2020
const val `googleServices version` =            "4.3.3"         // Released: Nov 11, 2019
const val `playServices version` =              "17.0.0"        // Released: Jun 19, 2019

// Other
const val `apache-commons-lang version` =       "3.4"           // Released: Apr 03, 2015
const val `arrow version` =                     "0.11.0"        // Released: Sep 09, 2020
const val `butterKnife version` =               "10.1.0"        // Released: Feb 14, 201
const val `firebase-messaging version` =        "20.2.4"        // Released: Jul 30, 2020
const val `desugaring version` =                "1.1.5"         // Released:
const val `gson version` =                      "2.8.5"         // Released: May 22, 201
const val `jackson version` =                   "2.10.2"        // Released: Jan 05, 2020
const val `jsoup version` =                     "1.13.1"        // Released: Mar 01, 2020
const val `leakcanary version` =                "2.7"           // Released: Mar 26, 2021
const val `minidns version` =                   "0.3.3"         // Released: Oct 14, 2018
const val `okHttp3 version` =                   "4.9.1"         // Released: Jan 30, 2021
const val `okHttp-url-connection version` =     "4.9.1"         // Released: Jan 30, 2021
const val `okio version` =                      "2.10.0"        // Released: Jan 07, 2021
const val `rf2 converter version` =             "2.9.0"         // Released: May 20, 2020
const val `rxJava version` =                    "2.0.2"         // Released: Dec 02, 201
const val `rxRelay version` =                   "2.1.1"         // Released: Aug 23, 201
const val `sentry version` =                    "6.11.0"
const val `sentry-plugin version` =             "3.4.2"
const val `sonarQube version` =                 "2.7.1"         // Released: May 14, 2019
const val `stetho version` =                    "1.5.1"         // Released: Mar 18, 2019
const val `timber version` =                    "4.7.1"         // Released:
const val `trustKit version` =                  "1.1.2"         // Released: Jun 09, 2019
const val `remark version` =                    "1.1.0"         // Released: Dec 08, 2016
const val `store version` =                     "4.0.5"
const val `coil version` =                      "1.2.1"         // Released: Apr 28, 2021
