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
package ch.protonmail.android.events;

import java.util.List;
import java.util.Map;

import ch.protonmail.android.core.Constants;

/**
 * Created by dino on 1/14/17.
 */

public class FetchEmailKeysEvent {

    private final Status mStatus;
    private final List<EmailKeyResponse> mResponse;
    private final boolean mRetry;

    public FetchEmailKeysEvent(Status status) {
        mStatus = status;
        mResponse = null;
        mRetry = false;
    }

    public FetchEmailKeysEvent(Status status, List<EmailKeyResponse> response, boolean retry) {
        mStatus = status;
        mResponse = response;
        mRetry = retry;
    }

    public Status getStatus() {
        return mStatus;
    }

    public List<EmailKeyResponse> getResponse() {
        return mResponse;
    }

    public boolean isRetry() {
        return mRetry;
    }

    public static class EmailKeyResponse {
        private final Map<String, String> mKeys;
        private Constants.RecipientLocationType mLocation; // to, cc or bcc
        private final Status mStatus;

        public EmailKeyResponse(Map<String, String> keys, Constants.RecipientLocationType location, Status status) {
            this.mKeys = keys;
            this.mLocation = location;
            mStatus = status;
        }

        public Map<String, String> getKeys() {
            return mKeys;
        }

        public Constants.RecipientLocationType getLocation() {
            return mLocation;
        }

        public Status getStatus() {
            return mStatus;
        }
    }
}
