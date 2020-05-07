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
package ch.protonmail.android.adapters

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat.startActivity
import ch.protonmail.android.R
import ch.protonmail.android.activities.multiuser.ConnectAccountActivity
import ch.protonmail.android.activities.multiuser.EXTRA_USERNAME
import ch.protonmail.android.uiModel.DrawerUserModel
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.extensions.inflate
import ch.protonmail.android.utils.extensions.setAccountLetters
import ch.protonmail.android.utils.extensions.setNotificationIndicatorSize
import ch.protonmail.android.utils.extensions.setStyle
import ch.protonmail.libs.core.ui.adapter.BaseAdapter
import ch.protonmail.libs.core.ui.adapter.ClickableAdapter
import ch.protonmail.libs.core.utils.onClick
import kotlinx.android.synthetic.main.drawer_user_list_item.view.*
import kotlinx.android.synthetic.main.drawer_user_list_item_footer.view.*

// region constants
private const val VIEW_TYPE_USER = 0 // for user list item in nav drawer list
private const val VIEW_TYPE_DIVIDER = 1 // for divider in every screen
private const val VIEW_TYPE_MANAGE = 2 // for manage accounts button
// endregion

/**
 * Adapter for Drawer Users drop-down (Spinner)
 *
 * Inherits from [BaseAdapter]
 */
internal class DrawerAccountsAdapter(onItemClick: (DrawerUserModel) -> Unit = {}) :
    BaseAdapter<DrawerUserModel, DrawerAccountsAdapter.ViewHolder<DrawerUserModel>>(
        ModelsComparator,
        onItemClick
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<DrawerUserModel> =
        parent.viewHolderForViewType(viewType)

    /** @return [Int] that identifies the View type for the Item at the given [position] */
    override fun getItemViewType(position: Int) = items[position].viewType

    /** A [BaseAdapter.ItemsComparator] for the Adapter */
    private object ModelsComparator : BaseAdapter.ItemsComparator<DrawerUserModel>() {
        override fun areItemsTheSame(oldItem: DrawerUserModel, newItem: DrawerUserModel): Boolean {
            // TODO: document why always false
            return false
        }
    }

    /** @return [LayoutRes] for the given [viewType] */
    @LayoutRes
    private fun layoutForViewType(viewType: Int) = when (viewType) {
        VIEW_TYPE_USER -> R.layout.drawer_user_list_item
        VIEW_TYPE_DIVIDER -> R.layout.drawer_list_item_divider
        VIEW_TYPE_MANAGE -> R.layout.drawer_user_list_item_footer
        else -> throw IllegalArgumentException("View type not found: '$viewType'")
    }

    // region extension functions

    private fun <DUM : DrawerUserModel> ViewGroup.viewHolderForViewType(viewType: Int): ViewHolder<DUM> {
        val view = inflate(layoutForViewType(viewType))
        @Suppress("UNCHECKED_CAST") // Type cannot be checked since is in invariant position
        return when (viewType) {
            VIEW_TYPE_USER -> NavUserViewHolder(view)
            VIEW_TYPE_DIVIDER -> DividerViewHolder(view)
            VIEW_TYPE_MANAGE -> ManageAccountsViewHolder(view)
            else -> throw IllegalArgumentException("View type not found: '$viewType'")
        } as ViewHolder<DUM>
    }

    /** @return [Int] view type for the receiver [DrawerUserModel] */
    private val DrawerUserModel.viewType: Int
        get() = when (this) {
            is DrawerUserModel.User -> VIEW_TYPE_USER
            is DrawerUserModel.Divider -> VIEW_TYPE_DIVIDER
            is DrawerUserModel.ManageAccounts -> VIEW_TYPE_MANAGE
        }

    // endregion

    // region view holders
    abstract class ViewHolder<DUM : DrawerUserModel>(itemView: View) : ClickableAdapter.ViewHolder<DUM>(itemView)

    private class NavUserViewHolder(itemView: View) : ViewHolder<DrawerUserModel.User>(itemView) {
        override fun onBind(item: DrawerUserModel.User) = with(itemView) {
            super.onBind(item)
            userName.text = item.displayName
            userEmailAddress.text = item.emailAddress
            userEmailAddress.visibility =
                if (item.emailAddress.isEmpty()) View.GONE else View.VISIBLE
            userAvatar.setAccountLetters(item.displayName)
            if (item.notificationsSnoozed) {
                buttonUserQuickSnooze.setImageResource(R.drawable.ic_notifications_off)
            } else {
                buttonUserQuickSnooze.setImageResource(R.drawable.ic_notifications_active)
            }
            if (item.loggedIn) {
                userSignIn.visibility = View.GONE
                userSignedIn.visibility = View.VISIBLE
                userNotifications.text = "${item.notificationCount}"
                userNotifications.setNotificationIndicatorSize(item.notificationCount)
                userNotifications.visibility = if (item.notificationCount == 0) View.GONE else View.VISIBLE
            } else {
                userName.setStyle(R.style.DrawerNameText_Red)
                userEmailAddress.setStyle(R.style.DrawerEmailAddressText_Red)
                userSignIn.setOnClickListener {
                    val intent = Intent(context, ConnectAccountActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    intent.putExtra(EXTRA_USERNAME, item.name)
                    // TODO: do not start Activity here, manage though callback
                    startActivity(context, AppUtil.decorInAppIntent(intent), null)
                }
            }
        }
    }

    private class ManageAccountsViewHolder(itemView: View) : ViewHolder<DrawerUserModel.ManageAccounts>(itemView) {
        override fun onBind(item: DrawerUserModel.ManageAccounts) = with(itemView) {
            super.onBind(item)
            // Scoping the Adapter's click listener to a particular view
            manageAccounts.onClick { clickListener(item) }
            setOnClickListener(null)
        }
    }

    private class DividerViewHolder(itemView: View) : ViewHolder<DrawerUserModel.Divider>(itemView)
    // endregion
}