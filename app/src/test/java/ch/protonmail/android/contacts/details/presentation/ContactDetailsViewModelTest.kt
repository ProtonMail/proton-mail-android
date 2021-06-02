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

package ch.protonmail.android.contacts.details.presentation

import android.content.Context
import android.net.Uri
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import app.cash.turbine.test
import ch.protonmail.android.contacts.details.domain.FetchContactDetails
import ch.protonmail.android.contacts.details.domain.FetchContactGroups
import ch.protonmail.android.utils.FileHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ContactDetailsViewModelTest : ArchTest, CoroutinesTest {

    private val fetchContactDetails: FetchContactDetails = mockk()
    private val fetchContactGroups: FetchContactGroups = mockk()
    private val mapper = ContactDetailsMapper()
    private val workManager: WorkManager = mockk()
    private val fileHelper: FileHelper = mockk()

    val viewModel = ContactDetailsViewModel(
        fetchContactDetails,
        fetchContactGroups,
        mapper,
        workManager,
        fileHelper
    )

    @Test
    fun getContactDetails() {
    }

    @Test
    fun verifyDeleteContactIsCallingWorker() {
        // given
        val contactId = "contactId1"
        every { workManager.enqueue(any<OneTimeWorkRequest>()) } returns mockk()

        // when
        val result = viewModel.deleteContact(contactId)

        // then
        assertNotNull(result)
    }

    @Test
    fun saveVcard() = runBlockingTest {
        // given
        val vCardToShare = "vcardContent123"
        val contactName = "contactName1"
        val context = mockk<Context>()
        val testUri = mockk<Uri>()
        coEvery { fileHelper.saveStringToFileProvider("$contactName.vcf", vCardToShare, context) } returns testUri

        // when
        viewModel.vCardSharedFlow.test {
            viewModel.saveVcard(vCardToShare, contactName, context)

            // then
            assertEquals(testUri, expectItem())
        }
    }
}
