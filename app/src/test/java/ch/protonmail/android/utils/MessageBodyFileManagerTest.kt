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

package ch.protonmail.android.utils

import android.content.Context
import ch.protonmail.android.data.local.model.Message
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the functionality of [MessageBodyFileManager].
 */

class MessageBodyFileManagerTest {

    @RelaxedMockK
    private lateinit var applicationContext: Context

    @MockK
    private lateinit var fileHelper: FileHelper

    private val dispatchers = TestDispatcherProvider()

    private lateinit var messageBodyFileManager: MessageBodyFileManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        messageBodyFileManager = MessageBodyFileManager(
            applicationContext,
            fileHelper,
            dispatchers
        )
    }

    @Test
    fun verifyNullIsReturnedIfMessageIdIsNullWhenReadMessageBodyFromFileIsCalled() {
        runTest(dispatchers.Main) {
            // given
            val mockMessage = mockk<Message> {
                every { messageId } returns null
            }

            // when
            val result = messageBodyFileManager.readMessageBodyFromFile(mockMessage)

            // then
            assertNull(result)
        }
    }

    @Test
    fun verifyNullIsReturnedIfReadingFromFileFailsWhenReadMessageBodyFromFileIsCalled() {
        runTest(dispatchers.Main) {
            // given
            val mockMessage = mockk<Message> {
                every { messageId } returns "messageId"
            }
            every { fileHelper.readFromFile(any()) } returns null
            every { fileHelper.createFile(any(), any()) } returns mockk()

            // when
            val result = messageBodyFileManager.readMessageBodyFromFile(mockMessage)

            // then
            assertNull(result)
        }
    }

    @Test
    fun verifyFileContentIsReturnedIfReadingFileIsSuccessfulWhenReadMessageBodyFromFileIsCalled() {
        runTest(dispatchers.Main) {
            // given
            val mockMessage = mockk<Message> {
                every { messageId } returns "messageId"
            }
            val expectedResult = "messageBody"
            every { fileHelper.readFromFile(any()) } returns expectedResult
            every { fileHelper.createFile(any(), any()) } returns mockk()

            // when
            val result = messageBodyFileManager.readMessageBodyFromFile(mockMessage)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyNullIsReturnedIfMessageBodyIsNullWhenSaveMessageBodyToFileIsCalled() {
        runTest(dispatchers.Main) {
            // given
            val mockMessage = mockk<Message> {
                every { messageId } returns "messageId"
                every { messageBody } returns null
            }

            // when
            val result = messageBodyFileManager.saveMessageBodyToFile(mockMessage)

            // then
            assertNull(result)
        }
    }

    @Test
    fun verifyNullIsReturnedIfWritingToFileFailsWhenSaveMessageBodyToFileIsCalled() {
        runTest(dispatchers.Main) {
            // given
            val mockMessage = mockk<Message> {
                every { messageId } returns "messageId"
                every { messageBody } returns "messageBody"
            }
            coEvery { fileHelper.writeToFile(any(), "messageBody") } returns false
            every { fileHelper.createFile(any(), any()) } returns mockk()

            // when
            val result = messageBodyFileManager.saveMessageBodyToFile(mockMessage)

            // then
            assertNull(result)
        }
    }

    @Test
    fun verifyFilePathIsReturnedIfWritingToFileIsSuccessfulWhenSaveMessageBodyToFileIsCalled() {
        runTest(dispatchers.Main) {
            // given
            val mockMessage = mockk<Message> {
                every { messageId } returns "messageId"
                every { messageBody } returns "messageBody"
            }
            val expectedResult = "file://filePath"
            coEvery { fileHelper.writeToFile(any(), "messageBody") } returns true
            every { fileHelper.createFile(any(), any()) } returns mockk {
                every { absolutePath } returns "filePath"
            }

            // when
            val result = messageBodyFileManager.saveMessageBodyToFile(mockMessage)

            // then
            assertEquals(expectedResult, result)
        }
    }
}
