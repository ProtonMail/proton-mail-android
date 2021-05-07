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
package ch.protonmail.android.uiModel

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.protonmail.android.core.Constants.DrawerOptionType
import ch.protonmail.android.core.Constants.MessageLocationType

/**
 * Ui Model for Items in Navigation Drawer
 * @see ch.protonmail.android.adapters.DrawerAdapter
 *
 * @author Davide Farella
 */
internal sealed class DrawerItemUiModel {

    /**
     * Header for the Drawer
     *
     * @param name [String] name of the current user
     * @param email [String] email of the current user
     * @param snoozeEnabled [Boolean] whether snooze is enabled for the current user
     */
    data class Header(
        val name: String,
        val email: String,
        val snoozeEnabled: Boolean
    ) : DrawerItemUiModel()

    /** Divider for Drawer Items */
    @Deprecated("To be removed, not needed anymore")
    object Divider : DrawerItemUiModel()

    /**
     * Title of a section for the Drawer
     *
     * @param text [CharSequence] text of the Section
     */
    data class SectionName(
        val text: CharSequence
    ) : DrawerItemUiModel()

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
         * @return a new instance of [Primary] by mutating [Primary.notificationCount] with the
         * given [count]
         */
        abstract fun copyWithNotificationCount( count: Int ) : Primary

        /**
         * @return a new instance of [Primary] by mutating [Primary.selected] with the given
         * [select]
         */
        abstract fun copyWithSelected( select: Boolean ) : Primary

        /**
         * Whether this element is currently selected
         * Default is `false`
         */
        open val selected: Boolean = false
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
        data class Static @JvmOverloads constructor (
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
                ACCOUNT_MANAGER(115, DrawerOptionType.ACCOUNT_MANAGER)
            }
        }

        /**
         * Item for the Drawer that represent a Label
         *
         * @param notificationCount [Int] overridden in constructor for `copy` purpose
         * @param selected [Boolean] overridden in constructor for `copy` purpose
         */
        data class Label @JvmOverloads constructor (
            val uiModel: LabelUiModel,
            override val notificationCount: Int = 0,
            override val selected: Boolean = false
        ) : Primary() {

            override fun copyWithNotificationCount(count: Int) = copy(notificationCount = count)
            override fun copyWithSelected(select: Boolean) = copy( selected = select )
        }
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

/**
 * @return [List] of [DrawerItemUiModel] changing the first [DrawerItemUiModel.Header] item with
 * the given [header]
 */
internal fun List<DrawerItemUiModel>.setHeader(
    header: DrawerItemUiModel.Header?
): List<DrawerItemUiModel> {
    val withoutHeader = dropWhile { it is DrawerItemUiModel.Header }
    val newHeaderToList = header?.let { listOf(it) } ?: listOf()
    return newHeaderToList + withoutHeader
}

/**
 * Removes all the [DrawerItemUiModel.Primary.Label] from the receiver [List] of
 * [DrawerItemUiModel] and add the given [labels] to the end of the [List]
 * It will also add or remove a [DrawerItemUiModel.Divider] before [labels], if needed
 *
 * @return [List] of [DrawerItemUiModel]
 */
internal fun List<DrawerItemUiModel>.setLabels(
    labels: List<DrawerItemUiModel.Primary.Label>
): List<DrawerItemUiModel> {

    // Remove all the Labels
    val withoutLabels = filterNot { it is DrawerItemUiModel.Primary.Label }.toMutableList()
    val lastItem = withoutLabels.last()

    // Add or remove Divider, if needed
    if ( labels.isNotEmpty() && lastItem !is DrawerItemUiModel.Divider )
        withoutLabels += DrawerItemUiModel.Divider
    else if ( labels.isEmpty() && lastItem is DrawerItemUiModel.Divider )
        withoutLabels -= lastItem

    return withoutLabels + labels
}
