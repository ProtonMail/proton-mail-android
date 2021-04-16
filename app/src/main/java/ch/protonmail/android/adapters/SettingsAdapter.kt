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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ch.protonmail.android.R
import ch.protonmail.android.activities.settings.SettingsEnum
import ch.protonmail.android.uiModel.SettingsItemUiModel
import ch.protonmail.android.views.SettingsDefaultItemView
import ch.protonmail.libs.core.ui.adapter.BaseAdapter
import ch.protonmail.libs.core.ui.adapter.ClickableAdapter
import me.proton.core.presentation.utils.inflate
import java.util.Locale

// region constants
const val VIEW_TYPE_SECTION = 0
const val VIEW_TYPE_ITEM = 1
// endregion

internal class SettingsAdapter :
    BaseAdapter<SettingsItemUiModel, SettingsAdapter.ViewHolder<SettingsItemUiModel>>(ModelsComparator) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<SettingsItemUiModel> =
        parent.viewHolderForViewType(viewType)

    override fun getItemViewType(position: Int) = items[position].viewType

    private object ModelsComparator : BaseAdapter.ItemsComparator<SettingsItemUiModel>() {

        override fun areItemsTheSame(oldItem: SettingsItemUiModel, newItem: SettingsItemUiModel): Boolean = false
    }

    abstract class ViewHolder<Model>(itemView: View) :
        ClickableAdapter.ViewHolder<Model>(itemView)

    companion object {

        fun getHeader(settingsId: String, context: Context): String =
            SettingsEnum.valueOf(settingsId).getHeader(context)
    }

    private class SectionViewHolder(itemView: View) : ViewHolder<SettingsItemUiModel>(itemView) {

        override fun onBind(item: SettingsItemUiModel) = with(itemView as TextView) {
            super.onBind(item)
            // TODO after we receive translations for TURKISH remove excess toUpperCase methods
            text = if (item.settingHeader.isNullOrEmpty())
                getHeader(item.settingId.toUpperCase(Locale.ENGLISH), context)
            else
                item.settingHeader
        }
    }

    class ItemViewHolder(itemView: View) : ViewHolder<SettingsItemUiModel>(itemView) {

        lateinit var header: String

        override fun onBind(item: SettingsItemUiModel) = with(itemView as SettingsDefaultItemView) {
            super.onBind(item)

            header = if (item.settingHeader.isNullOrEmpty()) {
                getHeader(item.settingId.toUpperCase(Locale.ENGLISH), context)
            } else {
                item.settingHeader!!
            }

            setSettingHeading(header)
            setHasValue(item.settingHasValue)
            setItemType(item.settingType!!.ordinal)
            setSettingValue(item.settingValue)

            checkToggle(item.enabled)
            setIconVisibility(item.iconVisibility)
            setSettingDisabled(item.settingDisabled, item.settingsDescription)

            setToggleChangedListener(item.toggleListener)
        }
    }

    private val SettingsItemUiModel.viewType: Int
        get() {
            return if (this.isSection) VIEW_TYPE_SECTION else VIEW_TYPE_ITEM
        }

    private fun layoutForViewType(viewType: Int) = when (viewType) {
        VIEW_TYPE_SECTION -> R.layout.settings_section
        VIEW_TYPE_ITEM -> R.layout.settings_item
        else -> throw IllegalArgumentException("View type not found: '$viewType'")
    }

    private fun <Model> ViewGroup.viewHolderForViewType(viewType: Int): ViewHolder<Model> {
        val view = inflate(layoutForViewType(viewType))
        @Suppress("UNCHECKED_CAST") // Type cannot be checked since is in invariant position
        return when (viewType) {
            VIEW_TYPE_SECTION -> SectionViewHolder(view)
            VIEW_TYPE_ITEM -> ItemViewHolder(view)
            else -> throw IllegalArgumentException("View type not found: '$viewType'")
        } as ViewHolder<Model>
    }
}
