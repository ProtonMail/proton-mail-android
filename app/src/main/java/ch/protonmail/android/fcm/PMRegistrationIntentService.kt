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
import android.content.Intent
import androidx.core.app.ProtonJobIntentService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.RegisterDeviceBody
import ch.protonmail.android.core.Constants
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

// region constants
private const val PREF_REGISTRATION_COMPLETE = "registrationComplete"
private const val EXTRA_FCM_TOKEN = "fcmToken"
// endregion

@AndroidEntryPoint
class PMRegistrationIntentService : ProtonJobIntentService() {

    @Inject
    internal lateinit var protonMailApiManager: ProtonMailApiManager

    override fun onHandleWork(intent: Intent) {
        try {
            sendRegistrationToServer(requireNotNull(intent.getStringExtra(EXTRA_FCM_TOKEN)))
            FcmUtil.setTokenSent(true)
        } catch (e: Exception) {
            Timber.w(e, "Failed to complete token refresh")
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            FcmUtil.setTokenSent(false)
        }

        // Notify UI that registration has completed, so the progress indicator can be hidden.
        val registrationComplete = Intent(PREF_REGISTRATION_COMPLETE)
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete)
    }

    /**
     * Persist registration to PM servers.
     *
     * @param token The new token.
     */
    @Throws(Exception::class)
    private fun sendRegistrationToServer(token: String) {
        Timber.v("Send Fcm token $token")
        FcmUtil.setRegistrationId(token)

        var exception: Exception? = null
        for (username in AccountManager.getInstance(baseContext).getLoggedInUsers()) {
            try {
                protonMailApiManager.registerDevice(RegisterDeviceBody(token), username)
            } catch (ioException: IOException) {
                Timber.w(ioException, "error registering user for FCM")
                exception = ioException
            }

        }

        if (exception != null) {
            throw exception
        }
    }

    companion object {

        fun startRegistration(context: Context, token: String) {
            val intent = Intent(context, PMRegistrationIntentService::class.java)
            intent.putExtra(EXTRA_FCM_TOKEN, token)
            enqueueWork(
                context,
                PMRegistrationIntentService::class.java,
                Constants.JOB_INTENT_SERVICE_ID_REGISTRATION, intent
            )
        }
    }
}
