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
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.testAndroid.rx.TrampolineScheduler
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.compose.SaveDraft
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchPublicKeys
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ComposeMessageViewModelTest : CoroutinesTest {

    @Rule
    private val trampolineSchedulerRule = TrampolineScheduler()

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
    fun saveDraftCallsSaveDraftUseCaseWhenTheDraftIsNew() =
        runBlockingTest {
            val message = Message()
            val parentId = "parentId"
            // Needed to set a value to _messageDataResult field
            viewModel.prepareMessageData(false, "addressId", "mail-alias", false)

            viewModel.saveDraft(message, hasConnectivity = false)

            coVerify { saveDraft(message, emptyList(), "parentId") }
        }
}
