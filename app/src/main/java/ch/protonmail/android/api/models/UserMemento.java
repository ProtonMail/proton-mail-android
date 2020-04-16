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
package ch.protonmail.android.api.models;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.protonmail.android.api.models.address.Address;

/**
 * Created by dkadrikj on 11/2/15.
 */
public class UserMemento {
    private String NotificationEmail;
    private String DisplayName;
    private String Signature;
    private int SwipeLeft;
    private int SwipeRight;
    private CopyOnWriteArrayList<Address> aliases;

    public UserMemento(String notificationEmail, String displayName, String signature, int swipeLeft, int swipeRight, List<Address> aliases) {
        NotificationEmail = notificationEmail;
        DisplayName = displayName;
        Signature = signature;
        SwipeLeft = swipeLeft;
        SwipeRight = swipeRight;
        this.aliases = new CopyOnWriteArrayList<>(aliases);
    }

    public String getNotificationEmail() {
        return NotificationEmail;
    }

    public String getDisplayName() {
        return DisplayName;
    }

    public String getSignature() {
        return Signature;
    }

    public int getSwipeLeft() {
        return SwipeLeft;
    }

    public int getSwipeRight() {
        return SwipeRight;
    }

    public CopyOnWriteArrayList<Address> getAddresses() {
        return aliases;
    }
}
