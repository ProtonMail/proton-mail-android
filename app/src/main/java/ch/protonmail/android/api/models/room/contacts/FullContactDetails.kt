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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.enumerations.ContactEncryption
import ch.protonmail.android.utils.VCardUtil
import ch.protonmail.android.utils.crypto.TextDecryptionResult
import ch.protonmail.android.utils.crypto.UserCrypto
import com.proton.gopenpgp.armor.Armor
import ezvcard.Ezvcard
import ezvcard.VCard

// region constants
const val TABLE_FULL_CONTACT_DETAILS = "fullContactsDetails"
const val COLUMN_CONTACT_ID = "ID"
const val COLUMN_CONTACT_NAME = "Name"
const val COLUMN_CONTACT_UID = "Uid"
const val COLUMN_CONTACT_CREATE_TIME = "CreateTime"
const val COLUMN_CONTACT_MODIFY_TIME = "ModifyTIme"
const val COLUMN_CONTACT_SIZE = "Size"
const val COLUMN_CONTACT_DEFAULTS = "Defaults"
const val COLUMN_CONTACT_ENCRYPTED_DATA = "EncryptedData"
// endregion

/**
 * Created by dkadrikj on 8/22/16.
 */

@Entity(tableName = TABLE_FULL_CONTACT_DETAILS)
data class FullContactDetails @Ignore constructor(
		@PrimaryKey
		@ColumnInfo(name = COLUMN_CONTACT_ID)
		val contactId: String,
		@ColumnInfo(name = COLUMN_CONTACT_NAME)
		var name: String? = null,
		@ColumnInfo(name = COLUMN_CONTACT_UID)
		val uid: String? = null,
		@ColumnInfo(name = COLUMN_CONTACT_CREATE_TIME)
		val createTime: Long = 0,
		@ColumnInfo(name = COLUMN_CONTACT_MODIFY_TIME)
		val modifyTime: Long = 0,
		@ColumnInfo(name = COLUMN_CONTACT_SIZE)
		val size: Int = 0,
		@ColumnInfo(name = COLUMN_CONTACT_DEFAULTS)
		val defaults: Int = 0,
		@Ignore
		var emails: List<ContactEmail>? = null,
		@ColumnInfo(name = COLUMN_CONTACT_ENCRYPTED_DATA)
		var encryptedData:MutableList<ContactEncryptedData>? = null
) {

	/**
	 * Room database constructor
	 */
	constructor(contactId:String,
				name:String?,
				uid:String?,
				createTime:Long,
				modifyTime:Long,
				size:Int,
				defaults:Int,
				encryptedData:MutableList<ContactEncryptedData>):this(
			contactId,
			name,
			uid,
			createTime,
			modifyTime,
			size,
			defaults,
			null,
			encryptedData)

	fun addEncryptedData(data:ContactEncryptedData) {
		encryptedData = ArrayList(encryptedData)
		encryptedData!!.add(data)
	}

	fun getPublicKeys(crypto:UserCrypto,email:String):List<String> {
		val cards=encryptedData
		val signedData=getSignedData(crypto,cards!!)
		val clearData=getClearData(cards)
		if(signedData==null) {
			return emptyList()
		}

		val signed=Ezvcard.parse(signedData).first()
		val clear=if(clearData==null) VCard() else Ezvcard.parse(clearData).first()
		val group:String
		try {
			group=VCardUtil.getGroup(clear,signed,email)
		} catch(e:Exception) {
			return emptyList()
		}

		val keyProps=signed.keys
		val publicKeys=ArrayList<String>()
		for(key in keyProps) {
			if(!key.group.equals(group,ignoreCase=true)) {
				continue
			}
			try {
				publicKeys.add(Armor.armorKey(key.data))
			} catch(e:Exception) {
				return emptyList()
			}
		}
		return publicKeys
	}

	private fun getSignedData(crypto:UserCrypto,cards:List<ContactEncryptedData>):String? {
		for(card in cards) {
			if(card.encryptionType==ContactEncryption.SIGNED) {
				val tdr:TextDecryptionResult
				try {
					tdr=crypto.verify(card.data,card.signature)
				} catch(e:Exception) {
					return null
				}

				return if(tdr.isSignatureValid) {
					tdr.decryptedData
				} else null
			}
		}
		return null
	}

	private fun getClearData(cards:List<ContactEncryptedData>):String? {
		for(card in cards) {
			if(card.encryptionType==ContactEncryption.CLEARTEXT) {
				return card.data
			}
		}
		return null
	}
}
