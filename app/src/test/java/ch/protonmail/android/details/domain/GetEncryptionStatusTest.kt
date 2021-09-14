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

package ch.protonmail.android.details.domain

import ch.protonmail.android.R
import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.details.domain.model.MessageEncryptionStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetEncryptionStatusTest {

    private val mockUserManager = mockk<ch.protonmail.android.core.UserManager>() {
        every { currentLegacyUser } returns mockk {
            every { addresses } returns listOf()
        }
    }

    val getEncryptionStatus = GetEncryptionStatus()

    @BeforeTest
    fun setUp() {
        // This mock is only needed while covering the existing logic with tests.
        // Will be dropped once we migrate the logic from SenderLockIcon to GetEncryptedStatus
        mockkStatic(ProtonMailApplication::class)
        every { ProtonMailApplication.getApplication() } returns mockk {
            every { userManager } returns mockUserManager
        }
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(ProtonMailApplication::class)
    }

    @Test
    fun internalMessageWithoutVerifiedSignatureIsRepresentedByBluePadlockAndE2eEncryptedAndSignedTooltip() {
        val messageEncryption = MessageEncryption.INTERNAL

        val actual = getEncryptionStatus(
            messageEncryption
        )

        val expected = MessageEncryptionStatus(
            R.string.lock_default,
            R.color.icon_purple,
            R.string.sender_lock_internal
        )
        assertEquals(expected, actual)
    }
}
