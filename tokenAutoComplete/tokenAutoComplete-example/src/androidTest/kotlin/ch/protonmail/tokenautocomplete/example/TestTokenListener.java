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
package ch.protonmail.tokenautocomplete.example;

import java.util.ArrayList;
import java.util.List;

import ch.protonmail.tokenautocomplete.TokenCompleteTextView;

class TestTokenListener implements TokenCompleteTextView.TokenListener<Person> {

    List<Person> added = new ArrayList<>();
    List<Person> ignored = new ArrayList<>();
    List<Person> removed = new ArrayList<>();

    @Override
    public void onTokenAdded(Person token) {
        added.add(token);
    }

    @Override
    public void onTokenIgnored(Person token) {
        ignored.add(token);
    }

    @Override
    public void onTokenRemoved(Person token) {
        removed.add(token);
    }
}
