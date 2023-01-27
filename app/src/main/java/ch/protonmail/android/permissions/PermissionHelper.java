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
package ch.protonmail.android.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import ch.protonmail.android.core.Constants;

public class PermissionHelper {
    private static final int REQUEST_CODE_READ_CONTACTS = 1001;
    private static final int REQUEST_CODE_ACCESS_STORAGE = 1011;

    private final PermissionCallback mPermissionCallback;
    private final Constants.PermissionType _permissionType;
    private String mManifestPermission;
    private String mPermissionRequestedPref;
    private int mRequestCode;
    private final Activity mActivity;

    private PermissionHelper(Constants.PermissionType type, Activity activity, PermissionCallback callback) {
        _permissionType = type;
        if (type == Constants.PermissionType.STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mManifestPermission = Manifest.permission.READ_MEDIA_IMAGES;
            } else mManifestPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            mRequestCode = REQUEST_CODE_ACCESS_STORAGE;
            mPermissionRequestedPref = Constants.Prefs.PREF_PERMISSION_ACCESS_STORAGE;
        } else if (type == Constants.PermissionType.CONTACTS) {
            mManifestPermission = Manifest.permission.READ_CONTACTS;
            mRequestCode = REQUEST_CODE_READ_CONTACTS;
            mPermissionRequestedPref = Constants.Prefs.PREF_PERMISSION_READ_CONTACTS;
        }
        this.mActivity = activity;
        this.mPermissionCallback = callback;
    }

    public static PermissionHelper newInstance(Constants.PermissionType type, Activity activity, PermissionCallback callback) {
        return new PermissionHelper(type, activity, callback);
    }

    public void checkPermission() {
        int permission = ActivityCompat.checkSelfPermission(mActivity, mManifestPermission);
        boolean should = ActivityCompat.shouldShowRequestPermissionRationale(mActivity, mManifestPermission);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            if (should) {
                // No explanation needed, we can request the permission.
                requestPermission();
            } else {
                //TWO CASE:
                //1. first time - system up - //request window
                SharedPreferences defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
                if (!defaultSharedPrefs.getBoolean(mPermissionRequestedPref, false)) {
                    defaultSharedPrefs.edit().putBoolean(mPermissionRequestedPref, true).apply();
                    requestPermission();
                } else {
                    //2. second time - user denied with never ask - go to settings
                    // not needed as per product decision
                    // todo show popup that the app needs permission
                    mPermissionCallback.onPermissionDenied(_permissionType);
                }
            }
            return;
        }

        if (this.mPermissionCallback != null) {
            this.mPermissionCallback.onHasPermission(_permissionType);
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(mActivity, new String[]{mManifestPermission}, mRequestCode);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode == mRequestCode) {
            boolean hasSth = grantResults.length > 0;
            if (hasSth) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //user accepted
                    if (this.mPermissionCallback != null) {
                        this.mPermissionCallback.onPermissionConfirmed(_permissionType);
                    }
                } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    if (this.mPermissionCallback != null) {
                        this.mPermissionCallback.onPermissionDenied(_permissionType);
                    }
                }
            }
        }
    }

    public interface PermissionCallback {
        void onPermissionConfirmed(Constants.PermissionType type);
        void onPermissionDenied(Constants.PermissionType type);
        void onHasPermission(Constants.PermissionType type);
    }
}
