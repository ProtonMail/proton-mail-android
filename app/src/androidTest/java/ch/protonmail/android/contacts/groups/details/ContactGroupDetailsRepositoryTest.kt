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
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.LabelEntity
import ch.protonmail.android.testAndroid.rx.TestSchedulerRule
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.Assert
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

class ContactGroupDetailsRepositoryTest {

    //region mocks
    private val database = mockk<ContactDao>(relaxed = true)
    private val userManager = mockk<UserManager>(relaxed = true)
    private val databaseProvider = mockk<DatabaseProvider>(relaxed = true) {
        every { provideContactDao(any()) } returns database
    }
    //endregion

    //region rules
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val rule2 = TestSchedulerRule()
    //endregion

    private lateinit var contactGroupDetailsRepository: ContactGroupDetailsRepository

    @BeforeTest
    fun setUp() {
        contactGroupDetailsRepository =
            ContactGroupDetailsRepository(databaseProvider, userManager)

        every { userManager.requireCurrentUserId() } returns UserId("id")
    }

    @Test
    fun testCorrectContactGroupReturnedById() {
        val label1 = LabelEntity("a", "aa", "color", 0, 1, EMPTY_STRING, "parent", 0, 0)
        every { database.findContactGroupByIdAsync("") } returns Single.just(label1)

        val testObserver = contactGroupDetailsRepository.findContactGroupDetailsBlocking("").test()
        testObserver.awaitTerminalEvent()
        testObserver.assertValue(label1)
    }

    @Test
    fun testReturnNullWrongContactId() {
        val label1 = LabelEntity("a", "aa", "color", 0, 1, EMPTY_STRING, "parent", 0, 0)
        every { database.findContactGroupByIdAsync("a") } returns Single.just(label1)
        every { database.findContactGroupByIdAsync(any()) } returns Single.error(
            EmptyResultSetException("no such element")
        )

        val testObserver = contactGroupDetailsRepository.findContactGroupDetailsBlocking("b").test()
        testObserver.awaitTerminalEvent()
        Assert.assertEquals(0, testObserver.valueCount())
        testObserver.assertError(EmptyResultSetException::class.java)
    }
}
