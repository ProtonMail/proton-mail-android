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
package ch.protonmail.android.data.local

import android.provider.BaseColumns
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_DATA_ID
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_DATA_NAME
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_EMAILS_CONTACT_ID
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_EMAILS_EMAIL
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_EMAILS_ID
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_EMAILS_LABEL_IDS
import ch.protonmail.android.data.local.model.COLUMN_CONTACT_ID
import ch.protonmail.android.data.local.model.COLUMN_LABEL_ID
import ch.protonmail.android.data.local.model.COLUMN_LABEL_NAME
import ch.protonmail.android.data.local.model.COLUMN_LABEL_ORDER
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.data.local.model.LabelEntity
import ch.protonmail.android.data.local.model.TABLE_CONTACT_DATA
import ch.protonmail.android.data.local.model.TABLE_CONTACT_EMAILS
import ch.protonmail.android.data.local.model.TABLE_CONTACT_EMAILS_LABELS_JOIN
import ch.protonmail.android.data.local.model.TABLE_FULL_CONTACT_DETAILS
import ch.protonmail.android.data.local.model.TABLE_LABELS
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    //region Contact data
    @Query("SELECT * FROM $TABLE_CONTACT_DATA WHERE $COLUMN_CONTACT_DATA_ID = :contactId")
    fun findContactDataById(contactId: String): ContactData?

    @Query("SELECT * FROM $TABLE_CONTACT_DATA WHERE ${BaseColumns._ID} = :contactDbId")
    fun findContactDataByDbId(contactDbId: Long): ContactData?

    @Query("SELECT * FROM $TABLE_CONTACT_DATA ORDER BY $COLUMN_CONTACT_DATA_NAME COLLATE NOCASE ASC")
    fun findAllContactData(): Flow<List<ContactData>>

    @Query("SELECT * FROM $TABLE_CONTACT_DATA ORDER BY $COLUMN_CONTACT_DATA_NAME COLLATE NOCASE ASC")
    fun findAllContactDataAsync(): LiveData<List<ContactData>>

    @Query("DELETE FROM $TABLE_CONTACT_DATA")
    fun clearContactDataCache()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveContactData(contactData: ContactData): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAllContactsData(vararg contactData: ContactData): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAllContactsData(contactData: Collection<ContactData>): List<Long>

    @Delete
    fun deleteContactData(vararg contactData: ContactData)

    @Delete
    fun deleteContactsData(contactData: Collection<ContactData>)

    //endregion

    //region Contact email
    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_ID = :id")
    fun findContactEmailById(id: String): ContactEmail?

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_EMAIL = :email")
    suspend fun findContactEmailByEmail(email: String): ContactEmail?

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_EMAIL = :email")
    fun findContactEmailByEmailBlocking(email: String): ContactEmail?

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_EMAIL = :email")
    fun findContactEmailByEmailLiveData(email: String): LiveData<ContactEmail>

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_CONTACT_ID = :contactId")
    fun findContactEmailsByContactIdBlocking(contactId: String): List<ContactEmail>

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_CONTACT_ID = :contactId")
    suspend fun findContactEmailsByContactId(contactId: String): List<ContactEmail>

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_CONTACT_ID = :contactId")
    fun findContactEmailsByContactIdObservable(contactId: String): Flowable<List<ContactEmail>>

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_CONTACT_ID = :contactId")
    fun observeContactEmailsByContactId(contactId: String): Flow<List<ContactEmail>>

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS ORDER BY $COLUMN_CONTACT_EMAILS_EMAIL")
    fun findAllContactsEmails(): Flow<List<ContactEmail>>

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_EMAIL IN (:emails)")
    fun findContactsByEmail(emails: List<String>): Flow<List<ContactEmail>>

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS ORDER BY $COLUMN_CONTACT_EMAILS_EMAIL")
    fun findAllContactsEmailsAsync(): LiveData<List<ContactEmail>>

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS ORDER BY $COLUMN_CONTACT_EMAILS_EMAIL")
    fun findAllContactsEmailsAsyncObservable(): Flowable<List<ContactEmail>>

    @Query(
        """
        SELECT *
        FROM $TABLE_CONTACT_EMAILS 
        WHERE $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_EMAIL LIKE :filter 
        ORDER BY $COLUMN_CONTACT_EMAILS_EMAIL"""
    )
    fun findAllContactsEmailsAsyncObservable(filter: String): Flowable<List<ContactEmail>>

    @Query(
        """
        SELECT $TABLE_CONTACT_EMAILS.* 
        FROM $TABLE_CONTACT_EMAILS 
        INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN
          ON $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_ID = 
            $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID 
        WHERE $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
    """
    )
    fun findAllContactsEmailsByContactGroupAsync(contactGroupId: String): LiveData<List<ContactEmail>>

    @Query(
        """
        SELECT $TABLE_CONTACT_EMAILS.* 
        FROM $TABLE_CONTACT_EMAILS 
        INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN 
          ON $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_ID =
            $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID 
        WHERE $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
    """
    )
    fun findAllContactsEmailsByContactGroup(contactGroupId: String): Flow<List<ContactEmail>>

    @Query(
        """
        SELECT $TABLE_CONTACT_EMAILS.* 
        FROM $TABLE_CONTACT_EMAILS 
        INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN 
          ON $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_ID =
            $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID 
        WHERE $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
    """
    )
    fun findAllContactsEmailsByContactGroupBlocking(contactGroupId: String): List<ContactEmail>

    @Query(
        """
        SELECT $TABLE_CONTACT_EMAILS.* 
        FROM $TABLE_CONTACT_EMAILS 
        INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN 
          ON $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_ID =
            $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID 
        WHERE $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
    """
    )
    fun findAllContactsEmailsByContactGroupAsyncObservable(
        contactGroupId: String
    ): Flowable<List<ContactEmail>>

    @Query(
        """
        SELECT $TABLE_LABELS.*
        FROM $TABLE_LABELS 
        INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN 
          ON $TABLE_LABELS.$COLUMN_LABEL_ID =
            $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID 
        WHERE $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID = :emailId
        ORDER BY $COLUMN_LABEL_NAME
    """
    )
    fun findAllContactGroupsByContactEmailAsync(emailId: String): LiveData<List<LabelEntity>>

    @Query(
        """
        SELECT $TABLE_LABELS.* 
        FROM $TABLE_LABELS 
        INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN
          ON $TABLE_LABELS.$COLUMN_LABEL_ID =
            $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID 
        WHERE $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID = :emailId
        ORDER BY $COLUMN_LABEL_NAME
    """
    )
    fun findAllContactGroupsByContactEmailAsyncObservable(emailId: String): Flowable<List<LabelEntity>>

    @Query(
        """
        SELECT $TABLE_LABELS.* 
        FROM $TABLE_LABELS 
        INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN
          ON $TABLE_LABELS.$COLUMN_LABEL_ID =
            $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID 
        WHERE $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID = :emailId
        ORDER BY $COLUMN_LABEL_NAME
    """
    )
    suspend fun getAllContactGroupsByContactEmail(emailId: String): List<LabelEntity>

    /**
     * Make sure you provide @param filter with included % or ?
     */
    @Query(
        """
        SELECT $TABLE_CONTACT_EMAILS.* 
        FROM $TABLE_CONTACT_EMAILS
        INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN
          ON $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_ID =
            $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID
        WHERE $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
          AND $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_EMAIL LIKE :filter
      """
    )
    fun filterContactsEmailsByContactGroupAsyncObservable(
        contactGroupId: String,
        filter: String
    ): Flowable<List<ContactEmail>>

    /**
     * Make sure you provide @param filter with included % or ?
     */
    @Query(
        """
        SELECT $TABLE_CONTACT_EMAILS.* 
        FROM $TABLE_CONTACT_EMAILS
        INNER JOIN $TABLE_CONTACT_EMAILS_LABELS_JOIN
          ON $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_ID =
            $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID
        WHERE $TABLE_CONTACT_EMAILS_LABELS_JOIN.$COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
          AND $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_EMAIL LIKE :filter
      """
    )
    fun filterContactsEmailsByContactGroup(contactGroupId: String, filter: String): Flow<List<ContactEmail>>

    @Query(
        """
        SELECT 
          $TABLE_CONTACT_DATA.$COLUMN_CONTACT_DATA_NAME,
          $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_EMAIL
        FROM $TABLE_CONTACT_DATA
        JOIN $TABLE_CONTACT_EMAILS 
          ON $TABLE_CONTACT_DATA.$COLUMN_CONTACT_DATA_ID =
            $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_CONTACT_ID
    """
    )
    fun findAllMessageRecipientsLiveData(): LiveData<List<MessageRecipient>>

    @Query(
        """
        SELECT 
          $TABLE_CONTACT_DATA.$COLUMN_CONTACT_DATA_NAME,
          $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_EMAIL
        FROM $TABLE_CONTACT_DATA
        JOIN $TABLE_CONTACT_EMAILS
        ON $TABLE_CONTACT_DATA.$COLUMN_CONTACT_DATA_ID = $TABLE_CONTACT_EMAILS.$COLUMN_CONTACT_EMAILS_CONTACT_ID
    """
    )
    fun findAllMessageRecipients(): Flowable<List<MessageRecipient>>

    @Query("DELETE FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_EMAIL = :email")
    suspend fun clearByEmail(email: String)

    @Query("DELETE FROM $TABLE_CONTACT_EMAILS WHERE $COLUMN_CONTACT_EMAILS_EMAIL = :email")
    fun clearByEmailBlocking(email: String)

    @Query("DELETE FROM $TABLE_CONTACT_EMAILS")
    fun clearContactEmailsCache()

    @Delete
    fun deleteContactEmail(vararg contactEmail: ContactEmail)

    @Delete
    fun deleteAllContactsEmails(contactEmail: Collection<ContactEmail>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveContactEmail(contactEmail: ContactEmail): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAllContactsEmails(vararg emailData: ContactEmail): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAllContactsEmails(emailData: Collection<ContactEmail>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAllContactsEmailsBlocking(emailData: Collection<ContactEmail>): List<Long>

    @Query(
        """
        SELECT count(*)
        FROM $TABLE_CONTACT_EMAILS
        WHERE $COLUMN_CONTACT_EMAILS_LABEL_IDS LIKE :contactGroupId
    """
    )
    fun countContactEmails(contactGroupId: String): Int
    //endregion

    //region contacts labels aka contacts groups
    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID = :labelId")
    fun findContactGroupById(labelId: String): Flow<LabelEntity?>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID = :labelId")
    fun findContactGroupByIdBlocking(labelId: String): LabelEntity?

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID = :labelId")
    fun findContactGroupByIdAsync(labelId: String): Single<LabelEntity>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_NAME = :labelName")
    fun findContactGroupByNameAsync(labelName: String): Single<LabelEntity>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_NAME = :groupName")
    fun findContactGroupByName(groupName: String): LabelEntity?

    @Query("SELECT * FROM $TABLE_LABELS ORDER BY $COLUMN_LABEL_NAME")
    fun findContactGroupsLiveData(): LiveData<List<LabelEntity>>

    @Query("SELECT * FROM $TABLE_LABELS ORDER BY $COLUMN_LABEL_NAME")
    fun findContactGroupsObservable(): Flowable<List<LabelEntity>>

    @Query("SELECT * FROM $TABLE_LABELS ORDER BY $COLUMN_LABEL_NAME")
    fun observeContactLabels(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM $TABLE_LABELS ORDER BY $COLUMN_LABEL_NAME")
    fun findContactGroups(): Flow<List<LabelEntity>>

    @Query(
        """
        SELECT *
        FROM $TABLE_LABELS 
        WHERE $COLUMN_LABEL_NAME LIKE :filter
        ORDER BY $COLUMN_LABEL_NAME
    """
    )
    fun findContactGroups(filter: String): Flow<List<LabelEntity>>

    @Query("DELETE FROM $TABLE_LABELS")
    suspend fun clearContactGroupsLabelsTable()

    @Query("DELETE FROM $TABLE_LABELS")
    fun clearContactGroupsLabelsTableBlocking()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveContactGroupLabel(contactLabel: LabelEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateFullContactGroup(contactLabel: LabelEntity)

    @Query("UPDATE $TABLE_LABELS SET $COLUMN_LABEL_NAME = :name")
    fun updateName(name: String)

    @Query("UPDATE $TABLE_LABELS SET $COLUMN_LABEL_ORDER = :order")
    fun updateOrder(order: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAllContactGroups(vararg contactLabels: LabelEntity): List<Long>

    @Query("DELETE FROM $TABLE_LABELS")
    fun clearContactGroupsList()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveContactGroupsList(contactLabels: List<LabelEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveContactGroupsListBlocking(contactLabels: List<LabelEntity>): List<Long>

    @Query("DELETE FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID = :labelId")
    fun deleteByContactGroupLabelId(labelId: String)

    @Delete
    fun deleteContactGroup(contactLabel: LabelEntity)

    @Query(
        """
        SELECT *
        FROM $TABLE_LABELS
        WHERE $COLUMN_LABEL_ID IN (:labelIds)
        ORDER BY $COLUMN_LABEL_NAME
    """
    )
    fun getAllContactGroupsByIds(labelIds: List<String>): LiveData<List<LabelEntity>>

    fun updatePartially(contactLabel: LabelEntity) {
        updateName(contactLabel.name)
        updateOrder(contactLabel.order)
    }
    //endregion

    //region Full contact details
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFullContactDetailsBlocking(fullContactDetails: FullContactDetails)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFullContactDetails(fullContactDetails: FullContactDetails)

    @Query("SELECT * FROM $TABLE_FULL_CONTACT_DETAILS WHERE $COLUMN_CONTACT_ID = :id")
    fun findFullContactDetailsByIdBlocking(id: String): FullContactDetails?

    @Query("SELECT * FROM $TABLE_FULL_CONTACT_DETAILS WHERE $COLUMN_CONTACT_ID = :id")
    fun observeFullContactDetailsById(id: String): Flow<FullContactDetails?>

    @Query("DELETE FROM $TABLE_FULL_CONTACT_DETAILS")
    fun clearFullContactDetailsCache()

    @Delete
    fun deleteFullContactsDetails(fullContactDetails: FullContactDetails)

    //endregion

    //region contact emails contact label join
    @Query(
        """
        SELECT count(*) 
        FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN
        WHERE $COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
    """
    )
    suspend fun countContactEmailsByLabelId(contactGroupId: String): Int

    @Query(
        """
        SELECT count(*) 
        FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN
        WHERE $COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
    """
    )
    fun countContactEmailsByLabelIdBlocking(contactGroupId: String): Int

    @Query("DELETE FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN")
    fun clearContactEmailsLabelsJoin()

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN")
    fun observeJoins(): Flow<List<ContactEmailContactLabelJoin>>

    @Query("SELECT * FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN")
    fun fetchJoinsObservable(): Flowable<List<ContactEmailContactLabelJoin>>

    @Query(
        """
        SELECT *
        FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN 
        WHERE $COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
    """
    )
    fun observeJoins(contactGroupId: String): Flow<List<ContactEmailContactLabelJoin>>

    @Query(
        """
        SELECT *
        FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN 
        WHERE $COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
    """
    )
    suspend fun fetchJoins(contactGroupId: String): List<ContactEmailContactLabelJoin>

    @Query(
        """
        SELECT *
        FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN 
        WHERE $COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
    """
    )
    fun fetchJoinsBlocking(contactGroupId: String): List<ContactEmailContactLabelJoin>

    @Query(
        """
        SELECT *
        FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN
        WHERE $COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID = :contactEmailId
    """
    )
    fun fetchJoinsByEmail(contactEmailId: String): List<ContactEmailContactLabelJoin>

    @Query(
        """
        DELETE FROM $TABLE_CONTACT_EMAILS_LABELS_JOIN
        WHERE $COLUMN_CONTACT_EMAILS_LABELS_JOIN_EMAIL_ID IN (:contactEmailIds)
          AND $COLUMN_CONTACT_EMAILS_LABELS_JOIN_LABEL_ID = :contactGroupId
    """
    )
    fun deleteJoinByGroupIdAndEmailId(contactEmailIds: List<String>, contactGroupId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveContactEmailContactLabel(
        contactEmailContactLabelJoin: ContactEmailContactLabelJoin
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveContactEmailContactLabelBlocking(
        contactEmailContactLabelJoin: ContactEmailContactLabelJoin
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveContactEmailContactLabel(
        vararg contactEmailContactLabelJoin: ContactEmailContactLabelJoin
    ): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveContactEmailContactLabel(
        contactEmailContactLabelJoin: List<ContactEmailContactLabelJoin>
    ): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveContactEmailContactLabelBlocking(
        contactEmailContactLabelJoin: List<ContactEmailContactLabelJoin>
    ): List<Long>

    @Delete
    fun deleteContactEmailContactLabel(
        contactEmailContactLabelJoin: Collection<ContactEmailContactLabelJoin>
    )

    @Transaction
    suspend fun insertNewContactsAndLabels(
        allContactEmails: List<ContactEmail>,
        contactLabelList: List<LabelEntity>,
        allJoins: List<ContactEmailContactLabelJoin>
    ) {
        clearContactEmailsCache()
        clearContactGroupsList()
        saveContactGroupsList(contactLabelList)
        saveAllContactsEmails(allContactEmails)
        saveContactEmailContactLabel(allJoins)
    }
    //endregion
}
