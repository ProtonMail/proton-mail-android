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
package ch.protonmail.android.api.models.room.contacts

import ch.protonmail.android.api.models.ContactEncryptedDataMatcher
import ch.protonmail.android.testAndroidInstrumented.HamcrestMismatchBuilder
import ch.protonmail.android.testAndroidInstrumented.build
import org.hamcrest.Description
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeDiagnosingMatcher

/**
 * Created by Kamil Rajtar on 07.09.18.  */
class FullContactsDetailsMatcher(fullContactDetails:FullContactDetails):TypeSafeDiagnosingMatcher<FullContactDetails>() {
	private val contactId=`is`(equalTo(fullContactDetails.contactId))
	private val name=`is`(equalTo(fullContactDetails.name))
	private val uid=`is`(equalTo(fullContactDetails.uid))
	private val createTime=`is`(equalTo(fullContactDetails.createTime))
	private val modifyTime=`is`(equalTo(fullContactDetails.modifyTime))
	private val size=`is`(equalTo(fullContactDetails.size))
	private val emails=`is`(equalTo(fullContactDetails.emails))
	private val encryptedData=fullContactDetails.encryptedData?.let{encryptedData->
			contains(encryptedData.map {`is`(ContactEncryptedDataMatcher(it))})}?: `is`(nullValue())

	override fun describeTo(description:Description) {
		description.build("contactId" to contactId,
				"name" to name,
				"uid" to uid,
				"createTime" to createTime,
				"modifyTime" to modifyTime,
				"size" to size,
				"emails" to emails,
				"encryptedData" to encryptedData)
	}

	override fun matchesSafely(item:FullContactDetails,mismatchDescription:Description):Boolean {
		return HamcrestMismatchBuilder(mismatchDescription)
				.match("contactId",contactId,item.contactId)
				.match("name",name,item.name)
				.match("uid",uid,item.uid)
				.match("createTime",createTime,item.createTime)
				.match("modifyTime",modifyTime,item.modifyTime)
				.match("size",size,item.size)
				.match("emails",emails,item.emails)
				.match("encryptedData",encryptedData,item.encryptedData)
				.build()
	}
}