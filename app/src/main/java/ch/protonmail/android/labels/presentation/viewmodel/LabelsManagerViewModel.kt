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
package ch.protonmail.android.activities.labelsManager

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.work.WorkInfo
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.domain.usecase.DeleteLabels
import ch.protonmail.android.labels.presentation.mapper.LabelUiModelMapper
import ch.protonmail.android.labels.presentation.model.LabelsManagerItemUiModel
import ch.protonmail.android.labels.presentation.model.LabelsManagerItemUiModel.Folder
import ch.protonmail.android.labels.presentation.ui.EXTRA_MANAGE_FOLDERS
import ch.protonmail.android.uiModel.LabelUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.runBlocking
import me.proton.core.accountmanager.domain.AccountManager
import studio.forface.viewstatestore.ViewStateStore
import studio.forface.viewstatestore.from
import studio.forface.viewstatestore.paging.PagedViewStateStore
import studio.forface.viewstatestore.paging.ViewStateStoreScope
import java.util.Locale
import javax.inject.Inject

/**
 * A [ViewModel] for Manage Labels
 * Inherit from [ViewModel]
 * Implements [ViewStateStoreScope] for being able to publish to a Locked [ViewStateStore]
 */
@HiltViewModel
internal class LabelsManagerViewModel @Inject constructor(
    private val labelRepository: LabelRepository,
    savedStateHandle: SavedStateHandle,
    private val deleteLabels: DeleteLabels,
    private val accountManager: AccountManager,
    private val labelMapper: LabelUiModelMapper
) : ViewModel(), ViewStateStoreScope {

    // Extract the original form of the data
    private val type: LabelType = if (savedStateHandle.get<Boolean>(EXTRA_MANAGE_FOLDERS) == true) {
        LabelType.FOLDER
    } else {
        LabelType.MESSAGE_LABEL
    }

    /** Triggered when a selection has changed */
    private val selectedLabelIds = MutableLiveData(mutableSetOf<LabelId>())
    private var deleteLabelIds = MutableSharedFlow<List<LabelId>>(extraBufferCapacity = 1)
    private var updateSaveTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Triggered when [selectedLabelIds] has changed */
    val hasSelectedLabels = ViewStateStore.from(selectedLabelIds.map { it.isNotEmpty() }).lock

    val hasSuccessfullyDeletedMessages: Flow<Boolean>
        get() = deleteLabelIds.flatMapLatest { ids ->
            deleteLabels(ids)
        }

    /**
     * [LiveData] of [PagedList] of [LabelUiModel]
     * Triggered when a Labels are updated in DB
     */
    private val labelsSource = when (type) {
        LabelType.MESSAGE_LABEL -> labelRepository.findAllLabelsPaged(
            runBlocking {
                accountManager.getPrimaryUserId().filterNotNull().first()
            }
        )
        LabelType.FOLDER -> labelRepository.findAllFoldersPaged(
            runBlocking {
                accountManager.getPrimaryUserId().filterNotNull().first()
            }
        )
        LabelType.CONTACT_GROUP -> throw IllegalArgumentException("We cannot manage contact groups here!")
    }

    /**
     * A Locked [PagedViewStateStore] of type [LabelUiModel]
     * This will emit every time a Label is changed in the database or the selection state is
     * changed for one of them
     */
    val labels = ViewStateStore.from(
        selectedLabelIds.switchMap { selectedList ->
            labelsSource.map {
                labelMapper.toUiModel(it).copy(isChecked = it.id in selectedList)
            }.toLiveData(20)
        }
    ).lock

    private var labelEditor: LabelEditor? = null

    @ColorInt
    private var tempLabelColor: Int = Color.WHITE
    private var tempLabelName: CharSequence = ""
    private var tempParentFolderId: LabelId? = null

    val createUpdateFlow = updateSaveTrigger
        .flatMapLatest {
            labelEditor?.let { editor ->
                with(editor.buildParams()) {
                    createOrUpdateLabel(
                        labelName = labelName,
                        color = color,
                        isUpdate = update,
                        labelType = type,
                        labelId = labelId,
                        parentId = parentId
                    )
                }
            } ?: createOrUpdateLabel(
                labelName = tempLabelName.toString(),
                color = tempLabelColor.toColorHex(),
                isUpdate = false,
                labelType = type,
                labelId = null,
                parentId = tempParentFolderId
            )
        }

    init {
        labels.setLoading()
    }

    /** Delete all Labels which id is in [selectedLabelIds] */
    fun deleteSelectedLabels() {
        deleteLabelIds.tryEmit(selectedLabelIds.mapValue { it })
        selectedLabelIds.clear()
    }

    /**
     * Initialize the editing of a Label
     */
    fun onLabelEdit(label: LabelsManagerItemUiModel) {
        labelEditor = LabelEditor(label)
    }

    /** Update the selected state of the Label with the given [labelId] */
    fun onLabelSelected(labelId: LabelId, isChecked: Boolean) {
        if (isChecked) selectedLabelIds += labelId
        else selectedLabelIds -= labelId
    }

    /** Close the editing of a Label */
    fun onNewLabel() {
        labelEditor = null
    }

    /** Save the editing label */
    fun saveLabel() =
        updateSaveTrigger.tryEmit(Unit)

    /** Set a [ColorInt] for the current editing Label */
    fun setLabelColor(@ColorInt color: Int) {
        labelEditor?.let { it.color = color } ?: run { tempLabelColor = color }
    }

    /** Set a [CharSequence] name for a new Label or for the currently editing Label */
    fun setLabelName(name: CharSequence) {
        labelEditor?.let { it.name = name } ?: run { tempLabelName = name }
    }

    fun setParentFolder(id: LabelId?) {
        labelEditor?.let { it.parentFolderId = id } ?: run { tempParentFolderId = id }
    }

    private fun createOrUpdateLabel(
        labelName: String,
        color: String,
        isUpdate: Boolean,
        labelType: LabelType,
        labelId: LabelId?,
        parentId: LabelId?
    ): Flow<WorkInfo> = labelRepository.scheduleSaveLabel(
        labelName = labelName,
        color = color,
        isUpdate = isUpdate,
        labelType = labelType,
        labelId = labelId,
        parentId = parentId
    )
}

/** A class that hold editing progress of a Label */
private class LabelEditor(private val initialLabel: LabelsManagerItemUiModel) {

    /**
     * [ColorInt] color for the current editing Label
     */
    @ColorInt
    var color: Int = initialLabel.icon.colorInt

    /**
     * Name for the current editing Label
     */
    var name: CharSequence = initialLabel.name

    /**
     * Id of the parent folder for the current editing Label ( only valid for [Folder]s )
     */
    var parentFolderId: LabelId? = (initialLabel as? Folder)?.parentId

    /** @return [SaveParams] */
    fun buildParams(): SaveParams {
        return SaveParams(
            labelName = name.toString(),
            color = color.toColorHex(),
            isFolder = initialLabel is Folder,
            update = true,
            labelId = initialLabel.id,
            parentId = parentFolderId
        )
    }

    /** A class holding params for save the Label */
    data class SaveParams(
        val labelName: String,
        val color: String,
        val isFolder: Boolean,
        val update: Boolean,
        val labelId: LabelId,
        val parentId: LabelId?
    )
}

private const val WHITE_COLOR_MASK = 0xFFFFFF

/** @return [String] color hex from a [ColorInt] */
private fun Int.toColorHex() = String.format(Locale.getDefault(), "#%06X", WHITE_COLOR_MASK and this)

// region Selected Labels extensions
private typealias MutableLiveIdsSet = MutableLiveData<MutableSet<LabelId>>

private fun MutableLiveIdsSet.clear() {
    value = mutableSetOf()
}

private fun <V> MutableLiveIdsSet.mapValue(f: (LabelId) -> V) = value!!.map(f)

private operator fun MutableLiveIdsSet.minusAssign(element: LabelId) {
    value = value!!.minus(element).toMutableSet()
}

private operator fun MutableLiveIdsSet.plusAssign(element: LabelId) {
    value = value!!.plus(element).toMutableSet()
}
// endregion
