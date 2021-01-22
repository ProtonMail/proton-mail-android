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
package ch.protonmail.android.api.models

import ch.protonmail.android.testAndroidInstrumented.HamcrestMismatchBuilder
import ch.protonmail.android.testAndroidInstrumented.build
import org.hamcrest.Description
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.TypeSafeDiagnosingMatcher

/**
 * Created by Kamil Rajtar on 07.09.18.  */
class MessageRecipientMatcher(messageRecipient:MessageRecipient):TypeSafeDiagnosingMatcher<MessageRecipient>() {
	private val name=`is`(equalTo(messageRecipient.Name))
	private val address=`is`(equalTo(messageRecipient.Address))
	private val icon=`is`(equalTo(messageRecipient.mIcon))
	private val iconColor=`is`(equalTo(messageRecipient.mIconColor))
	private val description=`is`(equalTo(messageRecipient.mDescription))
	private val isPgp=`is`(equalTo(messageRecipient.mIsPGP))

	override fun describeTo(description:Description) {
		description.build("name" to name,
				"address" to address,
				"icon" to icon,
				"iconColor" to iconColor,
				"description" to this.description,
				"isPgp" to isPgp)
	}

	override fun matchesSafely(item:MessageRecipient,mismatchDescription:Description):Boolean {
		return HamcrestMismatchBuilder(mismatchDescription)
				.match("name",name,item.Name)
				.match("address",address,item.Address)
				.match("icon", icon,item.icon)
				.match("iconColor",iconColor,item.iconColor)
				.match("description",description,item.description)
				.match("isPgp",isPgp,item.isPGP)
				.build()
	}
}

