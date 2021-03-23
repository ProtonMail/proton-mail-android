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

package ch.protonmail.android.usecase.fetch

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.PublicKeyBody
import ch.protonmail.android.api.models.enumerations.KeyFlag
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.di.CurrentUserCrypto
import ch.protonmail.android.utils.crypto.KeyInformation
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class FetchVerificationKeys @Inject constructor(
    private val api: ProtonMailApiManager,
    private val userManager: UserManager,
    @CurrentUserCrypto private val userCrypto: UserCrypto,
    private val contactDao: ContactDao,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(email: String): List<KeyInformation> = withContext(dispatchers.Io) {
        Timber.v("FetchVerificationKeys email: $email")
        val publicKeys = userManager.requireCurrentUser().addresses.addresses.values
            .find { it.email.s == email }?.keys?.keys
            ?.map { key ->
                val armouredKey = userCrypto.buildArmoredPublicKey(key.privateKey)
                val keyInfo = userCrypto.deriveKeyInfo(armouredKey)
                if (!KeyFlag.fromInteger(key.buildBackEndFlags()).contains(KeyFlag.VERIFICATION_ENABLED)) {
                    keyInfo.flagAsCompromised()
                }
                keyInfo
            }?.toList()

        if (!publicKeys.isNullOrEmpty()) {
            Timber.v("FetchVerificationKeys Success keys $publicKeys")
            return@withContext publicKeys
        }

        val contactEmail = contactDao.findContactEmailByEmail(email)
        contactEmail?.contactId?.let {

            return@withContext runCatching {
                val contactResponse = api.fetchContactDetails(it)
                val fullContactDetails = contactResponse.contact
                contactDao.insertFullContactDetails(fullContactDetails)
                val response = api.getPublicKeys(email)
                if (response.hasError()) {
                    Timber.w("FetchVerificationKeys Error ${response.error}")
                    emptyList()
                } else {
                    val trustedKeys = fullContactDetails.getPublicKeys(userCrypto, email)
                    val verificationKeys = filterVerificationKeys(userCrypto, response.keys, trustedKeys)
                    Timber.v("FetchVerificationKeys Success verificationKeys $verificationKeys")
                    verificationKeys
                }
            }.fold(
                onSuccess = { it },
                onFailure = {
                    Timber.w(it, "FetchVerificationKeys failure")
                    emptyList()
                }
            )

        } ?: emptyList()
    }

    private fun filterVerificationKeys(
        crypto: UserCrypto,
        publicKeyBodies: Array<PublicKeyBody>,
        trustedKeys: List<String>
    ): List<KeyInformation> {

        val bannedFingerprints = mutableListOf<String>()
        for (body in publicKeyBodies) {
            if (!body.isAllowedForVerification) {
                val keyInfo = crypto.deriveKeyInfo(body.publicKey)
                keyInfo.fingerprint?.let {
                    bannedFingerprints.add(it)
                }
            }
        }

        return trustedKeys.map { pubKey ->
            val keyInfo = crypto.deriveKeyInfo(pubKey)
            if (bannedFingerprints.contains(keyInfo.fingerprint)) {
                keyInfo.flagAsCompromised()
            }
            keyInfo
        }.toList()
    }

}
