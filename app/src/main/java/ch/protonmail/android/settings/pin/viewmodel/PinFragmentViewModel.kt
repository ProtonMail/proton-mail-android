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
package ch.protonmail.android.settings.pin.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ch.protonmail.android.settings.pin.PinAction
import javax.inject.Inject

/**
 * Created by dkadrikj on 14.03.19. */
class PinFragmentViewModel @Inject constructor() : ViewModel() {

    val setupDoneObservable: MutableLiveData<PinSetup> = MutableLiveData()
    private val invalidPinObservable: MutableLiveData<ValidationResult> = MutableLiveData()

    private var signOutPossible: Boolean = false
    private var wantedPin: String? = null
    private var useFingerprint: Boolean = false

    var actionType: PinAction = PinAction.CREATE
        private set

    fun wantedPin() = wantedPin

    private var fingerprintDialogListener: ReopenFingerprintDialogListener? = null
    private lateinit var listener: IPinCreationListener

    fun setup(actionType: PinAction, signOutPossible: Boolean, wantedPin: String?, useFingerprint: Boolean) {
        this.actionType = actionType
        this.signOutPossible = signOutPossible
        this.wantedPin = wantedPin
        this.useFingerprint = useFingerprint
        setupDoneObservable.postValue(PinSetup(actionType, signOutPossible, useFingerprint))
    }

    fun setListener(listener: IPinCreationListener) {
        this.listener = listener
    }

    fun setListener(listener: ReopenFingerprintDialogListener) {
        if (useFingerprint && actionType == PinAction.VALIDATE) {
            this.fingerprintDialogListener = listener
        }
    }

    fun onBackClicked() {
        if (actionType == PinAction.CONFIRM) {
            listener.showCreatePin()
        }
    }

    fun onForgotPin() {
        listener.onForgotPin()
    }

    fun onFingerprintReopen() {
        fingerprintDialogListener?.onFingerprintReopen()
    }

    fun nextClicked(pin: String, createdPinValid: Boolean, validationPinValid: Boolean): MutableLiveData<ValidationResult> {
        if (actionType == PinAction.CREATE) {
            invalidPinObservable.postValue(ValidationResult(actionType, createdPinValid))
            if (createdPinValid) {
                listener.onPinCreated(pin)
            }
        } else if (actionType == PinAction.CONFIRM) {
            invalidPinObservable.postValue(ValidationResult(actionType, validationPinValid))
            if (validationPinValid) {
                listener.onPinConfirmed(wantedPin)
            }
        }
        return invalidPinObservable
    }

    // region interfaces and helper classes
    interface ReopenFingerprintDialogListener {
        fun onFingerprintReopen()
    }

    interface IPinCreationListener {
        fun onPinCreated(pin: String)
        fun showCreatePin()
        fun onPinConfirmed(confirmPin: String?)
        fun onForgotPin()
    }

    data class PinSetup(val actionType: PinAction, val signOutPossible: Boolean, val useFingerprint: Boolean)

    data class ValidationResult(val actionType: PinAction, val valid: Boolean)
    // endregion
}