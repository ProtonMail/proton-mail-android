/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.feature.rating

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class MailboxScreenViewInMemoryRepositoryTest {

    private val showReviewAppRepository = MailboxScreenViewInMemoryRepository()

    @Test
    fun `increase mailbox screen views counter when record mailbox screen view is called`() = runTest {
        // given
        check(showReviewAppRepository.screenViewCount == 0)

        // when
        showReviewAppRepository.recordScreenView()

        // then
        assertEquals(1, showReviewAppRepository.screenViewCount)
    }

}