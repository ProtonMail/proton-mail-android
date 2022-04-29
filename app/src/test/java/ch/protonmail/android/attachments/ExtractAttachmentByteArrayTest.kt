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

package ch.protonmail.android.attachments

import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.testdata.AttachmentTestData
import io.mockk.called
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertSame

internal class ExtractAttachmentByteArrayTest {

    private val addressCryptoMock = mockk<AddressCrypto>()
    private val attachmentsRepositoryMock = mockk<AttachmentsRepository>()
    private val extractAttachmentByteArray = ExtractAttachmentByteArray(
        attachmentsRepositoryMock
    )

    @Test
    fun `should return the in memory mime data when it is present`() = runBlockingTest {
        // given
        val expectedAttachmentBytes = AttachmentTestData.WITH_MIME_DATA.mimeData

        // when
        val actualAttachmentBytes = extractAttachmentByteArray(
            AttachmentTestData.WITH_MIME_DATA,
            addressCryptoMock
        )

        // then
        assertSame(expectedAttachmentBytes, actualAttachmentBytes)
        verify { addressCryptoMock wasNot called }
        verify { attachmentsRepositoryMock wasNot called }
    }

    @Test
    fun `should get attachment byte array through repository when in memory mime data not present`() = runBlockingTest {
        // given
        val expectedAttachmentBytes = ByteArray(42)
        coEvery {
            attachmentsRepositoryMock.getAttachmentDataOrNull(
                addressCryptoMock,
                AttachmentTestData.ID,
                AttachmentTestData.KEY_PACKETS
            )
        } returns expectedAttachmentBytes

        // when
        val actualAttachmentBytes = extractAttachmentByteArray(
            AttachmentTestData.WITHOUT_MIME_DATA,
            addressCryptoMock
        )

        // then
        assertSame(expectedAttachmentBytes, actualAttachmentBytes)
    }
}
