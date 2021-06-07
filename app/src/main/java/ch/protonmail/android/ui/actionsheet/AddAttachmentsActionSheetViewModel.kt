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

import androidx.lifecycle.ViewModel
import ch.protonmail.android.ui.actionsheet.AddAttachmentsActionSheet.Action
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

/**
 * [ViewModel] for [AddAttachmentsActionSheet].
 * It must be a shared ViewModel, between the ActionSheet and the caller, for enable the caller to receive callbacks
 *  from the ActionSheet
 */
@HiltViewModel
class AddAttachmentsActionSheetViewModel @Inject constructor() : ViewModel() {

    private val _result = Channel<Action>(Channel.BUFFERED)
    val result = _result.receiveAsFlow()

    fun requestOpenGallery() {
        _result.offer(Action.GALLERY)
    }

    fun requestOpenCamera() {
        _result.offer(Action.CAMERA)
    }

    fun requestOpenFileExplorer() {
        _result.offer(Action.FILE_EXPLORER)
    }
}
