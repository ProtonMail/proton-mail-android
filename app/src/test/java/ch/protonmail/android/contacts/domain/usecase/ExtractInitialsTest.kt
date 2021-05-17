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

package ch.protonmail.android.contacts.domain.usecase

import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Name
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [ExtractInitials]
 */
class ExtractInitialsTest {

    private val extractInitials = ExtractInitials()

    @Test
    fun twoWordsName() {
        // given
        val name = Name("Proton user")

        // when - then
        val result = extractInitials(name, DummyEmail)
        assertEquals("PU", result)
    }

    @Test
    fun ignoresInitialSymbolInFirstWordOfTheName() {
        // given
        val name = Name("📛Proton user")

        // when - then
        val result = extractInitials(name, DummyEmail)
        assertEquals("PU", result)
    }

    @Test
    fun ignoresInitialSymbolInSecondWordOfTheName() {
        // given
        val name = Name("Proton ❌user")

        // when - then
        val result = extractInitials(name, DummyEmail)
        assertEquals("PU", result)
    }

    @Test
    fun takesFirstTwoLettersOfTheNameIfOneWord() {
        // given
        val name = Name("Proton")

        // when - then
        val result = extractInitials(name, DummyEmail)
        assertEquals("PR", result)
    }

    @Test
    fun takesFirstTwoValidLettersOfTheNameIfOneWordWithSymbols() {
        // given
        val name = Name("a?Proton")

        // when - then
        val result = extractInitials(name, DummyEmail)
        assertEquals("AP", result)
    }

    @Test
    fun takesFirstTwoLettersOfTheEmailIfNameIsNotComplaint() {
        // given
        val name = Name("P")
        val email = EmailAddress("user@proton.me")

        // when - then
        val result = extractInitials(name, email)
        assertEquals("US", result)
    }

    @Test
    fun edgeCase() {
        // given
        val name = Name("!@")
        val email = EmailAddress("$%a@ho.x")

        // when - then
        val result = extractInitials(name, email)
        assertEquals("AH", result)
    }

    private companion object {

        val DummyEmail = EmailAddress("dummy@email.address")
    }
}
