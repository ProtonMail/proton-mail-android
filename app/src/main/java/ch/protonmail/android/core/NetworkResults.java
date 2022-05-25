/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.core;

import com.squareup.otto.Produce;

import javax.inject.Inject;
import javax.inject.Singleton;

import ch.protonmail.android.events.MailboxLoadedEvent;

/**
 * Created by dkadrikj on 1/13/16.
 */
@Singleton
public class NetworkResults {

    private MailboxLoadedEvent mMailboxLoadedEvent;

    @Inject
    public NetworkResults(ProtonMailApplication app) {
        app.getBus().register(this);
    }

    @Produce
    public MailboxLoadedEvent produceMailboxLoaded() {
        return mMailboxLoadedEvent;
    }

    public void setMailboxLoaded(MailboxLoadedEvent event) {
        this.mMailboxLoadedEvent = event;
    }
}
