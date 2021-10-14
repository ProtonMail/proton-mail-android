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

import android.database.Cursor
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.text.TextUtils
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.ContactResponse
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.models.factories.makeInt
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmailContactLabelJoin
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_CONTACT_EXIST_THIS_EMAIL
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_VALIDATION_FAILED
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_GROUP_ALREADY_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_INVALID_EMAIL
import ch.protonmail.android.contacts.list.listView.ContactItem
import ch.protonmail.android.contacts.repositories.andorid.details.IAndroidContactDetailsRepository
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.crypto.Crypto
import ch.protonmail.android.events.ContactEvent
import ch.protonmail.android.events.ContactProgressEvent
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.views.models.LocalContact
import ch.protonmail.android.views.models.LocalContactAddress
import com.birbit.android.jobqueue.Params
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.parameter.EmailType
import ezvcard.property.Address
import ezvcard.property.Email
import ezvcard.property.Telephone
import ezvcard.property.Uid
import java.io.Serializable
import java.util.ArrayList
import java.util.UUID

class ConvertLocalContactsJob(localContacts: List<ContactItem>) : ProtonMailEndlessJob(Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_CONTACT)) {

    private val mLocalContacts: List<LocalContactItem>

    init {
        mLocalContacts = localContacts.asSequence().filter { it.contactId != null }.map { LocalContactItem(it.contactId!!, it.getName(), it.getEmail()) }.toList()
    }

    override fun onAdded() {
        if (!getQueueNetworkUtil().isConnected()) {
            AppUtil.postEventOnUi(ContactEvent(ContactEvent.NO_NETWORK, false))
        }
    }

    @Throws(Throwable::class)
    override fun onRun() {

        val contactsDatabase = ContactsDatabaseFactory.getInstance(
                applicationContext).getDatabase()
        val crypto = Crypto.forUser(getUserManager(), getUserManager().username)

        val executionResults = ContactsDatabaseFactory.getInstance(applicationContext).runInTransaction<List<Int>> {

            val contactsGroups = getLocalContactsGroups()
            val contactGroupsOnServer = uploadLocalContactsGroupsAndGetIds(contactsGroups)

            val results = ArrayList<Int>()
            var counter = 1
            for (contactItem in mLocalContacts) {

                val c = ProtonMailApplication.getApplication()
                        .contentResolver
                        .query(ContactsContract.Data.CONTENT_URI,
                                IAndroidContactDetailsRepository.ANDROID_DETAILS_PROJECTION,
                                IAndroidContactDetailsRepository.ANDROID_DETAILS_SELECTION,
                                arrayOf(contactItem.id), null) ?: continue

                val localContact = createLocalContact(c, contactsGroups)
                c.close()

                val contactGroupIds = contactGroupsOnServer.filter { localContact.groups.contains(it.key) }.map { it.value }

                val vCardEncrypted = VCard()
                vCardEncrypted.version = VCardVersion.V4_0

                val vCard = VCard()
                vCard.version = VCardVersion.V4_0
                vCard.uid = Uid("proton-android-" + UUID.randomUUID().toString())
                vCard.setFormattedName(contactItem.name)

                val contactData = ContactData(
                    ContactData.generateRandomContactId(),
                    contactItem.name
                )

                val dbId = contactsDatabase.saveContactData(contactData)
                var emailGroupCounter = 1
                for (email in localContact.emails) {
                    val vCardEmail = Email(email)
                    vCardEmail.types.add(EmailType.HOME)
                    vCardEmail.group = "item" + emailGroupCounter++
                    vCard.addEmail(vCardEmail)
                }
                for (phone in localContact.phones) {
                    val vCardPhone = Telephone(phone)
                    vCardEncrypted.addTelephoneNumber(vCardPhone)
                }
                for (address in localContact.addresses) {
                    val isEmpty = TextUtils.isEmpty(address.street) && TextUtils.isEmpty(
                        address.city
                    ) && TextUtils.isEmpty(
                        address.region
                    ) && TextUtils.isEmpty(address.postcode) && TextUtils.isEmpty(address.country)
                    if (!isEmpty) {
                        val vCardAddress = Address()
                        vCardAddress.streetAddress = address.street
                        vCardAddress.locality = address.city
                        vCardAddress.region = address.region
                        vCardAddress.postalCode = address.postcode
                        vCardAddress.country = address.country
                        vCardEncrypted.addAddress(vCardAddress)
                    }
                }

                val signedDataSignature = crypto.sign(vCard.write())
                val contactEncryptedDataType2 = ContactEncryptedData(vCard.write(), signedDataSignature, Constants.VCardType.SIGNED)

                val vCardEncryptedData = vCardEncrypted.write()
                val encryptedData = crypto.encrypt(vCardEncryptedData, false)
                val encryptDataSignature = crypto.sign(vCardEncryptedData)
                val contactEncryptedDataType3 = ContactEncryptedData(
                    encryptedData.armored, encryptDataSignature, Constants.VCardType.SIGNED_ENCRYPTED
                )

                val contactEncryptedDataList = ArrayList<ContactEncryptedData>()
                contactEncryptedDataList.add(contactEncryptedDataType2)
                contactEncryptedDataList.add(contactEncryptedDataType3)

                val body = CreateContact(contactEncryptedDataList)
                val response = getApi().createContactBlocking(body)

                @ContactEvent.Status val status = handleResponse(contactsDatabase, response!!, dbId, contactGroupIds)
                if (status != ContactEvent.SUCCESS) {
                    results.add(status)
                }
                AppUtil.postEventOnUi(ContactProgressEvent(counter++))
            }
            results
        }

        if (executionResults.isEmpty()) {
            AppUtil.postEventOnUi(ContactEvent(ContactEvent.SUCCESS, false))
        } else {
            AppUtil.postEventOnUi(
                ContactEvent(
                    ContactEvent.NOT_ALL_SYNC, false,
                    executionResults
                )
            )
        }
    }

    private fun createLocalContact(data: Cursor, contactsGroups: Map<Long, String>): LocalContact {
        var name = ""
        val phones = ArrayList<String>()
        val emails = ArrayList<String>()
        val addresses = ArrayList<LocalContactAddress>()
        val groups = ArrayList<String>()
        while (data.moveToNext()) {
            val dataKind = data.getString(data.getColumnIndex(ContactsContract.Data.MIMETYPE))
            when (dataKind) {
                Phone.CONTENT_ITEM_TYPE -> {
                    phones.add(data.getString(data.getColumnIndex(Phone.NUMBER)))
                }
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                    name = data.getString(
                        data.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    )
                    emails.add(
                        data.getString(
                            data.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                        )
                    )
                }
                StructuredPostal.CONTENT_ITEM_TYPE -> {
                    val street = data.getString(data.getColumnIndex(StructuredPostal.STREET))
                    val city = data.getString(data.getColumnIndex(StructuredPostal.CITY))
                    val region = data.getString(data.getColumnIndex(StructuredPostal.REGION))
                    val postcode = data.getString(
                        data.getColumnIndex(StructuredPostal.POSTCODE)
                    )
                    val country = data.getString(data.getColumnIndex(StructuredPostal.COUNTRY))
                    addresses.add(LocalContactAddress(street, city, region, postcode, country))
                }
                GroupMembership.CONTENT_ITEM_TYPE -> {
                    val groupId = data.getLong(data.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID))
                    contactsGroups[groupId]?.let { groups.add(it) }
                }
            }
        }
        return LocalContact(name, emails, phones, addresses, groups)
    }

    /**
     * @return Map <group_database_id, group_name>
     */
    private fun getLocalContactsGroups(): Map<Long, String> {

        val groups = mutableMapOf<Long, String>()

        val groupsCursor = ProtonMailApplication.getApplication().contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE),
            String.format("%s = 0", ContactsContract.Groups.GROUP_VISIBLE),
            null,
            null
        )

        groupsCursor?.use {
            it.moveToFirst()
            do {
                groups[it.getLong(0)] = it.getString(1)
            } while (it.moveToNext())
        }

        return groups
    }

    /**
     * @return Map <group_name, group_api_id>
     */
    private fun uploadLocalContactsGroupsAndGetIds(localGroups: Map<Long, String>): Map<String, String> {

        val result = mutableMapOf<String, String>()
        val defaultColor = "#7272A7"

        var someGroupsAlreadyExist = false

        localGroups.forEach {
            val response = getApi().createLabel(LabelBody(it.value, defaultColor, 1, false.makeInt(), Constants.LABEL_TYPE_CONTACT_GROUPS))
            if (response.code == RESPONSE_CODE_ERROR_GROUP_ALREADY_EXIST) {
                someGroupsAlreadyExist = true
            } else {
                result[it.value] = response.contactGroup.ID
                ContactsDatabaseFactory.getInstance(applicationContext).getDatabase().saveContactGroupLabel(response.contactGroup)
            }
        }

        if (someGroupsAlreadyExist) { // at least one local group already exist on server, we fetch all of them to get IDs
            val serverGroups = getApi().fetchContactGroups()
                .map { it.contactGroups }
                .subscribeOn(ThreadSchedulers.io())
                .observeOn(ThreadSchedulers.io())
                .blockingGet()

            localGroups.filterNot { result.containsKey(it.value) }.forEach { localGroupEntry ->
                serverGroups.find { it.name == localGroupEntry.value }?.run {
                    result[localGroupEntry.value] = this.ID
                }
            }
        }

        return result
    }

    @ContactEvent.Status
    private fun handleResponse(contactsDatabase: ContactsDatabase, response: ContactResponse,
                               contactDataDbId: Long, contactGroupIds: List<String>): Int {
        val remoteContactId = response.contactId
        val previousContactData = contactsDatabase.findContactDataByDbId(contactDataDbId)
        if (remoteContactId != "") {
            val contactEmails = contactsDatabase.findContactEmailsByContactId(
                    previousContactData!!.contactId!!)
            previousContactData.contactId = remoteContactId
            contactsDatabase.saveContactData(previousContactData)
            contactsDatabase.deleteAllContactsEmails(contactEmails)
            val responses = response.responses
            for (contactResponse in responses) {
                val contact = contactResponse.response.contact
                contactsDatabase.saveAllContactsEmailsBlocking(contact.emails!!)
                contactGroupIds.forEach { contactGroupId ->
                    val emailsList = contact.emails!!.map { it.contactEmailId }
                    getApi().labelContacts(LabelContactsBody(contactGroupId, emailsList))
                            .doOnComplete {
                                val joins = contactsDatabase.fetchJoins(contactGroupId) as ArrayList
                                for (contactEmail in emailsList) {
                                    joins.add(ContactEmailContactLabelJoin(contactEmail, contactGroupId))
                                }
                                contactsDatabase.saveContactEmailContactLabelBlocking(joins)
                            }
                            .blockingAwait()
                }
            }
            return ContactEvent.SUCCESS
        } else if (response.responseErrorCode == RESPONSE_CODE_ERROR_EMAIL_EXIST || response.responseErrorCode == RESPONSE_CODE_ERROR_CONTACT_EXIST_THIS_EMAIL) {
            contactsDatabase.deleteContactData(previousContactData!!)
            return ContactEvent.ALREADY_EXIST
        } else if (response.responseErrorCode == RESPONSE_CODE_ERROR_INVALID_EMAIL || response
                        .responseErrorCode == RESPONSE_CODE_ERROR_EMAIL_VALIDATION_FAILED) {
            contactsDatabase.deleteContactData(previousContactData!!)
            return ContactEvent.INVALID_EMAIL
        } else if (response.responseErrorCode == RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED) {
            contactsDatabase.deleteContactData(previousContactData!!)
            return ContactEvent.DUPLICATE_EMAIL
        } else if (response.responseErrorCode != Constants.RESPONSE_CODE_OK) {
            contactsDatabase.deleteContactData(previousContactData!!)
            return ContactEvent.ERROR
        } else {
            return ContactEvent.SAVED
        }
    }

    private inner class LocalContactItem(val id: String, val name: String, val email: String) : Serializable
}
