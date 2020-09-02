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
import ch.protonmail.android.attachments.KEY_INPUT_DATA_ATTACHMENT_ID_STRING
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class DeleteAttachmentWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun verifyWorkerFailsWithNoAttachmentIdProvided() {
        // given
        val worker =
            TestListenableWorkerBuilder<DeleteAttachmentWorker>(context).build()
        val expected = ListenableWorker.Result.failure(
            workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot delete attachment with an empty id")
        )

        // when
        val result = worker.startWork().get()

        // then
        assertEquals(expected, result)
    }

    @Ignore("Ignored until integration of new network module and a way of mocking net calls")
    @Test
    fun verifyWorkerSucceedsWithStringAttachmentIdProvided() {
        // given
        val attachemtId = "id232"
        val worker = TestListenableWorkerBuilder<DeleteAttachmentWorker>(
            context = context,
            inputData = workDataOf(KEY_INPUT_DATA_ATTACHMENT_ID_STRING to attachemtId)
        ).build()
        val expected = ListenableWorker.Result.success()

        // when
        val result = worker.startWork().get()

        // then
        assertEquals(expected, result)
    }

}
