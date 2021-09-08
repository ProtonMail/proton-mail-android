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

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity.State.CREATE
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity.State.UNDEFINED
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity.State.UPDATE
import ch.protonmail.android.adapters.LabelColorsAdapter
import ch.protonmail.android.adapters.LabelsAdapter
import ch.protonmail.android.labels.data.remote.worker.KEY_POST_LABEL_WORKER_RESULT_ERROR
import ch.protonmail.android.uiModel.LabelUiModel
import ch.protonmail.android.uiModel.LabelUiModel.Type.FOLDERS
import ch.protonmail.android.uiModel.LabelUiModel.Type.LABELS
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.onTextChange
import ch.protonmail.android.utils.extensions.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_labels_manager.*
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
 *
 * @author Davide Farella
 */
@AndroidEntryPoint
class LabelsManagerActivity : BaseActivity(), ViewStateActivity {

    /** Lazy instance of [LabelColorsAdapter] */
    private val colorsAdapter by lazy {
        LabelColorsAdapter(
            applicationContext, colorOptions,
            if (type == LABELS) R.layout.label_color_item_circle else R.layout.folder_color_item
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

    /** [LabelsAdapter] for show `Labels` or `Folders` */
    private val labelsAdapter = LabelsAdapter().apply {
        onItemClick = ::onLabelClick
        onItemSelect = ::onLabelSelectionChange
    }

    /** Whether this `Activity` must use a Popup Style. Default is `false` */
    private val popupStyle by lazy {
        intent?.extras?.getBoolean(EXTRA_POPUP_STYLE, false) ?: false
    }

    /** Current [State] of the `Activity` */
    private var state: State = UNDEFINED
        set(value) {
            if (field != value) {
                field = value
                onStateChange(value)
            }
        }

    /**
     * The [Type] handled by the `Activity`.
     * It represents which type of entity is currently managing
     * @see Type.FOLDERS
     * @see Type.LABELS
     */
    private val type by lazy {
        val managingFolders = intent?.extras?.getBoolean(EXTRA_MANAGE_FOLDERS, false) ?: false
        if (managingFolders) FOLDERS else LABELS
    }

    private val viewModel: LabelsManagerViewModel by viewModels()

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
                if (type == LABELS) R.string.labels_manager
                else R.string.folders_manager
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

        state = if (createOnly) CREATE else UNDEFINED

        // Set listeners
        delete_labels.setOnClickListener { showDeleteConfirmation() }
        label_name.onTextChange(::onLabelNameChange)
        save_button.setOnClickListener { saveCurrentLabel() }
        colors_grid_view.setOnItemClickListener { _, _, position, _ -> onLabelColorChange(position) }

        // Setup Labels RecyclerView
        labels_recycler_view.apply {
            adapter = labelsAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // Observe labels
        viewModel.labels.observeData(::onLabels)

        // Observe labels selection
        viewModel.hasSelectedLabels.observeData { delete_labels.isEnabled = it }

        // Observe deleted labels
        viewModel.hasSuccessfullyDeletedMessages.observe(
            this,
            { onLabelDeletedEvent(it) }
        )
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
        if (state != UNDEFINED && !createOnly) {
            state = UNDEFINED
        } else {
            saveLastInteraction()
            super.onBackPressed()
        }
    }

    /** Close the soft keyboard */
    private fun closeKeyboard() {
        UiUtil.hideKeyboard(this, label_name)
    }

    /** When labels are received from [LabelsManagerViewModel] */
    private fun onLabels(labels: PagedList<LabelUiModel>) {
        no_labels.isVisible = labels.isEmpty()
        delete_labels.isVisible = labels.isNotEmpty()
        labelsAdapter.submitList(labels)
    }

    /** When a Label is clicked */
    private fun onLabelClick(label: LabelUiModel) {
        state = UPDATE

        label_name.setText(label.name)
        add_label_container.isVisible = true
        toggleColorPicker(true)

        val currentColorPosition = colorOptions.indexOf(label.color)
        colorsAdapter.setChecked(currentColorPosition)

        // Set viewModel
        viewModel.apply {
            onLabelEdit(label)
            setLabelColor(colorOptions[currentColorPosition])
        }
    }

    /** When Label color is changed in the [colors_grid_view] */
    private fun onLabelColorChange(positionInColorOptions: Int) {
        colorsAdapter.setChecked(positionInColorOptions)
        viewModel.setLabelColor(colorOptions[positionInColorOptions])
        closeKeyboard()
    }

    /** When Label name is changed in the [label_name] `EditText` */
    private fun onLabelNameChange(name: CharSequence) {
        save_button.isVisible = name.isNotBlank()

        if (name.isNotEmpty() && state == UNDEFINED) {
            state = CREATE
        }

        viewModel.setLabelName(name)
    }

    /** When a Label is selected or unselected */
    private fun onLabelSelectionChange(label: LabelUiModel, isSelected: Boolean) {
        viewModel.onLabelSelected(
            label.labelId, isSelected
        )
    }

    /** When the [state] is changed */
    private fun onStateChange(state: State) {
        when (state) {

            UNDEFINED -> {
                viewModel.onNewLabel()
                toggleColorPicker(false)
                closeKeyboard()
                label_name.setText("")
                save_button.setText(R.string.done)
            }

            CREATE -> {
                selectRandomColor()
                toggleColorPicker(true)
                save_button.setText(R.string.done)
            }

            UPDATE -> {
                toggleColorPicker(true)

                save_button.setText(
                    when (type) {
                        LABELS -> R.string.update_label
                        FOLDERS -> R.string.update_folder
                    }
                )
            }
        }
    }

    /** Show a message that prompt the user to confirm the deletion */
    private fun showDeleteConfirmation() {
        val title = when (type) {
            LABELS -> R.string.delete_label_confirmation_title
            FOLDERS -> R.string.delete_folder_confirmation_title
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(R.string.delete_label_confirmation_message)
            .setPositiveButton(R.string.okay) { _, _ -> viewModel.deleteSelectedLabels() }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    private fun saveCurrentLabel() {
        viewModel.saveLabel().observe(
            this,
            {
                if (it.state.isFinished) {
                    displayLabelCreationOutcome(it)
                }
            }
        )

        state = UNDEFINED
    }

    private fun displayLabelCreationOutcome(workInfo: WorkInfo) {
        val success = workInfo.state == WorkInfo.State.SUCCEEDED
        val errorMessage = workInfo.outputData.getString(KEY_POST_LABEL_WORKER_RESULT_ERROR)

        if (success && createOnly) onBackPressed()

        colors_grid_view.isVisible = true

        val message = when (type) {
            FOLDERS -> {
                when (success) {
                    true -> getString(R.string.folder_created)
                    false -> errorMessage ?: getString(R.string.folder_invalid)
                }
            }
            LABELS -> {
                when (success) {
                    true -> getString(R.string.label_created)
                    false -> errorMessage ?: getString(R.string.label_invalid)
                }
            }
        }

        showToast(message, Toast.LENGTH_SHORT)
    }

    /** Select a random Color for the Label */
    private fun selectRandomColor() {
        val index = Random.nextInt(colorOptions.size)
        colorsAdapter.setChecked(index)
        viewModel.setLabelColor(colorOptions[index])
    }

    /** Show or hide the color picker */
    private fun toggleColorPicker(show: Boolean) {
        label_color_parent.isVisible = show
        labels_list_view_parent.isVisible = !show
    }

    /**
     * Set text for the `Activity` regarding which type of entity is handling
     * @see [type]
     */
    private fun initTexts() {
        save_button.setText(
            when (type) {
                LABELS -> R.string.update_label
                FOLDERS -> R.string.update_folder
            }
        )

        no_labels.setText(
            when (type) {
                LABELS -> R.string.no_labels
                FOLDERS -> R.string.no_folders
            }
        )

        labels_dialog_title.setText(
            when (type) {
                LABELS -> R.string.label_add_new
                FOLDERS -> R.string.folder_add_new
            }
        )

        label_name.setHint(
            when (type) {
                LABELS -> R.string.label_name
                FOLDERS -> R.string.folder_name
            }
        )

        available_labels_title.setText(
            when (type) {
                LABELS -> R.string.available_labels
                FOLDERS -> R.string.available_folders
            }
        )
    }

    private fun onLabelDeletedEvent(isSuccessful: Boolean) {
        val message = when (type) {
            FOLDERS -> if (isSuccessful) R.string.folder_deleted else R.string.folder_deleted_error
            LABELS -> if (isSuccessful) R.string.label_deleted else R.string.label_deleted_error
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
