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
package ch.protonmail.android.adapters.swipe;

import ch.protonmail.android.R;

/**
 * Created by dkadrikj on 9.7.15.
 */
public enum SwipeAction {
    TRASH {
        @Override
        public int getActionDescription() {
            return R.string.swipe_action_trash;
        }

        @Override
        public int getActionBackgroundResource(boolean left) {
            return left ? R.layout.message_list_item_swipe_trash : R.layout.message_list_item_swipe_trash_right;
        }
    }, SPAM {
        @Override
        public int getActionDescription() {
            return R.string.swipe_action_spam;
        }

        @Override
        public int getActionBackgroundResource(boolean left) {
            return R.layout.message_list_item_swipe_spam;
        }
    }, STAR {
        @Override
        public int getActionDescription() {
            return R.string.swipe_action_star;
        }

        @Override
        public int getActionBackgroundResource(boolean left) {
            return R.layout.message_list_item_swipe_star;
        }
    }, ARCHIVE {
        @Override
        public int getActionDescription() {
            return R.string.swipe_action_archive;
        }

        @Override
        public int getActionBackgroundResource(boolean left) {
            return R.layout.message_list_item_swipe_archive;
        }
    }, MARK_READ {
        @Override
        public int getActionDescription() {
            return R.string.swipe_action_mark_read;
        }

        @Override
        public int getActionBackgroundResource(boolean left) {
            return R.layout.message_list_item_swipe_mark_read;
        }
    };

    public abstract int getActionDescription();
    public abstract int getActionBackgroundResource(boolean left);
}
