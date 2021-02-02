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

package ch.protonmail.android.activities.messageDetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import ch.protonmail.android.activities.messageDetails.MessageDetailsActivity
import ch.protonmail.android.activities.messageDetails.MessageRenderer
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabase
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchVerificationKeys
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageDetailsViewModelTest : ArchTest, CoroutinesTest {

    private var savedStateHandle = mockk<SavedStateHandle> {
        every { get<String>(MessageDetailsActivity.EXTRA_MESSAGE_ID) } returns "id1"
        every { get<Boolean>(MessageDetailsActivity.EXTRA_TRANSIENT_MESSAGE) } returns false
    }

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var contactsRepository: ContactsRepository

    @RelaxedMockK
    private lateinit var attachmentMetadataDatabase: AttachmentMetadataDatabase

    private var messageRendererFactory = mockk<MessageRenderer.Factory> {
        every { create(any(), any()) } returns mockk(relaxed = true) {
            every { renderedBody } returns Channel()
        }
    }

    @RelaxedMockK
    private lateinit var deleteMessageUseCase: DeleteMessage

    @RelaxedMockK
    private lateinit var fetchVerificationKeys: FetchVerificationKeys

    @RelaxedMockK
    private lateinit var verifyConnection: VerifyConnection

    @RelaxedMockK
    private lateinit var networkConfigurator: NetworkConfigurator

    @RelaxedMockK
    private lateinit var attachemntsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer

    @InjectMockKs
    private lateinit var viewModel: MessageDetailsViewModel

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun getParsedMessage() {
        val decryptedMessage = "decrypted message content"
        val windowWidth = 500
        val defaultErrorMessage = "errorHappened"
        val cssContent = "css"
        // given
        val expected = "<html>\n <head>\n  <style>$cssContent</style>\n  <meta name=\"viewport\" content=\"width=$windowWidth, maximum-scale=2\"> \n </head>\n <body>\n  <div id=\"pm-body\" class=\"inbox-body\">   $decryptedMessage  \n  </div>\n </body>\n</html>"

        // when
        val parsedMessage = viewModel.getParsedMessage(decryptedMessage, windowWidth, cssContent, defaultErrorMessage)

        // then
        assertEquals(expected, parsedMessage)
    }
}
