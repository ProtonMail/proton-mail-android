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
package ch.protonmail.android.events.general;

import java.util.List;

import ch.protonmail.android.events.Status;

public class AvailableDomainsEvent {
    private final Status status;
    private final boolean retryOnError;
    private List<String> domains;
    public AvailableDomainsEvent(Status status, boolean retryOnError) {
        this.status = status;
        this.retryOnError = retryOnError;
    }

    public AvailableDomainsEvent(Status status, List<String> domains, boolean retryOnError) {
        this.status = status;
        this.domains = domains;
        this.retryOnError = retryOnError;
    }

    public Status getStatus() {
        return status;
    }

    public List<String> getDomains() {
        return domains;
    }

    public boolean isRetryOnError() {
        return retryOnError;
    }
}
