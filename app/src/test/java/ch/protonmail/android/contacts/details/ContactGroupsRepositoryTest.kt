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

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.testAndroid.rx.TestSchedulerRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

class ContactGroupsRepositoryTest {

    @get:Rule
    val testSchedulerRule = TestSchedulerRule()

    @RelaxedMockK
    private lateinit var protonMailApi: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var contactsDao: ContactsDao

    @InjectMockKs
    private lateinit var contactGroupsRepository: ContactGroupsRepository

    private val dispatcherProvider = TestDispatcherProvider

    private val label1 = ContactLabel("a", "aa")
    private val label2 = ContactLabel("b", "bb")
    private val label3 = ContactLabel("c", "cc")
    private val label4 = ContactLabel("d", "dd")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun verifyThatDbAndApiContactsAreEmittedInOrder() {
        runBlockingTest {
            // given
            val dbContactsList = listOf(label1)
            val searchTerm = "Rob"
            coEvery { contactsDao.findContactGroupsFlow("%$searchTerm%") } returns flowOf(dbContactsList)
            coEvery { contactsDao.countContactEmailsByLabelId(any()) } returns 1

            // when
            val result = contactGroupsRepository.observeContactGroups(searchTerm).first()

            // then
            assertEquals(dbContactsList, result)
        }
    }

    @Test
    fun verifyThatDbAndApiContactsAreEmittedIn() {
        runBlockingTest {
            // given
            val dbContactsList = listOf(label1)
            val searchTerm = "Rob"
            coEvery { contactsDao.findContactGroupsFlow("%$searchTerm%") } returns flowOf(dbContactsList)
            coEvery { contactsDao.countContactEmailsByLabelId(any()) } returns 1

            // when
            val result = contactGroupsRepository.observeContactGroups(searchTerm).first()

            // then
            assertEquals(dbContactsList, result)
        }
    }

    @Test
    fun verifyThatDbContactsAreEmitted() {
        runBlockingTest {
            // given
            val searchTerm = "search"
            val dbContactsList = listOf(label1)
            coEvery { contactsDao.findContactGroupsFlow("%$searchTerm%") } returns flowOf(dbContactsList)
            coEvery { contactsDao.countContactEmailsByLabelId(any()) } returns 1

            // when
            val result = contactGroupsRepository.observeContactGroups(searchTerm).first()

            // then
            assertEquals(dbContactsList, result)
        }
    }

    @Test
    fun saveContactGroupStoresGivenContactGroupInDatabase() {
        val contactGroup = ContactLabel("Id", "name", "color")

        contactGroupsRepository.saveContactGroup(contactGroup)

        verify { contactsDao.saveContactGroupLabel(contactGroup) }
    }

}
