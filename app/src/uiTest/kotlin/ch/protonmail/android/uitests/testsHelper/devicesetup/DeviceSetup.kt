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

package ch.protonmail.android.uitests.testsHelper.devicesetup

import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.uitests.tests.BaseTest.Companion.automation
import ch.protonmail.android.uitests.tests.BaseTest.Companion.downloadArtifactsPath
import ch.protonmail.android.uitests.tests.BaseTest.Companion.targetContext
import java.io.File
import java.io.FileOutputStream

object DeviceSetup {

    /**
     * Sets up device in ready for automation mode.
     * Animations turned off, long press timeout is set to 2 seconds, notifications popups are disabled.
     */
    fun setupDevice() {
        automation.executeShellCommand("settings put secure long_press_timeout 2000")
        // Disable floating notification pop-ups.
        automation.executeShellCommand("settings put global heads_up_notifications_enabled 0")
        automation.executeShellCommand("settings put global animator_duration_scale 0.0")
        automation.executeShellCommand("settings put global transition_animation_scale 0.0")
        automation.executeShellCommand("settings put global window_animation_scale 0.0")
    }

    // Clears logcat log.
    fun clearLogcat() {
        automation.executeShellCommand("logcat -c")
    }

    // Deletes artifacts folder from /sdcard/Download.
    fun deleteDownloadArtifactsFolder() {
        automation.executeShellCommand("rm -rf $downloadArtifactsPath")
    }

    // Prepares artifacts directory in provided path.
    fun prepareArtifactsDir(path: String) {
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        } else {
            if (dir.list() != null) {
                dir.list().forEach { File(it).delete() }
            }
        }
    }

    /**
     * Copies files from test project assets into main app files internal storage.
     * @param fileName - name of the file which exists in androidTests/assets folder.
     */
    fun copyAssetFileToInternalFilesStorage(fileName: String) {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val file = File("${targetContext.filesDir.path}/$fileName")

        if (!file.exists()) {
            try {
                val inputStream = testContext.assets.open(fileName)
                val fileOutputStream = FileOutputStream(file)
                val size = inputStream.available()
                val buffer = ByteArray(size)

                inputStream.read(buffer)
                inputStream.close()

                fileOutputStream.write(buffer)
                fileOutputStream.close()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}
