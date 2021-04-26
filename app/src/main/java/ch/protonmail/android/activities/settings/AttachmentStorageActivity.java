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
package ch.protonmail.android.activities.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.BaseActivity;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.storage.AttachmentClearingService;
import ch.protonmail.android.utils.extensions.TextExtensions;

public class AttachmentStorageActivity extends BaseActivity {

    public static final String EXTRA_SETTINGS_ATTACHMENT_STORAGE_VALUE = "EXTRA_SETTINGS_ATTACHMENT_STORAGE_VALUE";

    @BindView(R.id.attachment_storage_value)
    SeekBar mSeekBar;
    @BindView(R.id.storage_text_value)
    TextView mStorageTextValue;

    private int mAttachmentStorageCurrentValue;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_attachment_storage;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAttachmentStorageCurrentValue = getIntent().getIntExtra(EXTRA_SETTINGS_ATTACHMENT_STORAGE_VALUE, Constants.MAX_ATTACHMENT_STORAGE_IN_MB);
        if (mAttachmentStorageCurrentValue == -1) {
            mSeekBar.setProgress(5);
            mStorageTextValue.setText(getString(R.string.attachment_storage_value_current_unlimited));
        } else {
            mStorageTextValue.setText(String.format(getString(R.string.attachment_storage_value_current), mAttachmentStorageCurrentValue));
            mSeekBar.setProgress((mAttachmentStorageCurrentValue / Constants.MIN_ATTACHMENT_STORAGE_IN_MB) - 1); // 0-based
        }
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekBar.setProgress(progress);
                if (progress == 5) {
                    mAttachmentStorageCurrentValue = -1;
                    mStorageTextValue.setText(getString(R.string.attachment_storage_value_current_unlimited));
                    return;
                }
                int value = (progress + 1) * Constants.MIN_ATTACHMENT_STORAGE_IN_MB;
                mAttachmentStorageCurrentValue = value;
                mStorageTextValue.setText(String.format(getString(R.string.attachment_storage_value_current), value));


                User user = mUserManager.getCurrentLegacyUser();
                boolean attachmentStorageChanged = mAttachmentStorageCurrentValue != user.getMaxAttachmentStorage();
                if (attachmentStorageChanged) {
                    user.setMaxAttachmentStorage(mAttachmentStorageCurrentValue);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // noop
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // noop
            }
        });
    }

    @OnClick(R.id.clear_local_cache)
    public void onLocalCacheClearClicked() {
        AttachmentClearingService.startClearUpImmediatelyService(
                getApplicationContext(),
                mUserManager.requireCurrentUserId()
        );
        TextExtensions.showToast(this, R.string.local_storage_cleared, Toast.LENGTH_SHORT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            saveLastInteraction();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_OK);
        saveLastInteraction();
        finish();
    }

}
