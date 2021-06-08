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
package ch.protonmail.android.views.navigation

import android.content.Context
import android.graphics.PorterDuff
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.util.AttributeSet
import android.view.View
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.nav_item_label.view.*

/**
 * Created by Kamil Rajtar on 20.08.18.  */
class NavigationItemLabelView @JvmOverloads constructor(
		context:Context,attrs:AttributeSet?=null,defStyleAttr:Int=0
): ConstraintLayout(context,attrs,defStyleAttr)
{
	init {
		inflate(context,R.layout.nav_item_label,this)
	}

	fun bind(name:CharSequence,@DrawableRes labelRes:Int,@ColorInt labelColor:Int,notificationCount:Int)
	{
		label_name.text=name

		val normalDrawable =
			ContextCompat.getDrawable(context,labelRes)?:throw RuntimeException("Cannot find drawable for $labelRes")
		val wrapDrawable = DrawableCompat.wrap(normalDrawable)
		DrawableCompat.setTint(wrapDrawable,labelColor)
		wrapDrawable.mutate().setColorFilter(labelColor,PorterDuff.Mode.SRC_IN)
		label_icon.setImageDrawable(wrapDrawable)

		if(notificationCount>0) {
			notifications.visibility=View.VISIBLE
			notifications.text=notificationCount.toString()
		}
	}
}
