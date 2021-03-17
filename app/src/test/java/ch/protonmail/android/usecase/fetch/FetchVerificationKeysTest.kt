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
import ch.protonmail.android.api.models.PublicKeyBody
import ch.protonmail.android.api.models.PublicKeyResponse
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.domain.entity.user.AddressKeys
import ch.protonmail.android.utils.crypto.KeyInformation
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FetchVerificationKeysTest : CoroutinesTest {

    @MockK
    private lateinit var api: ProtonMailApiManager

    @MockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var userCrypto: UserCrypto

    @MockK
    private lateinit var contactDao: ContactDao

    lateinit var useCase: FetchVerificationKeys

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        val testUser = mockk<User> {
            every { id } returns "id"
            every { toNewUser() } returns mockk()
            every { toNewUser().addresses } returns mockk()
            val testAddresses = listOf(mockk<Address>())
            val primaryKey = mockk<AddressKey> {
                every { privateKey } returns PgpField.PrivateKey(NotBlankString("privateKey"))
                every { buildBackEndFlags() } returns 0
            }
            val addressKeys = AddressKeys(
                primaryKey = primaryKey,
                keys = listOf(primaryKey)
            )
            val newAddresses = mockk<ch.protonmail.android.domain.entity.user.Address> {
                every { email } returns EmailAddress("testemail@asd.com")
                every { keys } returns addressKeys
            }
            val newTestAddresses = listOf(newAddresses)
            every { toNewUser().addresses.addresses } returns mapOf(1 to newAddresses)
            every { toNewUser().addresses.sorted() } returns newTestAddresses
            every { getAddresses() } returns CopyOnWriteArrayList(testAddresses)
        }
        every { userManager.user } returns testUser
        every { userManager.currentUserId } returns Id("id")
        every { userManager.openPgp } returns mockk()
        useCase = FetchVerificationKeys(api, userManager, userCrypto, contactDao, TestDispatcherProvider)
    }

    @Test
    fun verifyThatContactsAreFetchedCorrectlyFromRemoteApi() = runBlockingTest {
        // given
        val testEmail = "testEmail"
        val testContactId = "contactId"
        val testContactEmail = mockk<ContactEmail> {
            every { contactId } returns testContactId
        }
        every { contactDao.findContactEmailByEmail(testEmail) } returns testContactEmail
        every { contactDao.insertFullContactDetails(any()) } returns Unit
        val fullContactDetailsResponse = mockk<FullContactDetailsResponse> {
            every { contact } returns mockk {
                every { contactId } returns "contactId"
                every { name } returns "name"
                every { uid } returns "uid"
                every { createTime } returns 0
                every { modifyTime } returns 0
                every { size } returns 1
                every { defaults } returns 1
                every { encryptedData } returns mutableListOf()
                every { emails } returns listOf(testContactEmail)
                every { getPublicKeys(any(), any()) } returns listOf("testKey")
            }
        }
        coEvery { api.fetchContactDetails(testContactId) } returns fullContactDetailsResponse
        val publicKeyResponse = mockk<PublicKeyResponse> {
            every { recipientType } returns PublicKeyResponse.RecipientType.INTERNAL
            every { mimeType } returns "testMimeType"
            every { keys } returns arrayOf(PublicKeyBody(0, "pubKey"))
            every { code } returns Constants.RESPONSE_CODE_OK
            every { hasError() } returns false
        }
        coEvery { api.getPublicKeys(testEmail) } returns publicKeyResponse
        val expected =
            listOf(
                KeyInformation(
                    null,
                    null,
                    false,
                    null,
                    false
                )
            )

        // when
        val result = useCase.invoke(testEmail)

        // then
        assertNotNull(result)
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatContactsAreDerivedLocally() = runBlockingTest {
        // given
        val testEmail = "testemail@asd.com"
        val testContactId = "contactId"
        val testContactEmail = mockk<ContactEmail> {
            every { contactId } returns testContactId
        }
        every { contactDao.findContactEmailByEmail(testEmail) } returns testContactEmail
        every { contactDao.insertFullContactDetails(any()) } returns Unit
        val fullContactDetailsResponse = mockk<FullContactDetailsResponse> {
            every { contact } returns mockk {
                every { contactId } returns "contactId"
                every { name } returns "name"
                every { uid } returns "uid"
                every { createTime } returns 0
                every { modifyTime } returns 0
                every { size } returns 1
                every { defaults } returns 1
                every { encryptedData } returns mutableListOf()
                every { emails } returns listOf(testContactEmail)
                every { getPublicKeys(any(), any()) } returns listOf("testKey")
            }
        }
        coEvery { api.fetchContactDetails(testContactId) } returns fullContactDetailsResponse
        val publicKeyResponse = mockk<PublicKeyResponse> {
            every { recipientType } returns PublicKeyResponse.RecipientType.INTERNAL
            every { mimeType } returns "testMimeType"
            every { keys } returns arrayOf(PublicKeyBody(0, "pubKey"))
            every { code } returns Constants.RESPONSE_CODE_OK
            every { hasError() } returns false
        }
        coEvery { api.getPublicKeys(testEmail) } returns publicKeyResponse
        val keyInformation = KeyInformation(
            null,
            null,
            false,
            null,
            true
        )
        val expected = listOf(keyInformation)
        every { userCrypto.deriveKeyInfo(any()) } returns keyInformation

        // when
        val result = useCase.invoke(testEmail)

        // then
        assertNotNull(result)
        assertEquals(expected, result)
    }
}
