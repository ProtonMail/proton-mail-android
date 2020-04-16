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
package ch.protonmail.android.views.models;

import java.io.Serializable;
import java.util.List;

/**
 * Created by dino on 12/28/17.
 */

public class LocalContact implements Serializable {
    private String name;
    private List<String> emails;
    private List<String> phones;
    private List<LocalContactAddress> addresses;
    private List<String> groups;

    public LocalContact(String name, List<String> emails, List<String> phones, List<LocalContactAddress> addresses, List<String> groups) {
        this.name = name;
        this.emails = emails;
        this.phones = phones;
        this.addresses = addresses;
        this.groups = groups;
    }

    public String getName() {
        return name;
    }

    public List<String> getEmails() {
        return emails;
    }

    public List<String> getPhones() {
        return phones;
    }

    public List<LocalContactAddress> getAddresses() {
        return addresses;
    }

    public List<String> getGroups() {
        return groups;
    }
}
