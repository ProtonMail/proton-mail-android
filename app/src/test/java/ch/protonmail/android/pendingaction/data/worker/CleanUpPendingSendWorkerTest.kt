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

package ch.protonmail.android.pendingaction.data.worker

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.R
import ch.protonmail.android.compose.send.SendMessageWorker
import ch.protonmail.android.pendingaction.domain.repository.PendingSendRepository
import ch.protonmail.android.testdata.MessageTestData
import ch.protonmail.android.testdata.UserTestData
import ch.protonmail.android.testdata.WorkerTestData
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.worker.repository.WorkerRepository
import io.mockk.called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import org.junit.Test

internal class CleanUpPendingSendWorkerTest {

    private var accountManager: AccountManager = mockk()

    private var accounts = flowOf(
        mockk<Account> {
            every { userId } returns UserId("user_id")
            every { state } returns AccountState.Ready
        },
        mockk<Account> {
            every { userId } returns UserId("user_id_2")
            every { state } returns AccountState.Removed
        }
    )

    private val pendingSendRepositoryMock = mockk<PendingSendRepository>(relaxUnitFun = true)
    private val workerRepositoryMock = mockk<WorkerRepository> {
        every { cancelUniqueWork(any()) } returns mockk()
    }
    private val provideUniqueNameMock = mockk<SendMessageWorker.ProvideUniqueName> {
        every { this@mockk.invoke(MessageTestData.MESSAGE_ID_RAW) } returns WorkerTestData.UNIQUE_WORK_NAME
    }
    private val userNotifier = mockk<UserNotifier>(relaxUnitFun = true)
    private val contextMock = mockk<Context> {
        every { getString(R.string.message_drafted) } returns ERROR_MESSAGE
    }
    private val workParametersMock = mockk<WorkerParameters>(relaxed = true) {
        every { inputData } returns INPUT_DATA
    }
    private val cleanUpPendingSendWorker = CleanUpPendingSendWorker(
        pendingSendRepositoryMock,
        workerRepositoryMock,
        provideUniqueNameMock,
        userNotifier,
        contextMock,
        accountManager,
        workParametersMock
    )

    @Test
    fun `should do nothing when the send message worker is already running`() = runBlockingTest {
        // when
        coEvery { accountManager.getAccount(UserTestData.userId) } returns accounts
        coEvery { workerRepositoryMock.findWorkInfoForUniqueWork(WorkerTestData.UNIQUE_WORK_NAME) } returns
            listOf(WorkerTestData.RUNNING_WORK_INFO)

        // when
        cleanUpPendingSendWorker.doWork()

        // then
        verify { pendingSendRepositoryMock wasNot called }
        verify { pendingSendRepositoryMock wasNot called }
        verify(exactly = 0) { workerRepositoryMock.cancelUniqueWork(any()) }
    }

    @Test
    fun `should delete pending send, cancel send worker, and notify user when an enqueued send worker found`() =
        runBlockingTest {
            // given
            coEvery { accountManager.getAccount(UserTestData.userId) } returns accounts
            coEvery { workerRepositoryMock.findWorkInfoForUniqueWork(WorkerTestData.UNIQUE_WORK_NAME) } returns
                listOf(WorkerTestData.ENQUEUED_WORK_INFO)

            // when
            cleanUpPendingSendWorker.doWork()

            // then
            pendingSendRepositoryMock.deletePendingSendByDatabaseId(MessageTestData.MESSAGE_DATABASE_ID)
            workerRepositoryMock.cancelUniqueWork(WorkerTestData.UNIQUE_WORK_NAME)
            userNotifier.showSendMessageError(ERROR_MESSAGE, MessageTestData.MESSAGE_SUBJECT)
        }

    @Test
    fun `should not try and delete from DB when the userId doesn't match a logged in user`() = runBlockingTest {
        // when
        coEvery { accountManager.getAccount(UserId("user_id_2")) } returns accounts
        coEvery { workerRepositoryMock.findWorkInfoForUniqueWork(WorkerTestData.UNIQUE_WORK_NAME) } returns
            listOf(WorkerTestData.ENQUEUED_WORK_INFO)

        // when
        cleanUpPendingSendWorker.doWork()

        // then
        verify(exactly = 0) {
            pendingSendRepositoryMock.deletePendingSendByDatabaseId(MessageTestData.MESSAGE_DATABASE_ID)
        }
        workerRepositoryMock.cancelUniqueWork(WorkerTestData.UNIQUE_WORK_NAME)
        userNotifier.showSendMessageError(ERROR_MESSAGE, MessageTestData.MESSAGE_SUBJECT)
    }

    private companion object TestData {

        const val ERROR_MESSAGE = "Could not send the message"
        val INPUT_DATA = workDataOf(
            KEY_INPUT_MESSAGE_ID to MessageTestData.MESSAGE_ID_RAW,
            KEY_INPUT_MESSAGE_SUBJECT to MessageTestData.MESSAGE_SUBJECT,
            KEY_INPUT_MESSAGE_DATABASE_ID to MessageTestData.MESSAGE_DATABASE_ID,
            KEY_INPUT_USER_ID to UserTestData.userId.id
        )
    }
}
