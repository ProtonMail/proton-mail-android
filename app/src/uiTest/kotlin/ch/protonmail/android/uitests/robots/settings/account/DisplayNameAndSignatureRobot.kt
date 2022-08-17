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
package ch.protonmail.android.uitests.robots.settings.account

import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsActions.changeToggleState
import ch.protonmail.android.views.SettingsDefaultItemView
import me.proton.fusion.Fusion
import me.proton.fusion.utils.ActivityProvider
import me.proton.fusion.utils.StringUtils.stringFromResource

/**
 * Class represents Display name and Signature view.
 */
class DisplayNameAndSignatureRobot: Fusion {

    fun setSignatureToggleTo(state: Boolean): DisplayNameAndSignatureRobot {
        changeToggleState(state, signatureTitle, switch(signatureTitleId))
        return this
    }

    fun setMobileSignatureToggleTo(state: Boolean): DisplayNameAndSignatureRobot {
        changeToggleState(state, mobileSignatureTitle, switch(mobileSignatureTitleId))
        return this
    }

    fun setDisplayNameTextTo(text: String): DisplayNameAndSignatureRobot {
        view.withId(R.id.verificationCodeEditText).hasParent(view.withText(displayName)).typeText(text)
        return this
    }

    /**
     * Contains all the validations that can be performed by [DisplayNameAndSignatureRobot].
     */
    class Verify {

        fun signatureToggleCheckedStateIs(state: Boolean): DisplayNameAndSignatureRobot {
//            UIActions.check.viewWithIdAndAncestorTagIsChecked(switchId, signatureTitle, state)
            return DisplayNameAndSignatureRobot()
        }

        fun mobileSignatureToggleCheckedStateIs(state: Boolean): DisplayNameAndSignatureRobot {
//            UIActions.check.viewWithIdAndAncestorTagIsChecked(switchId, mobileSignatureTitle, state)
            return DisplayNameAndSignatureRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    private fun switch(@StringRes tagId: Int) = ActivityProvider.currentActivity!!
        .findViewById<RecyclerView>(R.id.settingsRecyclerView)
        .findViewWithTag<SettingsDefaultItemView>(stringFromResource(tagId))
        .findViewById<SwitchCompat>(switchId)

    companion object {
        private const val switchId = R.id.actionSwitch
        private const val signatureTitleId = R.string.signature
        private const val mobileSignatureTitleId = R.string.signature
        private val signatureTitle = stringFromResource(R.string.signature)
        private val mobileSignatureTitle = stringFromResource(R.string.signature)
        private val displayName = stringFromResource(R.string.display_name)
    }
}
