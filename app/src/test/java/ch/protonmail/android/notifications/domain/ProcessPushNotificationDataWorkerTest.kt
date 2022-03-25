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

package ch.protonmail.android.notifications.domain

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.api.segments.event.FetchEventsAndReschedule
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.notifications.data.remote.model.NotificationAction
import ch.protonmail.android.notifications.data.remote.model.PushNotification
import ch.protonmail.android.notifications.data.remote.model.PushNotificationData
import ch.protonmail.android.notifications.data.remote.model.PushNotificationSender
import ch.protonmail.android.notifications.domain.model.Notification
import ch.protonmail.android.notifications.domain.model.NotificationId
import ch.protonmail.android.notifications.domain.model.NotificationType
import ch.protonmail.android.notifications.presentation.usecase.ClearNotification
import ch.protonmail.android.notifications.presentation.utils.NotificationServer
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.utils.AppUtil
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
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

    private val testId = UserId("id")
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

    private val notificationRepository: NotificationRepository = mockk(relaxed = true)

    private val messageRepository: MessageRepository = mockk(relaxed = true)

    private val notificationServer: NotificationServer = mockk(relaxed = true) {
        every { createRetrievingNotificationsNotification() } returns mockk()
    }

    private val queueNetworkUtil: QueueNetworkUtil = mockk(relaxed = true)

    private val conversationModeEnabled: ConversationModeEnabled = mockk(relaxed = true)

    private val fetchEventsAndReschedule: FetchEventsAndReschedule = mockk(relaxed = true)

    private val clearNotification: ClearNotification = mockk(relaxed = true)

    private val processPushNotificationDataWorker: ProcessPushNotificationDataWorker = spyk(
        ProcessPushNotificationDataWorker(
            context,
            workerParameters,
            notificationServer,
            alarmReceiver,
            queueNetworkUtil,
            userManager,
            notificationRepository,
            messageRepository,
            sessionManager,
            conversationModeEnabled,
            fetchEventsAndReschedule,
            clearNotification
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

    private fun mockForCallingSendNotificationSuccessfully(
        notificationAction: NotificationAction
    ): PushNotification {
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
            every { body } returns "subject"
            every { sender } returns mockNotificationSender
            every { action } returns notificationAction

        }
        val mockPushNotification = mockk<PushNotification> {
            every { data } returns mockNotificationEncryptedData
        }

        every { "decryptedData".deserialize<PushNotification>(any()) } returns mockPushNotification

        every { userManager.currentUserId } returns testId
        every { userManager.requireCurrentUserId() } returns testId
        coEvery { userManager.isSnoozeQuickEnabled() } returns false
        every { userManager.isSnoozeScheduledEnabled() } returns true
        every { processPushNotificationDataWorker invokeNoArgs "shouldSuppressNotification" } returns false

        return mockPushNotification
    }

    @Test
    fun verifyCorrectMethodInvocationAfterDecryptionAndDeserializationSucceedsWhenSnoozingNotificationsIsNotActive() {
        runBlockingTest {
            // given
            val mockPushNotification = mockForCallingSendNotificationSuccessfully(NotificationAction.CREATED)

            justRun {
                val arguments = listOf(
                    any<UserId>(), // userId
                    any<User>(), // user
                    any<PushNotification>(), // notification
                    any<Boolean>() // isPrimaryUser
                )
                processPushNotificationDataWorker invoke "sendNotification" withArguments arguments
            }

            // when
            processPushNotificationDataWorker.doWork()

            // then
            coVerifyOrder {
                mockPushNotification.data
                userManager.currentUserId
                userManager.isSnoozeQuickEnabled()
                userManager.isSnoozeScheduledEnabled()
                processPushNotificationDataWorker invokeNoArgs "shouldSuppressNotification"
            }
        }
    }

    @Test
    fun `verify notifySingleNewEmail is called with correct parameters when the notification type is EMAIL`() {
        runBlockingTest {
            // given
            mockForCallingSendNotificationSuccessfully(NotificationAction.CREATED)

            val mockNotification = getTestNotification(NotificationType.EMAIL)
            coEvery { notificationRepository.saveNotification(any(), any()) } returns mockNotification
            val mockMessage = mockk<Message>()
            coEvery { messageRepository.getMessage(testId, "messageId") } returns mockMessage
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
                    testId,
                    capture(senderSlot),
                    capture(primaryUserSlot)
                )
            }
            assertEquals(userManager, userManagerSlot.captured)
            assertEquals(testId, userSlot.captured.id)
            assertEquals(mockMessage, messageSlot.captured)
            assertEquals("messageId", messageIdSlot.captured)
            assertEquals("subject", notificationBodySlot.captured)
            assertEquals("senderAddress", senderSlot.captured)
            assertEquals(true, primaryUserSlot.captured)
        }
    }

    @Test
    fun `verify notifyOpenUrlNotification is called with correct parameters when the notification type is OPEN_URL`() {
        runBlockingTest {
            // given
            mockForCallingSendNotificationSuccessfully(NotificationAction.CREATED)

            val mockNotification = getTestNotification(NotificationType.OPEN_URL)
            coEvery { notificationRepository.saveNotification(any(), any()) } returns mockNotification
            val mockMessage = mockk<Message>()
            coEvery { messageRepository.getMessage(testId, "messageId") } returns mockMessage
            every {
                notificationServer.notifyOpenUrlNotification(
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
            val userSlot = slot<ch.protonmail.android.domain.entity.user.User>()
            val notificationUrlSlot = slot<String>()
            val messageIdSlot = slot<String>()
            val notificationBodySlot = slot<String>()
            val senderSlot = slot<String>()
            verify {
                notificationServer.notifyOpenUrlNotification(
                    capture(userSlot),
                    any(),
                    any(),
                    any(),
                    capture(notificationUrlSlot),
                    capture(messageIdSlot),
                    capture(notificationBodySlot),
                    capture(senderSlot)
                )
            }
            assertEquals(testId, userSlot.captured.id)
            assertEquals("https://www.example.com/", notificationUrlSlot.captured)
            assertEquals("messageId", messageIdSlot.captured)
            assertEquals("subject", notificationBodySlot.captured)
            assertEquals("senderAddress", senderSlot.captured)
        }
    }

    @Test
    fun `return success when notification was sent`() {
        runBlockingTest {
            // given
            mockForCallingSendNotificationSuccessfully(NotificationAction.CREATED)

            val mockNotification = getTestNotification()
            coEvery { notificationRepository.saveNotification(any(), any()) } returns mockNotification
            val mockMessage = mockk<Message>()
            coEvery { messageRepository.getMessage(testId, "messageId") } returns mockMessage
            every {
                notificationServer.notifySingleNewEmail(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
                )
            } just runs

            val expectedResult = ListenableWorker.Result.success()

            // when
            val workerResult = processPushNotificationDataWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun `verify that notifySingleNewEmail is not called when notification action is TOUCHED`() {
        runBlockingTest {
            // given
            mockForCallingSendNotificationSuccessfully(NotificationAction.TOUCHED)

            // when
            processPushNotificationDataWorker.doWork()

            // then
            verify(exactly = 0) {
                notificationServer.notifySingleNewEmail(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
                )
            }
        }
    }

    @Test
    fun `verify that notification is cleared when notification action is TOUCHED`() {
        runBlockingTest {
            // given
            mockForCallingSendNotificationSuccessfully(NotificationAction.TOUCHED)
            every {
                notificationRepository.getNotificationByIdBlocking("messageId")
            } returns getTestNotification()

            // when
            processPushNotificationDataWorker.doWork()

            // then
            coVerify {
                clearNotification.invoke(testId, "messageId")
            }
        }
    }

    private fun getTestNotification(type: NotificationType = NotificationType.EMAIL) = Notification(
        id = NotificationId("messageId"),
        notificationTitle = "senderAddress",
        notificationBody = "subject",
        url = "https://www.example.com/",
        type = type
    )
}
