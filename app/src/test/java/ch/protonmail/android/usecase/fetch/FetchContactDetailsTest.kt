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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.api.models.room.contacts.FullContactDetails
import ch.protonmail.android.api.models.room.contacts.server.FullContactDetailsResponse
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.usecase.model.FetchContactDetailsResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Rule
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchContactDetailsTest : CoroutinesTest {

    @get:Rule
    val archRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var contactsDao: ContactsDao

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var api: ProtonMailApiManager

    @InjectMockKs
    private lateinit var useCase: FetchContactDetails

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun verifyThatExistingContactsDataIsFetchedFromTheDbAndPassedInLiveDataAndThenApiReturnsData() =
        runBlockingTest {
            // given
            val contactId = "testContactId"
            val testDataDb = "testDataDb"
            val testDataNet = "testDataNet"
            every { userManager.username } returns "testUserName"
            every { userManager.openPgp } returns mockk(relaxed = true)
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
            every { contactsDao.findFullContactDetailsById(contactId) } returns fullContactsFromDb
            every { contactsDao.insertFullContactDetails(any()) } returns Unit
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
            every { userManager.username } returns "testUserName"
            every { userManager.openPgp } returns mockk(relaxed = true)
            val contactEncryptedData = mockk<ContactEncryptedData> {
                every { type } returns 2
                every { data } returns testData
                every { signature } returns testSignature
            }
            val fullContactsFromDb = mockk<FullContactDetails> {
                every { encryptedData } returns mutableListOf(contactEncryptedData)
            }
            every { contactsDao.findFullContactDetailsById(contactId) } returns fullContactsFromDb
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
            every { userManager.username } returns "testUserName"
            every { userManager.openPgp } returns mockk(relaxed = true)

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
            every { contactsDao.findFullContactDetailsById(contactId) } returns fullContactsFromDb
            every { contactsDao.insertFullContactDetails(any()) } returns Unit
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
            every { contactsDao.findFullContactDetailsById(contactId) } returns null
            coEvery { api.fetchContactDetails(contactId) } throws exception
            val expected = FetchContactDetailsResult.Error(exception)

            // when
            val observer = useCase.invoke(contactId).testObserver()

            // then
            assertEquals(expected, observer.observedValues[0])
        }
}
