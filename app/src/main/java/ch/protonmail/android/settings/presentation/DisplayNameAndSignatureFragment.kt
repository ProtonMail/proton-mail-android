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
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import ch.protonmail.android.R
import ch.protonmail.android.activities.settings.BaseSettingsActivity
import ch.protonmail.android.databinding.SettingsFragmentDisplayNameAndSignatureBinding
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.jobs.UpdateSettingsJob
import ch.protonmail.android.utils.extensions.showToast
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisplayNameAndSignatureFragment : Fragment() {

    var user: User? = null
    var jobManager: JobManager? = null

    private var _binding: SettingsFragmentDisplayNameAndSignatureBinding? = null

    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsFragmentDisplayNameAndSignatureBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val legacyUser = (activity as BaseSettingsActivity).legacyUser
        val selectedAddress = checkNotNull(user?.addresses?.primary)
        val (newAddressId, signature) = selectedAddress.id to selectedAddress.signature?.s

        var displayName = selectedAddress.displayName?.s ?: selectedAddress.email.s
        binding.settingsInputDisplayName.setText(displayName)

        binding.settingsInputDisplayName.doAfterTextChanged {
            var newDisplayName = it.toString()

            val containsBannedChars = newDisplayName.matches(".*[<>].*".toRegex())
            if (containsBannedChars) {
                context?.showToast(R.string.display_name_banned_chars, Toast.LENGTH_SHORT, Gravity.CENTER)
                val primaryAddress = checkNotNull(user?.addresses?.primary)
                newDisplayName = primaryAddress.displayName?.s ?: primaryAddress.email.s
            }

            val displayChanged = newDisplayName != displayName
            if (displayChanged) {
                displayName = newDisplayName

                val job = UpdateSettingsJob(
                    newDisplayName = newDisplayName,
                    addressId = newAddressId
                )
                jobManager?.addJobInBackground(job)
            }
        }

        binding.settingsInputSignature.setText(signature)

        binding.settingsInputSignature.doAfterTextChanged {
            val newSignature = it.toString()
            val isSignatureChanged = newSignature != signature
            if (isSignatureChanged) {
                val job = UpdateSettingsJob(
                    newSignature = newSignature,
                    addressId = newAddressId
                )
                jobManager?.addJobInBackground(job)
            }
        }
        binding.settingsToggleSignature.isChecked = legacyUser.isShowSignature
        binding.settingsToggleSignature.setOnCheckedChangeListener { _, isChecked ->
            legacyUser.isShowSignature = isChecked
        }

        val mobileSignature = legacyUser.mobileSignature
        binding.settingsInputMobileFooter.setText(mobileSignature)

        binding.settingsInputMobileFooter.doAfterTextChanged {
            val newMobileSignature = it.toString()
            val isMobileSignatureChanged = newMobileSignature != mobileSignature

            if (isMobileSignatureChanged) {
                legacyUser.mobileSignature = newMobileSignature
            }
        }

        legacyUser.isPaidUserSignatureEdit.apply {
            binding.mobileFooterDisabledTextView.isVisible = !this
            binding.settingsTextViewMobileFooterSection.isEnabled = this
            binding.settingsToggleMobileFooter.isEnabled = this
            binding.mobileFooterDisabledTextView.isEnabled = this
            binding.settingsInputMobileFooter.isEnabled = this
        }
        binding.settingsToggleMobileFooter.isChecked = legacyUser.isShowMobileSignature
        binding.settingsToggleMobileFooter.setOnCheckedChangeListener { _, isChecked ->
            legacyUser.isShowMobileSignature = isChecked
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
