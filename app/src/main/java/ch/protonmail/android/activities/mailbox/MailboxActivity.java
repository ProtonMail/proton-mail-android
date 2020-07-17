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
package ch.protonmail.android.activities.mailbox;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.birbit.android.jobqueue.Job;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.SnackbarContentLayout;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.EditSettingsItemActivity;
import ch.protonmail.android.activities.EngagementActivity;
import ch.protonmail.android.activities.MailboxViewModel;
import ch.protonmail.android.activities.NavigationActivity;
import ch.protonmail.android.activities.SearchActivity;
import ch.protonmail.android.activities.SettingsActivity;
import ch.protonmail.android.activities.SettingsItem;
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity;
import ch.protonmail.android.activities.dialogs.ManageLabelsDialogFragment;
import ch.protonmail.android.activities.dialogs.MoveToFolderDialogFragment;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.activities.guest.MailboxLoginActivity;
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity;
import ch.protonmail.android.activities.messageDetails.MessageDetailsActivity;
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository;
import ch.protonmail.android.activities.settings.SettingsEnum;
import ch.protonmail.android.adapters.messages.MessagesListViewHolder;
import ch.protonmail.android.adapters.messages.MessagesRecyclerViewAdapter;
import ch.protonmail.android.adapters.swipe.ArchiveSwipeHandler;
import ch.protonmail.android.adapters.swipe.MarkReadSwipeHandler;
import ch.protonmail.android.adapters.swipe.SpamSwipeHandler;
import ch.protonmail.android.adapters.swipe.StarSwipeHandler;
import ch.protonmail.android.adapters.swipe.SwipeAction;
import ch.protonmail.android.adapters.swipe.TrashSwipeHandler;
import ch.protonmail.android.api.models.MessageCount;
import ch.protonmail.android.api.models.SimpleMessage;
import ch.protonmail.android.api.models.UnreadTotalMessagesResponse;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.room.counters.CountersDatabase;
import ch.protonmail.android.api.models.room.counters.CountersDatabaseFactory;
import ch.protonmail.android.api.models.room.counters.TotalLabelCounter;
import ch.protonmail.android.api.models.room.counters.TotalLocationCounter;
import ch.protonmail.android.api.models.room.messages.Label;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory;
import ch.protonmail.android.api.models.room.pendingActions.PendingSend;
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.api.services.MessagesService;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.data.ContactsRepository;
import ch.protonmail.android.events.AttachmentFailedEvent;
import ch.protonmail.android.events.AuthStatus;
import ch.protonmail.android.events.ConnectivityEvent;
import ch.protonmail.android.events.FetchLabelsEvent;
import ch.protonmail.android.events.FetchUpdatesEvent;
import ch.protonmail.android.events.ForceSwitchedAccountEvent;
import ch.protonmail.android.events.LabelAddedEvent;
import ch.protonmail.android.events.LogoutEvent;
import ch.protonmail.android.events.MailboxLoadedEvent;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.events.MailboxNoMessagesEvent;
import ch.protonmail.android.events.MessageCountsEvent;
import ch.protonmail.android.events.MessageDeletedEvent;
import ch.protonmail.android.events.MessageSentEvent;
import ch.protonmail.android.events.ParentEvent;
import ch.protonmail.android.events.RefreshDrawerEvent;
import ch.protonmail.android.events.SettingsChangedEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.user.MailSettingsEvent;
import ch.protonmail.android.events.user.UserInfoEvent;
import ch.protonmail.android.gcm.GcmUtil;
import ch.protonmail.android.gcm.PMRegistrationIntentService;
import ch.protonmail.android.jobs.EmptyFolderJob;
import ch.protonmail.android.jobs.FetchByLocationJob;
import ch.protonmail.android.jobs.FetchLabelsJob;
import ch.protonmail.android.jobs.PingJob;
import ch.protonmail.android.jobs.PostArchiveJob;
import ch.protonmail.android.jobs.PostDeleteJob;
import ch.protonmail.android.jobs.PostInboxJob;
import ch.protonmail.android.jobs.PostLabelJob;
import ch.protonmail.android.jobs.PostReadJob;
import ch.protonmail.android.jobs.PostSpamJob;
import ch.protonmail.android.jobs.PostStarJob;
import ch.protonmail.android.jobs.PostTrashJobV2;
import ch.protonmail.android.jobs.PostUnreadJob;
import ch.protonmail.android.jobs.PostUnstarJob;
import ch.protonmail.android.prefs.SecureSharedPreferences;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Event;
import ch.protonmail.android.utils.MessageUtils;
import ch.protonmail.android.utils.NetworkUtil;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;
import ch.protonmail.android.utils.ui.selection.SelectionModeEnum;
import ch.protonmail.android.views.alerts.StorageLimitAlert;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import timber.log.Timber;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING;
import static ch.protonmail.android.activities.EditSettingsItemActivityKt.EXTRA_SETTINGS_ITEM_TYPE;
import static ch.protonmail.android.activities.MailboxViewModelKt.FLOW_START_ACTIVITY;
import static ch.protonmail.android.activities.MailboxViewModelKt.FLOW_TRY_COMPOSE;
import static ch.protonmail.android.activities.MailboxViewModelKt.FLOW_USED_SPACE_CHANGED;
import static ch.protonmail.android.activities.NavigationActivityKt.EXTRA_FIRST_LOGIN;
import static ch.protonmail.android.activities.NavigationActivityKt.EXTRA_LOGOUT;
import static ch.protonmail.android.activities.NavigationActivityKt.EXTRA_SWITCHED_TO_USER;
import static ch.protonmail.android.activities.NavigationActivityKt.EXTRA_SWITCHED_USER;
import static ch.protonmail.android.activities.NavigationActivityKt.REQUEST_CODE_SWITCHED_USER;
import static ch.protonmail.android.activities.labelsManager.LabelsManagerActivityKt.EXTRA_CREATE_ONLY;
import static ch.protonmail.android.activities.labelsManager.LabelsManagerActivityKt.EXTRA_MANAGE_FOLDERS;
import static ch.protonmail.android.activities.labelsManager.LabelsManagerActivityKt.EXTRA_POPUP_STYLE;
import static ch.protonmail.android.activities.settings.BaseSettingsActivityKt.EXTRA_CURRENT_MAILBOX_LABEL_ID;
import static ch.protonmail.android.activities.settings.BaseSettingsActivityKt.EXTRA_CURRENT_MAILBOX_LOCATION;
import static ch.protonmail.android.core.Constants.Prefs.PREF_SWIPE_GESTURES_DIALOG_SHOWN;
import static ch.protonmail.android.core.Constants.SWIPE_GESTURES_CHANGED_VERSION;
import static ch.protonmail.android.servers.notification.NotificationServerKt.EXTRA_MAILBOX_LOCATION;
import static ch.protonmail.android.servers.notification.NotificationServerKt.EXTRA_USERNAME;
import static ch.protonmail.android.settings.pin.ValidatePinActivityKt.EXTRA_TOTAL_COUNT_EVENT;

public class MailboxActivity extends NavigationActivity implements
        AbsListView.MultiChoiceModeListener,
        SwipeRefreshLayout.OnRefreshListener,
        ManageLabelsDialogFragment.ILabelCreationListener,
        ManageLabelsDialogFragment.ILabelsChangeListener,
        MoveToFolderDialogFragment.IMoveMessagesListener, DialogInterface.OnDismissListener {

    private static final String TAG_MAILBOX_ACTIVITY = "MailboxActivity";
    private static final String ACTION_MESSAGE_DRAFTED = "ch.protonmail.MESSAGE_DRAFTED";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String STATE_MAILBOX_LOCATION = "mailbox_location";
    private static final String STATE_MAILBOX_LABEL_LOCATION = "mailbox_label_location";
    private static final String STATE_MAILBOX_LABEL_LOCATION_NAME = "mailbox_label_location_name";

    public static final int LOADER_ID = 0;
    public static final int LOADER_ID_LABELS_OFFLINE = 32;
    private static final int REQUEST_CODE_TRASH_MESSAGE_DETAILS = 1;
    private static final int REQUEST_CODE_COMPOSE_MESSAGE = 19;

    private static final String TAG = "MailboxActivity";

    private CountersDatabase countersDatabase;
    private PendingActionsDatabase pendingActionsDatabase;
    @Inject
    MessageDetailsRepository messageDetailsRepository;
    @Inject
    ContactsRepository contactsRepository;

    @BindView(R.id.messages_list_view)
    RecyclerView mMessagesListView;
    @BindView(R.id.swipe_refresh_wrapper)
    FrameLayout mSwipeRefreshWrapper;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.spinner_layout)
    SwipeRefreshLayout mSpinnerSwipeRefreshLayout;
    @BindView(R.id.no_messages_layout)
    SwipeRefreshLayout mNoMessagesRefreshLayout;
    @BindView(R.id.move_to_trash)
    TextView mMoveToTrashView;
    @BindView(R.id.layout_sync)
    View mSyncView;
    @BindView(R.id.storageLimitAlert)
    StorageLimitAlert storageLimitAlert;
    @BindView(R.id.screenShotPreventer)
    View screenshotProtector;
    private Snackbar mNoConnectivitySnack;
    private Snackbar mCheckForConnectivitySnack;

    private MessagesRecyclerViewAdapter mAdapter;

    private MutableLiveData<Constants.MessageLocationType> mMailboxLocation = new MutableLiveData<>();
    private AtomicBoolean mIsLoadingMore = new AtomicBoolean(false);
    private boolean mScrollStateChanged = false;
    private ActionMode mActionMode;
    private Snackbar swipeCustomizeSnack;
    private String mLabelId;
    private String mLabelName;
    private boolean refreshMailboxJobRunning;
    private String mSyncUUID;
    private boolean mCustomizeSwipeSnackShown = false;
    private boolean mCatchLabelEvents;
    private MailboxViewModel mailboxViewModel;
    private AlertDialog storageLimitApproachingAlertDialog;
    private LiveSharedPreferences liveSharedPreferences;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_mailbox;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getAppComponent().inject(this);
        countersDatabase = CountersDatabaseFactory.Companion.getInstance(this).getDatabase();
        pendingActionsDatabase = PendingActionsDatabaseFactory.Companion.getInstance(this).getDatabase();

        // force reload of MessageDetailsRepository's internal dependencies in case we just switched user
        // TODO if we decide to use special flag for switching (and not login), change this
        if (getIntent().getBooleanExtra(EXTRA_FIRST_LOGIN, false)) {
            messageDetailsRepository.reloadDependenciesForUser(mUserManager.getUsername());
            GcmUtil.setTokenSent(false); // force GCM to re-register
        }

        Bundle extras = getIntent().getExtras();

        if (!mUserManager.isEngagementShown()) {
            startActivity(AppUtil.decorInAppIntent(new Intent(this, EngagementActivity.class)));
        }
        mMailboxLocation.setValue(Constants.MessageLocationType.INBOX);
        // Set the padding to match the Status Bar height
        if (savedInstanceState != null) {
            //noinspection ResourceType
            final int locationInt = savedInstanceState.getInt(STATE_MAILBOX_LOCATION);
            mLabelId = savedInstanceState.getString(STATE_MAILBOX_LABEL_LOCATION);
            mLabelName = savedInstanceState.getString(STATE_MAILBOX_LABEL_LOCATION_NAME);
            mMailboxLocation.setValue(Constants.MessageLocationType.Companion.fromInt(locationInt));
        }

        if (extras != null && extras.containsKey(EXTRA_MAILBOX_LOCATION)) {
            setupNewMessageLocation(extras.getInt(EXTRA_MAILBOX_LOCATION));
        }

        mailboxViewModel = MailboxViewModel.Companion.create(this, messageDetailsRepository, mUserManager, mJobManager);

        startObserving();

        mailboxViewModel.getToastMessageMaxLabelsReached().observe(this, event -> {
            MailboxViewModel.MaxLabelsReached maxLabelsReached = event.getContentIfNotHandled();
            if (maxLabelsReached != null) {
                String message = String.format(getString(R.string.max_labels_exceeded), maxLabelsReached.getSubject(), maxLabelsReached.getMaxAllowedLabels());
                TextExtensions.showToast(MailboxActivity.this, message, Toast.LENGTH_SHORT);
            }
        });

        startObservingUsedSpace();

        mAdapter = new MessagesRecyclerViewAdapter(this, new Function1<SelectionModeEnum, Unit>() {
            ActionMode actionMode;

            @Override
            public Unit invoke(SelectionModeEnum selectionModeEvent) {

                switch (selectionModeEvent) {
                    case STARTED:
                        actionMode = startActionMode(MailboxActivity.this);
                        break;
                    case ENDED:
                        ActionMode actionMode = this.actionMode;
                        if (actionMode != null) {
                            actionMode.finish();
                            this.actionMode = null;
                        }
                        break;
                }
                return null;
            }
        });

        contactsRepository.findAllContactsEmailsAsync().observe(this, contactEmails -> mAdapter.setContactsList(contactEmails));

        mailboxViewModel.getPendingSendsLiveData().observe(this, mAdapter::setPendingForSendingList);
        mailboxViewModel.getPendingUploadsLiveData().observe(this, mAdapter::setPendingUploadsList);
        messageDetailsRepository.getAllLabels().observe(this, labels -> {
            if (labels != null) {
                mAdapter.setLabels(labels);
            }
        });
        setRefreshing(true);
        checkUserAndFetchNews();

        if (extras != null && extras.containsKey(EXTRA_SWITCHED_TO_USER)) {
            switchAccountProcedure(extras.getString(EXTRA_SWITCHED_TO_USER));
        }

        setUpDrawer();
        setTitle();

        mMessagesListView.setAdapter(mAdapter);
        mMessagesListView.setLayoutManager(new LinearLayoutManager(this));

        buildSwipeProcessor();

        initializeSwipeRefreshLayout(mSwipeRefreshLayout);
        initializeSwipeRefreshLayout(mSpinnerSwipeRefreshLayout);
        initializeSwipeRefreshLayout(mNoMessagesRefreshLayout);

        if (mUserManager.isFirstMailboxLoad()) {
            swipeCustomizeSnack = Snackbar.make(findViewById(R.id.drawer_layout),
                    getString(R.string.customize_swipe_actions), Snackbar.LENGTH_INDEFINITE);
            View view = swipeCustomizeSnack.getView();
            TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            swipeCustomizeSnack.setAction(getString(R.string.settings), v -> {
                Intent settingsIntent = AppUtil.decorInAppIntent(new Intent(MailboxActivity.this, SettingsActivity.class));
                settingsIntent.putExtra(EXTRA_CURRENT_MAILBOX_LOCATION, mMailboxLocation.getValue() != null ? mMailboxLocation.getValue().getMessageLocationTypeValue() : Constants.MessageLocationType.INBOX.getMessageLocationTypeValue());
                settingsIntent.putExtra(EXTRA_CURRENT_MAILBOX_LABEL_ID, mLabelId);
                startActivity(settingsIntent);
            });
            swipeCustomizeSnack.setActionTextColor(getResources().getColor(R.color.icon_purple));
            mUserManager.firstMailboxLoadDone();
        } else if (savedInstanceState == null) {
            // If user enabled to remember mailbox password, this will ensure getting latest UserInfo
            // and checking saved password on current private key
            // if failed ask user to login to mailbox again
        }
        mAdapter.setItemClick(message -> {
            if (message != null && !TextUtils.isEmpty(message.getMessageId())) {
                new OnMessageClickTask(new WeakReference<>(MailboxActivity.this),
                        messageDetailsRepository, message).execute();
            }
            return null;
        });

        checkRegistration();

        closeDrawer();

        mMessagesListView.addOnScrollListener(mListScrollListener);
        fetchOrganizationData();
        LiveData<List<Message>> messagesLiveData = Transformations.switchMap(mMailboxLocation, location -> getLiveDataByLocation(messageDetailsRepository, location));
        mMailboxLocation.observe(this, newLocation -> mAdapter.setNewLocation(newLocation));
        messagesLiveData.observe(this, new MessagesListObserver(mAdapter));
        new ItemTouchHelper(new SwipeController(mUserManager.getUser())).attachToRecyclerView(mMessagesListView);

        if (extras != null && extras.getBoolean(EXTRA_SWITCHED_USER, false)) {
            String newUser = extras.getString(EXTRA_SWITCHED_TO_USER);
            if (!TextUtils.isEmpty(newUser)) {
                switchAccountProcedure(newUser);
            } else {
                onSwitchedAccounts();
            }
        }
    }

    @Override
    protected boolean secureContent() {
        return true;
    }

    @Override
    protected void enableScreenshotProtector() {
        screenshotProtector.setVisibility(View.VISIBLE);
    }

    @Override
    protected void disableScreenshotProtector() {
        screenshotProtector.setVisibility(View.GONE);
    }

    private void startObserving() {
        mailboxViewModel.usedSpaceActionEvent(FLOW_START_ACTIVITY);
        mailboxViewModel.getManageLimitReachedWarning().observe(this, setupUpLimitReachedObserver);
        mailboxViewModel.getManageLimitApproachingWarning().observe(this, setupUpLimitApproachingObserver);
        mailboxViewModel.getManageLimitBelowCritical().observe(this, setupUpLimitBelowCriticalObserver);
        mailboxViewModel.getManageLimitReachedWarningOnTryCompose().observe(this, setupUpLimitReachedTryComposeObserver);
    }

    private void startObservingUsedSpace() {
        if (liveSharedPreferences != null && liveSharedPreferences.hasObservers()) {
            liveSharedPreferences.removeObservers(this);
        }
        liveSharedPreferences = new LiveSharedPreferences((SecureSharedPreferences)
                ProtonMailApplication.getApplication().getSecureSharedPreferences(mUserManager.getUsername()), Constants.Prefs.PREF_USED_SPACE);
        liveSharedPreferences.observe(this, aLong -> {
            mailboxViewModel.usedSpaceActionEvent(FLOW_USED_SPACE_CHANGED);
        });
    }

    private Observer setupUpLimitReachedObserver = (Observer<Event<Boolean>>) limitReached -> {
        Boolean _limitReached = limitReached.getContentIfNotHandled();
        if (_limitReached != null) {
            if (storageLimitApproachingAlertDialog != null) {
                storageLimitApproachingAlertDialog.dismiss();
                storageLimitApproachingAlertDialog = null;
            }
            if (mUserManager.canShowStorageLimitReached()) {
                DialogUtils.Companion.showInfoDialogWithTwoButtons(MailboxActivity.this,
                        getString(R.string.storage_limit_warning_title), getString(R.string.storage_limit_reached_text), getString(R.string.learn_more), getString(R.string.okay), unit -> {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.limit_reached_learn_more)));
                            startActivity(browserIntent);
                            mUserManager.setShowStorageLimitReached(false);
                            return unit;
                        }, unit -> {
                            mUserManager.setShowStorageLimitReached(false);
                            return unit;
                        }, true);
            }
            mUserManager.setShowStorageLimitWarning(true);
            storageLimitAlert.setVisibility(View.VISIBLE);
            storageLimitAlert.setIcon(getResources().getDrawable(R.drawable.inbox));
            storageLimitAlert.setText(getResources().getString(R.string.storage_limit_alert));
        }
    };

    private void showStorageLimitApproachingAlertDialog() {
        storageLimitApproachingAlertDialog = DialogUtils.Companion.showInfoDialogWithTwoButtons(MailboxActivity.this,
                getString(R.string.storage_limit_warning_title),
                getString(R.string.storage_limit_approaching_text),
                getString(R.string.dont_remind_again), getString(R.string.okay), unit -> {
                    mUserManager.setShowStorageLimitWarning(false);
                    storageLimitApproachingAlertDialog = null;
                    return unit;
                }, unit -> {
                    storageLimitApproachingAlertDialog = null;
                    return unit;
                }, true);
    }

    private Observer setupUpLimitApproachingObserver = (Observer<Event<Boolean>>) limitApproaching -> {
        Boolean _limitApproaching = limitApproaching.getContentIfNotHandled();
        if (_limitApproaching != null) {
            if (mUserManager.canShowStorageLimitWarning()) {
                if (storageLimitApproachingAlertDialog == null || !storageLimitApproachingAlertDialog.isShowing()) {
                    // This is the first time the dialog is going to be showed or
                    // the dialog is not showing and had previously been dismissed by clicking the positive or negative button or
                    // the dialog is not showing and had previously been dismissed on touch outside or by clicking the back button
                    showStorageLimitApproachingAlertDialog();
                }
            }
            mUserManager.setShowStorageLimitReached(true);
            storageLimitAlert.setVisibility(View.GONE);
        }
    };

    private Observer setupUpLimitBelowCriticalObserver = (Observer<Event<Boolean>>) limitReached -> {
        Boolean _limitReached = limitReached.getContentIfNotHandled();
        if (_limitReached != null) {
            mUserManager.setShowStorageLimitWarning(true);
            mUserManager.setShowStorageLimitReached(true);
            storageLimitAlert.setVisibility(View.GONE);
        }
    };

    private Observer setupUpLimitReachedTryComposeObserver = (Observer<Event<Boolean>>) limitReached -> {
        Boolean _limitReached = limitReached.getContentIfNotHandled();
        if (_limitReached != null && _limitReached) {
            DialogUtils.Companion.showInfoDialogWithTwoButtons(MailboxActivity.this,
                    getString(R.string.storage_limit_warning_title), getString(R.string
                            .storage_limit_reached_text), getString(R.string.learn_more), getString(R.string.okay), unit -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse
                                (getString(R.string.limit_reached_learn_more)));
                        startActivity(browserIntent);
                        return unit;
                    }, unit -> unit,
                    true);
        } else {
            Intent intent = AppUtil.decorInAppIntent(new Intent(MailboxActivity.this, ComposeMessageActivity.class));
            startActivityForResult(intent, REQUEST_CODE_COMPOSE_MESSAGE);
        }
    };

    //TODO change to MessageDatabase extension function after kotlin
    private LiveData<List<Message>> getLiveDataByLocation(MessageDetailsRepository messageDetailsRepository, Constants.MessageLocationType mMailboxLocation) {
        switch (mMailboxLocation) {
            case STARRED:
                return messageDetailsRepository.getStarredMessagesAsync();
            case LABEL:
            case LABEL_OFFLINE:
            case LABEL_FOLDER:
                return messageDetailsRepository.getMessagesByLabelIdAsync(mLabelId);
            case DRAFT:
            case SENT:
            case ARCHIVE:
            case INBOX:
            case SEARCH:
            case SPAM:
            case TRASH:
                return messageDetailsRepository.getMessagesByLocationAsync(mMailboxLocation.getMessageLocationTypeValue());
            case ALL_MAIL:
                return messageDetailsRepository.getAllMessages();
            case INVALID:
                throw new RuntimeException("Invalid location.");
            default:
                throw new RuntimeException("Unknown location: " + mMailboxLocation);
        }
    }

    private void startObservingPendingActions() {
        mailboxViewModel.getPendingSendsLiveData().removeObservers(this);
        mailboxViewModel.getPendingUploadsLiveData().removeObservers(this);
        mailboxViewModel.reloadDependenciesForUser();
        mailboxViewModel.getPendingSendsLiveData().observe(this, mAdapter::setPendingForSendingList);
        mailboxViewModel.getPendingUploadsLiveData().observe(this, mAdapter::setPendingUploadsList);
    }

    @Override
    protected void onSwitchedAccounts() {
        String username = mUserManager.getUsername();
        mJobManager.start();
        countersDatabase = CountersDatabaseFactory.Companion.getInstance(this).getDatabase();
        pendingActionsDatabase = PendingActionsDatabaseFactory.Companion.getInstance(this).getDatabase();
        messageDetailsRepository.reloadDependenciesForUser(username);
        startObservingPendingActions();
        AppUtil.clearNotifications(this, username);
        getLazyManager().reset();
        setUpDrawer();
        setupAccountsList();
        checkRegistration();
        new Handler().postDelayed(() -> {
            mJobManager.addJobInBackground(new FetchLabelsJob());
            setupNewMessageLocation(Constants.DrawerOptionType.INBOX.getDrawerOptionTypeValue());
        }, 500);
        LiveData<List<Message>> messagesLiveData = Transformations.switchMap(mMailboxLocation, location -> getLiveDataByLocation(messageDetailsRepository, location));
        messagesLiveData.observe(this, new MessagesListObserver(mAdapter));
        messageDetailsRepository.getAllLabels().observe(this, labels -> {
            if (labels != null) {
                mAdapter.setLabels(labels);
            }
        });
        // Account has been switched, so used space changed as well
        mailboxViewModel.usedSpaceActionEvent(FLOW_USED_SPACE_CHANGED);
        // Observe used space for current account
        startObservingUsedSpace();

        // manually update the flags for preventing screenshots
        if (isPreventingScreenshots() || mUserManager.getUser().isPreventTakingScreenshots()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private class MessagesListObserver implements Observer<List<Message>> {

        private final MessagesRecyclerViewAdapter adapter;

        private MessagesListObserver(MessagesRecyclerViewAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onChanged(@Nullable List<Message> messages) {
            if (messages != null) {
                adapter.clear();
                adapter.addAll(messages);
            }
        }
    }

    private RecyclerView.OnScrollListener mListScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView view, int scrollState) {
            mScrollStateChanged = (scrollState == SCROLL_STATE_DRAGGING || scrollState == SCROLL_STATE_SETTLING);
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            if (!mScrollStateChanged) {
                return;
            }
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            RecyclerView.Adapter adapter = recyclerView.getAdapter();

            int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
            int lastPosition = adapter.getItemCount() - 1;

            if (lastVisibleItem == lastPosition && dy > 0 && !setLoadingMore(true)) {
                loadMoreMessages();
            }
        }

        private void loadMoreMessages() {
            final Constants.MessageLocationType mailboxLocation = mMailboxLocation.getValue() != null ? mMailboxLocation.getValue() : Constants.MessageLocationType.INBOX;
            long earliestTime = MessagesService.Companion.getLastMessageTime(mailboxLocation, mLabelId);
            if (mailboxLocation != Constants.MessageLocationType.LABEL && mailboxLocation != Constants.MessageLocationType.LABEL_FOLDER) {
                MessagesService.Companion.startFetchMessages(mailboxLocation, earliestTime);
            } else {
                MessagesService.Companion.startFetchMessagesByLabel(mailboxLocation, earliestTime, mLabelId);
            }
        }
    };

    private void registerGcmReceiver() {
        IntentFilter filter = new IntentFilter(getString(R.string.action_notification));
        filter.setPriority(2);
        LocalBroadcastManager.getInstance(this).registerReceiver(gcmBroadcastReceiver, filter);
    }

    private void registerHumanVerificationReceiver() {

        IntentFilter filter = new IntentFilter(getString(R.string.notification_action_verify));
        filter.setPriority(10);
        registerReceiver(humanVerificationBroadcastReceiver, filter);
    }

    private Handler pingHandler = new Handler();
    private Runnable pingRunnable = () -> mJobManager.addJobInBackground(new PingJob());

    View.OnClickListener connectivityRetryListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mNetworkUtil.setCurrentlyHasConnectivity(true);
            pingHandler.removeCallbacks(pingRunnable);
            pingHandler.postDelayed(pingRunnable, 1000);
            mCheckForConnectivitySnack = NetworkUtil.setCheckingConnectionSnackLayout(mConnectivitySnackLayout, MailboxActivity.this);
            mCheckForConnectivitySnack.show();
            mSyncUUID = UUID.randomUUID().toString();
            if (mNetworkUtil.isConnected()) {
                new Handler().postDelayed(new FetchMessagesRetryRunnable(MailboxActivity.this), 3000);
                boolean thirdPartyConnectionsEnabled = mUserManager.getUser().getAllowSecureConnectionsViaThirdParties();
                if (thirdPartyConnectionsEnabled) {
                    networkConfigurator.refreshDomainsAsync();
                }
            }
        }
    };

    private void checkRegistration() {
        // Check device for Play Services APK.
        if (checkPlayServices()) {
            boolean tokenSent = GcmUtil.isTokenSent();
            if (!tokenSent) {
                PMRegistrationIntentService.Companion.startRegistration(this);
            }
        }
    }

    private Boolean mFirstLogin = null;

    private boolean checkUserAndFetchNews() {
        mSyncUUID = UUID.randomUUID().toString();
        if (mUserManager.isBackgroundSyncEnabled()) {
            setRefreshing(true);
            mSyncView.setVisibility(View.VISIBLE);
        }
        if (mFirstLogin == null) {
            mFirstLogin = getIntent().getBooleanExtra(EXTRA_FIRST_LOGIN, false);
        }
        if (!mFirstLogin) {
            AlarmReceiver alarmReceiver = new AlarmReceiver();
            alarmReceiver.setAlarm(this, true);
            return false;
        } else {
            mFirstLogin = false;
            refreshMailboxJobRunning = true;
            ProtonMailApplication.getApplication().updateDone();
            mJobManager.addJobInBackground(new FetchByLocationJob(mMailboxLocation.getValue(), mLabelId, false, mSyncUUID));
            return true;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // force reload of MessageDetailsRepository's internal dependencies in case we just switched user
        if (intent.getExtras() != null && intent.getExtras().containsKey(EXTRA_SWITCHED_TO_USER)) {
            switchAccountProcedure(intent.getStringExtra(EXTRA_SWITCHED_TO_USER));
        } else if (intent.getBooleanExtra(EXTRA_SWITCHED_USER, false)) {
            onSwitchedAccounts();
        } else if (intent.getBooleanExtra(EXTRA_LOGOUT, false)) {
            onLogout();
        } else if (intent.getExtras() != null && intent.getExtras().containsKey(EXTRA_USERNAME)) {
            if (!mUserManager.getUsername().equals(intent.getStringExtra(EXTRA_USERNAME))) {
                switchAccountProcedure(intent.getStringExtra(EXTRA_USERNAME));
            }
        } else {
            checkRegistration();
            checkUserAndFetchNews();
            setupNewMessageLocation(Constants.DrawerOptionType.INBOX.getDrawerOptionTypeValue());
        }
    }

    private boolean shouldShowSwipeGesturesChangedDialog() {
        SharedPreferences prefs = ((ProtonMailApplication) getApplicationContext()).getDefaultSharedPreferences();
        int previousVersion = prefs.getInt(Constants.Prefs.PREF_PREVIOUS_APP_VERSION, Integer.MIN_VALUE);
        // The dialog should be shown once on the update when swiping gestures are switched
        return previousVersion < SWIPE_GESTURES_CHANGED_VERSION && previousVersion > 0
                && !prefs.getBoolean(PREF_SWIPE_GESTURES_DIALOG_SHOWN, false);
    }

    private void showSwipeGesturesChangedDialog() {
        SharedPreferences prefs = ((ProtonMailApplication) getApplicationContext()).getDefaultSharedPreferences();
        DialogUtils.Companion.showInfoDialogWithTwoButtons(MailboxActivity.this,
                getString(R.string.swipe_gestures_changed),
                getString(R.string.swipe_gestures_changed_message),
                getString(R.string.go_to_settings), getString(R.string.okay), unit -> {
                    Intent swipeGestureIntent = new Intent(this, EditSettingsItemActivity.class);
                    swipeGestureIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.SWIPE);
                    startActivityForResult(AppUtil.decorInAppIntent(swipeGestureIntent), SettingsEnum.SWIPING_GESTURE.ordinal());
                    return unit;
                }, unit -> unit, true, true, true);
        prefs.edit().putBoolean(PREF_SWIPE_GESTURES_DIALOG_SHOWN, true).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mUserManager.isLoggedIn()) {
            return;
        }
        reloadMessageCounts();
        registerGcmReceiver();
        registerHumanVerificationReceiver();
        checkDelinquency();
        mNoMessagesRefreshLayout.setVisibility(View.GONE);
        if (!mNetworkUtil.isConnectedAndHasConnectivity()) {
            showNoConnSnack();
        }

        checkForDraftedMessages();
        final Constants.MessageLocationType mailboxLocation = mMailboxLocation.getValue();
        if (mailboxLocation == Constants.MessageLocationType.INBOX) {
            AppUtil.clearNotifications(this, mUserManager.getUsername());
        }
        if (mailboxLocation == Constants.MessageLocationType.ALL_DRAFT || mailboxLocation == Constants.MessageLocationType.DRAFT) {
            AppUtil.clearSendingFailedNotifications(this, mUserManager.getUsername());
        }

        setUpDrawer();
        closeDrawer(true);

        if (shouldShowSwipeGesturesChangedDialog()) {
            showSwipeGesturesChangedDialog();
        }
    }

    @Override
    protected void onPause() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(gcmBroadcastReceiver);
            unregisterReceiver(showDraftedSnackBroadcastReceiver);
            unregisterReceiver(humanVerificationBroadcastReceiver);
        } catch (Exception e) {
            // noop
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_MAILBOX_LOCATION, mMailboxLocation.getValue() != null ? mMailboxLocation.getValue().getMessageLocationTypeValue() : Constants.MessageLocationType.INBOX.getMessageLocationTypeValue());
        outState.putString(STATE_MAILBOX_LABEL_LOCATION, mLabelId);
        outState.putString(STATE_MAILBOX_LABEL_LOCATION_NAME, mLabelName);
        super.onSaveInstanceState(outState);
    }

    //TODO refactor with onPrepareOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mailbox_options_menu, menu);
        final Constants.MessageLocationType mailboxLocation = mMailboxLocation.getValue();
        menu.findItem(R.id.empty).setVisible(mailboxLocation == Constants.MessageLocationType.DRAFT ||
                mailboxLocation == Constants.MessageLocationType.SPAM ||
                mailboxLocation == Constants.MessageLocationType.TRASH);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.mailbox_options_menu, menu);
        final Constants.MessageLocationType mailboxLocation = mMailboxLocation.getValue();
        menu.findItem(R.id.empty).setVisible(mailboxLocation == Constants.MessageLocationType.DRAFT ||
                mailboxLocation == Constants.MessageLocationType.SPAM ||
                mailboxLocation == Constants.MessageLocationType.TRASH ||
                mailboxLocation == Constants.MessageLocationType.LABEL ||
                mailboxLocation == Constants.MessageLocationType.LABEL_FOLDER
        );
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.search: {
                Intent intent = AppUtil.decorInAppIntent(new Intent(MailboxActivity.this, SearchActivity.class));
                startActivity(intent);
            }
            return true;
            case R.id.compose: {
                mailboxViewModel.usedSpaceActionEvent(FLOW_TRY_COMPOSE);
            }
            return true;
            case R.id.empty: {
                DialogInterface.OnClickListener clickListener = (dialog, which) -> {

                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        setRefreshing(true);
                        mJobManager.addJobInBackground(new EmptyFolderJob(mMailboxLocation.getValue(), mLabelId));
                        setLoadingMore(false);
                    }
                    dialog.dismiss();
                };
                if (!isFinishing()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.empty_folder)
                            .setMessage(R.string.are_you_sure_empty)
                            .setNegativeButton(R.string.no, clickListener)
                            .setPositiveButton(R.string.yes, clickListener)
                            .create()
                            .show();
                }
            }
            return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDrawerToggle().syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDrawerToggle().syncState();
    }

    @Override
    public void onBackPressed() {
        saveLastInteraction();
        boolean drawerClosed = closeDrawer();
        if (!drawerClosed && mMailboxLocation.getValue() != Constants.MessageLocationType.INBOX) {
            setupNewMessageLocation(Constants.DrawerOptionType.INBOX.getDrawerOptionTypeValue());
        } else if (!drawerClosed) {
            finish();
        }
    }

    private void initializeSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
        swipeRefreshLayout.setColorSchemeResources(R.color.ultramarine_blue, R.color.lake_blue);
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    void setRefreshing(boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
        mSpinnerSwipeRefreshLayout.setRefreshing(refreshing);
        mNoMessagesRefreshLayout.setRefreshing(refreshing);
        mSpinnerSwipeRefreshLayout.setVisibility(refreshing ? View.VISIBLE : View.GONE);
    }

    private boolean setLoadingMore(final boolean loadingMore) {

        boolean previousValue = mIsLoadingMore.getAndSet(loadingMore);
        mMessagesListView.post(() -> mAdapter.setIncludeFooter(mIsLoadingMore.get()));
        return previousValue;
    }

    private List<SimpleMessage> getSelectedMessages() {
        List<SimpleMessage> simpleMessages = new ArrayList<>();
        for (Message message : mAdapter.getCheckedMessages()) {
            simpleMessages.add(new SimpleMessage(message));
        }
        return simpleMessages;
    }

    @Subscribe
    public void onSwitchedAccountEvent(ForceSwitchedAccountEvent event) {
        DialogUtils.Companion.showSignedInSnack(mMessagesListView,
                String.format(getString(R.string.signed_in_with_logged_out_from), event.getFromAccount(), event.getToAccount()));
        onSwitchedAccounts();
    }

    @Subscribe
    public void onMailSettingsEvent(MailSettingsEvent event) {
        loadMailSettings();
    }

    @Override
    protected void onLogout() {
        onLogoutEvent(new LogoutEvent(Status.SUCCESS));
    }

    @Override
    protected void onInbox(Constants.DrawerOptionType type) {
        AppUtil.clearNotifications(getApplicationContext(), mUserManager.getUsername());
        setupNewMessageLocation(type.getDrawerOptionTypeValue());
    }

    @Override
    protected void onOtherMailBox(Constants.DrawerOptionType type) {
        setupNewMessageLocation(type.getDrawerOptionTypeValue());
    }

    public void onLabelMailBox(Constants.DrawerOptionType type, String labelId, String labelName, boolean isFolder) {
        setupNewMessageLocation(type.getDrawerOptionTypeValue(), labelId, labelName, isFolder);
    }

    @Override
    protected Constants.MessageLocationType getCurrentMailboxLocation() {
        return mMailboxLocation.getValue() != null ? mMailboxLocation.getValue() : Constants.MessageLocationType.INBOX;
    }

    @Override
    protected String getCurrentLabelId() {
        return mLabelId;
    }

    private void setTitle() {
        int titleRes;

        switch (mMailboxLocation.getValue()) {
            case INBOX:
                titleRes = R.string.inbox_option;
                break;
            case STARRED:
                titleRes = R.string.starred_option;
                break;
            case DRAFT:
                titleRes = R.string.drafts_option;
                break;
            case SENT:
                titleRes = R.string.sent_option;
                break;
            case ARCHIVE:
                titleRes = R.string.archive_option;
                break;
            case TRASH:
                titleRes = R.string.trash_option;
                break;
            case SPAM:
                titleRes = R.string.spam_option;
                break;
            case ALL_MAIL:
                titleRes = R.string.allmail_option;
                break;
            default:
                titleRes = R.string.app_name;
        }
        getSupportActionBar().setTitle(titleRes);
    }

    private void showNoConnSnack() {
        if (mNoConnectivitySnack == null) {
            mNoConnectivitySnack = NetworkUtil.setNoConnectionSnackLayout(mConnectivitySnackLayout, this, connectivityRetryListener, mUserManager.getUser(), this);
        }
        final SnackbarContentLayout contentLayout = (SnackbarContentLayout) ((ViewGroup) mNoConnectivitySnack.getView()).getChildAt(0);
        final TextView vvv = contentLayout.getActionView();
        mNoConnectivitySnack.show();
        if (mUserManager.getUser().getAllowSecureConnectionsViaThirdParties() && autoRetry && !isDohOngoing && !isFinishing()) {
            getWindow().getDecorView().postDelayed(vvv::callOnClick, 500);
        }
    }

    private void hideNoConnSnack() {
        if (mNoConnectivitySnack != null) {
            if (mCheckForConnectivitySnack != null) {
                mCheckForConnectivitySnack.dismiss();
            }
            mNoConnectivitySnack.dismiss();
        }
    }

    @Subscribe
    public void onAttachmentFailedEvent(AttachmentFailedEvent event) {
        TextExtensions.showToast(this, getString(R.string.attachment_failed) + " " + event.getMessageSubject() + " " + event.getAttachmentName(), Toast.LENGTH_SHORT);
    }

    @Subscribe
    public void onMailboxLoginEvent(MailboxLoginEvent event) {
        if (event == null) {
            return;
        }
        ProtonMailApplication.getApplication().resetMailboxLoginEvent();
        if (event.status == AuthStatus.INVALID_CREDENTIAL) {
            final Context context = this;
            TextExtensions.showToast(this, R.string.invalid_mailbox_password, Toast.LENGTH_SHORT);
            context.startActivity(AppUtil.decorInAppIntent(new Intent(context, MailboxLoginActivity.class)));
            finish();
        } else {
            mUserManager.setLoggedIn(true);
        }
    }

    @Subscribe
    public void onSettingsChangedEvent(SettingsChangedEvent event) {
        User user = mUserManager.getUser();
        if (event.getStatus() == AuthStatus.SUCCESS) {
            refreshDrawerHeader(user);
        } else {
            switch (event.getStatus()) {
                case INVALID_CREDENTIAL: {
                    TextExtensions.showToast(this, R.string.settings_not_saved_password, Toast.LENGTH_SHORT, Gravity.CENTER);
                }
                break;
                case INVALID_SERVER_PROOF: {
                    TextExtensions.showToast(this, R.string.invalid_server_proof, Toast.LENGTH_SHORT, Gravity.CENTER);
                }
                break;
                case FAILED:
                default: {
                    if (event.getOldEmail() != null) {
                        TextExtensions.showToast(this, R.string.settings_not_saved_email, Toast.LENGTH_SHORT, Gravity.CENTER);
                    } else {
                        TextExtensions.showToast(this, R.string.saving_failed_no_conn, Toast.LENGTH_LONG, Gravity.CENTER);
                    }
                }
                break;
            }
        }
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        if (getOverlayDialog() != null) {
            getOverlayDialog().dismiss();
            setOverlayDialog(null);
        }

        if (event.status == Status.NO_NETWORK) {
            TextExtensions.showToast(this, R.string.no_network, Toast.LENGTH_SHORT);
        }

        // destroy loader as database will be deleted on logout
        getSupportLoaderManager().destroyLoader(LOADER_ID);
        getSupportLoaderManager().destroyLoader(LOADER_ID_LABELS_OFFLINE);
        mAdapter.clear();
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
    }

    @Subscribe
    public void onMailboxLoaded(MailboxLoadedEvent event) {
        if (event == null || (event.uuid != null && !event.uuid.equals(mSyncUUID))) {
            return;
        }
        refreshMailboxJobRunning = false;
        mSyncingHandler.postDelayed(new SyncDoneRunnable(this), 1000);
        setLoadingMore(false);
        if(!isDohOngoing) {
            showToast(event.status);
        }
        final Constants.MessageLocationType mailboxLocation = this.mMailboxLocation.getValue();
        if (event.status == Status.NO_NETWORK && (mailboxLocation == Constants.MessageLocationType.LABEL || mailboxLocation == Constants.MessageLocationType.LABEL_FOLDER || mailboxLocation == Constants.MessageLocationType.LABEL_OFFLINE)) {
            this.mMailboxLocation.setValue(Constants.MessageLocationType.LABEL_OFFLINE);
        }
        mNetworkResults.setMailboxLoaded(new MailboxLoadedEvent(Status.SUCCESS, null));
    }

    @Subscribe
    public void onConnectivityEvent(ConnectivityEvent event) {
        Timber.d("onConnectivityEvent");
        if(!isDohOngoing) {
            Timber.d("DoH NOT ongoing showing UI");
            if (!event.hasConnection()) {
                Timber.d("Has connection: false");
                // mPingHasConnection = false;
                showNoConnSnack();
            } else {
                Timber.d("Has connection: true");
                hideNoConnSnack();
                if (!mPingHasConnection) {
                    setRefreshing(true);
                    fetchUpdates();
                    mPingHasConnection = true;
                }
            }
        } else {
            Timber.d("DoH ongoing, not showing UI");
        }
    }

    @Subscribe
    public void onMailboxNoMessages(MailboxNoMessagesEvent event) {
        // show toast only if user initiated load more
        mSyncingHandler.postDelayed(new SyncDoneRunnable(this), 300);
        if (mIsLoadingMore.get()) {
            TextExtensions.showToast(this, R.string.no_more_messages, Toast.LENGTH_SHORT);
            mAdapter.notifyDataSetChanged();
        }
        setLoadingMore(false);
    }

    @Subscribe
    public void onUpdatesLoaded(FetchUpdatesEvent event) {
        syncingDone();
        refreshDrawerHeader(mUserManager.getUser());
        mSyncingHandler.postDelayed(new SyncDoneRunnable(this), 1000);
    }

    private void showToast(Status status) {
        switch (status) {
            case FAILED:
            case NO_NETWORK:
                showNoConnSnack();
                break;
            case SUCCESS: {
                if (mNetworkUtil.isConnectedAndHasConnectivity()) {
                    hideNoConnSnack();
                }
            }
            break;
        }
    }

    @Subscribe
    public void onParentEvent(final ParentEvent event) {
        new OnParentEventTask(messageDetailsRepository, mAdapter, event).execute();
    }

    @Subscribe
    public void onMessageSentEvent(MessageSentEvent event) {
        super.onMessageSentEvent(event);
        mSyncUUID = UUID.randomUUID().toString();
    }

    @Subscribe
    public void onLabelsLoadedEvent(FetchLabelsEvent event) {
        if (mAdapter != null && event.status == Status.SUCCESS) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe
    public void onMessageCountsEvent(MessageCountsEvent event) {
        //region old total count
        if (event.getStatus() != Status.SUCCESS) {
            refreshDrawer();
            return;
        }
        UnreadTotalMessagesResponse response = event.getUnreadMessagesResponse();

        // TODO: should be refactored (most probably removed and put inside the job)
        if (response == null) {
            return;
        }
        final List<MessageCount> messageCountsList = response.getCounts();
        if (messageCountsList != null) {
            countersDatabase = CountersDatabaseFactory.Companion.getInstance(getApplicationContext(), mUserManager.getUsername()).getDatabase();
            new OnMessageCountsListTask(new WeakReference<>(MailboxActivity.this), countersDatabase, messageCountsList).execute();
        }

        refreshDrawer();
        //endregion
    }

    public void refreshEmptyView(int count) {
        if (mAdapter != null) {
            if (count == 0) {
                mSpinnerSwipeRefreshLayout.setVisibility(View.GONE);
                mNoMessagesRefreshLayout.setVisibility(View.VISIBLE);
                mSwipeRefreshLayout.setVisibility(View.GONE);
                mSwipeRefreshWrapper.setVisibility(View.GONE);
            } else {
                mSpinnerSwipeRefreshLayout.setVisibility(View.VISIBLE);
                mNoMessagesRefreshLayout.setVisibility(View.GONE);
                mSwipeRefreshLayout.setVisibility(View.VISIBLE);
                mSwipeRefreshWrapper.setVisibility(View.VISIBLE);
            }
        }
    }

    /* AbsListView.MultiChoiceModeListener */

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        int checkedItems = mAdapter.getCheckedMessages().size();
        // on many devices there is a strange UnknownFormatConversionException:
        // "Conversion: D" if using string formatting, which is probably a memory corruption issue
        String title = String.valueOf(checkedItems) + " " + getString(R.string.selected);
        mode.setTitle(title);

        if (checkedItems == 1) {
            mode.invalidate();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {

        mActionMode = mode;
        UiUtil.setStatusBarColor(this, UiUtil.scaleColor(getResources().getColor(R.color.dark_purple_statusbar), 1f, true));
        mode.getMenuInflater().inflate(R.menu.message_selection_menu, menu);
        menu.findItem(R.id.move_to_trash).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.delete_message).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.findItem(R.id.add_star).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.findItem(R.id.remove_star).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.findItem(R.id.mark_unread).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.findItem(R.id.mark_read).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.findItem(R.id.move_to_archive).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.findItem(R.id.add_label).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.add_folder).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        MenuItem moveToInbox = menu.findItem(R.id.move_to_inbox);
        final Constants.MessageLocationType mailboxLocation = this.mMailboxLocation.getValue();
        //TODO refactor to locations list
        if (mailboxLocation == Constants.MessageLocationType.TRASH || mailboxLocation == Constants.MessageLocationType.SPAM
                || mailboxLocation == Constants.MessageLocationType.ARCHIVE) {
            moveToInbox.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        } else {
            menu.removeItem(moveToInbox.getItemId());
        }
        menu.findItem(R.id.move_to_spam).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        mSwipeRefreshLayout.setEnabled(false);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        List<SimpleMessage> messages = getSelectedMessages();

        final Constants.MessageLocationType mailboxLocation = this.mMailboxLocation.getValue();
        if (messages.size() == 1) {
            SimpleMessage message = messages.get(0);
            menu.findItem(R.id.move_to_trash).setVisible(
                    mailboxLocation != Constants.MessageLocationType.TRASH && mailboxLocation != Constants.MessageLocationType.DRAFT);
            menu.findItem(R.id.delete_message).setVisible(
                    mailboxLocation == Constants.MessageLocationType.TRASH || mailboxLocation == Constants.MessageLocationType.DRAFT);
            menu.findItem(R.id.add_star).setVisible(!message.isStarred());
            menu.findItem(R.id.remove_star).setVisible(message.isStarred());
            menu.findItem(R.id.mark_read).setVisible(!message.isRead() && mailboxLocation != Constants.MessageLocationType.DRAFT);
            menu.findItem(R.id.mark_unread).setVisible(message.isRead() && mailboxLocation != Constants.MessageLocationType.DRAFT);
            menu.findItem(R.id.move_to_archive).setVisible(mailboxLocation != Constants.MessageLocationType.ARCHIVE);
            MenuItem moveToInbox = menu.findItem(R.id.move_to_inbox);
            if (moveToInbox != null) {
                moveToInbox.setVisible(mailboxLocation != Constants.MessageLocationType.INBOX);
            }
            menu.findItem(R.id.move_to_spam).setVisible(mailboxLocation != Constants.MessageLocationType.SPAM);
            menu.findItem(R.id.add_label).setVisible(true);
            menu.findItem(R.id.add_folder).setVisible(true);
        } else {
            menu.findItem(R.id.move_to_trash).setVisible(
                    mailboxLocation != Constants.MessageLocationType.TRASH && mailboxLocation != Constants.MessageLocationType.DRAFT);
            menu.findItem(R.id.delete_message).setVisible(
                    mailboxLocation == Constants.MessageLocationType.TRASH || mailboxLocation == Constants.MessageLocationType.DRAFT);
            if (containsUnstar(messages)) menu.findItem(R.id.add_star).setVisible(true);
            if (containsStar(messages)) menu.findItem(R.id.remove_star).setVisible(true);
            MenuItem markReadItem = menu.findItem(R.id.mark_read);
            if (MessageUtils.areAllRead(messages)) {
                markReadItem.setVisible(false);
            } else {
                markReadItem.setVisible(mailboxLocation != Constants.MessageLocationType.DRAFT);
            }
            MenuItem markUnreadItem = menu.findItem(R.id.mark_unread);
            if (MessageUtils.areAllUnRead(messages)) {
                markUnreadItem.setVisible(false);
            } else {
                markUnreadItem.setVisible(mailboxLocation != Constants.MessageLocationType.DRAFT);
            }
            menu.findItem(R.id.move_to_archive).setVisible(mailboxLocation != Constants.MessageLocationType.ARCHIVE);
            MenuItem moveToInbox = menu.findItem(R.id.move_to_inbox);
            if (moveToInbox != null) {
                moveToInbox.setVisible(mailboxLocation != Constants.MessageLocationType.INBOX);
            }
            menu.findItem(R.id.move_to_spam).setVisible(mailboxLocation != Constants.MessageLocationType.SPAM);
            menu.findItem(R.id.add_label).setVisible(true);
            menu.findItem(R.id.add_folder).setVisible(true);
        }
        return true;
    }

    private boolean containsStar(List<SimpleMessage> messages) {
        for (SimpleMessage message : messages) {
            if (message.isStarred()) return true;
        }
        return false;
    }

    private boolean containsUnstar(List<SimpleMessage> messages) {
        for (SimpleMessage message : messages) {
            if (!message.isStarred())
                return true;
        }
        return false;
    }

    @Override
    public void move(String folderId) {
        MessageUtils.moveMessage(this, mJobManager, folderId, Arrays.asList(mLabelId), getSelectedMessages());

        if (actionModeRunnable != null) {
            actionModeRunnable.run();
        }
    }

    @Override
    public void showFoldersManager() {
        Intent foldersManagerIntent = new Intent(this, LabelsManagerActivity.class);
        foldersManagerIntent.putExtra(EXTRA_MANAGE_FOLDERS, true);
        foldersManagerIntent.putExtra(EXTRA_POPUP_STYLE, true);
        foldersManagerIntent.putExtra(EXTRA_CREATE_ONLY, true);
        startActivity(AppUtil.decorInAppIntent(foldersManagerIntent));
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mCatchLabelEvents = true;
    }

    class ActionModeInteractionRunnable implements Runnable {
        private final ActionMode actionMode;

        ActionModeInteractionRunnable(ActionMode actionMode) {
            this.actionMode = actionMode;
        }

        @Override
        public void run() {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    private ActionModeInteractionRunnable actionModeRunnable;

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        List<SimpleMessage> selectedMessages = getSelectedMessages();
        List<String> messageIds = new ArrayList<>();
        for (SimpleMessage message : selectedMessages) {
            messageIds.add(message.getMessageId());
        }
        int menuItemId = menuItem.getItemId();
        Job job = null;

        switch (menuItemId) {
            case R.id.move_to_trash:
                job = new PostTrashJobV2(messageIds, mLabelId);
                undoSnack = DialogUtils.Companion.showUndoSnackbar(MailboxActivity.this, findViewById(R.id.drawer_layout),
                        getResources().getQuantityString(R.plurals.action_move_to_trash, messageIds.size()), unit -> unit, false);
                undoSnack.show();
                break;
            case R.id.delete_message:
                DialogUtils.Companion.showDeleteConfirmationDialog(
                        this, getString(R.string.delete_messages),
                        getString(R.string.confirm_destructive_action), unit -> {
                            mJobManager.addJobInBackground(new PostDeleteJob(messageIds));
                            mode.finish();
                            return unit;
                        });
                break;
            case R.id.mark_read:
                job = new PostReadJob(messageIds);
                break;
            case R.id.mark_unread:
                job = new PostUnreadJob(messageIds);
                break;
            case R.id.add_star:
                job = new PostStarJob(messageIds);
                break;
            case R.id.add_label:
                actionModeRunnable = new ActionModeInteractionRunnable(mode);
                new ShowLabelsManagerDialogTask(getSupportFragmentManager(), messageDetailsRepository, messageIds).execute();
                break;
            case R.id.add_folder:
                actionModeRunnable = new ActionModeInteractionRunnable(mode);
                showFoldersManagerDialog(messageIds);
                break;
            case R.id.remove_star:
                job = new PostUnstarJob(messageIds);
                break;
            case R.id.move_to_archive:
                job = new PostArchiveJob(messageIds);
                undoSnack = DialogUtils.Companion.showUndoSnackbar(MailboxActivity.this, findViewById(R.id.drawer_layout),
                        getResources().getQuantityString(R.plurals.action_move_to_archive, messageIds.size()), unit -> unit, false);
                undoSnack.show();
                break;
            case R.id.move_to_inbox:
                job = new PostInboxJob(messageIds, Arrays.asList(mLabelId));
                break;
            case R.id.move_to_spam:
                job = new PostSpamJob(messageIds);
                undoSnack = DialogUtils.Companion.showUndoSnackbar(MailboxActivity.this, findViewById(R.id.drawer_layout),
                        getResources().getQuantityString(R.plurals.action_move_to_spam, messageIds.size()), unit -> unit, false);
                undoSnack.show();
                break;
        }
        if (job != null) {

            //show progress bar for visual representation of work in background,
            // if all the messages inside the folder are impacted by the action
            if( mAdapter.getItemCount() == messageIds.size()){
                setRefreshing(true);
            }

            mJobManager.addJobInBackground(job);
        }

        //TODO refactor to list
        if (menuItemId != R.id.add_label && menuItemId != R.id.add_folder && menuItemId != R.id.delete_message) {
            mode.finish();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        mSwipeRefreshLayout.setEnabled(true);
        mAdapter.endSelectionMode();
        UiUtil.setStatusBarColor(this, getResources().getColor(R.color.dark_purple_statusbar));
    }

    /* END AbsListView.MultiChoiceModeListener */

    private void showFoldersManagerDialog(List<String> messageIds) {
        //show progress bar for visual representation of work in background,
        // if all the messages inside the folder are impacted by the action
        if( mAdapter.getItemCount() == messageIds.size()){
            setRefreshing(true);
        }
        mCatchLabelEvents = false;
        MoveToFolderDialogFragment moveToFolderDialogFragment = MoveToFolderDialogFragment.newInstance(mMailboxLocation.getValue());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(moveToFolderDialogFragment, moveToFolderDialogFragment.getFragmentKey());
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void onLabelCreated(String labelName, String color) {
        mJobManager.addJobInBackground(new PostLabelJob(labelName, color, 0, 0, false, null));
    }

    @Override
    public void onLabelsDeleted(List<String> checkedLabelIds) {
        // NOOP
    }

    @Override
    public void onLabelsChecked(final List<String> checkedLabelIds, List<String> unchangedLabels, List<String> messageIds) {
        if (actionModeRunnable != null) {
            actionModeRunnable.run();
        }
        if (unchangedLabels == null) {
            unchangedLabels = new ArrayList<>();
        }
        mailboxViewModel.processLabels(messageIds, checkedLabelIds, unchangedLabels);
    }

    @Subscribe
    public void onLabelAddedEvent(LabelAddedEvent event) {
        if (!mCatchLabelEvents) {
            return;
        }
        String message;
        if (event.getStatus() == Status.SUCCESS) {
            message = getString(R.string.label_created);
        } else {
            if (TextUtils.isEmpty(event.getError())) {
                message = getString(R.string.label_invalid);
            } else {
                message = event.getError();
            }
        }
        if (!TextUtils.isEmpty(message)) {
            TextExtensions.showToast(this, message, Toast.LENGTH_SHORT);
        }
    }

    @Subscribe
    public void onRefreshDrawer(RefreshDrawerEvent event) {
        refreshDrawer();
    }

    @Subscribe
    public void onUserInfoEvent(UserInfoEvent userInfoEvent) {
        super.onUserInfoEvent(userInfoEvent);
    }

    @Subscribe
    public void onMessagesDeletedEvent(MessageDeletedEvent event) {
        if (!event.getNotDeletedMessages().isEmpty()) {
            TextExtensions.showToast(this, R.string.message_deleted_error);
        }
    }

    @Override
    public void onLabelsChecked(List<String> checkedLabelIds, List<String> unchangedLabels, List<String> messageIds, List<String> messagesToArchive) {
        mJobManager.addJobInBackground(new PostArchiveJob(messagesToArchive));
        onLabelsChecked(checkedLabelIds, unchangedLabels, messageIds);
    }

    /* SwipeRefreshLayout.OnRefreshListener */

    @Override
    public void onRefresh() {
        if (!mSpinnerSwipeRefreshLayout.isRefreshing()) {
            setRefreshing(true);
            fetchUpdates();
        }
    }

    private void fetchUpdates() {
        mSyncUUID = UUID.randomUUID().toString();
        reloadMessageCounts();
        mJobManager.addJobInBackground(new FetchByLocationJob(mMailboxLocation.getValue(), mLabelId, true, mSyncUUID));
    }
    /* END SwipeRefreshLayout.OnRefreshListener */

    private final Handler mSyncingHandler = new Handler();

    private void syncingDone() {
        mSyncView.setVisibility(View.GONE);
    }

    private static class SyncDoneRunnable implements Runnable {
        private final WeakReference<MailboxActivity> mailboxActivityWeakReference;

        SyncDoneRunnable(MailboxActivity activity) {
            mailboxActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            MailboxActivity mailboxActivity = mailboxActivityWeakReference.get();
            if (mailboxActivity != null) {
                mailboxActivity.syncingDone();
            }
        }
    }

    public void setupNewMessageLocation(int newLocation) {
        Constants.MessageLocationType newMessageLocationType = Constants.MessageLocationType.Companion.fromInt(newLocation);
        mSwipeRefreshLayout.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshWrapper.setVisibility(View.VISIBLE);
        getSupportLoaderManager().destroyLoader(LOADER_ID_LABELS_OFFLINE);
        mNoMessagesRefreshLayout.setVisibility(View.GONE);
        mSpinnerSwipeRefreshLayout.setVisibility(View.VISIBLE);
        if (mActionMode != null) {
            mActionMode.finish();
        }
        mLabelId = null;
        invalidateOptionsMenu();
        mMailboxLocation.setValue(newMessageLocationType);
        setTitle();
        closeDrawer();
        mMessagesListView.clearFocus();
        mMessagesListView.scrollToPosition(0);
        if (newMessageLocationType == Constants.MessageLocationType.STARRED) {
            MessagesService.Companion.startFetchFirstPage(newMessageLocationType);
        } else {
            mSyncUUID = UUID.randomUUID().toString();
            mJobManager.addJobInBackground(new FetchByLocationJob(newMessageLocationType, mLabelId, false, mSyncUUID));
        }
        checkForDraftedMessages();
        new RefreshEmptyViewTask(new WeakReference<>(this), countersDatabase, getMessagesDatabase(), newMessageLocationType, mLabelId).execute();
        if (newMessageLocationType == Constants.MessageLocationType.ALL_DRAFT || newMessageLocationType == Constants.MessageLocationType.DRAFT) {
            AppUtil.clearSendingFailedNotifications(this, mUserManager.getUsername());
        }
    }

    //version for label views
    public void setupNewMessageLocation(final int newLocation, final String labelId,
                                        final String labelName, final boolean isFolder) {
        new SetUpNewMessageLocationTask(new WeakReference<>(MailboxActivity.this), MailboxActivity.this.messageDetailsRepository, labelId, isFolder, newLocation, labelName).execute();
    }

    private Snackbar undoSnack;

    private void buildSwipeProcessor() {
        mSwipeProcessor.addHandler(SwipeAction.TRASH, new TrashSwipeHandler());
        mSwipeProcessor.addHandler(SwipeAction.SPAM, new SpamSwipeHandler());
        mSwipeProcessor.addHandler(SwipeAction.STAR, new StarSwipeHandler());
        mSwipeProcessor.addHandler(SwipeAction.ARCHIVE, new ArchiveSwipeHandler());
        mSwipeProcessor.addHandler(SwipeAction.MARK_READ, new MarkReadSwipeHandler());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_TRASH_MESSAGE_DETAILS:
                    mMoveToTrashView.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        if (mMoveToTrashView != null) {
                            mMoveToTrashView.setVisibility(View.GONE);
                        }
                    }, 1000);
                    break;
                case REQUEST_CODE_VALIDATE_PIN:
                    if (data.hasExtra(EXTRA_TOTAL_COUNT_EVENT)) {
                        Object totalCountEvent = data.getSerializableExtra(
                                EXTRA_TOTAL_COUNT_EVENT);
                        if (totalCountEvent instanceof MessageCountsEvent) {
                            onMessageCountsEvent((MessageCountsEvent) totalCountEvent);
                        }
                    }
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
                case REQUEST_CODE_SWITCHED_USER:
                    if (data.hasExtra(EXTRA_SWITCHED_USER)) {
                        onSwitchedAccounts();
                    }
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                if (result == ConnectionResult.SERVICE_MISSING || result == ConnectionResult.SERVICE_INVALID || result == ConnectionResult.SERVICE_DISABLED || result == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
                    final SharedPreferences prefs = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
                    boolean dontShowPlaySrvices = prefs.getBoolean(Constants.Prefs.PREF_DONT_SHOW_PLAY_SERVICES, false);
                    if (!dontShowPlaySrvices) {
                        DialogUtils.Companion.showInfoDialogWithTwoButtons(MailboxActivity.this,
                                getString(R.string.push_notifications_alert_title),
                                getString(R.string.push_notifications_alert_subtitle),
                                getString(R.string.dont_remind_again), getString(R.string.okay), unit -> {
                                    prefs.edit().putBoolean(Constants.Prefs.PREF_DONT_SHOW_PLAY_SERVICES, true).apply();
                                    return unit;
                                }, unit -> unit,
                                true);
                    }
                } else {
                    googleAPI.getErrorDialog(this, result, PLAY_SERVICES_RESOLUTION_REQUEST, dialog -> TextExtensions.showToast(MailboxActivity.this, "cancel", Toast.LENGTH_SHORT)).show();
                }
            } else {
                Timber.d("%s: This device is not GCM supported.", TAG_MAILBOX_ACTIVITY);
            }
            return false;
        }
        return true;
    }

    private void checkForDraftedMessages() {
        Constants.MessageLocationType mailboxLocation = mMailboxLocation.getValue();
        if (mailboxLocation == Constants.MessageLocationType.ALL_DRAFT  || mailboxLocation == Constants.MessageLocationType.DRAFT
                && mDraftedMessageSnack != null) {
            mDraftedMessageSnack.dismiss();
        }
        registerReceiver(showDraftedSnackBroadcastReceiver, new IntentFilter(ACTION_MESSAGE_DRAFTED));
    }

    private void showDraftedSnack(Intent intent) {
        String errorText = getString(R.string.message_drafted);
        if (intent.hasExtra(Constants.ERROR)) {
            String extraText = intent.getStringExtra(Constants.ERROR);
            if (!TextUtils.isEmpty(extraText)) {
                errorText = extraText;
            }
        }
        mDraftedMessageSnack = Snackbar.make(mConnectivitySnackLayout, errorText, Snackbar.LENGTH_INDEFINITE);
        View view = mDraftedMessageSnack.getView();
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        mDraftedMessageSnack.setAction(getString(R.string.dismiss), v -> {
            mDraftedMessageSnack.dismiss();
        });
        mDraftedMessageSnack.setActionTextColor(getResources().getColor(R.color.icon_purple));
        mDraftedMessageSnack.show();
    }

    private BroadcastReceiver showDraftedSnackBroadcastReceiver = new ShowDraftedSnackBroadcastReceiver();

    final BroadcastReceiver gcmBroadcastReceiver = new GcmBroadcastReceiver();

    private static class FetchMessagesRetryRunnable implements Runnable {
        // non leaky runnable
        private final WeakReference<MailboxActivity> mailboxActivityWeakReference;

        FetchMessagesRetryRunnable(MailboxActivity activity) {
            mailboxActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            MailboxActivity mailboxActivity = mailboxActivityWeakReference.get();
            if (mailboxActivity != null) {
                mailboxActivity.mJobManager.addJobInBackground(
                        new FetchByLocationJob(mailboxActivity.mMailboxLocation.getValue(), mailboxActivity.mLabelId,
                                true,
                                mailboxActivity.mSyncUUID));
            }
        }
    }

    private static class OnMessageClickTask extends AsyncTask<Void, Void, Message> {
        private final WeakReference<MailboxActivity> mailboxActivity;
        private final MessageDetailsRepository messageDetailsRepository;
        private final Message message;

        OnMessageClickTask(WeakReference<MailboxActivity> mailboxActivity, MessageDetailsRepository messageDetailsRepository, Message message) {
            this.mailboxActivity = mailboxActivity;
            this.messageDetailsRepository = messageDetailsRepository;
            this.message = message;
        }

        @Override
        protected Message doInBackground(Void... voids) {
            return messageDetailsRepository.findMessageById(message.getMessageId());
        }

        @Override
        public void onPostExecute(@Nullable Message savedMessage) {
            MailboxActivity mailboxActivity = this.mailboxActivity.get();
            if (savedMessage != null) {
                message.setInline(savedMessage.isInline());
            }
            Constants.MessageLocationType messageLocation = message.locationFromLabel();
            if (messageLocation == Constants.MessageLocationType.DRAFT || messageLocation == Constants.MessageLocationType.ALL_DRAFT) {
                new TryToOpenMessageTask(this.mailboxActivity, mailboxActivity.pendingActionsDatabase,
                        message.getMessageId(), message.isInline(), message.getAddressID()).execute();
            } else {
                Intent intent = AppUtil.decorInAppIntent(new Intent(mailboxActivity, MessageDetailsActivity.class));
                if (!TextUtils.isEmpty(mailboxActivity.mLabelId)) {
                    intent.putExtra(MessageDetailsActivity.EXTRA_TRANSIENT_MESSAGE, false);
                }
                intent.putExtra(MessageDetailsActivity.EXTRA_MESSAGE_ID, message.getMessageId());
                mailboxActivity.startActivityForResult(intent, REQUEST_CODE_TRASH_MESSAGE_DETAILS);
            }
        }
    }

    private static class TryToOpenMessageTask extends AsyncTask<Void, Void, Boolean> {

        private final WeakReference<MailboxActivity> mailboxActivity;
        private final PendingActionsDatabase pendingActionsDatabase;
        private final String messageId;
        private final boolean isInline;
        private final String addressId;

        TryToOpenMessageTask(WeakReference<MailboxActivity> mailboxActivity,
                             PendingActionsDatabase pendingActionsDatabase, String messageId,
                             boolean isInline, String addressId) {
            this.mailboxActivity = mailboxActivity;
            this.pendingActionsDatabase = pendingActionsDatabase;
            this.messageId = messageId;
            this.isInline = isInline;
            this.addressId = addressId;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            // return if message is not in sending process and can be opened
            PendingUpload pendingUploads = pendingActionsDatabase.findPendingUploadByMessageId(messageId);
            PendingSend pendingForSending = pendingActionsDatabase.findPendingSendByMessageId(messageId);
            return pendingUploads == null && (pendingForSending == null || (pendingForSending.getSent() != null && !pendingForSending.getSent()));
        }

        @Override
        protected void onPostExecute(Boolean openMessage) {
            MailboxActivity mailboxActivity = this.mailboxActivity.get();
            if (!openMessage) {
                TextExtensions.showToast(mailboxActivity, R.string.draft_attachments_uploading, Toast.LENGTH_SHORT);
                return;
            }

            Intent intent = AppUtil.decorInAppIntent(new Intent(mailboxActivity, ComposeMessageActivity.class));
            intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, messageId);
            intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_RESPONSE_INLINE, isInline);
            intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_ID, addressId);
            mailboxActivity.startActivityForResult(intent, REQUEST_CODE_COMPOSE_MESSAGE);
        }
    }

    private static class SetUpNewMessageLocationTask extends AsyncTask<Void, Void, Label> {

        private final WeakReference<MailboxActivity> mailboxActivity;
        private final MessageDetailsRepository messageDetailsRepository;
        private final String labelId;
        private final boolean isFolder;
        private final int newLocation;
        private final String labelName;

        SetUpNewMessageLocationTask(WeakReference<MailboxActivity> mailboxActivity,
                                    MessageDetailsRepository messageDetailsRepository, String labelId,
                                    boolean isFolder, int newLocation, String labelName) {
            this.mailboxActivity = mailboxActivity;
            this.messageDetailsRepository = messageDetailsRepository;
            this.labelId = labelId;
            this.isFolder = isFolder;
            this.newLocation = newLocation;
            this.labelName = labelName;
        }

        @Override
        protected Label doInBackground(Void... voids) {
            List<Label> labels = messageDetailsRepository.findAllLabelsWithIds(Collections.singletonList(labelId));
            return labels.isEmpty() ? null : labels.get(0);
        }

        @Override
        protected void onPostExecute(Label label) {
            MailboxActivity mailboxActivity = this.mailboxActivity.get();
            if (mailboxActivity == null) {
                return;
            }

            mailboxActivity.mSwipeRefreshLayout.setVisibility(View.VISIBLE);
            mailboxActivity.mSwipeRefreshWrapper.setVisibility(View.VISIBLE);
            mailboxActivity.mSwipeRefreshLayout.setRefreshing(true);
            mailboxActivity.mNoMessagesRefreshLayout.setVisibility(View.GONE);
            mailboxActivity.mSpinnerSwipeRefreshLayout.setVisibility(View.VISIBLE);
            if (mailboxActivity.mActionMode != null) {
                mailboxActivity.mActionMode.finish();
            }
            mailboxActivity.invalidateOptionsMenu();
            Constants.MessageLocationType locationToSet;
            if (isFolder) {
                locationToSet = Constants.MessageLocationType.LABEL_FOLDER;
            } else {
                locationToSet = Constants.MessageLocationType.Companion.fromInt(newLocation);
            }
            mailboxActivity.mLabelId = labelId;
            mailboxActivity.mLabelName = labelName;
            mailboxActivity.mMailboxLocation.setValue(locationToSet);
            if (label != null) {
                ActionBar actionBar = mailboxActivity.getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(label.getName());
                }
            }
            mailboxActivity.closeDrawer();
            mailboxActivity.mMessagesListView.scrollToPosition(0);
            MessagesService.Companion.startFetchFirstPageByLabel(Constants.MessageLocationType.Companion.fromInt(newLocation), labelId);
            new RefreshEmptyViewTask(this.mailboxActivity, mailboxActivity.countersDatabase,
                    mailboxActivity.getMessagesDatabase(), locationToSet, labelId).execute();
        }
    }

    private class GcmBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
            String messageType = gcm.getMessageType(intent);

            if (!GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType) &&
                    !GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                mSyncUUID = UUID.randomUUID().toString();
                checkUserAndFetchNews();
                if (((LinearLayoutManager) mMessagesListView.getLayoutManager()).findFirstVisibleItemPosition() > 1) {
                    new Handler().postDelayed(() -> {
                        final Snackbar newMessageSnack = Snackbar.make(findViewById(R.id.drawer_layout),
                                getString(R.string.new_message_arrived),
                                Snackbar.LENGTH_LONG);
                        View view = newMessageSnack.getView();
                        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextColor(Color.WHITE);
                        newMessageSnack.show();

                    }, 750);
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    private class SwipeController extends ItemTouchHelper.Callback {
        private final User user;

        SwipeController(User user) {
            this.user = user;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder) {

            if (viewHolder instanceof MessagesListViewHolder.MessageViewHolder) {
                Constants.MessageLocationType mailboxLocation = mMailboxLocation.getValue();
                if (mMailboxLocation.getValue() != null && mailboxLocation == Constants.MessageLocationType.DRAFT ||
                        mailboxLocation == Constants.MessageLocationType.ALL_DRAFT) {
                    return makeMovementFlags(0, 0);
                } else {
                    return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                }
            }
            return makeMovementFlags(0, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            throw new UnsupportedOperationException("Not implemented");
        }

        SwipeAction normalise(SwipeAction swipeAction, Constants.MessageLocationType mailboxLocation) {
            if (mailboxLocation == Constants.MessageLocationType.DRAFT || mailboxLocation == Constants.MessageLocationType.ALL_DRAFT && swipeAction != SwipeAction.STAR) {
                return SwipeAction.TRASH;
            }
            return swipeAction;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();

            Message swipedItem = mAdapter.getItem(position);
            final SimpleMessage messageSwiped = new SimpleMessage(swipedItem);
            final Constants.MessageLocationType mailboxLocation = mMailboxLocation.getValue();
            final int swipeActionOrdinal;
            switch (direction) {
                case ItemTouchHelper.RIGHT:
                    swipeActionOrdinal = mUserManager.getMailSettings().getRightSwipeAction();
                    break;
                case ItemTouchHelper.LEFT:
                    swipeActionOrdinal = mUserManager.getMailSettings().getLeftSwipeAction();
                    break;
                default:
                    throw new RuntimeException("Unrecognised direction: " + direction);
            }

            final SwipeAction swipeAction = normalise(SwipeAction.values()[swipeActionOrdinal],
                    mMailboxLocation.getValue());

            mSwipeProcessor.handleSwipe(swipeAction, messageSwiped, mJobManager, mLabelId);
            if (swipeAction != null) {
                if (undoSnack != null && undoSnack.isShownOrQueued()) {
                    undoSnack.dismiss();
                }

                undoSnack = DialogUtils.Companion.showUndoSnackbar(MailboxActivity.this, findViewById(R.id.drawer_layout), getString(swipeAction.getActionDescription()), unit -> {
                    mSwipeProcessor.handleUndo(swipeAction, messageSwiped, mJobManager, mailboxLocation, mLabelId);
                    mAdapter.notifyDataSetChanged();
                    return unit;
                }, true);

                if (!(swipeAction == SwipeAction.TRASH && mMailboxLocation.getValue() == Constants.MessageLocationType.DRAFT)) {
                    undoSnack.show();
                }
                if (swipeCustomizeSnack != null && !mCustomizeSwipeSnackShown) {
                    new Handler().postDelayed(() -> {
                        swipeCustomizeSnack.show();
                        mCustomizeSwipeSnackShown = true;
                    }, 2750);
                }
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder, float deltaX, float deltaY,
                                int actionState, boolean isCurrentlyActive) {

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

                View itemView = viewHolder.itemView;
                int height = itemView.getBottom() - itemView.getTop();
                int width = itemView.getRight() - itemView.getLeft();
                final int layoutId;
                if (mMailboxLocation.getValue() == Constants.MessageLocationType.DRAFT) {
                    layoutId = SwipeAction.TRASH.getActionBackgroundResource(deltaX < 0);
                } else if (deltaX < 0) {
                    layoutId = SwipeAction.values()[mUserManager.getMailSettings().getLeftSwipeAction()].getActionBackgroundResource(
                            false);
                } else {
                    layoutId = SwipeAction.values()[mUserManager.getMailSettings().getRightSwipeAction()].getActionBackgroundResource(
                            true);
                }
                View view = getLayoutInflater().inflate(layoutId, null);
                int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
                view.measure(widthSpec, heightSpec);

                view.layout(0, 0, width, height);
                canvas.save();
                canvas.translate(itemView.getLeft(), itemView.getTop());
                view.draw(canvas);
                canvas.restore();
            }
            super.onChildDraw(canvas, recyclerView, viewHolder, deltaX, deltaY, actionState,
                    isCurrentlyActive);
        }
    }

    private class ShowDraftedSnackBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                showDraftedSnack(intent);
            }
        }
    }

    private static class OnMessageCountsListTask extends AsyncTask<Void, Void, Integer> {
        private final WeakReference<MailboxActivity> mailboxActivity;
        private final CountersDatabase countersDatabase;
        private final List<MessageCount> messageCountsList;

        OnMessageCountsListTask(WeakReference<MailboxActivity> mailboxActivity,
                                CountersDatabase countersDatabase,
                                List<MessageCount> messageCountsList) {
            this.mailboxActivity = mailboxActivity;
            this.countersDatabase = countersDatabase;
            this.messageCountsList = messageCountsList;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            TotalLocationCounter totalInbox = countersDatabase.findTotalLocationById(Constants.MessageLocationType.INBOX.getMessageLocationTypeValue());
            if (totalInbox != null) {
                return totalInbox.getCount();
            }
            return -1;
        }

        @Override
        protected void onPostExecute(Integer inboxMessagesCount) {
            MailboxActivity mailboxActivity = this.mailboxActivity.get();
            if (mailboxActivity == null) {
                return;
            }
            boolean foundMailbox = false;
            final List<TotalLocationCounter> locationCounters = new ArrayList<>();
            final List<TotalLabelCounter> labelCounters = new ArrayList<>();
            for (MessageCount messageCount : messageCountsList) {
                String labelId = messageCount.getLabelId();
                int total = messageCount.getTotal();
                if (labelId.length() <= 2) {
                    Constants.MessageLocationType location = Constants.MessageLocationType.Companion.fromInt(Integer.valueOf(labelId));
                    if (location == Constants.MessageLocationType.INBOX && inboxMessagesCount != -1 && total != inboxMessagesCount && !mailboxActivity.refreshMailboxJobRunning) {
                        mailboxActivity.checkUserAndFetchNews();
                    }
                    if (mailboxActivity.mMailboxLocation.getValue() == location) {
                        mailboxActivity.refreshEmptyView(total);
                        foundMailbox = true;
                    }
                    locationCounters.add(new TotalLocationCounter(location.getMessageLocationTypeValue(), total));
                } else {
                    // label
                    if (labelId.equals(mailboxActivity.mLabelId)) {
                        mailboxActivity.refreshEmptyView(total);
                        foundMailbox = true;
                    }
                    if (!foundMailbox) {
                        mailboxActivity.refreshEmptyView(0);
                    }
                    labelCounters.add(new TotalLabelCounter(labelId, total));
                }
            }
            mailboxActivity.setRefreshing(false);
            new RefreshTotalCountersTask(countersDatabase, locationCounters, labelCounters).execute();
        }
    }
}
