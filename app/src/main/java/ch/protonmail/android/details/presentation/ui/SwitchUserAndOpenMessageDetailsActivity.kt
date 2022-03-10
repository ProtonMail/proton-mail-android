/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.details.presentation.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.domain.util.requireNotBlank
import ch.protonmail.android.feature.account.AccountStateManager
import dagger.hilt.android.AndroidEntryPoint
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Switch the current user and then open [MessageDetailsActivity]
 * This is useful, for example, when tapping on a notification for the secondary user: this will help us avoid
 *  background limitations, like opening an Activity.
 */
@AndroidEntryPoint
internal class SwitchUserAndOpenMessageDetailsActivity : AppCompatActivity() {

    @Inject
    lateinit var accountStateManager: AccountStateManager

    private val messageDetailsLauncher = registerForActivityResult(MessageDetailsActivity.Launcher()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val input = Input.fromIntent(intent)
        accountStateManager.switch(input.userId)
            .invokeOnCompletion { launchMessageDetailsActivity(input) }
    }

    private fun launchMessageDetailsActivity(input: Input) {
        val messageDetailsInput = MessageDetailsActivity.Input(
            messageId = input.messageId,
            locationType = input.locationType,
            labelId = null,
            messageSubject = input.messageSubject
        )
        messageDetailsLauncher.launch(messageDetailsInput)
    }

    data class Input(
        val userId: UserId,
        val messageId: String,
        val locationType: Constants.MessageLocationType?,
        val messageSubject: String?
    ) {

        fun toIntent(context: Context) = Intent(context, SwitchUserAndOpenMessageDetailsActivity::class.java)
            .putExtra(EXTRA_USER_ID, userId.id)
            .putExtra(MessageDetailsActivity.EXTRA_MESSAGE_OR_CONVERSATION_ID, messageId)
            .putExtra(MessageDetailsActivity.EXTRA_MESSAGE_LOCATION_ID, locationType?.messageLocationTypeValue)
            .putExtra(MessageDetailsActivity.EXTRA_MESSAGE_SUBJECT, messageSubject)
            .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

        companion object {

            private const val EXTRA_USER_ID = "extra.userId"

            fun fromIntent(intent: Intent): Input {
                val userId = requireNotBlank(intent.getStringExtra(EXTRA_USER_ID)) {
                    "No user id found in the intent"
                }
                val messageId =
                    requireNotBlank(intent.getStringExtra(MessageDetailsActivity.EXTRA_MESSAGE_OR_CONVERSATION_ID)) {
                        "Message Id is null or black for the Intent"
                    }
                val locationType = intent.getIntExtra(
                    MessageDetailsActivity.EXTRA_MESSAGE_LOCATION_ID,
                    Constants.MessageLocationType.INVALID.messageLocationTypeValue
                ).let(Constants.MessageLocationType::fromInt)
                val subject = requireNotNull(intent.getStringExtra(MessageDetailsActivity.EXTRA_MESSAGE_SUBJECT)) {
                    "Message Subject is null for the Intent"
                }
                return Input(
                    userId = UserId(userId),
                    messageId = messageId,
                    locationType = locationType,
                    messageSubject = subject
                )
            }
        }
    }
}
