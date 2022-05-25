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
package ch.protonmail.android.api.segments.event

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.exceptions.ApiException
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.core.Constants
import ch.protonmail.android.event.data.remote.model.EventResponse
import ch.protonmail.android.prefs.SecureSharedPreferences
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// region constants
private const val PREF_NEXT_EVENT_ID = "latest_event_id"
const val PREF_LATEST_EVENT = "latest_event"
// endregion

/**
 * EventManager manages the fetching of the proper events and delegates their handling.
 */
@Singleton
class EventManager @Inject constructor(
    private val context: Context,
    protonMailApiManager: ProtonMailApiManager,
    private val eventHandlerFactory: EventHandler.AssistedFactory,
    private val dispatchers: DispatcherProvider
) {

    private var service: EventService = protonMailApiManager.getSecuredServices().event
    private val sharedPrefs = mutableMapOf<UserId, SharedPreferences>()
    private val eventHandlers = mutableMapOf<UserId, EventHandler>()

    fun reconfigure(service: EventService) {
        this.service = service
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private val hasMoreEventContext = newSingleThreadContext("EventHandler.hasMoreEvent")

    /**
     * Handle next event for given [EventHandler]
     * This will be executed on a single thread
     * @return `true` if there are any more events to process
     *
     * @throws ApiException if service call fails
     */
    private suspend fun handleNextEvent(handler: EventHandler): Boolean =
        withContext(hasMoreEventContext) {
            // This must be done sequentially, parallel should not work at the same time
            if (recoverNextEventId(handler.userId) == null) {
                refreshContacts(handler)
                refresh(handler) // refresh other things like messages
                generateNewEventId(handler.userId)
            }

            val eventID = recoverNextEventId(handler.userId)
            val response = service.check(eventID!!, UserIdTag(handler.userId))

            if (response.code == Constants.RESPONSE_CODE_OK) {
                handleEvents(handler, response)
                response.hasMore()
            } else {
                throw ApiException(response, response.error)
            }
        }

    private suspend fun handleAllEvents(handler: EventHandler) {
        while (handleNextEvent(handler)) {
            // noop - used for handle all the events
        }
    }

    suspend fun consumeEventsFor(loggedInUsers: Collection<UserId>) = withContext(dispatchers.Io) {
        for (user in loggedInUsers) {
            eventHandlers.putIfAbsentApi23(user, eventHandlerFactory.create(user))
        }

        for ((_, handler) in eventHandlers) {
            handleAllEvents(handler)
        }
    }

    private fun <K, V> MutableMap<K, V>.putIfAbsentApi23(key: K, value: V): V? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            putIfAbsent(key, value)
        } else {
            var v: V? = get(key)
            if (v == null) {
                v = put(key, value)
            }
            v
        }
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("consumeEventsFor(loggedInUsers)")
    )
    fun consumeEventsForBlocking(loggedInUsers: Collection<UserId>) =
        runBlocking { consumeEventsFor(loggedInUsers) }

    /**
     * Clears the state of the EventManager for all users
     */
    fun clearState() {
        sharedPrefs.clear()
        eventHandlers.clear()
    }

    /**
     * Clears the state of the EventManager for the given [userId]
     */
    fun clearState(userId: UserId) {
        sharedPrefs -= userId
        eventHandlers -= userId
    }

    private fun refreshContacts(handler: EventHandler) {
        Timber.d("EventManager handler refreshContacts")
        synchronized(this) {
            handler.handleRefreshContacts()
        }
    }

    private fun refresh(handler: EventHandler) {
        Timber.d("EventManager handler handleRefresh")
        synchronized(this) {
            lockState(handler.userId)
            handler.handleRefresh(handler.userId)
        }
    }

    /**
     * @throws ApiException if service call fails
     */
    private suspend fun generateNewEventId(userId: UserId) {
        val response = service.latestId(UserIdTag(userId))
        if (response.code == Constants.RESPONSE_CODE_OK) {
            backupNextEventId(userId, response.eventID)
        } else {
            throw ApiException(response, response.error)
        }
    }

    private fun handleEvents(handler: EventHandler, response: EventResponse) {
        if (response.refreshContacts()) {
            refreshContacts(handler)
        }
        if (response.refresh()) {
            refresh(handler)
            return
        }
        Timber.d("EventManager handler stage and write")
        if (handler.stage(response.messageUpdates)) {
            // Write the updates since the staging was completed without any error
            handler.write(response)
            // Update next event id only after writing updates to local cache has finished successfully
            backupNextEventId(handler.userId, response.eventID)
        }
    }

    private fun lockState(userId: UserId) {
        backupNextEventId(userId, "")
    }

    private fun recoverNextEventId(userId: UserId): String? {
        val prefs = sharedPrefs.getOrPut(
            userId,
            {
                SecureSharedPreferences.getPrefsForUser(context, userId)
            }
        )
        Timber.d("EventManager recoverLastEventId")
        val lastEventId = prefs.getString(PREF_NEXT_EVENT_ID, null)
        return if (lastEventId.isNullOrEmpty()) null else lastEventId
    }

    private fun backupNextEventId(userId: UserId, eventId: String) {
        val prefs = sharedPrefs.getOrPut(
            userId,
            {
                SecureSharedPreferences.getPrefsForUser(context, userId)
            }
        )
        Timber.d("EventManager backupLastEventId")
        prefs.edit().putString(PREF_NEXT_EVENT_ID, eventId).apply()
    }
}
