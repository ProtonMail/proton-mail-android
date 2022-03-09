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
package ch.protonmail.android.utils

import android.content.Context
import android.content.Intent
import ch.protonmail.android.navigation.presentation.EXTRA_FIRST_LOGIN
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.mailbox.presentation.MailboxActivity
import ch.protonmail.android.navigation.presentation.EXTRA_FIRST_LOGIN
import ch.protonmail.android.navigation.presentation.EXTRA_FIRST_LOGIN
import ch.protonmail.android.notifications.presentation.utils.EXTRA_MAILBOX_LOCATION
import ch.protonmail.android.notifications.presentation.utils.EXTRA_USER_ID
import ch.protonmail.android.utils.extensions.app
import me.proton.core.domain.entity.UserId

fun Context.startMailboxActivity(
    userId: UserId? = null,
    type: Constants.MessageLocationType? = null
) =
    startActivity(getMailboxActivityIntent(userId, type))

fun Context.getMailboxActivityIntent(
    userId: UserId? = null,
    type: Constants.MessageLocationType? = null
): Intent =
    Intent(this, MailboxActivity::class.java).apply {
        userId?.let { putExtra(EXTRA_USER_ID, it.id) }
        type?.let { putExtra(EXTRA_MAILBOX_LOCATION, it.messageLocationTypeValue) }
        putExtra(EXTRA_FIRST_LOGIN, app.hasUpdateOccurred())
    }
