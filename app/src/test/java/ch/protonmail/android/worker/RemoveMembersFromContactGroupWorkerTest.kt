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
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.data.local.ContactDao
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoveMembersFromContactGroupWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters


    @MockK
    private lateinit var contactDb: ContactDao

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var worker: RemoveMembersFromContactGroupWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        worker = RemoveMembersFromContactGroupWorker(
            context,
            parameters,
            api,
            contactDb,
            TestDispatcherProvider
        )
    }

    @Test
    fun verifyWorkerFailsWithNoGroupIdProvided() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty contacts group id")
            )

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun verifyWorkerFailsWithNoMembersListProvided() {
        runBlockingTest {
            // given
            val groupId = "Id1"
            every { parameters.inputData } returns
                workDataOf(KEY_INPUT_DATA_CONTACT_GROUP_ID to groupId)
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty members list")
            )

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
    }

    @Test
    fun verifySuccessResultIsGeneratedWithRequiredParameters() {
        runBlockingTest {
            // given
            val groupId = "Id1"
            val member1 = "MemberId1"
            every { parameters.inputData } returns
                workDataOf(
                    KEY_INPUT_DATA_CONTACT_GROUP_ID to groupId,
                    KEY_INPUT_DATA_MEMBERS_LIST to arrayOf(member1)
                )
            val expected = ListenableWorker.Result.success()

            coEvery { api.unlabelContactEmails(any()) } returns Unit

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }


}
