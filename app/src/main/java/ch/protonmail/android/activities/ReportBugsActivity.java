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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.squareup.otto.Subscribe;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.BugReportEvent;
import ch.protonmail.android.events.LogoutEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.user.MailSettingsEvent;
import ch.protonmail.android.jobs.ReportBugsJob;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;

public class ReportBugsActivity extends BaseConnectivityActivity {
    @BindView(R.id.bug_description_title)
    EditText mBugDescriptionTitle;

    @BindView(R.id.bug_description)
    EditText mBugDescription;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_report_bugs;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.report_bugs);
        }

        mBugDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No op
            }

            @Override
            public void afterTextChanged(Editable s) {
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProtonMailApplication.getApplication().getBus().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ProtonMailApplication.getApplication().getBus().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if(!mBugDescription.getText().toString().isEmpty()) {
            DialogUtils.Companion.showDeleteConfirmationDialog(
                    this, getString(R.string.unsaved_changes_title),
                    getString(R.string.unsaved_changes_subtitle), unit ->
                    {
                        super.onBackPressed();
                        return unit;
                    });
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.report_bugs_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.send_report).setEnabled(!mBugDescription.getText().toString().trim().isEmpty());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.send_report:
                String OSName = "Android";
                String OSVersion = "" + Build.VERSION.SDK_INT;
                String client = "Android";
                String appVersionName = String.format(getString(R.string.full_version_name_report_bugs), AppUtil.getAppVersionName(this), AppUtil.getAppVersionCode(this));
                String title = mBugDescriptionTitle.getText().toString();
                String description = mBugDescription.getText().toString();
                String username = mUserManager.getUsername();
                String email = mUserManager.getUser().getDefaultEmail();
                mJobManager.addJobInBackground(new ReportBugsJob(OSName, OSVersion, client, appVersionName, title, description, username, email));
                TextExtensions.showToast(this, R.string.sending_report, Toast.LENGTH_SHORT);
                saveLastInteraction();
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Subscribe
    public void  onMailSettingsEvent(MailSettingsEvent event) {
        loadMailSettings();
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
    }

    @Subscribe
    public void onBugReportEvent(BugReportEvent event){
        if (event.getStatus() == Status.SUCCESS){
            TextExtensions.showToast(this, R.string.received_report, Toast.LENGTH_SHORT);
        } else if (event.getStatus() == Status.NO_NETWORK) {
            TextExtensions.showToast(this, R.string.not_received_report_offline);
        } else {
            TextExtensions.showToast(this, R.string.not_received_report, Toast.LENGTH_SHORT);
        }
    }
}
