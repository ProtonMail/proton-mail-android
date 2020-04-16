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

package ch.protonmail.android.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Class to hold a ListView item and the swipe backgrounds
 *
 * Created by wdullaer on 22.06.14.
 */
public class SwipeViewGroup extends FrameLayout {
    private View contentView = null;

    private int visibleView = SwipeDirections.DIRECTION_NEUTRAL;
    private SparseArray<View> mBackgroundMap = new SparseArray<>();
    private OnTouchListener swipeTouchListener;

    /**
     * Standard android View constructor
     *
     * @param context
     */
    public SwipeViewGroup(Context context) {
        super(context);
        initialize();
    }

    /**
     * Standard android View constructor
     *
     * @param context
     * @param attrs
     */
    public SwipeViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    /**
     * Standard android View constructor
     *
     * @param context
     * @param attrs
     * @param defStyle
     */
    public SwipeViewGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    /**
     * Common code for all the constructors
     */
    private void initialize() {
        // Allows click events to reach the ListView in case the row has a clickable View like a Button
        // FIXME: probably messes with accessibility. Doesn't fix root cause (see onTouchEvent)
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
    }

    /**
     * Add a View to the background of the Layout. The background should have the same height
     * as the contentView
     *
     * @param background The View to be added to the Layout
     * @param direction The key to be used to find it again
     * @return A reference to the a layout so commands can be chained
     */
    public SwipeViewGroup addBackground(View background, int direction){
        if(mBackgroundMap.get(direction) != null) removeView(mBackgroundMap.get(direction));

        background.setVisibility(View.INVISIBLE);
        mBackgroundMap.put(direction, background);
        addView(background);
        return this;
    }

    /**
     * Show the View linked to a key. Don't do anything if the key is not found
     *
     * @param direction The key of the View to be shown
     */
    public void showBackground(int direction){
        if(SwipeDirections.DIRECTION_NEUTRAL != direction && mBackgroundMap.get(direction) == null) return;

        if(SwipeDirections.DIRECTION_NEUTRAL != visibleView)
            mBackgroundMap.get(visibleView).setVisibility(View.INVISIBLE);
        if(SwipeDirections.DIRECTION_NEUTRAL != direction) {
            mBackgroundMap.get(direction).setVisibility(View.VISIBLE);
        }

        visibleView = direction;
    }

    /**
     * Add a contentView to the Layout
     *
     * @param contentView The View to be added
     * @return A reference to the layout so commands can be chained
     */
    public SwipeViewGroup setContentView(View contentView){
        if(this.contentView != null) removeView(contentView);
        addView(contentView);
        this.contentView = contentView;

        return this;
    }

    /**
     * Returns the current contentView of the Layout
     *
     * @return contentView of the Layout
     */
    public View getContentView(){
        return contentView;
    }

    /**
     * Move all backgrounds to the edge of the Layout so they can be swiped in
     */
    public void translateBackgrounds(){
        this.setClipChildren(false);
        for(int i=0;i<mBackgroundMap.size();i++){
            int key = mBackgroundMap.keyAt(i);
            View value = mBackgroundMap.valueAt(i);
            value.setTranslationX(-Integer.signum(key)*value.getWidth());
        }
    }

    /**
     * Set a touch mListener the SwipeViewGroup will watch: once the OnTouchListener is interested in
     * events, the SwipeViewGroup will stop propagating touch events to its children
     *
     * @param swipeTouchListener The OnTouchListener to watch
     * @return A reference to the layout so commands can be chained
     */
    public SwipeViewGroup setSwipeTouchListener(OnTouchListener swipeTouchListener) {
        this.swipeTouchListener = swipeTouchListener;
        return this;
    }

    @Override
    public Object getTag() {
        if(contentView != null) return contentView.getTag();
        else return null;
    }

    @Override
    public void setTag(Object tag) {
        if(contentView != null) contentView.setTag(tag);
    }

    @Override
    public Object getTag(int key) {
        if(contentView != null) return contentView.getTag(key);
        else return null;
    }

    @Override
    public void setTag(int key, Object tag) {
        if(contentView != null) contentView.setTag(key, tag);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Start tracking the touch when a child is processing it
        return super.onInterceptTouchEvent(ev) || swipeTouchListener.onTouch(this, ev);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        // Finish the swipe gesture: our parent will no longer do it if this function is called
        return swipeTouchListener.onTouch(this, ev);
    }
}
