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
package ch.protonmail.android.events.organizations;

import ch.protonmail.android.api.models.Keys;
import ch.protonmail.android.api.models.OrganizationResponse;
import ch.protonmail.android.events.Status;

/**
 * Created by dkadrikj on 7/12/16.
 */
public class OrganizationEvent {
    private final Status status;
    private final OrganizationResponse response;
    private final Keys organizationKeys;

    public OrganizationEvent(Status status, OrganizationResponse response, Keys organizationKeys) {
        this.status = status;
        this.response = response;
        this.organizationKeys = organizationKeys;
    }

    public Status getStatus() {
        return status;
    }

    public OrganizationResponse getResponse() {
        return response;
    }

    public Keys getOrganizationKeys() {
        return organizationKeys;
    }
}
