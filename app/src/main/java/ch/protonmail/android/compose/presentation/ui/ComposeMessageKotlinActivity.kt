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

package ch.protonmail.android.compose.presentation.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.activities.BaseContactsActivity
import ch.protonmail.android.attachments.domain.model.UriPair
import ch.protonmail.android.compose.ComposeMessageViewModel
import ch.protonmail.android.compose.presentation.model.AttachmentsEventUiModel
import ch.protonmail.android.compose.presentation.model.ComposerAttachmentUiModel
import ch.protonmail.android.databinding.ActivityComposeMessage2Binding
import ch.protonmail.android.domain.entity.user.MimeType
import ch.protonmail.android.ui.actionsheet.AddAttachmentsActionSheet
import ch.protonmail.android.ui.actionsheet.AddAttachmentsActionSheet.Action
import ch.protonmail.android.ui.actionsheet.AddAttachmentsActionSheetViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.presentation.utils.onClick
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber

@AndroidEntryPoint
abstract class ComposeMessageKotlinActivity : BaseContactsActivity() {

    protected val composeViewModel: ComposeMessageViewModel by viewModels()
    private val addAttachmentsViewModel: AddAttachmentsActionSheetViewModel by viewModels()
    protected lateinit var binding: ActivityComposeMessage2Binding

    // region Attachments activity results
    private var takePhotoFromCameraUri: UriPair? = null
    private val takePhotoFromCameraLauncher: ActivityResultLauncher<Uri> =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                val uri = checkNotNull(takePhotoFromCameraUri)
                composeViewModel.addAttachments(listOf(uri.insecure), deleteOriginalFiles = false)
            } else {
                Timber.i("Take photo canceled!")
            }
            takePhotoFromCameraUri = null
        }
    private val getContentsLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            composeViewModel.addAttachments(uris, deleteOriginalFiles = false)
        }
    // endregion

    override fun getRootView(): View {
        binding = ActivityComposeMessage2Binding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // region setup UI
        binding.addAttachments.onClick { AddAttachmentsActionSheet.show(supportFragmentManager) }
        // endregion
        // region observe
        composeViewModel.attachmentsEvent
            .onEach { event ->
                when (event) {
                    is AttachmentsEventUiModel.OnAttachmentsChange -> onAttachmentsChanged(event.attachments)
                    is AttachmentsEventUiModel.OnPhotoUriReady -> takePhotoFromCamera(event.uri)
                }
            }.launchIn(lifecycleScope)

        addAttachmentsViewModel.result
            .onEach { result ->
                when (result) {
                    Action.GALLERY -> openGallery()
                    Action.CAMERA -> composeViewModel.requestNewPhotoUri()
                    Action.FILE_EXPLORER -> openFileExplorer()
                }.exhaustive
            }.launchIn(lifecycleScope)
        // endregion
    }

    // region attachments
    private fun openGallery() {
        getContentsLauncher.launch(MimeType.IMAGE.string)
    }

    private fun takePhotoFromCamera(uri: UriPair) {
        takePhotoFromCameraUri = uri
        takePhotoFromCameraLauncher.launch(uri.secure)
    }

    private fun openFileExplorer() {
        getContentsLauncher.launch(MimeType.ALL.string)
    }

    private fun onAttachmentsChanged(newAttachments: List<ComposerAttachmentUiModel>) {

    }
 // endregion
}
