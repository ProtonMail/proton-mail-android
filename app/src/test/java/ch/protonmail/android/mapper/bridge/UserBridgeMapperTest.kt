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

import android.os.SystemClock
import android.text.TextUtils
import assert4k.assert
import assert4k.contains
import assert4k.equals
import assert4k.that
import assert4k.times
import assert4k.unaryPlus
import ch.protonmail.android.core.Constants.Prefs.PREF_DELINQUENT
import ch.protonmail.android.core.Constants.Prefs.PREF_MAX_SPACE
import ch.protonmail.android.core.Constants.Prefs.PREF_MAX_UPLOAD_FILE_SIZE
import ch.protonmail.android.core.Constants.Prefs.PREF_ROLE
import ch.protonmail.android.core.Constants.Prefs.PREF_SUBSCRIBED
import ch.protonmail.android.core.Constants.Prefs.PREF_USED_SPACE
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_CREDIT
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_CURRENCY
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_ID
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_LEGACY_ACCOUNT
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_NAME
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_PRIVATE
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.domain.entity.user.Addresses
import ch.protonmail.android.domain.entity.user.Delinquent
import ch.protonmail.android.domain.entity.user.Plan
import ch.protonmail.android.domain.entity.user.Role
import ch.protonmail.android.domain.entity.user.UserKeys
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import me.proton.core.util.kotlin.invoke
import org.junit.Ignore
import kotlin.test.Test
import assert4k.invoke as fix
import ch.protonmail.android.api.models.Keys as LegacyKeys
import ch.protonmail.android.api.models.User as LegacyUser
import ch.protonmail.android.api.models.address.Address as LegacyAddress

/**
 * Test suite for [UserBridgeMapper]
 */
internal class UserBridgeMapperTest {

    private val mapper = UserBridgeMapper(
        mockk { every { any<Collection<LegacyAddress>>().toNewModel() } returns Addresses(emptyMap()) },
        mockk { every { any<Collection<LegacyKeys>>().toNewModel() } returns UserKeys.Empty }
    )

    @Test
    fun `transform from api`() {

        // GIVEN
        val legacyUser = mockk<LegacyUser>(relaxed = true) {
            every { id } returns "id"
            every { name } returns "name"
            every { services } returns 4
            every { subscribed } returns 4
            every { private } returns 1
            every { role } returns 2
            every { currency } returns "eur"
            every { credit } returns 10
            every { delinquentValue } returns 3
            every { maxUpload } returns 12_345
            every { usedSpace } returns 15_000
            every { maxSpace } returns 30_000
            every { legacyAccount } returns true
        }

        // WHEN
        val newUser = mapper { legacyUser.toNewModel() }

        // THEN
        assert that newUser * {
            +id.s equals "id"
            +name.s equals "name"
            +(plans * {
                +size.fix() equals 1
                it contains Plan.Vpn.Paid
            })
            +private equals true
            +role equals Role.ORGANIZATION_ADMIN
            +currency.s equals "eur"
            +credits.fix() equals 10
            +delinquent equals Delinquent.InvoiceDelinquent
            +totalUploadLimit.l equals 12_345uL
            +(dedicatedSpace * {
                +used.l.fix() equals 15_000uL
                +total.l.fix() equals 30_000uL
            })
            + isLegacy equals true
        }
    }

    @Test
    @Ignore("java.lang.UnsatisfiedLinkError: 'long android.os.SystemClock.elapsedRealtime()'")
    fun `transform from preferences`() {
        mockkStatic(ProtonMailApplication::class, TextUtils::class, SystemClock::class)
        every { ProtonMailApplication.getApplication() } returns mockk {
            every { getString(any()) } returns ""
            every { resources } returns mockk(relaxed = true)
            every { getSecureSharedPreferences("username") } returns mockk() {

                // Defaults
                every { getBoolean(any(), any()) } returns false
                every { getInt(any(), any()) } returns 0
                every { getLong(any(), any()) } returns 0
                every { getString(any(), any()) } returns ""

                // Meaningful User data
                every { getString(PREF_USER_ID, any()) } returns "id"
                every { getString(PREF_USER_NAME, any()) } returns "username"
                every { getInt(PREF_SUBSCRIBED, any()) } returns 4
                every { getInt(PREF_USER_PRIVATE, any()) } returns 1
                every { getInt(PREF_ROLE, any()) } returns 2
                every { getString(PREF_USER_CURRENCY, any()) } returns "eur"
                every { getInt(PREF_USER_CREDIT, any()) } returns 10
                every { getInt(PREF_DELINQUENT, any()) } returns 3
                every { getInt(PREF_MAX_UPLOAD_FILE_SIZE, any()) } returns 12_345
                every { getLong(PREF_USED_SPACE, any()) } returns 15_000
                every { getLong(PREF_MAX_SPACE, any()) } returns 30_000
                every { getBoolean(PREF_USER_LEGACY_ACCOUNT, any()) } returns true

                every { edit() } returns mockk(relaxed = true)
            }
            every { getSharedPreferences(any(), any()) } returns mockk(relaxed = true)
        }
        every { TextUtils.isEmpty(any()) } answers { firstArg<String?>().isNullOrEmpty() }
        every { SystemClock.elapsedRealtime() } returns 0

        // GIVEN
        val oldUser = LegacyUser.load("username")

        // WHEN
        val legacyUser = mapper { oldUser.toNewModel() }

        // THEN
        assert that legacyUser * {
            +id.s equals "id"
            +name.s equals "username"
            +(plans * {
                +size.fix() equals 1
                it contains Plan.Vpn.Paid
            })
            +private equals true
            +role equals Role.ORGANIZATION_ADMIN
            +currency.s equals "eur"
            +credits.fix() equals 10
            +delinquent equals Delinquent.InvoiceDelinquent
            +totalUploadLimit.l equals 12_345uL
            +(dedicatedSpace * {
                +used.l.fix() equals 15_000uL
                +total.l.fix() equals 30_000uL
            })
            +isLegacy equals true
        }

        unmockkStatic(ProtonMailApplication::class, TextUtils::class, SystemClock::class)
    }
}
