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
import android.util.AttributeSet
import android.view.View.OnClickListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import ch.protonmail.android.R
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.locks.LockIcon
import kotlinx.android.synthetic.main.message_recipient.view.*

/**
 * Created by Kamil Rajtar on 13.08.18.  */
class MessageDetailsRecipientView @JvmOverloads constructor(
		context:Context,attrs:AttributeSet?=null,defStyleAttr:Int=0): ConstraintLayout(context,
		attrs,
		defStyleAttr) {
	init {
		inflate(context,R.layout.message_recipient,this)
		val typefacePgp=Typeface.createFromAsset(context.assets,"pgp-icons-android.ttf")
		receiver_drop_down.typeface=typefacePgp
		pgp_icon.typeface=typefacePgp
	}

	fun bind(senderName:String?,
			 senderEmail:String,
			 senderLock:LockIcon,
			 openContextMenuCallbackFactory:OnClickListener) {
		val nameText=if(senderName!=null&&senderName.isNotEmpty()) senderName else senderEmail
		setName(nameText)
		setEmail(senderEmail)
		setIconLock(senderLock)
		setOnOpenContextMenuListener(openContextMenuCallbackFactory)
	}

	 fun setIconLock(lockIcon:LockIcon) {
		lockIcon.apply {
			val description=tooltip
			val onClickListener=if(description == 0) null else OnClickListener { context.showToast(description) }
			pgp_icon.setOnClickListener(onClickListener)
			pgp_icon.text=context.getString(icon)
			pgp_icon.setTextColor(ContextCompat.getColor(context,color))
		}
	}

	private fun setOnOpenContextMenuListener(onClickListener:OnClickListener) {
		receiver_drop_down.setOnClickListener(onClickListener)
		email.setOnClickListener(onClickListener)
	}

	private fun setName(text:CharSequence) {
		name.text=text
	}

	private fun setEmail(text:CharSequence) {
		email.text=text
	}
}
