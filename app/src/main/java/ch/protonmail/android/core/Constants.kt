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
package ch.protonmail.android.core

import android.content.Context
import android.text.TextUtils

import ch.protonmail.android.R

object Constants {

    // region Urls
    const val REDIRECT_URI = "https://protonmail.ch"
    const val ENDPOINT_URI = "https://api.protonmail.ch/"
    const val DUMMY_URL_PREFIX = "http://androidlinksfix.protonmail.com"
    const val SECURE_ENDPOINT_HOST = "secure.protonmail.com"

    // Mail domains
    const val MAIL_DOMAIN_CH = "protonmail.ch"
    const val MAIL_DOMAIN_COM = "protonmail.com"
    const val MAIL_DOMAIN_PM_ME = "pm.me"
    //endregion

    const val EMAIL_DELIMITER = ","

    const val BEGIN_PGP = "-----BEGIN PGP MESSAGE-----"
    const val END_PGP = "-----END PGP MESSAGE-----"

    const val DIR_EMB_ATTACHMENT_DOWNLOADS = "/ProtonMail/emb_att/"
    const val DIR_ATTACHMENT_DOWNLOADS = "/"
    const val DIR_MESSAGE_BODY_DOWNLOADS = "/ProtonMail/messages/"

    const val FONTS_FOLDER = "fonts/"

    const val HIGH_SECURITY_BITS = 2048
    const val EXTREME_SECURITY_BITS = 4096

    const val STORAGE_LIMIT_WARNING_PERCENTAGE: Long = 90

    const val CONTACTS_PAGE_SIZE = 1000

    // This is the app version in which the switch of the swipe gestures happens
    const val SWIPE_GESTURES_CHANGED_VERSION = 729
    // This is the version in which the FCM migration happens
    const val FCM_MIGRATION_VERSION = 739

    const val MAX_ATTACHMENTS = 100
    const val MAX_ATTACHMENT_FILE_SIZE_IN_BYTES = (25 * 1000 * 1000).toLong() // 25 MB
    const val MAX_ATTACHMENT_STORAGE_IN_MB = 1000
    const val MIN_ATTACHMENT_STORAGE_IN_MB = 200
    const val MAX_EMAILS_PER_CONTACT_GROUP = 25
    const val MAX_USERNAME_LENGTH = 40
    const val MAX_INCORRECT_PIN_ATTEMPTS = 10
    const val MAX_INTENT_STRING_SIZE = 200000
    const val MIN_LOCAL_STORAGE_CLEARING_SIZE = 0.4

    // Indicates the first time when all clients should be signing all messages.
    const val PM_SIGNATURES_START = Integer.MAX_VALUE.toLong()

    const val TOKEN_TYPE = "Bearer"
    const val TOKEN_SCOPE_FULL = "full"
    const val TOKEN_SCOPE_SELF = "self"

    // MIME types
    const val MIME_TYPE_HTML = "text/html"
    const val MIME_TYPE_PLAIN_TEXT = "text/plain"
    const val MIME_TYPE_UNKNOWN_FILE = "application/octet-stream"
    const val MIME_TYPE_MULTIPART_MIXED = "multipart/mixed"

    // Password types
    const val PASSWORD_TYPE_LOGIN = 0
    const val PASSWORD_TYPE_MAILBOX = 1

    // Label types
    const val LABEL_TYPE_MESSAGE = 1
    const val LABEL_TYPE_CONTACT_GROUPS = 2

    // Address types
    const val ADDRESS_TYPE_PRIMARY = 1

    // Response codes
    const val RESPONSE_CODE_OK = 1000
    const val RESPONSE_CODE_MULTIPLE_OK = 1001
    const val RESPONSE_CODE_API_OFFLINE = 7001

    // JobIntentService IDs
    const val JOB_INTENT_SERVICE_ID_EVENT_UPDATER = 870
    const val JOB_INTENT_SERVICE_ID_MESSAGES = 872
    const val JOB_INTENT_SERVICE_ID_LOGOUT = 873
    const val JOB_INTENT_SERVICE_ID_ATTACHMENT_CLEARING = 874
    const val JOB_INTENT_SERVICE_ID_MESSAGE_BODY_CLEARING = 875
    const val JOB_INTENT_SERVICE_ID_LOGIN = 876
    const val JOB_INTENT_SERVICE_ID_REGISTRATION = 877

    // Job related constants
    const val JOB_RETRY_LIMIT_DEFAULT = 10
    const val JOB_GROUP_CONTACT = "contact"
    const val JOB_GROUP_LABEL = "label"
    const val JOB_GROUP_MESSAGE = "message"
    const val JOB_GROUP_BUGS = "bugs"
    const val JOB_GROUP_MISC = "misc"
    const val JOB_GROUP_PAYMENT = "payment"
    const val JOB_GROUP_SENDING = "sending"
    const val ERROR = "error"

    object Prefs {
        const val PREF_APP_VERSION = "appVersion"
        const val PREF_PREVIOUS_APP_VERSION = "previousAppVersion"

        const val PREF_VERIFY_CODE = "verifyCode"
        const val PREF_CONTACTS_LOADING = "contactsLoading"
        const val PREF_PM_ADDRESS_CHANGED = "pmAddressChanged"
        const val PREF_HYPERLINK_CONFIRM = "confirmHyperlinks"

        // user
        const val PREF_ADDRESS = "address"
        const val PREF_ADDRESS_ID = "address_id"
        const val PREF_ALIASES = "ui_aliases"
        const val PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES = "allow_secure_connections_via_third_parties"
        const val PREF_AUTO_LOCK_PIN_PERIOD = "auto_lock_pin_period"
        const val PREF_AUTO_LOGOUT = "auto_logout"
        const val PREF_BACKGROUND_SYNC = "background_sync"
        const val PREF_COMBINED_CONTACTS = "combined_contacts"
        const val PREF_DELINQUENT = "deliquent"
        const val PREF_DISPLAY_MOBILE = "ui_display_mobile"
        const val PREF_DISPLAY_NAME = "ui_display_name"
        const val PREF_DISPLAY_SIGNATURE = "ui_display_signature"
        const val PREF_DONT_SHOW_PLAY_SERVICES = "dont_show_play_services"
        const val PREF_GCM_DOWNLOAD_MESSAGE_DETAILS = "gcm_download_message_details"
        const val PREF_KEYS = "ui_keys"
        const val PREF_LAST_INTERACTION = "last_interaction"
        const val PREF_MANUALLY_LOCKED = "manually_locked"
        const val PREF_MAX_ATTACHMENT_STORAGE = "max_attachment_storage"
        const val PREF_MAX_SPACE = "ui_max_space"
        const val PREF_MAX_UPLOAD_FILE_SIZE = "ui_max_upload_file_size"
        const val PREF_MOBILE_SIGNATURE = "ui_mobile_signature"
        const val PREF_NOTIFICATION = "notification"
        const val PREF_NOTIFICATION_EMAIL = "ui_notification_email"
        const val PREF_NOTIFICATION_VISIBILITY_LOCK_SCREEN = "notification_lock_screen_int"
        const val PREF_NUM_MESSAGE_PER_PAGE = "ui_num_message_per_page"
        const val PREF_PREVENT_TAKING_SCREENSHOTS = "prevent_taking_screenshots"
        const val PREF_RINGTONE = "ringtone"
        const val PREF_ROLE = "role"
        const val PREF_SIGNATURE = "ui_signature"
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
        const val PREF_USING_REGULAR_API = "pref_doh_using_regular_api"
        const val PREF_USER_LEGACY_ACCOUNT = "user_legacy_account"

        // permissions
        const val PREF_PERMISSION_READ_CONTACTS = "pref_permission_contacts"
        const val PREF_PERMISSION_ACCESS_STORAGE = "pref_permission_storage"

        const val PREF_REGISTRATION_ID = "registration_id"
        const val PREF_SENT_TOKEN_TO_SERVER = "token_sent_to_server"

        const val PREF_SWIPE_GESTURES_DIALOG_SHOWN = "swipe_gestures_dialog_shown"

        object UserSettings {
            const val PREF_PASSWORD_MODE = "user_settings_PasswordMode"
            const val PREF_NEWS = "user_settings_News"
            const val PREF_LOCALE = "user_settings_Locale"
            const val PREF_LOG_AUTH = "user_settings_LogAuth"
            const val PREF_INVOICE_TEXT = "user_settings_InvoiceText"
            const val PREF_TWO_FACTOR = "user_settings_TwoFactor"
            const val PREF_EMAIL = "user_settings_Email"
        }
    }

    object LogTags {
        const val SENDING_FAILED_TAG = "SENDINGFAILEDv3"
        const val SENDING_FAILED_REASON_TAG = "SENDINGFAILEDREASON"
        const val SENDING_FAILED_DEVICE_TAG = "SENDINGFAILEDDEVICE"
        const val SENDING_FAILED_SAME_USER_TAG = "SENDINGFAILEDSAMEUSER"
    }

    object PrefsType {
        const val BACKUP = "backup"
        const val DEFAULT = "default"
        const val BACKUP_PREFS_NAME = "backup_prefs"
    }

    object FeatureFlags {
        const val SAVE_MESSAGE_BODY_TO_FILE = true
        const val CUSTOM_LANGUAGE_SELECTION = true
        const val HYPERLINK_CONFIRMATION = true
        const val PAYPAL_PAYMENT = false
        const val TLS_12_UPGRADE = true
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
        STARRED(10),
        LABEL(77),
        LABEL_OFFLINE(88),
        SEARCH(99),
        LABEL_FOLDER(999);

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
        SENT(MessageLocationType.SENT.messageLocationTypeValue),
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
        UPSELLING(113),
        ACCOUNT_MANAGER(115)
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

    enum class AccountType {
        FREE,
        PLUS,
        VISIONARY,
        BUSINESS
    }

    enum class PlanType(val planTypeValue: String) {
        FREE("free"),
        PLUS("plus"),
        VISIONARY("visionary"),
        BUSINESS("business"),
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
        USD,
        CHF,
        EUR
    }

    enum class PaymentCycleType(val paymentCycleTypeValue: Int) {
        MONTHLY(1),
        YEARLY(12)
    }

    enum class BillingType {
        CREATE,
        UPGRADE,
        DONATE
    }

    enum class TokenType(val tokenTypeValue: String) {
        SMS("sms"),
        EMAIL("email"),
        PAYMENT("payment"),
        CAPTCHA("captcha");

        companion object {
            fun fromString(tokenTypeValue: String): TokenType {
                return values().find {
                    tokenTypeValue == it.tokenTypeValue
                } ?: SMS
            }
        }
    }

    enum class PermissionType {
        CONTACTS,
        STORAGE
    }

    enum class PasswordMode(val passwordModeValue: Int) {
        SINGLE(1),
        DUAL(2);

        companion object {
            fun fromInt(passwordModeValue: Int): PasswordMode {
                return values().find {
                    passwordModeValue == it.passwordModeValue
                } ?: SINGLE
            }
        }
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
        SIGNED(2),
        SIGNED_ENCRYPTED(3);
    }
}
