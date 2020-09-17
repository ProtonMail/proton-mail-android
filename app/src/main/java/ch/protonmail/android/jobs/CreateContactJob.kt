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
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_CONTACT_EXIST_THIS_EMAIL
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_INVALID_EMAIL
import ch.protonmail.android.core.Constants
import ch.protonmail.android.events.ContactEvent
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.crypto.Crypto
import com.birbit.android.jobqueue.Params
import java.util.ArrayList

class CreateContactJob()
    : ProtonMailEndlessJob(Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_CONTACT)) {
    private lateinit var mContactData: ContactData
    private lateinit var mContactEmails: List<ContactEmail>
    private lateinit var mEncryptedData: String
    private lateinit var mSignedData: String

    constructor(contactData: ContactData,
                contactEmails: List<ContactEmail>,
                encryptedData: String,
                signedData: String): this() {
        mContactData = contactData
        mContactEmails = contactEmails
        mEncryptedData = encryptedData
        mSignedData = signedData
    }

    override fun onAdded() {
        for (email in mContactEmails!!) {
            email.contactId = mContactData.contactId
        }
        val contactsDatabase = ContactsDatabaseFactory.getInstance(applicationContext).getDatabase()
        contactsDatabase.saveAllContactsEmails(mContactEmails!!)

        if (!getQueueNetworkUtil().isConnected()) {
            AppUtil.postEventOnUi(ContactEvent(ContactEvent.NO_NETWORK, true))
        }
    }

    @Throws(Throwable::class)
    override fun onRun() {
        val contactsDatabase = ContactsDatabaseFactory.getInstance(applicationContext)
                .getDatabase()

        val crypto = Crypto.forUser(getUserManager(), getUserManager().username)
        val encryptedData = crypto.encrypt(mEncryptedData, false).armored
        val encryptDataSignature = crypto.sign(mEncryptedData)
        val signedDataSignature = crypto.sign(mSignedData)
        // TODO: 9/17/17 add type 0, type 2 and type 3
        val contactEncryptedDataType2 = ContactEncryptedData(mSignedData, signedDataSignature, Constants.VCardType.SIGNED)
        val contactEncryptedDataType3 = ContactEncryptedData(encryptedData, encryptDataSignature, Constants.VCardType.SIGNED_ENCRYPTED)
        val contactEncryptedDataList = ArrayList<ContactEncryptedData>()
        contactEncryptedDataList.add(contactEncryptedDataType2)
        contactEncryptedDataList.add(contactEncryptedDataType3)
        val body = CreateContact(contactEncryptedDataList)
        val response = getApi().createContact(body)

        if (response!!.code != Constants.RESPONSE_CODE_MULTIPLE_OK) {
            AppUtil.postEventOnUi(ContactEvent(ContactEvent.ERROR, true))
            return
        }

        // update local contact with contactId received from server
        val contactId = response.contactId
        val result: Int
        if (!TextUtils.isEmpty(contactId)) {
            val contactData = contactsDatabase.findContactDataByDbId(mContactData.dbId!!)
            mContactEmails = contactsDatabase.findContactEmailsByContactId(contactData!!.contactId!!)
            contactData.contactId = contactId
            contactsDatabase.saveContactData(contactData)
            contactsDatabase.deleteAllContactsEmails(mContactEmails!!)
            val responses = response.responses
            for (contactResponse in responses) {
                val contact = contactResponse.response.contact
                contactsDatabase.saveAllContactsEmails(contact.emails!!)
            }
            result = ContactEvent.SUCCESS
        } else if (response.responseErrorCode == RESPONSE_CODE_ERROR_EMAIL_EXIST || response
                        .responseErrorCode == RESPONSE_CODE_ERROR_CONTACT_EXIST_THIS_EMAIL) {
            contactsDatabase.deleteContactData(mContactData)
            result = ContactEvent.ALREADY_EXIST
        } else if (response.responseErrorCode == RESPONSE_CODE_ERROR_INVALID_EMAIL) {
            contactsDatabase.deleteContactData(mContactData)
            result = ContactEvent.INVALID_EMAIL
        } else if (response.responseErrorCode == RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED) {
            contactsDatabase.deleteContactData(mContactData)
            result = ContactEvent.DUPLICATE_EMAIL
        }else {
            result = ContactEvent.SAVED
        }
        AppUtil.postEventOnUi(ContactEvent(result, true))
    }

    data class ContactDataDbId(val dbId: Long): java.io.Serializable
}
