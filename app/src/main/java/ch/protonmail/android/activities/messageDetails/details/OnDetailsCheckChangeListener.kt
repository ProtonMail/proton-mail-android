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
package ch.protonmail.android.activities.messageDetails.details

import android.widget.CompoundButton
import android.widget.TextView

import ch.protonmail.android.views.messageDetails.MessageDetailsRecipientsLayout

/**
 * Created by Kamil Rajtar on 14.08.18.
 */
internal class OnDetailsCheckChangeListener(private val recipientsLayout: MessageDetailsRecipientsLayout,
											private val messageTitle: TextView) : CompoundButton.OnCheckedChangeListener {

	override fun onCheckedChanged(compoundButton: CompoundButton, isExpanded: Boolean) {
		recipientsLayout.isExpanded = isExpanded
		messageTitle.setSingleLine(!isExpanded)
	}
}
