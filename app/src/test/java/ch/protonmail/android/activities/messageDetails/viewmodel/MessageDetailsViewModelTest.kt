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
//package ch.protonmail.android.activities.messageDetails.viewmodel
//
//import androidx.arch.core.executor.testing.InstantTaskExecutorRule
//import androidx.lifecycle.MutableLiveData
//import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
//import ch.protonmail.android.api.models.room.messages.Message
//import ch.protonmail.android.api.models.room.messages.MessagesDatabase
//import ch.protonmail.android.sharedTest.kotlin.CoroutinesTestRule
//import com.nhaarman.mockito_kotlin.any
//import com.nhaarman.mockito_kotlin.doReturn
//import com.nhaarman.mockito_kotlin.mock
//import org.junit.After
//import org.junit.Rule
//import org.junit.rules.TestRule
//import org.mockito.Mockito
//
///**
// * Test suite for [MessageDetailsViewModel]
// * @author Davide Farella
// */
//internal class MessageDetailsViewModelTest {
//
//    @get:Rule val archRule: TestRule = InstantTaskExecutorRule()
//    @get:Rule val coroutinesRule: TestRule = CoroutinesTestRule()
//
//    /**
//     * See [Memory leak in mockito-inline...](https://github.com/mockito/mockito/issues/1614)
//     * TODO: replicate in other tests
//     */
//    @After
//    fun clearMocks() {
//        Mockito.framework().clearInlineMocks()
//    }
//
//    private val mockMessagesDatabase by lazy { mock<MessagesDatabase> {
//        on { findMessageByIdAsync(any()) } doReturn MutableLiveData<Message>()
//                .apply { value = mock() }
//    } }
//
//    private val realDetailsRepository by lazy { MessageDetailsRepository(
//            mock(), mock(), mockMessagesDatabase, mock(), mock(), mock()
//    ) }
//
//    // TODO comment due to different ViewModel initialization
//    //private fun viewModel(
//    //        detailsRepo: MessageDetailsRepository = realDetailsRepository,
//    //        userManager: UserManager = mock(),
//    //        contactsRepository: ContactsRepository = mock(),
//    //        metadataDatabase: AttachmentMetadataDatabase = mock(),
//    //        messageId: String = "",
//    //        transient: Boolean = false
//    //) = MessageDetailsViewModel(
//    //        detailsRepo, userManager, contactsRepository, metadataDatabase, messageId, transient
//    //)
//
//    // TODO: Impossible to test due to static methods and too many properties initialized conditionally
//    //@Test
//    //fun messageAttachments() = runBlocking {
//    //    val observer = mock<(List<Attachment>) -> Unit>()
//    //    val lifecycle = TestLifecycle()
//    //
//    //    // Init MessageDetailsViewModel
//    //    val viewModel = viewModel()
//    //
//    //    // Start observing
//    //    viewModel.messageAttachments.observe(lifecycle, Observer(observer))
//    //    verify(observer, never()).invoke(any())
//    //
//    //    lifecycle.resume()
//    //    verify(observer, times(1)).invoke(any())
//    //}
//
//}
