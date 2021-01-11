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

package ch.protonmail.android.uitests.testsHelper.intentutils

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.uitests.tests.BaseTest.Companion.automation
import junit.framework.TestCase.fail
import java.io.File
import java.io.FileOutputStream

object IntentHelper {

    fun sendShareFileIntent(mimeType: String, fileName: String) {
//        val fileType = mimeType.split("/")[1]
//        // Check if provided mime type corresponds to the file
//        if (fileName.contains(fileType)) {
            automation.executeShellCommand("am start -a android.intent.action.SEND -t $mimeType " +
                "--eu android.intent.extra.STREAM " +
                "file:///data/data/ch.protonmail.android.beta/files/$fileName " +
                " --grant-read-uri-permission")
//        } else {
//            fail("Mime type:\"$mimeType\" doesn't correspond to the file:\"$fileName\"")
//        }
    }

    // Creates new activity result for a file in test app assets.
    fun createImageResultFromAssets(fileName: String): Instrumentation.ActivityResult {
        val resultIntent = Intent()

        // Declare variables for test and application context.
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File("${appContext.cacheDir}/$fileName")

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

        // Build a stubbed result from temp file.
        resultIntent.data = Uri.fromFile(file)
        return Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent)
    }
}
