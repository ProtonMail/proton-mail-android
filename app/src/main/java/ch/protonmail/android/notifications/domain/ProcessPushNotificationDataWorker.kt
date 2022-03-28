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
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.api.segments.event.FetchEventsAndReschedule
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.notifications.data.remote.model.NotificationAction
import ch.protonmail.android.notifications.data.remote.model.PushNotification
import ch.protonmail.android.notifications.data.remote.model.PushNotificationData
import ch.protonmail.android.notifications.domain.model.NotificationType
import ch.protonmail.android.notifications.presentation.usecase.ClearNotification
import ch.protonmail.android.notifications.presentation.utils.NotificationServer
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.utils.AppUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import me.proton.core.util.kotlin.deserialize
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

const val KEY_PUSH_NOTIFICATION_UID = "UID"
const val KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE = "encryptedMessage"
const val KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR = "ProcessPushNotificationDataError"

/**
 * A worker that is responsible for processing the data payload of the received FCM push notifications.
 */
@HiltWorker
internal class ProcessPushNotificationDataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val notificationServer: NotificationServer,
    private val alarmReceiver: AlarmReceiver,
    private val queueNetworkUtil: QueueNetworkUtil,
    private val userManager: UserManager,
    private val notificationRepository: NotificationRepository,
    private val messageRepository: MessageRepository,
    private val sessionManager: SessionManager,
    private val conversationModeEnabled: ConversationModeEnabled,
    private val fetchEventsAndReschedule: FetchEventsAndReschedule,
    private val clearNotification: ClearNotification
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        // start a foreground service because the following operations will take a longer time to finish
        // and are important to the user
        setForeground(
            ForegroundInfo(
                id.hashCode(),
                notificationServer.createRetrievingNotificationsNotification()
            )
        )

        val sessionId = inputData.getString(KEY_PUSH_NOTIFICATION_UID)
        val encryptedMessage = inputData.getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE)

        if (sessionId.isNullOrEmpty() || encryptedMessage.isNullOrEmpty()) {
            return Result.failure(
                workDataOf(KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "Input data is missing")
            )
        }

        if (!AppUtil.isAppInBackground()) {
            alarmReceiver.setAlarm(applicationContext, true)
        }

        queueNetworkUtil.setCurrentlyHasConnectivity()

        val notificationUserId = sessionManager.getUserId(SessionId(sessionId))
// we do not show notifications for unknown/inactive users
            ?: return Result.failure(
                workDataOf(
                    KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "User is unknown or inactive"
                )
            )

        val userId = UserId(notificationUserId.id)
        val user = userManager.getLegacyUser(userId)
        if (!user.isBackgroundSync) {
            // we do not show notifications for users who have disabled background sync
            return Result.failure(
                workDataOf(
                    KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "Background sync is disabled"
                )
            )
        }

        var pushNotification: PushNotification? = null
        var pushNotificationData: PushNotificationData? = null
        try {
            val userCrypto = UserCrypto(userManager, userManager.openPgp, userId)
            val textDecryptionResult = userCrypto.decryptMessage(encryptedMessage)
            val decryptedData = textDecryptionResult.decryptedData
            pushNotification = decryptedData.deserialize(PushNotification.serializer())
            pushNotificationData = pushNotification.data
        } catch (e: Exception) {
            Timber.e(e, "Error with decryption or deserialization of the notification data")
        }

        if (pushNotification == null || pushNotificationData == null) {
            return Result.failure(
                workDataOf(KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "Decryption or deserialization error")
            )
        }

        val isPrimaryUser = userManager.currentUserId == userId
        val isQuickSnoozeEnabled = userManager.isSnoozeQuickEnabled()
        val isScheduledSnoozeEnabled = userManager.isSnoozeScheduledEnabled()

        when (pushNotificationData.action) {
            NotificationAction.CREATED -> {
                if (!isQuickSnoozeEnabled && (!isScheduledSnoozeEnabled || !shouldSuppressNotification())) {
                    sendNotification(userId, user, pushNotification, isPrimaryUser)
                }
            }
            NotificationAction.TOUCHED -> {
                fetchEventsAndReschedule()
                val notification =
                    checkNotNull(notificationRepository.getNotificationByIdBlocking(pushNotificationData.messageId))
                clearNotification.invoke(userId, notification.id.value)
            }

        }

        return Result.success()
    }

    private suspend fun sendNotification(
        userId: UserId,
        user: User,
        pushNotification: PushNotification,
        isPrimaryUser: Boolean
    ) {


        // Insert current Notification in Database
        val notification = checkNotNull(notificationRepository.saveNotification(pushNotification, userId)) {
            "Notification not found"
        }

        when (notification.type) {
            NotificationType.EMAIL -> {
                val message = messageRepository.getMessage(userId, notification.id.value)

                notificationServer.notifySingleNewEmail(
                    userManager,
                    user.toNewUser(),
                    user.notificationSetting,
                    user.ringtone,
                    user.isNotificationVisibilityLockScreen,
                    message,
                    if (conversationModeEnabled(null, userId)) message?.conversationId ?: ""
                    else notification.id.value,
                    notification.notificationBody,
                    userId = userId,
                    notification.notificationTitle,
                    isPrimaryUser
                )
            }
            NotificationType.OPEN_URL -> {
                notificationServer.notifyOpenUrlNotification(
                    user.toNewUser(),
                    user.notificationSetting,
                    user.ringtone,
                    user.isNotificationVisibilityLockScreen,
                    notification.url,
                    notification.id.value,
                    notification.notificationBody,
                    notification.notificationTitle,
                )
            }

        }

    }

    private fun shouldSuppressNotification(): Boolean {
        val rightNow = Calendar.getInstance()
        return userManager.snoozeSettings?.shouldSuppressNotification(rightNow) ?: false
    }

    class Enqueuer @Inject constructor(
        private val workManager: WorkManager
    ) {

        operator fun invoke(pushNotificationData: Map<String, String>) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putAll(pushNotificationData)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ProcessPushNotificationDataWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            workManager.enqueue(workRequest)
        }
    }
}
