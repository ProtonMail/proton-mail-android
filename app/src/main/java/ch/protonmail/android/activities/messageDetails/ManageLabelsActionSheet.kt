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
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import ch.protonmail.android.R
import ch.protonmail.android.databinding.FragmentManageLabelsActionSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManageLabelsActionSheet : BottomSheetDialogFragment() {

    private val viewModel: ManageLabelsActionSheetViewModel by viewModels()

    private var _binding: FragmentManageLabelsActionSheetBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageLabelsActionSheetBinding.inflate(inflater)

        with(binding.includeLayoutActionSheetHeader) {
            actionsSheetSubTitleTextView.isVisible = false
            actionsSheetTitleTextView.text = getString(R.string.label_as)
            textviewActionsSheetRightAction.isVisible = true
            actionsSheetCloseView.setOnClickListener {
                dismiss()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val EXTRA_ARG_MESSAGE_CHECKED_LABELS = "extra_arg_message_checked_labels"

        fun newInstance(checkedLabels: List<String>): ManageLabelsActionSheet {

            return ManageLabelsActionSheet().apply {
                arguments = bundleOf(
                    EXTRA_ARG_MESSAGE_CHECKED_LABELS to checkedLabels,
                )
            }
        }
    }
}
