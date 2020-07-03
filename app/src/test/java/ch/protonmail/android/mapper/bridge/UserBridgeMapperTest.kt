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
package ch.protonmail.android.mapper.bridge

import assert4k.*
import ch.protonmail.android.domain.entity.user.Role
import io.mockk.every
import io.mockk.mockk
import me.proton.core.util.kotlin.invoke
import kotlin.test.Test
import ch.protonmail.android.api.models.User as OldUser

/**
 * Test suite for [UserBridgeMapper]
 */
internal class UserBridgeMapperTest {

    private val mapper = UserBridgeMapper()

    @Test
    fun `verify it transform correctly`() {

        // GIVEN
        val oldUser = mockk<OldUser>(relaxed = true) {
            every { name } returns "name"
            every { role } returns 1
        }

        // WHEN
        val newUser = mapper { oldUser.toNewModel() }

        // THEN
        assert that newUser * {
            +name.s equals "name"
            +role equals Role.ORGANIZATION_MEMBER
        }
    }
}
