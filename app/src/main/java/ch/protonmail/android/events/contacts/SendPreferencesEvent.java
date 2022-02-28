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
package ch.protonmail.android.events.contacts;

import java.util.Map;

import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.jobs.contacts.GetSendPreferenceJob;

/**
 * Created by protonlabs on 5/26/18.
 */

public class SendPreferencesEvent {

    private final Status status;
    private final Map<String, SendPreference> mSendPreferenceMap;
    private final GetSendPreferenceJob.Destination mDestination;
    private final boolean recipientExists;

    public SendPreferencesEvent(Status status,
                                Map<String, SendPreference> mSendPreferenceMap,
                                GetSendPreferenceJob.Destination destination,
                                boolean recipientExists) {
        this.status = status;
        this.mSendPreferenceMap = mSendPreferenceMap;
        this.mDestination = destination;
        this.recipientExists = recipientExists;
    }

    public Map<String, SendPreference> getSendPreferenceMap() {
        return mSendPreferenceMap;
    }

    public GetSendPreferenceJob.Destination getDestination() {
        return mDestination;
    }

    public boolean recipientExists() {
        return recipientExists;
    }

    public Status getStatus() {
        return status;
    }
}
