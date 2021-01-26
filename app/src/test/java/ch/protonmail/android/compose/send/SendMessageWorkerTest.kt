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

package ch.protonmail.android.compose.send

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.extensions.serialize
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.Test

class SendMessageWorkerTest : CoroutinesTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var workManager: WorkManager

    @InjectMockKs
    private lateinit var worker: SendMessageWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun workerEnqueuerCreatesOneTimeRequestWorkerWhichIsUniqueForMessageId() {
        runBlockingTest {
            // Given
            val messageParentId = "98234"
            val attachmentIds = listOf("attachmentId234", "238")
            val messageId = "2834"
            val messageDbId = 534L
            val messageActionType = Constants.MessageActionType.REPLY_ALL
            val message = Message(messageId = messageId)
            message.dbId = messageDbId
            val previousSenderAddressId = "previousSenderId82348"
            val requestSlot = slot<OneTimeWorkRequest>()
            every {
                workManager.enqueueUniqueWork(messageId, ExistingWorkPolicy.REPLACE, capture(requestSlot))
            } answers { mockk() }

            // When
            SendMessageWorker.Enqueuer(workManager).enqueue(
                message,
                attachmentIds,
                messageParentId,
                messageActionType,
                previousSenderAddressId
            )

            // Then
            val workSpec = requestSlot.captured.workSpec
            val constraints = workSpec.constraints
            val inputData = workSpec.input
            val actualMessageDbId = inputData.getLong(KEY_INPUT_SEND_MESSAGE_MSG_DB_ID, -1)
            val actualAttachmentIds = inputData.getStringArray(KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS)
            val actualMessageLocalId = inputData.getString(KEY_INPUT_SEND_MESSAGE_MESSAGE_ID)
            val actualMessageParentId = inputData.getString(KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID)
            val actualMessageActionType = inputData.getString(KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_JSON)
            val actualPreviousSenderAddress = inputData.getString(KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID)
            assertEquals(message.dbId, actualMessageDbId)
            assertEquals(message.messageId, actualMessageLocalId)
            assertArrayEquals(attachmentIds.toTypedArray(), actualAttachmentIds)
            assertEquals(messageParentId, actualMessageParentId)
            assertEquals(messageActionType.serialize(), actualMessageActionType)
            assertEquals(previousSenderAddressId, actualPreviousSenderAddress)
            assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
            assertEquals(BackoffPolicy.EXPONENTIAL, workSpec.backoffPolicy)
            assertEquals(20000, workSpec.backoffDelayDuration)
            verify { workManager.getWorkInfoByIdLiveData(any()) }
        }
    }

}
