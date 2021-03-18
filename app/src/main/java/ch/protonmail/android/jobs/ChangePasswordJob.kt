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

import android.util.Base64
import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.api.models.LoginInfoResponse
import ch.protonmail.android.api.models.PasswordVerifier
import ch.protonmail.android.api.models.PrivateKeyBody
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.SinglePasswordChange
import ch.protonmail.android.api.models.SrpResponseBody
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.api.models.requests.PasswordChange
import ch.protonmail.android.api.segments.RESPONSE_CODE_NEW_PASSWORD_INCORRECT
import ch.protonmail.android.api.segments.RESPONSE_CODE_NEW_PASSWORD_MESSED_UP
import ch.protonmail.android.api.segments.RESPONSE_CODE_OLD_PASSWORD_INCORRECT
import ch.protonmail.android.api.services.LoginService
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.PasswordMode
import ch.protonmail.android.events.AuthStatus
import ch.protonmail.android.events.PasswordChangeEvent
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.ConstantTime
import ch.protonmail.android.utils.Logger
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.extensions.app
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint

class ChangePasswordJob(
    private val passwordType: Int,
    private val passwordMode: PasswordMode,
    private val oldPassword: ByteArray?,
    private val twoFactorCode: String?,
    private val newPassword: ByteArray
) : ProtonMailBaseJob(Params(Priority.HIGH).requireNetwork()) {

    @Throws(Throwable::class)
    override fun onRun() {

        val openPGP = applicationContext.app.openPGP
        val user = getUserManager().user
        val username = user.name ?: user.username
        val infoResponse = getApi().loginInfo(username)
        val proofs = LoginService.srpProofsForInfo(username, oldPassword, infoResponse, 2)
        var keysResponse: Keys? = null

        if (user.isPaidUser) {
            val organizationResponse = getApi().fetchOrganization()
            if (organizationResponse.code == Constants.RESPONSE_CODE_OK) {
                keysResponse = getApi().fetchOrganizationKeys()
            }
        }

        if (passwordMode == PasswordMode.DUAL) {
            when (passwordType) {
                Constants.PASSWORD_TYPE_LOGIN -> {
                    // region dual password mode change login password
                    val newModulus = getApi().randomModulus()
                    val verifier = PasswordVerifier.calculate(newPassword, newModulus)
                    val response = getApi().updateLoginPassword(PasswordChange(infoResponse.srpSession,
                        ConstantTime.encodeBase64(proofs.clientEphemeral, true),
                        ConstantTime.encodeBase64(proofs.clientProof, true),
                        twoFactorCode, verifier))
                    val invalidServerProof: (SrpResponseBody?) -> Boolean = { srpResponse ->
                        !ConstantTime.isEqual(Base64.decode(srpResponse!!.serverProof,
                            Base64.DEFAULT), proofs.expectedServerProof)
                    }

                    when {
                        response?.code in listOf(RESPONSE_CODE_OLD_PASSWORD_INCORRECT,
                            RESPONSE_CODE_NEW_PASSWORD_INCORRECT,
                            RESPONSE_CODE_NEW_PASSWORD_MESSED_UP) -> {
                            AppUtil.postEventOnUi(PasswordChangeEvent(passwordType, AuthStatus.INVALID_CREDENTIAL,
                                response?.error))
                        }
                        invalidServerProof(response) -> {
                            AppUtil.postEventOnUi(PasswordChangeEvent(passwordType, AuthStatus.INVALID_SERVER_PROOF,
                                response?.error))
                        }
                        else -> {
                            AppUtil.postEventOnUi(PasswordChangeEvent(passwordType, AuthStatus.SUCCESS,
                                response?.error))
                        }
                    }

                }
                Constants.PASSWORD_TYPE_MAILBOX -> {
                    // region dual password mode change mailbox password
                    updatePrivateKeys(passwordMode, openPGP, user, infoResponse, keysResponse)
                }
            }
        } else {
            // region single password mode change password
            updatePrivateKeys(passwordMode, openPGP, user, infoResponse, keysResponse)
        }
    }

    private fun updatePrivateKeys(
        isSingleMode: PasswordMode,
        openPGP: OpenPGP,
        user: User,
        infoResponse: LoginInfoResponse,
        keysResponse: Keys?
    ) {
        val newModulus = getApi().randomModulus()
        val keySalt = openPGP.createNewKeySalt()
        val generatedMailboxPassword = openPGP.generateMailboxPassword(keySalt, newPassword)
        val verifier = if (isSingleMode == PasswordMode.SINGLE)
            PasswordVerifier.calculate(newPassword, newModulus) else null
        val username = user.name ?: user.username
        val proofs = LoginService.srpProofsForInfo(username, oldPassword, infoResponse, 2)
        val userAddresses: List<Address> = user.addresses
        val privateKeyBodies = ArrayList<PrivateKeyBody>()
        val userPrivateKeyBodies = ArrayList<PrivateKeyBody>()
        val userKeys = user.keys
        for (keys in userKeys) {
            updateKey(keys, generatedMailboxPassword, openPGP, privateKeyBodies, false)
        }
        if(!getUserManager().user.legacyAccount) { // non-empty body for UserKeys for migrated users
            userPrivateKeyBodies.addAll(privateKeyBodies)
        }
        for (address in userAddresses) {
            val keysList = address.keys
            for (i in keysList.indices) {
                val keys = keysList[i]
                updateKey(keys, generatedMailboxPassword, openPGP, privateKeyBodies, i == 0)
            }
        }

        var newOrganizationPrivateKey: String? = ""
        keysResponse?.let { it ->
            newOrganizationPrivateKey = if (openPGP.checkPassphrase(it.privateKey, getUserManager().getCurrentUserMailboxPassword()!!)) {
                openPGP.updatePrivateKeyPassphrase(it.privateKey, getUserManager().getCurrentUserMailboxPassword(), generatedMailboxPassword)
            } else {
                null
            }
        }

        val privateKeyArray = privateKeyBodies.toTypedArray()
        val userPrivateKeyArray = userPrivateKeyBodies.toTypedArray()
        val response = getApi().updatePrivateKeys(
            SinglePasswordChange(keySalt, privateKeyArray, userPrivateKeyArray, newOrganizationPrivateKey, infoResponse
                .srpSession, ConstantTime.encodeBase64(proofs.clientEphemeral, true),
                ConstantTime.encodeBase64(proofs.clientProof, true), twoFactorCode, verifier))
        handleUpdatePrivateKeysResponse(response, generatedMailboxPassword)
    }

    private fun handleUpdatePrivateKeysResponse(
        response: ResponseBody,
        generatedMailboxPassword: ByteArray
    ) {
        if (response.code == Constants.RESPONSE_CODE_OK) {
            getUserManager().saveMailboxPasswordBlocking(checkNotNull(userId), generatedMailboxPassword)
            AppUtil.postEventOnUi(PasswordChangeEvent(passwordType, AuthStatus.SUCCESS, response.error))
        } else {
            when (response.code) {
                RESPONSE_CODE_OLD_PASSWORD_INCORRECT,
                RESPONSE_CODE_NEW_PASSWORD_INCORRECT,
                RESPONSE_CODE_NEW_PASSWORD_MESSED_UP -> {
                    AppUtil.postEventOnUi(PasswordChangeEvent(passwordType,
                        AuthStatus.INVALID_CREDENTIAL, response.error))
                }
                else -> {
                    AppUtil.postEventOnUi(PasswordChangeEvent(passwordType, AuthStatus.FAILED, response.error))
                }
            }
        }
    }

    private fun updateKey(
        keys: Keys,
        generatedMailboxPassword: ByteArray,
        openPGP: OpenPGP,
        privateKeyBodies: ArrayList<PrivateKeyBody>,
        throwOnError: Boolean
    ) {
        try {
            try {
                val newPrivateKey = openPGP.updatePrivateKeyPassphrase(keys.privateKey,
                    getUserManager().getCurrentUserMailboxPassword(), generatedMailboxPassword)
                val privateKeyBody = PrivateKeyBody(newPrivateKey, keys.id)
                privateKeyBody.let { privateKeyBodies.add(privateKeyBody) }
            } catch (e: Exception) {
                // should catch keys that are not "decryptable" with the old mailbox password
                Logger.doLogException(TAG_CHANGE_PASSWORD_JOB, e)
            }
        } catch (e: Exception) {
            if (throwOnError) {
                AppUtil.postEventOnUi(PasswordChangeEvent(passwordType, AuthStatus.FAILED))
            }
        }
    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int):
        RetryConstraint = RetryConstraint.CANCEL

    override fun onProtonCancel(cancelReason: Int, throwable: Throwable?) {
        AppUtil.postEventOnUi(PasswordChangeEvent(passwordType, AuthStatus.FAILED))
    }

    companion object {
        private const val TAG_CHANGE_PASSWORD_JOB = "ChangePasswordJob"
    }
}
