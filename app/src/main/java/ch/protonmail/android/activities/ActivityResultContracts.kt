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

package ch.protonmail.android.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.settings.EXTRA_CURRENT_MAILBOX_LABEL_ID
import ch.protonmail.android.activities.settings.EXTRA_CURRENT_MAILBOX_LOCATION
import ch.protonmail.android.contacts.ContactsActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.details.presentation.ui.MessageDetailsActivity
import ch.protonmail.android.utils.AppUtil

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

class StartMessageDetails : ActivityResultContract<StartMessageDetails.Input, Unit?>() {

    override fun createIntent(context: Context, input: Input): Intent =
        AppUtil.decorInAppIntent(Intent(context, MessageDetailsActivity::class.java)).apply {
            putExtra(MessageDetailsActivity.EXTRA_MESSAGE_OR_CONVERSATION_ID, input.messageId)
            putExtra(MessageDetailsActivity.EXTRA_MESSAGE_LOCATION_ID, input.locationType.messageLocationTypeValue)
            putExtra(MessageDetailsActivity.EXTRA_MAILBOX_LABEL_ID, input.labelId)
            putExtra(MessageDetailsActivity.EXTRA_MESSAGE_SUBJECT, input.messageSubject)
        }

    override fun parseResult(resultCode: Int, result: Intent?): Unit? {
        if (resultCode != Activity.RESULT_OK) return null
        return Unit
    }

    data class Input(
        val messageId: String,
        val locationType: Constants.MessageLocationType,
        val labelId: String?,
        val messageSubject: String
    )
}

class StartCompose : ActivityResultContract<StartCompose.Input, Unit?>() {

    override fun createIntent(context: Context, input: Input): Intent =
        AppUtil.decorInAppIntent(Intent(context, ComposeMessageActivity::class.java)).apply {
            input.messageId?.let { putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, it) }
            input.isInline?.let { putExtra(ComposeMessageActivity.EXTRA_MESSAGE_RESPONSE_INLINE, it) }
            input.addressId?.let { putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_ID, it) }
        }

    override fun parseResult(resultCode: Int, result: Intent?): Unit? {
        if (resultCode != Activity.RESULT_OK) return null
        return Unit
    }

    data class Input(
        val messageId: String? = null,
        val isInline: Boolean? = null,
        val addressId: String? = null,
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
