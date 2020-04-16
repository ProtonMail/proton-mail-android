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


public class NotificationsUpdatedEvent {
    private final AuthStatus emailStatus;
    private final Status updatesNotifyStatus;
    private String email;
    private String displayName;

    public NotificationsUpdatedEvent(final AuthStatus emailStatus, final Status updatesNotifyStatus) {
        this.emailStatus = emailStatus;
        this.updatesNotifyStatus = updatesNotifyStatus;
    }

    public NotificationsUpdatedEvent(AuthStatus emailStatus, Status updatesNotifyStatus, String email) {
        this.emailStatus = emailStatus;
        this.updatesNotifyStatus = updatesNotifyStatus;
        this.email = email;
    }

    public NotificationsUpdatedEvent(AuthStatus emailStatus, Status updatesNotifyStatus, String email, String displayName) {
        this.emailStatus = emailStatus;
        this.updatesNotifyStatus = updatesNotifyStatus;
        this.email = email;
        this.displayName = displayName;
    }

    public AuthStatus getEmailStatus() {
        return emailStatus;
    }

    public Status getUpdatesNotifyStatus() {
        return updatesNotifyStatus;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }
}
