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

package ch.protonmail.android.utils

import java.net.MalformedURLException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MailToParserTest {

    private lateinit var parser: MailToParser

    private val testEmail1 = "name1@mail.com"
    private val testEmail2 = "name2@mail.com"
    private val testEmail3 = "abc@abc.ch"
    private val testEmail4 = "zxv@abc.ch"
    private val testTitle = "Tytul"
    private val testBody = "LoraineIpsum"

    @BeforeTest
    fun setUp() {
        parser = MailToParser()
    }

    @Test(expected = MalformedURLException::class)
    fun verifyThatUnsupportedFormatThrowsAnException() {
        // given
        val unsupportedDataString = "asdafdfsdf"

        // when
        parser.parseUrl(unsupportedDataString)
    }

    @Test(expected = IllegalArgumentException::class)
    fun verifyThatUnsupportedProtocolThrowsAnException() {
        // given
        val unsupportedDataString = "file:$testEmail1"

        // when
        parser.parseUrl(unsupportedDataString)
    }

    @Test
    fun verifyThatReferenceStringWithAllParametersIsParsedProperly() {
        // given
        val dataString =
            "mailto:$testEmail1,$testEmail2?cc=$testEmail3&bcc=$testEmail4&subject=$testTitle&body=$testBody"
        val expected = MailTo(
            listOf(testEmail1, testEmail2),
            listOf(testEmail3),
            testTitle,
            testBody,
            listOf(testEmail4)
        )

        // when
        val result = parser.parseUrl(dataString)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatReferenceStringWithOnlyToAddressesIsParsedProperly() {
        // given
        val dataString = "mailto:$testEmail1,$testEmail2"
        val expected = MailTo(
            listOf(testEmail1, testEmail2),
            emptyList(),
            "",
            "",
            emptyList()
        )

        // when
        val result = parser.parseUrl(dataString)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatReferenceStringWithOnlyToAddressAndTitleIsParsedProperly() {
        // given
        val dataString = "mailto:$testEmail1?subject=$testTitle"
        val expected = MailTo(
            listOf(testEmail1),
            emptyList(),
            testTitle,
            "",
            emptyList()
        )

        // when
        val result = parser.parseUrl(dataString)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatReferenceStringWithOnlyToAddressAndBodyIsParsedProperly() {
        // given
        val dataString = "mailto:$testEmail1?body=$testBody"
        val expected = MailTo(
            listOf(testEmail1),
            emptyList(),
            "",
            testBody,
            emptyList()
        )

        // when
        val result = parser.parseUrl(dataString)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatReferenceStringWithOnlyToAddressTitleAndBodyIsParsedProperly() {
        // given
        val dataString = "mailto:$testEmail1?body=$testBody&subject=$testTitle"
        val expected = MailTo(
            listOf(testEmail1),
            emptyList(),
            testTitle,
            testBody,
            emptyList()
        )

        // when
        val result = parser.parseUrl(dataString)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatReferenceStringWithTwoCcAddressesIsParsedProperly() {
        // given
        val dataString = "mailto:$testEmail1,$testEmail2?cc=$testEmail3,$testEmail4&subject=$testTitle&body=$testBody"
        val expected = MailTo(
            listOf(testEmail1, testEmail2),
            listOf(testEmail3, testEmail4),
            testTitle,
            testBody,
            emptyList()
        )

        // when
        val result = parser.parseUrl(dataString)

        // then
        assertEquals(expected, result)
    }
}
