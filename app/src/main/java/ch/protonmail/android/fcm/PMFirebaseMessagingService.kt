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

import android.content.Intent
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ch.protonmail.android.R
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.fcm.model.FirebaseToken
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * An extension of FirebaseMessagingService, handling token refreshing and receiving messages.
 */
@AndroidEntryPoint
public class PMFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var multiUserFcmTokenManager: MultiUserFcmTokenManager

    @Inject
    lateinit var pmRegistrationWorkerEnqueuer: PMRegistrationWorker.Enqueuer
    @Inject
    lateinit var processPushNotificationData: ProcessPushNotificationDataWorker.Enqueuer

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        multiUserFcmTokenManager.run {
            setTokenUnsentForAllSavedUsersBlocking()
            saveTokenBlocking(FirebaseToken(token))
        }
        pmRegistrationWorkerEnqueuer()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (remoteMessage.data.isNotEmpty()) {
            val broadcastIntent = Intent()
            val bundle = Bundle()
            for ((key, value) in remoteMessage.data) {
                bundle.putString(key, value)
            }
            broadcastIntent.putExtras(bundle)
            broadcastIntent.action = baseContext.getString(R.string.action_notification)
            if (!LocalBroadcastManager.getInstance(baseContext).sendBroadcast(broadcastIntent)) {
                processPushNotificationData(remoteMessage.data)
            }
        }
    }
}
