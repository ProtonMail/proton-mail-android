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
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.usecase.model.FetchContactDetailsResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchContactDetailsTest : ArchTest, CoroutinesTest {

    private val contactDao: ContactDao = mockk()

    private val testUserId = Id("id")

    private val userManager: UserManager = mockk {
        every { currentUserId } returns testUserId
        every { requireCurrentUserId() } returns testUserId
        every { openPgp } returns mockk(relaxed = true)
    }

    private val api: ProtonMailApiManager = mockk()

    private val useCase = FetchContactDetails(
        contactDao, userManager, api, mockk(), dispatchers
    )

    @Test
    fun verifyThatExistingContactsDataIsFetchedFromTheDbAndPassedInLiveDataAndThenApiReturnsData() =
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
            every { contactDao.findFullContactDetailsById(contactId) } returns fullContactsFromDb
            every { contactDao.insertFullContactDetails(any()) } returns Unit
            coEvery { api.fetchContactDetails(contactId) } returns fullContactsResponse
            val expectedDb = FetchContactDetailsResult.Data(
                decryptedVCardType0 = testDataDb
            )
            val expectedNet = FetchContactDetailsResult.Data(
                decryptedVCardType0 = testDataNet
            )

            // when
            val observer = useCase.invoke(contactId).testObserver()

            // then
            assertEquals(expectedDb, observer.observedValues[0])
            assertEquals(expectedNet, observer.observedValues[1])
        }

    @Test
    fun verifyThatExistingContactsDataIsFetchedFromTheDbAndPassedInLiveData() =
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
            every { contactDao.findFullContactDetailsById(contactId) } returns fullContactsFromDb
            val ioException = IOException("Cannot load contacts")
            coEvery { api.fetchContactDetails(contactId) } throws ioException
            val expected = FetchContactDetailsResult.Data(
                decryptedVCardType2 = testData,
                vCardType2Signature = testSignature
            )
            val expectedNetError = FetchContactDetailsResult.Error(ioException)

            // when
            val observer = useCase.invoke(contactId).testObserver()

            // then
            assertEquals(expected, observer.observedValues[0])
            assertEquals(expectedNetError, observer.observedValues[1])
        }

    @Test
    fun verifyThatWhenThereIsNotContactsDataForGivenContactInTheDbOnlyApiDataWillBeEmitted() =
        runBlockingTest {
            // given
            val contactId = "testContactId"
            val testSignature = "testSignature"
            val testDataNet = "testDataNet"

            val fullContactsFromDb = mockk<FullContactDetails> {
                every { encryptedData } returns mutableListOf()
            }
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
            every { contactDao.findFullContactDetailsById(contactId) } returns fullContactsFromDb
            every { contactDao.insertFullContactDetails(any()) } returns Unit
            coEvery { api.fetchContactDetails(contactId) } returns fullContactsResponse
            val expected = FetchContactDetailsResult.Data(
                decryptedVCardType2 = testDataNet,
                vCardType2Signature = testSignature
            )

            // when
            val observer = useCase.invoke(contactId).testObserver()

            // then
            assertEquals(expected, observer.observedValues[0])
        }

    @Test
    fun verifyThatWhenExceptionIsThrownErrorDataWillBeEmitted() =
        runBlockingTest {
            // given
            val contactId = "testContactId"
            val exception = Exception("An error!")
            every { contactDao.findFullContactDetailsById(contactId) } returns null
            coEvery { api.fetchContactDetails(contactId) } throws exception
            val expected = FetchContactDetailsResult.Error(exception)

            // when
            val observer = useCase.invoke(contactId).testObserver()

            // then
            assertEquals(expected, observer.observedValues[0])
        }
}
