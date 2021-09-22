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
import app.cash.turbine.test
import ch.protonmail.android.details.presentation.model.RenderedMessage
import ch.protonmail.android.jobs.helper.EmbeddedImage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.plus
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MessageRendererTest : CoroutinesTest {

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()
        .also { it.create() }

    private val mockImageDecoder: ImageDecoder = mockk(relaxed = true)

    private val mockDocumentParser: DocumentParser = mockk(relaxed = true)

    private fun CoroutineScope.buildRenderer() =
        MessageRenderer(
            dispatchers = dispatchers,
            documentParser = mockDocumentParser,
            bitmapImageDecoder = mockImageDecoder,
            attachmentsDirectory = folder.root,
            scope = this + Job()
        ).apply { messageBody = "" }

    @BeforeTest
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } returns "string"
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun renderedBodyDoesNotEmitForImagesSentWithTooShortDelay() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildMockEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildMockEmbeddedImages(idsRange = 4..7)
        createFilesFor(imageSet1, imageSet2)

        // when
        messageRenderer.renderedMessage.consumeAsFlow().test {

            // then
            messageRenderer.images.trySend(imageSet1)
            expectItem()

            messageRenderer.images.trySend(imageSet2)
            expectNoEvents()
        }
    }

    @Test
    fun renderedBodyEmitsForEveryImageSentWithRightDelay() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildMockEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildMockEmbeddedImages(idsRange = 4..7)
        createFilesFor(imageSet1, imageSet2)

        // when
        messageRenderer.renderedMessage.consumeAsFlow().test {

            // then
            messageRenderer.images.trySend(imageSet1)
            expectItem()

            advanceUntilIdle()

            messageRenderer.images.trySend(imageSet2)
            expectItem()
        }
    }

    @Test
    @Ignore("To be refactored")
    fun messageRendererRendersImagesForDifferentMessagesByChangingTheMessageBody() = coroutinesTest {
        // Render images for the first message
        // Given
        val messageRenderer = buildRenderer()
        val firstMessageBody = "first message body"
        val secondMessageBody = "second message body"
        val imageSet = buildMockEmbeddedImages(idsRange = 1..10)
        createFilesFor(imageSet)
        messageRenderer.messageBody = firstMessageBody
        every { Base64.encodeToString(any(), any()) } returns "base64EncodedImageData"
        every { mockDocumentParser.invoke(firstMessageBody) } returns mockk(relaxed = true) {
            every { this@mockk.toString() } returns "First message body with inlined images"
        }

        // When
        messageRenderer.images.trySend(imageSet)
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
        messageRenderer.images.trySend(imageSet)

        // Then
        val secondMessageExpected = RenderedMessage("messageId-1", "Second message body with inlined images")
        val secondMessageActual = messageRenderer.renderedMessage.tryReceive().getOrNull()
        assertEquals(secondMessageExpected, secondMessageActual)
    }

    private fun buildMockEmbeddedImages(
        messageIdSuffix: Int = 1,
        idsRange: IntRange = 0..10
    ): List<EmbeddedImage> = idsRange.map {
        mockk(relaxed = true) {
            every { localFileName } returns "$it"
            every { contentId } returns "id $it"
            every { encoding } returns ""
            every { messageId } returns "messageId-$messageIdSuffix"
        }
    }

    private fun createFilesFor(vararg imageSets: List<EmbeddedImage>) {
        val messagesIdsToFilesNames = imageSets.map { imageSet ->
            // Group by message id
            imageSet.groupBy { image -> image.messageId }
                // Take only file names
                .mapValues { (_, imageList) -> imageList.map { image -> image.localFileName } }
        }.mergeMaps()

        for ((messageId, filesNames) in messagesIdsToFilesNames) {
            folder.newFolder(messageId)
            for (fileName in filesNames) {
                val file = folder.newFile("$messageId/$fileName")
                file.writeText("_")
            }
        }
    }

    private fun <K, V> Collection<Map<K, List<V>>>.mergeMaps(): Map<K, List<V>> {
        val keys = flatMap { map -> map.keys }
        return keys.map { key ->
            // Take values of each maps, for the given key
            key to flatMap { map ->
                map[key] ?: emptyList()
            }
        }.toMap()
    }
}
