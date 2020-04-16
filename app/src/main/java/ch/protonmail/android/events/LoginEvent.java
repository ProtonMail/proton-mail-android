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

import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.address.Address;

public class LoginEvent extends ResponseBody {
    private final AuthStatus status;
    private final String keySalt;
    private final boolean redirectToSetup;
    private final User user;
    private List<Address> addresses;
    private final String domainName;
    private final String username;

    public LoginEvent(AuthStatus status, String keySalt, boolean redirectToSetup, User user,
                      String username, String domainName, List<Address> addresses) {
        this.status = status;
        this.keySalt = keySalt;
        this.redirectToSetup = redirectToSetup;
        this.user = user;
        this.username = username;
        this.domainName = domainName;
        this.addresses = addresses;
    }

    public AuthStatus getStatus() {
        return status;
    }

    public String getKeySalt() {
        return keySalt;
    }

    public boolean isRedirectToSetup() {
        return redirectToSetup;
    }

    public User getUser() {
        return user;
    }

    public String getUsername() {
        return username;
    }

    public String getDomainName() {
        return domainName;
    }

    public List<Address> getAddresses() {
        return addresses;
    }
}
