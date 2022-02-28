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
import android.widget.ListView;

public class ListViewAdapter implements ViewAdapter {

    private final ListView mListView;

    public ListViewAdapter(ListView listView) {
        mListView = listView;
    }

    @Override
    public Context getContext() {
        return mListView.getContext();
    }

    @Override
    public int getWidth() {
        return mListView.getWidth();
    }

    @Override
    public int getChildCount() {
        return mListView.getChildCount();
    }

    @Override
    public void getLocationOnScreen(int[] locations) {
        mListView.getLocationOnScreen(locations);
    }

    @Override
    public View getChildAt(int index) {
        return mListView.getChildAt(index);
    }

    @Override
    public int getChildPosition(View child) {
        return mListView.getPositionForView(child);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        mListView.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public void onTouchEvent(MotionEvent e) {
        mListView.onTouchEvent(e);
    }

    @Override
    public AbsListView.OnScrollListener makeScrollListener(AbsListView.OnScrollListener listener) {
        return listener;
    }
}
