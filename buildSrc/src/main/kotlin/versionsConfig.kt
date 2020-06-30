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

import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

/** Initialize Easy Gradle versions */
fun initVersions() {

    // region Kotlin
    `kotlin version` =                          "1.3.70"        // Released: Mar 03, 2020
    `coroutines version` =                      "1.3.4"         // Released: Mar 06, 2020
    `serialization version` =                   "0.20.0"        // Released: Mar 04, 2020
    // endregion

    // region Android
    `android-gradle-plugin version` =           "4.0.0"         // Released: May 28, 2020

    `android-annotation version` =              "1.1.0"         // Released: Jun 05, 2019
    `appcompat version` =                       "1.1.0"         // Released: Sep 06, 2019
    `android-arch version` =                    "2.1.0"         // Released: Sep 06, 2019
    `constraint-layout version` =               "2.0.0-beta2"   // Released: Jun 17, 2019
    `dagger version` =                          "2.16"          // Released: May 04, 2018 TODO: 2.24 removed `HasActivityInjector` in favor of `HasAndroidInjector
    `espresso version` =                        "3.2.0"         // Released: May 30, 2019
    `ktx version` =                             "1.2.0-rc01"    // Released: Nov 23, 2019
    `lifecycle version` =                       "2.2.0-rc03"    // Released: Dec 05, 2019
    `material version` =                        "1.1.0-beta02"  // Released: Nov 10, 2019
    `android-paging version` =                  "2.1.0"         // Released: Jan 26, 2019
    `android-room version` =                    "2.2.1"         // Released: Oct 23, 2019
    `android-work version` =                    "2.2.0"         // Released: Aug 16, 2019

    `android-test version` =                    "1.2.0"         // Released: May 31, 2019
    // endregion

    // region Others
    `detekt version` =                          "1.9.1"         // Released: May 17, 2020 // TODO: remove after util-gradle 0.1.4
    `detect-code-analysis version` =            "0.3.2"         // Released: // TODO: remove after util-gradle 0.1.4
    `mockK version` =                           "1.10.0"        // Released: Apr 19, 2020
    `retrofit version` =                        "2.6.1"         // Released: Jul 31, 2019
    `retrofit-kotlin-serialization version` =   ""
    `viewStateStore version` =                  "1.4-beta-4"    // Released: Mar 02, 2020
    // endregion
}

// Proton Core
// Common
const val `Proton-kotlin-util version` =        "0.1"           // Released: Jun 10, 2020
const val `Proton-shared-preferences version` = "0.1"           // Released: Jun 10, 2020
const val `Proton-work-manager version` =       "0.1"           // Released: Jun 10, 2020
// Test
const val `Proton-android-test version` =       "0.1"           // Released: May 30, 2020
const val `Proton-android-instr-test version` = "0.1"           // Released: May 30, 2020
const val `Proton-kotlin-test version` =        "0.1"           // Released: Jun 10, 2020
@Suppress("unused") const val `composer version` =              "1.0-beta-3"    // Released: Feb 12, 2020
@Deprecated("To be removed in favour of package published on Bintray") const val `old protonCore version` =        "0.2.21"        // Released: Mar 13, 2020

// Test
const val `assertJ version` =                   "3.13.2"        // Released: Aug 04, 2019
const val `hamcrest version` =                  "1.3"           // Released:
const val `jUnit5 version` =                    "5.5.0"         // Released:
const val `robolectric version` =               "4.3.1"         // Released: Oct 11, 2019
const val `conditionalwatcher version` =        "0.2"           // Released: Aug 1, 2017

// Android
const val `android-biometric version` =         "1.0.1"         // Released: Jan 23, 2020
const val `android-fragment version` =          "1.2.0-rc01"    // Released: Oct 24, 2019
const val `android-media version` =             "1.1.0"         // Released: Sep 06, 2019
const val `playServices version` =              "17.0.0"        // Released: Jun 19, 2019

// Other
const val `apache-commons-lang version` =       "3.4"           // Released: Apr 03, 2015
const val `butterKnife version` =               "10.1.0"        // Released: Feb 14, 201
const val `gson version` =                      "2.8.5"         // Released: May 22, 201
const val `hugo version` =                      "1.2.1"         // Released: Feb 18, 201
const val `jsoup version` =                     "1.8.3"         // Released: Aug 02, 2015
const val `okHttp3 version` =                   "3.12.5"        // Released: Sep 11, 2019 TODO: 4.x requires some refactor / 3.13+ requires minSDK 2
const val `rxJava version` =                    "2.0.2"         // Released: Dec 02, 201
const val `rxRelay version` =                   "2.1.1"         // Released: Aug 23, 201
const val `sentry version` =                    "1.7.30"        // Released: Jan 28, 2020 TODO: 2.x requires minor refactor for captureEvent and different imports
const val `sentry-plugin version` =             "1.7.34"        // Released: Apr 15, 2020
const val `sonarQube version` =                 "2.7.1"         // Released: May 14, 201
const val `stetho version` =                    "1.5.1"         // Released: Mar 18, 201
const val `timber version` =                    "4.7.1"         // Released:
const val `trustKit version` =                  "1.1.2"         // Released: Jun 09, 2019
const val `minidns version` =                   "0.3.3"         // Released: Oct 14, 2018
const val `rf2 converter version` =             "2.7.1"         // Released: Jan 02, 2020
const val `jackson version` =                   "2.10.2"        // Released: Jan 05, 2020
const val `aerogear version` =                  "1.0.0"         // Released: Mar 23, 2013
