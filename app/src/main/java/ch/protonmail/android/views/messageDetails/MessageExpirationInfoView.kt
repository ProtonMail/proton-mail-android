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
package ch.protonmail.android.views.messageDetails

import android.content.Context
import android.graphics.Typeface
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import ch.protonmail.android.R
import ch.protonmail.android.utils.DateUtil
import kotlinx.android.synthetic.main.view_message_expiration_info.view.*

/**
 * Created by Kamil Rajtar on 13.08.18.  */
class MessageExpirationInfoView @JvmOverloads constructor(
		context:Context,attrs:AttributeSet?=null,defStyleAttr:Int=0
): ConstraintLayout(context,attrs,defStyleAttr) {

	init {
		inflate(context,R.layout.view_message_expiration_info,this)
		val typeface=Typeface.createFromAsset(context.assets,"protonmail-mobile-icons.ttf")
		expiration_icon.typeface=typeface
		expiration_time.setTypeface(null,Typeface.ITALIC)
	}

	var remainingSeconds:Long=0
	set(value){
		expiration_time.text=String.format(resources.getString(R.string.expires_in),
				DateUtil.formatDaysAndHours(context,value))
		field=value
	}

}