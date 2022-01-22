/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.data.local.model

import android.util.Base64
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.enumerations.ContactEncryption
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.utils.VCardUtil
import com.proton.gopenpgp.armor.Armor
import ezvcard.Ezvcard
import ezvcard.VCard
import me.proton.core.util.kotlin.equalsNoCase
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

const val TABLE_FULL_CONTACT_DETAILS = "fullContactsDetails"
const val COLUMN_CONTACT_ID = "ID"
const val COLUMN_CONTACT_NAME = "Name"
const val COLUMN_CONTACT_UID = "Uid"
const val COLUMN_CONTACT_CREATE_TIME = "CreateTime"
const val COLUMN_CONTACT_MODIFY_TIME = "ModifyTIme"
const val COLUMN_CONTACT_SIZE = "Size"
const val COLUMN_CONTACT_DEFAULTS = "Defaults"
const val COLUMN_CONTACT_ENCRYPTED_DATA = "EncryptedData"

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
    var encryptedData: List<ContactEncryptedData>? = null
) {

    /**
     * Room database constructor
     */
    constructor (
        contactId: String,
        name: String?,
        uid: String?,
        createTime: Long,
        modifyTime: Long,
        size: Int,
        defaults: Int,
        encryptedData: List<ContactEncryptedData>?
    ) : this(
        contactId,
        name,
        uid,
        createTime,
        modifyTime,
        size,
        defaults,
        null,
        encryptedData
    )

    fun addEncryptedData(data: ContactEncryptedData) {
        encryptedData = (encryptedData ?: emptyList()) + data
    }

    fun getPublicKeys(crypto: UserCrypto, email: String): List<String> {
        val cards = encryptedData
            ?: return emptyList()
        val signedData = getSignedData(crypto, cards)
            ?: return emptyList()

        val clearData = getClearData(cards)
        val signed = Ezvcard.parse(signedData).first()
        val clear = if (clearData == null) VCard() else Ezvcard.parse(clearData).first()
        val group = runCatching { VCardUtil.getGroup(clear, signed, email) }
            .getOrNull() ?: return emptyList()

        return signed.keys
            .filter { it.group equalsNoCase group }
            .mapNotNull {
                runCatching { Armor.armorKey(it.data) }.getOrNull()
            }
    }

    private fun getSignedData(crypto: UserCrypto, cards: List<ContactEncryptedData>): String? =
        cards
            .find { it.encryptionType == ContactEncryption.SIGNED }
            ?.let {
                runCatching { crypto.verify(it.data, it.signature) }
                    .fold(
                        onSuccess = { if (it.isSignatureValid) it.data else null },
                        onFailure = { null }
                    )
            }

    private fun getClearData(cards: List<ContactEncryptedData>): String? =
        cards.find { it.encryptionType == ContactEncryption.CLEARTEXT }?.data
}

class FullContactDetailsConverter {

    @TypeConverter
    fun contactEncryptedDataListToString(contactEncryptedDataList: List<ContactEncryptedData>?): String? {
        val out = ByteArrayOutputStream()
        try {
            val encryptedDataArray = contactEncryptedDataList?.toTypedArray() ?: emptyArray()
            ObjectOutputStream(out).use { stream -> stream.writeObject(encryptedDataArray) }
        } catch (e: IOException) {
            Timber.e(e, "Serialization of encrypted data failed ")
        }

        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
    }

    @TypeConverter
    fun stringToContactEncryptedDataList(contactEncryptedDataString: String?): List<ContactEncryptedData>? {
        return try {
            val inputStream = ByteArrayInputStream(Base64.decode(contactEncryptedDataString, Base64.DEFAULT))
            val resultArray = ObjectInputStream(inputStream).readObject() as Array<ContactEncryptedData>
            resultArray.toList()
        } catch (e: Exception) {
            Timber.i(e, "Deserialization of contact encrypted data failed")
            null
        }
    }
}
