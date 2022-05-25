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
package ch.protonmail.android.activities.dialogs;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.adapters.QuickSnoozeOptionAdapter;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.settings.presentation.SnoozeNotificationsActivity;
import ch.protonmail.android.views.CustomQuickSnoozeDialog;
import dagger.hilt.android.AndroidEntryPoint;

import static ch.protonmail.android.navigation.presentation.NavigationActivityKt.REQUEST_CODE_SNOOZED_NOTIFICATIONS;

@AndroidEntryPoint
public class QuickSnoozeDialogFragment
        extends AbstractDialogFragment
        implements AdapterView.OnItemClickListener, CustomQuickSnoozeDialog.CustomQuickSnoozeListener {

    @BindView(R.id.snooze_options_list_view)
    ListView mSnoozeOptionsListView;
    @BindView(R.id.dialog_title)
    TextView mTitle;
    @BindView(R.id.notifications_resume_in)
    TextView mNotificationsResumeIn;
    @BindView(R.id.quick_snooze_turn_off_container)
    View mQuickSnoozeTurnOffContainer;
    @BindView(R.id.quick_snooze_turn_off)
    TextView mQuickSnoozeTurnOff;

    private List<String> mQuickSnoozeValues;
    private QuickSnoozeOptionAdapter mAdapter;
    private int mSelectedSnoozeMinutes = 0;

    @Inject
    UserManager userManager;

    private CustomQuickSnoozeDialog mCustomValueDialog;

    /**
     * Instantiates a new fragment of this class.
     *
     * @return new instance of {@link QuickSnoozeDialogFragment}
     */
    public static QuickSnoozeDialogFragment newInstance() {
        return new QuickSnoozeDialogFragment();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.dialog_fragment_quick_snooze;
    }

    @Override
    protected void initUi(View rootView) {
        getDialog().setCanceledOnTouchOutside(true);
        boolean isQuickSnoozeEnabled = userManager.isSnoozeQuickEnabledBlocking();
        if (isQuickSnoozeEnabled) {
            int notificationsResumeInMinutes = (int) ((userManager.getSnoozeSettings().getSnoozeQuickEndTime() - System.currentTimeMillis()) / 1000 / 60);
            if (notificationsResumeInMinutes > 60) {
                mNotificationsResumeIn.setText(String.format(getString(R.string.quick_snooze_resume_hours), notificationsResumeInMinutes / 60, notificationsResumeInMinutes % 60));
            } else {
                mNotificationsResumeIn.setText(getResources().getQuantityString(R.plurals.quick_snooze_resume_min, notificationsResumeInMinutes, notificationsResumeInMinutes));
            }
            mNotificationsResumeIn.setVisibility(View.VISIBLE);
        } else {
            mQuickSnoozeTurnOffContainer.setVisibility(View.GONE);
        }
        mQuickSnoozeValues = Arrays.asList(getResources().getStringArray(R.array.quick_snooze_values));
        List<QuickSnoozeOptionAdapter.QuickSnoozeItem> items = new ArrayList<>();
        for (String value : mQuickSnoozeValues) {
            items.add(new QuickSnoozeOptionAdapter.QuickSnoozeItem(false, value));
        }
        mAdapter = new QuickSnoozeOptionAdapter(getContext(), items);
        mSnoozeOptionsListView.setAdapter(mAdapter);
        mSnoozeOptionsListView.setOnItemClickListener(this);
    }

    @Override
    protected int getStyleResource() {
        return R.style.AppTheme_Dialog_Labels;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.QuickSnoozeFragment";
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (position == 7) {
            Intent scheduledSnoozeIntent = new Intent(activity, SnoozeNotificationsActivity.class);
            activity.startActivityForResult(scheduledSnoozeIntent, REQUEST_CODE_SNOOZED_NOTIFICATIONS);
            return;
        }
        if (position == 6) {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            mCustomValueDialog = new CustomQuickSnoozeDialog();
            mCustomValueDialog.init(this, 0, 0);
            fragmentTransaction.add(mCustomValueDialog, "custom_quick_snooze");
            fragmentTransaction.commitAllowingStateLoss();
            return;
        }
        mSelectedSnoozeMinutes = getResources().getIntArray(R.array.quick_snooze_values_int)[position];
        userManager.setSnoozeQuickBlocking(true, mSelectedSnoozeMinutes);
        dismissAllowingStateLoss();
    }

    @OnClick({R.id.quick_snooze_turn_off_container, R.id.quick_snooze_turn_off})
    public void onQuickSnoozeTurnOffClicked() {
        mSelectedSnoozeMinutes = 0;
        userManager.setSnoozeQuickBlocking(false, 0);
        dismissAllowingStateLoss();
    }

    @Override
    public void onCustomQuickSnoozeSet(int minutes) {
        mSelectedSnoozeMinutes = minutes;
        userManager.setSnoozeQuickBlocking(true, mSelectedSnoozeMinutes);
        dismissAllowingStateLoss();
    }

    @Override
    public void onCancel() {
        mCustomValueDialog.dismissAllowingStateLoss();
    }
}
