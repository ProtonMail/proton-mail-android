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

package ch.protonmail.android.ui.actionsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import ch.protonmail.android.databinding.ActionsheetAddAttachmentsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import me.proton.core.presentation.utils.onClick

@AndroidEntryPoint
class AddAttachmentsActionSheet : BottomSheetDialogFragment() {

    private val viewModel: AddAttachmentsActionSheetViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ActionsheetAddAttachmentsBinding.inflate(inflater).apply {
            addAttachmentsGalleryItem.onClick(viewModel::requestOpenGallery)
            addAttachmentsCameraItem.onClick(viewModel::requestOpenCamera)
            addAttachmentsFileExplorerItem.onClick(viewModel::requestOpenFileExplorer)
        }.root
    }

    enum class Action {

        /**
         * Open Gallery for select one or more photos or videos
         */
        GALLERY,

        /**
         * Open Camera for shot a new photo or video
         */
        CAMERA,

        /**
         * Open File Explorer for select one or more files
         */
        FILE_EXPLORER
    }

    companion object {

        /**
         * Creates a new action sheet instance
         */
        fun newInstance() = AddAttachmentsActionSheet()

        /**
         * Creates a new instance of the action sheet and shows it
         * @return the newly created [AddAttachmentsActionSheet]
         */
        @JvmOverloads
        fun show(
            fragmentManager: FragmentManager,
            tag: String? = AddAttachmentsActionSheet::class.qualifiedName,
        ) = newInstance()
            .also { it.show(fragmentManager, tag) }
    }
}
