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

package ch.protonmail.android.worker

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.core.Constants
import ch.protonmail.android.crypto.UserCrypto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class CreateContactWorkerTest {


    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var contactsDao: ContactsDao

    @RelaxedMockK
    private lateinit var gson: Gson

    @RelaxedMockK
    private lateinit var crypto: UserCrypto

    @InjectMockKs
    private lateinit var worker: CreateContactWorker

    private var dispatcherProvider = TestDispatcherProvider

    private val contactEmailsJson = """
                [{"selected":false,"pgpIcon":0,"pgpIconColor":0,"pgpDescription":0,"isPGP":false,"ID":"ID1","Email":"email@proton.com","Defaults":0,"Order":0},
                {"selected":false,"pgpIcon":0,"pgpIconColor":0,"pgpDescription":0,"isPGP":false,"ID":"ID2","Email":"secondary@proton.com","Defaults":0,"Order":0}]
        """.trimIndent()

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun workerFailsWhenRequestedContactDataDatabaseIdParameterIsNotPassed() {
        runBlockingTest {
            every { parameters.inputData.getLong(KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID, -1) } answers { -1 }

            val result = worker.doWork()

            assertEquals(Result.failure(), result)
        }
    }

    @Test
    fun workerFailsWhenRequestedContactEmailsSerialisedParameterIsNotPassed() {
        runBlockingTest {
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED) } answers { null }

            val result = worker.doWork()

            assertEquals(Result.failure(), result)
        }
    }

    @Test
    fun workerFailsWhenContactEmailsAreNotDeserializableFromJsonToListOfContactEmail() {
        runBlockingTest {
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED) } answers { "{ invalid json }" }

            val result = worker.doWork()

            assertEquals(Result.failure(), result)
        }
    }

    @Test
    fun workerReadsContactDataFromDbUsingIdPassedAsParameter() {
        runBlockingTest {
            val contactDataDbId = 123L
            every { parameters.inputData.getLong(KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID, -1) } answers { contactDataDbId }

            worker.doWork()

            verify { contactsDao.findContactDataByDbId(contactDataDbId) }
        }
    }

    @Test
    fun workerInvokesCreateContactEndpointWithEncryptedContactData() {
        runBlockingTest {
            givenParametersValidationSucceeds()
            val encryptedContactData = "encrypted-data"
            val signedContactData = "signed-data"
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_ENCRYPTED_DATA) } answers { encryptedContactData }
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_SIGNED_DATA) } answers { signedContactData }

            worker.doWork()

            val encryptedData = crypto.encrypt(encryptedContactData, false).armored
            val encryptDataSignature = crypto.sign(encryptedContactData)
            val signedDataSignature = crypto.sign(signedContactData)
            val contactEncryptedDataType2 = ContactEncryptedData(signedContactData, signedDataSignature, Constants.VCardType.SIGNED)
            val contactEncryptedDataType3 = ContactEncryptedData(encryptedData, encryptDataSignature, Constants.VCardType.SIGNED_ENCRYPTED)
            val contactEncryptedDataList = listOf(contactEncryptedDataType2, contactEncryptedDataType3)
            val createContactRequest = CreateContact(contactEncryptedDataList)
            coVerify { apiManager.createContact(createContactRequest) }
        }
    }

    private fun givenParametersValidationSucceeds() {
        val contactDataDbId = 123L
        val deserialisedContactEmails = listOf(
            ContactEmail("ID1", "email@proton.com", "Tom"),
            ContactEmail("ID2", "secondary@proton.com", "Mike")
        )
        val emailListType = TypeToken.getParameterized(
            List::class.java, ContactEmail::class.java
        ).type
        val contactData = ContactData("contactDataId", "name")
        every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED) } answers { contactEmailsJson }
        every { parameters.inputData.getLong(KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID, -1) } answers { contactDataDbId }
        every { gson.fromJson<List<ContactEmail>>(contactEmailsJson, emailListType) } answers { deserialisedContactEmails }
        every { contactsDao.findContactDataByDbId(contactDataDbId) } answers { contactData }
    }
}
