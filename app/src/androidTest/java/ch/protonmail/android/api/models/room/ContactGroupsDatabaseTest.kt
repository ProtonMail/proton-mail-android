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
import androidx.room.Room
import androidx.test.InstrumentationRegistry
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactEmailContactLabelJoin
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Rule
import java.util.Arrays
import kotlin.test.Test

/**
 * Created by Dino Kadrikj on 13.08.2018.
 */
class ContactGroupsDatabaseTest {
    private val context = InstrumentationRegistry.getTargetContext()
    private val databaseFactory = Room.inMemoryDatabaseBuilder(context, ContactsDatabaseFactory::class.java).build()
    private val database = databaseFactory.getDatabase()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    //region tests
    @Test
    fun testFindLabelById() {
        val label1 = ContactLabel("a", "aa")
        val label2 = ContactLabel("b", "bb")
        val label3 = ContactLabel("c", "cc")
        database.saveContactGroupLabel(label1)
        database.saveContactGroupLabel(label2)
        database.saveContactGroupLabel(label3)

        val needed = database.findContactGroupByIdBlocking("b")
        Assert.assertEquals(label2, needed)
    }

    @Test
    fun testUpdateFullLabel() {
        val label1 = ContactLabel("a", "aa", "aaa")
        val label2 = ContactLabel("a", "ab")
        database.saveContactGroupLabel(label1)
        database.updateFullContactGroup(label2)

        val needed = database.findContactGroupByIdBlocking("a")
        Assert.assertEquals(needed?.name, "ab")
    }

    @Test
    fun testUpdateFullLabelNotUpdatingOtherRows() {
        val label1 = ContactLabel("a", "aa", "aaa")
        val label2 = ContactLabel("b", "bb", "bbb")
        val label3 = ContactLabel("a", "ab")
        database.saveAllContactGroups(label1, label2)
        database.updateFullContactGroup(label3)

        val neededUpdated = database.findContactGroupByIdBlocking("a")
        val neededNotUpdated = database.findContactGroupByIdBlocking("b")
        Assert.assertEquals(neededUpdated?.name, "ab")
        Assert.assertEquals(neededNotUpdated?.name, "bb")
    }

    @Test
    fun testUpdateLabelPartially() {
        val label1 = ContactLabel("a", "aa", "aaa")
        val label2 = ContactLabel("a", "ab", "aaa", 0, 1)
        database.saveContactGroupLabel(label1)
        database.updatePartially(label2)

        val needed = database.findContactGroupByIdBlocking("a")
        Assert.assertEquals(needed?.color, "aaa")
        Assert.assertEquals(needed?.name, "ab")
        Assert.assertEquals(needed?.order, 1)
    }

    @Test
    fun testInsertAllLabels() {
        val label1 = ContactLabel("a", "aa", "aaa")
        val label2 = ContactLabel("b", "ab", "aaa", 0, 1)
        val label3 = ContactLabel("c", "ac", "aaa")
        val label4 = ContactLabel("d", "ad", "aaa", 0, 1)
        database.saveAllContactGroups(label1, label2, label3, label4)
        val size = database.findContactGroupsLiveData().testValue?.size
        Assert.assertEquals(size, 4)
    }

    @Test
    fun testLabelSameKeyShouldSaveOnlyLast() {
        val label1 = ContactLabel("a", "aa", "aaa")
        val label2 = ContactLabel("a", "ab", "aab", 0, 1)
        val label3 = ContactLabel("a", "ac", "aac")
        database.saveContactGroupLabel(label1)
        database.saveContactGroupLabel(label2)
        database.saveContactGroupLabel(label3)

        val actual = database.findContactGroupByIdBlocking("a")
        Assert.assertEquals(label3, actual)
    }

    @Test
    fun testClearLabelsTable() {
        val label1 = ContactLabel("a", "aa", "aaa")
        val label2 = ContactLabel("b", "ab", "aab", 0, 1)
        val label3 = ContactLabel("c", "ac", "aac")
        database.saveAllContactGroups(label1, label2, label3)
        val sizeAfterInsert = database.findContactGroupsLiveData().testValue?.size
        Assert.assertEquals(sizeAfterInsert, 3)
        database.clearContactGroupsLabelsTableBlocking()
        val sizeAfterClearing = database.findContactGroupsLiveData().testValue?.size
        Assert.assertEquals(sizeAfterClearing, 0)
    }

    @Test
    fun testFindAllLabelsByIds() {
        val label1 = ContactLabel("a", "aa", "aaa")
        val label2 = ContactLabel("b", "ab", "aab", 0, 1)
        val label3 = ContactLabel("c", "ac", "aac")
        val label4 = ContactLabel("d", "ad", "aaa")
        val label5 = ContactLabel("e", "ae", "aab", 0, 1)
        val label6 = ContactLabel("f", "af", "aac")
        database.saveAllContactGroups(label1, label2, label3, label4, label5, label6)
        val requiredIds = Arrays.asList("b", "d", "f")
        val returnedLabels = database.getAllContactGroupsByIds(requiredIds).testValue
        val sizeOfReturnedLabels = returnedLabels?.size
        Assert.assertEquals(sizeOfReturnedLabels, 3)
        val labelFirst = returnedLabels?.get(0)
        val labelSecond = returnedLabels?.get(1)
        val labelThird = returnedLabels?.get(2)
        Assert.assertEquals(labelFirst, label2)
        Assert.assertEquals(labelSecond, label4)
        Assert.assertEquals(labelThird, label6)
    }

    @Test
    fun testRelationship() {
        val label1 = ContactLabel("la", "aa", "aaa")
        val label2 = ContactLabel("lb", "ab", "aab", 0, 1)
        val label3 = ContactLabel("lc", "ac", "aac")
        val label4 = ContactLabel("ld", "ad", "aad")

        val email1 = ContactEmail("e1", "1@1.1", "a", labelIds = listOf("la", "lc"))
        val email2 = ContactEmail("e2", "2@2.2", "b", labelIds = listOf("la", "lc"))
        val email3 = ContactEmail("e3", "3@3.3", "c", labelIds = listOf("la", "lc"))
        val email4 = ContactEmail("e4", "4@4.4", "d", labelIds = listOf("lb"))
        val email5 = ContactEmail("e5", "5@5.5", "e", labelIds = listOf("lb"))
        val email6 = ContactEmail("e6", "6@6.6", "f", labelIds = listOf("ld"))

        database.saveAllContactsEmailsBlocking(email1, email2, email3, email4, email5, email6)
        database.saveAllContactGroups(label1, label2, label3, label4)

        val contactEmailContactLabel1 = ContactEmailContactLabelJoin("e1", "la")
        val contactEmailContactLabel2 = ContactEmailContactLabelJoin("e1", "lc")
        val contactEmailContactLabel3 = ContactEmailContactLabelJoin("e2", "la")
        val contactEmailContactLabel4 = ContactEmailContactLabelJoin("e2", "lc")
        val contactEmailContactLabel5 = ContactEmailContactLabelJoin("e3", "la")
        val contactEmailContactLabel6 = ContactEmailContactLabelJoin("e3", "lc")
        val contactEmailContactLabel7 = ContactEmailContactLabelJoin("e4", "lb")
        val contactEmailContactLabel8 = ContactEmailContactLabelJoin("e5", "lb")
        val contactEmailContactLabel9 = ContactEmailContactLabelJoin("e6", "ld")

        database.saveContactEmailContactLabelBlocking(contactEmailContactLabel1, contactEmailContactLabel2, contactEmailContactLabel3,
            contactEmailContactLabel4, contactEmailContactLabel5, contactEmailContactLabel6, contactEmailContactLabel7,
            contactEmailContactLabel8, contactEmailContactLabel9)
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
        val label1 = ContactLabel("la", "aa", "aaa")
        val label2 = ContactLabel("lb", "ab", "aab", 0, 1)

        val email1 = ContactEmail("e1", "1@1.1", labelIds = listOf("la", "lb"), name = "ce1")
        val email2 = ContactEmail("e2", "2@2.2", labelIds = listOf("la"), name = "ce2")
        val email4 = ContactEmail("e4", "4@4.4", labelIds = listOf("lb"), name = "ce3")
        val email5 = ContactEmail("e5", "5@5.5", labelIds = listOf("lb", "la"), name = "ce4")

        database.saveAllContactsEmailsBlocking(email1, email2, email4, email5)
        database.saveAllContactGroups(label1, label2)

        val contactEmailContactLabel1 = ContactEmailContactLabelJoin("e1", "la")
        val contactEmailContactLabel2 = ContactEmailContactLabelJoin("e1", "lb")
        val contactEmailContactLabel3 = ContactEmailContactLabelJoin("e2", "la")
        val contactEmailContactLabel4 = ContactEmailContactLabelJoin("e4", "lb")
        val contactEmailContactLabel7 = ContactEmailContactLabelJoin("e5", "lb")
        val contactEmailContactLabel8 = ContactEmailContactLabelJoin("e5", "la")

        database.saveContactEmailContactLabelBlocking(contactEmailContactLabel1, contactEmailContactLabel2,
            contactEmailContactLabel3, contactEmailContactLabel4, contactEmailContactLabel7, contactEmailContactLabel8)
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
        val label1 = ContactLabel("la", "aa", "aaa")
        val label2 = ContactLabel("lb", "ab", "aab", 0, 1)

        val email1 = ContactEmail("e1", "1@1.1", labelIds = listOf("la", "lb"), name = "ce1")
        val email2 = ContactEmail("e2", "2@2.2", labelIds = listOf("la"), name = "ce2")
        val email4 = ContactEmail("e3", "3@3.3", labelIds = listOf("lb"), name = "ce3")
        val email5 = ContactEmail("e4", "4@3.4", labelIds = listOf("lb", "la"), name = "ce4")

        database.saveAllContactsEmailsBlocking(email1, email2, email4, email5)
        database.saveAllContactGroups(label1, label2)

        val contactEmailContactLabel1 = ContactEmailContactLabelJoin("e1", "la")
        val contactEmailContactLabel2 = ContactEmailContactLabelJoin("e1", "lb")
        val contactEmailContactLabel3 = ContactEmailContactLabelJoin("e2", "la")
        val contactEmailContactLabel4 = ContactEmailContactLabelJoin("e3", "lb")
        val contactEmailContactLabel7 = ContactEmailContactLabelJoin("e4", "lb")
        val contactEmailContactLabel8 = ContactEmailContactLabelJoin("e4", "la")

        database.saveContactEmailContactLabelBlocking(contactEmailContactLabel1, contactEmailContactLabel2,
            contactEmailContactLabel3, contactEmailContactLabel4, contactEmailContactLabel7, contactEmailContactLabel8)
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
    fun testReturnedCorrectContactGroupsForContactEmail() {
        val label1 = ContactLabel("la", "aa", "aaa")
        val label2 = ContactLabel("lb", "ab", "aab", 0, 1)

        val email1 = ContactEmail("e1", "1@1.1", labelIds = listOf("la", "lb"), name = "ce1")
        val email2 = ContactEmail("e2", "2@2.2", labelIds = listOf("la"), name = "ce2")
        val email4 = ContactEmail("e4", "4@4.4", labelIds = listOf("lb"), name = "ce3")
        val email5 = ContactEmail("e5", "5@5.5", labelIds = listOf("lb", "la"), name = "ce4")

        database.saveAllContactsEmailsBlocking(email1, email2, email4, email5)
        database.saveAllContactGroups(label1, label2)

        val contactEmailContactLabel1 = ContactEmailContactLabelJoin("e1", "la")
        val contactEmailContactLabel2 = ContactEmailContactLabelJoin("e1", "lb")
        val contactEmailContactLabel3 = ContactEmailContactLabelJoin("e2", "la")
        val contactEmailContactLabel4 = ContactEmailContactLabelJoin("e4", "lb")
        val contactEmailContactLabel7 = ContactEmailContactLabelJoin("e5", "lb")
        val contactEmailContactLabel8 = ContactEmailContactLabelJoin("e5", "la")

        database.saveContactEmailContactLabelBlocking(contactEmailContactLabel1, contactEmailContactLabel2,
            contactEmailContactLabel3, contactEmailContactLabel4, contactEmailContactLabel7, contactEmailContactLabel8)
        val laReturnedEmails = database.findAllContactGroupsByContactEmailAsync("e1").testValue

        Assert.assertNotNull(laReturnedEmails)
        Assert.assertEquals(listOf(label1, label2), laReturnedEmails)
    }
    //endregion
}
