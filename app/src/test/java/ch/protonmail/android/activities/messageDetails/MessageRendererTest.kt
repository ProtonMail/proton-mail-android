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

package ch.protonmail.android.activities.messageDetails

import android.util.Base64
import ch.protonmail.android.details.presentation.model.RenderedMessage
import ch.protonmail.android.jobs.helper.EmbeddedImage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for [MessageRenderer]
 * @author Davide Farella
 */
internal class MessageRendererTest : CoroutinesTest {

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()
        .also { it.create() }

    private val mockImageDecoder: ImageDecoder = mockk(relaxed = true)

    private val mockDocumentParser: DocumentParser = mockk(relaxed = true)

    private val mockEmbeddedImages = (1..10).map {
        mockk<EmbeddedImage>(relaxed = true) {
            every { localFileName } returns "$it"
            every { contentId } returns "id"
            every { encoding } returns ""
            every { messageId } returns "messageId-1"
        }
    }

    @BeforeTest
    fun setUp() {
        mockkStatic(Base64::class)

        // As the same MessageRenderer instance can be called for different messages
        // So the SUT saves images in a folder named as the messageId (which is the same for all embedded images in a list)
        folder.newFolder(mockEmbeddedImages.first().messageId)
        for (image in mockEmbeddedImages) {
            val file = folder.newFile("${image.messageId}/${image.localFileName}")
            file.writeText("_")
        }
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    private fun CoroutineScope.Renderer() =
        MessageRenderer(dispatchers, mockDocumentParser, mockImageDecoder, folder.root, this)
            .apply { messageBody = "" }

    @Test
    fun `renderedBody doesn't emit for images sent with too short delay`() = coroutinesTest {
        every { Base64.encodeToString(any(), any()) } returns "string"

        val scope = this + Job()
        val messageRenderer = scope.Renderer()

        val count = 2
        val consumer: (RenderedMessage) -> Unit = mockk(relaxed = true)

        with(messageRenderer) {
            launch(Unconfined) {
                renderedMessage.consumeEach(consumer)
            }
            repeat(count) {
                images.offer(mockEmbeddedImages)
            }
            advanceUntilIdle()
            renderedMessage.close()
            scope.cancel()
        }

        verify(exactly = 1) { consumer(any()) }
    }

    @Test
    fun `renderedBody emits for every image sent with right delay`() = coroutinesTest {
        every { Base64.encodeToString(any(), any()) } returns "string"

        val scope = this + Job()
        val messageRenderer = scope.Renderer()

        val count = 2
        val consumer: (RenderedMessage) -> Unit = mockk(relaxed = true)
        val expectedDebounceTime = 500L

        with(messageRenderer) {
            launch(Unconfined) {
                renderedMessage.consumeEach(consumer)
            }
            repeat(count) {
                images.offer(mockEmbeddedImages)
                // By setting a new body we reset messageRender internal "inlinedImageIds" list as it would otherwise
                // prevent the second emission since the contentIds were already inlined.
                messageBody = "new message body"
                advanceTimeBy(expectedDebounceTime)
            }
            advanceUntilIdle()
            renderedMessage.close()
            scope.cancel()
        }

        verify(exactly = count) { consumer(any()) }
    }

    @Test
    fun messageRendererRendersImagesForDifferentMessagesByChangingTheMessageBody() = coroutinesTest {
        // Render images for the first message
        // Given
        val scope = this + Job()
        val messageRenderer = scope.Renderer()
        val firstMessageBody = "first message body"
        val secondMessageBody = "second message body"
        messageRenderer.messageBody = firstMessageBody
        every { Base64.encodeToString(any(), any()) } returns "base64EncodedImageData"
        every { mockDocumentParser.invoke(firstMessageBody) } returns mockk(relaxed = true) {
            every { this@mockk.toString() } returns "First message body with inlined images"
        }

        // When
        messageRenderer.images.offer(mockEmbeddedImages)
        advanceTimeBy(500)

        // Then
        val expected = RenderedMessage("messageId-1", "First message body with inlined images")
        val actual = messageRenderer.renderedMessage.tryReceive().getOrNull()
        assertEquals(expected, actual)

        // Render images for a second message
        // Given
        every { mockDocumentParser.invoke(secondMessageBody) } returns mockk(relaxed = true) {
            every { this@mockk.toString() } returns "Second message body with inlined images"
        }
        messageRenderer.messageBody = secondMessageBody

        // When
        messageRenderer.images.offer(mockEmbeddedImages)

        // Then
        val secondMessageExpected = RenderedMessage("messageId-1", "Second message body with inlined images")
        val secondMessageActual = messageRenderer.renderedMessage.tryReceive().getOrNull()
        assertEquals(secondMessageExpected, secondMessageActual)
    }
}
