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

package ch.protonmail.android.drawer.presentation.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel
import ch.protonmail.android.drawer.presentation.ui.DrawerAdapter
import ch.protonmail.android.mailbox.domain.model.UnreadCounter

internal class ProtonSideDrawer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val bodyRecyclerView: RecyclerView
    private val bodyAdapter = DrawerAdapter()

    // lists
    private var locationItems = listOf<DrawerItemUiModel.Primary.Static>()
    private var foldersSectionItem: DrawerItemUiModel.SectionName? = null
    private var folderItems = listOf<DrawerItemUiModel.Primary.Label>()
    private var labelsSectionItem: DrawerItemUiModel.SectionName? = null
    private var labelItems = listOf<DrawerItemUiModel.Primary.Label>()
    private var moreSectionItem: DrawerItemUiModel.SectionName? = null
    private var moreItems = listOf<DrawerItemUiModel.Primary.Static>()
    private var footerItem: DrawerItemUiModel.Footer? = null

    init {
        bodyRecyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bodyAdapter
        }

        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(bodyRecyclerView)
        }

        addView(linearLayout)
    }

    fun setOnItemClick(block: (DrawerItemUiModel) -> Unit) {
        bodyAdapter.onItemClick = { drawerItemUiModel ->
            block(drawerItemUiModel)
            if (drawerItemUiModel is DrawerItemUiModel.Primary)
                bodyAdapter.setSelected(drawerItemUiModel)
        }
    }

    fun setLocationItems(items: List<DrawerItemUiModel.Primary.Static>) {
        locationItems = items
        update()
    }

    /**
     * Set unread counters to "Location items"
     * @see setLocationItems
     *
     * @see DrawerItemUiModel.Primary.Static.Type.itemId
     */
    internal fun setUnreadCounters(counters: List<UnreadCounter>) {
        val countersMap = counters.map { it.labelId to it.unreadCount }.toMap()
        setUnreadCounters(countersMap)
    }

    /**
     * Set unread counters to "Location items"
     * @see setLocationItems
     *
     * @param locationsToUnreadsMap [Map] associating type's id to the count of unread Messages
     * @see DrawerItemUiModel.Primary.Static.Type.itemId
     */
    @JvmName("SetUnreadCountersByLocations")
    internal fun setUnreadCounters(locationsToUnreadsMap: Map<Int, Int>) {
        val countersMap = locationsToUnreadsMap.mapKeys { it.toString() }
        setUnreadCounters(countersMap)
    }

    private fun setUnreadCounters(labelIdsToUnreadsMap: Map<String, Int>) {
        fun <T : DrawerItemUiModel.Primary> List<T>.mapWithCounters(
            labelIdsToUnreadsMap: Map<String, Int>,
            getDrawerItemLabelId: (T) -> String
        ): List<T> = map { drawerItem ->
            // Get unread count by id of item's type from unread
            val unreadCount = labelIdsToUnreadsMap.getOrElse(getDrawerItemLabelId(drawerItem)) { 0 }
            // Update the notificationCount for the item
            @Suppress("UncheckedCast") // We know the item is T
            drawerItem.copyWithNotificationCount(unreadCount) as T
        }

        locationItems = locationItems.mapWithCounters(labelIdsToUnreadsMap) { it.type.itemId.toString() }
        folderItems = folderItems.mapWithCounters(labelIdsToUnreadsMap) { it.uiModel.labelId }
        labelItems = labelItems.mapWithCounters(labelIdsToUnreadsMap) { it.uiModel.labelId }
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

    fun scrollToInitialPosition() {
        bodyRecyclerView.smoothScrollToPosition(0)
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
