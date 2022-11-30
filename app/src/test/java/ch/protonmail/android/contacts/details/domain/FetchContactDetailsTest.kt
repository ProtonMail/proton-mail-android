/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.contacts.details.domain

import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.contacts.details.domain.model.FetchContactDetailsResult
import ch.protonmail.android.data.local.model.FullContactDetails
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchContactDetailsTest : ArchTest by ArchTest(), CoroutinesTest by CoroutinesTest() {

    private val repository: ContactDetailsRepository = mockk()

    val mapper: FetchContactsMapper = mockk()

    private val useCase = FetchContactDetails(
        repository, mapper
    )

    @Test
    fun verifyThatExistingContactsDataIsFetchedFromTheRepositoryTwiceAsFromDbAndNetAndIsMappedAndReturned() =
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
            coEvery { repository.observeFullContactDetails(testContactId) } returns
                flowOf(fullContactsFromDb, fullContactsFromNet)

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
}
