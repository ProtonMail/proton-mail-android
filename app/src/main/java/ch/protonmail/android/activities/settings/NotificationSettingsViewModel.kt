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

import android.app.Application
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_NOTIFICATION
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.R
import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.exceptions.InvalidRingtoneException
import ch.protonmail.android.exceptions.NoDefaultRingtoneException
import ch.protonmail.android.utils.extensions.isEmpty
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.forface.viewstatestore.ViewStateStore
import studio.forface.viewstatestore.ViewStateStoreScope
import java.io.File

/**
 * A [ViewModel] for Notification's Settings
 * Inherit from [AndroidViewModel] for handle eventual exceptions on [Ringtone] management
 * Implements [ViewStateStoreScope] for being able to publish to a Locked [ViewStateStore]
 */
internal class NotificationSettingsViewModel(
    application: Application,
    userManager: UserManager
) : AndroidViewModel(application), ViewStateStoreScope {

    private companion object {

        /**
         * [Uri] of default ringtone from [RingtoneManager]
         * This [Uri] could be [Uri.EMPTY]
         */
        val DEFAULT_RINGTONE_URI: Uri =
            RingtoneManager.getDefaultUri(TYPE_NOTIFICATION) ?: Uri.EMPTY
    }

    /** @return [Context] from [getApplication] */
    private val context: Context get() = getApplication()

    /**
     * @return [Uri] of the current ringtone, whether from [user] or default one
     * This [Uri] could be [Uri.EMPTY]
     */
    val currentRingtoneUri get() = user.ringtone ?: DEFAULT_RINGTONE_URI

    /** A Locked [ViewStateStore] of type [RingtoneSettingsUiModel] */
    val ringtoneSettings = ViewStateStore<RingtoneSettingsUiModel>().lock

    /** Lazy instance of [User] from [UserManager] */
    private val user by lazy { userManager.requireCurrentLegacyUser() }

    init {
        sendRingtoneSettings()
    }

    /** @return [RingtoneSettingsUiModel] */
    @VisibleForTesting
    internal fun createRingtoneSettings(): RingtoneSettingsUiModel {
        val ringtoneTitle = if (currentRingtoneUri.isEmpty()) {
            ringtoneSettings.setError(NoDefaultRingtoneException())
            NONE

        } else {
            // Try to getUserRingtone else getDefaultRingtone
            val ringtone = try {
                getUserRingtone() // could be null
            } catch (e: SecurityException) {
                ringtoneSettings.setError(InvalidRingtoneException(e, currentRingtoneUri))
                null
            } ?: getDefaultRingtone()

            ringtone.title
        }

        return RingtoneSettingsUiModel(user.notificationSetting, ringtoneTitle)
    }

    /**
     * @return [Ringtone] from [DEFAULT_RINGTONE_URI]
     * @throws AssertionError if [currentRingtoneUri] is empty
     */
    private fun getDefaultRingtone(): Ringtone {
        if (currentRingtoneUri.isEmpty()) throw AssertionError(
            "'${::currentRingtoneUri.name}' is empty. Check 'Uri.isEmpty()' before call this"
        )
        return RingtoneManager.getRingtone(context, currentRingtoneUri)
    }

    /**
     * @return OPTIONAL [Ringtone] if [currentRingtoneUri] is not [DEFAULT_RINGTONE_URI] else `null`
     * @throws SecurityException
     * @throws AssertionError if [currentRingtoneUri] is empty
     */
    @VisibleForTesting
    internal fun getUserRingtone(): Ringtone? {
        if (currentRingtoneUri.isEmpty()) throw AssertionError(
            "'${::currentRingtoneUri.name}' is empty. Check 'Uri.isEmpty()' before call this"
        )

        return if (currentRingtoneUri != DEFAULT_RINGTONE_URI) {
            RingtoneManager.getRingtone(context, currentRingtoneUri)
        } else null
    }

    /** Create and publish a [RingtoneSettingsUiModel] from the current [user]s Setting */
    private fun sendRingtoneSettings() {
        ringtoneSettings.setData(createRingtoneSettings())
    }

    /** Set new ringtone [Uri] to [User] and refresh data */
    fun setRingtone(uri: Uri) {
        ringtoneSettings.setLoading()
        viewModelScope.launch {
            try {
                val safeUri = withContext(IO) { storeToPrivateIfFileScheme(uri) }
                user.ringtone = safeUri
                sendRingtoneSettings()
            } catch (t: Throwable) {
                ringtoneSettings.setError(InvalidRingtoneException(t, uri))
            }
        }
    }

    /**
     * If the given [Uri] has a **file** scheme, store the relative file into a private folder
     * @return original [Uri] if it safe, else the [Uri] from file just copied to private location
     * @throws ( May throws exception )
     */
    @Suppress("RedundantSuspendModifier") // We don't wanna run it on UI
    private suspend fun storeToPrivateIfFileScheme(uri: Uri): Uri {
        // Return same uri if scheme is not file
        if (uri.scheme != "file") return uri

        val directory = context.filesDir
        val file = File(directory, "cachedNotificationRingtone")
        val output = file.outputStream()
        context.contentResolver.openInputStream(uri)!!.use { it.copyTo(output) }
        return FileProvider.getUriForFile(context, context.packageName, file)
    }

    /** [ViewModelProvider.NewInstanceFactory] for [NotificationSettingsViewModel] */
    class Factory(
        private val application: Application,
        private val userManager: UserManager
    ) : ViewModelProvider.NewInstanceFactory() {

        /** @return new instance of [NotificationSettingsViewModel] casted as T */
        @Suppress("UNCHECKED_CAST") // NotificationSettingsViewModel is T, since T is ViewModel
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NotificationSettingsViewModel::class.java))
            return NotificationSettingsViewModel(application, userManager) as T
        }
    }

    /** A [CharSequence] representing [R.string.x_none] resource */
    @Suppress("PrivatePropertyName")
    private val NONE = context.getString(R.string.x_none)

    /** @return [String] from title of [Ringtone] */
    private val Ringtone.title get() = getTitle(context)
}

/**
 * Ui Model for Ringtone settings.
 *
 * @param userOption [Int]
 * @see User.NotificationSetting
 */
internal data class RingtoneSettingsUiModel(
    val userOption: Int,
    val name: CharSequence
)
