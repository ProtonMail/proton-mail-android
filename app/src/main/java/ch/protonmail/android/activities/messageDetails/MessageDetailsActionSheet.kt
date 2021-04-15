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

package ch.protonmail.android.activities.messageDetails

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import ch.protonmail.android.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber

/**
 * Fragment popping up with actions for message details screen.
 */
class MessageDetailsActionSheet : BottomSheetDialogFragment() {

    private lateinit var closeViewIcon: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_message_details_action_sheet, container, false)
        val title = arguments?.getString(EXTRA_ARG_TITLE)
        if (!title.isNullOrEmpty()) {
            rootView.findViewById<TextView>(R.id.detailsActionsTitleTextView).text = title
        }
        val subtitle = arguments?.getString(EXTRA_ARG_SUBTITLE)
        if (!subtitle.isNullOrEmpty()) {
            rootView.findViewById<TextView>(R.id.detailsActionsSubTitleTextView).text = subtitle
        }
        closeViewIcon = rootView.findViewById<TextView>(R.id.detailsActionsCloseView)
        closeViewIcon.setOnClickListener {
            dismiss()
        }
        return rootView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState)
        bottomSheetDialog.setOnShowListener { dialogInterface ->
            val dialog = dialogInterface as BottomSheetDialog
            val bottomSheet: View? = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                bottomSheetBehavior.addBottomSheetCallback(
                    object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            Timber.v("State changed to $newState")
                            if (newState == STATE_EXPANDED) {
                                showCloseIcon()
                            } else {
                                hideCloseIcon()
                            }
                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) {
                            Timber.v("onSlide to offset $slideOffset")
                        }
                    }

                )
            }
        }
        return bottomSheetDialog
    }

    private fun hideCloseIcon() {
        closeViewIcon.isVisible = false
    }

    private fun showCloseIcon() {
        closeViewIcon.isVisible = true
    }

    companion object {

        const val EXTRA_ARG_TITLE = "arg_message_details_actions_title"
        const val EXTRA_ARG_SUBTITLE = "arg_message_details_actions_sub_title"
    }
}
