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

package ch.protonmail.android.uitests.robots.device

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import me.proton.core.test.android.instrumented.intentutils.IntentHelper.sendShareFileIntent

open class DeviceRobot {

    fun clickHomeButton(): DeviceRobot {
        device.pressHome()
        return this
    }

    fun clickRecentAppsButton(): DeviceRobot {
        device.pressRecentApps()
        return this
    }

    fun clickRecentAppView(): DeviceRobot {
        val recentAppSelector = UiSelector().resourceId("com.google.android.apps.nexuslauncher:id/overview_panel")
        device.findObject(recentAppSelector).waitForExists(5000)
        device.findObject(recentAppSelector).click()
        return this
    }

    fun expandNotifications(): DeviceRobot {
        device.openNotification()
        return this
    }

    fun clickNotificationByText(text: String): DeviceRobot {
        device.wait(Until.findObject(By.text(text)), TIMEOUT_5S).click()
        return this
    }

    fun clickShareDialogJustOnceButton(): DeviceRobot {
        device.wait(Until.findObject(By.textStartsWith("ProtonMail")), TIMEOUT_30S)?.click()
        device.wait(Until.findObject(By.res("android:id/button_once")), TIMEOUT_30S).click()
        return this
    }

    fun sendShareIntent(mimeType: String, fileName: String): DeviceRobot {
        sendShareFileIntent(mimeType, fileName)
        return this
    }

    companion object {
        private const val TIMEOUT_5S = 5000L
        private const val TIMEOUT_30S = 30_000L
        private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }
}
