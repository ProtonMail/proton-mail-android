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
package ch.protonmail.android.settings.pin

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import ch.protonmail.android.api.models.room.testValue
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModel
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * Created by dkadrikj on 3/15/19. */
class PinFragmentViewModelTest {

    // region mocks
    private val pinCreationListener = mockk<PinFragmentViewModel.IPinCreationListener>(relaxed = true)
    private val reopenFingerprintDialogListener = mockk<PinFragmentViewModel.ReopenFingerprintDialogListener>(relaxed = true)
    // endregion

    @get:Rule val taskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testSetupOk() {
        val pinFragmentViewModel = PinFragmentViewModel()
        val actionType = PinAction.VALIDATE
        val signOutPossible = true
        val wantedPin = "1234"
        val useFingerprint = false

        val setupLiveData = pinFragmentViewModel.setupDoneObservable
        pinFragmentViewModel.setup(actionType, signOutPossible, wantedPin, useFingerprint)
        val pinSetup = setupLiveData.testValue
        assertNotNull(pinSetup)
        assertEquals(actionType, pinSetup?.actionType)
        assertEquals(signOutPossible, pinSetup?.signOutPossible)
        assertEquals(useFingerprint, pinSetup?.useFingerprint)
    }

    @Test
    fun testPinConfirmBackPress() {
        val pinFragmentViewModel = PinFragmentViewModel()
        val actionType = PinAction.CONFIRM
        val signOutPossible = true
        val wantedPin = null
        val useFingerprint = false

        pinFragmentViewModel.setup(actionType, signOutPossible, wantedPin, useFingerprint)
        pinFragmentViewModel.setListener(pinCreationListener)
        pinFragmentViewModel.setListener(reopenFingerprintDialogListener)
        pinFragmentViewModel.onBackClicked()
        verify(exactly = 1) { pinCreationListener.showCreatePin() }
    }

    @Test
    fun testPinConfirmNextPress() {
        val pinFragmentViewModel = PinFragmentViewModel()
        val actionType = PinAction.CONFIRM
        val signOutPossible = true
        val wantedPin = "1234"
        val useFingerprint = false

        pinFragmentViewModel.setup(actionType, signOutPossible, wantedPin, useFingerprint)
        pinFragmentViewModel.setListener(pinCreationListener)
        pinFragmentViewModel.setListener(reopenFingerprintDialogListener)
        val validationResult = pinFragmentViewModel.nextClicked(wantedPin, createdPinValid = true, validationPinValid = true).testValue

        assertNotNull(validationResult)
        assertEquals(true, validationResult?.valid)
        assertEquals(actionType, validationResult?.actionType)
        verify(exactly = 1) { pinCreationListener.onPinConfirmed(wantedPin) }
        verify(exactly = 0) { pinCreationListener.onPinCreated(wantedPin) }
        verify(exactly = 0) { pinCreationListener.onForgotPin() }
    }

    @Test
    fun testPinCreateNextPress() {
        val pinFragmentViewModel = PinFragmentViewModel()
        val actionType = PinAction.CREATE
        val signOutPossible = true
        val wantedPin = "1234"
        val useFingerprint = false

        pinFragmentViewModel.setup(actionType, signOutPossible, wantedPin, useFingerprint)
        pinFragmentViewModel.setListener(pinCreationListener)
        pinFragmentViewModel.setListener(reopenFingerprintDialogListener)
        val validationResult = pinFragmentViewModel.nextClicked(wantedPin, createdPinValid = true, validationPinValid = true).testValue

        assertNotNull(validationResult)
        assertEquals(true, validationResult?.valid)
        assertEquals(actionType, validationResult?.actionType)
        verify(exactly = 1) { pinCreationListener.onPinCreated(wantedPin) }
        verify(exactly = 0) { pinCreationListener.onPinConfirmed(wantedPin) }
        verify(exactly = 0) { pinCreationListener.onForgotPin() }
    }
}
