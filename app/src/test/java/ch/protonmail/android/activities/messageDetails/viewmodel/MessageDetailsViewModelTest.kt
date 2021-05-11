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
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.activities.messageDetails.MessageRenderer
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.attachments.AttachmentsHelper
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.fetch.FetchVerificationKeys
import ch.protonmail.android.utils.DownloadUtils
import com.birbit.android.jobqueue.JobManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageDetailsViewModelTest : ArchTest, CoroutinesTest {

    private var savedStateHandle = mockk<SavedStateHandle> {
        every { get<String>(MessageDetailsActivity.EXTRA_MESSAGE_ID) } returns "id1"
        every { get<Boolean>(MessageDetailsActivity.EXTRA_TRANSIENT_MESSAGE) } returns false
    }

    private val downloadUtils = DownloadUtils()

    private val messageDetailsRepository: MessageDetailsRepository = mockk(relaxed = true)

    private val userManager: UserManager = mockk(relaxed = true)

    private val jobManager: JobManager = mockk(relaxed = true)

    private val contactsRepository: ContactsRepository = mockk(relaxed = true)

    private val attachmentsHelper: AttachmentsHelper = mockk(relaxed = true)

    private val attachmentMetadataDao: AttachmentMetadataDao = mockk(relaxed = true)

    private var messageRendererFactory = mockk<MessageRenderer.Factory> {
        every { create(any(), any()) } returns mockk(relaxed = true) {
            every { renderedBody } returns Channel()
        }
    }

    private val moveMessagesToFolder: MoveMessagesToFolder = mockk(relaxed = true)

    private val fetchVerificationKeys: FetchVerificationKeys = mockk(relaxed = true)

    private val verifyConnection: VerifyConnection = mockk(relaxed = true)

    private val networkConfigurator: NetworkConfigurator = mockk(relaxed = true)

    private val attachmentsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer = mockk(relaxed = true)

    private val viewModel = MessageDetailsViewModel(
        savedStateHandle,
        messageDetailsRepository,
        userManager,
        contactsRepository,
        attachmentMetadataDao,
        fetchVerificationKeys,
        attachmentsWorker,
        dispatchers,
        attachmentsHelper,
        downloadUtils,
        moveMessagesToFolder,
        messageRendererFactory,
        verifyConnection,
        networkConfigurator,
    )

    @Test
    fun verifyThatMessageIsParsedProperly() {
        // given
        val decryptedMessage = "decrypted message content"
        val windowWidth = 500
        val defaultErrorMessage = "errorHappened"
        val cssContent = "css"
        val expected =
            "<html>\n <head>\n  <style>$cssContent</style>\n  <meta name=\"viewport\" content=\"width=$windowWidth, maximum-scale=2\"> \n </head>\n <body>\n  <div id=\"pm-body\" class=\"inbox-body\">   $decryptedMessage  \n  </div>\n </body>\n</html>"

        // when
        val parsedMessage = viewModel.getParsedMessage(decryptedMessage, windowWidth, cssContent, defaultErrorMessage)

        // then
        assertEquals(expected, parsedMessage)
    }
}
