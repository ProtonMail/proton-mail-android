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

package ch.protonmail.android.contacts.details.domain

import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.contacts.details.domain.model.FetchContactDetailsResult
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
import ch.protonmail.android.domain.entity.Id
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

class FetchContactsMapperTest {

    private val testUserId = Id("id")
    private val userManager: UserManager = mockk {
        every { currentUserId } returns testUserId
        every { requireCurrentUserId() } returns testUserId
        every { openPgp } returns mockk(relaxed = true)
    }

    val mapper = FetchContactsMapper(userManager, mockk())


    @Test
    fun verifyThatExistingContactsDataIsFetchedFromTheDbAndPassedInFlowAndThenApiReturnsData() =
        runBlockingTest {
            // given
            val contactId = "testContactId"
            val testDataDb = "testDataDb"
            val testDataNet = "testDataNet"
            val contactEncryptedDataDb = mockk<ContactEncryptedData> {
                every { type } returns 0
                every { data } returns testDataDb
            }
            val contactEncryptedDataNet = mockk<ContactEncryptedData> {
                every { type } returns 0
                every { data } returns testDataNet
            }
            val fullContactsFromDb = mockk<FullContactDetails> {
                every { encryptedData } returns mutableListOf(contactEncryptedDataDb)
            }
            val fullContactsFromNet = mockk<FullContactDetails> {
                every { encryptedData } returns mutableListOf(contactEncryptedDataNet)
            }
            val fullContactsResponse = mockk<FullContactDetailsResponse> {
                every { contact } returns fullContactsFromNet
            }
            val expectedDb = FetchContactDetailsResult.Data(
                decryptedVCardType0 = testDataDb
            )
            val expectedNet = FetchContactDetailsResult.Data(
                decryptedVCardType0 = testDataNet
            )

            // when
            val result = mapper.mapEncryptedDataToResult(mutableListOf(contactEncryptedDataDb))

            // then
            assertEquals(expectedDb, result[0])
            assertEquals(expectedNet, result[1])
        }

}
