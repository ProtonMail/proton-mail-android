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
package ch.protonmail.android.api.segments.event

import android.content.SharedPreferences
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.exceptions.ApiException
import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.EventResponse
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_NEXT_EVENT_ID = "latest_event_id"
const val PREF_LATEST_EVENT = "latest_event"

/**
 * EventManager manages the fetching of the proper events and delegates their handling.
 */
@Singleton
class EventManager @Inject constructor(
    private val protonMailApplication: ProtonMailApplication,
    protonMailApiManager: ProtonMailApiManager,
    private val eventHandlerFactory: EventHandler.AssistedFactory
) {

    private var service: EventService = protonMailApiManager.getSecuredServices().event
    private var sharedPrefs = mutableMapOf<String, SharedPreferences>()
    private var eventHandlers = mutableMapOf<String, EventHandler>()

    fun reconfigure(service: EventService) {
        this.service = service
    }

    /**
     * @return if there are any more events to process
     */
    @Throws(IOException::class)
    private fun nextEvent(handler: EventHandler): Boolean {
        // This must be done sequentially, parallel should not work at the same time
        synchronized(this) {
            if (recoverNextEventId(handler.username) == null) {
                refreshContacts(handler)
                refresh(handler) // refresh other things like messages
                getNewId(handler.username)
            }

            val eventID = recoverNextEventId(handler.username)
            val response = ParseUtils.parse(service.check(eventID!!, RetrofitTag(handler.username)).execute())

            if (response.code == Constants.RESPONSE_CODE_OK) {
                handleEvents(handler, response)
                return response.hasMore()
            } else {
                throw ApiException(response, response.error)
            }
        }
    }

    @Throws(IOException::class)
    fun start(loggedInUsers: List<String>) {
        loggedInUsers.forEach { username ->
            if (!eventHandlers.containsKey(username)) {
                eventHandlers[username] = eventHandlerFactory.create(username)
            }
        }

        // go through all of the logged in users and check their event separately
        eventHandlers.forEach {
            while (nextEvent(it.value)) {
                // iterate as long as there are more events to be fetched
            }
        }
    }

    /**
     * Clears the state of the EventManager for all users.
     */
    fun clearState() {
        sharedPrefs = mutableMapOf()
        eventHandlers = mutableMapOf()
    }

    /**
     * Clears the state of the EventManager for one user only.
     */
    fun clearState(username: String) {
        sharedPrefs.remove(username)
        eventHandlers.remove(username)
    }

    @Throws(IOException::class)
    private fun refreshContacts(handler: EventHandler) {
        Timber.d("EventManager handler refreshContacts")
        synchronized(this) {
            handler.handleRefreshContacts()
        }
    }

    @Throws(IOException::class)
    private fun refresh(handler: EventHandler) {
        Timber.d("EventManager handler handleRefresh")
        synchronized(this) {
            lockState(handler.username)
            handler.handleRefresh()
        }
    }

    @Throws(IOException::class)
    private fun getNewId(username: String) {
        val response = ParseUtils.parse(service.latestID(RetrofitTag(username)).execute())
        if (response.code == Constants.RESPONSE_CODE_OK) {
            backupNextEventId(username, response.eventID)
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
            handler.handleNewKeysIfNeeded(response)
            // Update next event id only after writing updates to local cache has finished successfully
            backupNextEventId(handler.username, response.eventID)
        }
    }

    private fun lockState(username: String) {
        backupNextEventId(username, "")
    }

    private fun recoverNextEventId(username: String): String? {
        val prefs = sharedPrefs.getOrPut(
            username,
            { protonMailApplication.getSecureSharedPreferences(username) }
        )
        Timber.d("EventManager recoverLastEventId")
        val lastEventId = prefs.getString(PREF_NEXT_EVENT_ID, null)
        return if (lastEventId.isNullOrEmpty()) null else lastEventId
    }

    private fun backupNextEventId(username: String, eventId: String) {
        val prefs = sharedPrefs.getOrPut(
            username,
            { protonMailApplication.getSecureSharedPreferences(username) }
        )
        Timber.d("EventManager backupLastEventId")
        prefs.edit().putString(PREF_NEXT_EVENT_ID, eventId).apply()
    }
}
