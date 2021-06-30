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
import org.junit.Test
import kotlin.test.assertEquals

class MessageLocationResolverTest {

    private val messageFactory = MessageLocationResolver()

    @Test
    fun verifyLocationIsCorrectlyResolvedFromLabelsWithMultipleInputs() {

        // given
        val testLabelIds = listOf(
            "5",
            "6",
            "a3z7Gw2gVTdgp00hH4NNoTouuQI2LH2kBzJd-SaGyF3UnlwKOgM-B32G9Fgj6aKq_ewuy3DAioOIXnQRGlrdJg=="
        )
        val expected = 6

        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        assertEquals(expected, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun verifyThatSingleAllLocationIsInvalid() {

        // given
        val testLabelIds = listOf(
            "5"
        )
        val expected = 5

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
        val expected = 4

        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        assertEquals(expected, result)
    }

    @Test
    fun verifyLocationIsCorrectlyResolvedFromEmptyLabelsList() {

        // given
        val testLabelIds = emptyList<String>()
        val expected = 0

        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        assertEquals(expected, result)
    }

    @Test
    fun verifyThatSingleLabelLocationIsInvalid() {

        // given
        val testLabelIds = listOf(
            "a3z7Gw2gVTdgp00hH4NNoTouuQI2LH2kBzJd-SaGyF3UnlwKOgM-B32G9Fgj6aKq_ewuy3DAioOIXnQRGlrdJg=="
        )
        val expected = Constants.MessageLocationType.LABEL_FOLDER.messageLocationTypeValue

        // when
        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThaLabelLocationWithAllLocationIsInvalid() {

        // given
        val testLabelIds = listOf(
            "5",
            "a3z7Gw2gVTdgp00hH4NNoTouuQI2LH2kBzJd-SaGyF3UnlwKOgM-B32G9Fgj6aKq_ewuy3DAioOIXnQRGlrdJg=="
        )
        val expected = Constants.MessageLocationType.LABEL_FOLDER.messageLocationTypeValue

        // when
        val result = messageFactory.resolveLocationFromLabels(testLabelIds)

        // then
        assertEquals(expected, result)
    }
}
