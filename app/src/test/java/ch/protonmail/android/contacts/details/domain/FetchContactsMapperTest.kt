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
import ch.protonmail.android.contacts.details.domain.model.FetchContactDetailsResult
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.UserCrypto
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.utils.crypto.TextVerificationResult
import ezvcard.parameter.VCardParameters
import ezvcard.property.Address
import ezvcard.property.Birthday
import ezvcard.property.Email
import ezvcard.property.Note
import ezvcard.property.Photo
import ezvcard.property.Telephone
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.text.SimpleDateFormat
import kotlin.test.Ignore
import kotlin.test.assertEquals

class FetchContactsMapperTest {

    private val testUserId = UserId("id")
    private val userManager: UserManager = mockk {
        every { currentUserId } returns testUserId
        every { requireCurrentUserId() } returns testUserId
        every { openPgp } returns mockk(relaxed = true)
        every { getUserBlocking(any()) } returns mockk()
    }

    private val crypto: UserCrypto = mockk()

    val mapper = FetchContactsMapper(userManager, mockk(), crypto)

    private val testCardTypeO = "BEGIN:VCARD\r\n" +
        "VERSION:4.0\r\n" +
        "PRODID:-//ProtonMail//ProtonMail vCard 1.0.0//EN\r\n" +
        "FN:jant.rosales\r\n" +
        "UID:proton-autosave-15713e29-80f7-44e3-8537-2ebabe4c7955\r\n" +
        "TEL;TYPE=voice:054644651212\r\n" +
        "ADR;TYPE=adr:;;Rainstreet 1\r\n" +
        "item1.EMAIL;PREF=1:jant.rosales@gmail.com\r\n" +
        "END:VCARD\r\n"

    private val testCardType2 = "BEGIN:VCARD\r\n" +
        "VERSION:4.0\r\n" +
        "FN:Tomek9\r\n" +
        "item1.EMAIL;TYPE=email:tomek9@abc.com\r\n" +
        "item1.X-PM-ENCRYPT:false\r\n" +
        "item1.X-PM-SIGN:false\r\n" +
        "UID:proton-web-3d6b22b5-04ce-4300-605a-3bfd2d3652d1\r\n" +
        "PHOTO:https://miro.medium.com/fit/c/96/96/1*6lE2RWE0pVCnr52yc6o6rw.jpeg\r\n" +
        "NOTE:Gender: Male\r\n" +
        "BDAY:19810404\r\n" +
        "END:VCARD\r\n"

    private val type2Signature = "-----BEGIN PGP SIGNATURE-----\r\n" +
        "Version: ProtonMail\r\n" +
        "type2Signature\r\n" +
        "-----END PGP SIGNATURE-----\r\n"

    private val testCardType3 = """BEGIN:VCARD
                VERSION:4.0
                ITEM3.X-ABDATE;TYPE=x-_${'$'}!<Anniversary>!$\_:20151107094
                ITEM1.ORG:N/A
                PHOTO:https://miro.medium.com/fit/c/96/96/1*6lE2RWE0pVCnr52yc6o6rw.jpeg
                NOTE:Gender: Male
                BDAY:19810404
                N:LastName;FirstName;;;
                END:VCARD"""

    val type3Signature = "-----BEGIN PGP SIGNATURE-----\r\n" +
        "Version: ProtonMail\r\n" +
        "type3Signature\r\n" +
        "-----END PGP SIGNATURE-----\r\n"

    @Test
    fun verifyThatBasicUnsignedVcardDataIsMappedProperly() =
        runBlockingTest {
            // given
            val testContactName = "jant.rosales"
            val testContactId =
                "4Grx7G3QwDCMLuPZEXcIUc8fpb7kqnuJei-dcqZhUKZGndYJnnrslj-Sb58OG0FVKsvODhMNxo910cLVDhUrOw=="

            val contactEncryptedDataDb = mockk<ContactEncryptedData> {
                every { type } returns Constants.VCardType.UNSIGNED.vCardTypeValue
                every { data } returns testCardTypeO
            }

            val email1 = Email(
                "jant.rosales@gmail.com"
            ).apply {
                group = "item1"
                parameters = VCardParameters(mapOf("PREF" to listOf("1")))
            }
            val phone1 = Telephone("054644651212").apply {
                parameters = VCardParameters(mapOf("TYPE" to listOf("voice")))
            }
            val address1 = Address().apply {
                streetAddress = "Rainstreet 1"
                parameters = VCardParameters(mapOf("TYPE" to listOf("adr")))
            }
            val expectedDb = FetchContactDetailsResult(
                testContactId,
                testContactName,
                emails = listOf(email1),
                telephoneNumbers = listOf(phone1),
                addresses = listOf(address1),
                photos = emptyList(),
                organizations = emptyList(),
                titles = emptyList(),
                nicknames = emptyList(),
                birthdays = emptyList(),
                anniversaries = emptyList(),
                roles = emptyList(),
                urls = emptyList(),
                vCardToShare = testCardTypeO,
                gender = null,
                notes = emptyList(),
                isType2SignatureValid = null,
                isType3SignatureValid = null,
                vDecryptedCardType0 = testCardTypeO,
                vDecryptedCardType1 = "",
                vDecryptedCardType2 = "",
                vDecryptedCardType3 = ""
            )

            // when
            val result = mapper.mapEncryptedDataToResult(mutableListOf(contactEncryptedDataDb), testContactId)

            // then
            assertEquals(expectedDb, result)
        }

    @Test
    fun verifyThatBasicSignedVcardDataIsMappedProperly() =
        runBlockingTest {
            // given
            val testContactName = "Tomek9"
            val testContactId =
                "4Grx7G3QwDCMLuPZEXcIUc8fpb7kqnuJei-dcqZhUKZGndYJnnrslj-Sb58OG0FVKsvODhMNxo910cLVDhUrOw=="

            val contactEncryptedDataDb = mockk<ContactEncryptedData> {
                every { type } returns Constants.VCardType.SIGNED.vCardTypeValue
                every { data } returns testCardType2
                every { signature } returns type2Signature
            }
            every { crypto.verify(testCardType2, type2Signature) } returns TextVerificationResult(
                testCardType2, true, 0
            )

            val email1 = Email(
                "tomek9@abc.com"
            ).apply {
                group = "item1"
                parameters = VCardParameters(mapOf("TYPE" to listOf("email")))
            }

            val photo1 = Photo("https://miro.medium.com/fit/c/96/96/1*6lE2RWE0pVCnr52yc6o6rw.jpeg", null)
            val simpleDate = SimpleDateFormat("yyyyMMdd")
            val date = simpleDate.parse("19810404")
            val birthday1 = Birthday(date)
            val note1 = Note("Gender: Male")
            val expectedDb = FetchContactDetailsResult(
                testContactId,
                testContactName,
                emails = listOf(email1),
                telephoneNumbers = emptyList(),
                addresses = emptyList(),
                photos = listOf(photo1),
                organizations = emptyList(),
                titles = emptyList(),
                nicknames = emptyList(),
                birthdays = listOf(birthday1),
                anniversaries = emptyList(),
                roles = emptyList(),
                urls = emptyList(),
                vCardToShare = testCardType2,
                gender = null,
                notes = listOf(note1),
                isType2SignatureValid = true,
                isType3SignatureValid = null,
                vDecryptedCardType0 = "",
                vDecryptedCardType1 = "",
                vDecryptedCardType2 = testCardType2,
                vDecryptedCardType3 = ""
            )

            // when
            val result = mapper.mapEncryptedDataToResult(mutableListOf(contactEncryptedDataDb), testContactId)

            // then
            assertEquals(expectedDb, result)
        }

    @Ignore(
        "java.lang.UnsatisfiedLinkError: no gojni in java.library.path: error due to use of CipherText " +
            "is preventing this test from running, any ideas?"
    )
    @Test
    fun verifyThatEncryptedSignedVcardDataWithPhotoUrlIsMappedProperly() =
        runBlockingTest {
            // given
            val testContactName = "Tomek9"
            val testContactId =
                "4Grx7G3QwDCMLuPZEXcIUc8fpb7kqnuJei-dcqZhUKZGndYJnnrslj-Sb58OG0FVKsvODhMNxo910cLVDhUrOw=="

            val contactEncryptedDataDb = mockk<ContactEncryptedData> {
                every { type } returns Constants.VCardType.SIGNED_ENCRYPTED.vCardTypeValue
                every { data } returns testCardType3
                every { signature } returns type3Signature
            }
            every { crypto.verify(testCardType3, type3Signature) } returns
                TextVerificationResult(testCardType3, true, 0)

            val email1 = Email(
                "tomek9@abc.com"
            ).apply {
                group = "item1"
                parameters = VCardParameters(mapOf("TYPE" to listOf("email")))
            }
            val expectedDb = FetchContactDetailsResult(
                testContactId,
                testContactName,
                listOf(email1),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                testCardType2,
                null,
                emptyList(),
                true,
                null,
                vDecryptedCardType0 = testCardTypeO,
                vDecryptedCardType1 = null,
                vDecryptedCardType2 = null,
                vDecryptedCardType3 = null
            )

            // when
            val result = mapper.mapEncryptedDataToResult(mutableListOf(contactEncryptedDataDb), testContactId)

            // then
            assertEquals(expectedDb, result)
        }

}
