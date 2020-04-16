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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package ch.protonmail.android.activities.messageDetails

import ch.protonmail.android.jobs.helper.EmbeddedImage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Test

/**
 * Test class for [MessageRenderer]
 * @author Davide Farella
 */
internal class MessageRendererTest {

    private val mockImageDecoder : ImageDecoder = { _, _ -> mockk() }
    private val mockDocumentParser : DocumentParser = { mockk() }
    private val mockEmbeddedImages = (1..10).map {
        mockk<EmbeddedImage>(relaxed = true) { every { localFileName } returns "" }
    }

    private val CoroutineScope.newRenderer get() =
        MessageRenderer(mockk(), mockDocumentParser, mockImageDecoder, this)
                .apply { messageBody = "" }

    @Test // TODO, replace with `runBlockingTest` and `advanceTimeBy` when this is resolved: java.lang.IllegalStateException: This job has not completed yet
    fun `renderedBody emits just once for every images sent`() {
        val count = 2
        val consumer: (String) -> Unit = mockk(relaxed = true)
        with(TestCoroutineScope().newRenderer) {
            launch { renderedBody.consumeEach(consumer) }
            repeat(count) { runBlocking {
                images.send(mockEmbeddedImages)
                delay(550)
            } }
            renderedBody.close()
        }
        verify(exactly = count) { consumer(any()) }
    }
}
