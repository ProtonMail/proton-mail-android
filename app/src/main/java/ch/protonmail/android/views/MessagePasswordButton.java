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
package ch.protonmail.android.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import ch.protonmail.android.R;

/**
 * Created by dkadrikj on 4/8/16.
 */
public class MessagePasswordButton extends ImageButton implements MessagePasswordViewDialog.MessagePasswordDialogListener {

    private String mMessagePassword;
    private String mPasswordHint;
    private boolean mIsPasswordSet;
    private boolean mIsActive;
    private GestureDetector gestureDetector;
    private AlertDialog messagePasswordDialog;
    private MessagePasswordViewDialog dialog;
    private final OnMessagePasswordChangedListener mListener;

    public MessagePasswordButton(Context context) {
        this(context, null);
    }

    public MessagePasswordButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MessagePasswordButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mListener = (OnMessagePasswordChangedListener) context;
        init(context);
    }

    private void init(Context context) {
        gestureDetector = new GestureDetector(context, new SingleTapConfirm());
    }

    public boolean isPasswordSet() {
        return mIsPasswordSet;
    }

    public void reset() {
        mIsPasswordSet = false;
        mMessagePassword = null;
    }

    public String getMessagePassword() {
        return mMessagePassword;
    }

    public String getPasswordHint() {
        return mPasswordHint;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private void onLongPressed() {
        DialogInterface.OnClickListener clickListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                mIsPasswordSet = false;
                mPasswordHint = "";
                mMessagePassword = "";
                mListener.onMessagePasswordChanged();
            }
            dialog.dismiss();
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getContext().getString(R.string.remove_password))
                .setMessage(getContext().getString(R.string.remove_password_question))
                .setPositiveButton(R.string.yes, clickListener)
                .setNeutralButton(R.string.cancel, clickListener)
                .setCancelable(false)
                .create()
                .show();
    }

    private void onClicked() {
        if (dialog == null || messagePasswordDialog == null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.message_password_view2, null);
            dialog = (MessagePasswordViewDialog) dialogView;
            dialog.setListener(this);
            builder.setView(dialogView);
            messagePasswordDialog = builder.create();
            messagePasswordDialog.show();
        } else {
            messagePasswordDialog.show();
        }
    }

    @Override
    public void cancelled() {
        mIsActive = false;
        messagePasswordDialog.dismiss();
    }

    @Override
    public void passwordSet(String password, @Nullable String passwordHint) {
        mMessagePassword = password;
        mPasswordHint = passwordHint;
        mIsPasswordSet = true;
        mIsActive = false;
        if (messagePasswordDialog != null) {
            messagePasswordDialog.dismiss();
            mListener.onMessagePasswordChanged();
        }
    }

    public boolean isValid() {
        return !mIsActive || !TextUtils.isEmpty(mMessagePassword);
    }

    @Override
    public void setActive() {
        mIsActive = true;
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            onClicked();
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            onLongPressed();
        }
    }

    public interface OnMessagePasswordChangedListener {
        void onMessagePasswordChanged();
    }
}
