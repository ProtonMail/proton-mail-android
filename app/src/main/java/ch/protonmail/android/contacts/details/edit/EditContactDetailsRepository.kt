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
package ch.protonmail.android.contacts.details.edit

import android.database.sqlite.SQLiteBlobTooBigException
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.contacts.data.remote.worker.UpdateContactWorker
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.core.Constants
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelId
import com.birbit.android.jobqueue.JobManager
import ezvcard.Ezvcard
import ezvcard.VCard
import kotlinx.coroutines.flow.first
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import java.security.GeneralSecurityException
import javax.inject.Inject

class EditContactDetailsRepository @Inject constructor(
    jobManager: JobManager,
    api: ProtonMailApiManager,
    dispatcherProvider: DispatcherProvider,
    contactDao: ContactDao,
    labelRepository: LabelRepository,
    contactRepository: ContactsRepository,
    private val userCrypto: UserCrypto,
    private val updateContactWorker: UpdateContactWorker.Enqueuer
) : ContactDetailsRepository(
    jobManager, api, contactDao, dispatcherProvider, labelRepository, contactRepository
) {

    suspend fun clearEmail(email: String) {
        contactDao.clearByEmail(email)
    }

    suspend fun updateContact(
        contactId: String,
        contactName: String,
        emails: List<ContactEmail>,
        vCardEncrypted: VCard,
        vCardSigned: VCard,
        mapContactLabelIds: Map<String, List<LabelId>>,
    ) {
        updateContactDb(
            contactId,
            contactName,
            emails,
            vCardEncrypted.write(),
            vCardSigned.write(),
            mapContactLabelIds
        )
        updateContactWorker.enqueue(
            contactName,
            contactId,
            vCardSigned.write()
        )
    }

    private suspend fun updateContactDb(
        contactId: String,
        contactName: String,
        contactEmails: List<ContactEmail>,
        encryptedData: String,
        signedData: String,
        mapContactLabelIds: Map<String, List<LabelId>>
    ) {
        try {
            val tct = userCrypto.encrypt(encryptedData, false)
            val encryptedDataSignature = userCrypto.sign(encryptedData)
            val signedDataSignature = userCrypto.sign(signedData)
            updateContact(
                contactId,
                contactName,
                contactEmails,
                tct.armored,
                encryptedDataSignature,
                signedDataSignature,
                signedData,
                mapContactLabelIds
            )
        } catch (exception: GeneralSecurityException) {
            Timber.w(exception, "UpdateContact error")
        }
    }

    suspend fun updateContact(
        contactId: String,
        contactName: String,
        contactEmails: List<ContactEmail>,
        encryptedData: String,
        encryptedDataSignature: String,
        signedDataSignature: String,
        signedData: String,
        mapContactLabelIds: Map<String, List<LabelId>>
    ) {
        val contactData = contactDao.findContactDataById(contactId)
        if (contactData != null) {
            contactData.name = contactName
            contactDao.saveContactData(contactData)
        }
        val emails = contactDao.findContactEmailsByContactId(contactId)
        if (emails.isNotEmpty()) {
            contactDao.deleteAllContactsEmails(emails)
        }
        for (email in contactEmails) {
            val emailToClear = email.email
            contactDao.clearByEmail(emailToClear)
        }

        val updatedEmails = contactEmails.map { email ->
            val labels = mapContactLabelIds[contactId]
            if (!labels.isNullOrEmpty()) {
                email.copy(labelIds = labels.map { it.id }, contactId = contactId)
            } else {
                email.copy(contactId = contactId)
            }
        }

        Timber.v("Saving updated emails: $updatedEmails")
        contactDao.saveAllContactsEmails(updatedEmails)

        var contact = try {
            contactDao.observeFullContactDetailsById(contactId).first()
        } catch (tooBigException: SQLiteBlobTooBigException) {
            Timber.i(tooBigException, "Data too big to be fetched")
            null
        }
        if (contact != null) {
            val contactEncryptedData =
                ContactEncryptedData(encryptedData, encryptedDataSignature, Constants.VCardType.SIGNED_ENCRYPTED)
            val contactSignedData = ContactEncryptedData(signedData, signedDataSignature, Constants.VCardType.SIGNED)
            var contactEncryptedDataType0: ContactEncryptedData? = null
            val contactEncryptedDataList: List<ContactEncryptedData>? = contact.encryptedData
            if (!contactEncryptedDataList.isNullOrEmpty()) {
                for (data in contactEncryptedDataList) {
                    if (data.type == 0) {
                        contactEncryptedDataType0 = data
                        break
                    }
                }
            }
            // Set the encrypted data to be null before adding the updated encrypted data
            contact = contact.copy(encryptedData = null)
            if (contactEncryptedDataType0 != null) {
                val vCardType0String = contactEncryptedDataType0.data
                val vCardType0 = if (vCardType0String != null) Ezvcard.parse(vCardType0String).first() else null
                val emailsType0 = vCardType0!!.emails
                vCardType0.emails.removeAll(emailsType0)
                contact.addEncryptedData(ContactEncryptedData(vCardType0.write(), "", Constants.VCardType.UNSIGNED))
            }
            contact.addEncryptedData(contactSignedData)
            contact.addEncryptedData(contactEncryptedData)
            contact.name = contactName
            contact.emails = contactEmails
            contactDao.insertFullContactDetails(contact)
        }
    }

    suspend fun getFullContactDetails(contactId: String): FullContactDetails? = try {
        contactDao.observeFullContactDetailsById(contactId).first()
    } catch (tooBigException: SQLiteBlobTooBigException) {
        Timber.i(tooBigException, "Data too big to be fetched")
        null
    }
}
