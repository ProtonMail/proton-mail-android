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

package ch.protonmail.android.usecase.fetch

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.contacts.details.ContactDetailsRepository
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.usecase.model.FetchContactDetailsResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchContactDetailsTest : ArchTest, CoroutinesTest {

    private val repository: ContactDetailsRepository = mockk()

    private val testUserId = Id("id")

    private val userManager: UserManager = mockk {
        every { currentUserId } returns testUserId
        every { requireCurrentUserId() } returns testUserId
        every { openPgp } returns mockk(relaxed = true)
    }

    private val api: ProtonMailApiManager = mockk()

    private val useCase = FetchContactDetails(
        repository, userManager, api, mockk(), dispatchers
    )

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
            coEvery { repository.getFullContactDetails(contactId) } returns fullContactsFromDb
            every { repository.insertFullContactDetails(any()) } returns Unit
            coEvery { api.fetchContactDetails(contactId) } returns fullContactsResponse
            val expectedDb = FetchContactDetailsResult.Data(
                decryptedVCardType0 = testDataDb
            )
            val expectedNet = FetchContactDetailsResult.Data(
                decryptedVCardType0 = testDataNet
            )

            // when
            val result = useCase.invoke(contactId).take(2).toList()

            // then
            assertEquals(expectedDb, result[0])
            assertEquals(expectedNet, result[1])
        }

    @Test
    fun verifyThatExistingContactsDataIsFetchedFromTheDbAndPassedInFlowAndThenNetworkErrorIsEmitted() =
        runBlockingTest {
            // given
            val contactId = "testContactId"
            val testData = "testData"
            val testSignature = "testSignature"
            val contactEncryptedData = mockk<ContactEncryptedData> {
                every { type } returns 2
                every { data } returns testData
                every { signature } returns testSignature
            }
            val fullContactsFromDb = mockk<FullContactDetails> {
                every { encryptedData } returns mutableListOf(contactEncryptedData)
            }
            coEvery { repository.getFullContactDetails(contactId) } returns fullContactsFromDb
            val ioException = IOException("Cannot load contacts")
            coEvery { api.fetchContactDetails(contactId) } throws ioException
            val expected = FetchContactDetailsResult.Data(
                decryptedVCardType2 = testData,
                vCardType2Signature = testSignature
            )
            val expectedNetError = FetchContactDetailsResult.Error(ioException)

            // when
            val result = useCase.invoke(contactId).take(2).toList()

            // then
            assertEquals(expected, result[0])
            assertEquals(expectedNetError, result[1])
        }

    @Test
    fun verifyThatWhenThereIsNotContactsDataForGivenContactInTheDbOnlyApiDataWillBeReturned() =
        runBlockingTest {
            // given
            val contactId = "testContactId"
            val testSignature = "testSignature"
            val testDataNet = "testDataNet"

            val contactEncryptedDataNet = mockk<ContactEncryptedData> {
                every { type } returns 2
                every { data } returns testDataNet
                every { signature } returns testSignature
            }
            val fullContactsFromNet = mockk<FullContactDetails> {
                every { encryptedData } returns mutableListOf(contactEncryptedDataNet)
            }
            val fullContactsResponse = mockk<FullContactDetailsResponse> {
                every { contact } returns fullContactsFromNet
            }
            coEvery { repository.getFullContactDetails(contactId) } returns null
            every { repository.insertFullContactDetails(any()) } returns Unit
            coEvery { api.fetchContactDetails(contactId) } returns fullContactsResponse
            val expected = FetchContactDetailsResult.Data(
                decryptedVCardType2 = testDataNet,
                vCardType2Signature = testSignature
            )

            // when
            val result = useCase.invoke(contactId).take(1).toList()

            // then
            assertEquals(expected, result[0])
        }

    @Test
    fun verifyThatWhenThereIsNotContactsDataForGivenContactInTheDbAndThereWasApiErrorAnErrorWillBeReturned() =
        runBlockingTest {
            // given
            val contactId = "testContactId"

            coEvery { repository.getFullContactDetails(contactId) } returns null
            every { repository.insertFullContactDetails(any()) } returns Unit
            val ioException = IOException("Cannot load contacts")
            coEvery { api.fetchContactDetails(contactId) } throws ioException
            val expectedNetError = FetchContactDetailsResult.Error(ioException)

            // when
            val result = useCase.invoke(contactId).take(1).toList()

            // then
            assertEquals(expectedNetError, result[0])
        }

    @Test
    fun verifyThatWhenExceptionIsThrownErrorDataWillBeReturned() =
        runBlockingTest {
            // given
            val contactId = "testContactId"
            val exception = Exception("An error!")
            coEvery { repository.getFullContactDetails(contactId) } returns null
            coEvery { api.fetchContactDetails(contactId) } throws exception
            val expected = FetchContactDetailsResult.Error(exception)

            // when
            val result = useCase.invoke(contactId).take(1).toList()

            // then
            assertEquals(expected, result[0])
        }
}
