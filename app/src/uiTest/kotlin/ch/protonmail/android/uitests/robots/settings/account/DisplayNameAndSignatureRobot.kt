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

import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsActions.changeToggleState
import ch.protonmail.android.uitests.testsHelper.StringUtils.stringFromResource
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.views.SettingsDefaultItemView
import me.proton.core.test.android.instrumented.utils.ActivityProvider
import me.proton.core.test.android.instrumented.utils.StringUtils

/**
 * Class represents Display name and Signature view.
 */
class DisplayNameAndSignatureRobot {

    fun setSignatureToggleTo(state: Boolean): DisplayNameAndSignatureRobot {
        changeToggleState(state, signatureTitle, switch(signatureTitleId))
        return this
    }

    fun setMobileSignatureToggleTo(state: Boolean): DisplayNameAndSignatureRobot {
        changeToggleState(state, mobileSignatureTitle, switch(mobileSignatureTitleId))
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
            UIActions.check.viewWithIdAndAncestorTagIsChecked(switchId, signatureTitle, state)
            return DisplayNameAndSignatureRobot()
        }

        fun mobileSignatureToggleCheckedStateIs(state: Boolean): DisplayNameAndSignatureRobot {
            UIActions.check.viewWithIdAndAncestorTagIsChecked(switchId, mobileSignatureTitle, state)
            return DisplayNameAndSignatureRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    private fun switch(@StringRes tagId: Int) = ActivityProvider.currentActivity!!
        .findViewById<RecyclerView>(R.id.settingsRecyclerView)
        .findViewWithTag<SettingsDefaultItemView>(StringUtils.stringFromResource(tagId))
        .findViewById<SwitchCompat>(switchId)

    companion object {
        private const val switchId = R.id.actionSwitch
        private const val signatureTitleId = R.string.signature
        private const val mobileSignatureTitleId = R.string.mobile_signature
        private val signatureTitle = stringFromResource(R.string.signature)
        private val mobileSignatureTitle = stringFromResource(R.string.mobile_signature)
        private val displayName = stringFromResource(R.string.display_name)
    }
}
