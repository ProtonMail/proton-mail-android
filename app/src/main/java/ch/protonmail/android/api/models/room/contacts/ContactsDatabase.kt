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

import android.provider.BaseColumns
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_ID
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_NAME
import ch.protonmail.android.api.models.room.messages.COLUMN_LABEL_ORDER
import io.reactivex.Flowable
import io.reactivex.Single

// TODO remove when we change name of this class to ContactsDao and *Factory to *Database
typealias ContactsDao = ContactsDatabase

@Dao
abstract class
ContactsDatabase {
	//region Contact data
	@Query("SELECT * FROM $TABLE_CONTACT_DATA WHERE ${COLUMN_CONTACT_DATA_ID}=:contactId")
	abstract fun findContactDataById(contactId:String):ContactData?

	@Query("SELECT * FROM $TABLE_CONTACT_DATA WHERE ${BaseColumns._ID}=:contactDbId")
	abstract fun findContactDataByDbId(contactDbId:Long):ContactData?

	@Query("SELECT * FROM $TABLE_CONTACT_DATA ORDER BY $COLUMN_CONTACT_DATA_NAME COLLATE NOCASE ASC")
	abstract fun findAllContactDataAsync():LiveData<List<ContactData>>

	@Query("DELETE FROM $TABLE_CONTACT_DATA")
	abstract fun clearContactDataCache()

	@Insert(onConflict=OnConflictStrategy.REPLACE)
	abstract fun saveContactData(contactData:ContactData):Long

	@Insert(onConflict=OnConflictStrategy.REPLACE)
	abstract fun saveAllContactsData(vararg contactData:ContactData):List<Long>

	@Insert(onConflict=OnConflictStrategy.REPLACE)
	abstract fun saveAllContactsData(contactData:Collection<ContactData>):List<Long>

	@Delete
	abstract fun deleteContactData(vararg contactData:ContactData)

	@Delete
	abstract fun deleteContactsData(contactData:Collection<ContactData>)

	//endregion

	//region Contact email
	@Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE ${COLUMN_CONTACT_EMAILS_ID}=:id")
	abstract fun findContactEmailById(id:String):ContactEmail?

	@Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE ${COLUMN_CONTACT_EMAILS_EMAIL}=:email")
	abstract fun findContactEmailByEmail(email:String):ContactEmail?

	@Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE ${COLUMN_CONTACT_EMAILS_EMAIL}=:email")
	abstract fun findContactEmailByEmailLiveData(email: String): LiveData<ContactEmail>

	@Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE ${COLUMN_CONTACT_EMAILS_CONTACT_ID}=:contactId")
	abstract fun findContactEmailsByContactId(contactId:String): List<ContactEmail>

	@Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE ${COLUMN_CONTACT_EMAILS_CONTACT_ID}=:contactId")
	abstract fun findContactEmailsByContactIdObservable(contactId:String): Flowable<List<ContactEmail>>

	@Query("SELECT * FROM $TABLE_CONTACT_EMAILS ORDER BY $COLUMN_CONTACT_EMAILS_EMAIL")
	abstract fun findAllContactsEmailsAsync():LiveData< List<ContactEmail>>

	@Query("SELECT * FROM $TABLE_CONTACT_EMAILS ORDER BY $COLUMN_CONTACT_EMAILS_EMAIL")
	abstract fun findAllContactsEmailsAsyncObservable(): Flowable<List<ContactEmail>>

	@Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE ${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_EMAIL} LIKE :filter ORDER BY $COLUMN_CONTACT_EMAILS_EMAIL")
	abstract fun findAllContactsEmailsAsyncObservable(filter: String): Flowable<List<ContactEmail>>

	@Query("SELECT ${TABLE_CONTACT_EMAILS}.* FROM $TABLE_CONTACT_EMAILS INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN ON ${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_ID} = ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID} WHERE ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID} = :contactGroupId")
	abstract fun findAllContactsEmailsByContactGroupAsync(contactGroupId: String):LiveData<List<ContactEmail>>

	@Query("SELECT ${TABLE_CONTACT_EMAILS}.* FROM $TABLE_CONTACT_EMAILS INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN ON ${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_ID} = ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID} WHERE ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID} = :contactGroupId")
	abstract fun findAllContactsEmailsByContactGroup(contactGroupId: String): List<ContactEmail>

	@Query("SELECT ${TABLE_CONTACT_EMAILS}.* FROM $TABLE_CONTACT_EMAILS INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN ON ${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_ID} = ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID} WHERE ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID} = :contactGroupId")
	abstract fun findAllContactsEmailsByContactGroupAsyncObservable(contactGroupId: String): Flowable<List<ContactEmail>>

	@Query("SELECT ${TABLE_CONTACT_LABEL}.* FROM $TABLE_CONTACT_LABEL INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN ON ${TABLE_CONTACT_LABEL}.${COLUMN_LABEL_ID} = ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID} WHERE ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID} = :emailId ORDER BY $COLUMN_LABEL_NAME")
	abstract fun findAllContactGroupsByContactEmailAsync(emailId: String): LiveData<List<ContactLabel>>

	@Query("SELECT ${TABLE_CONTACT_LABEL}.* FROM $TABLE_CONTACT_LABEL INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN ON ${TABLE_CONTACT_LABEL}.${COLUMN_LABEL_ID} = ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID} WHERE ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID} = :emailId ORDER BY $COLUMN_LABEL_NAME")
	abstract fun findAllContactGroupsByContactEmailAsyncObservable(emailId: String): Flowable<List<ContactLabel>>

	/**
	 * Make sure you provide @param filter with included % or ?
	 */
	@Query("SELECT ${TABLE_CONTACT_EMAILS}.* FROM $TABLE_CONTACT_EMAILS INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN ON ${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_ID} = ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID} WHERE ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID} = :contactGroupId AND ${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_EMAIL} LIKE :filter")
	abstract fun filterContactsEmailsByContactGroupAsyncObservable(contactGroupId: String, filter: String): Flowable<List<ContactEmail>>

	/**
	 * Make sure you provide @param filter with included % or ?
	 */
	@Query("SELECT ${TABLE_CONTACT_EMAILS}.* FROM $TABLE_CONTACT_EMAILS INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN ON ${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_ID} = ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID} WHERE ${TABLE_CONTACT_EMAILS_LABELS_JOIN}.${COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID} = :contactGroupId AND ${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_EMAIL} LIKE :filter")
	abstract fun filterContactsEmailsByContactGroup(contactGroupId: String, filter: String): List<ContactEmail>

	@Query("SELECT ${TABLE_CONTACT_DATA}.${COLUMN_CONTACT_DATA_NAME},${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_EMAIL} FROM $TABLE_CONTACT_DATA JOIN $TABLE_CONTACT_EMAILS ON ${TABLE_CONTACT_DATA}.${COLUMN_CONTACT_DATA_ID}=${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_CONTACT_ID}")
	abstract fun findAllMessageRecipientsLiveData():LiveData<List<MessageRecipient>>

	@Query("SELECT ${TABLE_CONTACT_DATA}.${COLUMN_CONTACT_DATA_NAME},${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_EMAIL} FROM $TABLE_CONTACT_DATA JOIN $TABLE_CONTACT_EMAILS ON ${TABLE_CONTACT_DATA}.${COLUMN_CONTACT_DATA_ID}=${TABLE_CONTACT_EMAILS}.${COLUMN_CONTACT_EMAILS_CONTACT_ID}")
	abstract fun findAllMessageRecipients(): Flowable<List<MessageRecipient>>

	@Query("DELETE FROM $TABLE_CONTACT_EMAILS WHERE ${COLUMN_CONTACT_EMAILS_EMAIL}=:email")
	abstract fun clearByEmail(email:String)

	@Query("DELETE FROM $TABLE_CONTACT_EMAILS")
	abstract fun clearContactEmailsCache()

	@Delete
	abstract fun deleteContactEmail(vararg contactEmail:ContactEmail)

	@Delete
	abstract fun deleteAllContactsEmails(contactEmail:Collection<ContactEmail>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun saveContactEmail(contactEmail: ContactEmail): Long

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun saveAllContactsEmails(vararg emailData: ContactEmail): List<Long>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun saveAllContactsEmails(emailData: Collection<ContactEmail>): List<Long>

    @Query("SELECT count(*) FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_LABEL_IDS LIKE :contactGroupId")
    abstract fun countContactEmails(contactGroupId: String): Int
	//endregion

	//region contacts labels aka contacts groups
	@Query("SELECT * FROM $TABLE_CONTACT_LABEL WHERE $COLUMN_LABEL_ID = :labelId")
	abstract fun findContactGroupById(labelId: String): ContactLabel?

	@Query("SELECT * FROM $TABLE_CONTACT_LABEL WHERE $COLUMN_LABEL_ID = :labelId")
	abstract fun findContactGroupByIdAsync(labelId: String): Single<ContactLabel>

	@Query("SELECT * FROM $TABLE_CONTACT_LABEL WHERE $COLUMN_LABEL_NAME = :labelName")
	abstract fun findContactGroupByNameAsync(labelName: String): Single<ContactLabel>

	@Query("SELECT * FROM $TABLE_CONTACT_LABEL WHERE $COLUMN_LABEL_NAME = :groupName")
	abstract fun findContactGroupByName(groupName: String): ContactLabel?

	@Query("SELECT * FROM $TABLE_CONTACT_LABEL ORDER BY $COLUMN_LABEL_NAME")
	abstract fun findContactGroupsLiveData(): LiveData<List<ContactLabel>>

	@Query("SELECT * FROM $TABLE_CONTACT_LABEL ORDER BY $COLUMN_LABEL_NAME")
	abstract fun findContactGroupsObservable(): Flowable<List<ContactLabel>>

	@Query("SELECT * FROM $TABLE_CONTACT_LABEL WHERE $COLUMN_LABEL_NAME LIKE :filter ORDER BY $COLUMN_LABEL_NAME")
	abstract fun findContactGroupsObservable(filter: String): Flowable<List<ContactLabel>>

	@Query("DELETE FROM $TABLE_CONTACT_LABEL")
	abstract fun clearContactGroupsLabelsTable()

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun saveContactGroupLabel(contactLabel: ContactLabel): Long

	@Update(onConflict = OnConflictStrategy.REPLACE)
	abstract fun updateFullContactGroup(contactLabel: ContactLabel)

	@Query("UPDATE $TABLE_CONTACT_LABEL SET $COLUMN_LABEL_NAME = :name")
	protected abstract fun updateName(name: String)

	@Query("UPDATE $TABLE_CONTACT_LABEL SET $COLUMN_LABEL_ORDER = :order")
	protected abstract fun updateOrder(order: Int)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun saveAllContactGroups(vararg contactLabels: ContactLabel): List<Long>


	@Query("DELETE FROM $TABLE_CONTACT_LABEL")
	abstract fun clearContactGroupsList()

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun saveContactGroupsList(contactLabels: List<ContactLabel>): List<Long>

	@Query("DELETE FROM $TABLE_CONTACT_LABEL WHERE ${COLUMN_LABEL_ID}=:labelId")
	abstract fun deleteByContactGroupLabelId(labelId: String)

	@Delete
	abstract fun deleteContactGroup(contactLabel: ContactLabel)

	@Query("SELECT * FROM $TABLE_CONTACT_LABEL WHERE $COLUMN_LABEL_ID IN (:labelIds) ORDER BY $COLUMN_LABEL_NAME")
	abstract fun getAllContactGroupsByIds(labelIds: List<String>): LiveData<List<ContactLabel>>

	fun updatePartially(contactLabel: ContactLabel) {
		updateName(contactLabel.name)
		updateOrder(contactLabel.order)
	}
	//endregion

	//region Full contact details
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertFullContactDetails(fullContactDetails:FullContactDetails)

	@Query("SELECT * FROM $TABLE_FULL_CONTACT_DETAILS WHERE ${COLUMN_CONTACT_ID}=:id")
	abstract fun findFullContactDetailsById(id:String):FullContactDetails?

	@Query("DELETE FROM $TABLE_FULL_CONTACT_DETAILS")
	abstract fun clearFullContactDetailsCache()

	@Delete
	abstract fun deleteFullContactsDetails(fullContactDetails:FullContactDetails)

	//endregion

	//region contact emails contact label join
	@Query("SELECT count(*) FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN WHERE ${COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID}=:contactGroupId")
	abstract fun countContactEmailsByLabelId(contactGroupId: String): Int

	@Query("DELETE FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN")
	abstract fun clearContactEmailsLabelsJoin()

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN")
    abstract fun fetchJoinsObservable(): Flowable<List<ContactEmailContactLabelJoin>>

	@Query("SELECT * FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN WHERE ${COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID}=:contactGroupId")
    abstract fun fetchJoins(contactGroupId: String): List<ContactEmailContactLabelJoin>

	@Query("SELECT * FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN WHERE ${COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID}=:contactEmailId")
    abstract fun fetchJoinsByEmail(contactEmailId: String): List<ContactEmailContactLabelJoin>

	@Query("DELETE FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN WHERE $COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID IN (:contactEmailIds) AND ${COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID}=:contactGroupId")
	abstract fun deleteJoinByGroupIdAndEmailId(contactEmailIds: List<String>, contactGroupId: String)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun saveContactEmailContactLabel(contactEmailContactLabelJoin: ContactEmailContactLabelJoin): Long

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun saveContactEmailContactLabel(vararg contactEmailContactLabelJoin: ContactEmailContactLabelJoin): List<Long>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun saveContactEmailContactLabel(contactEmailContactLabelJoin: List<ContactEmailContactLabelJoin>): List<Long>

	@Delete
	abstract fun deleteContactEmailContactLabel(contactEmailContactLabelJoin: Collection<ContactEmailContactLabelJoin>)
	//endregion
}
