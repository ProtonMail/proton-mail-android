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
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.notifications.Notification
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.fcm.models.PushNotification
import ch.protonmail.android.fcm.models.PushNotificationData
import ch.protonmail.android.fcm.models.PushNotificationSender
import ch.protonmail.android.servers.notification.NotificationServer
import ch.protonmail.android.utils.AppUtil
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
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
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import me.proton.core.util.kotlin.deserialize
import org.junit.After
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the functionality of [ProcessPushNotificationDataWorker].
 */

class ProcessPushNotificationDataWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context
    @RelaxedMockK
    private lateinit var userManager: UserManager
    @RelaxedMockK
    private lateinit var workerParameters: WorkerParameters
    @RelaxedMockK
    private lateinit var workManager: WorkManager

    @MockK
    private lateinit var alarmReceiver: AlarmReceiver
    @MockK
    private lateinit var databaseProvider: DatabaseProvider
    @MockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository
    @MockK
    private lateinit var notificationServer: NotificationServer
    @MockK
    private lateinit var protonMailApiManager: ProtonMailApiManager
    @MockK
    private lateinit var queueNetworkUtil: QueueNetworkUtil

    private lateinit var processPushNotificationDataWorker: ProcessPushNotificationDataWorker
    private lateinit var processPushNotificationDataWorkerEnqueuer: ProcessPushNotificationDataWorker.Enqueuer

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkConstructor(UserCrypto::class)
        mockkStatic(AppUtil::class)
        mockkStatic("me.proton.core.util.kotlin.SerializationUtilsKt")

        processPushNotificationDataWorker = spyk(
            ProcessPushNotificationDataWorker(
                context,
                workerParameters,
                notificationServer,
                alarmReceiver,
                queueNetworkUtil,
                userManager,
                databaseProvider,
                messageDetailsRepository,
                protonMailApiManager
            ),
            recordPrivateCalls = true
        )
        processPushNotificationDataWorkerEnqueuer = ProcessPushNotificationDataWorker.Enqueuer(
            workManager
        )
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
        runBlocking {
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
        runBlocking {
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
        runBlocking {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false

            // when
            processPushNotificationDataWorker.doWork()

            // then
            verify { queueNetworkUtil.setCurrentlyHasConnectivity(true) }
        }
    }

    @Test
    fun returnFailureIfUserIsUnknownOrInactive() {
        runBlocking {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false
            every { userManager.getUsernameBySessionId("uid") } returns null

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
        runBlocking {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false
            every { userManager.getUsernameBySessionId("uid") } returns "username"
            every { userManager.getUser("username") } returns mockk {
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
        runBlocking {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false
            every { userManager.getUsernameBySessionId("uid") } returns "username"
            every { userManager.getUser("username") } returns mockk {
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
        runBlocking {
            // given
            every { workerParameters.inputData } returns mockk {
                every { getString(KEY_PUSH_NOTIFICATION_UID) } returns "uid"
                every { getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE) } returns "encryptedMessage"
            }
            every { AppUtil.isAppInBackground() } returns false
            every { userManager.getUsernameBySessionId("uid") } returns "username"
            every { userManager.getUser("username") } returns mockk {
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
        every { userManager.getUsernameBySessionId("uid") } returns "username"
        every { userManager.getUser("username") } returns mockk {
            every { isBackgroundSync } returns true
            every { username } returns "username"
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
        every { userManager.username } returns "username"
        every { userManager.isSnoozeQuickEnabled() } returns false
        every { userManager.isSnoozeScheduledEnabled() } returns true
        every { processPushNotificationDataWorker invokeNoArgs "shouldSuppressNotification" } returns false

        return Pair(mockNotificationSender, mockNotificationEncryptedData)
    }

    @Test
    fun verifyCorrectMethodInvocationAfterDecryptionAndDeserializationSucceedsWhenSnoozingNotificationsIsNotActive() {
        runBlocking {
            // given
            val (mockNotificationSender, mockNotificationEncryptedData) = mockForCallingSendNotificationSuccessfully()

            justRun { processPushNotificationDataWorker invoke "sendNotification" withArguments listOf(any<User>(), any<String>(), any<String>(), any<String>(), any<Boolean>()) }

            // when
            processPushNotificationDataWorker.doWork()

            // then
            verifyOrder {
                mockNotificationEncryptedData.messageId
                mockNotificationEncryptedData.body
                mockNotificationEncryptedData.sender
                mockNotificationSender.senderName
                mockNotificationSender.senderAddress
                userManager.username
                userManager.isSnoozeQuickEnabled()
                userManager.isSnoozeScheduledEnabled()
                processPushNotificationDataWorker invokeNoArgs "shouldSuppressNotification"
            }
        }
    }

    @Test
    fun verifyNotifySingleNewEmailIsCalledWithCorrectParametersWhenThereIsOneNotificationInTheDB() {
        runBlocking {
            // given
            mockForCallingSendNotificationSuccessfully()

            val mockNotification = mockk<Notification>()
            every { databaseProvider.provideNotificationsDao("username") } returns mockk(relaxed = true) {
                every { insertNewNotificationAndReturnAll(any()) } returns listOf(mockNotification)
            }
            val mockMessage = mockk<Message>()
            every { processPushNotificationDataWorker invoke "fetchMessage" withArguments listOf(any<User>(), any<String>()) } returns mockMessage
            every { notificationServer.notifySingleNewEmail(any(), any(), any(), any(), any(), any(), any()) } just runs

            // when
            processPushNotificationDataWorker.doWork()

            // then
            val userManagerSlot = slot<UserManager>()
            val userSlot = slot<User>()
            val messageSlot = slot<Message>()
            val messageIdSlot = slot<String>()
            val notificationBodySlot = slot<String>()
            val senderSlot = slot<String>()
            val primaryUserSlot = slot<Boolean>()
            verify {
                notificationServer.notifySingleNewEmail(capture(userManagerSlot), capture(userSlot), capture(messageSlot), capture(messageIdSlot), capture(notificationBodySlot), capture(senderSlot), capture(primaryUserSlot))
            }
            assertEquals(userManager, userManagerSlot.captured)
            assertEquals(userManager.getUser("username"), userSlot.captured)
            assertEquals(mockMessage, messageSlot.captured)
            assertEquals("messageId", messageIdSlot.captured)
            assertEquals("body", notificationBodySlot.captured)
            assertEquals("senderAddress", senderSlot.captured)
            assertEquals(true, primaryUserSlot.captured)
        }
    }

    @Test
    fun verifyNotifyMultipleUnreadEmailIsCalledWithCorrectParametersWhenThereAreMoreThanOneNotificationsInTheDB() {
        runBlocking {
            // given
            mockForCallingSendNotificationSuccessfully()

            val mockNotification1 = mockk<Notification>()
            val mockNotification2 = mockk<Notification>()
            val unreadNotifications = listOf(mockNotification1, mockNotification2)
            every { databaseProvider.provideNotificationsDao("username") } returns mockk(relaxed = true) {
                every { insertNewNotificationAndReturnAll(any()) } returns unreadNotifications
            }
            val mockMessage = mockk<Message>()
            every { processPushNotificationDataWorker invoke "fetchMessage" withArguments listOf(any<User>(), any<String>()) } returns mockMessage
            every { notificationServer.notifyMultipleUnreadEmail(any(), any(), any()) } just runs

            // when
            processPushNotificationDataWorker.doWork()

            // then
            val userManagerSlot = slot<UserManager>()
            val userSlot = slot<User>()
            val unreadNotificationsSlot = slot<List<Notification>>()
            verify {
                notificationServer.notifyMultipleUnreadEmail(capture(userManagerSlot), capture(userSlot), capture(unreadNotificationsSlot))
            }
            assertEquals(userManager, userManagerSlot.captured)
            assertEquals(userManager.getUser("username"), userSlot.captured)
            assertEquals(unreadNotifications, unreadNotificationsSlot.captured)
        }
    }

    @Test
    fun returnSuccessWhenNotificationWasSent() {
        runBlocking {
            // given
            mockForCallingSendNotificationSuccessfully()

            val mockNotification = mockk<Notification>()
            every { databaseProvider.provideNotificationsDao("username") } returns mockk(relaxed = true) {
                every { insertNewNotificationAndReturnAll(any()) } returns listOf(mockNotification)
            }
            val mockMessage = mockk<Message>()
            every { processPushNotificationDataWorker invoke "fetchMessage" withArguments listOf(any<User>(), any<String>()) } returns mockMessage
            every { notificationServer.notifySingleNewEmail(any(), any(), any(), any(), any(), any(), any()) } just runs

            val expectedResult = ListenableWorker.Result.success()

            // when
            val workerResult = processPushNotificationDataWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }
}
