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
object Module {
    // Layers
    const val domain = ":domain"
    const val credentials = ":credentials"

    // Libs
    const val tokenAutoComplete = ":tokenAutoComplete:tokenAutoComplete-lib"

    // Test
    const val testKotlin = ":sharedTest:testKotlin"
    const val testAndroid = ":sharedTest:testAndroid"
    const val testAndroidInstrumented = ":sharedTest:testAndroidInstrumented"
}

/*** Internal libs */
object Lib {
    @Suppress("unused") const val composer = "Composer"
    @Suppress("unused") const val composerTest = "Composer-test"
    @Deprecated("To be removed in favour of package published on Bintray") const val protonCore = "Proton-core"
}
