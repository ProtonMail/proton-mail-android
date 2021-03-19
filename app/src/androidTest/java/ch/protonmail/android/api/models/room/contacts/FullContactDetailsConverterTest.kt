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
package ch.protonmail.android.api.models.room.contacts

import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.ContactEncryptedDataMatcher
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.FullContactDetailsConverter
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.contains
import org.junit.Assert
import kotlin.test.Test

internal class FullContactDetailsConverterTest {

    private val converter = FullContactDetailsConverter()

    @Test
    fun encryptDecryptTest() {
        val data = listOf(
            ContactEncryptedData("data", "signature", Constants.VCardType.SIGNED),
            ContactEncryptedData("data2", "signature2", Constants.VCardType.SIGNED_ENCRYPTED)
        )
        val expected = data.map {
            `is`(ContactEncryptedDataMatcher(it))
        }
        val encoded = converter.contactEncryptedDataListToString(data)
        val actual = converter.stringToContactEncryptedDataList(encoded)
        Assert.assertThat(actual, contains(expected))
    }

}
