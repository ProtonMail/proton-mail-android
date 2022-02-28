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
package ch.protonmail.android.views;

/**
 * Created by dkadrikj on 7.10.15.
 */
public class PopupMenuItem {
    private String mTitle;
    private int mImageResId;

    /**
     * Constructor.
     *
     * @param title      title
     * @param imageResId the image resource id
     */
    public PopupMenuItem(String title, int imageResId) {
        mTitle = title;
        mImageResId = imageResId;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getImageResId() {
        return mImageResId;
    }
}
