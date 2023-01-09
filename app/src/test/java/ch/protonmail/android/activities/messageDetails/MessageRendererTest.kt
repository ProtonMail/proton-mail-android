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

package ch.protonmail.android.activities.messageDetails

import android.graphics.Bitmap
import android.util.Base64
import ch.protonmail.android.details.presentation.model.RenderedMessage
import ch.protonmail.android.jobs.helper.EmbeddedImage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.kotlin.EMPTY_STRING
import org.jsoup.nodes.Document
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TEST_MESSAGE_ID = "message id"
private const val TEST_MESSAGE_ID_1 = "message 1"
private const val TEST_MESSAGE_ID_2 = "message 2"
private const val TEST_MESSAGE_BODY = "Message body"
private const val TEST_MESSAGE_BODY_1 = "Message body 1"
private const val TEST_MESSAGE_BODY_2 = "Message body 2"
private const val TEST_DOCUMENT_CONTENT = "document"

internal class MessageRendererTest : CoroutinesTest by CoroutinesTest() {

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()
        .also { it.create() }

    private val mockImageDecoder: ImageDecoder = mockk {
        every { this@mockk(any(), any()) } returns buildMockBitmap()
    }

    private val mockDocumentParser: DocumentParser = mockk {
        coEvery { this@mockk(any()) } returns buildMockDocument()
    }

    private fun CoroutineScope.buildRenderer() =
        MessageRenderer(
            dispatchers = dispatchers,
            documentParser = mockDocumentParser,
            bitmapImageDecoder = mockImageDecoder,
            attachmentsDirectory = folder.root,
            scope = this + Job()
        ).apply {
            setMessageBody(TEST_MESSAGE_ID, TEST_MESSAGE_BODY)
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
    fun returnsResultForASingleImagesSetSent() = runTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet = buildEmbeddedImages(idsRange = 1..3)
        createFilesFor(imageSet)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID, EMPTY_STRING)

        // when
        val result = messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID, imageSet)

        // then
        assertEquals(RenderedMessage(TEST_MESSAGE_ID, TEST_DOCUMENT_CONTENT), result)
    }

    @Test
    fun returnsResultForEachMessageSequentially() = runTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 4..7)
        createFilesFor(imageSet1, imageSet2)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_1, EMPTY_STRING)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_2, EMPTY_STRING)

        // when
        val result1 = messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_1, imageSet1)
        val result2 = messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_2, imageSet2)

        // then
        assertEquals(RenderedMessage(TEST_MESSAGE_ID_1, TEST_DOCUMENT_CONTENT), result1)
        assertEquals(RenderedMessage(TEST_MESSAGE_ID_2, TEST_DOCUMENT_CONTENT), result2)
    }

    @Test
    fun returnsResultForEachMessageInParallel() = runTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 4..7)
        createFilesFor(imageSet1, imageSet2)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_1, EMPTY_STRING)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_2, EMPTY_STRING)

        // when
        val result1Deferred = async { messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_1, imageSet1) }
        val result2Deferred = async { messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_2, imageSet2) }

        // then
        assertEquals(RenderedMessage(TEST_MESSAGE_ID_1, TEST_DOCUMENT_CONTENT), result1Deferred.await())
        assertEquals(RenderedMessage(TEST_MESSAGE_ID_2, TEST_DOCUMENT_CONTENT), result2Deferred.await())
    }

    @Test
    fun returnsResultForEachMessageInParallelWithDifferentExecutionTimes() = runTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 4..7)
        createFilesFor(imageSet1, imageSet2)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_1, TEST_MESSAGE_BODY_1)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_2, TEST_MESSAGE_BODY_2)

        coEvery { mockDocumentParser(TEST_MESSAGE_BODY_1) } coAnswers {
            delay(500)
            buildMockDocument()
        }
        coEvery { mockDocumentParser(TEST_MESSAGE_BODY_2) } coAnswers  {
            delay(1)
            buildMockDocument()
        }

        // when
        val result1Deferred = async { messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_1, imageSet1) }
        val result2Deferred = async { messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_2, imageSet2) }

        // then
        assertEquals(RenderedMessage(TEST_MESSAGE_ID_1, TEST_DOCUMENT_CONTENT), result1Deferred.await())
        assertEquals(RenderedMessage(TEST_MESSAGE_ID_2, TEST_DOCUMENT_CONTENT), result2Deferred.await())
    }

    @Test
    fun returnsResultForEachMessageWithReversedOrder() = runTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 4..7)
        createFilesFor(imageSet1, imageSet2)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_1, EMPTY_STRING)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_2, EMPTY_STRING)

        // when
        val result2 = messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_2, imageSet2)
        val result1 = messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_1, imageSet1)

        // then
        assertEquals(RenderedMessage(TEST_MESSAGE_ID_1, TEST_DOCUMENT_CONTENT), result1)
        assertEquals(RenderedMessage(TEST_MESSAGE_ID_2, TEST_DOCUMENT_CONTENT), result2)
    }

    @Test
    fun correctlyInlineImagesInTheMessage() = runTest {
        // given
        val messageRenderer = buildRenderer()
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
        val expected = RenderedMessage(TEST_MESSAGE_ID, expectedMessageBody)

        // when
        messageRenderer.setMessageBody(TEST_MESSAGE_ID, messageBody)
        val result = messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID, imageSet)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun correctlyCompressesTheImages() = runTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet = buildEmbeddedImages(idsRange = 1..3)
        createFilesFor(imageSet)

        val mockBitmap = buildMockBitmap()
        every { mockImageDecoder(any(), any()) } returns mockBitmap

        // when
        messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID, imageSet)

        // then
        verify(exactly = imageSet.size) { mockBitmap.compress(any(), any(), any()) }
    }

    @Test
    fun skipsImagesAlreadyProcessedForTheSameMessage() = runTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 1..5)
        createFilesFor(imageSet1, imageSet2)

        val mockBitmap = buildMockBitmap()
        every { mockImageDecoder(any(), any()) } returns mockBitmap

        // when
        messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID, imageSet1)
        advanceUntilIdle()
        messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID, imageSet2)

        // then
        verify(exactly = imageSet2.size) { mockBitmap.compress(any(), any(), any()) }
    }

    @Test
    fun doesNotSkipImagesAlreadyProcessedForAnotherMessage() = runTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet1 = buildEmbeddedImages(idsRange = 1..3)
        val imageSet2 = buildEmbeddedImages(idsRange = 1..5)
        createFilesFor(imageSet1, imageSet2)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_1, EMPTY_STRING)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_2, EMPTY_STRING)

        val mockBitmap = buildMockBitmap()
        every { mockImageDecoder(any(), any()) } returns mockBitmap

        val expectedImagesProcessedCount = imageSet1.size + imageSet2.size

        // when
        messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_1, imageSet1)
        messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_2, imageSet2)

        // then
        verify(exactly = expectedImagesProcessedCount) { mockBitmap.compress(any(), any(), any()) }
    }

    @Test(expected = IllegalStateException::class)
    fun setImagesAndProcessThrowsExceptionIfNoMessageBodySetForGivenMessageId() = runTest {
        // given
        val messageRenderer = buildRenderer()

        // when
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_1, EMPTY_STRING)
        messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_2, emptyList())
    }

    @Test
    fun rendersImagesForDifferentMessages() = runTest {
        // given
        val messageRenderer = buildRenderer()
        val imageSet = buildEmbeddedImages(idsRange = 1..10)
        createFilesFor(imageSet)

        val firstMessageBodyWithInlinedImages = "$TEST_MESSAGE_BODY_1 with inlined images"
        val secondMessageBodyWithInlinedImages = "$TEST_MESSAGE_BODY_2 with inlined images"

        coEvery { mockDocumentParser(TEST_MESSAGE_BODY_1) } returns
            buildMockDocument(content = firstMessageBodyWithInlinedImages)
        coEvery { mockDocumentParser(TEST_MESSAGE_BODY_2) } returns
            buildMockDocument(content = secondMessageBodyWithInlinedImages)

        val expectedFirstRenderedMessage = RenderedMessage(TEST_MESSAGE_ID_1, firstMessageBodyWithInlinedImages)
        val expectedSecondRenderedMessage = RenderedMessage(TEST_MESSAGE_ID_2, secondMessageBodyWithInlinedImages)

        // when
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_1, TEST_MESSAGE_BODY_1)
        messageRenderer.setMessageBody(TEST_MESSAGE_ID_2, TEST_MESSAGE_BODY_2)
        val result1 = messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_1, imageSet)
        val result2 = messageRenderer.setImagesAndProcess(TEST_MESSAGE_ID_2, imageSet)

        // then
        assertEquals(expectedFirstRenderedMessage, result1)
        assertEquals(expectedSecondRenderedMessage, result2)
    }

    private fun buildMockDocument(
        content: String = TEST_DOCUMENT_CONTENT,
        block: Document.() -> Unit = {}
    ): Document =
        mockk(relaxed = true) {
            every { this@mockk.toString() } returns content
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

    private fun buildMockBitmap(): Bitmap =
        mockk {
            every { compress(any(), any(), any()) } returns true
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
        // Take values of each maps, for the given key
        return keys.associateWith { key ->
            // Take values of each maps, for the given key
            flatMap { map ->
                map[key] ?: emptyList()
            }
        }
    }

    private fun setBase64EncodeToStringToIncrementalResult() {
        var count = 0
        every { Base64.encodeToString(any(), any()) } answers { "image ${++count}" }
    }

    private fun setMockDocumentParserToReplaceStringsInMessage(message: String) {
        coEvery { mockDocumentParser(any()) } returns buildMockDocumentWithReplaceFeature(message)
    }
}
