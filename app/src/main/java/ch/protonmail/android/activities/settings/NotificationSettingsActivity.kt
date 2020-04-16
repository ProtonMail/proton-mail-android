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
package ch.protonmail.android.activities.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.user.MailSettingsEvent
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.moveToLogin
import com.squareup.otto.Subscribe
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_notification_settings.*
import studio.forface.viewstatestore.ViewStateActivity
import javax.inject.Inject

// region constants
private const val EXTRA_CURRENT_ACTION = "EXTRA_CURRENT_ACTION"
private const val REQUEST_CODE_PICK_RINGTONE = 5
// endregion

/**
 * An `Activity` for set Notification related Settings.
 * Inherit from [BaseActivity]
 * Implements [ViewStateActivity] for bind `ViewStateStore`s implicitly to the Lifecycle
 */

internal class NotificationSettingsActivity : BaseActivity(), ViewStateActivity {

    /** @return an instance of [getApplicationContext] casted as [ProtonMailApplication] */
    private val app get() = applicationContext as ProtonMailApplication

    /** TODO: doc */
    private var currentAction = 0

    /** An [Array] of [String] for the available options for notifications */
    private val notificationOptions by lazy {
        resources.getStringArray( R.array.notification_options )
    }

    /** [NotificationSettingsViewModel.Factory] for [NotificationSettingsViewModel] */
    @Inject lateinit var viewModelFactory: NotificationSettingsViewModel.Factory

    /** A Lazy instance of [NotificationSettingsViewModel] */
    private val viewModel by lazy {
        ViewModelProviders.of( this, viewModelFactory )
                .get( NotificationSettingsViewModel::class.java )
    }

    /** @return [LayoutRes] for the content View */
    override fun getLayoutId() = R.layout.activity_notification_settings

    override fun onCreate( savedInstanceState: Bundle? ) {
        AndroidInjection.inject( this )
        super.onCreate( savedInstanceState )

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled( true )

        currentAction = intent.getIntExtra( EXTRA_CURRENT_ACTION, 0 )
        createOptions()

        ringtone_settings.setOnClickListener { onRingtoneChooserClicked() }

        viewModel.ringtoneSettings.observe {
            doOnData( ::onRingtoneSettings )
            doOnError { showToast( it ) }
            doOnLoadingChange { /* TODO: show progress */ }
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

    /** Handle OptionsMenu selection */
    override fun onOptionsItemSelected( item: MenuItem ): Boolean {
        // Override of back soft button for save current progress
        if ( item.itemId == android.R.id.home ) {
            saveAndFinish()
            return true
        }
        return super.onOptionsItemSelected( item )
    }

    /** Override of [onBackPressed] for save current progress */
    override fun onBackPressed() {
        saveAndFinish()
    }

    /** Handle ringtone picking event */
    override fun onActivityResult( requestCode: Int, resultCode: Int, data: Intent? ) {
        if ( resultCode == Activity.RESULT_OK ) {
            if ( requestCode == REQUEST_CODE_PICK_RINGTONE ) {
                val uri = data!!.getParcelableExtra<Uri>( RingtoneManager.EXTRA_RINGTONE_PICKED_URI )
                viewModel.setRingtone( uri )
            }
        } else super.onActivityResult( requestCode, resultCode, data )
    }

    private fun createOptions() {
        // Create a map associating a generated view Id to notificationOptions
        val idOptionsMap = notificationOptions.associateBy { View.generateViewId() }

        // Create a list on RadioButton's with dividers
        idOptionsMap.map { (id, option) ->

            // Create a RadioButton
            val button = layoutInflater.inflate(
                    R.layout.swipe_list_item,
                    notification_radio_group,
                    false
            ) as RadioButton

            // Create a divider
            val divider = layoutInflater.inflate(
                    R.layout.horizontal_divider, notification_radio_group, false
            )

            // Setup the RadioButton
            button.apply {
                this.id = id
                text = option
                isChecked = option == notificationOptions[currentAction]
            }

            // Return button and divider
            return@map button to divider

        }.forEach { (button, divider) ->

            // Add button and divider to the RadioGroup
            notification_radio_group.apply {
                addView( button )
                addView( divider )
            }
        }

        notification_radio_group.setOnCheckedChangeListener { _, checkedId ->
            currentAction = notificationOptions.indexOf( idOptionsMap[checkedId] )
            toggleRingtoneContainerVisibility()

            val user = mUserManager.user
            val notificationSettingsChanged = currentAction != user.notificationSetting
            if (notificationSettingsChanged) {
                user.notificationSetting = currentAction

                user.save()
                mUserManager.user = user
            }
        }
    }

    /** Called when user requests to select a new ringtone for the Notification */
    private fun onRingtoneChooserClicked() {
        val currentRingtone = viewModel.currentRingtoneUri
        val intent = Intent( RingtoneManager.ACTION_RINGTONE_PICKER )
                .putExtra( RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION )
                .putExtra( RingtoneManager.EXTRA_RINGTONE_TITLE, getString( R.string.select_tone ) )
                .putExtra( RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentRingtone )

        // Open ringtone picker or show Toast
        intent.resolveActivity( packageManager )?.let {
            startActivityForResult( intent, REQUEST_CODE_PICK_RINGTONE )
        } ?: showToast( R.string.no_application_found )
    }

    /** When [RingtoneSettingsUiModel] is received from [NotificationSettingsViewModel] */
    private fun onRingtoneSettings( settings: RingtoneSettingsUiModel ) {
        toggleRingtoneContainerVisibility( settings.userOption )
        ringtone_title.text = settings.name
    }

    /** Save the current Action and the last interaction ( [saveLastInteraction] ), then [finish] */
    private fun saveAndFinish() {
        setResult(Activity.RESULT_OK)
        saveLastInteraction()
        finish()
    }

    /**
     * Toggle the visibility of [ringtone_container] whether the passed [Int]
     * @param someUnknownInt [Int] which entity is not clearly declared/
     * Default is [currentAction]
     */
    private fun toggleRingtoneContainerVisibility( someUnknownInt: Int = currentAction ) {
        ringtone_container.isVisible = someUnknownInt == 1 || someUnknownInt == 3
    }

    /** Subscribe to EventBut [LogoutEvent] */
    @Subscribe
    fun onLogoutEvent( event: LogoutEvent ) { // Could event param be removed?
        moveToLogin()
    }

    @Subscribe
    fun onMailSettingsEvent(event: MailSettingsEvent) {
        loadMailSettings()
    }
}