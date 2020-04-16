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
import android.text.TextUtils
import android.util.Log
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.api.exceptions.ApiException
import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.EventResponse
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.Logger
import ch.protonmail.android.utils.extensions.ifNull
import ch.protonmail.android.utils.extensions.ifNullElse
import com.birbit.android.jobqueue.JobManager
import com.google.gson.Gson
import java.io.IOException
import javax.inject.Inject

// region constants
private const val PREF_LATEST_EVENT_ID = "latest_event_id"
private const val PREF_LATEST_EVENT = "latest_event"
// endregion

class EventManager {

    @Inject
    lateinit var mApi : ProtonMailApi
    @Inject
    lateinit var mUserManager : UserManager
    @Inject
    lateinit var mJobManager : JobManager
    @Inject
    lateinit var databaseProvider: DatabaseProvider

    private val service : EventService
    private var sharedPrefs = mutableMapOf<String, SharedPreferences>()
    private var lastEventIds = mutableMapOf<String, String?>()
    private var eventHandlers = mutableMapOf<String, EventHandler>()

    init {
        ProtonMailApplication.getApplication().appComponent.inject(this)
        service = mApi.securedServices.event
    }

    private fun getLastEventId(username: String) : String? {
        if (lastEventIds[username] == null) {
            lastEventIds[username] = recoverLastEventId(username)
        }
        return lastEventIds[username]
    }

    private fun setLastEventId(username: String, eventId: String) {
        lastEventIds[username] = eventId
        backupLastEventId(username, eventId)
    }

    /**
     * @return if there are any more events to process
     */
    @Throws(IOException::class)
    private fun next(handler: EventHandler): Boolean {
        // This must be done sequentially, parallel should not work at the same time
        synchronized(this) {
            val eventID = getLastEventId(handler.username) // try to get the saved event Id
            if (eventID == null) {
                refreshContacts(handler)
                refresh(handler) // refresh other things like messages
                getNewId(handler.username)
            }

            val lastEvent = recoverLastEvent(handler.username)
            var response = EventResponse()
            lastEvent.ifNullElse({
                response = ParseUtils.parse(service.check(getLastEventId(handler.username)!!, RetrofitTag(handler.username)).execute())
            },{
                response = Gson().fromJson(lastEvent, EventResponse::class.java)
            })

            if (response.code == Constants.RESPONSE_CODE_OK) {
                backupLastEvent(handler.username, Gson().toJson(response))
                handleEvents(handler, response)
                removeLastEvent(handler.username)
                return if (lastEvent == null) response.hasMore() else true
            } else {
                throw ApiException(response, response.error)
            }
        }
    }

    @Throws(IOException::class)
    fun pull(loggedInUsers: List<String>) {
        loggedInUsers.forEach {username ->
            if (!eventHandlers.containsKey(username)) { /* TODO make EventManager aware of different users, we already parametrised "username" */
                eventHandlers[username] = EventHandler(mApi, databaseProvider, mUserManager, mJobManager, username)
            }
        }

        // go thourh all of the logged in users and check their event separately
        eventHandlers.forEach { while (next(it.value)); }
    }

    fun clearState() {
        sharedPrefs = mutableMapOf()
        lastEventIds = mutableMapOf()
        eventHandlers = mutableMapOf()
    }

    @Throws(IOException::class)
    private fun refreshContacts(handler: EventHandler) {
        Log.d("PMTAG", "EventManager handler refreshContacts")
        synchronized(this) {
            handler.handleRefreshContacts()
        }
    }

    @Throws(IOException::class)
    private fun refresh(handler: EventHandler) {
        Log.d("PMTAG", "EventManager handler handleRefresh")
        synchronized(this) {
            lockState(handler.username)
            handler.handleRefresh()
        }
    }

    @Throws(IOException::class)
    private fun getNewId(username: String) {
        val response = ParseUtils.parse(service.latestID(RetrofitTag(username)).execute())
        if (response.code == Constants.RESPONSE_CODE_OK) {
            setLastEventId(username, response.eventID)
        } else {
            throw ApiException(response, response.error)
        }
    }

    private fun handleEvents(handler: EventHandler, response: EventResponse) {
        setLastEventId(handler.username, response.eventID)
        if (response.refreshContacts()) {
            refreshContacts(handler)
        }
        if (response.refresh()) {
            refresh(handler)
            return
        }
        Log.d("PMTAG", "EventManager handler stage and write")
        handler.stage(response)
        // if we crash between these steps, we need to force refresh: our current state will become invalid
        handler.write()
    }

    private fun lockState(username: String) {
        backupLastEventId(username, "")
        lastEventIds[username] = null
    }

    private fun recoverLastEventId(username: String): String? {
        val prefs = sharedPrefs.getOrPut(username, { ProtonMailApplication.getApplication().getSecureSharedPreferences(username) })
        Log.d("PMTAG", "EventManager recoverLastEventId, user=${mUserManager.username}")
        val lastEventId = prefs.getString(PREF_LATEST_EVENT_ID, null)
        return if (TextUtils.isEmpty(lastEventId)) null else lastEventId
    }

    private fun backupLastEventId(username: String, eventId: String) {
        val prefs = sharedPrefs.getOrPut(username, { ProtonMailApplication.getApplication().getSecureSharedPreferences(username) })
        Log.d("PMTAG", "EventManager backupLastEventId, user=${mUserManager.username}")
        prefs.edit().putString(PREF_LATEST_EVENT_ID, eventId).apply()
    }

    private fun recoverLastEvent(username: String): String? {
        val prefs = sharedPrefs.getOrPut(username, { ProtonMailApplication.getApplication().getSecureSharedPreferences(username) })
        return prefs.getString(PREF_LATEST_EVENT, null)
    }

    private fun backupLastEvent(username: String, event: String) {
        val prefs = sharedPrefs.getOrPut(username, { ProtonMailApplication.getApplication().getSecureSharedPreferences(username) })
        recoverLastEvent(username).ifNull {
            prefs.edit().putString(PREF_LATEST_EVENT, event).apply()
        }
    }

    private fun removeLastEvent(username: String) {
        val prefs = sharedPrefs.getOrPut(username, { ProtonMailApplication.getApplication().getSecureSharedPreferences(username) })
        prefs.edit().remove(PREF_LATEST_EVENT).apply()
    }
}
