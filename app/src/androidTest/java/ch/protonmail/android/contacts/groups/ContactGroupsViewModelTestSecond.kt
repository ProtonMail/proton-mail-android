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
package ch.protonmail.android.contacts.groups

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.contacts.groups.list.ContactGroupsViewModel
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.testAndroid.rx.TrampolineScheduler
import ch.protonmail.android.usecase.delete.DeleteLabel
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ContactGroupsViewModelTestSecond {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseFactory = Room.inMemoryDatabaseBuilder(context, ContactsDatabaseFactory::class.java)
            .allowMainThreadQueries()
            .build()

    //region mocks and vars
    private val label1 = ContactLabel("a", "aa")
    private val label2 = ContactLabel("b", "bb")
    private val label3 = ContactLabel("c", "cc")

    private val protonMailApi = mockk<ProtonMailApiManager>(relaxed = true) {
        every { fetchContactGroupsAsObservable() } returns Observable.error(IOException(":("))
    }
    private val deleteLabel = mockk<DeleteLabel>(relaxed = true)
    private val userManager = mockk<UserManager>(relaxed = true)
    private val databaseProvider = mockk<DatabaseProvider>(relaxed = true) {
        every { provideContactsDao(any()) } returns databaseFactory.getDatabase()
    }
    //endregion

    //region rules
    @get: Rule val taskExecutorRule = InstantTaskExecutorRule()
    @get: Rule val rxSchedulerRule = TrampolineScheduler()
    //endregion

    //region tests

    @After
    @Throws(Exception::class)
    fun close() {
        databaseFactory.close()
    }

    @Test
    fun dbUpdatesLiveData() {
        databaseFactory.getDatabase().saveAllContactGroups(label1, label2, label3)

        val contactGroupsRepository = ContactGroupsRepository(protonMailApi, databaseProvider)
        val contactGroupsViewModel = ContactGroupsViewModel(contactGroupsRepository, userManager, deleteLabel)
        val resultLiveData = contactGroupsViewModel.contactGroupsResult.testObserver()
        val errorLiveData = contactGroupsViewModel.contactGroupsError.testObserver()
        contactGroupsViewModel.fetchContactGroups(ThreadSchedulers.io())
        assertEquals(listOf(label1, label2, label3), resultLiveData.observedValues[0])
    }
    //endregion
}
