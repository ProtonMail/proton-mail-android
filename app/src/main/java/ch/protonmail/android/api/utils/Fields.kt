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
package ch.protonmail.android.api.utils

/**
 * Created by dkadrikj on 16.7.15.
 * Java and Android code style requires to have the properties of a model classes named with
 * small first letter. The API is requiring the fields with an Capital letter, so we are defining
 * the required names of the field in this class, having them in one place, and referencing them
 * where needed.
 */
object Fields {

    object General {
        const val DOMAINS = "Domains"
        const val COUNTS = "Counts"
    }

    object Label {
        const val NAME = "Name"
        const val COLOR = "Color"
        const val DISPLAY = "Display"
        const val ORDER = "Order"
        const val EXCLUSIVE = "Exclusive"
        const val TYPE = "Type"
        const val NOTIFY = "Notify"
    }

    object Payment {
        const val TYPE = "Type"
        const val DETAILS = "Details"
        const val NUMBER = "Number"
        const val EXPIRATION_MONTH = "ExpMonth"
        const val EXPIRATION_YEAR = "ExpYear"
        const val CVC = "CVC"
        const val NAME = "Name"
        const val COUNTRY = "Country"
        const val ZIP = "ZIP"
        const val ID = "ID"
        const val BRAND = "Brand"
        const val LAST_4 = "Last4"
        const val BILLING_AGREEMENT_ID = "BillingAgreementID"
        const val PAYER = "Payer"
        const val AMOUNT = "Amount"
        const val CURRENCY = "Currency"
        const val COUPON_CODE = "CouponCode"
        const val PLAN_IDS = "PlanIDs"
        const val PAYMENT = "Payment"
        const val PAYMENT_METHODS = "PaymentMethods"
        const val STRIPE = "Stripe"
        const val PAYPAL = "Paypal"
        const val PAYMENT_METHOD = "PaymentMethod"
        const val CYCLE = "Cycle"
        const val TOKEN = "Token"
    }

    object Subscription {
        const val SUBSCRIPTION = "Subscription"
        const val ID = "ID"
        const val INVOICE_ID = "InvoiceID"
        const val CYCLE = "Cycle"
        const val PERIOD_START = "PeriodStart"
        const val PERIOD_END = "PeriodEnd"
        const val COUPON_CODE = "CouponCode"
        const val CURRENCY = "Currency"
        const val AMOUNT = "Amount"
        const val AMOUNT_DUE = "AmountDue"
        const val PLANS = "Plans"
        const val PRORATION = "Proration"
        const val COUPON_DISCOUNT = "CouponDiscount"
        const val CREDIT = "Credit"
        const val CODE = "Code"
        const val DESCRIPTION = "Description"
        const val COUPON = "Coupon"

        object Plan {
            const val ID = "ID"
            const val TYPE = "Type"
            const val CYCLE = "Cycle"
            const val NAME = "Name"
            const val TITLE = "Title"
            const val CURRENCY = "Currency"
            const val AMOUNT = "Amount"
            const val MAX_DOMAINS = "MaxDomains"
            const val MAX_ADDRESSES = "MaxAddresses"
            const val MAX_SPACE = "MaxSpace"
            const val MAX_MEMBERS = "MaxMembers"
            const val TWO_FACTOR = "TwoFactor"
            const val QUANTITY = "Quantity"
        }

    }

    object Bugs {
        const val OS = "OS"
        const val OS_VERSION = "OSVersion"
        const val CLIENT = "Client"
        const val CLIENT_VERSION = "ClientVersion"
        const val TITLE = "Title"
        const val DESCRIPTION = "Description"
        const val USERNAME = "Username"
        const val EMAIL = "Email"
    }

    object Message {
        const val MESSAGE_IDS = "MessageIDs"
        const val LABEL_ID = "LabelID"
        const val CURRENT_LABEL_ID = "CurrentLabelID"
        const val IDS = "IDs"
        const val MESSAGE = "Message"

        const val TO_LIST = "ToList"
        const val BCC_LIST = "BCCList"
        const val CC_LIST = "CCList"
        const val SUBJECT = "Subject"
        const val MESSAGE_BODY = "Body"
        const val ADDRESS_ID = "AddressID"
        const val PARENT_ID = "ParentID"
        const val ACTION = "Action"
        const val SELF = "self"
        const val TOTAL = "Total"
        const val SENDER = "Sender"
        const val ID = "ID"
        const val UNREAD = "Unread"
        const val CONVERSATION_ID = "ConversationID"

        object Send {
            const val EXPIRES_IN = "ExpiresIn"
            const val AUTO_SAVE_CONTACTS = "AutoSaveContacts"
            const val TYPE = "Type"
            const val BODY_KEY_PACKET = "BodyKeyPacket"
            const val ATTACHMENT_KEY_PACKETS = "AttachmentKeyPackets"
            const val SIGNATURE = "Signature"
            const val TOKEN = "Token"
            const val ENC_TOKEN = "EncToken"
            const val AUTH = "Auth"
            const val ADDRESSES = "Addresses"
            const val BODY = "Body"
            const val MIME_TYPE = "MIMEType"
            const val KEY = "Key"
            const val ALGORITHM = "Algorithm"
            const val BODY_KEY = "BodyKey"
            const val ATTACHMENT_KEYS = "AttachmentKeys"
            const val PACKAGES = "Packages"
            const val PASSWORD_HINT = "PasswordHint"
        }

        object Sender {
            const val NAME = "Name"
            const val ADDRESS = "Address"
        }
    }

    object Unread {
        const val UNREAD = "Unread"
    }

    object Attachment {
        const val ATTACHMENT = "Attachment"
        const val MESSAGE_ID = "MessageID"
        const val ATTACHMENT_ID = "AttachmentID"
        const val CONTENT_TYPE = "content-type"
        const val CONTENT_TRANSFER_ENCODING = "content-transfer-encoding"
        const val CONTENT_DISPOSITION = "content-disposition"
        const val CONTENT_ID = "content-id"
        const val CONTENT_LOCATION = "content-location"
        const val CONTENT_ENCRYPTION = "x-pm-content-encryption"
        const val SIZE = "Size"
    }

    object User {
        const val CREDIT = "Credit"
        const val CURRENCY = "Currency"
        const val DELINQUENT = "Delinquent"
        const val DIRECT = "Direct"
        const val EMAIL = "Email"
        const val ID = "ID"
        const val INVOICE_TEXT = "InvoiceText"
        const val KEYS = "Keys"
        const val LOCALE = "Locale"
        const val LOG_AUTH = "LogAuth"
        const val MAX_SPACE = "MaxSpace"
        const val MAX_UPLOAD = "MaxUpload"
        const val NAME = "Name"
        const val NEWS = "News"
        const val NOTIFY = "Notify"
        const val PASSWORD_MODE = "PasswordMode"
        const val PRIVATE = "Private"
        const val RESET = "Reset"
        const val ROLE = "Role"
        const val SERVICES = "Services"
        const val SUBSCRIBED = "Subscribed"
        const val STATUS = "Status"
        const val TOKEN = "Token"
        const val TOKEN_TYPE = "TokenType"
        const val TWO_FACTOR = "TwoFactor"
        const val USED_SPACE = "UsedSpace"
        const val USER = "User"
        const val VALUE = "Value"
        const val VERIFY_METHODS = "VerifyMethods"
    }

    object Country {
        const val COUNTRY_CODE = "country_code"
        const val COUNTRY_NAME = "country_en"
        const val CODE = "phone_code"
    }

    object Organization {
        const val DISPLAY_NAME = "DisplayName"
        const val PRIVATE_KEY = "PrivateKey"
        const val BACKUP_PRIVATE_KEY = "BackupPrivateKey"
        const val PLAN_NAME = "PlanName"
        const val VPN_PLAN_NAME = "VPNPlanName"
        const val MAX_DOMAINS = "MaxDomains"
        const val MAX_ADDRESSES = "MaxAddresses"
        const val MAX_SPACE = "MaxSpace"
        const val MAX_MEMBERS = "MaxMembers"
        const val MAX_VPN = "MaxVPN"
        const val TWO_FACTOR_GRACE_PERIOD = "TwoFactorGracePeriod"
        const val USED_DOMAINS = "UsedDomains"
        const val USED_MEMBERS = "UsedMembers"
        const val USED_ADDRESSES = "UsedAddresses"
        const val USED_SPACE = "UsedSpace"
        const val ASSIGNED_SPACE = "AssignedSpace"
        const val ORGANIZATION = "Organization"
    }

    object Auth {
        const val AUTH = "Auth"
        const val VERSION = "Version"
        const val MODULUS_ID = "ModulusID"
        const val SALT = "Salt"
        const val VERIFIER = "Verifier"
        const val KEY_SALT = "KeySalt"
        const val KEY_SALTS = "KeySalts"
        const val KEY_ID = "ID"
        const val KEYS = "Keys"
        const val USER_KEYS = "UserKeys"
        const val ORGANIZATION_KEY = "OrganizationKey"

        const val DOMAIN_ID = "DomainID"
        const val ID = "ID"
        const val EMAIL = "Email"
        const val SEND = "Send"
        const val RECEIVE = "Receive"
        const val HAS_KEYS = "HasKeys"
        const val STATUS = "Status"
        const val TYPE = "Type"
        const val DISPLAY_NAME = "DisplayName"
        const val SIGNATURE = "Signature"
        const val TWOFA = "2FA"
        const val SCOPE = "Scope"
        const val PAYLOAD = "Payload"
    }

    object Domain {
        const val ID = "ID"
        const val DOMAIN_NAME = "DomainName"
        const val VERIFY_CODE = "VerifyCode"
        const val DKIM_PUBLIC_KEY = "DkimPublicKey"
        const val STATE = "State"
        const val CHECK_TIME = "CheckTime"
        const val VERIFY_STATE = "VerifyState"
        const val MX_STATE = "MxState"
        const val SPF_STATE = "SpfState"
        const val DKIM_STATE = "DkimState"
        const val DMARC_STATE = "DmarcState"
        const val DOMAIN = "Domain"
    }

    object Contact {
        const val CONTACT = "Contact"
        const val CONTACTS = "Contacts"
        const val CARDS = "Cards"
        const val TYPE = "Type"
        const val SIGNATURE = "Signature"
        const val DATA = "Data"
        const val OVERWRITE = "Overwrite"
        const val GROUPS = "Groups"
        const val LABELS = "Labels"
        const val CONTACT_EMAILS = "ContactEmails"
        const val TOTAL = "Total"
    }

    object Response {
        const val CODE = "Code"
        const val ERROR = "Error"
        const val ERROR_DETAILS = "Details"
        const val RESPONSES = "Responses"
        const val RESPONSE = "Response"
        const val CONTACT = "Contact"
        const val INDEX = "Index"
        const val ID = "ID"
    }

    object Events {
        const val EVENT_ID = "EventID"
        const val REFRESH = "Refresh"
        const val MORE = "More"
        const val USED_SPACE = "UsedSpace"
        const val MESSAGES = "Messages"
        const val CONVERSATIONS = "Conversations"
        const val CONTACTS = "Contacts"
        const val CONTACT_EMAILS = "ContactEmails"
        const val LABELS = "Labels"
        const val USER = "User"
        const val MAIL_SETTINGS = "MailSettings"
        const val USER_SETTINGS = "UserSettings"
        const val ID = "ID"
        const val MESSAGE = "Message"
        const val ACTION = "Action"
        const val CONTACT = "Contact"
        const val CONTACT_EMAIL = "ContactEmail"
        const val LABEL = "Label"
        const val MESSAGE_COUNTS = "MessageCounts"
        const val CONVERSATION_COUNTS = "ConversationCounts"
        const val ADDRESSES = "Addresses"
        const val ADDRESS = "Address"
    }

    object MailSettings {
        const val MAIL_SETTINGS = "MailSettings"
    }

    object Addresses {
        const val DOMAIN = "Domain"
        const val ORDER = "Order"
        const val DISPLAY_NAME = "DisplayName"
        const val SIGNATURE = "Signature"
        const val ADDRESSES = "Addresses"
        const val ADDRESS = "Address"
        const val ADDRESS_IDS = "AddressIDs"
    }

    object Keys {
        const val RECIPIENT_TYPE = "RecipientType"
        const val MIME_TYPE = "MIMEType"
        const val KEYS = "Keys"

        object KeyBody {
            const val FLAGS = "Flags"
            const val PUBLIC_KEY = "PublicKey"
            const val PRIVATE_KEY = "PrivateKey"
        }
    }

    object Settings {
        const val NOTIFY = "Notify"
        const val SRP_SESSION = "SRPSession"
        const val CLIENT_PROOF = "ClientProof"
        const val CLIENT_EPHEMERAL = "ClientEphemeral"
        const val TWO_FACTOR_CODE = "TwoFactorCode"
        const val EMAIL = "Email"
        const val USER_SETTINGS = "UserSettings"
        const val DISPLAY_NAME = "DisplayName"
        const val SIGNATURE = "Signature"
        const val SHOW_IMAGES = "ShowImages"
    }

}
