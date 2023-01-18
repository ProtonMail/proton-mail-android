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

package ch.protonmail.android.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.messageDetails.IntentExtrasData
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.contacts.ContactsActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.onboarding.newuser.presentation.NewUserOnboardingActivity
import ch.protonmail.android.settings.presentation.EXTRA_CURRENT_MAILBOX_LABEL_ID
import ch.protonmail.android.settings.presentation.EXTRA_CURRENT_MAILBOX_LOCATION
import ch.protonmail.android.settings.presentation.SettingsActivity
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.MessageUtils
import java.io.Serializable

class StartSettings : ActivityResultContract<StartSettings.Input, Unit?>() {

    override fun createIntent(context: Context, input: Input): Intent =
        AppUtil.decorInAppIntent(Intent(context, SettingsActivity::class.java)).apply {
            putExtra(EXTRA_CURRENT_MAILBOX_LOCATION, input.locationType.messageLocationTypeValue)
            putExtra(EXTRA_CURRENT_MAILBOX_LABEL_ID, input.labelId)
        }

    override fun parseResult(resultCode: Int, result: Intent?): Unit? {
        if (resultCode != Activity.RESULT_OK) return null
        return Unit
    }

    data class Input(
        val locationType: Constants.MessageLocationType,
        val labelId: String?,
    )
}

class StartContacts : ActivityResultContract<Unit, Unit?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        AppUtil.decorInAppIntent(Intent(context, ContactsActivity::class.java))

    override fun parseResult(resultCode: Int, result: Intent?): Unit? {
        if (resultCode != Activity.RESULT_OK) return null
        return Unit
    }
}

class StartCompose : ActivityResultContract<StartCompose.Input, String?>() {

    override fun createIntent(context: Context, input: Input): Intent =
        AppUtil.decorInAppIntent(Intent(context, ComposeMessageActivity::class.java)).apply {
            input.messageId?.let { putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, it) }
            input.isInline?.let { putExtra(ComposeMessageActivity.EXTRA_MESSAGE_RESPONSE_INLINE, it) }
            input.addressId?.let { putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_ID, it) }
            input.toRecipients?.let { putExtra(ComposeMessageActivity.EXTRA_TO_RECIPIENTS, it.toTypedArray()) }
            input.toRecipientGroups?.let {
                putExtra(ComposeMessageActivity.EXTRA_TO_RECIPIENT_GROUPS, it as Serializable)
            }
            input.intentExtrasData?.let { intentExtrasData ->
                MessageUtils.addRecipientsToIntent(
                    this,
                    ComposeMessageActivity.EXTRA_TO_RECIPIENTS,
                    intentExtrasData.toRecipientListString,
                    intentExtrasData.messageAction,
                    intentExtrasData.userAddresses
                )
                if (intentExtrasData.includeCCList) {
                    MessageUtils.addRecipientsToIntent(
                        this,
                        ComposeMessageActivity.EXTRA_CC_RECIPIENTS,
                        intentExtrasData.messageCcList,
                        intentExtrasData.messageAction,
                        intentExtrasData.userAddresses
                    )
                }
                putExtra(ComposeMessageActivity.EXTRA_LOAD_IMAGES, intentExtrasData.imagesDisplayed)
                putExtra(ComposeMessageActivity.EXTRA_LOAD_REMOTE_CONTENT, intentExtrasData.remoteContentDisplayed)
                putExtra(ComposeMessageActivity.EXTRA_SENDER_NAME, intentExtrasData.messageSenderName)
                putExtra(ComposeMessageActivity.EXTRA_SENDER_ADDRESS, intentExtrasData.senderEmailAddress)
                putExtra(ComposeMessageActivity.EXTRA_PGP_MIME, intentExtrasData.isPGPMime)
                putExtra(ComposeMessageActivity.EXTRA_MESSAGE_TITLE, intentExtrasData.newMessageTitle)
                putExtra(ComposeMessageActivity.EXTRA_MESSAGE_BODY_LARGE, intentExtrasData.largeMessageBody)
                putExtra(ComposeMessageActivity.EXTRA_MESSAGE_BODY, intentExtrasData.body)
                putExtra(ComposeMessageActivity.EXTRA_MESSAGE_TIMESTAMP, intentExtrasData.timeMs)
                putExtra(ComposeMessageActivity.EXTRA_PARENT_ID, intentExtrasData.messageId)
                putExtra(ComposeMessageActivity.EXTRA_ACTION_ID, intentExtrasData.messageAction)
                putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_ID, intentExtrasData.addressID)
                putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_EMAIL_ALIAS, intentExtrasData.addressEmailAlias)
                if (intentExtrasData.embeddedImagesAttachmentsExist) {
                    putParcelableArrayListExtra(
                        ComposeMessageActivity.EXTRA_MESSAGE_EMBEDDED_ATTACHMENTS,
                        intentExtrasData.attachments
                    )
                }
                val attachments = intentExtrasData.attachments
                if (attachments.size > 0) {
                    if (!intentExtrasData.isPGPMime) {
                        attachments.map { it.doSaveInDB = false }
                    }
                    putParcelableArrayListExtra(
                        ComposeMessageActivity.EXTRA_MESSAGE_ATTACHMENTS,
                        attachments
                    )
                }
            }
        }

    override fun parseResult(resultCode: Int, result: Intent?): String? {
        return if (resultCode == Activity.RESULT_OK) {
            result?.getStringExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID)
        } else {
            null
        }
    }

    data class Input(
        val messageId: String? = null,
        val isInline: Boolean? = null,
        val addressId: String? = null,
        val toRecipients: List<String>? = null,
        val toRecipientGroups: List<MessageRecipient>? = null,
        val intentExtrasData: IntentExtrasData? = null
    )
}

class StartSearch : ActivityResultContract<Unit, Unit?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        AppUtil.decorInAppIntent(Intent(context, SearchActivity::class.java))

    override fun parseResult(resultCode: Int, result: Intent?): Unit? {
        if (resultCode != Activity.RESULT_OK) return null
        return Unit
    }
}

class StartOnboarding : ActivityResultContract<Unit, Boolean?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        AppUtil.decorInAppIntent(Intent(context, NewUserOnboardingActivity::class.java))

    override fun parseResult(resultCode: Int, result: Intent?): Boolean {
        return true
    }
}
