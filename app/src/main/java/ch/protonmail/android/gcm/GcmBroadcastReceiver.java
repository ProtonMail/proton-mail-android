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
package ch.protonmail.android.gcm;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import ch.protonmail.android.R;

public class GcmBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);
        Bundle extras = intent.getExtras();
        if (!GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(
                messageType) && !GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(
                messageType) && extras != null) {
            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtras(intent.getExtras());
            broadcastIntent.setAction(context.getString(R.string.action_notification));
            if (!LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent)) {
                Intent serviceIntent = new Intent(broadcastIntent);

                ComponentName comp = new ComponentName(context.getPackageName(),
                        GcmIntentService.class.getName());
                ContextCompat.startForegroundService(context, serviceIntent.setComponent(comp));
            }
        }
    }
}
