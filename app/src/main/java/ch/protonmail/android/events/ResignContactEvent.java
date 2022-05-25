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
package ch.protonmail.android.events;

import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.jobs.contacts.GetSendPreferenceJob;

/**
 * Created by protonlabs on 5/28/18.
 */

public class ResignContactEvent {

    private final SendPreference mSendPreference;
    private final GetSendPreferenceJob.Destination mDestination;
    public final @ContactEvent.Status
    int mStatus;

    public ResignContactEvent(SendPreference sendPreference, int status, GetSendPreferenceJob.Destination destination) {
        mSendPreference = sendPreference;
        mStatus = status;
        mDestination = destination;
    }

    public SendPreference getSendPreference() {
        return mSendPreference;
    }

    public int getStatus() {
        return mStatus;
    }

    public GetSendPreferenceJob.Destination getDestination() {
        return mDestination;
    }
}
