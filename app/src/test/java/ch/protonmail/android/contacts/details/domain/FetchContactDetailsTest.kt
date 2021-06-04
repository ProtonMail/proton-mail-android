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

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.contacts.details.domain.model.FetchContactDetailsResult
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
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

    private val api: ProtonMailApiManager = mockk()

    val mapper: FetchContactsMapper = mockk()

    private val useCase = FetchContactDetails(
        repository, api, mapper, dispatchers,
    )

    @Test
    fun verifyThatExistingContactsDataIsFetchedFromTheDbAndPassedInFlowAndThenApiReturnsData() =
        runBlockingTest {
            // given
            val testContactId = "testContactId"
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
            val listOfDbData = mutableListOf(contactEncryptedDataDb)
            val fullContactsFromDb = mockk<FullContactDetails> {
                every { encryptedData } returns listOfDbData
                every { contactId } returns testContactId
            }
            val listOfNetData = mutableListOf(contactEncryptedDataNet)
            val fullContactsFromNet = mockk<FullContactDetails> {
                every { encryptedData } returns listOfNetData
                every { contactId } returns testContactId
            }
            val fullContactsResponse = mockk<FullContactDetailsResponse> {
                every { contact } returns fullContactsFromNet
            }
            coEvery { repository.getFullContactDetails(testContactId) } returns fullContactsFromDb
            coEvery { repository.insertFullContactDetails(any()) } returns Unit
            coEvery { api.fetchContactDetails(testContactId) } returns fullContactsResponse

            val expectedDb = mockk<FetchContactDetailsResult>()
            every { mapper.mapEncryptedDataToResult(listOfDbData, testContactId) } returns expectedDb
            val expectedNet = mockk<FetchContactDetailsResult>()
            every { mapper.mapEncryptedDataToResult(listOfNetData, testContactId) } returns expectedNet

            // when
            val result = useCase.invoke(testContactId).take(2).toList()

            // then
            assertEquals(expectedDb, result[0])
            assertEquals(expectedNet, result[1])
        }

    @Test
    fun verifyThatWhenThereIsNotContactsDataForGivenContactInTheDbOnlyApiDataWillBeReturned() =
        runBlockingTest {
            // given
            val testContactId = "testContactId"
            val testSignature = "testSignature"
            val testDataNet = "testDataNet"

            val contactEncryptedDataNet = mockk<ContactEncryptedData> {
                every { type } returns 2
                every { data } returns testDataNet
                every { signature } returns testSignature
            }
            val listOfNetData = mutableListOf(contactEncryptedDataNet)
            val fullContactsFromNet = mockk<FullContactDetails> {
                every { encryptedData } returns listOfNetData
                every { contactId } returns testContactId
            }
            val fullContactsResponse = mockk<FullContactDetailsResponse> {
                every { contact } returns fullContactsFromNet
            }
            coEvery { repository.getFullContactDetails(testContactId) } returns null
            coEvery { repository.insertFullContactDetails(any()) } returns Unit
            coEvery { api.fetchContactDetails(testContactId) } returns fullContactsResponse
            val expected = mockk<FetchContactDetailsResult>()
            every { mapper.mapEncryptedDataToResult(listOfNetData, testContactId) } returns expected

            // when
            val result = useCase.invoke(testContactId).take(1).toList()

            // then
            assertEquals(expected, result[0])
        }

    @Test(expected = IOException::class)
    fun verifyThatWhenThereIsNotContactsDataForGivenContactInTheDbAndThereWasApiErrorExceptionWillBeThrown() =
        runBlockingTest {
            // given
            val contactId = "testContactId"

            coEvery { repository.getFullContactDetails(contactId) } returns null
            coEvery { repository.insertFullContactDetails(any()) } returns Unit
            val ioException = IOException("Cannot load contacts")
            coEvery { api.fetchContactDetails(contactId) } throws ioException

            // when
            useCase.invoke(contactId).take(1).toList()

            // then expect IOException
        }
}
