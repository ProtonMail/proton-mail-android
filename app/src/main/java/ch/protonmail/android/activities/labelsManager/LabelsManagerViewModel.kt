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
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.mapper.LabelUiModelMapper
import ch.protonmail.android.mapper.map
import ch.protonmail.android.uiModel.LabelUiModel
import ch.protonmail.android.usecase.delete.DeleteLabel
import ch.protonmail.android.worker.PostLabelWorker
import com.birbit.android.jobqueue.JobManager
import studio.forface.viewstatestore.ViewStateStore
import studio.forface.viewstatestore.from
import studio.forface.viewstatestore.paging.PagedViewStateStore
import studio.forface.viewstatestore.paging.ViewStateStoreScope
import javax.inject.Named

/**
 * A [ViewModel] for Manage Labels
 * Inherit from [ViewModel]
 * Implements [ViewStateStoreScope] for being able to publish to a Locked [ViewStateStore]
 */
internal class LabelsManagerViewModel @ViewModelInject constructor(
    private val jobManager: JobManager,
    @Named("messages") private val messagesDatabase: MessagesDatabase,
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val deleteLabel: DeleteLabel,
    private val workManager: WorkManager
) : ViewModel(), ViewStateStoreScope {

    // Extract the original form of the data
    private val type: LabelUiModel.Type = if (savedStateHandle.get<Boolean>(EXTRA_MANAGE_FOLDERS) == true)
        LabelUiModel.Type.FOLDERS
    else
        LabelUiModel.Type.LABELS

    /** Triggered when a selection has changed */
    private val selectedLabelIds = MutableLiveData(mutableSetOf<String>())
    private var deleteLabelIds = MutableLiveData<List<String>>()

    /** Triggered when [selectedLabelIds] has changed */
    val hasSelectedLabels = ViewStateStore.from(selectedLabelIds.map { it.isNotEmpty() }).lock

    val hasSuccessfullyDeletedMessages: LiveData<Boolean>
        get() = deleteLabelIds.switchMap {
            liveData {
                emitSource(deleteLabel(it))
            }
        }

    /**
     * [LiveData] of [PagedList] of [LabelUiModel]
     * Triggered when a Labels are updated in DB
     */
    private val labelsSource = when (type) {
        LabelUiModel.Type.LABELS -> messagesDatabase.getAllLabelsNotExclusivePaged()
        LabelUiModel.Type.FOLDERS -> messagesDatabase.getAllLabelsExclusivePaged()
    }

    private val labelMapper = LabelUiModelMapper(isLabelEditable = true)

    /**
     * A Locked [PagedViewStateStore] of type [LabelUiModel]
     * This will emit every time a Label is changed in the database or the selection state is
     * changed for one of them
     */
    val labels = ViewStateStore.from(
        selectedLabelIds.switchMap { selectedList ->
            labelsSource.map(labelMapper) {
                it.toUiModel().copy(isChecked = it.id in selectedList)
            }.toLiveData(20)
        }
    ).lock

    /** A reference to a [LabelEditor] for edit a [Label] */
    private var labelEditor: LabelEditor? = null

    /** [ColorInt] that hold the color for a new Label */
    @ColorInt
    private var tempLabelColor: Int = Color.WHITE

    /** [CharSequence] that hold the name for a new Label */
    private var tempLabelName: CharSequence = ""

    init {
        labels.setLoading()
    }

    /** Delete all Labels which id is in [selectedLabelIds] */
    fun deleteSelectedLabels() {
        deleteLabelIds.value = selectedLabelIds.mapValue { it }
        selectedLabelIds.clear()
    }

    /** Initialize the editing of a Label */
    fun onLabelEdit(label: LabelUiModel) {
        labelEditor = LabelEditor(label)
    }

    /** Update the selected state of the Label with the given [labelId] */
    fun onLabelSelected(labelId: String, isChecked: Boolean) {
        if (isChecked) selectedLabelIds += labelId
        else selectedLabelIds -= labelId
    }

    /** Close the editing of a Label */
    fun onNewLabel() {
        labelEditor = null
    }

    /** Save the editing label */
    fun saveLabel(): LiveData<WorkInfo> {
        labelEditor?.let {
            return with(it.buildParams()) {
                createOrUpdateLabel(labelName, color, display, exclusive, update, labelId)
            }
        }

        return createOrUpdateLabel(
            tempLabelName.toString(),
            tempLabelColor.toColorHex(),
            0,
            type.toExclusive(),
            false,
            null
        )
    }

    /** Set a [ColorInt] for the current editing Label */
    fun setLabelColor(@ColorInt color: Int) {
        labelEditor?.let { it.color = color } ?: run { tempLabelColor = color }
    }

    /** Set a [CharSequence] name for a new Label or for the currently editing Label */
    fun setLabelName(name: CharSequence) {
        labelEditor?.let { it.name = name } ?: run { tempLabelName = name }
    }

    private fun createOrUpdateLabel(labelName: String,
                                    color: String,
                                    display: Int,
                                    exclusive: Int,
                                    update: Boolean,
                                    labelId: String?): LiveData<WorkInfo> {
        return PostLabelWorker.Enqueuer(workManager).enqueue(
            labelName,
            color,
            display,
            exclusive,
            update,
            labelId
        )
    }
}

/** A class that hold editing progress of a Label */
private class LabelEditor(private val initialLabel: LabelUiModel) {

    /** [ColorInt] color for the current editing Label */
    @ColorInt
    var color: Int = initialLabel.color

    /** [CharSequence] name for the current editing Label */
    var name: CharSequence = initialLabel.name

    /** @return [SaveParams] */
    fun buildParams(): SaveParams {
        return SaveParams(
            labelName = name.toString(),
            color = color.toColorHex(),
            display = initialLabel.display,
            exclusive = initialLabel.type.toExclusive(),
            update = true,
            labelId = initialLabel.labelId
        )
    }

    /** A class holding params for save the Label */
    data class SaveParams(
        val labelName: String,
        val color: String,
        val display: Int,
        val exclusive: Int,
        val update: Boolean,
        val labelId: String
    )
}

/** @return [String] color hex from a [ColorInt] */
private fun Int.toColorHex() = String.format("#%06X", 0xFFFFFF and this)

/** @return [Boolean] exclusive from a [LabelUiModel.Type] */
private fun LabelUiModel.Type.toExclusive() = if (this == LabelUiModel.Type.FOLDERS) 1 else 0

// region Selected Labels extensions
private typealias MutableLiveStringSet = MutableLiveData<MutableSet<String>>

private fun MutableLiveStringSet.clear() {
    value = mutableSetOf()
}

private fun <V> MutableLiveStringSet.mapValue(f: (String) -> V) = value!!.map(f)

private operator fun MutableLiveStringSet.minusAssign(element: String) {
    value = value!!.minus(element).toMutableSet()
}

private operator fun MutableLiveStringSet.plusAssign(element: String) {
    value = value!!.plus(element).toMutableSet()
}
// endregion
