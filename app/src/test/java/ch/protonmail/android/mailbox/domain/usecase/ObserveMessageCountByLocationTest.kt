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

package ch.protonmail.android.mailbox.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.core.Constants
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.testdata.UserTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveMessageCountByLocationTest {

    private val messagesRepository: MessageRepository = mockk()

    private val observeMessageCountByLocation = ObserveMessageCountByLocation(messagesRepository)

    @Test
    fun `emits data from messages repository`() = runBlockingTest {
        // given
        every {
            messagesRepository.observeMessagesCountByLocationFromDatabase(
                UserTestData.userId, Constants.MessageLocationType.ALL_SCHEDULED.asLabelIdString()
            )
        } returns flowOf(2)

        // when
        observeMessageCountByLocation(
            UserTestData.userId, Constants.MessageLocationType.ALL_SCHEDULED.asLabelIdString()
        ).test {

            // then
            assertEquals(2, awaitItem())
            awaitComplete()
        }
    }
}
