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
package ch.protonmail.android.adapters.swipe

import ch.protonmail.android.R

enum class SwipeAction {
    TRASH {

        override val actionName: Int
            get() = R.string.swipe_action_trash_short

        override val actionDescription: Int
            get() = R.string.swipe_action_trash

        override fun getActionBackgroundResource(right: Boolean): Int =
            if (right) R.layout.mailbox_right_swipe_action_trash else R.layout.mailbox_left_swipe_action_trash

        override fun getActionPreviewBackgroundResource(right: Boolean): Int =
            if (right) R.layout.settings_right_swipe_action_trash else R.layout.settings_left_swipe_action_trash
    },
    SPAM {

        override val actionName: Int
            get() = R.string.swipe_action_spam_short

        override val actionDescription: Int
            get() = R.string.swipe_action_spam

        override fun getActionBackgroundResource(right: Boolean): Int =
            if (right) R.layout.mailbox_right_swipe_action_spam else R.layout.mailbox_left_swipe_action_spam

        override fun getActionPreviewBackgroundResource(right: Boolean): Int =
            if (right) R.layout.settings_right_swipe_action_spam else R.layout.settings_left_swipe_action_spam
    },
    UPDATE_STAR {

        override val actionName: Int
            get() = R.string.swipe_action_star_short

        override val actionDescription: Int
            get() = R.string.swipe_action_star

        override fun getActionBackgroundResource(right: Boolean): Int =
            if (right) R.layout.mailbox_right_swipe_action_star else R.layout.mailbox_left_swipe_action_star

        override fun getActionPreviewBackgroundResource(right: Boolean): Int =
            if (right) R.layout.settings_right_swipe_action_star else R.layout.settings_left_swipe_action_star
    },
    ARCHIVE {

        override val actionName: Int
            get() = R.string.swipe_action_archive_short

        override val actionDescription: Int
            get() = R.string.swipe_action_archive

        override fun getActionBackgroundResource(right: Boolean): Int =
            if (right) R.layout.mailbox_right_swipe_action_archive else R.layout.mailbox_left_swipe_action_archive

        override fun getActionPreviewBackgroundResource(right: Boolean): Int =
            if (right) R.layout.settings_right_swipe_action_archive else R.layout.settings_left_swipe_action_archive
    },
    MARK_READ {

        override val actionName: Int
            get() = R.string.swipe_action_mark_read_short

        override val actionDescription: Int
            get() = R.string.swipe_action_mark_read

        override fun getActionBackgroundResource(right: Boolean): Int =
            if (right) R.layout.mailbox_right_swipe_action_mark_read else R.layout.mailbox_left_swipe_action_mark_read

        override fun getActionPreviewBackgroundResource(right: Boolean): Int =
            if (right) R.layout.settings_right_swipe_action_mark_read else R.layout.settings_left_swipe_action_mark_read
    };

    abstract val actionName: Int
    abstract val actionDescription: Int
    abstract fun getActionBackgroundResource(right: Boolean): Int
    abstract fun getActionPreviewBackgroundResource(right: Boolean): Int
}
