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
package ch.protonmail.android.drawer.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.protonmail.android.core.Constants.DrawerOptionType
import ch.protonmail.android.core.Constants.MessageLocationType

/**
 * Ui Model for Items in Navigation Drawer
 * @see ch.protonmail.android.drawer.presentation.ui.DrawerAdapter
 */
internal sealed class DrawerItemUiModel {

    /**
     * Header of a section for the Drawer
     */
    data class SectionName(
        val text: CharSequence,
        val type: Type,
        val shouldShowCreateButton: Boolean
    ) : DrawerItemUiModel() {

        enum class Type { LABEL, FOLDER, OTHER }
    }

    /**
     * Primary Item for the Drawer.
     * It could be a static item from the App configuration or dynamic item like a Label
     */
    sealed class Primary : DrawerItemUiModel() {

        /**
         * Count of active notifications for this Item
         * Default is `0`
         */
        open val notificationCount: Int = 0

        /**
         * Whether this element is currently selected
         * Default is `false`
         */
        open val selected: Boolean = false

        /**
         * @return a new instance of [Primary] by mutating [Primary.notificationCount] with the
         * given [count]
         */
        abstract fun copyWithNotificationCount(count: Int): Primary

        /**
         * @return a new instance of [Primary] by mutating [Primary.selected] with the given
         * [select]
         */
        abstract fun copyWithSelected(select: Boolean): Primary

        /** @return `true` if [notificationCount] greater than 0 */
        fun hasNotifications() = notificationCount > 0

        /**
         * Static Item for the Drawer. like: Inbox, Sent, Draft, etc
         *
         * @param type [DrawerItemUiModel.Primary.Static.Type]
         * @param labelRes [StringRes] of the label
         * @param iconRes [DrawableRes] of the icon
         * @param notificationCount [Int] overridden in constructor for `copy` purpose
         * @param selected [Boolean] overridden in constructor for `copy` purpose
         */
        data class Static @JvmOverloads constructor(
            val type: Type,
            @StringRes val labelRes: Int,
            @DrawableRes val iconRes: Int,
            override val notificationCount: Int = 0,
            override val selected: Boolean = false
        ) : Primary() {

            override fun copyWithNotificationCount(count: Int) = copy(notificationCount = count)
            override fun copyWithSelected(select: Boolean) = copy(selected = select)

            /**
             * Available type of [DrawerItemUiModel.Primary.Static]
             * @param itemId [Int] id of the Item
             */
            enum class Type(
                val itemId: Int,
                val drawerOptionType: DrawerOptionType
            ) {

                INBOX(MessageLocationType.INBOX.messageLocationTypeValue, DrawerOptionType.INBOX),
                STARRED(MessageLocationType.STARRED.messageLocationTypeValue, DrawerOptionType.STARRED),
                DRAFTS(MessageLocationType.DRAFT.messageLocationTypeValue, DrawerOptionType.DRAFTS),
                SENT(MessageLocationType.SENT.messageLocationTypeValue, DrawerOptionType.SENT),
                ARCHIVE(MessageLocationType.ARCHIVE.messageLocationTypeValue, DrawerOptionType.ARCHIVE),
                TRASH(MessageLocationType.TRASH.messageLocationTypeValue, DrawerOptionType.TRASH),
                SPAM(MessageLocationType.SPAM.messageLocationTypeValue, DrawerOptionType.SPAM),
                LABEL(MessageLocationType.LABEL.messageLocationTypeValue, DrawerOptionType.LABEL),
                ALLMAIL(MessageLocationType.ALL_MAIL.messageLocationTypeValue, DrawerOptionType.ALL_MAIL),
                CONTACTS(108, DrawerOptionType.CONTACTS),
                SETTINGS(109, DrawerOptionType.SETTINGS),
                REPORT_BUGS(101, DrawerOptionType.REPORT_BUGS),
                SIGNOUT(111, DrawerOptionType.SIGN_OUT),
                LOCK(112, DrawerOptionType.LOCK),
            }
        }

        /**
         * Item for the Drawer that represent a Label
         *
         * @param notificationCount [Int] overridden in constructor for `copy` purpose
         * @param selected [Boolean] overridden in constructor for `copy` purpose
         */
        data class Label @JvmOverloads constructor(
            val uiModel: DrawerLabelUiModel,
            override val notificationCount: Int = 0,
            override val selected: Boolean = false
        ) : Primary() {

            override fun copyWithNotificationCount(count: Int) = copy(notificationCount = count)
            override fun copyWithSelected(select: Boolean) = copy(selected = select)
        }
    }

    /**
     * Represent a View ( Button ) for crate a new Item
     * @see CreateItem.Folder
     * @see CreateItem.Label
     */
    sealed class CreateItem : DrawerItemUiModel() {

        @get:StringRes
        abstract val textRes: Int

        data class Folder(@StringRes override val textRes: Int) : CreateItem()

        data class Label(@StringRes override val textRes: Int) : CreateItem()
    }

    /**
     * Footer for the Drawer
     *
     * @param text [CharSequence] text of the Footer
     */
    data class Footer(
        val text: CharSequence
    ) : DrawerItemUiModel()
}
