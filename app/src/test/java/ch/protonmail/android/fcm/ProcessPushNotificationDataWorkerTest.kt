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

package ch.protonmail.android.fcm

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.Notification
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.fcm.model.PushNotification
import ch.protonmail.android.fcm.model.PushNotificationData
import ch.protonmail.android.fcm.model.PushNotificationSender
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.servers.notification.NotificationServer
import ch.protonmail.android.utils.AppUtil
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import me.proton.core.util.kotlin.deserialize
import org.junit.After
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the functionality of [ProcessPushNotificationDataWorker].
 */

class ProcessPushNotificationDataWorkerTest {

    private val testId = Id("id")
    private val testUserId = UserId("id")

    private val context: Context = mockk(relaxed = true)

    private val userManager: UserManager = mockk(relaxed = true) {
        coEvery { getLegacyUser(any()) } returns mockk(relaxed = true)
    }

    private val sessionManager: SessionManager = mockk(relaxed = true) {
        coEvery { getUserId(any()) } returns UserId("id")
    }

    private val workerParameters: WorkerParameters = mockk(relaxed = true)

    private val workManager: WorkManager = mockk(relaxed = true)

    private val alarmReceiver: AlarmReceiver = mockk(relaxed = true)

    private val databaseProvider: DatabaseProvider = mockk(relaxed = true)

    private val messageRepository: MessageRepository = mockk(relaxed = true)

    private val notificationServer: NotificationServer = mockk(relaxed = true) {
        every { createRetrievingNotificationsNotification() } returns mockk()
    }

    private val queueNetworkUtil: QueueNetworkUtil = mockk(relaxed = true)

    private val processPushNotificationDataWorker: ProcessPushNotificationDataWorker = spyk(
        ProcessPushNotificationDataWorker(
            context,
            workerParameters,
            notificationServer,
            alarmReceiver,
            queueNetworkUtil,
            userManager,
            databaseProvider,
            messageRepository,
            sessionManager
        ),
        recordPrivateCalls = true
    ) {
        coEvery { setForeground(any()) } just Runs
    }
    private val processPushNotificationDataWorkerEnqueuer =
        ProcessPushNotificationDataWorker.Enqueuer(workManager)

    @BeforeTest
    fun setUp() {
        mockkConstructor(UserCrypto::class)
        mockkStatic(AppUtil::class)
        mockkStatic("me.proton.core.util.kotlin.SerializationUtilsKt")
    }

    @After
    fun tearDown() {
        unmockkConstructor(UserCrypto::class)
        unmockkStatic(AppUtil::class)
        unmockkStatic("me.proton.core.util.kotlin.SerializationUtilsKt")
    }

    @Test
    fun verifyWorkIsEnqueuedWhenEnqueuerIsInvoked() {
        // given
        val mockPushNotificationData = mockk<Map<String, String>>(relaxed = true)

        // when
        processPushNotificationDataWorkerEnqueuer(mockPushNotificationData)

        // then
        verify { workManager.enqueue(any<OneTimeWorkRequest>()) }
    }

    @Test
    fun returnFailureIfInputDataIsMissing() {
        runBlockingTest {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns ""
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns null
            }
            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "Input data is missing")
            )

            // when
            val workResult = processPushNotificationDataWorker.doWork()

            // then
            assertEquals(expectedResult, workResult)
        }
    }

    @Test
    fun verifyAlarmIsSetIfAppIsNotInBackground() {
        runBlockingTest {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false

            // when
            processPushNotificationDataWorker.doWork()

            // then
            verify { alarmReceiver.setAlarm(any(), true) }
        }
    }

    @Test
    fun verifySettingHasConnectivityToTrue() {
        runBlockingTest {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false

            // when
            processPushNotificationDataWorker.doWork()

            // then
            verify { queueNetworkUtil.setCurrentlyHasConnectivity() }
        }
    }

    @Test
    fun returnFailureIfUserIsUnknownOrInactive() {
        runBlockingTest {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false
            coEvery { sessionManager.getUserId(SessionId("uid")) } returns null

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "User is unknown or inactive")
            )

            // when
            val workerResult = processPushNotificationDataWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun returnFailureIfBackgroundSyncIsDisabled() {
        runBlockingTest {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false
            coEvery { sessionManager.getUserId(SessionId("uid")) } returns testUserId
            coEvery { userManager.getLegacyUser(testId) } returns mockk {
                every { isBackgroundSync } returns false
            }

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "Background sync is disabled")
            )

            // when
            val workerResult = processPushNotificationDataWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun returnFailureIfDecryptionFails() {
        runBlockingTest {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false
            coEvery { sessionManager.getUserId(SessionId("uid")) } returns testUserId
            coEvery { userManager.getLegacyUser(testId) } returns mockk {
                every { isBackgroundSync } returns true
            }
            every { userManager.openPgp } returns mockk(relaxed = true)
            every { anyConstructed<UserCrypto>().decryptMessage(any()) } throws IllegalStateException()

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "Decryption or deserialization error")
            )

            // when
            val workerResult = processPushNotificationDataWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun returnFailureIfDeserializationFails() {
        runBlockingTest {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false
            coEvery { sessionManager.getUserId(SessionId("uid")) } returns testUserId
            coEvery { userManager.getLegacyUser(testId) } returns mockk {
                every { isBackgroundSync } returns true
            }
            every { userManager.openPgp } returns mockk(relaxed = true)
            every { anyConstructed<UserCrypto>().decryptMessage(any()) } returns mockk {
                every { decryptedData } returns "decryptedData"
            }
            every { "decryptedData".deserialize<PushNotification>() } returns mockk {
                every { data } returns null
            }

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "Decryption or deserialization error")
            )

            // when
            val workerResult = processPushNotificationDataWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    private fun mockForCallingSendNotificationSuccessfully(): Pair<PushNotificationSender, PushNotificationData> {
        every { workerParameters.inputData } returns mockk {
            every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
            every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
        }
        every { AppUtil.isAppInBackground() } returns false
        coEvery { sessionManager.getUserId(SessionId("uid")) } returns testUserId
        coEvery { userManager.getLegacyUser(testId) } returns mockk(relaxed = true) {
            every { isBackgroundSync } returns true
            every { toNewUser() } returns mockk(relaxed = true) {
                every { id } returns testId
            }
        }
        every { userManager.openPgp } returns mockk(relaxed = true)
        every { anyConstructed<UserCrypto>().decryptMessage(any()) } returns mockk {
            every { decryptedData } returns "decryptedData"
        }
        val mockNotificationSender = mockk<PushNotificationSender> {
            every { senderName } returns ""
            every { senderAddress } returns "senderAddress"
        }
        val mockNotificationEncryptedData = mockk<PushNotificationData> {
            every { messageId } returns "messageId"
            every { body } returns "body"
            every { sender } returns mockNotificationSender
        }
        every { "decryptedData".deserialize<PushNotification>(any()) } returns mockk {
            every { data } returns mockNotificationEncryptedData
        }
        every { userManager.currentUserId } returns testId
        every { userManager.requireCurrentUserId() } returns testId
        coEvery { userManager.isSnoozeQuickEnabled() } returns false
        every { userManager.isSnoozeScheduledEnabled() } returns true
        every { processPushNotificationDataWorker invokeNoArgs "shouldSuppressNotification" } returns false

        return Pair(mockNotificationSender, mockNotificationEncryptedData)
    }

    @Test
    fun verifyCorrectMethodInvocationAfterDecryptionAndDeserializationSucceedsWhenSnoozingNotificationsIsNotActive() {
        runBlockingTest {
            // given
            val (mockNotificationSender, mockNotificationEncryptedData) = mockForCallingSendNotificationSuccessfully()

            justRun {
                val arguments = listOf(
                    any<Id>(), // userId
                    any<User>(), // user
                    any<String>(), // messageId
                    any<String>(), // notificationBody
                    any<String>(), // sender
                    any<Boolean>() // isPrimaryUser
                )
                processPushNotificationDataWorker invoke "sendNotification" withArguments arguments
            }

            // when
            processPushNotificationDataWorker.doWork()

            // then
            coVerifyOrder {
                mockNotificationEncryptedData.messageId
                mockNotificationEncryptedData.body
                mockNotificationEncryptedData.sender
                mockNotificationSender.senderName
                mockNotificationSender.senderAddress
                userManager.currentUserId
                userManager.isSnoozeQuickEnabled()
                userManager.isSnoozeScheduledEnabled()
                processPushNotificationDataWorker invokeNoArgs "shouldSuppressNotification"
            }
        }
    }

    @Test
    fun verifyNotifySingleNewEmailIsCalledWithCorrectParametersWhenThereIsOneNotificationInTheDB() {
        runBlockingTest {
            // given
            mockForCallingSendNotificationSuccessfully()

            val mockNotification = mockk<Notification>()
            every { databaseProvider.provideNotificationDao(testId) } returns mockk(relaxed = true) {
                every { insertNewNotificationAndReturnAll(any()) } returns listOf(mockNotification)
            }
            val mockMessage = mockk<Message>()
            coEvery { messageRepository.findMessage(testId, "messageId") } returns mockMessage
            every {
                notificationServer.notifySingleNewEmail(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } just runs

            // when
            processPushNotificationDataWorker.doWork()

            // then
            val userManagerSlot = slot<UserManager>()
            val userSlot = slot<ch.protonmail.android.domain.entity.user.User>()
            val messageSlot = slot<Message>()
            val messageIdSlot = slot<String>()
            val notificationBodySlot = slot<String>()
            val senderSlot = slot<String>()
            val primaryUserSlot = slot<Boolean>()
            verify {
                notificationServer.notifySingleNewEmail(
                    capture(userManagerSlot),
                    capture(userSlot),
                    any(),
                    any(),
                    any(),
                    capture(messageSlot),
                    capture(messageIdSlot),
                    capture(notificationBodySlot),
                    capture(senderSlot),
                    capture(primaryUserSlot)
                )
            }
            assertEquals(userManager, userManagerSlot.captured)
            assertEquals(testId, userSlot.captured.id)
            assertEquals(mockMessage, messageSlot.captured)
            assertEquals("messageId", messageIdSlot.captured)
            assertEquals("body", notificationBodySlot.captured)
            assertEquals("senderAddress", senderSlot.captured)
            assertEquals(true, primaryUserSlot.captured)
        }
    }

    @Test
    fun verifyNotifyMultipleUnreadEmailIsCalledWithCorrectParametersWhenThereAreMoreThanOneNotificationsInTheDB() {
        runBlockingTest {
            // given
            mockForCallingSendNotificationSuccessfully()

            val mockNotification1 = mockk<Notification>()
            val mockNotification2 = mockk<Notification>()
            val unreadNotifications = listOf(mockNotification1, mockNotification2)
            every { databaseProvider.provideNotificationDao(testId) } returns mockk(relaxed = true) {
                every { insertNewNotificationAndReturnAll(any()) } returns unreadNotifications
            }
            val mockMessage = mockk<Message>()
            coEvery { messageRepository.findMessage(testId, "messageId") } returns mockMessage
            every { notificationServer.notifyMultipleUnreadEmail(any(), any(), any(), any(), any()) } just runs

            // when
            processPushNotificationDataWorker.doWork()

            // then
            val userSlot = slot<ch.protonmail.android.domain.entity.user.User>()
            val unreadNotificationsSlot = slot<List<Notification>>()
            verify {
                notificationServer.notifyMultipleUnreadEmail(
                    capture(userSlot),
                    any(),
                    any(),
                    any(),
                    capture(unreadNotificationsSlot)
                )
            }
            assertEquals(testId, userSlot.captured.id)
            assertEquals(unreadNotifications, unreadNotificationsSlot.captured)
        }
    }

    @Test
    fun returnSuccessWhenNotificationWasSent() {
        runBlockingTest {
            // given
            mockForCallingSendNotificationSuccessfully()

            val mockNotification = mockk<Notification>()
            every { databaseProvider.provideNotificationDao(testId) } returns mockk(relaxed = true) {
                every { insertNewNotificationAndReturnAll(any()) } returns listOf(mockNotification)
            }
            val mockMessage = mockk<Message>()
            coEvery { messageRepository.findMessage(testId, "messageId") } returns mockMessage
            every { notificationServer.notifySingleNewEmail(any(), any(), any(), any(), any(), any(), any()) } just runs

            val expectedResult = ListenableWorker.Result.success()

            // when
            val workerResult = processPushNotificationDataWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }
}
