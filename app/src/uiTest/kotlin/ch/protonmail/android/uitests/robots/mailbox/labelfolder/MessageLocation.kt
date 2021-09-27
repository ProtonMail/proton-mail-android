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

package ch.protonmail.android.uitests.robots.mailbox.labelfolder

import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.StringUtils.stringFromResource

object MessageLocation {
    val archive = stringFromResource(R.string.archive_option)
    val drafts = stringFromResource(R.string.drafts_option)
    val inbox = stringFromResource(R.string.inbox_option)
    val sent = stringFromResource(R.string.sent_option)
    val spam = stringFromResource(R.string.spam_option)
    val trash = stringFromResource(R.string.trash_option)
}
