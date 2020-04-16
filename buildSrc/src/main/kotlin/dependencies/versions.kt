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

/**
 * Versions of the various dependencies ( libraries or plugins )
 * @author Davide Farella
 */
internal object V {

    // region Kotlin
    const val kotlin =                          "1.3.50"        // Updated: Aug 22, 2019
    const val coroutines =                      "1.3.0-RC2"     // Updated: Aug 09, 2019
    const val serialization =                   "0.11.1"        // Updated: Jun 19, 2019
    // endregion

    // region Tests
    const val androidx_test =                   "1.2.0"         // Updated: May 31, 2019
    const val assertJ =                         "3.13.2"        // Updated: Aug 04, 2019
    const val espresso =                        "3.2.0"         // Updated: May 30, 2019
    const val hamcrest =                        "1.3"           // Updated:
    const val jUnit4 =                          "4.12"
    const val jUnit5 =                          "5.5.0"         // Updated:
    const val mockk =                           "1.9.3"         // Updated: Mar 25, 2019
    // endregion

    // region Android
    const val android_annotations =             "1.1.0"         // Updated: Jun 05, 2019
    const val android_arch =                    "2.1.0-rc01"    // Updated: Jul 03, 2019
    const val android_biometric =               "1.0.1"         // Updated: Jan 23, 2020
    const val android_constraintLayout =        "2.0.0-beta2"   // Updated: Jun 17, 2019
    const val android_espresso =                "3.2.0"         // Updated: May 30, 2019
    const val android_gradle_plugin =           "3.3.2"         // Updated: Mar 04, 2019 TODO 3.4.x uses R8, so proguard rules must be revisited, specially for Gson and retrofit. https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md
    const val android_ktx =                     "1.1.0-rc03"    // Updated: Aug 09, 2019
    const val android_lifecycle =               "2.1.0-rc01"    // Updated: Jul 03, 2019
    const val android_material =                "1.1.0-alpha09" // Updated: Jul 30, 2019
    const val android_paging =                  "2.1.0"         // Updated: Jan 26, 2019
    const val android_palette =                 "1.0.0"         // Updated: Sep 22, 2018
    const val android_room =                    "2.1.0"         // Updated: Jun 14, 2019
    const val android_support =                 "1.2.0-alpha03" // Updated: Mar 26, 2020
    const val android_tools =                   "26.5.0-rc01"   // Updated: Jul 17, 2019
    const val android_work =                    "2.1.0-beta01"  // Updated:
    // endregion

    // region Other
    const val butterKnife =                     "10.1.0"        // Updated: Feb 14, 2019
    const val dagger =                          "2.16"          // Updated: May 04, 2018 TODO: 2.24 removed `HasActivityInjector` in favor of `HasAndroidInjector`
    const val gson =                            "2.8.5"         // Updated: May 22, 2018
    const val hugo =                            "1.2.1"         // Updated: Feb 18, 2015
    const val okHttp3 =                         "3.12.5"        // Updated: Sep 11, 2019 TODO: 4.x requires some refactor / 3.13+ requires minSDK 21
    const val retrofit =                        "2.6.1"         // Updated: Jul 31, 2019
    const val rxJava =                          "2.0.2"         // Updated: Dec 02, 2016
    const val rxRelay =                         "2.1.1"         // Updated: Aug 23, 2019
    const val sonarQube =                       "2.7.1"         // Updated: May 14, 2019
    const val stetho =                          "1.5.1"         // Updated: Mar 18, 2019
    const val timber =                          "4.7.1"         // Updated:
    const val trustKit =                        "1.1.2"         // Updated: Jun 09, 2019
    const val viewStateStore =                  "1.3-alpha-1"   // Updated: May 22, 2019
    const val sentry =                          "1.7.22"        // Updated:
    // endregion

    // region Publishing
    const val publishing_dokka_plugin =         "0.9.18"        // Updated: Mar 19, 2019
    // endregion
}
