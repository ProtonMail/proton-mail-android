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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ch.protonmail.android.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Fragment popping up with actions for message details screen.
 */
class MessageDetailsActionSheet : BottomSheetDialogFragment() {

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
        return rootView
    }

    companion object {

        const val EXTRA_ARG_TITLE = "arg_message_details_actions_title"
        const val EXTRA_ARG_SUBTITLE = "arg_message_details_actions_sub_title"
    }
}
