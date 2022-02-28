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
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;

public class MessagePasswordViewDialog extends LinearLayout {

    // can be null
    @Nullable
    private MessagePasswordDialogListener mListener;

    @BindView(R.id.define_password)
    EditText mDefinePassword;
    @BindView(R.id.confirm_password)
    EditText mConfirmPassword;
    @BindView(R.id.define_hint)
    EditText mDefineHint;
    private String mMessagePassword;
    private String mPasswordHint;
    private boolean mIsActive;
    private InputMethodManager imm;

    public MessagePasswordViewDialog(Context context) {
        this(context, null, 0);
    }

    public MessagePasswordViewDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MessagePasswordViewDialog(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater.from(context).inflate(R.layout.message_password_view_dialog, this, true);
        ButterKnife.bind(this);

        TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                    UiUtil.hideKeyboard(context, mDefineHint);
                    return true;
                }

                return false;
            }
        };

        mDefineHint.setOnEditorActionListener(onEditorActionListener);
    }

    public void setListener(MessagePasswordDialogListener listener) {
        mListener = listener;
        mListener.setActive();
    }

    public void show(InputMethodManager imm) {
        mMessagePassword = "";
        mPasswordHint = "";

        mDefinePassword.setText("");
        mConfirmPassword.setText("");
        mDefineHint.setText("");

        mIsActive = true;
        setVisibility(VISIBLE);
        mDefinePassword.requestFocus();
        this.imm = imm;
        imm.showSoftInput(mDefinePassword, InputMethodManager.SHOW_IMPLICIT);
    }

    @OnClick(R.id.apply)
    void onNext() {
        if (isValid() && mListener != null) {
            mListener.passwordSet(mMessagePassword, mPasswordHint);
        }
    }

    @OnClick(R.id.cancel)
    public void onCancelClicked() {
        mIsActive = false;
        if (mListener != null) {
            mListener.cancelled();
        }
        if (imm == null) {
            imm = (InputMethodManager) ProtonMailApplication.getApplication().getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(mDefineHint.getWindowToken(), 0);
        }
    }

    @OnClick(R.id.more_info)
    public void onMoreInfo() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getContext().getString(R.string.eo_info)));
        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            getContext().startActivity(intent);
        } else {
            TextExtensions.showToast(getContext(), R.string.no_browser_found, Toast.LENGTH_SHORT);
        }
    }

    public boolean isValid() {
        if (TextUtils.isEmpty(mDefinePassword.getText())) {
            TextExtensions.showToast(getContext(), R.string.eo_password_not_completed, Toast.LENGTH_LONG, Gravity.CENTER);
            return false;
        }
        if (TextUtils.isEmpty(mConfirmPassword.getText())) {
            TextExtensions.showToast(getContext(), R.string.eo_password_not_completed, Toast.LENGTH_LONG, Gravity.CENTER);
            return false;
        }
        mMessagePassword = mDefinePassword.getText().toString();
        mPasswordHint = mDefineHint.getText().toString();
        if (!mMessagePassword.equals(mConfirmPassword.getText().toString())) {
            TextExtensions.showToast(getContext(), R.string.eo_passwords_do_not_match, Toast.LENGTH_LONG, Gravity.CENTER);
            return false;
        }
        return true;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, mMessagePassword, mPasswordHint, mIsActive);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mMessagePassword = savedState.getMessagePassword();
        mPasswordHint = savedState.getPasswordHint();
        mIsActive = savedState.isActive();

        setVisibility(mIsActive ? VISIBLE : GONE);
    }

    public interface MessagePasswordDialogListener {
        void cancelled();
        void passwordSet(String password, @Nullable String passwordHint);
        void setActive();
    }

    protected static class SavedState extends BaseSavedState {

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        private final String messagePassword;
        private final String passwordHint;
        private final boolean isActive;

        private SavedState(Parcelable superState, String messagePassword, String passwordHint, boolean isActive) {
            super(superState);
            this.messagePassword = messagePassword;
            this.passwordHint = passwordHint;
            this.isActive = isActive;
        }

        private SavedState(Parcel in) {
            super(in);
            this.messagePassword = in.readString();
            this.passwordHint = in.readString();
            this.isActive = (in.readInt() == 1);
        }

        public String getMessagePassword() {
            return messagePassword;
        }

        public String getPasswordHint() {
            return passwordHint;
        }

        public boolean isActive() {
            return isActive;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(this.messagePassword);
            out.writeString(this.passwordHint);
            out.writeInt(this.isActive ? 1 : 0);
        }
    }
}
