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
package ch.protonmail.android.api.models.room

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.labels.data.model.LabelType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.Assert
import org.junit.Rule
import kotlin.test.Test

class ContactGroupsDatabaseTest : CoroutinesTest {

    private val context = ApplicationProvider.getApplicationContext<ProtonMailApplication>()
    private val databaseFactory = ContactDatabase.buildInMemoryDatabase(context)
    private val database = databaseFactory.getDao()
    private val testUserId = UserId("TestUserId")

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    //region tests

    @Test
    fun testRelationship() = runBlocking {
        val label1 = LabelEntity(LabelId("la"), testUserId, "aa", "aaa", 0, LabelType.MESSAGE_LABEL, EMPTY_STRING, "parent", 0, 0, 0)
        val label2 = LabelEntity(LabelId("lb"), testUserId, "ab", "aab", 0, LabelType.FOLDER, EMPTY_STRING, "parent", 0, 0, 0)
        val label3 = LabelEntity(LabelId("lc"), testUserId, "ac", "aac", 0, LabelType.MESSAGE_LABEL, EMPTY_STRING, "parent", 0, 0, 0)
        val label4 = LabelEntity(LabelId("ld"), testUserId, "ad", "aad", 0, LabelType.MESSAGE_LABEL, EMPTY_STRING, "parent", 0, 0, 0)

        val email1 = ContactEmail("e1", "1@1.1", "a", labelIds = listOf("la", "lc"))
        val email2 = ContactEmail("e2", "2@2.2", "b", labelIds = listOf("la", "lc"))
        val email3 = ContactEmail("e3", "3@3.3", "c", labelIds = listOf("la", "lc"))
        val email4 = ContactEmail("e4", "4@4.4", "d", labelIds = listOf("lb"))
        val email5 = ContactEmail("e5", "5@5.5", "e", labelIds = listOf("lb"))
        val email6 = ContactEmail("e6", "6@6.6", "f", labelIds = listOf("ld"))

        database.saveAllContactsEmails(email1, email2, email3, email4, email5, email6)

        val contactEmailContactLabel1 = ContactEmailContactLabelJoin("e1", "la")
        val contactEmailContactLabel2 = ContactEmailContactLabelJoin("e1", "lc")
        val contactEmailContactLabel3 = ContactEmailContactLabelJoin("e2", "la")
        val contactEmailContactLabel4 = ContactEmailContactLabelJoin("e2", "lc")
        val contactEmailContactLabel5 = ContactEmailContactLabelJoin("e3", "la")
        val contactEmailContactLabel6 = ContactEmailContactLabelJoin("e3", "lc")
        val contactEmailContactLabel7 = ContactEmailContactLabelJoin("e4", "lb")
        val contactEmailContactLabel8 = ContactEmailContactLabelJoin("e5", "lb")
        val contactEmailContactLabel9 = ContactEmailContactLabelJoin("e6", "ld")

        database.saveContactEmailContactLabel(
            contactEmailContactLabel1,
            contactEmailContactLabel2,
            contactEmailContactLabel3,
            contactEmailContactLabel4,
            contactEmailContactLabel5,
            contactEmailContactLabel6,
            contactEmailContactLabel7,
            contactEmailContactLabel8,
            contactEmailContactLabel9
        )
        val laCount = database.countContactEmailsByLabelIdBlocking("la")
        val lbCount = database.countContactEmailsByLabelIdBlocking("lb")
        val lcCount = database.countContactEmailsByLabelIdBlocking("lc")
        val ldCount = database.countContactEmailsByLabelIdBlocking("ld")
        Assert.assertEquals(3, laCount)
        Assert.assertEquals(2, lbCount)
        Assert.assertEquals(3, lcCount)
        Assert.assertEquals(1, ldCount)
    }

    @Test
    fun testReturnedCorrectContactEmailsForContactGroup() {
        val label1 = LabelEntity(LabelId("la"), testUserId, "aa", "aaa", 0, LabelType.MESSAGE_LABEL, EMPTY_STRING, "parent", 0, 0, 0)
        val label2 = LabelEntity(LabelId("lb"), testUserId, "ab", "aab", 0, LabelType.FOLDER, EMPTY_STRING, "parent", 0, 0, 0)

        val email1 = ContactEmail("e1", "1@1.1", labelIds = listOf("la", "lb"), name = "ce1")
        val email2 = ContactEmail("e2", "2@2.2", labelIds = listOf("la"), name = "ce2")
        val email4 = ContactEmail("e4", "4@4.4", labelIds = listOf("lb"), name = "ce3")
        val email5 = ContactEmail("e5", "5@5.5", labelIds = listOf("lb", "la"), name = "ce4")

        database.saveAllContactsEmails(email1, email2, email4, email5)

        val contactEmailContactLabel1 = ContactEmailContactLabelJoin("e1", "la")
        val contactEmailContactLabel2 = ContactEmailContactLabelJoin("e1", "lb")
        val contactEmailContactLabel3 = ContactEmailContactLabelJoin("e2", "la")
        val contactEmailContactLabel4 = ContactEmailContactLabelJoin("e4", "lb")
        val contactEmailContactLabel7 = ContactEmailContactLabelJoin("e5", "lb")
        val contactEmailContactLabel8 = ContactEmailContactLabelJoin("e5", "la")

        database.saveContactEmailContactLabel(
            contactEmailContactLabel1,
            contactEmailContactLabel2,
            contactEmailContactLabel3,
            contactEmailContactLabel4,
            contactEmailContactLabel7,
            contactEmailContactLabel8
        )
        val laReturnedEmails = database.findAllContactsEmailsByContactGroupAsync("la").testValue
        val lbReturnedEmails = database.findAllContactsEmailsByContactGroupAsync("lb").testValue
        val lcReturnedEmails = database.findAllContactsEmailsByContactGroupAsync("lc").testValue
        Assert.assertNotNull(laReturnedEmails)
        Assert.assertNotNull(lbReturnedEmails)
        Assert.assertEquals(emptyList<ContactEmail>(), lcReturnedEmails)
        Assert.assertEquals(listOf(email1, email2, email5), laReturnedEmails)
        Assert.assertEquals(listOf(email1, email4, email5), lbReturnedEmails)
    }

    @Test
    fun filterCorrectContactEmailsForContactGroup() {
        val label1 = LabelEntity(LabelId("la"), testUserId, "aa", "aaa", 0, LabelType.MESSAGE_LABEL, EMPTY_STRING, "parent", 0, 0, 0)
        val label2 = LabelEntity(LabelId("lb"), testUserId, "ab", "aab", 0, LabelType.FOLDER, EMPTY_STRING, "parent", 0, 0, 0)

        val email1 = ContactEmail("e1", "1@1.1", labelIds = listOf("la", "lb"), name = "ce1")
        val email2 = ContactEmail("e2", "2@2.2", labelIds = listOf("la"), name = "ce2")
        val email4 = ContactEmail("e3", "3@3.3", labelIds = listOf("lb"), name = "ce3")
        val email5 = ContactEmail("e4", "4@3.4", labelIds = listOf("lb", "la"), name = "ce4")

        database.saveAllContactsEmails(email1, email2, email4, email5)

        val contactEmailContactLabel1 = ContactEmailContactLabelJoin("e1", "la")
        val contactEmailContactLabel2 = ContactEmailContactLabelJoin("e1", "lb")
        val contactEmailContactLabel3 = ContactEmailContactLabelJoin("e2", "la")
        val contactEmailContactLabel4 = ContactEmailContactLabelJoin("e3", "lb")
        val contactEmailContactLabel7 = ContactEmailContactLabelJoin("e4", "lb")
        val contactEmailContactLabel8 = ContactEmailContactLabelJoin("e4", "la")

        database.saveContactEmailContactLabel(
            contactEmailContactLabel1,
            contactEmailContactLabel2,
            contactEmailContactLabel3,
            contactEmailContactLabel4,
            contactEmailContactLabel7,
            contactEmailContactLabel8
        )
        runBlockingTest {
            val result = database.filterContactsEmailsByContactGroup("la", "%2%").first()

            Assert.assertNotNull(result)
            Assert.assertEquals(1, result.size)
            Assert.assertEquals(listOf(email2), result)

            val result2 = database.filterContactsEmailsByContactGroup("lb", "%3%").first()
            Assert.assertNotNull(result2)
            Assert.assertEquals(2, result2.size)
            Assert.assertEquals(listOf(email4, email5), result2)
        }
    }

    @Test
    fun testReturnedCorrectContactGroupsForContactEmail() = runBlockingTest {
        val label1 = LabelEntity(LabelId("la"), testUserId, "aa", "aaa", 0, LabelType.MESSAGE_LABEL, EMPTY_STRING, "parent", 0, 0, 0)
        val label2 = LabelEntity(LabelId("lb"), testUserId, "ab", "aab", 0, LabelType.FOLDER, EMPTY_STRING, "parent", 0, 0, 0)

        val email1 = ContactEmail("e1", "1@1.1", labelIds = listOf("la", "lb"), name = "ce1")
        val email2 = ContactEmail("e2", "2@2.2", labelIds = listOf("la"), name = "ce2")
        val email4 = ContactEmail("e4", "4@4.4", labelIds = listOf("lb"), name = "ce3")
        val email5 = ContactEmail("e5", "5@5.5", labelIds = listOf("lb", "la"), name = "ce4")

        database.saveAllContactsEmails(email1, email2, email4, email5)

        val contactEmailContactLabel1 = ContactEmailContactLabelJoin("e1", "la")
        val contactEmailContactLabel2 = ContactEmailContactLabelJoin("e1", "lb")
        val contactEmailContactLabel3 = ContactEmailContactLabelJoin("e2", "la")
        val contactEmailContactLabel4 = ContactEmailContactLabelJoin("e4", "lb")
        val contactEmailContactLabel7 = ContactEmailContactLabelJoin("e5", "lb")
        val contactEmailContactLabel8 = ContactEmailContactLabelJoin("e5", "la")

        database.saveContactEmailContactLabel(
            contactEmailContactLabel1,
            contactEmailContactLabel2,
            contactEmailContactLabel3,
            contactEmailContactLabel4,
            contactEmailContactLabel7,
            contactEmailContactLabel8
        )
        val laReturnedEmails = database.getAllContactGroupsByContactEmail("e1")

        Assert.assertNotNull(laReturnedEmails)
        Assert.assertEquals(listOf(label1, label2), laReturnedEmails)
    }
    //endregion
}
