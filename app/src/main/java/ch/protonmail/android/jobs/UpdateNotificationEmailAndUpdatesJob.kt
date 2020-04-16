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
package ch.protonmail.android.jobs

import android.text.TextUtils
import android.util.Base64
import ch.protonmail.android.R
import ch.protonmail.android.api.segments.RESPONSE_CODE_INCORRECT_PASSWORD
import ch.protonmail.android.api.segments.RESPONSE_CODE_INVALID_EMAIL
import ch.protonmail.android.api.services.LoginService
import ch.protonmail.android.core.Constants
import ch.protonmail.android.events.AuthStatus
import ch.protonmail.android.events.NotificationsUpdatedEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.ConstantTime
import com.birbit.android.jobqueue.Params

class UpdateNotificationEmailAndUpdatesJob(private val email: String, private val password: ByteArray, private val displayName: String, private val updateNotify: Boolean) : ProtonMailBaseJob(Params(Priority.LOW).requireNetwork().persist()) {

    @Throws(Throwable::class)
    override fun onRun() {
        var failed = false
        try {
            val infoResponse = mApi.loginInfo(mUserManager.username)

            val proofs = LoginService.srpProofsForInfo(mUserManager.username, password, infoResponse, 2)
            val response = mApi.updateNotificationEmail(infoResponse.srpSession, ConstantTime.encodeBase64(proofs!!.clientEphemeral, true), ConstantTime.encodeBase64(proofs.clientProof, true), null, email)

            val responseUpdateNotify = mApi.updateNotify(updateNotify)
            if (!TextUtils.isEmpty(displayName)) {
                mApi.updateDisplayName(displayName)
            }

            var emailStatus = AuthStatus.SUCCESS
            var updateNotifyStatus = Status.FAILED

            if (response!!.code == RESPONSE_CODE_INVALID_EMAIL) {
                emailStatus = AuthStatus.FAILED
                failed = true
            } else if (response.code == RESPONSE_CODE_INCORRECT_PASSWORD) {
                emailStatus = AuthStatus.INVALID_CREDENTIAL
                failed = true
            } else if (!ConstantTime.isEqual(Base64.decode(response.serverProof, Base64.DEFAULT), proofs.expectedServerProof)) {
                emailStatus = AuthStatus.INVALID_SERVER_PROOF
                failed = true
            }
            if (responseUpdateNotify!!.code != Constants.RESPONSE_CODE_OK) {
                updateNotifyStatus = Status.FAILED
            }
            val user = mUserManager.user

            if (!TextUtils.isEmpty(displayName)) {
                user.addresses[0].displayName = displayName
            }

            if (!failed) {
                if (TextUtils.isEmpty(email)) {
                    mUserManager.userSettings!!.notificationEmail = applicationContext.resources.getString(R.string.not_set)
                } else {
                    mUserManager.userSettings!!.notificationEmail = email

                }
                AppUtil.postEventOnUi(NotificationsUpdatedEvent(emailStatus, updateNotifyStatus, email, displayName))
            }

                mUserManager.user = user

        } catch (e: Exception) {
            AppUtil.postEventOnUi(NotificationsUpdatedEvent(AuthStatus.FAILED, Status.FAILED))
        }
    }
}
