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

package ch.protonmail.android

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.utils.ProtonCalendarUtils
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtonCalendarUtilsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sut = ProtonCalendarUtils(context)
    private val packageManager = mockk<PackageManager>()

    @Test
    fun should_show_Proton_Calendar_button_when_Calendar_not_installed() {
        every { packageManager.getLaunchIntentForPackage(ProtonCalendarUtils.packageName) } returns null
        every { packageManager.resolveActivity(any(), any()) } returns null

        assertTrue { sut.shouldShowProtonCalendarButton(packageManager) }
    }

    @Test
    fun should_show_Proton_Calendar_button_when_Calendar_installed_and_can_handle_ICS() {
        every { packageManager.getLaunchIntentForPackage(ProtonCalendarUtils.packageName) } returns Intent()
        every { packageManager.resolveActivity(any(), any()) } returns ResolveInfo()

        assertTrue { sut.shouldShowProtonCalendarButton(packageManager) }
    }

    @Test
    fun should_not_show_Proton_Calendar_button_when_Calendar_installed_but_cant_handle_ICS() {
        every { packageManager.getLaunchIntentForPackage(ProtonCalendarUtils.packageName) } returns Intent()
        every { packageManager.resolveActivity(any(), any()) } returns null

        assertFalse { sut.shouldShowProtonCalendarButton(packageManager) }
    }

}
