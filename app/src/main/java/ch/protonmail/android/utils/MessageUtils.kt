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
package ch.protonmail.android.utils

import android.content.Context
import android.content.Intent
import ch.protonmail.android.R
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.SimpleMessage
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.api.models.enumerations.MessageFlag
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageActionType
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.user.Addresses
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.equalsNoCase
import timber.log.Timber
import java.util.Locale
import java.util.UUID

/**
 * A class that contains methods for utility operations with messages
 */
object MessageUtils {

    fun addRecipientsToIntent(
        intent: Intent,
        extraName: String,
        recipientList: String?,
        messageAction: MessageActionType,
        userAddresses: Addresses
    ) {
        val addresses = userAddresses.addresses.values
        if (!recipientList.isNullOrEmpty()) {
            val recipients = recipientList.split(Constants.EMAIL_DELIMITER)
            val numberOfMatches = recipients.intersect(addresses.map { it.email.s }).size
            val list = recipients.filter { recipient ->
                addresses.none { it.email.s equalsNoCase recipient } ||
                    messageAction == MessageActionType.REPLY
            } as ArrayList

            if (list.size > 0) {
                intent.putExtra(extraName, list.toTypedArray())
            } else if (numberOfMatches == recipients.size && ComposeMessageActivity.EXTRA_TO_RECIPIENTS == extraName) {
                list.add(recipients[0])
                intent.putExtra(extraName, list.toTypedArray())
            }
        }
    }

    @Deprecated("Use with Addresses model")
    fun addRecipientsToIntent(
        intent: Intent,
        extraName: String,
        recipientList: String?,
        messageAction: MessageActionType,
        userAddresses: List<Address>
    ) {
        if (!recipientList.isNullOrEmpty()) {
            val recipients = recipientList.split(Constants.EMAIL_DELIMITER)
            val numberOfMatches = recipients.intersect(userAddresses.map { it.email }).size
            val list = recipients.filter { recipient ->
                userAddresses.none { it.email equalsNoCase recipient } ||
                    messageAction == MessageActionType.REPLY
            } as ArrayList

            if (list.size > 0) {
                intent.putExtra(extraName, list.toTypedArray())
            } else if (numberOfMatches == recipients.size && ComposeMessageActivity.EXTRA_TO_RECIPIENTS == extraName) {
                list.add(recipients[0])
                intent.putExtra(extraName, list.toTypedArray())
            }
        }
    }

    // TODO: discard nullability of parameters once MessageDetailsActivity is converted to Kotlin
    fun buildNewMessageTitle(
        context: Context,
        messageAction: MessageActionType?,
        messageTitle: String?
    ): String {
        val normalizedMessageTitle = normalizeMessageTitle(context, messageTitle)
        val messagePrefix = when (messageAction) {
            MessageActionType.REPLY,
            MessageActionType.REPLY_ALL -> context.getString(R.string.reply_prefix) + " "
            MessageActionType.FORWARD -> context.getString(R.string.forward_prefix) + " "
            else -> EMPTY_STRING
        }
        return messagePrefix + normalizedMessageTitle
    }

    // TODO: discard nullability of parameters once MessageDetailsActivity is converted to Kotlin (dependent on above)
    private fun normalizeMessageTitle(context: Context, messageTitle: String?): String? {
        val prefixes = arrayOf(
            context.getString(R.string.reply_prefix),
            context.getString(R.string.forward_prefix)
        )
        for (prefix in prefixes) {
            if (messageTitle?.toLowerCase(Locale.getDefault())
                ?.startsWith(prefix.toLowerCase(Locale.getDefault())) == true
            ) {
                return messageTitle.substring(prefix.length).trim { it <= ' ' }
            }
        }
        return messageTitle
    }

    // TODO: replace with expression in MailboxActivity once it's converted to Kotlin and delete method
    fun areAllRead(messages: List<SimpleMessage>): Boolean = messages.all { it.isRead }

    // TODO: replace with expression in MailboxActivity once it's converted to Kotlin and delete method
    fun areAllUnRead(messages: List<SimpleMessage>): Boolean = messages.all { !it.isRead }

    fun containsRealContent(text: String): Boolean = text.replace("<div>", "")
        .replace("</div>", "")
        .replace("<br />", "").isNotEmpty()

    // TODO: replace with expression once ComposeMessageActivity is converted to Kotlin
    fun isPmMeAddress(address: String): Boolean = address.endsWith(Constants.MAIL_DOMAIN_PM_ME)

    fun toContactString(messageRecipients: List<MessageRecipient>): String {
        if (messageRecipients.isEmpty()) return EMPTY_STRING
        val builder = StringBuilder()
        var skip = true
        for (messageRecipient in messageRecipients) {
            if (skip) {
                skip = false
            } else {
                builder.append(Constants.EMAIL_DELIMITER)
            }
            builder.append(messageRecipient.emailAddress)
        }
        return builder.toString()
    }

    fun toContactsAndGroupsString(messageRecipients: List<MessageRecipient>): String {
        if (messageRecipients.isEmpty()) return EMPTY_STRING
        val setOfEmailsIncludingGroups: MutableSet<String> = HashSet()
        for (messageRecipient in messageRecipients) {
            setOfEmailsIncludingGroups.add(
                if (!messageRecipient.group.isNullOrEmpty()) {
                    messageRecipient.group
                } else {
                    messageRecipient.emailAddress
                }
            )
        }
        val builder = StringBuilder()
        var skip = true
        for (recipient in setOfEmailsIncludingGroups) {
            if (skip) {
                skip = false
            } else {
                builder.append(Constants.EMAIL_DELIMITER)
            }
            builder.append(recipient)
        }
        return builder.toString()
    }

    fun getListOfStringsAsString(messageRecipients: List<String>): String {
        if (messageRecipients.isEmpty()) return EMPTY_STRING
        val builder = StringBuilder()
        var firstTime = true
        for (messageRecipient in messageRecipients) {
            if (firstTime) {
                firstTime = false
            } else {
                builder.append(Constants.EMAIL_DELIMITER)
            }
            builder.append(messageRecipient)
        }
        return builder.toString()
    }

    fun isLocalMessageId(messageId: String?): Boolean {
        var valid = false
        try {
            UUID.fromString(messageId)
            valid = true
        } catch (exception: IllegalArgumentException) {
            Timber.i(exception)
        }
        return valid
    }

    fun calculateType(flags: Long): Message.MessageType {
        val received = flags and MessageFlag.RECEIVED.value == MessageFlag.RECEIVED.value
        val sent = flags and MessageFlag.SENT.value == MessageFlag.SENT.value
        return if (received && sent) {
            Message.MessageType.INBOX_AND_SENT
        } else if (received) {
            Message.MessageType.INBOX
        } else if (sent) {
            Message.MessageType.SENT
        } else {
            Message.MessageType.DRAFT
        }
    }

    fun calculateEncryption(flags: Long): MessageEncryption {
        val internal = flags and MessageFlag.INTERNAL.value == MessageFlag.INTERNAL.value
        val e2e = flags and MessageFlag.E2E.value == MessageFlag.E2E.value
        val received = flags and MessageFlag.RECEIVED.value == MessageFlag.RECEIVED.value
        val sent = flags and MessageFlag.SENT.value == MessageFlag.SENT.value
        val auto = flags and MessageFlag.AUTO.value == MessageFlag.AUTO.value

        if (internal) {
            if (e2e) {

                if (received && sent) {
                    return MessageEncryption.INTERNAL
                } else if (received && auto) {
                    return MessageEncryption.AUTO_RESPONSE
                }
                return MessageEncryption.INTERNAL
            }

            return if (auto) {
                MessageEncryption.AUTO_RESPONSE
            } else MessageEncryption.INTERNAL
        } else if (received && e2e) {
            return MessageEncryption.EXTERNAL_PGP
        } else if (received) {
            return MessageEncryption.EXTERNAL
        }

        return if (e2e) {
            MessageEncryption.MIME_PGP
        } else MessageEncryption.EXTERNAL
    }
}
