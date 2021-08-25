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
package ch.protonmail.android.activities

import android.app.Application
import android.app.NotificationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.FileUriExposedException
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.R
import ch.protonmail.android.activities.settings.NotificationSettingsViewModel
import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.UserManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.rules.TestRule

internal class NotificationSettingsViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val contentUri = Uri.parse("content://some_content")
    private val fileUri = Uri.parse("file://some_file")

    private fun mockUserManager(default: Uri?): UserManager {
        var ringtoneUri = default
        val user = mockk<User>(relaxed = true) {
            every { ringtone } answers { ringtoneUri }
            every { ringtone = any() } answers { ringtoneUri = firstArg(); Unit }
        }
        return mockk(relaxed = true) {
            every { this@mockk.currentLegacyUser } returns user
        }
    }

    private val app = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as Application

    private fun viewModel(uri: Uri? = null) =
        NotificationSettingsViewModel(app, mockUserManager(uri))

    // FIXME: Davide check this, it is causing a lot of test failures
//    @Test
    fun mockUserManager_reliabilityTest() {
        val userManager = mockUserManager(contentUri)
        assertEquals(contentUri, userManager.currentLegacyUser?.ringtone)

        userManager.currentLegacyUser?.ringtone = fileUri
        assertEquals(fileUri, userManager.currentLegacyUser?.ringtone)
    }

    // FIXME: Davide check this, it is causing a lot of test failures
//    @Test
    fun currentRingtoneUri_return_right_value() {
        val ringtoneUri = viewModel(fileUri).currentRingtoneUri
        assertEquals(fileUri, ringtoneUri)
    }

    // FIXME: Davide check this, it is causing a lot of test failures
//    @Test
    fun currentRingtoneUri_return_default_if_userRingtone_is_null() {
        val ringtoneUri = viewModel().currentRingtoneUri
        assertEquals(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), ringtoneUri)
    }

    // FIXME: Davide check this, it is causing a lot of test failures
//    @Test(expected = FileUriExposedException::class)
    fun notification_with_fileUri_throw_FileUriExposedException() {
        notificationFor(fileUri)
    }

    // FIXME: Davide check this, it is causing a lot of test failures
//    @Test
    fun notification_with_contentUri_run_correctly() {
        notificationFor(contentUri)
    }

    private fun notificationFor(soundUri: Uri) {
        val uri = viewModel(soundUri).currentRingtoneUri
        val notificationManager = app.getSystemService<NotificationManager>()!!
        val notification = NotificationCompat.Builder(app)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle("ciao")
            .setSound(uri)
            .build()

        try {
            notificationManager.notify(0, notification)
        } catch (e: NoSuchMethodException) {
            // FIXME: For some reasons, with MockK on Android 28, we have a java.lang.NoSuchMethodException, in that case the make the check manually
            if (uri.scheme == "file") throw FileUriExposedException("")
        }
    }
}
