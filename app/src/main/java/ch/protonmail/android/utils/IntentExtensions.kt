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

import android.app.Activity
import android.content.Context
import android.content.Intent
import ch.protonmail.android.activities.EXTRA_HAS_SWITCHED_USER
import ch.protonmail.android.activities.EXTRA_LOGOUT
import ch.protonmail.android.activities.mailbox.MailboxActivity

fun Context.moveToMailbox() {
    val mailboxIntent = AppUtil.decorInAppIntent(Intent(this, MailboxActivity::class.java))
    mailboxIntent.putExtra(EXTRA_HAS_SWITCHED_USER, true)
    mailboxIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    startActivity(mailboxIntent)
}

fun Activity.moveToMailboxLogout() {
    val mailboxIntent = AppUtil.decorInAppIntent(Intent(this, MailboxActivity::class.java))
    mailboxIntent.putExtra(EXTRA_LOGOUT, true)
    startActivity(mailboxIntent)
    finish()
}

fun Activity.moveToLogin() {
    /*
    startActivity(AppUtil.decorInAppIntent(Intent(this, LoginActivity::class.java)))
    finish()
    */
    TODO("startLoginWorkflow")
}
