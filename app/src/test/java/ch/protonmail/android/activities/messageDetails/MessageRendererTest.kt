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
import ch.protonmail.android.jobs.helper.EmbeddedImage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
import kotlin.test.BeforeTest
import kotlin.test.Test

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
        }
    }

    @BeforeTest
    fun before() {
        for (image in mockEmbeddedImages) {
            val file = folder.newFile(image.localFileName)
            file.writeText("_")
        }
    }

    private fun CoroutineScope.Renderer() =
        MessageRenderer(dispatchers, folder.root, mockDocumentParser, mockImageDecoder, this)
            .apply { messageBody = "" }

    @Test
    fun `renderedBody doesn't emit for images sent with too short delay`() = coroutinesTest {
        mockkStatic(Base64::class) {
            every { Base64.encodeToString(any(), any()) } returns "string"

            val scope = this + Job()
            val renderer = scope.Renderer()

            val count = 2
            val consumer: (String) -> Unit = mockk(relaxed = true)

            with(renderer) {
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
    }

    @Test
    fun `renderedBody emits for every image sent with right delay`() = coroutinesTest {
        mockkStatic(Base64::class) {
            every { Base64.encodeToString(any(), any()) } returns "string"

            val scope = this + Job()
            val renderer = scope.Renderer()

            val count = 2
            val consumer: (String) -> Unit = mockk(relaxed = true)
            val expectedDebounceTime = 500L

            with(renderer) {
                launch(Unconfined) {
                    renderedMessage.consumeEach(consumer)
                }
                repeat(count) {
                    images.offer(mockEmbeddedImages)
                    advanceTimeBy(expectedDebounceTime)
                }
                advanceUntilIdle()
                renderedMessage.close()
                scope.cancel()
            }

            verify(exactly = count) { consumer(any()) }
        }
    }

}
