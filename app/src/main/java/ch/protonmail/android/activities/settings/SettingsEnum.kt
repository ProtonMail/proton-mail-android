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

import android.content.Context
import ch.protonmail.android.R

enum class SettingsEnum {
    ACCOUNT {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = ""
    },
    SUBSCRIPTION {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.subscription)
    },
    PASSWORD_MANAGEMENT {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.password_manager)
    },
    RECOVERY_EMAIL {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.recovery_email)
    },
    MAILBOX_SIZE {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.storage)
    },
    CONVERSATION_MODE {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String =
            context.resources.getString(R.string.enable_conversation_mode)
    },
    DEFAULT_EMAIL {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.default_mail_address)
    },
    DISPLAY_NAME_N_SIGNATURE {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String =
            context.resources.getString(R.string.display_name_n_signature)
    },
    DISPLAY_NAME {

        override fun getHint(context: Context): String = context.resources.getString(R.string.edit_display_name)
        override fun getHeader(context: Context): String = context.resources.getString(R.string.display_name)
    },
    SIGNATURE {

        override fun getHint(context: Context): String = context.resources.getString(R.string.edit_settings)
        override fun getHeader(context: Context): String = context.resources.getString(R.string.signature)
    },
    MOBILE_SIGNATURE {

        override fun getHint(context: Context): String = context.resources.getString(R.string.edit_mobile_signature)
        override fun getHeader(context: Context): String = context.resources.getString(R.string.mobile_signature)
    },
    NOTIFICATION_SNOOZE {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.notifications_snooze)
    },
    PRIVACY {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.privacy)
    },
    AUTO_DOWNLOAD_MESSAGES {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String =
            context.resources.getString(R.string.auto_download_messages_title)
    },
    BACKGROUND_REFRESH {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String =
            context.resources.getString(R.string.settings_background_sync)
    },
    SEARCH {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.x_search)
    },
    LABELS_N_FOLDERS {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.labels_and_folders)
    },
    LABELS_MANAGER {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.labels_manage)
    },
    FOLDERS_MANAGER {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.folders_manage)
    },
    SWIPING_GESTURE {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.swiping_gesture)
    },
    SWIPE_LEFT {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.swipe_action_left)
    },
    SWIPE_RIGHT {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.swipe_action_right)
    },
    LOCAL_STORAGE_LIMIT {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.local_storage)
    },
    PUSH_NOTIFICATION {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.push_notifications)
    },
    NOTIFICATION_SETTINGS {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.notification_settings)
    },
    AUTO_LOCK {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.auto_lock)
    },
    APP_VERSION {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.app_version)
    },
    APP_LANGUAGE {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.app_language)
    },
    CONNECTIONS_VIA_THIRD_PARTIES {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String =
            context.resources.getString(R.string.connections_via_third_parties)
    },
    ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String =
            context.resources.getString(R.string.allow_secure_connections_via_third_parties)
    },
    COMBINED_CONTACTS {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.combined_contacts)
    },
    EXTENDED_NOTIFICATION {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.extended_notifications)
    },
    SHOW_EMBEDDED_IMAGES {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String =
            context.resources.getString(R.string.settings_auto_show_embedded_images)
    },
    SHOW_REMOTE_IMAGES {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String =
            context.resources.getString(R.string.settings_auto_show_images)
    },
    PREVENT_SCREENSHOTS {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String =
            context.resources.getString(R.string.settings_prevent_taking_screenshots)
    },
    LINK_CONFIRMATION {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.hyperlink_confirmation)
    },
    APP_LOCAL_CACHE {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.local_cache_management)
    },
    ACCOUNT_SETTINGS {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.account_settings)
    },
    APP_SETTINGS {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.app_settings)
    },
    APP_INFORMATION {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.app_info)
    },
    ACCOUNT_SECTION {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.account)
    },
    ADDRESSES {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.addresses)
    },
    SNOOZE {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.snooze_header)
    },
    MAILBOX {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String = context.resources.getString(R.string.mailbox)
    },
    BACKGROUND_SYNC {

        override fun getHint(context: Context): String = ""
        override fun getHeader(context: Context): String =
            context.resources.getString(R.string.settings_background_sync)
    }, ;

    abstract fun getHint(context: Context): String
    abstract fun getHeader(context: Context): String
}
