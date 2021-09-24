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

import android.graphics.Bitmap
import android.util.Base64
import app.cash.turbine.test
import ch.protonmail.android.details.presentation.model.RenderedMessage
import ch.protonmail.android.jobs.helper.EmbeddedImage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.plus
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.kotlin.EMPTY_STRING
import org.jsoup.nodes.Document
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MessageRendererTest : CoroutinesTest {

    private val testMessageId = "message id"
    private val testMessageBody = "Message body"

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()
        .also { it.create() }

    private val mockImageDecoder: ImageDecoder = mockk {
        every { this@mockk(any(), any()) } returns buildMockBitmap()
    }

    private val mockDocumentParser: DocumentParser = mockk {
        every { this@mockk(any()) } returns buildMockDocument()
    }

    private fun CoroutineScope.buildRenderer() =
        MessageRenderer(
            dispatchers = dispatchers,
            documentParser = mockDocumentParser,
            bitmapImageDecoder = mockImageDecoder,
            attachmentsDirectory = folder.root,
            scope = this + Job()
        ).apply {
            setMessageBody(testMessageId, testMessageBody)
        }

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
    fun doesNotEmitResultForImagesSentWithTooShortDelayForTheSameMessage() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 4..7)
        createFilesFor(imageSet1, imageSet2)

        // when
        messageRenderer.results.test {

            // then
            messageRenderer.setImagesAndStartProcess(testMessageId, imageSet1)
            expectItem()

            messageRenderer.setImagesAndStartProcess(testMessageId, imageSet2)
            expectNoEvents()
        }
    }

    @Test
    fun emitsResultForEveryImagesSentWithTooShortDelayForDifferentMessages() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val firstMessageId = "message 1"
        val secondMessageId = "message 2"
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 4..7)
        createFilesFor(imageSet1, imageSet2)
        messageRenderer.setMessageBody(firstMessageId, EMPTY_STRING)
        messageRenderer.setMessageBody(secondMessageId, EMPTY_STRING)

        // when
        messageRenderer.results.test {

            // then
            messageRenderer.setImagesAndStartProcess(firstMessageId, imageSet1)
            expectItem()

            messageRenderer.setImagesAndStartProcess(secondMessageId, imageSet2)
            expectItem()
        }
    }

    @Test
    fun emitsResultForEveryImageSentWithRightDelay() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 4..7)
        createFilesFor(imageSet1, imageSet2)

        // when
        messageRenderer.results.test {

            // then
            messageRenderer.setImagesAndStartProcess(testMessageId, imageSet1)
            expectItem()

            advanceUntilIdle()

            messageRenderer.setImagesAndStartProcess(testMessageId, imageSet2)
            expectItem()
        }
    }

    @Test
    fun correctlyInlineImagesInTheMessage() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val messageId = "message id"
        val imageSet = buildEmbeddedImages(idsRange = 1..2)
        val messageBody = """
            This is the first picture:
            img[src=${imageSet[0].contentId}]
            And this is another picture:
            img[src=${imageSet[1].contentId}]
        """.trimIndent()

        setBase64EncodeToStringToIncrementalResult()
        setMockDocumentParserToReplaceStringsInMessage(messageBody)
        createFilesFor(imageSet)

        val expectedMessageBody = """
            This is the first picture:
            data:;,image 1
            And this is another picture:
            data:;,image 2
        """.trimIndent()
        val expected = RenderedMessage(messageId, expectedMessageBody)

        // when
        messageRenderer.setMessageBody(testMessageId, messageBody)
        messageRenderer.results.test {
            messageRenderer.setImagesAndStartProcess(testMessageId, imageSet)

            // then
            assertEquals(expected, expectItem())
        }
    }

    @Test
    fun correctlyCompressesTheImages() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet = buildEmbeddedImages(idsRange = 1..3)
        createFilesFor(imageSet)

        val mockBitmap = buildMockBitmap()
        every { mockImageDecoder(any(), any()) } returns mockBitmap

        // when
        messageRenderer.setImagesAndStartProcess(testMessageId, imageSet)

        // then
        verify(exactly = imageSet.size) { mockBitmap.compress(any(), any(), any()) }
    }

    @Test
    fun skipsImagesAlreadyProcessedForTheSameMessage() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 1..5)
        createFilesFor(imageSet1, imageSet2)

        val mockBitmap = buildMockBitmap()
        every { mockImageDecoder(any(), any()) } returns mockBitmap

        // when
        messageRenderer.setImagesAndStartProcess(testMessageId, imageSet1)
        advanceUntilIdle()
        messageRenderer.setImagesAndStartProcess(testMessageId, imageSet2)

        // then
        verify(exactly = imageSet2.size) { mockBitmap.compress(any(), any(), any()) }
    }

    @Test
    fun doesNotSkipImagesAlreadyProcessedForAnotherMessage() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val firstMessageId = "message 1"
        val secondMessageId = "message 2"
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 1..5)
        createFilesFor(imageSet1, imageSet2)
        messageRenderer.setMessageBody(firstMessageId, EMPTY_STRING)
        messageRenderer.setMessageBody(secondMessageId, EMPTY_STRING)

        val mockBitmap = buildMockBitmap()
        every { mockImageDecoder(any(), any()) } returns mockBitmap

        val expectedImagesProcessedCount = imageSet1.size + imageSet2.size

        // when
        messageRenderer.setImagesAndStartProcess(firstMessageId, imageSet1)
        messageRenderer.setImagesAndStartProcess(secondMessageId, imageSet2)

        // then
        verify(exactly = expectedImagesProcessedCount) { mockBitmap.compress(any(), any(), any()) }
    }

    @Test(expected = IllegalStateException::class)
    fun setImagesAndStartProcessThrowsExceptionIfNoMessageBodySetForGivenMessageId() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val messageId1 = "message 1"
        val messageId2 = "message 2"

        // when
        messageRenderer.setMessageBody(messageId1, EMPTY_STRING)
        messageRenderer.setImagesAndStartProcess(messageId2, emptyList())
    }

    @Test
    fun rendersImagesForDifferentMessages() = coroutinesTest {
        // given
        val messageRenderer = buildRenderer()
        val firstMessageId = "id1"
        val secondMessageId = "id2"
        val firstMessageBody = "First message body"
        val secondMessageBody = "Second message body"
        val imageSet = buildEmbeddedImages(idsRange = 1..10)
        createFilesFor(imageSet)

        val firstMessageBodyWithInlinedImages = "$firstMessageBody with inlined images"
        val secondMessageBodyWithInlinedImages = "$secondMessageBody with inlined images"

        every { mockDocumentParser(firstMessageBody) } returns
            buildMockDocument(content = firstMessageBodyWithInlinedImages)
        every { mockDocumentParser(secondMessageBody) } returns
            buildMockDocument(content = secondMessageBodyWithInlinedImages)

        val expectedFirstRenderedMessage = RenderedMessage(firstMessageId, firstMessageBodyWithInlinedImages)
        val expectedSecondRenderedMessage = RenderedMessage(secondMessageId, secondMessageBodyWithInlinedImages)

        // when
        messageRenderer.setMessageBody(firstMessageId, firstMessageBody)
        messageRenderer.setMessageBody(secondMessageId, secondMessageBody)
        messageRenderer.setImagesAndStartProcess(firstMessageId, imageSet)
        messageRenderer.setImagesAndStartProcess(secondMessageId, imageSet)

        messageRenderer.results.test {

            // then
            assertEquals(expectedFirstRenderedMessage, expectItem())
            assertEquals(expectedSecondRenderedMessage, expectItem())
        }
    }

    private fun buildMockDocument(content: String? = null, block: Document.() -> Unit = {}): Document =
        mockk(relaxed = true) {
            every { this@mockk.toString() } returns (content ?: "document")
            block()
        }

    private fun buildMockDocumentWithReplaceFeature(documentString: String) = buildMockDocument document@{
        var document = documentString
        every { this@document.select(any<String>()) } answers {
            val query = firstArg<String>()

            mockk(relaxed = true) elements@{
                every { this@elements.attr(any(), any()) } answers {
                    document = document.replace(query, secondArg())
                    this@elements
                }
            }
        }
        every { this@document.toString() } answers { document }
    }

    private fun buildMockBitmap(block: Bitmap.() -> Unit = {}): Bitmap =
        mockk {
            every { compress(any(), any(), any()) } returns true
            block()
        }

    private fun buildEmbeddedImages(
        messageId: String = "message id",
        idsRange: IntRange = 0..10
    ): List<EmbeddedImage> = idsRange.map {
        EmbeddedImage(
            attachmentId = "attachment $it",
            fileName = "file $it",
            key = EMPTY_STRING,
            contentType = EMPTY_STRING,
            encoding = EMPTY_STRING,
            contentId = "content $it",
            mimeData = null,
            size = 10,
            messageId = messageId,
            localFileName = "local file $it"
        )
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
                runCatching { // ignore pre-existent files
                    val file = folder.newFile("$messageId/$fileName")
                    file.writeText("_")
                }
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

    private fun setBase64EncodeToStringToIncrementalResult() {
        var count = 0
        every { Base64.encodeToString(any(), any()) } answers { "image ${++count}" }
    }

    private fun setMockDocumentParserToReplaceStringsInMessage(message: String) {
        every { mockDocumentParser(any()) } returns buildMockDocumentWithReplaceFeature(message)
    }
}
