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

package ch.protonmail.android.api.segments.event

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.EventResponse
import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.usecase.keys.LogOutIfNotAllActiveKeysAreDecryptable
import ch.protonmail.android.utils.AppUtil
import io.mockk.Runs
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val USERNAME = "username"
private const val UPDATE_EVENT_TYPE = 2
private const val LEGACY_ADDRESS_EVENT_ID = "legacyAddressEventId"
private const val MIGRATED_ADDRESS_EVENT_ID = "migratedAddressEventId"

class EventHandlerTest {

    private val mockUser = mockk<User>(relaxUnitFun = true) {
        every { notificationSetting } returns 0
        every { addresses } returns CopyOnWriteArrayList()
    }

    private val userManager = mockk<UserManager>(relaxUnitFun = true) {
        every { getUser(USERNAME) } returns mockUser
    }

    private val messageDetailsRepository = mockk<MessageDetailsRepository>(relaxUnitFun = true)

    private val logOutIfNotAllActiveKeysAreDecryptable = mockk<LogOutIfNotAllActiveKeysAreDecryptable> {
        every { this@mockk() } returns true
    }

    private val eventHandler = EventHandler(
        mockk(),
        mockk(),
        userManager,
        messageDetailsRepository,
        mockk(),
        mockk(),
        mockk(),
        logOutIfNotAllActiveKeysAreDecryptable,
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        USERNAME
    )

    private val addressWithMigratedKey = buildAddress(
        MIGRATED_ADDRESS_EVENT_ID,
        buildKey(
            "Migrated Key Id",
            "valid token indicating a successfull migration",
            "valid signature indicating a successful migration"
        )
    )

    private val addressWithLegacyKey = buildAddress(
        LEGACY_ADDRESS_EVENT_ID,
        buildKey("Legacy Key Id", null, null)
    )

    @BeforeTest
    fun setUp() {
        mockkStatic(AppUtil::class)
        every { AppUtil.postEventOnUi(any()) } just Runs
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(AppUtil::class)
    }

    @Test
    fun whenReceivingAnAddressesUpdateEventWhereAnyOfTheAddressesKeysHaveNonNullSignatureAndTokenThenMarkTheAccountLocallyAsMigrated() {
        val legacyAddressEvent: EventResponse.AddressEventBody = mockk {
            every { address } returns addressWithLegacyKey
            every { type } returns UPDATE_EVENT_TYPE
            every { id } returns LEGACY_ADDRESS_EVENT_ID
        }
        val migratedAddressEvent: EventResponse.AddressEventBody = mockk {
            every { address } returns addressWithMigratedKey
            every { type } returns UPDATE_EVENT_TYPE
            every { id } returns MIGRATED_ADDRESS_EVENT_ID
        }
        val response = mockEventResponse(listOf(legacyAddressEvent, migratedAddressEvent))

        eventHandler.write(response)

        verify { mockUser.legacyAccount = false }
    }

    @Test
    fun whenReceivingAnAddressesUpdateEventWhereAllOfTheAddressesKeysHaveNullSignatureAndTokenThenMarkTheAccountLocallyAsLegacy() {
        val addressEventBody: EventResponse.AddressEventBody = mockk {
            every { address } returns addressWithLegacyKey
            every { type } returns UPDATE_EVENT_TYPE
            every { id } returns LEGACY_ADDRESS_EVENT_ID
        }
        val response = mockEventResponse(listOf(addressEventBody))

        eventHandler.write(response)

        verify { mockUser.legacyAccount = true }
    }

    @Test
    fun `should not verify keys when user updates and address in the response are null`() {
        // given
        val ignoredEventResponse = EventResponse()

        // when
        eventHandler.handleNewKeysIfNeeded(ignoredEventResponse)

        // then
        verify { logOutIfNotAllActiveKeysAreDecryptable wasNot called }
    }

    @Test
    fun `should verify keys when user updates in the response are not null`() {
        // given
        val ignoredEventResponse = spyk<EventResponse> {
            every { userUpdates } returns User()
        }

        // when
        eventHandler.handleNewKeysIfNeeded(ignoredEventResponse)

        // then
        verify { logOutIfNotAllActiveKeysAreDecryptable() }
    }

    @Test
    fun `should verify keys when address updates in the response are not null`() {
        // given
        val ignoredEventResponse = spyk<EventResponse> {
            every { addresses } returns listOf(AddressEventBody())
        }

        // when
        eventHandler.handleNewKeysIfNeeded(ignoredEventResponse)

        // then
        verify { logOutIfNotAllActiveKeysAreDecryptable() }
    }


    private fun mockEventResponse(listOf: List<EventResponse.AddressEventBody>) =
        mockk<EventResponse> {
            every { messageUpdates } returns null
            every { contactUpdates } returns null
            every { contactEmailsUpdates } returns null
            every { mailSettingsUpdates } returns null
            every { userSettingsUpdates } returns null
            every { labelUpdates } returns null
            every { messageCounts } returns null
            every { usedSpace } returns 0
            every { userUpdates } returns mockUser
            every { addresses } returns listOf
        }

    private fun buildKey(keyId: String, signature: String?, token: String?) = Keys(
        keyId,
        "private key value",
        0,
        0,
        signature,
        token,
        "",
        0
    )

    private fun buildAddress(addressId: String, key: Keys) = Address(
        addressId,
        "DomainId",
        "email@pm.me",
        0,
        0,
        0,
        UPDATE_EVENT_TYPE,
        0,
        "namee",
        "valid non empty signature",
        0,
        listOf(key)
    )
}
