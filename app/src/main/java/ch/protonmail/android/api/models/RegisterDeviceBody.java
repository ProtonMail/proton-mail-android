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

import android.content.Context;
import android.os.Build;
import android.provider.Settings.Secure;
import androidx.annotation.NonNull;

import ch.protonmail.android.gcm.GcmUtil;
import ch.protonmail.android.utils.AppUtil;

public class RegisterDeviceBody {
    private String DeviceToken;
    private String DeviceName;
    private String DeviceModel;
    private String DeviceVersion;
    private String AppVersion;
    private int Environment;

    public RegisterDeviceBody(@NonNull Context context) {
        DeviceToken = GcmUtil.getRegistrationId();
        DeviceName = "Android";
        DeviceModel = Build.MODEL;
        DeviceVersion = "" + Build.VERSION.SDK_INT;
        AppVersion = "Android_" + AppUtil.getAppVersionName(context.getApplicationContext()); // TODO remove context
        Environment = 4;
    }
}
