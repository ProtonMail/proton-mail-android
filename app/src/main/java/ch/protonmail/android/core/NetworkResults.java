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
package ch.protonmail.android.core;

import com.squareup.otto.Produce;

import javax.inject.Singleton;

import ch.protonmail.android.events.MailboxLoadedEvent;

/**
 * Created by dkadrikj on 1/13/16.
 */
@Singleton
public class NetworkResults {

    private MailboxLoadedEvent mMailboxLoadedEvent;

    public NetworkResults() {
        ProtonMailApplication.getApplication().getBus().register(this);
    }

    @Produce
    public MailboxLoadedEvent produceMailboxLoaded() {
        return mMailboxLoadedEvent;
    }

    public void setMailboxLoaded(MailboxLoadedEvent event) {
        this.mMailboxLoadedEvent = event;
    }
}
