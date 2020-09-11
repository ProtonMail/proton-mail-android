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
package ch.protonmail.android.contacts.groups.details

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.EmptyResultSetException
import androidx.work.WorkManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.factories.IConverterFactory
import ch.protonmail.android.api.models.messages.receive.ServerLabel
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.core.Constants
import ch.protonmail.android.testAndroid.rx.TestSchedulerRule
import com.birbit.android.jobqueue.JobManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Flowable
import io.reactivex.Single
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

class ContactGroupDetailsRepositoryTest {
    //region mocks
    private val protonMailApi = mockk<ProtonMailApiManager>(relaxed = true)
    private val database = mockk<ContactsDatabase>(relaxed = true)
    private val jobManager = mockk<JobManager>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val contactLabelFactory = mockk<IConverterFactory<ServerLabel, ContactLabel>>(relaxed = true)
    private val databaseProvider = mockk<DatabaseProvider>(relaxed = true) {
        every { provideContactsDao(any()) } returns database
    }
    //endregion
    
    //region rules
    @get:Rule val rule = InstantTaskExecutorRule()
    @get:Rule val rule2 = TestSchedulerRule()
    //endregion
    
    //region tests
    @Test
    fun testCorrectContactGroupReturnedById() {
        val label1 = ContactLabel("a", "aa")
        every { database.findContactGroupByIdAsync("") } returns Single.just(label1)
        val contactGroupDetailsRepository = ContactGroupDetailsRepository(workManager, jobManager, protonMailApi, databaseProvider)

        val testObserver = contactGroupDetailsRepository.findContactGroupDetails("").test()
        testObserver.awaitTerminalEvent()
        testObserver.assertValue(label1)
    }

    @Test
    fun testReturnNullWrongContactId() {
        val label1 = ContactLabel("a", "aa")
        every { database.findContactGroupByIdAsync("a") } returns Single.just(label1)
        every { database.findContactGroupByIdAsync(any()) } returns Single.error(EmptyResultSetException("no such element"))
        val contactGroupDetailsRepository = ContactGroupDetailsRepository(workManager, jobManager, protonMailApi, databaseProvider)

        val testObserver = contactGroupDetailsRepository.findContactGroupDetails("b").test()
        testObserver.awaitTerminalEvent()
        Assert.assertEquals(0, testObserver.valueCount())
        testObserver.assertError(EmptyResultSetException::class.java)
    }

    @Test
    fun testCorrectGetContactGroupEmails() {
        val email1 = ContactEmail("a", "a@a.a", name = "ce1")
        val email2 = ContactEmail("b", "b@b.b", name = "ce2")
        every { database.findAllContactsEmailsByContactGroupAsyncObservable(any()) } returns Flowable.just(listOf(email1, email2))
        val contactGroupDetailsRepository = ContactGroupDetailsRepository(workManager, jobManager, protonMailApi, databaseProvider)

        val testObserver = contactGroupDetailsRepository.getContactGroupEmails("").test()
        testObserver.awaitTerminalEvent()
        val returnedResult = testObserver.values()[0]
        val first = returnedResult?.get(0)
        val second = returnedResult?.get(1)

        Assert.assertEquals(email1, first)
        Assert.assertEquals(email2, second)
    }

    @Test
    fun testEmptyGetContactGroupEmails() {
        val emptyList: List<ContactEmail> = emptyList()
        every { database.findAllContactsEmailsByContactGroupAsyncObservable(any()) } returns Flowable.just(emptyList)
        val contactGroupDetailsRepository = ContactGroupDetailsRepository(workManager, jobManager, protonMailApi, databaseProvider)

        val testObserver = contactGroupDetailsRepository.getContactGroupEmails("").test()
        testObserver.awaitTerminalEvent()
        val returnedResult = testObserver.values()
        Assert.assertEquals(emptyList<ContactEmail>(), returnedResult[0]) //not sure about this
    }

    @Test
    fun testCreateNewContactGroup() {
        val toCreateContactGroup = ContactLabel("a", "aa", "aaa", type = Constants.LABEL_TYPE_CONTACT_GROUPS)
        val serverLabel = ServerLabel("a", "aa", "aaa", type = Constants.LABEL_TYPE_CONTACT_GROUPS)
        val contactLabel = ContactLabel("a1", "aa", "aaa", type = Constants.LABEL_TYPE_CONTACT_GROUPS)
        every { contactLabelFactory.createServerObjectFromDBObject(any()) } returns serverLabel
        every { protonMailApi.createLabelCompletable(any()) } returns Single.just(contactLabel)
        val contactGroupDetailsRepository = ContactGroupDetailsRepository(workManager, jobManager, protonMailApi, databaseProvider)

        val testObserver = contactGroupDetailsRepository.createContactGroup(toCreateContactGroup).test()
        testObserver.awaitTerminalEvent()
        verify(exactly = 1) { database.saveContactGroupLabel(contactLabel) }
        testObserver.assertValue(contactLabel)
    }

    @Test
    fun testFailedToContactApiScheduledJob() {
        val toCreateContactGroup = ContactLabel("a", "aa", "aaa", type = Constants.LABEL_TYPE_CONTACT_GROUPS)
        val serverLabel = ServerLabel("a", "aa", "aaa", type = Constants.LABEL_TYPE_CONTACT_GROUPS)
        every { contactLabelFactory.createServerObjectFromDBObject(any()) } returns serverLabel
        every { protonMailApi.createLabelCompletable(any()) } returns Single.error(IOException("api unreachable"))
        val contactGroupDetailsRepository = ContactGroupDetailsRepository(workManager, jobManager, protonMailApi, databaseProvider)

        val testObserver = contactGroupDetailsRepository.createContactGroup(toCreateContactGroup).test()
        testObserver.awaitTerminalEvent()
        verify(exactly = 1) { jobManager.addJobInBackground(any()) }
        verify(exactly = 0) { database.saveContactGroupLabel(any()) }
    }

    @Test
    fun testOtherExceptionScheduledJobNotCalled() {
        val toCreateContactGroup = ContactLabel("a", "aa", "aaa", type = Constants.LABEL_TYPE_CONTACT_GROUPS)
        val serverLabel = ServerLabel("a", "aa", "aaa", type = Constants.LABEL_TYPE_CONTACT_GROUPS)
        every { contactLabelFactory.createServerObjectFromDBObject(any()) } returns serverLabel
        every { protonMailApi.createLabelCompletable(any()) } returns Single.error(NullPointerException(":("))
        val contactGroupDetailsRepository = ContactGroupDetailsRepository(workManager, jobManager, protonMailApi, databaseProvider)

        val testObserver = contactGroupDetailsRepository.createContactGroup(toCreateContactGroup).test()
        testObserver.awaitTerminalEvent()
        verify(exactly = 0) { jobManager.addJob(any()) }
    }

    @Test
    fun testCorrectGetContactEmailGroups() {
        val email1 = ContactEmail("a", "a@a.a", name = "ce1")
        val email2 = ContactEmail("b", "b@b.b", name = "ce2")
        every { database.findAllContactsEmailsByContactGroupAsyncObservable(any()) } returns Flowable.just(listOf(email1, email2))
        val contactGroupDetailsRepository = ContactGroupDetailsRepository(workManager, jobManager, protonMailApi, databaseProvider)

        val testObserver = contactGroupDetailsRepository.getContactGroupEmails("").test()
        testObserver.awaitTerminalEvent()
        val returnedResult = testObserver.values()[0]
        val first = returnedResult?.get(0)
        val second = returnedResult?.get(1)

        assertEquals(email1, first)
        assertEquals(email2, second)
    }
    //endregion
}
