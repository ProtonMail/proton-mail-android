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

import android.database.sqlite.SQLiteBlobTooBigException
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.CreateContactV2BodyItem
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_INVALID_EMAIL
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.contacts.groups.jobs.SetMembersForContactGroupJob
import ch.protonmail.android.core.Constants
import ch.protonmail.android.crypto.Crypto.Companion.forUser
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.events.ContactEvent
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Params
import ezvcard.Ezvcard
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.ArrayList
import java.util.HashMap

class UpdateContactJob(
    private val contactId: String,
    private val contactName: String,
    private val contactEmails: List<ContactEmail>,
    private val encryptedData: String,
    private val signedData: String,
    private val mapEmailGroupsIds: Map<ContactEmail, List<ContactLabelUiModel>>,
    private val labelRepository: LabelRepository
) : ProtonMailEndlessJob(Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_CONTACT)) {

    @Transient
    private var contactDao: ContactDao? = null
    override fun onAdded() {
        val crypto = forUser(getUserManager(), userId!!)
        try {
            val tct = crypto.encrypt(encryptedData, false)
            val encryptedDataSignature = crypto.sign(encryptedData)
            val signedDataSignature = crypto.sign(signedData)
            updateContact(contactName, contactEmails, tct.armored, encryptedDataSignature, signedDataSignature, false)
            AppUtil.postEventOnUi(ContactEvent(ContactEvent.SAVED, true))
        } catch (e: Exception) {
            Timber.w(e, "UpdateContact error")
        }
        if (!getQueueNetworkUtil().isConnected()) {
            AppUtil.postEventOnUi(ContactEvent(ContactEvent.NO_NETWORK, true))
        }
    }

    @Throws(Throwable::class)
    override fun onRun() {
        val crypto = forUser(getUserManager(), userId!!)
        requireContactDao()
        val tct = crypto.encrypt(encryptedData, false)
        val encryptedDataSignature = crypto.sign(encryptedData)
        val signedDataSignature = crypto.sign(signedData)
        val body = CreateContactV2BodyItem(
            signedData, signedDataSignature,
            tct.armored, encryptedDataSignature
        )
        val response = getApi().updateContact(contactId, body)
        if (response != null) {
            Timber.v("Update contacts response code:%s error:%s", response.code, response.error)
            if (response.code == RESPONSE_CODE_ERROR_EMAIL_EXIST) {
                // TODO: 9/14/17 todoContacts throw error
                AppUtil.postEventOnUi(ContactEvent(ContactEvent.ALREADY_EXIST, true))
            } else if (response.code == RESPONSE_CODE_ERROR_INVALID_EMAIL) {
                // TODO: 9/14/17 todoContacts throw error
                AppUtil.postEventOnUi(ContactEvent(ContactEvent.INVALID_EMAIL, true))
            } else if (response.code == RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED) {
                AppUtil.postEventOnUi(ContactEvent(ContactEvent.DUPLICATE_EMAIL, true))
            } else {
                updateContact(
                    contactName, response.contact.emails!!, tct.armored, encryptedDataSignature, signedDataSignature,
                    true
                )
                AppUtil.postEventOnUi(ContactEvent(ContactEvent.SUCCESS, true))
            }
        }
    }

    private fun updateContact(
        contactName: String,
        contactEmails: List<ContactEmail>,
        encryptedData: String,
        encryptedDataSignature: String,
        signedDataSignature: String,
        updateJoins: Boolean
    ) {
        requireContactDao()
        val contactData = contactDao!!.findContactDataById(contactId)
        if (contactData != null) {
            contactData.name = contactName
            contactDao!!.saveContactData(contactData)
        }
        val emails = contactDao!!.findContactEmailsByContactIdBlocking(contactId)
        contactDao!!.deleteAllContactsEmails(emails)
        for (email in contactEmails) {
            val emailToClear = email.email
            contactDao!!.clearByEmailBlocking(emailToClear)
        }
        contactDao!!.saveAllContactsEmailsBlocking(contactEmails)
        val mapContactGroupContactEmails: MutableMap<ContactLabelUiModel, MutableList<String>> = HashMap()
        if (updateJoins) {
            for (email in contactEmails) {
                val labels = findContactLabelsByEmail(email)
                for (label in labels) {
                    var labelEmails = mapContactGroupContactEmails[label]
                    if (labelEmails == null) {
                        labelEmails = ArrayList()
                    }
                    labelEmails.add(email.contactEmailId)
                    mapContactGroupContactEmails[label] = labelEmails
                }
            }
        }
        var contact: FullContactDetails? = null
        try {
            contact = contactDao!!.findFullContactDetailsByIdBlocking(contactId)
        } catch (tooBigException: SQLiteBlobTooBigException) {
            Timber.i(tooBigException, "Data too big to be fetched")
        }
        if (contact != null) {
            val contactEncryptedData =
                ContactEncryptedData(encryptedData, encryptedDataSignature, Constants.VCardType.SIGNED_ENCRYPTED)
            val contactSignedData = ContactEncryptedData(signedData, signedDataSignature, Constants.VCardType.SIGNED)
            var contactEncryptedDataType0: ContactEncryptedData? = null
            val contactEncryptedDataList: List<ContactEncryptedData>? = contact.encryptedData
            for (data in contactEncryptedDataList!!) {
                if (data.type == 0) {
                    contactEncryptedDataType0 = data
                    break
                }
            }
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
            contactDao!!.insertFullContactDetailsBlocking(contact)
            if (updateJoins) {
                for ((key, value) in mapContactGroupContactEmails) {
                    updateJoins(key.id.id, key.name, value)
                }
            } else {
                AppUtil.postEventOnUi(ContactEvent(ContactEvent.SAVED, true))
            }
        }
    }

    private fun updateJoins(
        contactGroupId: String,
        contactGroupName: String,
        membersList: List<String>
    ) {
        val labelContactsBody = LabelContactsBody(contactGroupId, membersList)
        runBlocking {
            runCatching {
                getApi().labelContacts(labelContactsBody)
            }.onFailure {
                getJobManager().addJobInBackground(
                    SetMembersForContactGroupJob(contactGroupId, contactGroupName, membersList, labelRepository)
                )
                AppUtil.postEventOnUi(ContactEvent(ContactEvent.ERROR, false))
            }
        }
    }

    private fun findContactLabelsByEmail(contactEmail: ContactEmail): List<ContactLabelUiModel> {
        for ((key, value) in mapEmailGroupsIds) {
            if (key.email == contactEmail.email) {
                return value
            }
        }
        return emptyList()
    }

    private fun requireContactDao() {
        if (contactDao == null) {
            contactDao = ContactDatabase
                .getInstance(applicationContext, userId!!)
                .getDao()
        }
    }
}
