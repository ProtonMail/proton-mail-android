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
package ch.protonmail.android.activities.messageDetails;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.print.PrintManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.birbit.android.jobqueue.Job;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.otto.Subscribe;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.BaseActivity;
import ch.protonmail.android.activities.BaseStoragePermissionActivity;
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity;
import ch.protonmail.android.activities.dialogs.ManageLabelsDialogFragment;
import ch.protonmail.android.activities.dialogs.MoveToFolderDialogFragment;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity;
import ch.protonmail.android.activities.mailbox.MailboxActivity;
import ch.protonmail.android.activities.messageDetails.attachments.MessageDetailsAttachmentListAdapter;
import ch.protonmail.android.activities.messageDetails.attachments.OnAttachmentDownloadCallback;
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel;
import ch.protonmail.android.api.models.SimpleMessage;
import ch.protonmail.android.api.models.room.messages.Attachment;
import ch.protonmail.android.api.models.room.messages.LocalAttachment;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.models.room.pendingActions.PendingSend;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.events.AttachmentFailedEvent;
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent;
import ch.protonmail.android.events.DownloadedAttachmentEvent;
import ch.protonmail.android.events.LabelAddedEvent;
import ch.protonmail.android.events.LogoutEvent;
import ch.protonmail.android.events.MessageSentEvent;
import ch.protonmail.android.events.PostPhishingReportEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.user.MailSettingsEvent;
import ch.protonmail.android.jobs.PostArchiveJob;
import ch.protonmail.android.jobs.PostDeleteJob;
import ch.protonmail.android.jobs.PostInboxJob;
import ch.protonmail.android.jobs.PostLabelJob;
import ch.protonmail.android.jobs.PostSpamJob;
import ch.protonmail.android.jobs.PostTrashJobV2;
import ch.protonmail.android.jobs.PostUnreadJob;
import ch.protonmail.android.jobs.ReportPhishingJob;
import ch.protonmail.android.events.MoveToFolderEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.CustomLocale;
import ch.protonmail.android.utils.DownloadUtils;
import ch.protonmail.android.utils.Event;
import ch.protonmail.android.utils.HTMLTransformer.AbstractTransformer;
import ch.protonmail.android.utils.HTMLTransformer.DefaultTransformer;
import ch.protonmail.android.utils.HTMLTransformer.Transformer;
import ch.protonmail.android.utils.HTMLTransformer.ViewportTransformer;
import ch.protonmail.android.utils.MessageUtils;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.UserUtils;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;
import ch.protonmail.android.views.PMWebViewClient;
import ch.protonmail.android.views.messageDetails.ReplyButtonsPanelView;
import dagger.android.AndroidInjection;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import timber.log.Timber;

import static ch.protonmail.android.activities.NavigationActivityKt.EXTRA_SWITCHED_USER;
import static ch.protonmail.android.activities.labelsManager.LabelsManagerActivityKt.EXTRA_CREATE_ONLY;
import static ch.protonmail.android.activities.labelsManager.LabelsManagerActivityKt.EXTRA_MANAGE_FOLDERS;
import static ch.protonmail.android.activities.labelsManager.LabelsManagerActivityKt.EXTRA_POPUP_STYLE;
import static ch.protonmail.android.activities.messageDetails.MessageViewHeadersActivityKt.EXTRA_VIEW_HEADERS;
import static ch.protonmail.android.utils.ui.ExpandableRecyclerAdapterKt.MODE_ACCORDION;

public class MessageDetailsActivity extends BaseStoragePermissionActivity implements ManageLabelsDialogFragment.ILabelCreationListener,
        ManageLabelsDialogFragment.ILabelsChangeListener, MoveToFolderDialogFragment.IMoveMessagesListener {

    public static final String EXTRA_MESSAGE_ID = "messageId";
    // if transient is true it will not save Message object to db
    public static final String EXTRA_TRANSIENT_MESSAGE = "transient_message";
    public static final String EXTRA_MESSAGE_RECIPIENT_USERNAME = "message_recipient_username";

    // region views
    @BindView(R.id.progress)
    View mProgressView;
    @BindView(R.id.wv_scroll)
    RecyclerView mWvScrollView;
    @BindView(R.id.action_buttons)
    ReplyButtonsPanelView replyButtonsPanelView;
    @BindView(R.id.coordinatorLayout)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.screenShotPreventer)
    View screenshotProtector;

    private PMWebViewClient pmWebViewClient;

    private boolean mMarkAsRead;
    private boolean showPhishingReportButton = true;

    private MessageDetailsViewModel viewModel;
    @Inject
    MessageDetailsViewModel.Factory factory;
    MessageDetailsAttachmentListAdapter attachmentsListAdapter;

    /**
     * The id of the current message
     */
    private String messageId;

    /**
     * Whether the current message needs to be store in database. If transient if won't be stored
     */
    private boolean isTransientMessage;
    private String messageRecipientUsername;

    private Handler buttonsVisibilityHandler = new Handler();

    private MessageDetailsAdapter messageExpandableAdapter = null;
    private OnAttachmentDownloadCallback downloadListener;
    private AtomicReference<String> mAttachmentToDownloadId = new AtomicReference<>(null);

    @Override
    protected int getLayoutId() {
        return R.layout.activity_message_details;
    }

    Runnable buttonsVisibilityRunnable = () -> replyButtonsPanelView.setVisibility(View.VISIBLE);

    @Override
    protected void storagePermissionGranted() {
        String attachmentToDownloadId = this.mAttachmentToDownloadId.getAndSet(null);
        if (TextUtils.isEmpty(attachmentToDownloadId)) {
            return;
        }
        viewModel.tryDownloadingAttachment(MessageDetailsActivity.this, attachmentToDownloadId, messageId);
    }

    @Override
    protected boolean checkForPermissionOnStartup() {
        return false;
    }


    private Context primaryBaseActivity;

    @Override
    protected void attachBaseContext(Context base) {
        primaryBaseActivity = base;
        super.attachBaseContext(CustomLocale.Companion.apply(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        if (mUserManager.isFirstMessageDetails()) {
            mUserManager.firstMessageDetailsDone();
        }

        mMarkAsRead = true;
        Intent intent = getIntent();
        messageId = intent.getStringExtra(EXTRA_MESSAGE_ID);
        messageRecipientUsername = intent.getStringExtra(EXTRA_MESSAGE_RECIPIENT_USERNAME);
        String currentAccountUsername = mUserManager.getUsername();
        isTransientMessage = intent.getBooleanExtra(EXTRA_TRANSIENT_MESSAGE, false);
        AppUtil.clearNotifications(this);
        if (!mUserManager.isLoggedIn()){
            startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        }
        loadMailSettings();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(null);
        }

        if (messageRecipientUsername != null && !currentAccountUsername.equals(messageRecipientUsername)) {
            DialogUtils.Companion.showInfoDialogWithTwoButtons(this, getString(R.string.switch_accounts_question),
                    String.format(getString(R.string.switch_to_account), messageRecipientUsername),
                    getString(R.string.cancel), getString(R.string.okay),
                    unit -> {
                        finish();
                        return unit;
                    },
                    unit -> {
                        mUserManager.switchToAccount(messageRecipientUsername);
                        onCreate();
                        invalidateOptionsMenu();
                        mApp.getBus().register(viewModel);
                        DialogUtils.Companion.showSignedInSnack(coordinatorLayout, String.format(getString(R.string.signed_in_with), messageRecipientUsername));
                        return unit;
                    }, false);
        } else {
            onCreate();
        }

    }

    private void onCreate() {
        downloadListener = new OnAttachmentDownloadCallback(MessageDetailsActivity.this.storagePermissionHelper, mAttachmentToDownloadId);
        attachmentsListAdapter = new MessageDetailsAttachmentListAdapter(MessageDetailsActivity.this, downloadListener);
        factory.setMessageId(messageId);
        factory.setTransientMessage(isTransientMessage);

        pmWebViewClient = new MessageDetailsPmWebViewClient(mUserManager, this);

        messageExpandableAdapter = new MessageDetailsAdapter(MessageDetailsActivity.this, mJobManager, new Message(),
                "", mWvScrollView, pmWebViewClient, this::onLoadEmbeddedImagesCLick, this::onDisplayImagesCLick);

        viewModel = ViewModelProviders.of(this, factory).get(MessageDetailsViewModel.class);
        viewModel.getMessageDetailsRepository().reloadDependenciesForUser(mUserManager.getUsername());

        viewModel.tryFindMessage();

        viewModel.getMessage().observe(this, new MessageObserver());
        viewModel.getDecryptedMessageData().observe(this, new DecryptedMessageObserver());
        viewModel.getLabels().observe(this, new LabelsObserver(messageExpandableAdapter, viewModel.getFolderIds()));
        viewModel.getWebViewContent().observe(this, new WebViewContentObserver());
        viewModel.getMessageDetailsError().observe(this, new MessageDetailsErrorObserver());
        viewModel.getPendingSend().observe(MessageDetailsActivity.this, new PendingSendObserver());
        listenForConnectivityEvent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkDelinquency();
        getPingHandler().postDelayed(getPingRunnable(), 0);
        if (!mNetworkUtil.isConnected()) {
            showNoConnSnackExtended();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        WebView webview = (WebView) v;
        WebView.HitTestResult result = webview.getHitTestResult();
        if (result == null) {
            return;
        }
        int type = result.getType();
        if (Arrays.asList(WebView.HitTestResult.UNKNOWN_TYPE, WebView.HitTestResult.EDIT_TEXT_TYPE).contains(type)) {
            return;
        }
        if (Arrays.asList(WebView.HitTestResult.EMAIL_TYPE, WebView.HitTestResult.SRC_ANCHOR_TYPE).contains(type)) {
            menu.add(getString(R.string.copy_link)).setOnMenuItemClickListener(new Copy(result.getExtra()));
            menu.add(getString(R.string.share_link)).setOnMenuItemClickListener(new Share(result.getExtra()));
        }
    }

    @Override
    public void move(final String folderId) {
        viewModel.removeMessageLabels();
        viewModel.getMessageSavedInDBResult().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                viewModel.getMessageSavedInDBResult().removeObserver(this);
                Message message = viewModel.getDecryptedMessageData().getValue();
                MessageUtils.moveMessage(MessageDetailsActivity.this, mJobManager, folderId, viewModel.getFolderIds(), Collections.singletonList(new SimpleMessage(message)));
                viewModel.markRead(true);
                // onBackPressed(); // handled by receiving MoveToFolder event
            }
        });
        viewModel.saveMessage();
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
    protected void onStart() {
        super.onStart();
        mApp.getBus().register(this);
        if (viewModel != null) {
            mApp.getBus().register(viewModel);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (viewModel != null) {
            if (mMarkAsRead) {
                viewModel.markRead(true);
            }
            if (viewModel != null) {
                mApp.getBus().unregister(viewModel);
            }
            stopEmbeddedImagesTask();
        }
        mApp.getBus().unregister(this);
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

    private void stopEmbeddedImagesTask() {
        messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_details_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (viewModel == null) {
            return true;
        }
        Message message = viewModel.getDecryptedMessageData().getValue();
        if (message != null) {
            MenuItem trash = menu.findItem(R.id.move_to_trash);
            MenuItem viewHeaders = menu.findItem(R.id.view_headers);
            MenuItem delete = menu.findItem(R.id.delete_message);
            MenuItem spam = menu.findItem(R.id.move_to_spam);
            MenuItem inbox = menu.findItem(R.id.move_to_inbox);
            MenuItem archive = menu.findItem(R.id.move_to_archive);
            MenuItem reportPhishing = menu.findItem(R.id.action_report_phishing);
            MenuItem printItem = menu.findItem(R.id.action_print);

            trash.setVisible(Constants.MessageLocationType.Companion.fromInt(message.getLocation()) != Constants.MessageLocationType.TRASH);
            viewHeaders.setVisible(true);
            delete.setVisible(Constants.MessageLocationType.Companion.fromInt(message.getLocation()) == Constants.MessageLocationType.TRASH);
            spam.setVisible(Constants.MessageLocationType.Companion.fromInt(message.getLocation()) != Constants.MessageLocationType.SPAM);
            inbox.setVisible(Constants.MessageLocationType.Companion.fromInt(message.getLocation()) != Constants.MessageLocationType.INBOX);
            archive.setVisible(Constants.MessageLocationType.Companion.fromInt(message.getLocation()) != Constants.MessageLocationType.ARCHIVE);
            reportPhishing.setVisible(showPhishingReportButton);
            printItem.setVisible(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Job job = null;
        Message message = viewModel.getDecryptedMessageData().getValue();
        switch (item.getItemId()) {
            case android.R.id.home:
                viewModel.markRead(true);
                onBackPressed();
                return true;
            case R.id.move_to_trash:
                if (message != null) {
                    job = new PostTrashJobV2(Collections.singletonList(message.getMessageId()), null);
                } else {
                    TextExtensions.showToast(this, R.string.message_not_loaded, Toast.LENGTH_SHORT);
                }
                break;
            case R.id.view_headers:
                if (message != null) {
                    startActivity(AppUtil.decorInAppIntent(new Intent(this, MessageViewHeadersActivity.class).putExtra(EXTRA_VIEW_HEADERS, message.getHeader())));
                }
                break;
            case R.id.delete_message:
                DialogUtils.Companion.showDeleteConfirmationDialog(
                        this, getString(R.string.delete_message),
                        getString(R.string.confirm_destructive_action), unit -> {
                            mJobManager.addJobInBackground(new PostDeleteJob(Collections.singletonList(messageId)));
                            onBackPressed();
                            return unit;
                        });
                break;
            case R.id.move_to_spam:
                job = new PostSpamJob(Collections.singletonList(messageId));
                break;
            case R.id.mark_unread:
                if (message != null) {
                    mMarkAsRead = false;
                    viewModel.markRead(false);
                }
                job = new PostUnreadJob(Collections.singletonList(messageId));
                break;
            case R.id.move_to_inbox:
                job = new PostInboxJob(Collections.singletonList(messageId));
                break;
            case R.id.move_to_archive:
                job = new PostArchiveJob(Collections.singletonList(messageId));
                break;
            case R.id.add_label:
                if (message == null) {
                    TextExtensions.showToast(this, R.string.message_not_loaded, Toast.LENGTH_SHORT);
                    break;
                }
                showLabelsManagerDialog(getSupportFragmentManager(), message);
                break;
            case R.id.add_folder:
                if (message == null) {
                    TextExtensions.showToast(this, R.string.message_not_loaded, Toast.LENGTH_SHORT);
                    break;
                }
                showFoldersManagerDialog(getSupportFragmentManager(), message);
                break;
            case R.id.action_report_phishing:
                showReportPhishingDialog(message);
                break;
            case R.id.action_print:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    String bodyString = viewModel.getBodyString();
                    if (bodyString == null) {
                        bodyString = "";
                    }
                    new MessagePrinter(primaryBaseActivity, primaryBaseActivity.getResources(),
                            (PrintManager) primaryBaseActivity.getSystemService(PRINT_SERVICE)).printMessage(message, bodyString);
                }
                break;
        }

        if (job != null) {
            mJobManager.addJobInBackground(job);
            if ((item.getItemId() != R.id.move_to_trash) && (item.getItemId() != R.id.move_to_spam) && (item.getItemId() != R.id.move_to_archive)) {
                onBackPressed();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onMoveToFolderEvent(MoveToFolderEvent event) {
        Timber.d("MessageDetailsActivity : MoveToFolderEvent received");
        onBackPressed();
    }

    private void showReportPhishingDialog(final Message message) {
        new AlertDialog.Builder(this).setTitle(R.string.phishing_dialog_title).setMessage(R.string.phishing_dialog_message).setPositiveButton(
                R.string.send, (dialogInterface, i) -> {
                    showPhishingReportButton = false;
                    invalidateOptionsMenu();
                    mJobManager.addJobInBackground(new ReportPhishingJob(message));
                }).setNegativeButton(R.string.cancel, null).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        buttonsVisibilityHandler.removeCallbacks(buttonsVisibilityRunnable);
    }

    protected class MessageDetailsRetryListener extends RetryListener {

        @Override
        public void onClick(@NonNull View v) {
            viewModel.setHasConnection(true);
            mNetworkUtil.setCurrentlyHasConnectivity(true);
            viewModel.fetchMessageDetails(false);
            super.onClick(v);
        }
    }

    private MessageDetailsRetryListener messageDetailsRetryListener = new MessageDetailsRetryListener();

    private void showNoConnSnackExtended() {
        Snackbar noConnectivitySnack = getMNoConnectivitySnack();
        if (noConnectivitySnack == null || !noConnectivitySnack.isShownOrQueued()) {
            showNoConnSnack(messageDetailsRetryListener, R.string.no_connectivity_detected_troubleshoot, coordinatorLayout, this);
            calculateAndUpdateActionButtonsPosition();
        }
        invalidateOptionsMenu();
    }

    private void hideNoConnSnackExtended() {
        Snackbar noConnectivitySnackBar = getMNoConnectivitySnack();
        if (noConnectivitySnackBar == null || !noConnectivitySnackBar.isShownOrQueued()) {
            return;
        }
        hideNoConnSnack();
        invalidateOptionsMenu();
    }

    private int coordinatorHeight = -1;

    private void calculateAndUpdateActionButtonsPosition() {
        Snackbar snackbar = getMNoConnectivitySnack();
        if (snackbar == null) {
            return;
        }
        snackbar.addCallback(noConnectivitySnackBarCallback);
    }

    Snackbar.Callback noConnectivitySnackBarCallback = new Snackbar.Callback() {
        @Override
        public void onShown(Snackbar sb) {
            super.onShown(sb);
            int height = sb.getView().getHeight();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) coordinatorLayout.getLayoutParams();
            coordinatorHeight = params.height;
            params.height = 2 * height;
            coordinatorLayout.setLayoutParams(params);
        }

        @Override
        public void onDismissed(Snackbar transientBottomBar, int event) {
            super.onDismissed(transientBottomBar, event);
            if (coordinatorHeight > 0) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) coordinatorLayout.getLayoutParams();
                params.height = coordinatorHeight;
                coordinatorLayout.setLayoutParams(params);
            }
        }
    };

    public void listenForConnectivityEvent() {
        viewModel.getConnectivityEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            Boolean content = event.getContentIfNotHandled();
            if (content == null) {
                return;
            }
            // if(!isDohOngoing) {
                if (content) {
                    hideNoConnSnackExtended();
                    BaseActivity.mPingHasConnection = true;
                } else {
                    showNoConnSnackExtended();
                }
            // }
        });
    }

    public void listenForRecipientsKeys() {
        viewModel.getReloadRecipientsEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            Boolean content = event.getContentIfNotHandled();
            if (content == null) {
                return;
            }
            if (content) {
                messageExpandableAdapter.refreshRecipientsLayout();
            }
        });
    }

    @Subscribe
    public void onPostPhishingReportEvent(PostPhishingReportEvent event) {
        Status status = event.getStatus();
        final int toastMessageId;
        switch (status) {
            case SUCCESS:
                mJobManager.addJobInBackground(new PostSpamJob(Collections.singletonList(messageId)));
                toastMessageId = R.string.phishing_report_send_message_moved_to_spam;
                finish();
                break;
            case STARTED:
            case FAILED:
            case NO_NETWORK:
            case UNAUTHORIZED:
                showPhishingReportButton = true;
                invalidateOptionsMenu();
                toastMessageId = R.string.cannot_send_report_send;
                break;
            default:
                throw new RuntimeException("Unknown message status: " + status);
        }
        TextExtensions.showToast(MessageDetailsActivity.this, toastMessageId, Toast.LENGTH_SHORT);
    }

    @Subscribe
    public void onLabelAddedEvent(LabelAddedEvent event) {
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
    public void onMailSettingsEvent(MailSettingsEvent event) {
        loadMailSettings();
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
    }

    @Subscribe
    public void onMessageSentEvent(MessageSentEvent event) {
        super.onMessageSentEvent(event);
    }


    private String getDecryptedBody(String decryptedHtml) {
        String decryptedBody = decryptedHtml;
        if (decryptedBody == null || decryptedBody.isEmpty()) {
            decryptedBody = getString(R.string.empty_message);
        }

        String regex2 = "<body[^>]*>";
        // break the backgrounds and other urls
        decryptedBody = decryptedBody.replaceAll(regex2, "<body>");

        return decryptedBody;
    }

    private void filterAndLoad(String decryptedMessage) {
        String css = AppUtil.readTxt(this, R.raw.editor);
        boolean showImages = isAutoShowRemoteImages();
        Transformer viewportTransformer = new ViewportTransformer(UiUtil.getRenderWidth(getWindowManager()), css);
        Transformer contentTransformer = new DefaultTransformer()
                .pipe(viewportTransformer)
                .pipe(new AbstractTransformer() {
                    @Override
                    public Document transform(Document doc) {
                        viewModel.setNonBrokenEmail(doc.toString());
                        return doc;
                    }
                });
        viewModel.setNonBrokenEmail(contentTransformer.transform(Jsoup.parse(decryptedMessage)).toString());
        viewModel.setBodyString(viewModel.getNonBrokenEmail());
        if (showImages) {
            viewModel.remoteContentDisplayed();
        }
        messageExpandableAdapter.displayContainerDisplayImages(View.GONE);
        pmWebViewClient.blockRemoteResources(!showImages);
        viewModel.getWebViewContentWithoutImages().setValue(viewModel.getBodyString());

    }

    @Override
    public void onBackPressed() {
        stopEmbeddedImagesTask();
        saveLastInteraction();
        if (messageRecipientUsername != null) {
            Intent mailboxIntent = AppUtil.decorInAppIntent(new Intent(this, MailboxActivity.class));
            mailboxIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mailboxIntent.putExtra(EXTRA_SWITCHED_USER, true);
            startActivity(mailboxIntent);
        }
        finish();
    }

    // todo should be moved inside view model
    @Subscribe
    public void onDownloadEmbeddedImagesEvent(DownloadEmbeddedImagesEvent event) {
        Status status = event.getStatus();
        Log.d("PMTAG", "onDownloadEmbeddedImagesEvent, status: " + status);
        switch (status) {
            case SUCCESS:
                messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.GONE);
                messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.VISIBLE);
                viewModel.getDownloadEmbeddedImagesResult().observe(this, pair -> {
                    String content = pair.first;
                    viewModel.setNonBrokenEmail(pair.second);
                    messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.GONE);
                    if (TextUtils.isEmpty(content)) {
                        return;
                    }
                    viewModel.getWebViewContentWithImages().setValue(content);
                });
                viewModel.onEmbeddedImagesDownloaded(event);
                break;
            case NO_NETWORK:
                messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.VISIBLE);
                messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.GONE);
                showNoConnSnackExtended();
                TextExtensions.showToast(this, R.string.load_embedded_images_failed_no_network);
                break;
            case FAILED:
                messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.VISIBLE);
                messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.GONE);
                TextExtensions.showToast(this, R.string.load_embedded_images_failed);
                break;
            case STARTED:
                messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.GONE);
                messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.VISIBLE);
                viewModel.setHasEmbeddedImages(false);
                break;
            case UNAUTHORIZED:
                break;
        }
    }

    @Subscribe
    public void onDownloadAttachmentEvent(DownloadedAttachmentEvent event) {
        Status status = event.getStatus();
        switch (status) {
            case STARTED:
            case SUCCESS:
                String eventAttachmentId = event.getAttachmentId();
                boolean isDownloaded = Status.SUCCESS.equals(status);
                attachmentsListAdapter.setIsPgpEncrypted(viewModel.isPgpEncrypted());
                attachmentsListAdapter.setDownloaded(eventAttachmentId, isDownloaded);
                if (isDownloaded) {
                    DownloadUtils.viewAttachment(this, event.getFilename());
                } else {
                    TextExtensions.showToast(this, R.string.downloading);
                }
                break;
            case NO_NETWORK:
                showNoConnSnackExtended();
                break;
            case FAILED:
                TextExtensions.showToast(this, R.string.cant_download_attachment);
                break;
            case UNAUTHORIZED:
                break;
        }
    }

    //TODO inline after kotlin
    private class StartActivityFunction implements Function1<Intent, Unit> {

        @Override
        public Unit invoke(Intent intent) {
            startActivity(intent);
            return null;
        }
    }

    @Override
    public void onLabelCreated(String labelName, String color) {
        mJobManager.addJobInBackground(new PostLabelJob(labelName, color, 0, 0, false, null));
    }

    @Override
    public void onLabelsDeleted(List<String> checkedLabelIds) {
        // NOOP
    }

    private void showFoldersManagerDialog(FragmentManager fragmentManager, @NonNull Message message) {
        MoveToFolderDialogFragment moveToFolderDialogFragment = MoveToFolderDialogFragment.newInstance(Constants.MessageLocationType.Companion.fromInt(message.getLocation()));
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(moveToFolderDialogFragment, moveToFolderDialogFragment.getFragmentKey());
        transaction.commitAllowingStateLoss();
    }

    private void showLabelsManagerDialog(FragmentManager fragmentManager, @NonNull Message message) {
        HashSet<String> attachedLabels = new HashSet<>(message.getLabelIDsNotIncludingLocations());
        ManageLabelsDialogFragment manageLabelsDialogFragment = ManageLabelsDialogFragment.newInstance(attachedLabels, null, null, true);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(manageLabelsDialogFragment, manageLabelsDialogFragment.getFragmentKey()).addToBackStack(null).commitAllowingStateLoss();
    }

    @Override
    public void onLabelsChecked(List<String> checkedLabelIds, List<String> unchangedLabelIds, List<String> messageIds) {
        Message message = viewModel.getDecryptedMessageData().getValue();
        // handle the case where too many labels exist for this message
        int maxLabelsAllowed = UserUtils.getMaxAllowedLabels(mUserManager);
        if (checkedLabelIds.size() > maxLabelsAllowed) {
            String messageText = String.format(getString(R.string.max_labels_in_message), message.getSubject(), maxLabelsAllowed);
            TextExtensions.showToast(this, messageText, Toast.LENGTH_SHORT);
        } else {
            viewModel.findAllLabelsWithIds(checkedLabelIds);
        }
    }

    @Override
    public void onLabelsChecked(List<String> checkedLabelIds, List<String> unchangedLabels, List<String> messageIds, List<String> messagesToArchive) {
        Message message = viewModel.getDecryptedMessageData().getValue();
        onLabelsChecked(checkedLabelIds, unchangedLabels, messageIds);
        mJobManager.addJobInBackground(new PostArchiveJob(Collections.singletonList(message.getMessageId())));
        onBackPressed();
    }

    @Subscribe
    public void onAttachmentFailedEvent(AttachmentFailedEvent event) {
        TextExtensions.showToast(this, getString(R.string.attachment_failed) + " " + event
                .getMessageSubject() + " " + event.getAttachmentName(), Toast.LENGTH_SHORT);
    }

    private boolean showActionButtons;

    private class Copy implements MenuItem.OnMenuItemClickListener {
        private final CharSequence mText;

        Copy(CharSequence text) {
            mText = text;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            UiUtil.copy(MessageDetailsActivity.this, mText);
            return true;
        }
    }

    private class Share implements MenuItem.OnMenuItemClickListener {
        private final String mUri;

        Share(String text) {
            mUri = text;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, mUri);
            try {
                startActivity(Intent.createChooser(send, getText(R.string.share_link)));
            } catch (android.content.ActivityNotFoundException ex) {
                // if no app handles it, do nothing
            }
            return true;
        }
    }

    private class MessageObserver implements Observer<Message> {

        @Override
        public void onChanged(@Nullable Message message) {
            mNetworkUtil.setCurrentlyHasConnectivity(true);
            if (message != null) {
                onMessageFound(message);
                viewModel.getMessage().removeObserver(this);
            } else {
                onMessageNotFound();
            }
        }

        private void onMessageFound(@NonNull Message message) {
            viewModel.addressId = message.getAddressID();
            if (message.isDownloaded()) {
                if (!viewModel.getRenderingPassed()) {
                    viewModel.fetchMessageDetails(true);
                }
            } else {
                boolean hasConnectivity = mNetworkUtil.isConnected();
                if (hasConnectivity) {
                    viewModel.fetchMessageDetails(false);
                } else {
                    showNoConnSnackExtended();
                }
            }
        }

        private void onMessageNotFound() {
            if ((isTransientMessage || messageRecipientUsername != null) && mNetworkUtil.isConnected()) {
                // request to fetch message if didn't find in local database
                viewModel.fetchMessageDetails(false);
            }
        }
    }

    private class MessageDetailsPmWebViewClient extends PMWebViewClient {

        MessageDetailsPmWebViewClient(UserManager userManager, MessageDetailsActivity activity) {
            super(userManager, activity, false);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (amountOfRemoteResourcesBlocked() > 0) {
                messageExpandableAdapter.displayContainerDisplayImages(View.VISIBLE);
            }
            if (showActionButtons) {
                // workaround on some devices, the buttons appear quicker than the webview renders
                // the data
                buttonsVisibilityHandler.postDelayed(buttonsVisibilityRunnable, 500);
            }
            super.onPageFinished(view, url);
        }
    }

    private List<Attachment> prevAttachments;

    private class AttachmentsObserver implements Observer<List<Attachment>> {
        @Override
        public void onChanged(@Nullable List<Attachment> newAttachments) {
            /* Workaround for don't let it loop. TODO: Find the cause of the infinite loop after
            Attachments are downloaded and screen has been locked / unlocked */
            List<Attachment> attachments = null;
            boolean loadImages = false;
            if (newAttachments != null && newAttachments.equals(prevAttachments)) {
                attachments = prevAttachments;
            } else {
                // load images only if there is a difference in attachments from the previous load
                loadImages = true;
                prevAttachments = newAttachments;
                attachments = prevAttachments;
            }

            if (attachments != null) {
                viewModel.setAttachmentsList(attachments);
                boolean hasEmbeddedImages = viewModel.prepareEmbeddedImages();
                if (hasEmbeddedImages) {
                    if ((isAutoShowEmbeddedImages() || viewModel.isEmbeddedImagesDisplayed())) {
                        if (loadImages) {
                            viewModel.displayEmbeddedImages();
                        }
                        messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.GONE);
                    } else {
                        messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.VISIBLE);
                    }
                } else {
                    messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.GONE);
                }
                displayAttachmentInfo(attachments);
            } else {
                messageExpandableAdapter.displayAttachmentsViews(View.GONE);
            }
        }
    }

    private void displayAttachmentInfo(List<Attachment> attachments) {
        int attachmentsCount = attachments.size();
        long totalAttachmentSize = 0;
        final int attachmentsVisibility;
        for (int i = 0; i < attachments.size(); i++) {
            Attachment attachment = attachments.get(i);
            totalAttachmentSize += attachment.getFileSize();
        }
        messageExpandableAdapter.getAttachmentsContainer().setTitle(attachmentsCount, totalAttachmentSize);
        ArrayList<Attachment> attachmentsList = new ArrayList<>(attachments);

        attachmentsListAdapter.setList(attachmentsList);
        messageExpandableAdapter.getAttachmentsContainer().setAttachmentsAdapter(attachmentsListAdapter);
        attachmentsVisibility = attachmentsCount > 0 ? View.VISIBLE : View.GONE;
        messageExpandableAdapter.displayAttachmentsViews(attachmentsVisibility);
    }

    private class DecryptedMessageObserver implements Observer<Message> {
        @Override
        public void onChanged(@Nullable final Message message) {
            if (message == null) {
                return;
            }
            viewModel.getMessageAttachments().observe(MessageDetailsActivity.this, new AttachmentsObserver());

            viewModel.setRenderedFromCache(new AtomicBoolean(true));

            final String decryptedBody = getDecryptedBody(message.getDecryptedHTML());
            if (decryptedBody == null || TextUtils.isEmpty(message.getMessageBody())) {
                UiUtil.showInfoSnack(mSnackLayout, MessageDetailsActivity.this, R.string.decryption_error_desc).show();
                return;
            }

            messageExpandableAdapter.setMessageData(message);
            messageExpandableAdapter.refreshRecipientsLayout();

            if (viewModel.getRefreshedKeys()) {
                filterAndLoad(decryptedBody);
                messageExpandableAdapter.setMode(MODE_ACCORDION);
                mWvScrollView.setLayoutManager(new LinearLayoutManager(MessageDetailsActivity.this));
                listenForRecipientsKeys();
                mWvScrollView.setAdapter(messageExpandableAdapter);
            }

            viewModel.triggerVerificationKeyLoading();

            replyButtonsPanelView.setOnMessageActionListener(messageAction -> {
                try {
                    String newMessageTitle = MessageUtils.buildNewMessageTitle(MessageDetailsActivity.this, messageAction, message.getSubject());

                    long userUsedSpace = ProtonMailApplication.getApplication().getSecureSharedPreferences(mUserManager.getUsername()).getLong(Constants.Prefs.PREF_USED_SPACE, 0);
                    long userMaxSpace = mUserManager.getUser().getMaxSpace() == 0L ? Long.MAX_VALUE : mUserManager.getUser().getMaxSpace();
                    long percentageUsed = userUsedSpace * 100 / userMaxSpace;

                    if (percentageUsed >= 100) {
                        DialogUtils.Companion.showInfoDialogWithTwoButtons(MessageDetailsActivity.this,
                                getString(R.string.storage_limit_warning_title), getString(R.string
                                        .storage_limit_reached_text), getString(R.string.learn_more), getString(R.string.okay), unit -> {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse
                                            (getString(R.string.limit_reached_learn_more)));
                                    startActivity(browserIntent);
                                    return unit;
                                }, unit -> unit,
                                true);
                    } else {
                        viewModel.getPrepareEditMessageIntent().observe(MessageDetailsActivity.this,
                                editIntentExtrasEvent -> {
                                    IntentExtrasData editIntentExtras = editIntentExtrasEvent.getContentIfNotHandled();
                                    if (editIntentExtras == null) {
                                        return;
                                    }
                                    Intent intent = AppUtil.decorInAppIntent(new Intent(MessageDetailsActivity.this, ComposeMessageActivity.class));
                                    MessageUtils.addRecipientsToIntent(
                                            intent, ComposeMessageActivity.EXTRA_TO_RECIPIENTS,
                                            editIntentExtras.getToRecipientListString(),
                                            editIntentExtras.getMessageAction(),
                                            editIntentExtras.getUserAddresses()
                                    );
                                    if (editIntentExtras.getIncludeCCList()) {
                                        MessageUtils.addRecipientsToIntent(
                                                intent, ComposeMessageActivity.EXTRA_CC_RECIPIENTS,
                                                editIntentExtras.getMessageCcList(),
                                                editIntentExtras.getMessageAction(),
                                                editIntentExtras.getUserAddresses()
                                        );
                                    }
                                    intent.putExtra(ComposeMessageActivity.EXTRA_LOAD_IMAGES,
                                            editIntentExtras.getImagesDisplayed());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_LOAD_REMOTE_CONTENT,
                                            editIntentExtras.getRemoteContentDisplayed());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_SENDER_NAME,
                                            editIntentExtras.getMessageSenderName());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_SENDER_ADDRESS,
                                            editIntentExtras.getSenderEmailAddress());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_PGP_MIME,
                                            editIntentExtras.isPGPMime());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_TITLE,
                                            editIntentExtras.getNewMessageTitle());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_BODY_LARGE,
                                            editIntentExtras.getLargeMessageBody());
                                    mBigContentHolder.setContent(editIntentExtras
                                            .getMBigContentHolder().getContent());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_BODY,
                                            editIntentExtras.getBody());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_TIMESTAMP,
                                            editIntentExtras.getTimeMs());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ENCRYPTED,
                                            editIntentExtras.getMessageIsEncrypted());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_PARENT_ID,
                                            editIntentExtras.getMessageId());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_ACTION_ID,
                                            editIntentExtras.getMessageAction());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_ID, editIntentExtras.getAddressID());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_EMAIL_ALIAS, editIntentExtras.getAddressEmailAlias());
                                    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_IS_TRANSIENT, isTransientMessage);
                                    if (editIntentExtras.getEmbeddedImagesAttachmentsExist()) {
                                        intent.putParcelableArrayListExtra(ComposeMessageActivity
                                                        .EXTRA_MESSAGE_EMBEDDED_ATTACHMENTS,
                                                editIntentExtras.getAttachments());
                                    }
                                    ArrayList<LocalAttachment> attachments = editIntentExtras.getAttachments();
                                    if (attachments.size() > 0) {
                                        intent.putParcelableArrayListExtra(ComposeMessageActivity.EXTRA_MESSAGE_ATTACHMENTS, attachments);
                                    }
                                    startActivityForResult(intent, 0);
                                });
                        viewModel.prepareEditMessageIntent(messageAction, message, newMessageTitle, decryptedBody, mBigContentHolder);
                    }
                } catch (Exception exc) {
                    Timber.tag("588").e(exc, "Exception on reply panel press");
                }
                return null;
            });

            mProgressView.setVisibility(View.GONE);
            invalidateOptionsMenu();
            viewModel.setRenderingPassed(true);
        }
    }


    private class MessageDetailsErrorObserver implements Observer<Event<Status>> {

        @Override
        public void onChanged(@Nullable Event<Status> status) {
            TextExtensions.showToast(MessageDetailsActivity.this, R.string.default_error_message);
        }
    }

    private class WebViewContentObserver implements Observer<String> {
        @Override
        public void onChanged(String content) {
            messageExpandableAdapter.loadDataFromUrlToMessageView(content);
        }
    }

    private class PendingSendObserver implements Observer<PendingSend> {
        @Override
        public void onChanged(@Nullable PendingSend pendingSend) {
            if (pendingSend != null) {
                Snackbar cannotEditSnack = Snackbar.make(findViewById(R.id.root_layout), R.string.message_can_not_edit, Snackbar.LENGTH_INDEFINITE);
                View view = cannotEditSnack.getView();
                view.setBackgroundColor(getResources().getColor(R.color.red));
                TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                cannotEditSnack.show();
                replyButtonsPanelView.setVisibility(View.INVISIBLE);
            } else {
                showActionButtons = true;
            }
        }
    }

    private Unit onLoadEmbeddedImagesCLick() {
        if (viewModel.getRenderingPassed()) { // this will ensure that the message has been loaded and will protect from premature clicking on download attachments button
            viewModel.startDownloadEmbeddedImagesJob();
        }
        return Unit.INSTANCE;
    }

    private Unit onDisplayImagesCLick() {
        viewModel.displayRemoteContentClicked();
        viewModel.getCheckStoragePermission().observe(this, booleanEvent -> storagePermissionHelper.checkPermission());
        return Unit.INSTANCE;
    }
}
