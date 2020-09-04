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
package ch.protonmail.android.gcm

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.ProtonJobIntentService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.RegisterDeviceBody
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.Logger
import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.android.gms.iid.InstanceID
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// region constants
private const val TAG_PM_REGISTRATION_INTENT_SERVICE = "PMRegIntentService"
private const val PREF_REGISTRATION_COMPLETE = "registrationComplete"
// endregion

/*
 * Created by dkadrikj on 1/7/16.
 */

@AndroidEntryPoint
class PMRegistrationIntentService : ProtonJobIntentService() {

    @Inject
    internal lateinit var mApi: ProtonMailApiManager

    @Inject
    internal lateinit var mUserManager: UserManager

    override fun onHandleWork(intent: Intent) {
        try {
            val instanceID = InstanceID.getInstance(this)
            val token = instanceID.getToken(GcmUtil.SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null)

            sendRegistrationToServer(token)

            GcmUtil.setTokenSent(true)
        } catch (e: Exception) {
            Logger.doLogException(TAG_PM_REGISTRATION_INTENT_SERVICE, "Failed to complete token refresh", e)
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            GcmUtil.setTokenSent(false)
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
        GcmUtil.setRegistrationId(token)

        var exception: Exception? = null
        for (username in AccountManager.getInstance(baseContext).getLoggedInUsers()) {
            try {
                mApi!!.registerDevice(RegisterDeviceBody(ProtonMailApplication.getApplication()), username)
            } catch (e: Exception) {
                Log.e(TAG_PM_REGISTRATION_INTENT_SERVICE, "error registering user $username for GCM", e)
                exception = e
            }

        }

        if (exception != null) {
            throw exception
        }
    }

    companion object {

        fun startRegistration(context: Context) {
            val intent = Intent(context, PMRegistrationIntentService::class.java)
            enqueueWork(context, PMRegistrationIntentService::class.java, Constants.JOB_INTENT_SERVICE_ID_REGISTRATION, intent)
        }
    }
}
