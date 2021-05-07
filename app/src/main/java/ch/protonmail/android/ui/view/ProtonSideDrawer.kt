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

package ch.protonmail.android.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.adapters.DrawerAdapter
import ch.protonmail.android.uiModel.DrawerItemUiModel
import ch.protonmail.android.utils.extensions.inflate
import ch.protonmail.android.views.DrawerHeaderView

internal class ProtonSideDrawer @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val headerView = inflate(R.layout.proton_nav_view_section_header) as DrawerHeaderView
    private val bodyAdapter = DrawerAdapter()

    // lists
    private var isSnoozeEnabled: Boolean = false
    private var locationItems = listOf<DrawerItemUiModel.Primary.Static>()
    private var foldersSectionItem: DrawerItemUiModel.SectionName? = null
    private var folderItems = listOf<DrawerItemUiModel.Primary.Label>()
    private var labelsSectionItem: DrawerItemUiModel.SectionName? = null
    private var labelItems = listOf<DrawerItemUiModel.Primary.Label>()
    private var moreSectionItem: DrawerItemUiModel.SectionName? = null
    private var moreItems = listOf<DrawerItemUiModel.Primary.Static>()
    private var footerItem: DrawerItemUiModel.Footer? = null

    init {
        setBackgroundResource(R.color.nav_view_background)

        val body = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bodyAdapter
        }

        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            addView(headerView)
            addView(body)
        }

        addView(linearLayout)
    }

    fun isHeaderOpen() =
        headerView.state == DrawerHeaderView.State.OPENED

    fun switchHeaderOpenState() {
        headerView.switchState()
    }

    fun setOnItemClick(block: (DrawerItemUiModel) -> Unit) {
        bodyAdapter.onItemClick = { drawerItemUiModel ->
            block(drawerItemUiModel)
            if (drawerItemUiModel is DrawerItemUiModel.Primary)
                bodyAdapter.setSelected(drawerItemUiModel)
        }
    }

    fun setUser(item: DrawerItemUiModel.Header) {
        headerView.setUser(name = item.name, emailAddress = item.email)
    }

    fun setSnoozeEnabled(isEnabled: Boolean) {
        isSnoozeEnabled = isEnabled
        update()
    }

    fun setLocationItems(items: List<DrawerItemUiModel.Primary.Static>) {
        locationItems = items
        update()
    }

    fun setFolderItems(sectionName: CharSequence, items: List<DrawerItemUiModel.Primary.Label>) {
        foldersSectionItem = DrawerItemUiModel.SectionName(sectionName)
        folderItems = items
        update()
    }

    fun setFolderItems(@StringRes sectionNameRes: Int, items: List<DrawerItemUiModel.Primary.Label>) {
        foldersSectionItem = DrawerItemUiModel.SectionName(context.getText(sectionNameRes))
        folderItems = items
        update()
    }

    fun setLabelItems(@StringRes sectionNameRes: Int, items: List<DrawerItemUiModel.Primary.Label>) {
        labelsSectionItem = DrawerItemUiModel.SectionName(context.getText(sectionNameRes))
        labelItems = items
        update()
    }

    fun setMoreItems(@StringRes sectionNameRes: Int, items: List<DrawerItemUiModel.Primary.Static>) {
        moreSectionItem = DrawerItemUiModel.SectionName(context.getText(sectionNameRes))
        moreItems = items
        update()
    }

    fun setFooterText(text: CharSequence) {
        footerItem = DrawerItemUiModel.Footer(text)
        update()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun update() {
        bodyAdapter.items = buildList {
            addAll(locationItems)
            foldersSectionItem?.let { add(it) }
            addAll(folderItems)
            labelsSectionItem?.let { add(it) }
            addAll(labelItems)
            moreSectionItem?.let { add(it) }
            addAll(moreItems)
            footerItem?.let { add(it) }
        }
    }
}
