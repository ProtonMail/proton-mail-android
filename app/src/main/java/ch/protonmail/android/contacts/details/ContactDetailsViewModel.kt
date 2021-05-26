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

package ch.protonmail.android.contacts.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import ch.protonmail.android.usecase.fetch.FetchContactDetails
import ch.protonmail.android.worker.DeleteContactWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import me.proton.core.user.domain.UserManager
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

@HiltViewModel
class ContactDetailsViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val contactDetailsRepository: ContactDetailsRepository,
    private val fetchContactDetails: FetchContactDetails,
    private val userManager: UserManager,
    private val workManager: WorkManager
) : ViewModel() {

    fun getContactDetails(contactId: String) {
        viewModelScope.launch {
            fetchContactDetails(contactId)
        }
    }

    fun deleteContact(contactId: String) = DeleteContactWorker.Enqueuer(workManager).enqueue(listOf(contactId))

    fun observeContactGroups() = contactDetailsRepository.observeContactGroups()
        .flowOn(dispatchers.Io)

    suspend fun observeContactEmails(contactId: String) = contactDetailsRepository.getContactEmails(contactId)

//    private fun decryptAndFillVCard(contact: FullContactDetails?) {
//        var hasDecryptionError = false
//        val crypto: Crypto<*> = forUser(userManager, userManager.requireCurrentUserId())
//        var encData: List<ContactEncryptedData>? = ArrayList()
//        if (contact != null && contact.encryptedData != null) {
//            encData = contact.encryptedData
//        } else {
//            hasDecryptionError = true
//        }
//        for (contactEncryptedData in encData!!) {
//            if (contactEncryptedData.type == 0) {
//                mVCardType0 = contactEncryptedData.data
//            } else if (contactEncryptedData.type == 2) {
//                mVCardType2 = contactEncryptedData.data
//                mVCardType2Signature = contactEncryptedData.signature
//            } else if (contactEncryptedData.type == 3) {
//                try {
//                    val tct = CipherText(contactEncryptedData.data)
//                    val tdr = crypto.decrypt(tct)
//                    mVCardType3 = tdr.decryptedData
//                } catch (e: Exception) {
//                    hasDecryptionError = true
//                    Logger.doLogException(e)
//                }
//                mVCardType3Signature = contactEncryptedData.signature
//            }
//        }
//        fillVCard(hasDecryptionError)
//    }


}
