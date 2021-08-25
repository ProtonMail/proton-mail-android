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
package ch.protonmail.android.jobs

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.di.JobEntryPoint
import dagger.hilt.EntryPoints
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val STARRED_LABEL_ID = "10"

public class PostStarJobTest {

    private val mockMessageDetailsRepository: MessageDetailsRepository = mockk(relaxed = true)

    private val mockUserManager: UserManager = mockk(relaxed = true)

    private val mockApiManager: ProtonMailApiManager = mockk(relaxed = true)

    private val jobEntryPoint: JobEntryPoint = mockk(relaxed = true)

    private lateinit var postStarJob: PostStarJob

    @BeforeTest
    fun setUp() {
        mockkStatic(ProtonMailApplication::class)
        every { ProtonMailApplication.getApplication() } returns mockk()

        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), JobEntryPoint::class.java) } returns jobEntryPoint

        every { jobEntryPoint.apiManager() } returns mockApiManager
        every { jobEntryPoint.userManager() } returns mockUserManager
        every { jobEntryPoint.messageDetailsRepository() } returns mockMessageDetailsRepository

        every { mockUserManager.requireCurrentUserId() } returns UserId("id")
    }

    @Test
    fun localMessageIsMarkedAsStarredWhenTheJobIsAdded() {
        // Starred status is determined by both the `isStarred` flag and
        // the presence of "STARRED" label ID (value = 10) in `allLabels`
        val messageId = "messageId"
        postStarJob = PostStarJob(listOf(messageId))
        val message = Message(
            messageId = messageId,
            isStarred = false,
            allLabelIDs = emptyList()
        )
        coEvery { mockMessageDetailsRepository.findMessageById(messageId) } returns flowOf(message)

        postStarJob.onAdded()

        // We also expect Location to be "Starred" because location is changed
        // silently when adding or removing labels
        val expected = Message(
            messageId = messageId,
            isStarred = true,
            allLabelIDs = listOf(STARRED_LABEL_ID),
            location = Constants.MessageLocationType.STARRED.messageLocationTypeValue
        )
        coVerify { mockMessageDetailsRepository.saveMessage(expected) }
    }
}

