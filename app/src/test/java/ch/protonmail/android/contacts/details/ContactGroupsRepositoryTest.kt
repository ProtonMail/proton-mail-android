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
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import io.reactivex.Flowable
import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Rule
import java.io.IOException
import java.util.concurrent.TimeUnit
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

    private val label1 = ContactLabel("a", "aa")
    private val label2 = ContactLabel("b", "bb")
    private val label3 = ContactLabel("c", "cc")
    private val label4 = ContactLabel("d", "dd")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { protonMailApi.fetchContactGroupsAsObservable() } answers {
            Observable.just(listOf(label1, label2, label3, label4)).delay(500, TimeUnit.MILLISECONDS)
        }
        every { contactsDao.findContactGroupsObservable() } answers { Flowable.just(listOf(label1, label2, label3)) }
    }

    @Test
    fun testDbAndAPIEventsEmitted() {
        val testObserver = contactGroupsRepository.getContactGroups().test()

        testSchedulerRule.schedulerTest.advanceTimeBy(1000, TimeUnit.MILLISECONDS)
        testObserver.awaitTerminalEvent()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(2)
    }

    @Test
    fun testDbEventBeforeAPIEvent() {
        val testObserver = contactGroupsRepository.getContactGroups().test()

        testObserver.assertValueCount(1)
        testObserver.assertValue(listOf(label1, label2, label3))
        testSchedulerRule.schedulerTest.advanceTimeBy(1000, TimeUnit.MILLISECONDS)
        testObserver.awaitCount(2)
        testObserver.assertValueCount(2)
        assertEquals(listOf(label1, label2, label3, label4), testObserver.values()[1])
    }

    @Test
    fun testApiErrorEvent() {
        every { protonMailApi.fetchContactGroupsAsObservable() } answers { Observable.error(IOException(":(")) }

        val testObserver = contactGroupsRepository.getContactGroups().test()

        testSchedulerRule.schedulerTest.triggerActions()
        testSchedulerRule.schedulerTest.advanceTimeBy(1000, TimeUnit.MILLISECONDS)
        testObserver.awaitTerminalEvent()
        testObserver.assertValue(listOf(label1, label2, label3))
        testObserver.assertError(IOException::class.java)
    }

    @Test
    fun saveContactGroupStoresGivenContactGroupInDatabase() {
        val contactGroup = ContactLabel("Id", "name", "color")

        contactGroupsRepository.saveContactGroup(contactGroup)

        verify { contactsDao.saveContactGroupLabel(contactGroup) }
    }

}
