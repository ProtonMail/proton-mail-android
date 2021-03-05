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
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.segments.RESPONSE_CODE_EMAIL_FAILED_VALIDATION
import ch.protonmail.android.api.segments.RESPONSE_CODE_INCORRECT_PASSWORD
import ch.protonmail.android.api.segments.RESPONSE_CODE_INVALID_EMAIL
import ch.protonmail.android.api.segments.RESPONSE_CODE_NEW_PASSWORD_INCORRECT
import ch.protonmail.android.api.segments.RESPONSE_CODE_OLD_PASSWORD_INCORRECT
import ch.protonmail.android.api.services.LoginService
import ch.protonmail.android.events.AuthStatus
import ch.protonmail.android.events.SettingsChangedEvent
import ch.protonmail.android.featureflags.FeatureFlagsManager
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.ConstantTime
import ch.protonmail.android.utils.Logger
import com.birbit.android.jobqueue.Params
import me.proton.core.util.kotlin.EMPTY_STRING
import java.util.ArrayList

class UpdateSettingsJob(
    private val displayChanged: Boolean = false,
    private val newDisplayName: String = "",
    private val notificationEmailChanged: Boolean = false,
    private val signatureChanged: Boolean = false,
    private val newSignature: String = "",
    private val sortAliasChanged: Boolean = false,
    private val actionLeftSwipeChanged: Boolean = false,
    private val actionRightSwipeChanged: Boolean = false,
    private val newEmail: String = "",
    private val mailSettings: MailSettings? = null,
    private val backPressed: Boolean = false,
    private val addressId: String = "",
    private val password: ByteArray = byteArrayOf(),
    private val twoFactor: String = "",
    private val featureFlags: FeatureFlagsManager = FeatureFlagsManager()
) : ProtonMailBaseJob(Params(Priority.LOW).requireNetwork()) {
    private var oldEmail: String? = null

    @Throws(Throwable::class)
    override fun onRun() {
        var failed = false
        try {
            val user = getUserManager().requireCurrentLegacyUserBlocking()
            if (notificationEmailChanged) {
                val username = user.username ?: user.name
                val infoResponse = getApi().loginInfo(username)
                val proofs = LoginService.srpProofsForInfo(username, password, infoResponse, 2)
                val response = getApi().updateNotificationEmail(
                    infoResponse.srpSession,
                    ConstantTime.encodeBase64(proofs!!.clientEphemeral, true),
                    ConstantTime.encodeBase64(proofs.clientProof, true), twoFactor, newEmail
                )

                if (response!!.code == RESPONSE_CODE_INVALID_EMAIL) {
                    AppUtil.postEventOnUi(
                        SettingsChangedEvent(
                            AuthStatus.FAILED,
                            getUserManager().userSettings?.notificationEmail, backPressed, response.error
                        )
                    )
                    failed = true
                } else if (response.code == RESPONSE_CODE_INCORRECT_PASSWORD ||
                    response.code == RESPONSE_CODE_OLD_PASSWORD_INCORRECT ||
                    response.code == RESPONSE_CODE_NEW_PASSWORD_INCORRECT
                ) {
                    AppUtil.postEventOnUi(
                        SettingsChangedEvent(
                            AuthStatus.FAILED,
                            getUserManager().userSettings?.notificationEmail, backPressed, response.error
                        )
                    )
                    failed = true
                } else if (response.code == RESPONSE_CODE_EMAIL_FAILED_VALIDATION) {
                    AppUtil.postEventOnUi(
                        SettingsChangedEvent(
                            AuthStatus.FAILED,
                            getUserManager().userSettings?.notificationEmail, backPressed, response.error
                        )
                    )
                    failed = true
                } else if (!ConstantTime.isEqual(
                        Base64.decode(response.serverProof, Base64.DEFAULT),
                        proofs.expectedServerProof
                    )
                ) {
                    AppUtil.postEventOnUi(
                        SettingsChangedEvent(
                            AuthStatus.INVALID_SERVER_PROOF,
                            getUserManager().userSettings?.notificationEmail, backPressed, EMPTY_STRING
                        )
                    )
                    failed = true
                }
                if (!failed) {
                    oldEmail = getUserManager().userSettings?.notificationEmail
                    if (TextUtils.isEmpty(newEmail)) {
                        getUserManager().userSettings?.notificationEmail = applicationContext.resources.getString(R.string.not_set)
                    } else {
                        getUserManager().userSettings?.notificationEmail = newEmail
                    }
                }
            }
            if (!failed) {
                if (sortAliasChanged) {
                    val addresses = getUserManager().user.addresses
                    val aliasSize = addresses.size
                    val newAliasesOrder = ArrayList<String>(aliasSize)
                    for (i in 0 until aliasSize) {
                        newAliasesOrder.add(addresses[i].id)
                    }
                    getApi().updateAlias(newAliasesOrder)
                }
                if (displayChanged && addressId.isNotBlank()) {
                    val addresses = user.addresses
                    for (address in addresses) {
                        if (address.id == addressId) {
                            val responseBody = getApi().editAddress(address.id, newDisplayName, address.signature)
                            Logger.doLog(
                                "editAddress",
                                "address: " + address.email + " new DN: " +
                                    newDisplayName + " response: " + responseBody.code
                            )
                            address.displayName = newDisplayName
                            break
                        }
                    }
                    user.setAddresses(addresses)
                    user.save()
                }
                if (signatureChanged && !TextUtils.isEmpty(addressId)) {
                    val addresses = user.addresses
                    for (address in user.addresses) {
                        if (address.id == addressId) {
                            getApi().editAddress(address.id, address.displayName, newSignature)
                            address.signature = newSignature
                            break
                        }
                    }
                    user.setAddresses(addresses)
                    user.save()
                }
                val mailSettings = getUserManager().getCurrentUserMailSettingsBlocking()!!
                if (actionLeftSwipeChanged) {
                    getApi().updateLeftSwipe(mailSettings.leftSwipeAction)
                }
                if (actionRightSwipeChanged) {
                    getApi().updateRightSwipe(mailSettings.rightSwipeAction)
                }
                if (mailSettings != null) {
                    updateMailSettings(mailSettings)
                }
                AppUtil.postEventOnUi(SettingsChangedEvent(AuthStatus.SUCCESS, oldEmail, backPressed, null))
            }
        } catch (e: Exception) {
            AppUtil.postEventOnUi(SettingsChangedEvent(AuthStatus.FAILED, null, backPressed, e.message))
        }

    }

    private fun updateMailSettings(mailSettings: MailSettings) {
        getApi().updateAutoShowImages(mailSettings.showImages)

        if (featureFlags.isChangeViewModeFeatureEnabled()) {
            getApi().updateViewMode(mailSettings.viewMode)
        }
    }
}
