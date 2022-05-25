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

package ch.protonmail.android.settings.data

import ch.protonmail.android.adapters.swipe.SwipeAction as MailSwipeAction
import me.proton.core.mailsettings.domain.entity.SwipeAction as CoreSwipeAction

internal fun CoreSwipeAction.toMailSwipeAction(): MailSwipeAction {
    return when (this) {
        CoreSwipeAction.Trash -> MailSwipeAction.TRASH
        CoreSwipeAction.Spam -> MailSwipeAction.SPAM
        CoreSwipeAction.Star -> MailSwipeAction.UPDATE_STAR
        CoreSwipeAction.Archive -> MailSwipeAction.ARCHIVE
        CoreSwipeAction.MarkRead -> MailSwipeAction.MARK_READ
    }
}
