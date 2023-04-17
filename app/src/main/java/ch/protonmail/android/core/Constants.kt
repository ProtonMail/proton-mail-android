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
package ch.protonmail.android.core

import android.content.Context
import android.text.TextUtils

import ch.protonmail.android.R
import ch.protonmail.android.labels.domain.model.LabelId

object Constants {

    // region Urls
    const val HOST = "proton.me"
    const val API_HOST = "mail-api.$HOST"
    const val BASE_URL = "https://$API_HOST"
    const val DUMMY_URL_PREFIX = "http://androidlinksfix.protonmail.com"
    const val HUMAN_VERIFICATION_URL = "https://verify.proton.me"

    // Mail domains
    const val MAIL_DOMAIN_PM_ME = "pm.me"
    //endregion

    const val EMAIL_DELIMITER = ","

    const val DIR_EMB_ATTACHMENT_DOWNLOADS = "/ProtonMail/emb_att/"
    const val DIR_MESSAGE_BODY_DOWNLOADS = "/ProtonMail/messages/"

    const val FONTS_FOLDER = "fonts/"

    const val STORAGE_LIMIT_WARNING_PERCENTAGE: Long = 90

    const val CONTACTS_PAGE_SIZE = 1000

    // This is the app version in which the switch of the swipe gestures happens
    const val SWIPE_GESTURES_CHANGED_VERSION = 729
    // This is the version in which the FCM migration happens
    const val FCM_MIGRATION_VERSION = 739

    const val MAX_SQL_ARGUMENTS = 100 // max number of arguments allowed in an sql query
    const val MAX_MESSAGE_ID_WORKER_ARGUMENTS = 100 // max number of arguments allowed in a worker

    const val MAX_ATTACHMENTS = 100
    const val MAX_ATTACHMENT_FILE_SIZE_IN_BYTES = (25 * 1000 * 1000).toLong() // 25 MB
    const val MAX_ATTACHMENT_STORAGE_IN_MB = 1000
    const val DEFAULT_ATTACHMENT_STORAGE_IN_MB = 600
    const val UNLIMITED_ATTACHMENT_STORAGE = -1
    const val MAX_INCORRECT_PIN_ATTEMPTS = 10
    const val MAX_INTENT_STRING_SIZE = 200_000
    const val MIN_LOCAL_STORAGE_CLEARING_SIZE = 0.4

    // Indicates the first time when all clients should be signing all messages.
    const val PM_SIGNATURES_START = Integer.MAX_VALUE.toLong()

    const val TOKEN_SCOPE_FULL = "full"

    // MIME types
    const val MIME_TYPE_HTML = "text/html"
    const val MIME_TYPE_PLAIN_TEXT = "text/plain"
    const val MIME_TYPE_UNKNOWN_FILE = "application/octet-stream"
    const val MIME_TYPE_MULTIPART_MIXED = "multipart/mixed"

    // Response codes
    const val RESPONSE_CODE_OK = 1000
    const val RESPONSE_CODE_MULTIPLE_OK = 1001
    const val RESPONSE_CODE_API_OFFLINE = 7001

    // JobIntentService IDs
    const val JOB_INTENT_SERVICE_ID_EVENT_UPDATER = 870
    const val JOB_INTENT_SERVICE_ID_MESSAGES = 872
    const val JOB_INTENT_SERVICE_ID_ATTACHMENT_CLEARING = 874
    const val JOB_INTENT_SERVICE_ID_MESSAGE_BODY_CLEARING = 875

    // Job related constants
    const val JOB_RETRY_LIMIT_DEFAULT = 10
    const val JOB_GROUP_CONTACT = "contact"
    const val JOB_GROUP_LABEL = "label"
    const val JOB_GROUP_MESSAGE = "message"
    const val JOB_GROUP_BUGS = "bugs"
    const val JOB_GROUP_MISC = "misc"
    const val JOB_GROUP_PAYMENT = "payment"
    const val ERROR = "error"

    const val BYTE_TO_MEGABYTE_RATIO = 1_000_000

    object Prefs {
        const val PREF_APP_VERSION = "appVersion"
        const val PREF_PREVIOUS_APP_VERSION = "previousAppVersion"

        const val PREF_PM_ADDRESS_CHANGED = "pmAddressChanged"

        const val PREF_NEW_USER_ONBOARDING_SHOWN = "new_user_onboarding_shown"
        const val PREF_EXISTING_USER_ONBOARDING_SHOWN = "existing_user_welcome_to_new_brand_shown"

        // user
        const val PREF_ADDRESS = "address"
        const val PREF_ADDRESS_ID = "address_id"
        const val PREF_ALIASES = "ui_aliases"
        const val PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES = "allow_secure_connections_via_third_parties"
        const val PREF_AUTO_LOCK_PIN_PERIOD = "auto_lock_pin_period"
        const val PREF_BACKGROUND_SYNC = "background_sync"
        const val PREF_COMBINED_CONTACTS = "combined_contacts"
        const val PREF_DELINQUENT = "deliquent"
        const val PREF_DISPLAY_MOBILE = "ui_display_mobile"
        const val PREF_DISPLAY_NAME = "ui_display_name"
        const val PREF_DISPLAY_SIGNATURE = "ui_display_signature"
        const val PREF_DONT_SHOW_PLAY_SERVICES = "dont_show_play_services"
        const val PREF_GCM_DOWNLOAD_MESSAGE_DETAILS = "gcm_download_message_details"
        const val PREF_KEYS = "ui_keys"
        const val PREF_MAX_ATTACHMENT_STORAGE = "max_attachment_storage"
        const val PREF_MAX_SPACE = "ui_max_space"
        const val PREF_MAX_UPLOAD_FILE_SIZE = "ui_max_upload_file_size"
        const val PREF_MOBILE_FOOTER = "ui_mobile_signature"
        const val PREF_NOTIFICATION = "notification"
        const val PREF_NOTIFICATION_VISIBILITY_LOCK_SCREEN = "notification_lock_screen_int"
        const val PREF_RINGTONE = "ringtone"
        const val PREF_ROLE = "role"
        const val PREF_SUBSCRIBED = "subscribed"
        const val PREF_TIME_AND_DATE_CHANGED = "time_and_date_changed"
        const val PREF_USE_FINGERPRINT = "use_fingerprint_for_lock"
        const val PREF_USE_PIN = "use_pin_for_lock"
        const val PREF_USED_SPACE = "ui_used_space"
        const val PREF_USER_CREDIT = "user_credit"
        const val PREF_USER_CURRENCY = "user_currency"
        const val PREF_USER_ID = "user_id"
        const val PREF_USER_NAME = "user_name"
        const val PREF_USER_PRIVATE = "user_private"
        const val PREF_USER_SERVICES = "user_services"
        const val PREF_USER_INITIALIZED = "user_initialized"
        const val PREF_USING_REGULAR_API = "pref_doh_using_regular_api"

        // permissions
        const val PREF_PERMISSION_READ_CONTACTS = "pref_permission_contacts"
        const val PREF_PERMISSION_ACCESS_STORAGE = "pref_permission_storage"

        const val PREF_REGISTRATION_ID = "registration_id"
        const val PREF_SENT_TOKEN_TO_SERVER = "token_sent_to_server"

        const val PREF_SWIPE_GESTURES_DIALOG_SHOWN = "swipe_gestures_dialog_shown"
    }

    object PrefsType {

        const val DEFAULT = "default"
        const val BACKUP_PREFS_NAME = "backup_prefs"
    }

    // Enums
    enum class MessageLocationType(val messageLocationTypeValue: Int) {
        INVALID(-1),
        INBOX(0),
        ALL_DRAFT(1),
        ALL_SENT(2),
        TRASH(3),
        SPAM(4),
        ALL_MAIL(5),
        ARCHIVE(6),
        SENT(7),
        DRAFT(8),
        OUTBOX(9),
        STARRED(10),
        ALL_SCHEDULED(12),
        LABEL(77),
        SEARCH(99),
        LABEL_FOLDER(999);

        @Deprecated(
            message = "Passing `MessageLocationType` as a string is very error prone as clients might" +
                " mistakenly use it as a location's ID (`messageLocationTypeValue`).",
            replaceWith = ReplaceWith(
                "Pass the enum type and get it's value on the client instead " +
                    "or pass the .toString() value of `messageLocationTypeValue"
            ),
            level = DeprecationLevel.ERROR
        )
        override fun toString() = super.toString()

        fun asLabelId(): LabelId =
            LabelId(asLabelIdString())

        fun asLabelIdString(): String =
            messageLocationTypeValue.toString()

        companion object {

            fun fromInt(messageLocationTypeValue: Int): MessageLocationType {
                return values().find {
                    messageLocationTypeValue == it.messageLocationTypeValue
                } ?: INVALID
            }
        }
    }

    enum class DrawerOptionType(val drawerOptionTypeValue: Int) {
        DIVIDER(-1),
        INBOX(MessageLocationType.INBOX.messageLocationTypeValue),
        STARRED(MessageLocationType.STARRED.messageLocationTypeValue),
        DRAFTS(MessageLocationType.DRAFT.messageLocationTypeValue),
        ALL_DRAFTS(MessageLocationType.ALL_DRAFT.messageLocationTypeValue),
        SENT(MessageLocationType.SENT.messageLocationTypeValue),
        ALL_SENT(MessageLocationType.ALL_SENT.messageLocationTypeValue),
        SCHEDULED(MessageLocationType.ALL_SCHEDULED.messageLocationTypeValue),
        ARCHIVE(MessageLocationType.ARCHIVE.messageLocationTypeValue),
        TRASH(MessageLocationType.TRASH.messageLocationTypeValue),
        SPAM(MessageLocationType.SPAM.messageLocationTypeValue),
        LABEL(MessageLocationType.LABEL.messageLocationTypeValue),
        ALL_MAIL(MessageLocationType.ALL_MAIL.messageLocationTypeValue),
        CONTACTS(108),
        SETTINGS(109),
        REPORT_BUGS(101),
        SIGN_OUT(111),
        LOCK(112),
        SUBSCRIPTION(113),
    }

    enum class MessageActionType(val messageActionTypeValue: Int) {
        NONE(-1),
        REPLY(0),
        REPLY_ALL(1),
        FORWARD(2),
        FROM_URL(3);

        companion object {
            fun fromInt(messageActionTypeValue: Int): MessageActionType {
                return values().find {
                    messageActionTypeValue == it.messageActionTypeValue
                } ?: REPLY
            }
        }
    }

    enum class PlanType(val planTypeValue: String) {
        FREE("free"),
        PLUS("plus"),
        VISIONARY("visionary"),
        PROFESSIONAL("professional");

        companion object {
            fun fromString(planTypeValue: String?): PlanType {
                return values().find {
                    planTypeValue == it.planTypeValue
                } ?: FREE
            }
        }
    }

    enum class CurrencyType {
        EUR
    }

    enum class PermissionType {
        CONTACTS,
        STORAGE
    }

    enum class VCardOtherInfoType {
        ORGANIZATION,
        NICKNAME,
        TITLE,
        BIRTHDAY,
        ANNIVERSARY,
        ROLE,
        URL,
        GENDER,
        CUSTOM;

        companion object {
            fun fromName(name: String, context: Context): VCardOtherInfoType {
                if (TextUtils.isEmpty(name)) {
                    return CUSTOM
                }
                return when (name) {
                    context.getString(R.string.vcard_other_option_org) -> ORGANIZATION
                    context.getString(R.string.vcard_other_option_nickname) -> NICKNAME
                    context.getString(R.string.vcard_other_option_title) -> TITLE
                    context.getString(R.string.vcard_other_option_birthday) -> BIRTHDAY
                    context.getString(R.string.vcard_other_option_anniversary) -> ANNIVERSARY
                    context.getString(R.string.vcard_other_option_role) -> ROLE
                    context.getString(R.string.vcard_other_option_url) -> URL
                    context.getString(R.string.vcard_other_option_gender) -> GENDER
                    else -> CUSTOM
                }
            }
        }
    }

    enum class RecipientLocationType {
        TO,
        CC,
        BCC
    }

    enum class VCardType(val vCardTypeValue: Int) {
        UNSIGNED(0),
        ENCRYPTED(1),
        SIGNED(2),
        SIGNED_ENCRYPTED(3);
    }

    enum class ConnectionState {
        PING_NEEDED,
        CONNECTED,
        NO_INTERNET,
        CANT_REACH_SERVER;
    }
}
