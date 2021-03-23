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
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableRow
import androidx.constraintlayout.widget.ConstraintLayout
import ch.protonmail.android.R
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.RecipientType
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.ui.locks.RecipientLockIcon
import ch.protonmail.android.utils.ui.locks.SenderLockIcon
import kotlinx.android.synthetic.main.layout_message_details_recipients.view.*

/**
 * Created by Kamil Rajtar on 14.08.18.  */
class MessageDetailsRecipientsLayout @JvmOverloads constructor(
		context:Context,attrs:AttributeSet?=null,defStyleAttr:Int=0
): ConstraintLayout(context,attrs,defStyleAttr) {
	init {
		inflate(context,R.layout.layout_message_details_recipients,this)
	}

	fun bind(message: Message, contextMenuFactory: (String) -> OnClickListener) {
		val hasValidSignature = message.hasValidSignature
		val hasInvalidSignature = message.hasInvalidSignature
		val senderLock = SenderLockIcon(message, hasValidSignature, hasInvalidSignature)

		val senderName = message.senderDisplayName ?: message.senderName
		val senderEmail = message.senderEmail
		val toList = message.getList(RecipientType.TO)
		val ccList = message.getList(RecipientType.CC)

		val shortenedRecipientsToText = toList.truncateToList(ccList)
		val detailedDateText = DateUtil.formatDetailedDateTime(context, message.timeMs)
		val openContextMenuCallback = contextMenuFactory.invoke(senderEmail)

		shortenedRecipientsTextView.text = shortenedRecipientsToText

		detailedDateTextView.text = detailedDateText

		shortenedSenderView.bind(senderName, senderEmail, senderLock, openContextMenuCallback)
		expandedSenderView.bind(senderName, senderEmail, senderLock, openContextMenuCallback)

		val parsedHeaders = message.parsedHeaders
		val recipientsEncryption = parsedHeaders?.recipientEncryption ?: mapOf()
		val recipientsAuthentication = parsedHeaders?.recipientAuthentication ?: mapOf()

		RecipientType.values().forEach {
			val tableRow = it.getTableRow()
			val recipientsLayout = it.getRecipientsLayout()
			val recipientsList = message.getList(it)
			addRecipientsToView(tableRow,
					recipientsLayout,
					recipientsList,
					recipientsEncryption,
					recipientsAuthentication,
					contextMenuFactory)
		}
	}

	private fun RecipientType.getTableRow():TableRow {
		return when(this) {
			RecipientType.TO->toTableRow
			RecipientType.CC->ccTableRow
			RecipientType.BCC->bccTableRow
		}
	}

	private fun RecipientType.getRecipientsLayout():LinearLayout {
		return when(this) {
			RecipientType.TO->toLinearLayout
			RecipientType.CC->ccLinearLayout
			RecipientType.BCC->bccLinearLayout
		}
	}

	private fun addRecipientsToView(tableRow:TableRow,layout:ViewGroup,
									recipientList:List<MessageRecipient>,
									recipientsEncryption:Map<String,String>,
									recipientsAuthentication:Map<String,String>,
									contextMenuFactory:(String)->OnClickListener) {
		if(recipientList.isEmpty()) {
			tableRow.visibility=View.GONE
			return
		}
		tableRow.visibility=View.VISIBLE
		layout.removeAllViews()
		for(recipient in recipientList) {
			val recipientName=recipient.name
			val recipientEmail=recipient.emailAddress
			val recipientEnc:String?=recipientsEncryption[recipient.emailAddress] ?: "none"
			val recipientAuth:String?=recipientsAuthentication[recipient.emailAddress] ?: "none"
			val lockIcon=RecipientLockIcon(recipientEnc,recipientAuth)
			val onContextMenuListener=contextMenuFactory(recipientEmail)

			val recipientView=MessageDetailsRecipientView(context)
			recipientView.bind(recipientName,recipientEmail,lockIcon,
					onContextMenuListener)

			layout.addView(recipientView)
		}
	}

	private fun List<MessageRecipient>.truncateToList(ccList: List<MessageRecipient>): String {
		val ccListSize = ccList.size
		var listForShowing = this

		if (isEmpty()) {
			listForShowing = ccList
			if (ccList.isEmpty()) {
				return context.getString(R.string.undisclosed_recipients)
			}
		}

		var firstName: String? = listForShowing[0].name
		if (firstName.isNullOrEmpty()) {
			firstName = listForShowing[0].emailAddress
		}

		if (firstName!!.length > 40) {
			firstName = firstName.substring(0, 40) + "..."
		}

		val extraNamesCount = if (!isEmpty()) {
			listForShowing.size - 1 + ccListSize
		} else {
			listForShowing.size - 1
		}
		return when (extraNamesCount) {
			0 -> firstName
			else -> "$firstName, +$extraNamesCount "
		}
	}

	var isExpanded=false
		set(value) {
			val shortenedViewVisibility:Int
			val expandedViewVisibility:Int
			if(value) {
				shortenedViewVisibility=View.GONE
				expandedViewVisibility=View.VISIBLE
			} else {
				shortenedViewVisibility=View.VISIBLE
				expandedViewVisibility=View.GONE
			}
			messageShortenedDetails.visibility = shortenedViewVisibility
			messageExpandedDetails.visibility = expandedViewVisibility
			field=value
		}
}
