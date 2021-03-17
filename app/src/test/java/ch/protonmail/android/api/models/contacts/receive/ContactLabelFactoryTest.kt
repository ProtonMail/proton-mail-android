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

package ch.protonmail.android.api.models.contacts.receive

import assert4k.assert
import assert4k.fails
import assert4k.that
import assert4k.with
import ch.protonmail.android.api.models.messages.receive.ServerLabel
import ch.protonmail.android.data.local.model.ContactLabel
import org.junit.Assert.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.Test

class ContactLabelFactoryTest {

    private lateinit var contactLabelFactory: ContactLabelFactory

    @BeforeTest
    fun setUp() {
        contactLabelFactory = ContactLabelFactory()
    }

    @Test
    fun `mapping ContactLabel to ServerLabel succeeds when all fields are valid`() {
        val contactLabel = ContactLabel("ID", "name", "color", 1, 0, false, 2)

        val actual = contactLabelFactory.createServerObjectFromDBObject(contactLabel)

        val expected = ServerLabel("ID", "name", "color", 1, 0, 0, 2)
        assertEquals(expected, actual)
    }

    @Test
    fun `mapping ContactLabel to ServerLabel succeeds when some fields are not passed explicitly`() {
        val contactLabel = ContactLabel("ID", "name", "color")

        val actual = contactLabelFactory.createServerObjectFromDBObject(contactLabel)

        val expected = ServerLabel("ID", "name", "color", 0, 0, 0, 2)
        assertEquals(expected, actual)
    }

    @Test
    fun `mapping ContactLabel to ServerLabel succeeds when ID is empty`() {
        val contactLabel = ContactLabel("", "name", "color", 1, 0, false, 2)

        val actual = contactLabelFactory.createServerObjectFromDBObject(contactLabel)

        val expected = ServerLabel("", "name", "color", 1, 0, 0, 2)
        assertEquals(expected, actual)
    }

    @Test
    fun `mapping ContactLabel to ServerLabel fails when name is empty`() {
        val contactLabel = ContactLabel("ID", "", "color", 1, 0, false, 2)

        assert that fails<IllegalArgumentException> {
            contactLabelFactory.createServerObjectFromDBObject(contactLabel)
        } with "name is empty"
    }

    @Test
    fun `mapping ContactLabel to ServerLabel fails when color is empty`() {
        val contactLabel = ContactLabel("ID", "name", "", 1, 0, false, 2)

        assert that fails<IllegalArgumentException> {
            contactLabelFactory.createServerObjectFromDBObject(contactLabel)
        } with "color is empty"
    }

    @Test
    fun `mapping ServerLabel to ContactLabel succeeds when all fields are valid`() {
        val serverLabel = ServerLabel("ID", "name", "color", 1, 0, 0, 2)

        val actual = contactLabelFactory.createDBObjectFromServerObject(serverLabel)

        val expected = ContactLabel("ID", "name", "color", 1, 0, false, 2)
        assertEquals(expected, actual)
    }

    @Test
    fun `mapping ServerLabel to ContactLabel fails when ID is empty`() {
        val serverLabel = ServerLabel("", "name", "color", 1, 0, 0, 2)

        assert that fails<IllegalArgumentException> {
            contactLabelFactory.createDBObjectFromServerObject(serverLabel)
        } with "id is empty"
    }

    @Test
    fun `mapping ServerLabel to ContactLabel fails when name is empty`() {
        val serverLabel = ServerLabel("ID", "", "color", 1, 0, 0, 2)

        assert that fails<IllegalArgumentException> {
            contactLabelFactory.createDBObjectFromServerObject(serverLabel)
        } with "name is empty"
    }

    @Test
    fun `mapping ServerLabel to ContactLabel fails when color is empty`() {
        val serverLabel = ServerLabel("ID", "name", "", 1, 0, 0, 2)

        assert that fails<IllegalArgumentException> {
            contactLabelFactory.createDBObjectFromServerObject(serverLabel)
        } with "color is empty"
    }

    @Test
    fun `mapping ServerLabel to ContactLabel fails when display is null`() {
        val serverLabel = ServerLabel("ID", "name", "color", null, 0, 0, 2)
        var thrownException: Exception? = null

        try {
            contactLabelFactory.createDBObjectFromServerObject(serverLabel)
        } catch (e: RuntimeException) {
            thrownException = e
        }

        assertEquals("display is null", thrownException?.message)
    }

    @Test
    fun `mapping ServerLabel to ContactLabel fails when order is null`() {
        val serverLabel = ServerLabel("ID", "name", "color", 0, null, 0, 2)
        var thrownException: Exception? = null

        try {
            contactLabelFactory.createDBObjectFromServerObject(serverLabel)
        } catch (e: RuntimeException) {
            thrownException = e
        }

        assertEquals("order is null", thrownException?.message)
    }

    @Test
    fun `mapping ServerLabel to ContactLabel fails when exclusive is null`() {
        val serverLabel = ServerLabel("ID", "name", "color", 0, 0, null, 2)
        var thrownException: Exception? = null

        try {
            contactLabelFactory.createDBObjectFromServerObject(serverLabel)
        } catch (e: RuntimeException) {
            thrownException = e
        }

        assertEquals("exclusive is null", thrownException?.message)
    }

    @Test
    fun `mapping ServerLabel to ContactLabel fails when type is null`() {
        val serverLabel = ServerLabel("ID", "name", "color", 0, 0, 0, null)
        var thrownException: Exception? = null

        try {
            contactLabelFactory.createDBObjectFromServerObject(serverLabel)
        } catch (e: RuntimeException) {
            thrownException = e
        }

        assertEquals("type is null", thrownException?.message)
    }
}
