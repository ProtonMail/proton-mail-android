/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.usecase.message

import ch.protonmail.android.core.UserManager
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.testdata.MessageTestData
import ch.protonmail.android.testdata.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class GetDecryptedMessageByIdTest {

    private val accountManagerMock = mockk<AccountManager> {
        coEvery { getPrimaryUserId() } returns flowOf(UserIdTestData.userId)
    }
    private val userManagerMock = mockk<UserManager>()
    private val messageRepositoryMock = mockk<MessageRepository>()
    private val getDecryptedMessageById = GetDecryptedMessageById(
        accountManagerMock,
        userManagerMock,
        messageRepositoryMock
    )

    @Test
    fun `should return null when primary user not found`() = runBlockingTest {
        // given
        coEvery { accountManagerMock.getPrimaryUserId() } returns flowOf(null)

        // when
        val decryptedMessage = getDecryptedMessageById.orNull(MessageTestData.MESSAGE_ID_RAW)

        // then
        assertNull(decryptedMessage)
    }

    @Test
    fun `should return null when message not found`() = runBlockingTest {
        // given
        coEvery {
            messageRepositoryMock.getMessage(UserIdTestData.userId, MessageTestData.MESSAGE_ID_RAW)
        } returns null

        // when
        val decryptedMessage = getDecryptedMessageById.orNull(MessageTestData.MESSAGE_ID_RAW)

        // then
        assertNull(decryptedMessage)
    }

    @Test
    fun `should return the decrypted message when message found`() = runBlockingTest {
        // given
        val messageSpy = MessageTestData.messageSpy()
        coEvery {
            messageRepositoryMock.getMessage(UserIdTestData.userId, MessageTestData.MESSAGE_ID_RAW)
        } returns messageSpy

        // when
        val decryptedMessage = getDecryptedMessageById.orNull(MessageTestData.MESSAGE_ID_RAW)

        // then
        assertEquals(messageSpy, decryptedMessage)
        verify { messageSpy.decrypt(userManagerMock, UserIdTestData.userId) }
    }
}
