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

package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Label
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class MessageLocationResolverTest {

    private val messageDao = mockk<MessageDao>()

    private val messageFactory = MessageLocationResolver(messageDao)

    @Test
    fun verifyLocationIsCorrectlyResolvedFromLabelsWithMultipleInputs() {

        // given
        val testLabelIds = listOf(
            "5",
            "6",
            "a3z7Gw2gVTdgp00hH4NNoTouuQI2LH2kBzJd-SaGyF3UnlwKOgM-B32G9Fgj6aKq_ewuy3DAioOIXnQRGlrdJg=="
        )
        val expected = Constants.MessageLocationType.ARCHIVE

        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        assertEquals(expected, result)
    }

    @Test
    fun verifyThatDraftsAreProperlyResolvedFromLabelsWithMultipleInputs() {

        // given
        val testLabelIds = listOf(
            "1",
            "5",
            "8"
        )
        val expected = Constants.MessageLocationType.DRAFT

        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        assertEquals(expected, result)
    }

    @Test
    fun verifyThatSingleAllLocationIsInvalid() {

        // given
        val testLabelIds = listOf(
            "5"
        )
        val expected = Constants.MessageLocationType.ALL_MAIL

        // when
        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyLocationIsCorrectlyResolvedFromLabelsWithJustOneValueNotAll() {

        // given
        val testLabelIds = listOf(
            "4"
        )
        val expected = Constants.MessageLocationType.SPAM

        // when
        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyLocationIsCorrectlyResolvedFromEmptyLabelsList() {

        // given
        val testLabelIds = emptyList<String>()
        val expected = Constants.MessageLocationType.INBOX

        // when
        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatSingleLabelFolderLocationIsParsedProperly() {

        // given
        val testLabelIds = listOf(
            "a3z7Gw2gVTdgp00hH4NNoTouuQI2LH2kBzJd-SaGyF3UnlwKOgM-B32G9Fgj6aKq_ewuy3DAioOIXnQRGlrdJg=="
        )
        val expected = Constants.MessageLocationType.LABEL_FOLDER
        val testLabel = mockk<Label> {
            every { exclusive } returns true
        }
        every { messageDao.findLabelById(any()) } returns testLabel

        // when
        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatSingleLabelLocationIsParsedProperly() {

        // given
        val testLabelIds = listOf(
            "a3z7Gw2gVTdgp00hH4NNoTouuQI2LH2kBzJd-SaGyF3UnlwKOgM-B32G9Fgj6aKq_ewuy3DAioOIXnQRGlrdJg=="
        )
        val expected = Constants.MessageLocationType.LABEL
        val testLabel = mockk<Label> {
            every { exclusive } returns false
        }
        every { messageDao.findLabelById(any()) } returns testLabel

        // when
        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThaLabelFolderLocationWithAllLocationIsParsedProperly() {

        // given
        val testLabelIds = listOf(
            "5",
            "a3z7Gw2gVTdgp00hH4NNoTouuQI2LH2kBzJd-SaGyF3UnlwKOgM-B32G9Fgj6aKq_ewuy3DAioOIXnQRGlrdJg=="
        )
        val expected = Constants.MessageLocationType.LABEL_FOLDER
        val testLabel = mockk<Label> {
            every { exclusive } returns true
        }
        every { messageDao.findLabelById(any()) } returns testLabel

        // when
        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThaLabelFolderLocationWithAllLocationAndStarredIsParsedProperly() {

        // given
        val testLabelIds = listOf(
            "5",
            "10",
            "KixlLFyrSj5yTKHwMrhJjKEwSQ3MW7nxyIMplCGpaH8jL5mamO1oYM9Djo2S6pAm8EQmJ3CYMmo4Jpg_ax-LWw==",
            "hk3g-efDXUe5pZKWzIkPPYKueFyAu9UCYRlD2ej-auBnu8gSC2g6hC0OVSkZm_3zdKkZdvLZBtRwydhjvUi-Wg=="
        )
        val expected = Constants.MessageLocationType.LABEL_FOLDER
        val testLabel = mockk<Label> {
            every { exclusive } returns true
        }
        every { messageDao.findLabelById(any()) } returns testLabel

        // when
        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        // then
        assertEquals(expected, result)
    }
}
