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
package ch.protonmail.android.uitests.robots.settings.account

import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.ActivityProvider.currentActivity
import ch.protonmail.android.uitests.testsHelper.StringUtils.stringFromResource
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.views.SettingsDefaultItemView

/**
 * Class represents Display name and Signature view.
 */
class DisplayNameAndSignatureRobot {

    fun setSignatureToggleTo(state: Boolean): DisplayNameAndSignatureRobot {
        changeToggleState(state, signature)
        return this
    }

    fun setMobileSignatureToggleTo(state: Boolean): DisplayNameAndSignatureRobot {
        changeToggleState(state, mobileSignature)
        return this
    }

    fun setDisplayNameTextTo(text: String): DisplayNameAndSignatureRobot {
        UIActions.allOf.setTextIntoFieldWithIdAndAncestorTag(R.id.editText, displayName, text)
        return this
    }

    /**
     * Contains all the validations that can be performed by [DisplayNameAndSignatureRobot].
     */
    class Verify {

        fun signatureToggleCheckedStateIs(state: Boolean): DisplayNameAndSignatureRobot {
            UIActions.check.viewWithIdAndAncestorTagIsChecked(switchId, signature, state)
            return DisplayNameAndSignatureRobot()
        }

        fun mobileSignatureToggleCheckedStateIs(state: Boolean): DisplayNameAndSignatureRobot {
            UIActions.check.viewWithIdAndAncestorTagIsChecked(switchId, mobileSignature, state)
            return DisplayNameAndSignatureRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    private fun changeToggleState(state: Boolean, tag: String) {
        val currentSwitchState = currentActivity!!
            .findViewById<RecyclerView>(R.id.settingsRecyclerView)
            .findViewWithTag<SettingsDefaultItemView>(tag)
            .findViewById<SwitchCompat>(switchId)
            .isChecked

        when (state xor currentSwitchState) {
            true -> {
                UIActions.allOf.clickViewWithIdAndAncestorTag(switchId, tag)
            }
            false -> {
                UIActions.allOf.clickViewWithIdAndAncestorTag(switchId, tag)
                UIActions.allOf.clickViewWithIdAndAncestorTag(switchId, tag)
            }
        }
    }

    companion object {
        private const val switchId = R.id.actionSwitch
        private val signature = stringFromResource(R.string.signature)
        private val mobileSignature = stringFromResource(R.string.mobile_signature)
        private val displayName = stringFromResource(R.string.display_name)
    }
}
