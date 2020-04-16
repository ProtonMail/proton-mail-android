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
package ch.protonmail.android.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.servers.notification.INotificationServer;
import ch.protonmail.android.servers.notification.NotificationServer;

/**
 * Created by dino on 12/25/16.
 */

public class VerificationOnSendReceiver extends BroadcastReceiver {

    public static final String EXTRA_NOTIFICATION_MESSAGE_TITLE = "notification_message_title";
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_MESSAGE_INLINE = "message_inline";
    public static final String EXTRA_MESSAGE_ADDRESS_ID = "message_address_id";

    @Inject
    UserManager mUserManager;

    public VerificationOnSendReceiver() {
        super();
        ProtonMailApplication.getApplication().getAppComponent().inject(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            INotificationServer notificationServer = new NotificationServer(context, notificationManager);
            String messageTitle = extras.getString(EXTRA_NOTIFICATION_MESSAGE_TITLE);
            String messageId = extras.getString(EXTRA_MESSAGE_ID);
            boolean messageInline = extras.getBoolean(EXTRA_MESSAGE_INLINE);
            String messageAddressId = extras.getString(EXTRA_MESSAGE_ADDRESS_ID);
            notificationServer.notifyVerificationNeeded( mUserManager.getUser(), messageTitle, messageId, messageInline, messageAddressId);
        }
    }
}
