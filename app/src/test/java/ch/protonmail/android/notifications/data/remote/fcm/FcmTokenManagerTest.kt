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

package ch.protonmail.android.notifications.data.remote.fcm

import assert4k.assert
import assert4k.coFails
import assert4k.equals
import assert4k.that
import ch.protonmail.android.domain.entity.ValidationException
import ch.protonmail.android.notifications.data.remote.fcm.model.FirebaseToken
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.test.android.mocks.mockSharedPreferences
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test

class FcmTokenManagerTest : CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private lateinit var fcmTokenManager: FcmTokenManager

    @BeforeTest
    fun setUp() {
        fcmTokenManager = FcmTokenManager(mockSharedPreferences, dispatchers)
    }

    @Test
    fun canStoreAndRetrieveProperFirebaseToken() = runTest {
        // given
        fcmTokenManager.saveToken(FirebaseToken("a token"))

        // when - then
        assert that fcmTokenManager.getToken() equals FirebaseToken("a token")
    }

    @Test
    fun cannotStoreAnInvalidToken() = runTest {
        assert that coFails<ValidationException> {
            fcmTokenManager.saveToken(FirebaseToken(""))
        }
    }

    @Test
    fun canStoreAndRetrieveWhenTheTokenHasBenSent() = runTest {
        // given
        fcmTokenManager.setTokenSent(true)

        // when - then
        assert that fcmTokenManager.isTokenSent()
    }

    @Test
    fun canStoreAndRetrieveWhenTheTokenHasBenUnSent() = runTest {
        // given
        fcmTokenManager.setTokenSent(false)

        // when - then
        assert that fcmTokenManager.isTokenSent().not()
    }
}
