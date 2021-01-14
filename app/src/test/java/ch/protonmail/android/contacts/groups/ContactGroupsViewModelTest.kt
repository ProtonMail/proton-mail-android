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
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.contacts.groups.list.ContactGroupsViewModel
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.testAndroid.rx.TrampolineScheduler
import ch.protonmail.android.usecase.delete.DeleteLabel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test


class ContactGroupsViewModelTest : CoroutinesTest {

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get: Rule
    val rxSchedulerRule = TrampolineScheduler()

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var deleteLabel: DeleteLabel

    @RelaxedMockK
    private lateinit var contactGroupsRepository: ContactGroupsRepository

    @InjectMockKs
    private lateinit var contactGroupsViewModel: ContactGroupsViewModel

    private val label1 = ContactLabel("a", "aa")
    private val label2 = ContactLabel("b", "bb")
    private val label3 = ContactLabel("c", "cc")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `fetch contact groups posts contactLabels on contactGroupResult LiveData when repository succeeds`() {
        val resultLiveData = contactGroupsViewModel.contactGroupsResult.testObserver()
        val contactLabels = listOf(label1, label2, label3)
        every { contactGroupsRepository.getContactGroups() } returns Observable.just(contactLabels)

        contactGroupsViewModel.fetchContactGroups(Schedulers.trampoline())

        val observedContactLabels = resultLiveData.observedValues[0]
        assertEquals(contactLabels, observedContactLabels)
    }

    @Test
    fun `fetch contact groups posts error on contactGroupsError LiveData when repository fails`() {
        val resultLiveData = contactGroupsViewModel.contactGroupsError.testObserver()
        val exception = Exception("test-exception")
        every { contactGroupsRepository.getContactGroups() } returns Observable.error(exception)

        contactGroupsViewModel.fetchContactGroups(Schedulers.trampoline())

        val observedError = resultLiveData.observedValues[0]?.getContentIfNotHandled()
        assertEquals("test-exception", observedError)
    }

}
