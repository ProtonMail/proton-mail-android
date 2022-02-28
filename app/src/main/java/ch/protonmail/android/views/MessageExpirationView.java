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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.utils.DateUtil;
import ch.protonmail.android.utils.ServerTime;

public class MessageExpirationView extends LinearLayout implements MessageExpirationDialog.MessageExpirationListener {

    @BindView(R.id.expiration_time)
    TextView mExpirationTime;
    @BindView(R.id.reset)
    ImageButton mReset;

    private int mDays;
    private int mHours;
    private boolean mIsActive;
    private final OnMessageExpirationChangedListener mListener;
    private MessageExpirationDialog dialog;

    public MessageExpirationView(Context context) {
        this(context, null, 0);
    }

    public MessageExpirationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MessageExpirationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater.from(context).inflate(R.layout.message_expiration_view, this, true);
        ButterKnife.bind(this);
        mListener = (OnMessageExpirationChangedListener) context;
    }

    public void show() {
        mIsActive = true;
        setVisibility(VISIBLE);
        renderView();
        onExpirationTime();
    }

    public long getExpirationTime() {
        return getTimeFromPresent();
    }

    private int getTimeFromPresent(){
        return (mDays * 24 * 60 * 60) + (mHours * 60 * 60);
    }

    private void renderView() {
        mExpirationTime.setText(DateUtil.formatDaysAndHours(getContext(), mDays, mHours, 0));
        mReset.setVisibility(getTimeFromPresent() > 0? VISIBLE : INVISIBLE);
    }

    @OnClick(R.id.expiration_time)
    public void onExpirationTime() {
        FragmentActivity activity = (FragmentActivity) getContext();
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        dialog = new MessageExpirationDialog();
        dialog.init(this, mDays, mHours);
        fragmentTransaction.add(dialog, "message_expiration");
        fragmentTransaction.commitAllowingStateLoss();
    }

    @OnClick(R.id.hide_view)
    public void onHideView() {
        if (dialog != null) {
            dialog.dismissAllowingStateLoss();
        }
        mListener.onMessageExpirationChanged();
        setVisibility(GONE);
        mIsActive = false;
    }

    @OnClick(R.id.reset)
    public void onReset() {
        mDays = 0;
        mHours = 0;
        renderView();
    }

    public void reset() {
        mDays = 0;
        mHours = 0;
        renderView();
    }

    @Override
    public void onMessageExpirationSet(int days, int hours) {
        mDays = days;
        mHours = hours;
        renderView();
    }

    @Override
    public void onCancel() {
        onHideView();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, mDays, mHours, mIsActive);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mDays = savedState.getDays();
        mHours = savedState.getHours();
        mIsActive = savedState.isActive();

        setVisibility(mIsActive ? VISIBLE : GONE);
        renderView();
    }

    public interface OnMessageExpirationChangedListener {
        void onMessageExpirationChanged();
    }

    protected static class SavedState extends BaseSavedState {

        private final int days;
        private final int hours;
        private final boolean isActive;

        private SavedState(Parcelable superState, int days, int hours, boolean isActive) {
            super(superState);
            this.days = days;
            this.hours = hours;
            this.isActive = isActive;
        }

        private SavedState(Parcel in) {
            super(in);
            this.days = in.readInt();
            this.hours = in.readInt();
            this.isActive = (in.readInt() == 1);
        }

        public int getDays() {
            return days;
        }

        public int getHours() {
            return hours;
        }

        public boolean isActive() {
            return isActive;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.days);
            out.writeInt(this.hours);
            out.writeInt(this.isActive ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
