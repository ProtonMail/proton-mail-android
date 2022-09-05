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

package ch.protonmail.android.uitests.robots.device

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import me.proton.core.test.android.instrumented.utils.Shell
import me.proton.fusion.Fusion

open class DeviceRobot: Fusion {

    fun clickHomeButton(): DeviceRobot {
        uiDevice.pressHome()
        return this
    }

    fun clickRecentAppsButton(): DeviceRobot {
        uiDevice.pressRecentApps()
        return this
    }

    fun clickRecentAppView(): DeviceRobot {
        val recentAppSelector = UiSelector().resourceId("com.google.android.apps.nexuslauncher:id/overview_panel")
        uiDevice.findObject(recentAppSelector).waitForExists(5000)
        uiDevice.findObject(recentAppSelector).click()
        return this
    }

    fun expandNotifications(): DeviceRobot {
        uiDevice.openNotification()
        return this
    }

    fun clickNotificationByText(text: String): DeviceRobot {
        uiDevice.wait(Until.findObject(By.text(text)), TIMEOUT_5S).click()
        return this
    }

    fun clickShareDialogJustOnceButton(): DeviceRobot {
        byObject.containsText("Proton Mail").withTimeout(TIMEOUT_30S).click()
        byObject.withResName("android:id/button_once").withTimeout(TIMEOUT_30S).click()
        return this
    }

    fun sendShareIntent(mimeType: String, fileName: String): DeviceRobot {
        Shell.sendShareFileIntent(mimeType, fileName)
        return this
    }

    companion object {

        private const val TIMEOUT_5S = 5000L
        private const val TIMEOUT_30S = 30_000L
        private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }
}
