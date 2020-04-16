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

import androidx.room.TypeConverter
import android.util.Base64
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.utils.Logger
import java.io.*

// region constants
private const val TAG_FULL_CONTACT_DETAILS_CONVERTER = "FullContactDetailsConverter"
// endregion

/**
 * Created by Kamil Rajtar on 21.08.18.
 */

class FullContactDetailsConverter{
	@TypeConverter
	fun contactEncryptedDataListToString(contactEncryptedDataList:List<ContactEncryptedData>?):String? {
		val out=ByteArrayOutputStream()
		try {
			val encryptedDataArray=contactEncryptedDataList?.toTypedArray()?: arrayOf()
			ObjectOutputStream(out).writeObject(encryptedDataArray)
		} catch(e:IOException) {
			Logger.doLogException(TAG_FULL_CONTACT_DETAILS_CONVERTER,
					"Serialization of encrypted data failed ",
					e)
		}

		return Base64.encodeToString(out.toByteArray(),Base64.DEFAULT)
	}

	@TypeConverter
	fun stringToContactEncryptedDataList(contactEncryptedDataString:String?):List<ContactEncryptedData>? {
		val inputStream=ByteArrayInputStream(Base64.decode(contactEncryptedDataString,Base64.DEFAULT))
		return try {
			val resultArray=ObjectInputStream(inputStream).readObject() as Array<ContactEncryptedData>
			resultArray.toList()
		} catch(e:Exception) {
			Logger.doLogException(TAG_FULL_CONTACT_DETAILS_CONVERTER,"DeSerialization of recipients failed",e)
			null
		}
	}
}
