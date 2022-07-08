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
package ch.protonmail.android.jobs

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.events.NoResultsEvent
import ch.protonmail.android.events.SearchResultEvent
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class SearchMessagesJob(private val queryString: String, private val page: Int) : ProtonMailBaseJob(
    Params(Priority.MEDIUM)
) {

    override fun onRun() {
        if (queryString.trim { it <= ' ' }.isEmpty()) return

        val results = if (!getQueueNetworkUtil().isConnected()) {
            doLocalSearch()
        } else {
            doRemoteSearch()
        }
        if (results.isEmpty()) {
            AppUtil.postEventOnUi(NoResultsEvent(page))
            return
        }


        AppUtil.postEventOnUi(SearchResultEvent(results))
    }

    private fun doLocalSearch(): List<Message> {
        return getMessageDetailsRepository().searchMessages(
            queryString, queryString, queryString
        )
    }

    private fun doRemoteSearch(): List<Message> {
        return try {
            val userId = checkNotNull(userId ?: getUserManager().currentUserId) {
                "No User Id provided and no current User Id from UserManager"
            }
            val response = runBlocking {
                getApi().getMessages(GetAllMessagesParameters(userId = userId, page = page, keyword = queryString))
            }
            response.messages
        } catch (error: Exception) {
            Timber.e(error, "Error searching messages")
            emptyList()
        }
    }
}
