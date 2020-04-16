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

import androidx.annotation.IntDef;

import java.util.List;

public class ContactEvent {

    @IntDef({SUCCESS, SAVED, ALREADY_EXIST, INVALID_EMAIL, NO_NETWORK, NOT_ALL_SYNC, ERROR, DUPLICATE_EMAIL})
    public @interface Status { }
    public static final int SUCCESS = 1;
    public static final int SAVED = 2;
    public static final int ALREADY_EXIST = 3;
    public static final int INVALID_EMAIL = 4;
    public static final int NO_NETWORK = 5;
    public static final int NOT_ALL_SYNC = 6;
    public static final int ERROR = 7;
    public static final int DUPLICATE_EMAIL = 8;

    public final @Status int status;
    public final boolean contactCreation;
    private List<Integer> statuses;

    public ContactEvent(int status, boolean contactCreation) {
        this.status = status;
        this.contactCreation = contactCreation;
    }

    public ContactEvent(int status, boolean contactCreation, List<Integer> statuses) {
        this.status = status;
        this.contactCreation = contactCreation;
        this.statuses = statuses;
    }

    public List<Integer> getStatuses() {
        return statuses;
    }
}
