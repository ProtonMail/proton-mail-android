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
package ch.protonmail.android.views.swipe.adapter;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;

/**
 * Interface for a given UI element to help extend the swipe-to-dismiss-undo pattern to other
 * elements.
 */
public interface ViewAdapter {
    Context getContext();

    int getWidth();

    int getChildCount();

    void getLocationOnScreen(int[] locations);

    View getChildAt(int index);

    int getChildPosition(View position);

    void requestDisallowInterceptTouchEvent(boolean disallowIntercept);

    void onTouchEvent(MotionEvent e);

    Object makeScrollListener(AbsListView.OnScrollListener listener);
}
