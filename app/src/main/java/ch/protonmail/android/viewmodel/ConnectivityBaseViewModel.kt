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

package ch.protonmail.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.usecase.NETWORK_CHECK_DELAY
import ch.protonmail.android.usecase.VerifyConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Base view model for activities that require connectivity check logic.
 */
open class ConnectivityBaseViewModel @Inject constructor(
    private val verifyConnection: VerifyConnection,
    private val networkConfigurator: NetworkConfigurator
) : ViewModel() {

    private val verifyConnectionTrigger: MutableLiveData<Unit> = MutableLiveData()

    val hasConnectivity: LiveData<Boolean> =
        verifyConnectionTrigger.switchMap { verifyConnection() }

    fun checkConnectivity() {
        Timber.v("checkConnectivity launch ping")
        verifyConnectionTrigger.value = Unit
    }

    /**
     * Check connectivity with a delay allowing snack bar to be displayed.
     */
    fun checkConnectivityDelayed() {
        viewModelScope.launch {
            delay(NETWORK_CHECK_DELAY)
            retryWithDoh()
            checkConnectivity()
        }
    }

    private fun retryWithDoh() = networkConfigurator.tryRetryWithDoh()
}
