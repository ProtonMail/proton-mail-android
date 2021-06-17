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

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import ch.protonmail.android.R
import ch.protonmail.android.activities.settings.BaseSettingsActivity
import ch.protonmail.android.databinding.SettingsFragmentDisplayNameAndSignatureBinding
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.jobs.UpdateSettingsJob
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showTwoButtonInfoDialog
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisplayNameAndSignatureFragment : Fragment() {

    var user: User? = null
    var jobManager: JobManager? = null

    private var _binding: SettingsFragmentDisplayNameAndSignatureBinding? = null

    private val binding get() = requireNotNull(_binding)

    private var hasChanges = false
    var newDisplayName = ""
    var newSignature: String? = null
    var newAddressId: Id? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.setTitle(R.string.display_name_n_signature)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.save).isVisible = true
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                if (hasChanges) {
                    context?.showTwoButtonInfoDialog(
                        titleStringId = R.string.dialog_discard_changes_title,
                        messageStringId = R.string.dialog_discard_changes_message,
                        rightStringId = R.string.discard,
                        onPositiveButtonClicked = {
                            activity?.onBackPressed()
                        }
                    )
                } else {
                    activity?.onBackPressed()
                }
                true
            }
            R.id.save -> {
                if (hasChanges) {

                    val job = UpdateSettingsJob(
                        newDisplayName = newDisplayName,
                        newSignature = newSignature,
                        addressId = newAddressId
                    )
                    jobManager?.addJobInBackground(job)
                }
                activity?.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsFragmentDisplayNameAndSignatureBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val legacyUser = (activity as BaseSettingsActivity).legacyUser
        val selectedAddress = checkNotNull(user?.addresses?.primary)
        val signature = selectedAddress.signature?.s
        newAddressId = selectedAddress.id

        val displayName = selectedAddress.displayName?.s ?: selectedAddress.email.s
        binding.settingsInputDisplayName.setText(displayName)
        newDisplayName = displayName

        binding.settingsInputDisplayName.doAfterTextChanged {
            newDisplayName = it.toString()

            val containsBannedChars = newDisplayName.matches(".*[<>].*".toRegex())
            if (containsBannedChars) {
                context?.showToast(R.string.display_name_banned_chars, Toast.LENGTH_SHORT, Gravity.CENTER)
                val primaryAddress = checkNotNull(user?.addresses?.primary)
                newDisplayName = primaryAddress.displayName?.s ?: primaryAddress.email.s
            }

            hasChanges = newDisplayName != displayName
        }

        binding.settingsInputSignature.setText(signature)
        newSignature = signature

        binding.settingsInputSignature.doAfterTextChanged {
            newSignature = it.toString()
            hasChanges = newSignature != signature
        }
        binding.settingsToggleSignature.isChecked = legacyUser.isShowSignature
        binding.settingsToggleSignature.setOnCheckedChangeListener { _, isChecked ->
            legacyUser.isShowSignature = isChecked
        }

        val mobileFooter = legacyUser.mobileFooter
        binding.settingsInputMobileFooter.setText(mobileFooter)

        binding.settingsInputMobileFooter.doAfterTextChanged {
            val newMobileFooter = it.toString()
            val isMobileFooterChanged = newMobileFooter != mobileFooter

            if (isMobileFooterChanged) {
                legacyUser.mobileFooter = newMobileFooter
            }
        }

        legacyUser.isPaidUserSignatureEdit.apply {
            binding.mobileFooterDisabledTextView.isVisible = !this
            binding.settingsTextViewMobileFooterSection.isEnabled = this
            binding.settingsToggleMobileFooter.isEnabled = this
            binding.mobileFooterInfoTextView.isEnabled = this
            binding.mobileFooterDisabledTextView.isEnabled = this
            binding.settingsInputMobileFooter.isEnabled = this
        }
        binding.settingsToggleMobileFooter.isChecked = legacyUser.isShowMobileFooter
        binding.settingsToggleMobileFooter.setOnCheckedChangeListener { _, isChecked ->
            legacyUser.isShowMobileFooter = isChecked
        }
    }

    companion object {

        fun newInstance(user: User, jobManager: JobManager): DisplayNameAndSignatureFragment {
            return DisplayNameAndSignatureFragment().apply {
                this.user = user
                this.jobManager = jobManager
            }
        }
    }
}
