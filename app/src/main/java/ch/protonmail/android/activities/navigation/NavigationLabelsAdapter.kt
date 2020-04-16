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
package ch.protonmail.android.activities.navigation

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import ch.protonmail.android.R
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.views.navigation.NavigationItemLabelView

/**
 * Created by Kamil Rajtar on 20.08.18.  */
class NavigationLabelsAdapter(context:Context):ArrayAdapter<LabelWithUnreadCounter>(
		context,
		android.R.layout.simple_list_item_1) {
	override fun getView(position:Int,convertView:View?,parent:ViewGroup):View {
		val label=getItem(position) ?: throw RuntimeException("No item at position: $position")
		val labelItemView=convertView as? NavigationItemLabelView
				?: NavigationItemLabelView(context)
		val name=label.label.name
		var color=label.label.color
		if(!TextUtils.isEmpty(color)) {
			color=UiUtil.normalizeColor(color)
		}
		val labelColor=if(color.isNotEmpty())
			Color.parseColor(color)
		else {
			0
		}
		val labelDrawable=if(label.label.exclusive) {
			R.drawable.ic_menu_folder
		} else {
			R.drawable.ic_menu_label
		}
		val notificationCount=label.unreadCount
		labelItemView.bind(name,labelDrawable,labelColor,notificationCount)
		return labelItemView
	}
}
