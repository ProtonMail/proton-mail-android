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
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.testValue
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.contacts.groups.list.ContactGroupsViewModel
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.testAndroid.rx.TrampolineScheduler
import com.birbit.android.jobqueue.JobManager
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import io.reactivex.Observable
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.IOException


/**
 * Created by kadrikj on 8/26/18. */
class ContactGroupsViewModelTest {

    //region mocks and vars
    private val label1 = ContactLabel("a", "aa")
    private val label2 = ContactLabel("b", "bb")
    private val label3 = ContactLabel("c", "cc")
    private val label4 = ContactLabel("d", "dd")

    private val jobManager = mockk<JobManager>(relaxed = true)
    private val protonMailApi = mockk<ProtonMailApi>(relaxed = true)
    private val contactsDatabase = mockk<ContactsDatabase>(relaxed = true) {
        every { findContactGroupsObservable() } returns Flowable.just(listOf(label1, label2, label3))
    }
    private val userManager = mockk<UserManager>(relaxed = true)
    private val databaseProvider = mockk<DatabaseProvider>(relaxed = true) {
        every { provideContactsDao() } returns contactsDatabase
    }
    //endregion

    //region rules
    @Rule
    @JvmField
    val taskExecutorRule = InstantTaskExecutorRule()
    @Rule
    @JvmField
    val rxSchedulerRule = TrampolineScheduler()
    //endregion

    //region tests
    @Test
    fun testUpdateFromDbAndFromApi() {
        val contactGroupsRepository = ContactGroupsRepository(jobManager, protonMailApi, databaseProvider)
        val contactGroupsViewModel = ContactGroupsViewModel(contactGroupsRepository, userManager)

        every { protonMailApi.fetchContactGroupsAsObservable() } returns Observable.just(listOf(label1, label2, label3, label4))
        val resultLiveData = contactGroupsViewModel.contactGroupsResult
        contactGroupsViewModel.fetchContactGroups(ThreadSchedulers.main())
        val size = resultLiveData.testValue?.size
        Assert.assertEquals(4, size)
    }

    @Test
    fun testUpdateFromDbOnly() {
        val contactGroupsRepository = ContactGroupsRepository(jobManager, protonMailApi, databaseProvider)
        val contactGroupsViewModel = ContactGroupsViewModel(contactGroupsRepository, userManager)

        every { protonMailApi.fetchContactGroupsAsObservable() } returns Observable.error(IOException(":("))
        val resultLiveData = contactGroupsViewModel.contactGroupsResult.testObserver()
        val errorLiveData = contactGroupsViewModel.contactGroupsError.testObserver()
        contactGroupsViewModel.fetchContactGroups(ThreadSchedulers.main())
        Assert.assertEquals(3, resultLiveData.observedValues[0]?.size)
        val actual = errorLiveData.observedValues[0]?.getContentIfNotHandled()?.let {
            it
        }
        Assert.assertEquals(":(", actual)
    }
    //endregion
}
