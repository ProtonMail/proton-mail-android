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

package ch.protonmail.android.mailbox.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

/**
 * Tests the behaviour of [UpdateConversationsLabels]
 */
class UpdateConversationsLabelsTest {

    private val conversationsRepository = mockk<ConversationsRepository>()

    private val updateConversationsLabels = UpdateConversationsLabels(
        conversationsRepository
    )

    @Test
    fun verifyLabelUnlabelMethodsFromRepositoryAreCalled() {
        runBlockingTest {
            // given
            val conversationIds = listOf("conversationId1", "conversationId2")
            val userId = UserId("userId")
            val label1 = "label1"
            val label2 = "label2"
            val label3 = "label3"
            val selectedLabels = listOf(label1, label2)
            val unselectedLabels = listOf(label3)
            coEvery { conversationsRepository.label(conversationIds, userId, any()) } just runs
            coEvery { conversationsRepository.unlabel(conversationIds, userId, any()) } just runs

            // when
            updateConversationsLabels.invoke(conversationIds, userId, selectedLabels, unselectedLabels)

            // then
            coVerify {
                conversationsRepository.label(conversationIds, userId, label1)
            }
            coVerify {
                conversationsRepository.label(conversationIds, userId, label2)
            }
            coVerify {
                conversationsRepository.unlabel(conversationIds, userId, label3)
            }
        }
    }
}
