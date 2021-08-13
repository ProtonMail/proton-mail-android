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
package ch.protonmail.android.activities;

import android.os.Bundle;
import androidx.annotation.NonNull;

import ch.protonmail.android.core.Constants;
import ch.protonmail.android.permissions.PermissionHelper;

/**
 * Created by dkadrikj on 9/18/16.
 */
public abstract class BaseStoragePermissionActivity extends BaseConnectivityActivity implements PermissionHelper.PermissionCallback {

    protected Boolean mHasStoragePermission;
    protected PermissionHelper storagePermissionHelper;

    protected abstract void storagePermissionGranted();

    protected abstract boolean checkForPermissionOnStartup();
    protected boolean checkPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storagePermissionHelper = PermissionHelper.newInstance(Constants.PermissionType.STORAGE, this, this);
        mHasStoragePermission = null;
        checkPin = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkPin = true;
        if (checkForPermissionOnStartup()) {
            storagePermissionHelper.checkPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        storagePermissionHelper.onRequestPermissionsResult(requestCode, grantResults);
    }

    @Override
    public void onPermissionConfirmed(Constants.PermissionType type) {
        mHasStoragePermission = true;
        storagePermissionGranted();
    }

    @Override
    public void onPermissionDenied(Constants.PermissionType type) { checkPin = false; }

    @Override
    public void onHasPermission(Constants.PermissionType type) { storagePermissionGranted(); }
}
