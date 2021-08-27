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
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.testAndroid.rx.TestSchedulerRule
import io.mockk.every
import io.mockk.mockk
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.BeforeTest

class ContactGroupDetailsRepositoryTest {

    //region mocks
    private val database = mockk<ContactDao>(relaxed = true)
    private val userManager = mockk<UserManager>(relaxed = true)
    private val labelRepository = mockk<LabelRepository>(relaxed = true)
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

    private val testUserId = UserId("id")

    @BeforeTest
    fun setUp() {
        contactGroupDetailsRepository =
            ContactGroupDetailsRepository(databaseProvider, userManager, labelRepository)

        every { userManager.requireCurrentUserId() } returns testUserId
    }


}
