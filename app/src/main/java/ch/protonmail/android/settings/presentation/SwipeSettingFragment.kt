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

package ch.protonmail.android.settings.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import ch.protonmail.android.R
import ch.protonmail.android.activities.fragments.BaseFragment
import ch.protonmail.android.activities.settings.SettingsEnum
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.libs.core.utils.onClick

class SwipeSettingFragment : BaseFragment() {

    override fun getLayoutResourceId() = R.layout.settings_swipe_fragment

    override fun getFragmentKey() = "ProtonMail.SwipeSettingFragment"

    lateinit var mailSettings: MailSettings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val leftToRightAction = view.findViewById<TextView>(R.id.left_to_right_swipe_action_text_view)
        val rightToLeftAction = view.findViewById<TextView>(R.id.right_to_left_swipe_action_text_view)
        val leftToRightPlaceholder = view.findViewById<ViewStub>(R.id.left_to_right_placeholder)
        val rightToLeftPlaceholder = view.findViewById<ViewStub>(R.id.right_to_left_placeholder)

        leftToRightAction.text = getString(SwipeAction.values()[mailSettings.rightSwipeAction].actionName)
        leftToRightAction.onClick {
            val rightLeftChooserIntent = Intent(context, SwipeChooserActivity::class.java)
            rightLeftChooserIntent.putExtra(EXTRA_CURRENT_ACTION, mailSettings.rightSwipeAction)
            rightLeftChooserIntent.putExtra(EXTRA_SWIPE_ID, SwipeType.RIGHT)
            startActivityForResult(
                AppUtil.decorInAppIntent(rightLeftChooserIntent),
                SettingsEnum.SWIPE_RIGHT.ordinal
            )
        }
        leftToRightPlaceholder.layoutResource =
            SwipeAction.values()[mailSettings.rightSwipeAction].getActionBackgroundResource(true)
        leftToRightPlaceholder.inflate()

        rightToLeftAction.text = getString(SwipeAction.values()[mailSettings.leftSwipeAction].actionName)
        rightToLeftAction.onClick {
            val swipeLeftChooserIntent = Intent(context, SwipeChooserActivity::class.java)
            swipeLeftChooserIntent.putExtra(EXTRA_CURRENT_ACTION, mailSettings.leftSwipeAction)
            swipeLeftChooserIntent.putExtra(EXTRA_SWIPE_ID, SwipeType.LEFT)
            startActivityForResult(
                AppUtil.decorInAppIntent(swipeLeftChooserIntent),
                SettingsEnum.SWIPE_LEFT.ordinal
            )
        }
        rightToLeftPlaceholder.layoutResource =
            SwipeAction.values()[mailSettings.leftSwipeAction].getActionBackgroundResource(false)
        rightToLeftPlaceholder.inflate()
    }

    companion object {

        fun newInstance(mailSettings: MailSettings): SwipeSettingFragment {
            return SwipeSettingFragment().apply {
                this.mailSettings = mailSettings
            }
        }
    }
}
