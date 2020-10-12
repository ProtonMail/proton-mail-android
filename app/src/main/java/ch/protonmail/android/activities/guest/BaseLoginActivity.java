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
package ch.protonmail.android.activities.guest;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.BaseConnectivityActivity;
import ch.protonmail.android.activities.mailbox.MailboxActivity;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;

import static ch.protonmail.android.activities.NavigationActivityKt.EXTRA_FIRST_LOGIN;

/**
 * Created by dkadrikj on 11/13/15.
 */
public abstract class BaseLoginActivity extends BaseConnectivityActivity {

    public static final String EXTRA_FORCE_UPGRADE = "force_upgrade";
    public static final String EXTRA_API_OFFLINE = "api_offline";

    protected static final int ANIMATION_START_DELAY = 0;
    protected static final int ANIMATION_DURATION = 300;
    protected final int mDefaultKeyboardDP = 100;
    protected final int mEstimatedKeyboardDP = mDefaultKeyboardDP + (Build.VERSION.SDK_INT >= Build
            .VERSION_CODES.LOLLIPOP ? 48 : 0);
    protected int mVerticalAnimValue = 0;
    protected int mHorizontalAnimValue = 0;
    protected int mSpacing = 0;
    protected int mLogoHeight = 0;
    protected int mTitleHeight = 0;
    protected int mInputLayoutVerticalValue = 0;
    protected int mLogoVerticalValue = 0;
    protected int mLogoHorizontalValue = 0;
    protected int mTitleVerticalValue = 0;
    protected int mTitleHorizontalValue = 0;
    protected boolean mKeyboardShown = false;
    protected int topOffset;

    protected View.OnTouchListener mTouchListener = (v, event) -> {
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        v.requestFocus();
        return false;
    };
    @BindView(R.id.logo)
    ImageView mLogo;
    @BindView(R.id.title)
    TextView mTitle;
    @BindView(R.id.input_layout)
    LinearLayout mInputLayout;
    @Nullable
    @BindView(R.id.progress_circular)
    ProgressBar mProgressBar;
    protected View.OnFocusChangeListener mFocusListener = (v, hasFocus) -> {
        if (hasFocus) {
            animate();
        } else {
            revertAnimation();
        }
    };
    @BindView(R.id.container)
    View mRootLayout;

    protected abstract void setFocuses();

    @Override
    protected boolean isPreventingScreenshots() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mProgressBar != null) {
            mProgressBar.getIndeterminateDrawable().setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.MULTIPLY);
        }

        mVerticalAnimValue = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        mHorizontalAnimValue = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        mSpacing = getResources().getDimensionPixelSize(R.dimen.spacing) + getResources().getDimensionPixelSize(R.dimen.fields_default_space_medium);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        topOffset = UiUtil.getStatusBarHeight(this);
        int[] locationLogo = new int[2];
        int[] locationTitle = new int[2];
        int[] locationInput = new int[2];
        mLogo.getLocationOnScreen(locationLogo);
        mTitle.getLocationOnScreen(locationTitle);
        mInputLayout.getLocationOnScreen(locationInput);
        mLogoHeight = mLogo.getHeight();
        mTitleHeight = mTitle.getHeight();
        mLogoHorizontalValue = locationLogo[0];
        mLogoVerticalValue = locationLogo[1] - topOffset;

        mTitleHorizontalValue = locationTitle[0];
        mTitleVerticalValue = locationTitle[1] - topOffset;

        mInputLayoutVerticalValue = locationInput[1] - topOffset;
        new Handler().postDelayed(this::setFocuses, 1000);

        mRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int estimatedKeyboardHeight = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, mEstimatedKeyboardDP,
                            mRootLayout.getResources().getDisplayMetrics());
            final Rect r = new Rect();
            // Conclude whether the keyboard is shown or not.
            mRootLayout.getWindowVisibleDisplayFrame(r);
            int heightDiff = mRootLayout.getRootView().getHeight() - (r.bottom - r.top);
            boolean isShown = heightDiff >= estimatedKeyboardHeight;
            if (!isShown && mKeyboardShown) {
                revertAnimation();
                mKeyboardShown = false;
            } else if (isShown) {
                animate();
                mKeyboardShown = true;
            }
        });
    }

    private void animate() {
        mLogo.animate().x(mHorizontalAnimValue).y(mVerticalAnimValue).setDuration(ANIMATION_DURATION)
                .setStartDelay(ANIMATION_START_DELAY);
        mTitle.animate().x(mHorizontalAnimValue).y(mVerticalAnimValue + mLogoHeight - 10)
                .setDuration(ANIMATION_DURATION).setStartDelay(ANIMATION_START_DELAY);
        mInputLayout.animate().y(mVerticalAnimValue + mSpacing + mTitleHeight)
                .setDuration(ANIMATION_DURATION).setStartDelay(ANIMATION_START_DELAY);
    }

    private void revertAnimation() {
        mLogo.animate().x(mLogoHorizontalValue).y(mLogoVerticalValue).setDuration(ANIMATION_DURATION)
                .setStartDelay(ANIMATION_START_DELAY);
        mTitle.animate().x(mTitleHorizontalValue).y(mTitleVerticalValue).setDuration(ANIMATION_DURATION)
                .setStartDelay(ANIMATION_START_DELAY);
        mInputLayout.animate().y(mInputLayoutVerticalValue).setDuration(ANIMATION_DURATION)
                .setStartDelay(ANIMATION_START_DELAY);
    }

    protected void onMailboxSuccess() { }
    protected void onMailboxNoNetwork() { }
    protected void onMailboxUpdate() { }
    protected void onMailboxInvalidCredential() { }
    protected void onMailboxNotSignedUp() { }
    protected void onMailboxFailed() { }

    protected void onLoginEvent(MailboxLoginEvent event) {
        if (event == null) {
            return;
        }
        ProtonMailApplication.getApplication().resetMailboxLoginEvent();
        switch (event.status) {
            case SUCCESS: {
                // Unregister from event bus, as MailboxActivity try mailboxLogin
                // when remember me is checked, which sends {@link MailboxLoginEvent}
                ProtonMailApplication.getApplication().resetLoginEvent();
                onMailboxSuccess();
                Intent mailboxIntent = AppUtil.decorInAppIntent(new Intent(this, MailboxActivity.class));
                mailboxIntent.putExtra(EXTRA_FIRST_LOGIN, true);
                mailboxIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mailboxIntent);
                finish();
            }
            break;
            case NO_NETWORK: {
                onMailboxNoNetwork();
                TextExtensions.showToast(this, R.string.no_network, Toast.LENGTH_LONG, Gravity.CENTER);
            }
            break;
            case UPDATE: {
                TextExtensions.showToast(this, R.string.update_app);
                onMailboxUpdate();
            }
            break;
            case INVALID_CREDENTIAL: {
                TextExtensions.showToast(this, R.string.invalid_mailbox_password, Toast.LENGTH_LONG, Gravity.CENTER);
                onMailboxInvalidCredential();
            }
            break;
            case INCORRECT_KEY_PARAMETERS: {
                TextExtensions.showToast(this, R.string.incorrect_key_parameters, Toast.LENGTH_LONG, Gravity.CENTER);
                onMailboxInvalidCredential();
            }
            break;
            case NOT_SIGNED_UP: {
                TextExtensions.showToast(this, R.string.not_signed_up, Toast.LENGTH_LONG, Gravity.CENTER);
                onMailboxNotSignedUp();
            }
            break;
            case FAILED:
            default: {
                TextExtensions.showToast(this, R.string.mailbox_login_failure, Toast.LENGTH_LONG, Gravity.CENTER);
                onMailboxFailed();
            }
        }
    }
}
