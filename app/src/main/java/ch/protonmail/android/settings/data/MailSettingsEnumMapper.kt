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

package ch.protonmail.android.settings.data

import me.proton.core.mailsettings.domain.entity.SwipeAction
import ch.protonmail.android.adapters.swipe.SwipeAction as SwipeActionLocal

internal fun SwipeAction.toLocal(): SwipeActionLocal {
    return when (this) {
        SwipeAction.Trash -> SwipeActionLocal.TRASH
        SwipeAction.Spam -> SwipeActionLocal.SPAM
        SwipeAction.Star -> SwipeActionLocal.STAR
        SwipeAction.Archive -> SwipeActionLocal.ARCHIVE
        SwipeAction.MarkRead -> SwipeActionLocal.MARK_READ
    }
}
