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

package ch.protonmail.android.domain.usecase.credentials

import assert4k.assert
import assert4k.equals
import assert4k.that
import ch.protonmail.android.domain.entity.Credential
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.repository.CredentialRepository
import ch.protonmail.android.domain.testDoubles.AnotherEmailAddress
import ch.protonmail.android.domain.testDoubles.FakeCredentialsRepository
import ch.protonmail.android.domain.testDoubles.JustAnEmailAddress
import ch.protonmail.android.domain.testDoubles.SomeEmailAddress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import me.proton.core.test.kotlin.CoroutinesTest

import kotlin.test.Test
import kotlin.time.DurationUnit
import kotlin.time.minutes
import kotlin.time.seconds
import kotlin.time.toDuration

class GetSavedCredentialsTest : CoroutinesTest {

    private val repo: CredentialRepository = FakeCredentialsRepository()
    private val getSavedCredentials = GetSavedCredentials(dispatchers, repository = repo)

    @Test
    fun `can get all saved credentials a single time`() = coroutinesTest {

        // Given
        repo[SomeEmailAddress] = Credential.FullyLoggedIn
        repo[AnotherEmailAddress] = Credential.MailboxPasswordRequired

        // When
        val result = getSavedCredentials().first()

        // Then
        assert that result equals mapOf(
            SomeEmailAddress to Credential.FullyLoggedIn,
            AnotherEmailAddress to Credential.MailboxPasswordRequired
        )
    }

    @Test
    fun `can get a single saved credentials a single time`() = coroutinesTest {

        // Given
        repo[SomeEmailAddress] = Credential.FullyLoggedIn

        // When
        val result = getSavedCredentials(SomeEmailAddress).first()

        // Then
        assert that result equals Credential.FullyLoggedIn
    }

    @Test
    fun `returns empty map for all credentials if no credential is found`() = coroutinesTest {

        // Given
        // * No saved credentials *

        // When
        val result = getSavedCredentials().first()

        // Then
        assert that result equals emptyMap()
    }

    @Test
    fun `returns NotFound for a single credentials a single time if not found`() = coroutinesTest {

        // Given
        // * No saved credentials *

        // When
        val result = getSavedCredentials(SomeEmailAddress).first()

        // Then
        assert that result equals Credential.NotFound
    }

    @Test
    fun `does publish updates correctly for all the credentials`() = coroutinesTest {

        val result = mutableListOf<Map<EmailAddress, Credential>>()
        val interval = 30.toDuration(DurationUnit.SECONDS)
        val flow = getSavedCredentials(interval - 5.toDuration(DurationUnit.SECONDS))
        val job = launch {
            delay(10)
            flow.toList(result)
        }

        // Given
        repo[SomeEmailAddress] = Credential.FullyLoggedIn

        // When
        advanceTimeBy(interval.toLongMilliseconds())
        repo[AnotherEmailAddress] = Credential.MailboxPasswordRequired

        advanceTimeBy(interval.toLongMilliseconds())
        repo -= AnotherEmailAddress

        advanceTimeBy(interval.toLongMilliseconds())
        repo[JustAnEmailAddress] = Credential.LoggedOut

        advanceTimeBy(interval.toLongMilliseconds())
        job.cancel()

        // Then
        assert that result equals listOf(

            // First
            mapOf(
                SomeEmailAddress to Credential.FullyLoggedIn
            ),

            // Second
            mapOf(
                SomeEmailAddress to Credential.FullyLoggedIn,
                AnotherEmailAddress to Credential.MailboxPasswordRequired
            ),

            // Third
            mapOf(
                SomeEmailAddress to Credential.FullyLoggedIn
            ),

            // Forth
            mapOf(
                SomeEmailAddress to Credential.FullyLoggedIn,
                JustAnEmailAddress to Credential.LoggedOut
            )
        )
    }

    @Test
    fun `does publish updates correctly for a single credentials`() = coroutinesTest {

        val result = mutableListOf<Credential>()
        val interval = 30.toDuration(DurationUnit.SECONDS)
        val flow = getSavedCredentials(SomeEmailAddress, interval - 5.toDuration(DurationUnit.SECONDS))
        val job = launch {
            delay(10)
            flow.toList(result)
        }

        // Given
        repo[SomeEmailAddress] = Credential.FullyLoggedIn

        // When
        advanceTimeBy(interval.toLongMilliseconds())
        repo -= SomeEmailAddress

        advanceTimeBy(interval.toLongMilliseconds())
        repo[SomeEmailAddress] = Credential.MailboxPasswordRequired

        advanceTimeBy(interval.toLongMilliseconds())
        repo[SomeEmailAddress] = Credential.FullyLoggedIn

        advanceTimeBy(interval.toLongMilliseconds())
        job.cancel()

        // Then
        assert that result equals listOf(

            // First
            Credential.FullyLoggedIn,

            // Second
            Credential.NotFound,

            // Third
            Credential.MailboxPasswordRequired,

            // Forth
            Credential.FullyLoggedIn
        )
    }

    @Test
    fun `does not publish multiple times the same value for all the credentials`() = coroutinesTest {

        val result = mutableListOf<Map<EmailAddress, Credential>>()
        val flow = getSavedCredentials(30.toDuration(DurationUnit.SECONDS))
        val job = launch {
            delay(10L)
            flow.toList(result)
        }

        // Given
        repo[SomeEmailAddress] = Credential.FullyLoggedIn
        repo[AnotherEmailAddress] = Credential.MailboxPasswordRequired

        // When
        advanceTimeBy(2.toDuration(DurationUnit.MINUTES).toLongMilliseconds())
        job.cancel()

        // Then
        assert that result equals listOf(
            mapOf(
                SomeEmailAddress to Credential.FullyLoggedIn,
                AnotherEmailAddress to Credential.MailboxPasswordRequired
            )
        )
    }

    @Test
    fun `does not publish multiple times the same value for a single credentials`() = coroutinesTest {

        val result = mutableListOf<Credential>()
        val flow = getSavedCredentials(SomeEmailAddress, 30.toDuration(DurationUnit.SECONDS))
        val job = launch {
            delay(10L)
            flow.toList(result)
        }

        // Given
        repo[SomeEmailAddress] = Credential.FullyLoggedIn

        // When
        advanceTimeBy(2.toDuration(DurationUnit.MINUTES).toLongMilliseconds())
        job.cancel()

        // Then
        assert that result equals listOf(
            Credential.FullyLoggedIn
        )
    }

}

