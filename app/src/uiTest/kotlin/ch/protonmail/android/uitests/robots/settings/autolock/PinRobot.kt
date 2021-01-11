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

package ch.protonmail.android.uitests.robots.settings.autolock

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.click
import junit.framework.Assert.fail

class PinRobot {

    fun setPin(pin: String): AutoLockRobot {
        return typePin(pin)
            .confirm()
            .typePin(pin)
            .create()
    }

    fun providePin(pin: String): ComposerRobot {
        typePin(pin)
        return ComposerRobot()
    }

    private fun typePin(pin: String): PinRobot {
        pin.forEach {
            when (it) {
                one -> UIActions.wait.forViewWithId(buttonOneId).click()
                two -> UIActions.wait.forViewWithId(buttonTwoId).click()
                three -> UIActions.wait.forViewWithId(buttonThreeId).click()
                four -> UIActions.wait.forViewWithId(buttonFourId).click()
                five -> UIActions.wait.forViewWithId(buttonFiveId).click()
                six -> UIActions.wait.forViewWithId(buttonSixId).click()
                seven -> UIActions.wait.forViewWithId(buttonSevenId).click()
                eight -> UIActions.wait.forViewWithId(buttonEightId).click()
                nine -> UIActions.wait.forViewWithId(buttonNineId).click()
                zero -> UIActions.wait.forViewWithId(buttonZeroId).click()
                else -> fail("Cannot click on PIN keyboard for provided input: \"$pin\". Invalid symbol: \"$it\"")
            }
        }
        return this
    }

    private fun confirm(): PinRobot {
        UIActions.wait.forViewWithId(createButtonId).click()
        return this
    }

    private fun create(): AutoLockRobot {
        UIActions.wait.forViewWithId(createButtonId).click()
        return AutoLockRobot()
    }

    companion object {
        private const val pinInputEditTextId = R.id.pin_input
        private const val deletePinImageButtonId = R.id.button_delete
        private const val pinAttemptsTextViewId = R.id.attempts
        private const val createButtonId = R.id.mBtnForward
        private const val buttonOneId = R.id.btn_pin_1
        private const val buttonTwoId = R.id.btn_pin_2
        private const val buttonThreeId = R.id.btn_pin_3
        private const val buttonFourId = R.id.btn_pin_4
        private const val buttonFiveId = R.id.btn_pin_5
        private const val buttonSixId = R.id.btn_pin_6
        private const val buttonSevenId = R.id.btn_pin_7
        private const val buttonEightId = R.id.btn_pin_8
        private const val buttonNineId = R.id.btn_pin_9
        private const val buttonZeroId = R.id.btn_pin_0
        private const val one = '1'
        private const val two = '2'
        private const val three = '3'
        private const val four = '4'
        private const val five = '5'
        private const val six = '6'
        private const val seven = '7'
        private const val eight = '8'
        private const val nine = '9'
        private const val zero = '0'
    }
}
