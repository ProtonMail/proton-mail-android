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

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat.startActivityForResult
import ch.protonmail.android.R
import ch.protonmail.android.activities.REQUEST_CODE_ACCOUNT_MANAGER
import ch.protonmail.android.activities.multiuser.AccountManagerActivity
import ch.protonmail.android.uiModel.DrawerUserModel
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.extensions.setAccountLetters
import ch.protonmail.android.utils.extensions.setNotificationIndicatorSize
import ch.protonmail.android.utils.extensions.setStyle
import ch.protonmail.libs.core.ui.adapter.BaseAdapter
import ch.protonmail.libs.core.ui.adapter.ClickableAdapter
import kotlinx.android.synthetic.main.drawer_user_list_item.view.*
import kotlinx.android.synthetic.main.drawer_user_list_item_footer.view.*
import kotlinx.android.synthetic.main.user_list_item.view.*
import kotlinx.android.synthetic.main.user_list_item_footer.view.*
import me.proton.core.domain.entity.UserId
import me.proton.core.presentation.ui.adapter.ProtonAdapter
import me.proton.core.presentation.utils.inflate

// region constants
private const val VIEW_TYPE_NAV_USER = 0 // for user list item in nav drawer list
private const val VIEW_TYPE_DIVIDER = 1 // for divider in every screen
private const val VIEW_TYPE_NAV_FOOTER = 2 // for footer list item in nav drawer list
private const val VIEW_TYPE_ACC_USER = 3 // for user list item in accounts manager screen
private const val VIEW_TYPE_ACC_FOOTER = 4 // for footer list item in accounts manager screen
// endregion

/**
 * Adapter for Drawer Users drop-down (Spinner)
 *
 * Inherits from [BaseAdapter]
 * TODO Inherit from [ProtonAdapter]
 *
 * TODO split to DrawerAccountsAdapter and AccountManagerAccountAdapter
 */
internal class AccountsAdapter :
    BaseAdapter<DrawerUserModel, AccountsAdapter.ViewHolder<DrawerUserModel>>(ModelsComparator) {

    var onLoginAccount: (UserId?) -> Unit = { }
    var onLogoutAccount: (UserId) -> Unit = { }
    var onRemoveAccount: (UserId) -> Unit = { }

    private val onLoginAccountInvoker: (UserId?) -> Unit get() = { onLoginAccount(it) }
    private val onLogoutAccountInvoker: (UserId) -> Unit get() = { onLogoutAccount(it) }
    private val onRemoveAccountInvoker: (UserId) -> Unit get() = { onRemoveAccount(it) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<DrawerUserModel> =
        parent.viewHolderForViewType(viewType)

    override fun onBindViewHolder(holder: ViewHolder<DrawerUserModel>, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.onLoginAccountInvoker = this.onLoginAccountInvoker
        holder.onLogoutAccountInvoker = this.onLogoutAccountInvoker
        holder.onRemoveAccountInvoker = this.onRemoveAccountInvoker
    }

    /** @return [Int] that identifies the View type for the Item at the given [position] */
    override fun getItemViewType(position: Int) = items[position].viewType

    abstract class ViewHolder<DUM : DrawerUserModel>(itemView: View) : ClickableAdapter.ViewHolder<DUM>(itemView) {
        internal var onLoginAccountInvoker: (UserId?) -> Unit = { }
        internal var onLogoutAccountInvoker: (UserId) -> Unit = { }
        internal var onRemoveAccountInvoker: (UserId) -> Unit = { }
    }

    /** A [BaseAdapter.ItemsComparator] for the Adapter */
    private object ModelsComparator : BaseAdapter.ItemsComparator<DrawerUserModel>() {
        override fun areItemsTheSame(oldItem: DrawerUserModel, newItem: DrawerUserModel): Boolean =
            false
    }

    /** @return [LayoutRes] for the given [viewType] */
    private fun layoutForViewType(viewType: Int) = when (viewType) {
        VIEW_TYPE_DIVIDER -> R.layout.drawer_list_item_divider
        VIEW_TYPE_NAV_FOOTER -> R.layout.drawer_user_list_item_footer
        VIEW_TYPE_ACC_FOOTER -> R.layout.user_list_item_footer
        VIEW_TYPE_NAV_USER -> R.layout.drawer_user_list_item
        VIEW_TYPE_ACC_USER -> R.layout.user_list_item
        else -> throw IllegalArgumentException("View type not found: '$viewType'")
    }

    // region extension functions

    private fun <DUM : DrawerUserModel> ViewGroup.viewHolderForViewType(viewType: Int): ViewHolder<DUM> {
        val view = inflate(layoutForViewType(viewType))
        @Suppress("UNCHECKED_CAST") // Type cannot be checked since is in invariant position
        return when (viewType) {
            VIEW_TYPE_DIVIDER -> DividerViewHolder(view)
            VIEW_TYPE_NAV_FOOTER -> FooterViewHolder(view)
            VIEW_TYPE_NAV_USER -> NavUserViewHolder(view)
            VIEW_TYPE_ACC_USER -> AccUserViewHolder(view)
            VIEW_TYPE_ACC_FOOTER -> AccFooterViewHolder(view)
            else -> throw IllegalArgumentException("View type not found: '$viewType'")
        } as ViewHolder<DUM>
    }

    /** @return [Int] view type for the receiver [DrawerUserModel] */
    private val DrawerUserModel.viewType: Int
        get() {
            return when (this) {
                is DrawerUserModel.Footer -> VIEW_TYPE_NAV_FOOTER
                is DrawerUserModel.Divider -> VIEW_TYPE_DIVIDER
                is DrawerUserModel.BaseUser.DrawerUser -> VIEW_TYPE_NAV_USER
                is DrawerUserModel.BaseUser.AccountUser -> VIEW_TYPE_ACC_USER
                is DrawerUserModel.AccFooter -> VIEW_TYPE_ACC_FOOTER
            }
        }

    // endregion

    // region view holders
    private class DividerViewHolder(itemView: View) : ViewHolder<DrawerUserModel.Divider>(itemView)

    private class AccFooterViewHolder(itemView: View) : ViewHolder<DrawerUserModel.AccFooter>(itemView) {
        override fun onBind(item: DrawerUserModel.AccFooter) = with(itemView) {
            super.onBind(item)
            addNewUserAccount.setOnClickListener {
                onLoginAccountInvoker.invoke(null)
            }
        }
    }

    private class FooterViewHolder(itemView: View) : ViewHolder<DrawerUserModel.Footer>(itemView) {
        override fun onBind(item: DrawerUserModel.Footer) = with(itemView) {
            super.onBind(item)
            manageAccounts.setOnClickListener {
                val activity = context as Activity
                startActivityForResult(
                    activity,
                    AppUtil.decorInAppIntent(Intent(context, AccountManagerActivity::class.java)),
                    REQUEST_CODE_ACCOUNT_MANAGER,
                    null
                )
                activity.overridePendingTransition(R.anim.slide_up, R.anim.slide_up_close)
            }
        }
    }

    private class AccUserViewHolder(itemView: View) : ViewHolder<DrawerUserModel.BaseUser.AccountUser>(itemView) {

        override fun onBind(item: DrawerUserModel.BaseUser.AccountUser) = with(itemView) {
            super.onBind(item)
            accUserName.text = item.displayName
            accUserEmailAddress.text = item.emailAddress
            accUserEmailAddress.visibility = if (item.emailAddress.isEmpty()) View.GONE else View.VISIBLE
            accUserAvatar.setAccountLetters(item.displayName)
            if (!item.loggedIn) {
                accUserName.text = String.format(context.getString(R.string.manage_accounts_user_loggedout), item.name)
                accUserName.setStyle(R.style.DrawerNameText_Red)
                accUserEmailAddress.setStyle(R.style.DrawerEmailAddressText_Red)
            } else if (item.primary) {
                accUserName.text = String.format(context.getString(R.string.manage_accounts_user_primary), item.displayName)
                accUserName.setTypeface(accUserName.typeface, Typeface.BOLD_ITALIC)
                accUserEmailAddress.setTypeface(accUserEmailAddress.typeface, Typeface.BOLD_ITALIC)
            }
            accUserMoreMenu.setOnClickListener {
                val popupMenu = PopupMenu(itemView.context, accUserMoreMenu)
                popupMenu.let {
                    it.menuInflater.inflate(
                        if (item.loggedIn) R.menu.account_item_menu
                        else R.menu.account_item_loggedout_menu, it.menu
                    )
                    it.gravity = Gravity.END
                }
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_remove_account -> {
                            onRemoveAccountInvoker(item.id)
                        }
                        R.id.action_logout_account -> {
                            onLogoutAccountInvoker(item.id)
                        }
                        R.id.action_login -> {
                            onLoginAccountInvoker(item.id)
                        }
                    }
                    true
                }
                popupMenu.show()
            }
        }
    }

    private class NavUserViewHolder(itemView: View) : ViewHolder<DrawerUserModel.BaseUser.DrawerUser>(itemView) {
        override fun onBind(item: DrawerUserModel.BaseUser.DrawerUser) = with(itemView) {
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
                userLoginStatusParent.visibility = View.GONE
                userSignedIn.visibility = View.VISIBLE
                userNotifications.text = "${item.notifications}"
                userNotifications.setNotificationIndicatorSize(item.notifications)
                userNotifications.visibility = if (item.notifications == 0) View.GONE else View.VISIBLE
            } else {
                userLoginStatusParent.visibility = View.VISIBLE
                userName.setStyle(R.style.DrawerNameText_Red)
                userEmailAddress.setStyle(R.style.DrawerEmailAddressText_Red)
            }
        }
    }
    // endregion
}
