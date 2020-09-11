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
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity.State.CREATE
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity.State.UNDEFINED
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity.State.UPDATE
import ch.protonmail.android.adapters.LabelColorsAdapter
import ch.protonmail.android.adapters.LabelsCirclesAdapter
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.LabelAddedEvent
import ch.protonmail.android.events.LabelDeletedEvent
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.events.user.MailSettingsEvent
import ch.protonmail.android.uiModel.LabelUiModel
import ch.protonmail.android.uiModel.LabelUiModel.Type.FOLDERS
import ch.protonmail.android.uiModel.LabelUiModel.Type.LABELS
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.onTextChange
import ch.protonmail.android.utils.moveToLogin
import ch.protonmail.libs.core.utils.showToast
import com.squareup.otto.Subscribe
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

class LabelsManagerActivity : BaseActivity(), ViewStateActivity {

    /** @return an instance of [getApplicationContext] casted as [ProtonMailApplication] */
    private val app get() = applicationContext as ProtonMailApplication

    /** Lazy instance of [LabelColorsAdapter] */
    private val colorsAdapter by lazy {
        LabelColorsAdapter( applicationContext, colorOptions, R.layout.label_color_item_circle )
    }

    /** [IntArray] of the available colors */
    private val colorOptions by lazy { resources.getIntArray( R.array.label_colors ) }

    /**
     * Whether this `Activity` is enabled only to create a Label / Folder
     * Default is `false`
     */
    private val createOnly
            by lazy { intent?.extras?.getBoolean( EXTRA_CREATE_ONLY, false ) ?: false }

    /** [LabelsCirclesAdapter] for show `Labels` or `Folders` */
    private val labelsAdapter = LabelsCirclesAdapter().apply {
        onItemClick = ::onLabelClick
        onItemSelect = ::onLabelSelectionChange
    }

    /** Whether this `Activity` must use a Popup Style. Default is `false` */
    private val popupStyle
            by lazy { intent?.extras?.getBoolean( EXTRA_POPUP_STYLE, false ) ?: false }

    /** Current [State] of the `Activity` */
    private var state: State = UNDEFINED
        set( value ) {
            if ( field != value ) {
                field = value
                onStateChange( value )
            }
        }

    /**
     * The [Type] handled by the `Activity`.
     * It represents which type of entity is currently managing
     * @see Type.FOLDERS
     * @see Type.LABELS
     */
    private val type by lazy {
        val managingFolders = intent?.extras?.getBoolean( EXTRA_MANAGE_FOLDERS, false ) ?: false
        if ( managingFolders ) FOLDERS else LABELS
    }

    /** [LabelsManagerViewModel.Factory] for [LabelsManagerViewModel] */
    private val viewModelFactory by lazy {
        val messagesDatabase = MessagesDatabaseFactory.getInstance(applicationContext).getDatabase()
        val workManager = WorkManager.getInstance(applicationContext) // THis should be injected after refactor
        LabelsManagerViewModel.Factory(workManager, mJobManager, messagesDatabase, type) // TODO: use DI for create Factory with variable param ( type )
    }

    /** A Lazy instance of [LabelsManagerViewModel] */
    private val viewModel by lazy {
        ViewModelProviders.of( this, viewModelFactory )
                .get( LabelsManagerViewModel::class.java )
    }

    /** @return [LayoutRes] for the content View */
    override fun getLayoutId() = R.layout.activity_labels_manager

    override fun onCreate( savedInstanceState: Bundle? ) {
        // If is popupStyle set the relative theme
        if ( popupStyle ) setTheme( R.style.PopupTheme )
        super.onCreate( savedInstanceState )

        // Setup the action bar
        supportActionBar?.run {
            if ( popupStyle ) hide()
            else setDisplayHomeAsUpEnabled( true )
        }

        // Setup Views
        initTexts()
        delete_labels.isEnabled = false
        labels_grid_view.apply {
            val gridLayoutParams = layoutParams as LinearLayout.LayoutParams
            val itemHeight = resources.getDimensionPixelSize( R.dimen.label_color_item_size ) + resources.getDimensionPixelSize( R.dimen.fields_default_space )
            gridLayoutParams.height = colorOptions.size * itemHeight / COLOR_PICKER_COLUMNS

            adapter = colorsAdapter
        }

        if ( createOnly ) state = CREATE else UNDEFINED

        // Set listeners
        delete_labels.setOnClickListener { showDeleteConfirmation() }
        label_name.onTextChange( ::onLabelNameChange )
        save_new_label.setOnClickListener { saveCurrentLabel() }
        labels_grid_view
                .setOnItemClickListener { _, _, position, _ -> onLabelColorChange( position ) }

        // Setup Labels RecyclerView
        labels_recycler_view.apply {
            adapter = labelsAdapter
            layoutManager = LinearLayoutManager( context )
        }

        // Observe labels
        viewModel.labels.observeData( ::onLabels )

        // Observe labels selection
        viewModel.hasSelectedLabels.observeData { delete_labels.isEnabled = it }
    }

    /** Custom setup if [popupStyle] */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if ( popupStyle ) {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics( metrics )
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            window.setLayout( ( width * 0.99f ).toInt(), ( height * 0.95f ).toInt() )
        }
    }

    /** Register to EventBus */
    override fun onStart() {
        super.onStart()
        app.bus.register( this )
    }

    /** Unregister from EventBus */
    override fun onStop() {
        app.bus.unregister( this )
        super.onStop()
    }

    override fun onOptionsItemSelected( item: MenuItem ): Boolean {
        if ( item.itemId == android.R.id.home ) onBackPressed()
        return super.onOptionsItemSelected( item )
    }

    override fun onBackPressed() {
        if ( state != UNDEFINED && ! createOnly ) {
            state = UNDEFINED

        } else {
            saveLastInteraction()
            super.onBackPressed()
        }
    }

    /** Close the soft keyboard */
    private fun closeKeyboard() {
        UiUtil.hideKeyboard( this, label_name )
    }

    /** When labels are received from [LabelsManagerViewModel] */
    private fun onLabels( labels: PagedList<LabelUiModel> ) {
        no_labels.isVisible = labels.isEmpty()
        delete_labels.isVisible = labels.isNotEmpty()
        labelsAdapter.submitList( labels )
    }

    /** When a Label is clicked */
    private fun onLabelClick( label: LabelUiModel ) {
        state = UPDATE

        label_name.setText( label.name )
        add_label_container.isVisible = true
        toggleColorPicker( true )

        val currentColorPosition = colorOptions.indexOf( label.color )
        colorsAdapter.setChecked( currentColorPosition )

        // Set viewModel
        viewModel.apply {
            onLabelEdit( label )
            setLabelColor( colorOptions[currentColorPosition] )
        }
    }

    /** When Label color is changed in the [labels_grid_view] */
    private fun onLabelColorChange( positionInColorOptions: Int ) {
        colorsAdapter.setChecked( positionInColorOptions )
        viewModel.setLabelColor( colorOptions[positionInColorOptions] )
        closeKeyboard()
    }

    /** When Label name is changed in the [label_name] `EditText` */
    private fun onLabelNameChange( name: CharSequence ) {
        save_new_label.isVisible = name.isNotBlank()

        if ( name.isEmpty() ) {
            state = UNDEFINED

        } else if ( state == UNDEFINED ) state = CREATE

        viewModel.setLabelName( name )
    }

    /** When a Label is selected or unselected */
    private fun onLabelSelectionChange( label: LabelUiModel, isSelected: Boolean ) {
        viewModel.onLabelSelected( label.labelId, isSelected )
    }

    /** When the [state] is changed */
    private fun onStateChange( state: State ) {
        when ( state ) {

            UNDEFINED -> {
                viewModel.onNewLabel()
                toggleColorPicker( false )
                closeKeyboard()
                label_name.setText( "" )

                save_new_label.setText( when ( type ) {
                    LABELS -> R.string.save_new_label
                    FOLDERS -> R.string.save_new_folder
                } )
            }

            CREATE -> {
                selectRandomColor()
                toggleColorPicker( true )

                save_new_label.setText( when ( type ) {
                    LABELS -> R.string.save_new_label
                    FOLDERS -> R.string.save_new_folder
                } )
            }

            UPDATE -> {
                toggleColorPicker( true )

                save_new_label.setText( when ( type ) {
                    LABELS -> R.string.update_label
                    FOLDERS -> R.string.update_folder
                } )
            }
        }
    }

    /** Show a message that prompt the user to confirm the deletion */
    private fun showDeleteConfirmation() {
        val title = when ( type ) {
            LABELS -> R.string.delete_label_confirmation_title
            FOLDERS -> R.string.delete_folder_confirmation_title
        }
        AlertDialog.Builder( this )
                .setTitle( title )
                .setMessage( R.string.delete_label_confirmation_message )
                .setPositiveButton( R.string.okay ) { _, _ -> viewModel.deleteSelectedLabels() }
                .setNegativeButton( R.string.cancel ) { _, _ -> }
                .show()
    }

    /** Save the current editing Label */
    private fun saveCurrentLabel() {
        viewModel.saveLabel()
        state = UNDEFINED
    }

    /** Select a random Color for the Label */
    private fun selectRandomColor() {
        val index = Random.nextInt( colorOptions.size )
        colorsAdapter.setChecked( index )
        viewModel.setLabelColor( colorOptions[index] )
    }

    /** Show or hide the color picker */
    private fun toggleColorPicker( show: Boolean ) {
        label_color_parent.isVisible = show
        labels_list_view_parent.isVisible = !show
    }

    /**
     * Set text for the `Activity` regarding which type of entity is handling
     * @see [type]
     */
    private fun initTexts() {
        setTitle( when ( type ) {
            LABELS -> R.string.labels_and_folders
            FOLDERS -> R.string.folders_manager
        } )

        save_new_label.setText( when ( type ) {
            LABELS -> R.string.save_new_label
            FOLDERS -> R.string.save_new_folder
        } )

        no_labels.setText( when ( type ) {
            LABELS -> R.string.no_labels
            FOLDERS -> R.string.no_folders
        } )

        labels_dialog_title.setText( when ( type ) {
            LABELS -> R.string.label_add_new
            FOLDERS -> R.string.folder_add_new
        } )

        label_name.setHint( when ( type ) {
            LABELS -> R.string.label_name
            FOLDERS -> R.string.folder_name
        } )

        available_labels_title.setText( when ( type ) {
            LABELS -> R.string.available_labels
            FOLDERS -> R.string.available_folders
        } )

        labels_colors_title.setText( when ( type ) {
            LABELS -> R.string.choose_label_color
            FOLDERS -> R.string.choose_folder_color
        } )
    }

    @Subscribe
    @Suppress("unused") // EventBus
    // TODO: Manage it with updates from DeleteLabelWorker
    fun onLabelDeletedEvent( event: LabelDeletedEvent ) {
        val success = event.status == Status.SUCCESS
        val message = when ( type ) {
            FOLDERS -> if ( success ) R.string.folder_deleted else R.string.folder_deleted_error
            LABELS -> if ( success ) R.string.label_deleted else R.string.label_deleted_error
        }

        showToast( message, Toast.LENGTH_SHORT )
    }

    @Subscribe
    @Suppress("unused") // EventBus
    fun onLabelAddedEvent( event: LabelAddedEvent ) {
        val success = event.status == Status.SUCCESS
        val error = event.error
        if ( success && createOnly ) onBackPressed()

        labels_grid_view.isVisible = true

        val message = when ( type ) {
            FOLDERS -> {
                when (success) {
                    true -> getString(R.string.folder_created)
                    false -> error ?: getString(R.string.folder_invalid)
                }
            }
            LABELS -> {
                when (success) {
                    true -> getString(R.string.label_created)
                    false -> error ?: getString(R.string.label_invalid)
                }
            }
        }

        showToast(message!!, Toast.LENGTH_SHORT)
    }

    @Subscribe
    @Suppress("unused") // EventBus
    fun onLogoutEvent( @Suppress("UNUSED_PARAMETER") event: LogoutEvent ) {
        moveToLogin()
    }

    @Subscribe
    fun onMailSettingsEvent(event: MailSettingsEvent) {
        loadMailSettings()
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
