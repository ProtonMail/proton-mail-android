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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.databinding.SettingsSwipeFragmentBinding
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.libs.core.utils.onClick
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.SwipeAction
import ch.protonmail.android.adapters.swipe.SwipeAction as SwipeActionLocal

@AndroidEntryPoint
class SwipeSettingFragment : Fragment() {

    lateinit var mailSettings: MailSettings
    private var _binding: SettingsSwipeFragmentBinding? = null

    private val swipeActionsViewModel: SwipeActionsViewModel by viewModels()

    private val binding get() = requireNotNull(_binding)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.setTitle(R.string.swipe_actions)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsSwipeFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            swipeActionsViewModel.getMailSettings()
                .dropWhile { it !is GetMailSettings.MailSettingsState.Success }
                .onEach { result ->
                    when (result) {
                        is GetMailSettings.MailSettingsState.Success -> {
                            result.mailSettings?.let {
                                mailSettings = it
                                renderLeftToRightPreview()
                                renderRightToLeftPreview()
                            }
                        }
                    }
                }.launchIn(lifecycleScope)
        }
    }

    private fun renderLeftToRightPreview() {
        val swipeRightAction: SwipeAction = mailSettings.swipeRight?.enum ?: SwipeAction.Trash

        binding.leftToRightSwipeActionTextView.text =
            getString(SwipeActionLocal.values()[swipeRightAction.value].actionName)

        binding.leftToRightSwipeActionTextView.onClick {
            val rightLeftChooserIntent = Intent(context, SwipeChooserActivity::class.java)
            rightLeftChooserIntent.putExtra(EXTRA_CURRENT_ACTION, mailSettings.swipeRight?.enum)
            rightLeftChooserIntent.putExtra(EXTRA_SWIPE_ID, SwipeType.RIGHT)
            startActivity(AppUtil.decorInAppIntent(rightLeftChooserIntent))
        }

        binding.leftToRightPlaceholderWrapper.removeAllViews()
        val leftToRightPlaceholder = createViewStub(
            SwipeActionLocal.values()[swipeRightAction.value].getActionPreviewBackgroundResource(true)
        )
        binding.leftToRightPlaceholderWrapper.addView(leftToRightPlaceholder)
        leftToRightPlaceholder.inflate()
    }

    private fun renderRightToLeftPreview() {
        val swipeLeftAction = mailSettings.swipeLeft?.enum ?: SwipeAction.Archive

        binding.rightToLeftSwipeActionTextView.text =
            getString(SwipeActionLocal.values()[swipeLeftAction.value].actionName)

        binding.rightToLeftSwipeActionTextView.onClick {
            val swipeLeftChooserIntent = Intent(context, SwipeChooserActivity::class.java)
            swipeLeftChooserIntent.putExtra(EXTRA_CURRENT_ACTION, mailSettings.swipeLeft?.enum)
            swipeLeftChooserIntent.putExtra(EXTRA_SWIPE_ID, SwipeType.LEFT)
            startActivity(AppUtil.decorInAppIntent(swipeLeftChooserIntent))
        }

        binding.rightToLeftPlaceholderWrapper.removeAllViews()
        val rightToLeftPlaceholder = createViewStub(
            SwipeActionLocal.values()[swipeLeftAction.value].getActionPreviewBackgroundResource(false)
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

        fun newInstance() = SwipeSettingFragment()
    }
}
