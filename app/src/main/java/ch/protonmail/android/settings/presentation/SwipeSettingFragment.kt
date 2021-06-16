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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.activities.settings.BaseSettingsActivity
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.databinding.SettingsSwipeFragmentBinding
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.libs.core.utils.onClick
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.util.android.sharedpreferences.observe

class SwipeSettingFragment : Fragment() {

    lateinit var mailSettings: MailSettings
    private var _binding: SettingsSwipeFragmentBinding? = null

    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsSwipeFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as BaseSettingsActivity).preferences?.observe()?.onEach {

            mailSettings =
                checkNotNull((activity as BaseSettingsActivity).userManager.getCurrentUserMailSettingsBlocking())
            renderLeftToRightPreview()
            renderRightToLeftPreview()
        }?.launchIn(lifecycleScope)
    }

    fun renderLeftToRightPreview() {
        binding.leftToRightSwipeActionTextView.text =
            getString(SwipeAction.values()[mailSettings.rightSwipeAction].actionName)

        binding.leftToRightSwipeActionTextView.onClick {
            val rightLeftChooserIntent = Intent(context, SwipeChooserActivity::class.java)
            rightLeftChooserIntent.putExtra(EXTRA_CURRENT_ACTION, mailSettings.rightSwipeAction)
            rightLeftChooserIntent.putExtra(EXTRA_SWIPE_ID, SwipeType.RIGHT)
            startActivity(AppUtil.decorInAppIntent(rightLeftChooserIntent))
        }

        binding.leftToRightPlaceholderWrapper.removeAllViews()
        val leftToRightPlaceholder = createViewStub(
            SwipeAction.values()[mailSettings.rightSwipeAction].getActionPreviewBackgroundResource(true)
        )
        binding.leftToRightPlaceholderWrapper.addView(leftToRightPlaceholder)
        leftToRightPlaceholder.inflate()
    }

    fun renderRightToLeftPreview() {

        binding.rightToLeftSwipeActionTextView.text =
            getString(SwipeAction.values()[mailSettings.leftSwipeAction].actionName)

        binding.rightToLeftSwipeActionTextView.onClick {
            val swipeLeftChooserIntent = Intent(context, SwipeChooserActivity::class.java)
            swipeLeftChooserIntent.putExtra(EXTRA_CURRENT_ACTION, mailSettings.leftSwipeAction)
            swipeLeftChooserIntent.putExtra(EXTRA_SWIPE_ID, SwipeType.LEFT)
            startActivity(AppUtil.decorInAppIntent(swipeLeftChooserIntent))
        }

        binding.rightToLeftPlaceholderWrapper.removeAllViews()
        val rightToLeftPlaceholder = createViewStub(
            SwipeAction.values()[mailSettings.leftSwipeAction].getActionPreviewBackgroundResource(false)
        )
        binding.rightToLeftPlaceholderWrapper.addView(rightToLeftPlaceholder)
        rightToLeftPlaceholder.inflate()
    }

    fun createViewStub(resourceId: Int): ViewStub {
        val viewStub = ViewStub(
            activity,
            resourceId
        )
        viewStub.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return viewStub
    }

    companion object {

        fun newInstance(mailSettings: MailSettings): SwipeSettingFragment {
            return SwipeSettingFragment().apply {
                this.mailSettings = mailSettings
            }
        }
    }
}
