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

package ch.protonmail.android.utils

import android.content.Context
import ch.protonmail.android.api.models.room.messages.Message
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Tests the functionality of [MessageBodyFileManager].
 */

class MessageBodyFileManagerTest {

    @RelaxedMockK
    private lateinit var applicationContext: Context

    private lateinit var messageBodyFileManager: MessageBodyFileManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        messageBodyFileManager = MessageBodyFileManager(
            applicationContext,
            TestDispatcherProvider
        )
    }

    @Test
    fun verifyNullIsReturnedIfMessageIdIsNullWhenReadMessageBodyFromFileIsCalled() {
        runBlocking {
            // given
            val mockMessage = mockk<Message> {
                every { messageId } returns null
            }

            // when
            val result = messageBodyFileManager.readMessageBodyFromFile(mockMessage)

            // then
            assertEquals(null, result)
        }
    }

    @Test
    fun verifyNullIsReturnedIfMessageBodyIsNullWhenSaveMessageBodyToFileIsCalled() {
        runBlocking {
            // given
            val mockMessage = mockk<Message> {
                every { messageId } returns "messageId"
                every { messageBody } returns null
            }

            // when
            val result = messageBodyFileManager.saveMessageBodyToFile(mockMessage)

            // then
            assertEquals(null, result)
        }
    }
}
