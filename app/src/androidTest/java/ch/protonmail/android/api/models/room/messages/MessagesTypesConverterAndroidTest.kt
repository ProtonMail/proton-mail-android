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
package ch.protonmail.android.api.models.room.messages

import ch.protonmail.android.api.models.messages.ParsedHeaders
import ch.protonmail.android.data.local.model.MessagesTypesConverter
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert
import java.net.URLEncoder
import kotlin.test.Test

/**
 * Created by Kamil Rajtar on 18.07.18.  */
internal class MessagesTypesConverterAndroidTest {
    private val messagesTypesConverter = MessagesTypesConverter()

    @Test
    fun parsedHeadersSimple() {
        val expected = ParsedHeaders("${URLEncoder.encode("a@a.com")}=a", "${URLEncoder.encode("a@a.com")}=b")
        val parsedHeadersString = messagesTypesConverter.parsedHeadersToString(expected)
        val actual = messagesTypesConverter.stringToParsedHeaders(parsedHeadersString)
        Assert.assertThat(actual, `is`(ParsedHeadersMatcher(expected)))
    }
}
