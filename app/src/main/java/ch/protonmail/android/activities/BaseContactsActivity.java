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

import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import ch.protonmail.android.core.Constants;
import ch.protonmail.android.permissions.PermissionHelper;

/**
 * Created by dkadrikj on 11/27/15.
 * @deprecated implement using composition of android contacts repositories and PermissionHelper
 */
@Deprecated
public abstract class BaseContactsActivity extends BaseConnectivityActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        PermissionHelper.PermissionCallback {

    protected static final int LOADER_ID_ANDROID_CONTACTS = 1;


    //region Android Contact store
    private static final String ANDROID_ORDER_BY = ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY + " ASC";
    private static final String ANDROID_SELECTION =
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY + " LIKE ?" + " OR " + ContactsContract.CommonDataKinds.Email.ADDRESS + " LIKE ?" + " OR "
                    + ContactsContract.CommonDataKinds.Email.DATA + " LIKE ?";
    private static final String[] ANDROID_PROJECTION = {
            ContactsContract.CommonDataKinds.Email.RAW_CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.DATA
    };
    //endregion

    protected PermissionHelper contactsPermissionHelper;
    protected PermissionHelper storagePermissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactsPermissionHelper = PermissionHelper.newInstance(Constants.PermissionType.CONTACTS, this, this);
        storagePermissionHelper = PermissionHelper.newInstance(Constants.PermissionType.STORAGE, this, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        contactsPermissionHelper.onRequestPermissionsResult(requestCode, grantResults);
        storagePermissionHelper.onRequestPermissionsResult(requestCode, grantResults);
    }



    private Loader<Cursor> createAndroidLoader(String searchString) {
        String[] selectionArgs = new String[]{"%" + searchString + "%", "%" + searchString + "%",
                "%" + searchString + "%"};
        return new CursorLoader(
                this,
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                ANDROID_PROJECTION,
                ANDROID_SELECTION,
                selectionArgs,
                ANDROID_ORDER_BY
        );
    }

    protected abstract void contactPermissionGranted();
    protected abstract void contactPermissionDenied();

    public abstract String getSearchTerm();

    @Override
    public void onPermissionConfirmed(Constants.PermissionType type) {
        contactPermissionGranted();
    }

    @Override
    public void onPermissionDenied(Constants.PermissionType type) {
        contactPermissionDenied();
    }

    @Override
    public void onHasPermission(Constants.PermissionType type) {
        contactPermissionGranted();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_ANDROID_CONTACTS:
                String searchTerm = getSearchTerm();
                if (searchTerm == null)
                    searchTerm = "";
                return createAndroidLoader(searchTerm);
            default:
                throw new LoaderCreationException("Unknown loader constant: " + id);
        }
    }

    public static class LoaderCreationException extends RuntimeException {
        LoaderCreationException(String message) {
            super(message);
        }
    }
}

