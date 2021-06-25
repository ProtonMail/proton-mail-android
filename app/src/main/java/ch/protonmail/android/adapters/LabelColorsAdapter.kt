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
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import ch.protonmail.android.R
import ch.protonmail.android.adapters.LabelColorsAdapter.LabelColorItem
import java.util.ArrayList

class LabelColorsAdapter(context: Context, colors: IntArray, private val mLayoutResourceId: Int) :
    ArrayAdapter<LabelColorItem>(
        context, mLayoutResourceId
    ) {

    private val labelColorItemList: MutableList<LabelColorItem>
    fun setChecked(position: Int) {
        for (item in labelColorItemList) {
            item.isChecked = false
        }
        val item = getItem(position)
        item?.isChecked = true
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(mLayoutResourceId, parent, false)
        val item = getItem(position) ?: return view
        selectItem(view, item)
        return view
    }

    private fun selectItem(view: View, item: LabelColorItem) {
        val circle = view.findViewById<View>(R.id.color_item)
        circle.backgroundTintList = ColorStateList.valueOf(item.colorId)
        val checkView = view.findViewById<View>(R.id.is_checked_indicator)
        if (item.isChecked) {
            checkView.visibility = View.VISIBLE
        } else {
            checkView.visibility = View.GONE
        }
    }

    init {
        labelColorItemList = ArrayList()
        for (color in colors) {
            val item = LabelColorItem()
            item.colorId = color
            item.isChecked = false
            labelColorItemList.add(item)
        }
        addAll(labelColorItemList)
        setNotifyOnChange(false)
    }

    class LabelColorItem {

        var isChecked = false
        var colorId = 0
    }
}
