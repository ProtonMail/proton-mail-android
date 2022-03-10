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
import ch.protonmail.android.domain.util.requireNotBlank
import ch.protonmail.android.feature.account.AccountStateManager
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Switch the current user and then open [MessageDetailsActivity]
 * This is useful, for example, when tapping on a notification for the secondary user: this will help us avoid
 *  background limitations, like opening an Activity.
 */
internal class SwitchUserAndOpenMessageDetailsActivity : AppCompatActivity() {

    @Inject
    lateinit var accountStateManager: AccountStateManager

    private val messageDetailsLauncher = registerForActivityResult(MessageDetailsActivity.Launcher()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val input = Input.fromIntent(intent)
        accountStateManager.switch(input.recipientUserId)
            .invokeOnCompletion { launchMessageDetailsActivity() }
    }

    private fun launchMessageDetailsActivity() {
        val input = MessageDetailsActivity.Input(TODO(), TODO(), TODO(), TODO())
        messageDetailsLauncher.launch(input)
    }

    data class Input(val recipientUserId: UserId) {

        fun toIntent(context: Context) = Intent(context, SwitchUserAndOpenMessageDetailsActivity::class.java)
            .putExtra(EXTRA_RECIPIENT_USER_ID, recipientUserId.id)

        companion object {

            private const val EXTRA_RECIPIENT_USER_ID = "extra.userId"

            fun fromIntent(intent: Intent): Input {
                val recipientUserId = requireNotBlank(intent.getStringExtra(EXTRA_RECIPIENT_USER_ID)) {
                    "No user id found in the intent"
                }
                return Input(recipientUserId = UserId(recipientUserId))
            }
        }
    }
}
