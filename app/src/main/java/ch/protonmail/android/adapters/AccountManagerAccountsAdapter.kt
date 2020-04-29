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

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.PopupMenu
import ch.protonmail.android.R
import ch.protonmail.android.uiModel.AccountManagerUserModel
import ch.protonmail.android.utils.extensions.inflate
import ch.protonmail.android.utils.extensions.setAccountLetters
import ch.protonmail.android.utils.extensions.setStyle
import ch.protonmail.libs.core.ui.adapter.BaseAdapter
import ch.protonmail.libs.core.ui.adapter.ClickableAdapter
import ch.protonmail.libs.core.utils.onClick
import kotlinx.android.synthetic.main.user_list_item.view.*
import kotlinx.android.synthetic.main.user_list_item_footer.view.*

// region constants
private const val VIEW_TYPE_USER = 0 // for user list item in accounts manager screen
private const val VIEW_TYPE_DIVIDER = 1 // for divider in every screen
private const val VIEW_TYPE_ADD = 2 // for footer list item in accounts manager screen
// endregion

/**
 * Adapter for Users for [ch.protonmail.android.activities.multiuser.AccountManagerActivity]
 * Inherits from [BaseAdapter]
 */
internal class AccountManagerAccountsAdapter(
    onItemClick: (AccountManagerUserModel) -> Unit,
    private val onLogin: (AccountManagerUserModel.User) -> Unit,
    private val onLogout: (AccountManagerUserModel.User) -> Unit,
    private val onRemove: (AccountManagerUserModel.User) -> Unit
) : BaseAdapter<AccountManagerUserModel, AccountManagerAccountsAdapter.ViewHolder<AccountManagerUserModel>>(
    ModelsComparator, onItemClick
) {

    var onLogoutAccount: (String) -> Unit = {  }
    var onRemoveAccount: (String) -> Unit = {  }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<AccountManagerUserModel> =
        parent.viewHolderForViewType(viewType)

    /** @return [Int] that identifies the View type for the Item at the given [position] */
    override fun getItemViewType(position: Int) = items[position].viewType

    /** A [BaseAdapter.ItemsComparator] for the Adapter */
    private object ModelsComparator : BaseAdapter.ItemsComparator<AccountManagerUserModel>() {
        override fun areItemsTheSame(oldItem: AccountManagerUserModel, newItem: AccountManagerUserModel): Boolean {
            // TODO: document why always false
            return false
        }
    }

    /** @return [LayoutRes] for the given [viewType] */
    @LayoutRes
    private fun layoutForViewType(viewType: Int) = when (viewType) {
        VIEW_TYPE_USER -> R.layout.user_list_item
        VIEW_TYPE_DIVIDER -> R.layout.drawer_list_item_divider
        VIEW_TYPE_ADD -> R.layout.user_list_item_footer
        else -> throw IllegalArgumentException("View type not found: '$viewType'")
    }

    // region extension functions

    private fun <DUM : AccountManagerUserModel> ViewGroup.viewHolderForViewType(viewType: Int): ViewHolder<DUM> {
        val view = inflate(layoutForViewType(viewType))
        @Suppress("UNCHECKED_CAST") // Type cannot be checked since is in invariant position
        return when (viewType) {
            VIEW_TYPE_USER -> AccUserViewHolder(view, onLogin, onLogout, onRemove)
            VIEW_TYPE_DIVIDER -> DividerViewHolder(view)
            VIEW_TYPE_ADD -> AddAccountViewHolder(view)
            else -> throw IllegalArgumentException("View type not found: '$viewType'")
        } as ViewHolder<DUM>
    }

    /** @return [Int] view type for the receiver [AccountManagerUserModel] */
    private val AccountManagerUserModel.viewType: Int
        get() = when (this) {
            is AccountManagerUserModel.User -> VIEW_TYPE_USER
            is AccountManagerUserModel.Divider -> VIEW_TYPE_DIVIDER
            is AccountManagerUserModel.AddAccount -> VIEW_TYPE_ADD
        }

    // endregion

    // region view holders
    abstract class ViewHolder<DUM : AccountManagerUserModel>(itemView: View) : ClickableAdapter.ViewHolder<DUM>(itemView)

    private class AccUserViewHolder(
        itemView: View,
        private val onLogin: (AccountManagerUserModel.User) -> Unit,
        private val onLogout: (AccountManagerUserModel.User) -> Unit,
        private val onRemove: (AccountManagerUserModel.User) -> Unit
    ) : ViewHolder<AccountManagerUserModel.User>(itemView) {

        override fun onBind(item: AccountManagerUserModel.User) = with (itemView) {
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
                val popupMenu = PopupMenu(itemView.context, accUserMoreMenu).apply {
                    val menuRes = if (item.loggedIn)
                        R.menu.account_item_menu else R.menu.account_item_loggedout_menu
                    menuInflater.inflate(menuRes, menu)
                    gravity = Gravity.END
                }
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_login -> {
                            onLogin(item)
                        }
                        R.id.action_logout_account -> {
                            onLogout(item)
                        }
                        R.id.action_remove_account -> {
                            onRemove(item)
                        }
                    }
                    true
                }
                popupMenu.show()
            }
        }
    }

    private class DividerViewHolder(itemView: View) : ViewHolder<AccountManagerUserModel.Divider>(itemView)

    private class AddAccountViewHolder(itemView: View) : ViewHolder<AccountManagerUserModel.AddAccount>(itemView) {
        override fun onBind(item: AccountManagerUserModel.AddAccount) = with(itemView) {
            super.onBind(item)
            // Scoping the Adapter's click listener to a particular view
            addNewUserAccount.onClick { clickListener(item) }
            setOnClickListener(null)
        }
    }
    // endregion
}