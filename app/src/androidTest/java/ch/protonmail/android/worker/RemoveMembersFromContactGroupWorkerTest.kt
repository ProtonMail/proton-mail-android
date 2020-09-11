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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoveMembersFromContactGroupWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun verifyWorkerFailsWithNoGroupIdProvided() {
        // given
        val worker =
            TestListenableWorkerBuilder<RemoveMembersFromContactGroupWorker>(context).build()
        val expected = ListenableWorker.Result.failure(
            workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty contacts group id")
        )

        // when
        val result = worker.startWork().get()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyWorkerFailsWithNoMembersListProvided() {
        // given
        val groupId = "Id1"
        val worker =
            TestListenableWorkerBuilder<RemoveMembersFromContactGroupWorker>(
                context,
                inputData = workDataOf(KEY_INPUT_DATA_CONTACT_GROUP_ID to groupId)
            ).build()
        val expected = ListenableWorker.Result.failure(
            workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty members list")
        )

        // when
        val result = worker.startWork().get()

        // then
        assertEquals(expected, result)
    }

}
