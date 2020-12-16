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

package ch.protonmail.android.compose

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.services.PostMessageServiceFactory
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.testAndroid.rx.TrampolineScheduler
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.compose.SaveDraft
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchPublicKeys
import ch.protonmail.android.utils.extensions.InstantExecutorExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class, InstantExecutorExtension::class)
class ComposeMessageViewModelTest : CoroutinesTest {

    @Rule
    val trampolineSchedulerRule = TrampolineScheduler()

    @RelaxedMockK
    lateinit var composeMessageRepository: ComposeMessageRepository

    @RelaxedMockK
    lateinit var userManager: UserManager

    @RelaxedMockK
    lateinit var messageDetailsRepository: MessageDetailsRepository

    @RelaxedMockK
    lateinit var saveDraft: SaveDraft

    @MockK
    lateinit var postMessageServiceFactory: PostMessageServiceFactory

    @MockK
    lateinit var deleteMessage: DeleteMessage

    @MockK
    lateinit var fetchPublicKeys: FetchPublicKeys

    @MockK
    lateinit var networkConfigurator: NetworkConfigurator

    @MockK
    lateinit var verifyConnection: VerifyConnection

    @InjectMockKs
    lateinit var viewModel: ComposeMessageViewModel

    @Test
    fun saveDraftCallsSaveDraftUseCaseWhenTheDraftIsNew() {
        runBlockingTest {
            val message = Message()
            // Needed to set class fields to the right value
            viewModel.prepareMessageData(false, "addressId", "mail-alias", false)
            viewModel.setupComposingNewMessage(false, Constants.MessageActionType.FORWARD, "parentId823", "")
            viewModel.oldSenderAddressId = "previousSenderAddressId"

            viewModel.saveDraft(message, hasConnectivity = false)

            val parameters = SaveDraft.SaveDraftParameters(
                message,
                emptyList(),
                "parentId823",
                Constants.MessageActionType.FORWARD,
                "previousSenderAddressId"
            )
            coVerify { saveDraft(parameters) }
        }
    }

    @Test
    fun saveDraftReadsNewlyCreatedDraftFromRepositoryAndPostsItToLiveDataWhenSaveDraftUseCaseSucceeds() {
        runBlockingTest {
            val message = Message()
            val createdDraftId = "newDraftId"
            val createdDraft = Message(messageId = createdDraftId, localId = "local28348")
            // Needed to set class fields to the right value
            viewModel.prepareMessageData(false, "addressId", "mail-alias", false)
            viewModel.setupComposingNewMessage(false, Constants.MessageActionType.FORWARD, "parentId823", "")
            viewModel.oldSenderAddressId = "previousSenderAddressId"
            coEvery { saveDraft(any()) } returns flowOf(SaveDraft.Result.Success(createdDraftId))
            val testObserver = viewModel.savingDraftComplete.testObserver()
            every { messageDetailsRepository.findMessageById(createdDraftId) } returns createdDraft

            viewModel.saveDraft(message, hasConnectivity = false)

            verify { messageDetailsRepository.findMessageById(createdDraftId) }
            assertEquals(createdDraft, testObserver.observedValues[0])
        }
    }

}
