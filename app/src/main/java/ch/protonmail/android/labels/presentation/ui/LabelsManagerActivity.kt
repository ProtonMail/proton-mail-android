/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.labels.presentation.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.adapters.LabelColorsAdapter
import ch.protonmail.android.labels.data.remote.worker.KEY_POST_LABEL_WORKER_RESULT_ERROR
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.presentation.model.LabelsManagerItemUiModel
import ch.protonmail.android.labels.presentation.viewmodel.LabelsManagerViewModel
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.onTextChange
import ch.protonmail.android.utils.extensions.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_labels_manager.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.presentation.utils.onClick
import studio.forface.viewstatestore.ViewStateActivity
import kotlin.random.Random

// region constants
private const val COLOR_PICKER_COLUMNS = 5

const val EXTRA_MANAGE_FOLDERS = "manage_folders"
const val EXTRA_POPUP_STYLE = "popup_style"
const val EXTRA_CREATE_ONLY = "create_only"
// endregion

/**
 * An `Activity` for manage Labels and Folders
 * Inherit from [BaseActivity]
 * Implements [ViewStateActivity] for bind `ViewStateStore`s implicitly to the Lifecycle
 */
@AndroidEntryPoint
class LabelsManagerActivity : BaseActivity(), ViewStateActivity {

    /** Lazy instance of [LabelColorsAdapter] */
    private val colorsAdapter by lazy {
        LabelColorsAdapter(
            applicationContext, colorOptions,
            if (type == LabelType.MESSAGE_LABEL) R.layout.label_color_item_circle else R.layout.folder_color_item
        )
    }

    /** [IntArray] of the available colors */
    private val colorOptions by lazy { resources.getIntArray(R.array.label_colors) }

    /**
     * Whether this `Activity` is enabled only to create a Label / Folder
     * Default is `false`
     */
    private val createOnly by lazy {
        intent?.extras?.getBoolean(EXTRA_CREATE_ONLY, false) ?: false
    }

    private val labelsAdapter = LabelsManagerAdapter(
        onItemClick = ::onLabelClick,
        onItemCheck = ::onLabelSelectionChange,
        onItemEditClick = ::onLabelClick
    )

    /** Whether this `Activity` must use a Popup Style. Default is `false` */
    private val popupStyle by lazy {
        intent?.extras?.getBoolean(EXTRA_POPUP_STYLE, false) ?: false
    }

    /** Current [State] of the `Activity` */
    private var state: State = State.UNDEFINED
        set(value) {
            if (field != value) {
                field = value
                onStateChange(value)
            }
        }

    /**
     * The [LabelType] handled by the `Activity`.
     * It represents which type of entity is currently managing
     * @see LabelType.FOLDER
     * @see LabelType.MESSAGE_LABEL
     */
    private val type by lazy {
        val managingFolders = intent?.extras?.getBoolean(EXTRA_MANAGE_FOLDERS, false) ?: false
        if (managingFolders) LabelType.FOLDER else LabelType.MESSAGE_LABEL
    }

    private val viewModel: LabelsManagerViewModel by viewModels()

    private var currentEditingLabel: LabelId? = null
    private var parentFolderId: LabelId? = null

    private val parentFolderPickerLauncher =
        registerForActivityResult(ParentFolderPickerActivity.Launcher()) { labelId ->
            viewModel.setParentFolder(labelId)
            updateParentFolder(labelId)
        }

    /** @return [LayoutRes] for the content View */
    override fun getLayoutId() = R.layout.activity_labels_manager

    override fun onCreate(savedInstanceState: Bundle?) {
        // If is popupStyle set the relative theme
        if (popupStyle) setTheme(R.style.PopupTheme)
        super.onCreate(savedInstanceState)

        // Setup the action bar
        supportActionBar?.run {
            if (popupStyle) hide()
            else setDisplayHomeAsUpEnabled(true)

            val elevation = resources.getDimension(R.dimen.action_bar_elevation)
            this.elevation = elevation
            title = getString(
                if (type == LabelType.MESSAGE_LABEL) R.string.labels
                else R.string.folders
            )
        }

        // Setup Views
        initTexts()
        delete_labels.isEnabled = false
        colors_grid_view.apply {
            val gridLayoutParams = layoutParams as LinearLayout.LayoutParams
            val itemHeight = resources.getDimensionPixelSize(R.dimen.settings_color_item_size) +
                resources.getDimensionPixelSize(R.dimen.padding_xl)
            gridLayoutParams.height = colorOptions.size * itemHeight / COLOR_PICKER_COLUMNS

            adapter = colorsAdapter
        }

        state = if (createOnly) State.CREATE else State.UNDEFINED

        // Set listeners
        delete_labels.setOnClickListener { showDeleteConfirmation() }
        label_name_text_view.onTextChange(::onLabelNameChange)
        save_button.setOnClickListener { saveCurrentLabel() }
        colors_grid_view.setOnItemClickListener { _, _, position, _ -> onLabelColorChange(position) }
        labels_manager_parent_folder_text_view.onClick {
            parentFolderPickerLauncher.launch(
                ParentFolderPickerActivity.Input(
                    currentFolder = currentEditingLabel,
                    selectedParentFolder = parentFolderId
                )
            )
        }

        // Setup Labels RecyclerView
        labels_recycler_view.apply {
            adapter = labelsAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // Observe labels
        with(viewModel) {
            labels
                .onEach(::onLabels)
                .launchIn(lifecycleScope)

            // Observe labels selection
            hasSelectedLabels
                .onEach { delete_labels.isEnabled = it }
                .launchIn(lifecycleScope)

            // Observe deleted labels
            hasSuccessfullyDeletedMessages
                .onEach(::onLabelDeletedEvent)
                .launchIn(lifecycleScope)

            createUpdateFlow
                .onEach { if (it.state.isFinished) displayLabelCreationOutcome(it) }
                .launchIn(lifecycleScope)
        }
    }

    /** Custom setup if [popupStyle] */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (popupStyle) {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            window.setLayout(
                (width * 0.99f).toInt(), (height * 0.95f).toInt()
            )
        }
    }

    /** Register to EventBus */
    override fun onStart() {
        super.onStart()
        app.bus.register(this)
    }

    /** Unregister from EventBus */
    override fun onStop() {
        app.bus.unregister(this)
        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (state != State.UNDEFINED && !createOnly) {
            state = State.UNDEFINED
        } else {
            super.onBackPressed()
        }
    }

    /** Close the soft keyboard */
    private fun closeKeyboard() {
        UiUtil.hideKeyboard(this, label_name_text_view)
    }

    private fun onLabels(labels: List<LabelsManagerItemUiModel>) {
        no_labels.isVisible = labels.isEmpty()
        delete_labels.isVisible = labels.isNotEmpty()
        labelsAdapter.submitList(labels)
    }

    private fun onLabelClick(label: LabelsManagerItemUiModel) {
        currentEditingLabel = label.id
        state = State.UPDATE

        label_name_text_view.setText(label.name)
        add_label_container.isVisible = true
        if (label is LabelsManagerItemUiModel.Folder) updateParentFolder(label.parentId)
        toggleEditor(true)

        viewModel.onLabelEdit(label)

        val currentColorPosition = colorOptions.indexOf(label.icon.colorInt)
        if (currentColorPosition > -1) {
            colorsAdapter.setChecked(currentColorPosition)
            viewModel.setLabelColor(colorOptions[currentColorPosition])
        }
    }

    private fun updateParentFolder(folderId: LabelId?) {
        parentFolderId = folderId
        val textRes =
            if (folderId != null) R.string.labels_manager_parent_folder_selected
            else R.string.labels_manager_select_parent_folder
        labels_manager_parent_folder_text_view.setText(textRes)
    }

    /** When Label color is changed in the [colors_grid_view] */
    private fun onLabelColorChange(positionInColorOptions: Int) {
        colorsAdapter.setChecked(positionInColorOptions)
        viewModel.setLabelColor(colorOptions[positionInColorOptions])
        closeKeyboard()
    }

    /** When Label name is changed in the [label_name_text_view] `EditText` */
    private fun onLabelNameChange(name: CharSequence) {
        save_button.isVisible = name.isNotBlank()

        if (name.isNotEmpty() && state == State.UNDEFINED) {
            state = State.CREATE
        }

        viewModel.setLabelName(name)
    }

    /** When a Label is selected or unselected */
    private fun onLabelSelectionChange(label: LabelsManagerItemUiModel, isSelected: Boolean) {
        viewModel.onLabelSelected(label.id, isSelected)
    }

    /** When the [state] is changed */
    private fun onStateChange(state: State) {
        when (state) {

            State.UNDEFINED -> {
                viewModel.onNewLabel()
                toggleEditor(false)
                closeKeyboard()
                label_name_text_view.setText("")
                save_button.setText(R.string.x_done)
            }

            State.CREATE -> {
                selectRandomColor()
                toggleEditor(true)
                save_button.setText(R.string.x_done)
            }

            State.UPDATE -> {
                toggleEditor(true)

                save_button.setText(
                    when (type) {
                        LabelType.MESSAGE_LABEL -> R.string.update_label
                        LabelType.FOLDER -> R.string.update_folder
                        else -> throw IllegalArgumentException("Unsupported type: $type")
                    }
                )
            }
        }
    }

    /** Show a message that prompt the user to confirm the deletion */
    private fun showDeleteConfirmation() {
        val title = when (type) {
            LabelType.MESSAGE_LABEL -> R.string.delete_label_confirmation_title
            LabelType.FOLDER -> R.string.delete_folder_confirmation_title
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(R.string.delete_label_confirmation_message)
            .setPositiveButton(R.string.okay) { _, _ -> viewModel.deleteSelectedLabels() }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    private fun saveCurrentLabel() {
        viewModel.saveLabel()
        state = State.UNDEFINED
    }

    private fun displayLabelCreationOutcome(workInfo: WorkInfo) {
        val success = workInfo.state == WorkInfo.State.SUCCEEDED
        val errorMessage = workInfo.outputData.getString(KEY_POST_LABEL_WORKER_RESULT_ERROR)

        if (success && createOnly) onBackPressed()

        colors_grid_view.isVisible = true

        val message = when (type) {
            LabelType.FOLDER -> {
                when (success) {
                    true -> getString(R.string.folder_created)
                    false -> errorMessage ?: getString(R.string.folder_invalid)
                }
            }
            LabelType.MESSAGE_LABEL -> {
                when (success) {
                    true -> getString(R.string.label_created)
                    false -> errorMessage ?: getString(R.string.label_invalid)
                }
            }
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }

        showToast(message, Toast.LENGTH_SHORT)
    }

    /** Select a random Color for the Label */
    private fun selectRandomColor() {
        val index = Random.nextInt(colorOptions.size)
        colorsAdapter.setChecked(index)
        viewModel.setLabelColor(colorOptions[index])
    }

    private fun toggleEditor(show: Boolean) {
        edit_label_layout.isVisible = show
        labels_list_view_parent.isVisible = !show
        labels_manager_parent_folder_text_view.isVisible = type == LabelType.FOLDER
        if (show.not()) {
            currentEditingLabel = null
            updateParentFolder(null)
        }
    }

    /**
     * Set text for the `Activity` regarding which type of entity is handling
     * @see [type]
     */
    private fun initTexts() {
        save_button.setText(
            when (type) {
                LabelType.MESSAGE_LABEL -> R.string.update_label
                LabelType.FOLDER -> R.string.update_folder
                else -> throw IllegalArgumentException("Unsupported type: $type")
            }
        )

        no_labels.setText(
            when (type) {
                LabelType.MESSAGE_LABEL -> R.string.no_labels
                LabelType.FOLDER -> R.string.no_folders
                else -> throw IllegalArgumentException("Unsupported type: $type")
            }
        )

        labels_dialog_title.setText(
            when (type) {
                LabelType.MESSAGE_LABEL -> R.string.label_add_new
                LabelType.FOLDER -> R.string.folder_add_new
                else -> throw IllegalArgumentException("Unsupported type: $type")
            }
        )

        label_name_text_view.setHint(
            when (type) {
                LabelType.MESSAGE_LABEL -> R.string.label_name
                LabelType.FOLDER -> R.string.folder_name
                else -> throw IllegalArgumentException("Unsupported type: $type")
            }
        )

        available_labels_title.setText(
            when (type) {
                LabelType.MESSAGE_LABEL -> R.string.available_labels
                LabelType.FOLDER -> R.string.available_folders
                else -> throw IllegalArgumentException("Unsupported type: $type")
            }
        )
    }

    private fun onLabelDeletedEvent(isSuccessful: Boolean) {
        val message = when (type) {
            LabelType.FOLDER -> if (isSuccessful) R.string.folder_deleted else R.string.folder_deleted_error
            LabelType.MESSAGE_LABEL -> if (isSuccessful) R.string.label_deleted else R.string.label_deleted_error
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }
        showToast(message, Toast.LENGTH_SHORT)
    }

    /** The current state of the `Activity` */
    enum class State {

        /** State is not defined */
        UNDEFINED,

        /** The `Activity` is currently creating a Label */
        CREATE,

        /** The `Activity` is currently updating a Label */
        UPDATE
    }
}
