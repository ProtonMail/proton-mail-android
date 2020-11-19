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

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.api.models.room.contacts.FullContactDetails
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.CipherText
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.usecase.model.FetchContactDetailsResult
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class FetchContactDetails @Inject constructor(
    private val contactsDao: ContactsDao,
    private val userManager: UserManager,
    private val api: ProtonMailApiManager,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(contactId: String): LiveData<FetchContactDetailsResult> =
        liveData(dispatchers.Io) {
            if (contactId.isBlank()) {
                throw IllegalArgumentException("Cannot fetch contact with an empty id")
            }

            // fetch existing data from the DB
            contactsDao.findFullContactDetailsById(contactId)?.let { fullDetailsFromDb ->
                val parsedContact = parseContactDetails(fullDetailsFromDb)
                Timber.v("Fetched existing Contacts Details $parsedContact")
                if (parsedContact != null) {
                    emit(parsedContact)
                }
            }

            // fetch data from the server
            runCatching {
                api.fetchContactDetails(contactId)
            }.fold(
                onSuccess = { response ->
                    val fetchedContact = response.contact
                    Timber.v("Fetched new Contact Details $fetchedContact")
                    contactsDao.insertFullContactDetails(fetchedContact)
                    val parsedContact = parseContactDetails(fetchedContact)
                    parsedContact?.let {
                        emit(it)
                    }
                },
                onFailure = {
                    emit(FetchContactDetailsResult.Error(it))
                }
            )
        }

    private fun parseContactDetails(contact: FullContactDetails): FetchContactDetailsResult? {
        val encryptedDataList: List<ContactEncryptedData>? = contact.encryptedData

        if (!encryptedDataList.isNullOrEmpty()) {
            val crypto = UserCrypto(userManager, userManager.openPgp, Name(userManager.username))
            for (contactEncryptedData in encryptedDataList) {
                return when (getCardTypeFromInt(contactEncryptedData.type)) {
                    Constants.VCardType.SIGNED_ENCRYPTED -> {
                        val tct = CipherText(contactEncryptedData.data)
                        FetchContactDetailsResult.Data(
                            decryptedVCardType3 = crypto.decrypt(tct).decryptedData,
                            vCardType3Signature = contactEncryptedData.signature
                        )
                    }
                    Constants.VCardType.SIGNED -> {
                        FetchContactDetailsResult.Data(
                            decryptedVCardType2 = contactEncryptedData.data,
                            vCardType2Signature = contactEncryptedData.signature
                        )
                    }
                    Constants.VCardType.UNSIGNED -> {
                        FetchContactDetailsResult.Data(
                            decryptedVCardType0 = contactEncryptedData.data
                        )
                    }
                }
            }
        }
        return null
    }

    private fun getCardTypeFromInt(vCardTypeValue: Int): Constants.VCardType {
        return Constants.VCardType.values().find {
            vCardTypeValue == it.vCardTypeValue
        } ?: Constants.VCardType.UNSIGNED
    }
}
