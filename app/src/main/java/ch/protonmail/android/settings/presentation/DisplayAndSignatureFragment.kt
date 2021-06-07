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
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import ch.protonmail.android.R
import ch.protonmail.android.activities.fragments.BaseFragment
import ch.protonmail.android.activities.settings.BaseSettingsActivity
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.jobs.UpdateSettingsJob
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.libs.core.utils.onTextChange
import com.birbit.android.jobqueue.JobManager
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.content_contact_group_details.*

@AndroidEntryPoint
class DisplayAndSignatureFragment : BaseFragment() {

    override fun getLayoutResourceId() = R.layout.settings_fragment_display_name_and_signature

    override fun getFragmentKey() = "ProtonMail.DisplayAndSignatureFragment"

    var user: User? = null
    var jobManager: JobManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val legacyUser = (activity as BaseSettingsActivity).legacyUser
        val selectedAddress = checkNotNull(user?.addresses?.primary)
        val (newAddressId, signature) = selectedAddress.id to selectedAddress.signature?.s

        var displayName = selectedAddress.displayName?.s ?: selectedAddress.email.s
        val inputDisplayName = view.findViewById<TextInputEditText>(R.id.settings_input_display_name)
        inputDisplayName.setText(displayName)

        inputDisplayName.doAfterTextChanged {
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

        val inputSignature = view.findViewById<TextInputEditText>(R.id.settings_input_signature)
        inputSignature.setText(signature)

        inputSignature.doAfterTextChanged {
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
        val toggleSignature = view.findViewById<SwitchCompat>(R.id.settings_toggle_signature)
        toggleSignature.isChecked = legacyUser.isShowSignature
        toggleSignature.setOnCheckedChangeListener { _, isChecked ->
            legacyUser.isShowSignature = isChecked
        }

        val mobileSignature = legacyUser.mobileSignature
        val inputMobileSignature = view.findViewById<TextInputEditText>(R.id.settings_input_mobile_signature)
        inputMobileSignature.setText(mobileSignature)

        inputMobileSignature.doAfterTextChanged {
            val newMobileSignature = it.toString()
            val isMobileSignatureChanged = newMobileSignature != mobileSignature

            if (isMobileSignatureChanged) {
                legacyUser.mobileSignature = newMobileSignature
            }
        }
        val toggleMobileSignature = view.findViewById<SwitchCompat>(R.id.settings_toggle_mobile_signature)
        toggleMobileSignature.isChecked = legacyUser.isShowMobileSignature
        toggleMobileSignature.setOnCheckedChangeListener { _, isChecked ->
            legacyUser.isShowMobileSignature = isChecked
        }
    }

    companion object {

        fun newInstance(user: User, jobManager: JobManager): DisplayAndSignatureFragment {
            return DisplayAndSignatureFragment().apply {
                this.user = user
                this.jobManager = jobManager
            }
        }
    }
}
