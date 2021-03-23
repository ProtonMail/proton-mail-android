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
package ch.protonmail.android.activities.mailbox

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.data.local.PendingActionDatabase
import ch.protonmail.android.data.local.model.ContactLabel
import com.birbit.android.jobqueue.JobManager
import io.mockk.every
import io.mockk.mockk

/**
 * Created by kadrikj on 8/19/18. */
class MailboxViewModelTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val labelsDatabaseFactory = Room.inMemoryDatabaseBuilder(context, ContactDatabase::class.java).build()
    private val messagesDatabaseFactory = Room.inMemoryDatabaseBuilder(context, MessageDatabase::class.java).build()
    private val pendingActionsDatabaseFactory = Room.inMemoryDatabaseBuilder(context, PendingActionDatabase::class.java).build()
    private val labelsDatabase = labelsDatabaseFactory.getDao()
    private val messagesDatabase = messagesDatabaseFactory.getDao()
    private val pendingActionsDatabase = pendingActionsDatabaseFactory.getDao()

    private val label1 = ContactLabel("a", "aa", "aaa")
    private val label2 = ContactLabel("b", "ab", "aab", 0, 1)
    private val label3 = ContactLabel("c", "ac", "aac")

    private val mockJobManager = mockk<JobManager>(relaxed = true)
    private val mockUser = mockk<User>(relaxed = true) {
        every { isPaidUser } returns false
    }
    private val mockUserManager = mockk<UserManager>(relaxed = true) {
        every { user } returns mockUser
    }
    //private val mockResources = mockk<Resources>(relaxed = true) {
    //    val accountTypeNames = arrayOf("Free", "Plus", "Visionary", "Professional")
    //    val maxLabelsPerPlan = arrayOf(3, 200, 10000, 10000)
    //    every { getTextArray(R.array.account_type_names) } returns accountTypeNames
    //    every { getIntArray(R.array.max_labels_per_plan) } returns maxLabelsPerPlan.toIntArray()
    //}

//    @Test
    fun testProcessLabels() {
        labelsDatabase.saveAllContactGroups(label1, label2, label3)

        // TODO mock MessageDetailsRepository instead of MessagesDatabase if we want to run this test
    //  val mailboxViewModel = MailboxViewModel(messagesDatabase, pendingActionsDatabase,mockUserManager, mockJobManager)
        val messagesIds = listOf("m1", "m2")
        val checkedLabelIds = listOf("a")
        val unchangedLabels = emptyList<String>()
        //mailboxViewModel.processLabels(messagesIds, checkedLabelIds, unchangedLabels)
    }
}
