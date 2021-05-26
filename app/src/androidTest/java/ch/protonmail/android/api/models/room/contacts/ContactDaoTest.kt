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
package ch.protonmail.android.api.models.room.contacts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.InstrumentationRegistry
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.room.testValue
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.FullContactDetails
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.junit.Assert
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import kotlin.test.BeforeTest

internal class ContactDaoTest {

    private val context = InstrumentationRegistry.getTargetContext()
    private var databaseFactory = ContactDatabase.buildInMemoryDatabase(context)
    private var database = databaseFactory.getDao()
    private val initiallyEmptyDatabase = databaseFactory.getDao()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val contactData = listOf(
        ContactData(contactId = "aa", name = "aaa").apply { dbId = 2 },
        ContactData(contactId = "bb", name = "bbb").apply { dbId = 4 },
        ContactData(contactId = "cc", name = "ccc").apply { dbId = 3 },
        ContactData(contactId = "dd", name = "ddd").apply { dbId = 1 },
        ContactData(contactId = "ee", name = "eee").apply { dbId = 7 }
    )


    private val contactEmails = listOf(
        ContactEmail(
            contactEmailId = "a",
            email = "a@a.com",
            contactId = "aa",
            labelIds = listOf("aaa", "aaaa", "aaaaa"),
            name = "ce1"
        ),
        ContactEmail(
            contactEmailId = "b",
            email = "b@b.com",
            contactId = "bb",
            labelIds = listOf("bbb", "bbbb", "bbbbb"),
            name = "ce2"
        ),
        ContactEmail(
            contactEmailId = "c",
            email = "c@c.com",
            contactId = "bb",
            labelIds = listOf("ccc", "cccc", "ccccc"),
            name = "ce3"
        ),
        ContactEmail(
            contactEmailId = "d",
            email = "b@b.com",
            contactId = "dd",
            labelIds = listOf("ddd", "dddd", "ddddd"),
            name = "ce4"
        ),
        ContactEmail(
            contactEmailId = "e",
            email = "e@e.com",
            contactId = "ee",
            labelIds = listOf("eee", "eeee", "eeeee"),
            name = "ce5"
        )
    )

    private val fullContactDetails = listOf(
        FullContactDetails(
            contactId = "a",
            name = "aa",
            uid = "aaa",
            createTime = 1,
            modifyTime = 1,
            size = 5,
            defaults = 3,
            encryptedData = mutableListOf(
                ContactEncryptedData("aaaa", "aaaaa", Constants.VCardType.SIGNED),
                ContactEncryptedData("aaaaaa", "aaaaaaa", Constants.VCardType.SIGNED_ENCRYPTED)
            )
        ),
        FullContactDetails(
            contactId = "b",
            name = "bb",
            uid = "bbb",
            createTime = 5,
            modifyTime = 7,
            size = 12,
            defaults = 2,
            encryptedData = mutableListOf(
                ContactEncryptedData("bbbb", "bbbbb", Constants.VCardType.SIGNED),
                ContactEncryptedData("bbbbbb", "bbbbbbb", Constants.VCardType.SIGNED_ENCRYPTED)
            )
        ),
        FullContactDetails(
            contactId = "c",
            name = "cc",
            uid = "ccc",
            createTime = 12,
            modifyTime = 1100,
            size = 2,
            defaults = 123,
            encryptedData = mutableListOf(
                ContactEncryptedData("cccc", "ccccc", Constants.VCardType.SIGNED),
                ContactEncryptedData("cccccc", "ccccccc", Constants.VCardType.SIGNED_ENCRYPTED)
            )
        ),
        FullContactDetails(
            contactId = "d",
            name = "dd",
            uid = "ddd",
            createTime = 3,
            modifyTime = 12,
            size = 112,
            defaults = 31,
            encryptedData = mutableListOf(
                ContactEncryptedData("dddd", "ddddd", Constants.VCardType.SIGNED),
                ContactEncryptedData("dddddd", "ddddddd", Constants.VCardType.SIGNED_ENCRYPTED)
            )
        ),
        FullContactDetails(
            contactId = "e",
            name = "ee",
            uid = "eee",
            createTime = 1,
            modifyTime = 131,
            size = 12,
            defaults = 321,
            encryptedData = mutableListOf(
                ContactEncryptedData("eeee", "eeeee", Constants.VCardType.SIGNED),
                ContactEncryptedData("eeeeee", "eeeeeee", Constants.VCardType.SIGNED_ENCRYPTED)
            )
        ),
        FullContactDetails("f")
    )

    private fun ContactDao.populate() {
        runBlocking {
            saveAllContactsData(contactData)
            saveAllContactsEmails(contactEmails)
        }
        fullContactDetails.forEach(this::insertFullContactDetails)
    }

    private fun assertDatabaseState(
        expectedContactData: Iterable<ContactData> = contactData,
        expectedContactEmails: Iterable<ContactEmail> = contactEmails,
        expectedFullContactDetails: Iterable<FullContactDetails> = fullContactDetails
    ) {
        val expectedContactDataSet = expectedContactData.toSet()
        val expectedContactEmailsSet = expectedContactEmails.toSet()
        //hack as encrypted data has equals not defined
        val expectedFullContactDetailsSet = expectedFullContactDetails
            .map { it.apply { encryptedData = mutableListOf() } }.toSet()

        val actualContactDataSet = database.findAllContactDataAsync().testValue!!.toSet()
        val actualContactEmailsSet = database.findAllContactsEmailsAsync().testValue!!.toSet()
        //hack as encrypted data has equals not defined
        val actualFullContactDetailsSet = expectedFullContactDetails
            .map(FullContactDetails::contactId)
            .map(database::findFullContactDetailsByIdBlocking)
            .map { it?.apply { encryptedData = mutableListOf() } }
            .toSet()

        Assert.assertEquals(expectedContactDataSet, actualContactDataSet)
        Assert.assertEquals(expectedContactEmailsSet, actualContactEmailsSet)
        Assert.assertEquals(expectedFullContactDetailsSet, actualFullContactDetailsSet)
    }

    @BeforeTest
    fun setUp() {
        database.populate()
    }

    @Test
    fun findContactDataById() {
        val expected = contactData[3]
        val actual = database.findContactDataById(expected.contactId!!)
        Assert.assertEquals(expected, actual)
        assertDatabaseState()
    }

    @Test
    fun findContactDataByDbId() {
        val expected = contactData[3]
        val actual = database.findContactDataByDbId(expected.dbId!!)
        Assert.assertEquals(expected, actual)
        assertDatabaseState()
    }

    @Test
    fun findAllContactDataAsync() {
        val expected = contactData
        val actual = database.findAllContactDataAsync().testValue
        Assert.assertEquals(expected, actual)
        assertDatabaseState()
    }

    @Test
    fun clearContactDataCache() {
        database.clearContactDataCache()
        assertDatabaseState(expectedContactData = emptyList())
    }

    @Test
    fun saveContactData() {
        val inserted = ContactData("z", "zz")
        val expected = contactData + inserted
        database.saveContactData(inserted)
        assertDatabaseState(expectedContactData = expected)
    }

    @Test
    fun saveAllContactsData() {
        runBlocking {
            val inserted = listOf(
                ContactData("y", "yy"), ContactData("z", "zz")
            )
            val expected = contactData + inserted
            database.saveAllContactsData(inserted)
            assertDatabaseState(expectedContactData = expected)
        }
    }

    @Test
    fun saveAllContactsData1() {
        val inserted = listOf(
            ContactData("y", "yy"), ContactData("z", "zz")
        )
        val expected = contactData + inserted
        database.saveAllContactsData(*inserted.toTypedArray())
        assertDatabaseState(expectedContactData = expected)
    }

    @Test
    fun deleteContactData() {
        val deleted = contactData[3]
        val expected = contactData - deleted
        database.deleteContactData(deleted)
        assertDatabaseState(expectedContactData = expected)
    }

    @Test
    fun deleteContactsData() {
        val deleted = listOf(contactData[3], contactData[1])
        val expected = contactData - deleted
        database.deleteContactsData(deleted)
        assertDatabaseState(expectedContactData = expected)
    }

    @Test
    fun findContactEmailById() {
        val expected = contactEmails[2]
        val actual = database.findContactEmailById(expected.contactEmailId!!)
        Assert.assertEquals(expected, actual)
        assertDatabaseState()
    }

    @Test
    fun findContactEmailByEmail() {
        val expected = contactEmails[2]
        val actual = database.findContactEmailByEmail(expected.email)
        Assert.assertEquals(expected, actual)
        assertDatabaseState()
    }

    @Test
    fun findContactEmailsByContactId() {
        val contactId = contactEmails[2].contactId!!
        val expected = contactEmails.filter { it.contactId == contactId }
        val actual = database.findContactEmailsByContactIdBlocking(contactId)
        Assert.assertEquals(expected, actual)
        assertDatabaseState()
    }

    @Test
    fun findAllContactsEmailsAsync() {
        val expected = contactEmails.toSet()
        val actual = database.findAllContactsEmailsAsync().testValue?.toSet()
        Assert.assertEquals(expected, actual)
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun findAllContactsEmailsByContactGroupAsync() {
        TODO()
        assertDatabaseState()
    }

    @Test
    fun clearByEmail() {
        val deletedEmail = contactEmails[1].email
        val expected = contactEmails.filterNot { it.email == deletedEmail }
        database.clearByEmailBlocking(deletedEmail)
        assertDatabaseState(expectedContactEmails = expected)
    }

    @Test
    fun clearContactEmailsCache() {
        val expected = emptyList<ContactEmail>()
        database.clearContactEmailsCache()
        assertDatabaseState(expectedContactEmails = expected)
    }

    @Test
    fun deleteContactEmail() {
        val deleted = listOf(contactEmails[3], contactEmails[1])
        val expected = contactEmails - deleted
        database.deleteContactEmail(*deleted.toTypedArray())
        assertDatabaseState(expectedContactEmails = expected)
    }

    @Test
    fun deleteAllContactsEmails() {
        val deleted = listOf(contactEmails[3], contactEmails[1])
        val expected = contactEmails - deleted
        database.deleteAllContactsEmails(deleted)
        assertDatabaseState(expectedContactEmails = expected)
    }

    @Test
    fun saveContactEmail() {
        val inserted = ContactEmail(
            "z",
            "z@z.com",
            contactId = "zzz",
            labelIds = listOf("zzzz", "zzzzz", "zzzzzz"),
            name = "ce1"
        )
        val expected = contactEmails + inserted
        database.saveContactEmail(inserted)
        assertDatabaseState(expectedContactEmails = expected)
    }

    @Test
    fun saveAllContactsEmails() {
        val inserted = listOf(
            ContactEmail(
                "y",
                "y@y.com",
                contactId = "yyy",
                labelIds = listOf("yyyy", "yyyyy", "yyyyyy"),
                name = "ce1"
            ),
            ContactEmail(
                "z",
                "z@z.com",
                contactId = "zzz",
                labelIds = listOf("zzzz", "zzzzz", "zzzzzz"),
                name = "ce2"
            )
        )
        val expected = contactEmails + inserted
        database.saveAllContactsEmailsBlocking(inserted)
        assertDatabaseState(expectedContactEmails = expected)
    }

    @Test
    fun saveAllContactsEmails1() {
        val inserted = listOf(
            ContactEmail(
                "y",
                "y@y.com",
                contactId = "yyy",
                labelIds = listOf("yyyy", "yyyyy", "yyyyyy"),
                name = "ce1"
            ),
            ContactEmail(
                "z",
                "z@z.com",
                contactId = "zzz",
                labelIds = listOf("zzzz", "zzzzz", "zzzzzz"),
                name = "ce2"
            )
        )
        val expected = contactEmails + inserted
        database.saveAllContactsEmails(*inserted.toTypedArray())
        assertDatabaseState(expectedContactEmails = expected)
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun countContactEmails() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun findContactGroupById() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun findContactGroupByIdAsync() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun findContactGroupsLiveData() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun findContactGroupsObservable() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun clearContactGroupsLabelsTable() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun saveContactGroupLabel() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun updateFullContactGroup() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun saveAllContactGroups() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun clearContactGroupsList() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun saveContactGroupsList() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun deleteByContactGroupLabelId() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun deleteContactGroup() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun getAllContactGroupsByIds() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun updatePartially() {
        TODO()
        assertDatabaseState()
    }

    @Test
    fun insertFullContactDetails() {
        val inserted = FullContactDetails(
            contactId = "z",
            name = "zz",
            uid = "zzz",
            createTime = 1,
            modifyTime = 131,
            size = 12,
            defaults = 321,
            encryptedData = mutableListOf(
                ContactEncryptedData("zzzz", "zzzzz", Constants.VCardType.SIGNED),
                ContactEncryptedData("zzzzzz", "zzzzzzz", Constants.VCardType.SIGNED_ENCRYPTED)
            )
        )
        val expected = fullContactDetails + inserted
        database.insertFullContactDetails(inserted)
        assertDatabaseState(expectedFullContactDetails = expected)
    }

    @Test
    fun findFullContactDetailsById() {
        val expected = fullContactDetails[1]
        val actual = database.findFullContactDetailsByIdBlocking(expected.contactId)
        Assert.assertThat(
            actual, `is`(FullContactsDetailsMatcher(expected))
        )
        assertDatabaseState()
    }

    @Test
    fun clearFullContactDetailsCache() {
        val expected = emptyList<FullContactDetails>()
        database.clearFullContactDetailsCache()
        assertDatabaseState(expectedFullContactDetails = expected)
    }

    @Test
    fun deleteFullContactsDetails() {
        val deleted = fullContactDetails[1]
        val expected = fullContactDetails - deleted
        database.deleteFullContactsDetails(deleted)
        val found = database.findFullContactDetailsByIdBlocking(deleted.contactId)
        Assert.assertNull(found)
        assertDatabaseState(expectedFullContactDetails = expected)
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun countContactEmailsByLabelId() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun saveContactEmailContactLabel() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun saveContactEmailContactLabel1() {
        TODO()
        assertDatabaseState()
    }

    @Ignore("Implement with contacts groups")
    @Test
    fun saveContactEmailContactLabel2() {
        TODO()
        assertDatabaseState()
    }


    @Test
    fun testContactEmailsConverter() {
        val email1 = ContactEmail(
            "e1",
            "1@1.1",
            "a",
            labelIds = listOf("la", "lc")
        )
        val email2 = ContactEmail(
            "e2",
            "2@2.2",
            "b",
            labelIds = listOf("la", "lc")
        )
        val email3 = ContactEmail(
            "e3",
            "3@3.3",
            "c",
            labelIds = listOf("la", "lc")
        )
        initiallyEmptyDatabase.saveAllContactsEmails(email1, email2, email3)
        val emailFromDb = initiallyEmptyDatabase.findContactEmailById("e1")
        Assert.assertNotNull(emailFromDb)
        val listOfGroups = emailFromDb?.labelIds
        Assert.assertNotNull(listOfGroups)
        val expectedGroupId = "la"
        Assert.assertEquals(expectedGroupId, listOfGroups?.get(0))
    }
}
