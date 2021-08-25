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

package ch.protonmail.android.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.api.models.MoveToFolderResponse
import ch.protonmail.android.data.local.CounterDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.UnreadLabelCounter
import ch.protonmail.android.repository.MessageRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplyLabelWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var api: ProtonMailApi

    @MockK
    private lateinit var counterDao: CounterDao

    @MockK
    private lateinit var accountManager: AccountManager

    @MockK
    private lateinit var messageRepository: MessageRepository

    private lateinit var worker: ApplyLabelWorker

    private val testUserId = UserId("testUser")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        every { counterDao.findUnreadLabelById(any()) } returns mockk<UnreadLabelCounter>(relaxed = true)
        every { counterDao.insertUnreadLabel(any()) } just Runs

        worker = ApplyLabelWorker(
            context,
            parameters,
            accountManager,
            messageRepository,
            counterDao,
            api,
        )
    }

    @Test
    fun verifyWorkerFailsWithNoLabelIdProvided() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty label id or message ids")
            )
            val response = mockk<MoveToFolderResponse>()
            every { api.labelMessages(any()) } returns response

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
    }

    @Test
    fun verifyWorkerSuccessesWithRequiredParametersProvided() {
        runBlockingTest {
            // given
            val testMessageId = "id1"
            val testLabelId = "LabelId1"
            val expected = ListenableWorker.Result.success()

            every { parameters.inputData } returns workDataOf(
                KEY_INPUT_DATA_MESSAGES_IDS to arrayOf(testMessageId),
                KEY_INPUT_DATA_LABEL_ID to testLabelId
            )

            val response = mockk<MoveToFolderResponse>()
            every { api.labelMessages(any()) } returns response
            val message = Message(messageId = testMessageId)
            coEvery { messageRepository.findMessage(testUserId, testMessageId) } returns  message

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
            verify { counterDao.insertUnreadLabel(any()) }
        }
    }
}
