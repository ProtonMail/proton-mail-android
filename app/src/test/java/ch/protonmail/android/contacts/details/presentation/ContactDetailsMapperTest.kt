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

package ch.protonmail.android.contacts.details.presentation

import android.graphics.Color
import ch.protonmail.android.contacts.details.domain.model.FetchContactDetailsResult
import ch.protonmail.android.contacts.details.domain.model.FetchContactGroupsResult
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsUiItem
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsViewState
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContactDetailsMapperTest {

    val mapper = ContactDetailsMapper()

    private val contactId1 = "contactUid1"
    private val contactName1 = "testContactName"
    private val vCardToShare1 = "testCardType2"
    private val decryptedCardType0 = "decryptedCardType0"
    private val fetchContactResult = FetchContactDetailsResult(
        contactId1,
        contactName1,
        emails = emptyList(),
        telephoneNumbers = emptyList(),
        addresses = emptyList(),
        photos = emptyList(),
        organizations = emptyList(),
        titles = emptyList(),
        nicknames = emptyList(),
        birthdays = emptyList(),
        anniversaries = emptyList(),
        roles = emptyList(),
        urls = emptyList(),
        vCardToShare = vCardToShare1,
        gender = null,
        notes = emptyList(),
        isType2SignatureValid = true,
        isType3SignatureValid = null,
        vDecryptedCardType0 = decryptedCardType0,
        vDecryptedCardType2 = null,
        vDecryptedCardType3 = null,
    )

    private val groupId1 = LabelId("ID1")
    private val groupName1 = "name1"
    private val userId = UserId("testUserId")
    private val contactLabel =
        LabelEntity(
            groupId1, userId, groupName1, "color", 1, LabelType.MESSAGE_LABEL, "a/b", "parentId", 0, 0, 0
        )
    private val fetchContactGroupResult = FetchContactGroupsResult(
        listOf(contactLabel)
    )

    private val testColorInt = 321

    @BeforeTest
    fun setUp() {
        mockkStatic(Color::class)
        every { Color.parseColor(any()) } returns testColorInt
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun verifyBasicMappingWithDefaultValues() {

        // given
        val expected = ContactDetailsViewState.Data(
            contactId1,
            contactName1,
            "T",
            listOf(
                ContactDetailsUiItem.Group(
                    groupId1,
                    groupName1,
                    testColorInt,
                    0
                )
            ),
            vCardToShare1,
            null,
            null,
            true,
            null,
            decryptedCardType0,
            null,
            null
        )

        // when
        val result = mapper.mapToContactViewData(fetchContactResult, fetchContactGroupResult)

        // then
        assertEquals(expected, result)
    }
}
