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
package ch.protonmail.android.activities.composeMessage;

import static ch.protonmail.android.attachments.ImportAttachmentsWorkerKt.KEY_INPUT_DATA_COMPOSER_INSTANCE_ID;
import static ch.protonmail.android.attachments.ImportAttachmentsWorkerKt.KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.Observer;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.otto.Subscribe;
import com.tokenautocomplete.TokenCompleteTextView;

import org.apache.http.protocol.HTTP;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import ch.protonmail.android.R;
import ch.protonmail.android.activities.AddAttachmentsActivity;
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository;
import ch.protonmail.android.api.models.MessageRecipient;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.enumerations.MessageEncryption;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker;
import ch.protonmail.android.attachments.ImportAttachmentsWorker;
import ch.protonmail.android.compose.ComposeMessageViewModel;
import ch.protonmail.android.compose.presentation.mapper.SendPreferencesToMessageEncryptionUiModelMapper;
import ch.protonmail.android.compose.presentation.ui.ComposeMessageKotlinActivity;
import ch.protonmail.android.compose.presentation.ui.MessageRecipientArrayAdapter;
import ch.protonmail.android.compose.presentation.util.HtmlToSpanned;
import ch.protonmail.android.compose.recipients.GroupRecipientsDialogFragment;
import ch.protonmail.android.contacts.PostResult;
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.crypto.AddressCrypto;
import ch.protonmail.android.crypto.CipherText;
import ch.protonmail.android.crypto.Crypto;
import ch.protonmail.android.data.local.model.LocalAttachment;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.MessageSender;
import ch.protonmail.android.details.presentation.model.MessageEncryptionUiModel;
import ch.protonmail.android.di.DefaultSharedPreferences;
import ch.protonmail.android.events.ContactEvent;
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent;
import ch.protonmail.android.events.FetchDraftDetailEvent;
import ch.protonmail.android.events.PostImportAttachmentEvent;
import ch.protonmail.android.events.ResignContactEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.contacts.SendPreferencesEvent;
import ch.protonmail.android.feature.account.AccountManagerKt;
import ch.protonmail.android.jobs.contacts.GetSendPreferenceJob;
import ch.protonmail.android.settings.data.AccountSettingsRepository;
import ch.protonmail.android.tasks.EmbeddedImagesThread;
import ch.protonmail.android.ui.view.ComposerBottomAppBar;
import ch.protonmail.android.usecase.model.FetchPublicKeysRequest;
import ch.protonmail.android.usecase.model.FetchPublicKeysResult;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.DateUtil;
import ch.protonmail.android.utils.Event;
import ch.protonmail.android.utils.HTMLTransformer.AbstractTransformer;
import ch.protonmail.android.utils.HTMLTransformer.DefaultTransformer;
import ch.protonmail.android.utils.HTMLTransformer.Transformer;
import ch.protonmail.android.utils.HTMLTransformer.ViewportTransformer;
import ch.protonmail.android.utils.MailToData;
import ch.protonmail.android.utils.MailToUtils;
import ch.protonmail.android.utils.MessageUtils;
import ch.protonmail.android.utils.ServerTime;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.crypto.TextDecryptionResult;
import ch.protonmail.android.utils.extensions.CommonExtensionsKt;
import ch.protonmail.android.utils.extensions.SerializationUtils;
import ch.protonmail.android.utils.extensions.StringExtensionsKt;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;
import ch.protonmail.android.utils.ui.screen.RenderDimensionsProvider;
import ch.protonmail.android.views.MessageRecipientView;
import ch.protonmail.android.views.PmWebViewClient;
import dagger.hilt.android.AndroidEntryPoint;
import kotlin.collections.CollectionsKt;
import me.proton.core.accountmanager.domain.AccountManager;
import me.proton.core.domain.entity.UserId;
import me.proton.core.user.domain.entity.AddressId;
import timber.log.Timber;

@AndroidEntryPoint
public class ComposeMessageActivity
        extends ComposeMessageKotlinActivity
        implements LoaderManager.LoaderCallbacks<Cursor>,
        GroupRecipientsDialogFragment.IGroupRecipientsListener {
    //region extras
    public static final String EXTRA_PARENT_ID = "parent_id";
    public static final String EXTRA_ACTION_ID = "action_id";
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_TO_RECIPIENTS = "to_recipients";
    public static final String EXTRA_TO_RECIPIENT_GROUPS = "to_recipient_groups";
    public static final String EXTRA_CC_RECIPIENTS = "cc_recipients";
    public static final String EXTRA_PGP_MIME = "pgp_mime";
    public static final String EXTRA_MESSAGE_TITLE = "message_title";
    public static final String EXTRA_MESSAGE_BODY = "message_body";
    public static final String EXTRA_MAIL_TO = "mail_to";
    public static final String EXTRA_MESSAGE_BODY_LARGE = "message_body_large";
    public static final String EXTRA_MESSAGE_ATTACHMENTS = "message_attachments";
    public static final String EXTRA_MESSAGE_EMBEDDED_ATTACHMENTS = "message_attachments_embedded";
    public static final String EXTRA_MESSAGE_TIMESTAMP = "message_timestamp";
    public static final String EXTRA_SENDER_NAME = "sender_name";
    public static final String EXTRA_SENDER_ADDRESS = "sender_address";
    public static final String EXTRA_MESSAGE_RESPONSE_INLINE = "response_inline";
    public static final String EXTRA_MESSAGE_ADDRESS_ID = "address_id";
    public static final String EXTRA_MESSAGE_ADDRESS_EMAIL_ALIAS = "address_email_alias";
    public static final String EXTRA_REPLY_FROM_NOTIFICATION = "reply_from_notification";
    public static final String EXTRA_LOAD_IMAGES = "load_images";
    public static final String EXTRA_LOAD_REMOTE_CONTENT = "load_remote_content";
    private static final int REQUEST_CODE_ADD_ATTACHMENTS = 1;
    private static final String STATE_ADDITIONAL_ROWS_VISIBLE = "additional_rows_visible";
    private static final String STATE_DRAFT_ID = "draft_id";
    private static final String STATE_ADDED_CONTENT = "added_content";
    private static final char[] RECIPIENT_SEPARATORS = {',', ';', ' '};
    //endregion

    //region views
    private Spinner fromAddressSpinner;

    private MessageRecipientView toRecipientView;
    private MessageRecipientView ccRecipientView;
    private MessageRecipientView bccRecipientView;

    private EditText subjectEditText;

    private EditText messageBodyEditText;
    private Button respondInlineButton;

    private ComposerBottomAppBar bottomAppBar;
    //endregion

    private WebView quotedMessageWebView;
    private PmWebViewClient pmWebViewClient;
    final String newline = "<br>";
    private MessageRecipientArrayAdapter recipientAdapter;
    private boolean mAreAdditionalRowsVisible;

    private int mSelectedAddressPosition = 0;
    private boolean askForPermission;

    private String mAction;
    private boolean mUpdateDraftPmMeChanged;
    private boolean largeBody;
    private boolean mSendingInProgress;

    private ComposeMessageViewModel composeMessageViewModel;
    @Inject
    MessageDetailsRepository messageDetailsRepository;
    @Inject
    AccountManager accountManager;

    @Inject
    DownloadEmbeddedAttachmentsWorker.Enqueuer attachmentsWorker;

    @Inject
    @DefaultSharedPreferences
    SharedPreferences defaultSharedPreferences;

    @Inject
    HtmlToSpanned htmlToSpanned;

    @Inject
    RenderDimensionsProvider renderDimensionsProvider;

    @Inject
    AccountSettingsRepository accountSettingsRepository;

    String composerInstanceId;

    Menu menu;

    @Override
    public void onPermissionConfirmed(Constants.PermissionType type) {

        if (type == Constants.PermissionType.CONTACTS) {
            super.onPermissionConfirmed(type);
            return;
        }
        onStoragePermissionGranted();
    }

    @Override
    public void onHasPermission(Constants.PermissionType type) {

        if (type == Constants.PermissionType.CONTACTS) {
            super.onHasPermission(type);
            return;
        }
        onStoragePermissionGranted();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSupportActionBar(binding.composerToolbar);

        // Setup view references
        fromAddressSpinner = binding.composerFromSpinner;
        toRecipientView = binding.composerToRecipientView;
        ccRecipientView = binding.composerCcRecipientView;
        bccRecipientView = binding.composerBccRecipientView;
        subjectEditText = binding.composerSubjectEditText;
        messageBodyEditText = binding.composerMessageBodyEditText;
        respondInlineButton = binding.composerRespondInlineButton;
        bottomAppBar = binding.composerBottomAppBar;

        // region setup click listeners
        binding.composerExpandRecipientsButton.setOnClickListener((View view) -> {
            mAreAdditionalRowsVisible = !mAreAdditionalRowsVisible;
            setAdditionalRowVisibility(mAreAdditionalRowsVisible);
        });
        // endregion

        composeMessageViewModel = getComposeViewModel();
        composeMessageViewModel.init(mHtmlProcessor);
        observeSetup();

        toRecipientView.performBestGuess(false);
        ccRecipientView.performBestGuess(false);
        bccRecipientView.performBestGuess(false);

        binding.rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new KeyboardManager());

        recipientAdapter = new MessageRecipientArrayAdapter(this);
        initRecipientsView(toRecipientView, recipientAdapter, Constants.RecipientLocationType.TO);
        initRecipientsView(ccRecipientView, recipientAdapter, Constants.RecipientLocationType.CC);
        initRecipientsView(bccRecipientView, recipientAdapter, Constants.RecipientLocationType.BCC);
        subjectEditText.setSelection(subjectEditText.getText().length(), subjectEditText.getText().length());

        setUpQuotedMessageWebView();

        messageBodyEditText.setOnKeyListener(new ComposeBodyChangeListener());
        respondInlineButton.setOnClickListener(new RespondInlineButtonClickListener());
        subjectEditText.setOnEditorActionListener(new MessageTitleEditorActionListener());

        Intent intent = getIntent();
        mAction = intent.getAction();
        Bundle extras = intent.getExtras();
        if (savedInstanceState == null) {
            initialiseActivityOnFirstStart(intent, extras);
        } else {
            initialiseActivityOnFirstStart(intent, savedInstanceState);
            setRespondInlineVisibility(!TextUtils.isEmpty(messageBodyEditText.getText()));
        }
        try {
            if (Arrays.asList(Constants.MessageActionType.FORWARD, Constants.MessageActionType.REPLY, Constants.MessageActionType.REPLY_ALL)
                    .contains(composeMessageViewModel.get_actionId()) || (extras != null && extras.getBoolean(EXTRA_MAIL_TO))) {
                // upload attachments if using pgp/mime
                composeMessageViewModel.setBeforeSaveDraft(composeMessageViewModel.getMessageDataResult().isPGPMime(), messageBodyEditText.getText().toString());
            }
        } catch (Exception exc) {
            Timber.e(exc, "Exception on create (failed to save draft)");
        }

        fromAddressSpinner.getBackground().setColorFilter(getResources().getColor(R.color.icon_norm), PorterDuff.Mode.SRC_ATOP);
        List<String> senderAddresses = composeMessageViewModel.getSenderAddresses();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item_black, senderAddresses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromAddressSpinner.setAdapter(adapter);

        if (!TextUtils.isEmpty(composeMessageViewModel.getMessageDataResult().getAddressId())) {
            mSelectedAddressPosition = composeMessageViewModel.getSenderAddressIndex();
        }

        // message was sent to our alias address, which we put as first
        if (!TextUtils.isEmpty(composeMessageViewModel.getMessageDataResult().getAddressEmailAlias())) {
            mSelectedAddressPosition = 0;
        }

        if (!composeMessageViewModel.isPaidUser() && MessageUtils.INSTANCE.isPmMeAddress(senderAddresses.get(mSelectedAddressPosition))) {
            composeMessageViewModel.setOldSenderAddressId(composeMessageViewModel.getMessageDataResult().getAddressId());
            mSelectedAddressPosition = composeMessageViewModel.getUserAddressByIdFromOnlySendAddresses();
            if (!TextUtils.isEmpty(composeMessageViewModel.getMessageDataResult().getAddressEmailAlias())) {
                mSelectedAddressPosition++; // if alias is on the list, index is actually 1 more than usual
            }
            composeMessageViewModel.setSenderAddressIdByEmail(fromAddressSpinner.getAdapter().getItem(mSelectedAddressPosition).toString());
            boolean dialogShowed = defaultSharedPreferences.getBoolean(Constants.Prefs.PREF_PM_ADDRESS_CHANGED, false);
            if (!dialogShowed && !isFinishing()) {
                showPmChangedDialog(senderAddresses.get(mSelectedAddressPosition));
            }
            mUpdateDraftPmMeChanged = true;
        }

        fromAddressSpinner.setSelection(mSelectedAddressPosition);

        fromAddressSpinner.getViewTreeObserver().addOnGlobalLayoutListener(new AddressSpinnerGlobalLayoutListener());
        askForPermission = true;
        composeMessageViewModel.setSignature(composeMessageViewModel.getSignatureByEmailAddress((String) fromAddressSpinner.getSelectedItem()));

        composeMessageViewModel.getFetchedBodyEvents().observe(this, this::setMessageBody);
    }

    private void setUpQuotedMessageWebView() {
        quotedMessageWebView = new WebView(this);
        pmWebViewClient = new PmWebViewClient(mUserManager, accountSettingsRepository, this, false);
        quotedMessageWebView.setWebViewClient(pmWebViewClient);
        quotedMessageWebView.requestDisallowInterceptTouchEvent(true);

        final WebSettings webSettings = quotedMessageWebView.getSettings();
        webSettings.setAllowFileAccess(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setGeolocationEnabled(false);
        webSettings.setSavePassword(false);
        webSettings.setJavaScriptEnabled(false);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setPluginState(WebSettings.PluginState.OFF);

        composeMessageViewModel.setUpWebViewDarkMode(
                this,
                mUserManager.requireCurrentUserId(),
                quotedMessageWebView,
                composeMessageViewModel.getDraftId()
        );

        binding.composerQuotedMessageContainer.addView(quotedMessageWebView);
    }

    private void observeSetup() {

        composeMessageViewModel.getSetupComplete().observe(this, booleanEvent -> {
            for (Map.Entry<MessageRecipientView, List<MessageRecipient>> entry : pendingRecipients.entrySet()) {
                addRecipientsToView(entry.getValue(), entry.getKey());
            }
            allowSpinnerListener = true;
        });

        composeMessageViewModel.getCloseComposer().observe(this, booleanEvent -> {
            finishActivity(false);
        });

        composeMessageViewModel.getDeleteResult().observe(ComposeMessageActivity.this, new CheckLocalMessageObserver());
        composeMessageViewModel.getOpenAttachmentsScreenResult().observe(ComposeMessageActivity.this, new AddAttachmentsObserver());
        composeMessageViewModel.getSavingDraftError().observe(this, savingDraftError -> {
            if (savingDraftError.getShowDialog()) {
                DialogUtils.Companion.showInfoDialog(this, "", savingDraftError.getErrorMessage(), unit -> unit);
            } else {
                Toast.makeText(this, savingDraftError.getErrorMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        composeMessageViewModel.getSavingDraftComplete().observe(this, event -> {
            if (mUpdateDraftPmMeChanged) {
                composeMessageViewModel.setBeforeSaveDraft(false, messageBodyEditText.getText().toString());
                mUpdateDraftPmMeChanged = false;
            }
            disableSendButton(false);
            onMessageLoaded(
                    event,
                    false,
                    TextUtils.isEmpty(mAction) &&
                            composeMessageViewModel.getMessageDataResult().getAttachmentList().isEmpty()
            );
        });

        composeMessageViewModel.getDbIdWatcher().observe(ComposeMessageActivity.this, new SendMessageObserver());

        composeMessageViewModel.getFetchKeyDetailsResult().observe(
                this,
                this::onFetchEmailKeysEvent
        );

        composeMessageViewModel.getBuildingMessageCompleted().observe(this, new BuildObserver());

        composeMessageViewModel.getHasConnectivity().observe(this, this::onConnectivityEvent);
    }

    private void initialiseActivityOnFirstStart(Intent intent, Bundle extras) {
        composeMessageViewModel.getLoadingDraftResult().observe(ComposeMessageActivity.this, new LoadDraftObserver(extras, intent));
        if (extras != null) {
            composeMessageViewModel.prepareMessageData(extras.getBoolean(EXTRA_PGP_MIME, false), extras.getString(EXTRA_MESSAGE_ADDRESS_ID, ""), extras.getString(EXTRA_MESSAGE_ADDRESS_EMAIL_ALIAS));
            composeMessageViewModel.setShowImages(extras.getBoolean(EXTRA_LOAD_IMAGES, false));
            composeMessageViewModel.setShowRemoteContent(extras.getBoolean(EXTRA_LOAD_REMOTE_CONTENT, false));
            boolean replyFromNotification = extras.getBoolean(EXTRA_REPLY_FROM_NOTIFICATION, false);
            String messageId = extras.getString(EXTRA_MESSAGE_ID);
            if (TextUtils.isEmpty(messageId)) {
                messageId = extras.getString(STATE_DRAFT_ID);
            }

            if (!TextUtils.isEmpty(messageId) && !replyFromNotification) {
                // already saved draft trying to edit here
                composeMessageViewModel.setDraftId(messageId);
                binding.composerProgressLayout.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(composeMessageViewModel.getMessageDataResult().getAddressId())) {
                    mSelectedAddressPosition = composeMessageViewModel.getSenderAddressIndex();
                }
                fromAddressSpinner.setSelection(mSelectedAddressPosition);

                composeMessageViewModel.setupEditDraftMessage(messageId, getString(R.string.composer_group_count_of));
                composeMessageViewModel.findDraftMessageById();
            } else {
                // composing new message here
                if (extras.containsKey(EXTRA_TO_RECIPIENTS) || extras.containsKey(EXTRA_TO_RECIPIENT_GROUPS)) {
                    List<MessageRecipient> recipientGroups = (List<MessageRecipient>) extras.getSerializable(EXTRA_TO_RECIPIENT_GROUPS);
                    if (recipientGroups != null && recipientGroups.size() > 0) {
                        addRecipientsToView(recipientGroups, toRecipientView);
                    }
                    String[] recipientEmails = extras.getStringArray(EXTRA_TO_RECIPIENTS);
                    if (recipientEmails != null && recipientEmails.length > 0) {
                        addStringRecipientsToView(new ArrayList<>(Arrays.asList(recipientEmails)), toRecipientView);
                    }
                    messageBodyEditText.requestFocus();
                } else {
                    checkPermissionsAndKeyboardToggle();
                }
                if (extras.containsKey(EXTRA_CC_RECIPIENTS)) {
                    String[] recipientEmails = extras.getStringArray(EXTRA_CC_RECIPIENTS);
                    addStringRecipientsToView(new ArrayList<>(Arrays.asList(recipientEmails)), ccRecipientView);
                    mAreAdditionalRowsVisible = true;
                    focusRespondInline();
                }
                List<LocalAttachment> attachmentsList = new ArrayList<>();
                if (extras.containsKey(EXTRA_MESSAGE_ATTACHMENTS)) {
                    attachmentsList = extras.getParcelableArrayList(EXTRA_MESSAGE_ATTACHMENTS);
                    if (attachmentsList != null) {
                        for (LocalAttachment localAttachment : attachmentsList) {
                            localAttachment.setAttachmentId("");
                            localAttachment.setMessageId("");
                        }
                    }
                    composeMessageViewModel.setEmbeddedAttachmentList(extras.getParcelableArrayList(EXTRA_MESSAGE_EMBEDDED_ATTACHMENTS));
                }
                String messageTitle = extras.getString(EXTRA_MESSAGE_TITLE, "");
                subjectEditText.setText(messageTitle);

                Constants.MessageActionType messageActionType = (Constants.MessageActionType) extras.getSerializable(EXTRA_ACTION_ID);
                composeMessageViewModel.setupComposingNewMessage(messageActionType != null ? messageActionType : Constants.MessageActionType.NONE,
                        extras.getString(EXTRA_PARENT_ID, null), getString(R.string.composer_group_count_of));

                composeMessageViewModel.prepareMessageData(messageTitle, new ArrayList(attachmentsList));

                String content;
                largeBody = extras.getBoolean(EXTRA_MESSAGE_BODY_LARGE, false);
                if (largeBody) {
                    content = mBigContentHolder.getContent();
                    mBigContentHolder.setContent(null);
                } else {
                    content = extras.getString(EXTRA_MESSAGE_BODY);
                }
                String composerContent = null;
                if (extras.containsKey(STATE_ADDED_CONTENT)) {
                    composerContent = extras.getString(STATE_ADDED_CONTENT);
                }
                if (extras.getBoolean(EXTRA_MAIL_TO, false)) {
                    composerContent = content;
                    content = "";
                }

                initialiseMessageBody(intent, extras, content, composerContent);
            }
        } else {
            composeMessageViewModel.prepareMessageData(false, "", "");
            initialiseMessageBody(intent, null, null, null);
        }
    }

    private void initialiseMessageBody(Intent intent, Bundle extras, String content, String composerContent) {
        if (extras != null && (!TextUtils.isEmpty(content) || (!TextUtils.isEmpty(composerContent) && extras.getBoolean(EXTRA_MAIL_TO)))) {
            // forward, reply, reply all here
            try {
                composeMessageViewModel.setMessageTimestamp(extras.getLong(EXTRA_MESSAGE_TIMESTAMP));
                String senderName = extras.getString(EXTRA_SENDER_NAME);
                String senderAddress = extras.getString(EXTRA_SENDER_ADDRESS);

                composeMessageViewModel.setSender(senderName != null ? senderName : "", senderAddress != null ? senderAddress : "");

                setMessageBodyInContainers(
                        composeMessageViewModel.setMessageBody(
                                composerContent,
                                content,
                                true,
                                composeMessageViewModel.getMessageDataResult().isPGPMime(),
                                getString(R.string.original_message_divider),
                                getQuoteHeader()
                        )
                );
            } catch (Exception exc) {
                Timber.tag("588").e(exc, "Exception on initialise message body");
            }
        } else if (extras != null && extras.containsKey(EXTRA_MESSAGE_ID) && extras.getBoolean(EXTRA_REPLY_FROM_NOTIFICATION, false)) {
            // reply from notification here
            composeMessageViewModel.setMessageTimestamp(extras.getLong(EXTRA_MESSAGE_TIMESTAMP));
            composeMessageViewModel.setSender(extras.getString(EXTRA_SENDER_NAME, ""), extras.getString(EXTRA_SENDER_ADDRESS, ""));
            composeMessageViewModel.startFetchMessageDetailJob(extras.getString(EXTRA_MESSAGE_ID, ""));
            setMessageBody();
        } else if (extras == null || (!extras.containsKey(EXTRA_MESSAGE_ID) && !extras.containsKey(STATE_DRAFT_ID))) {
            // compose new message here
            composeMessageViewModel.setBeforeSaveDraft(false, messageBodyEditText.getText().toString());
            setMessageBody();
        }
        if (Intent.ACTION_SEND.equals(mAction)) {
            handleSendSingleFile(intent);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(mAction)) {
            handleSendMultipleFiles(intent);
        } else if (Intent.ACTION_VIEW.equals(mAction)) {
            handleActionView(intent);
        } else if (Intent.ACTION_SENDTO.equals(mAction)) {
            extractMailTo(intent);
        }
    }

    private void setMessageBody() {
        setMessageBody("");
    }

    private void setMessageBody(String composerBody) {
        setMessageBodyInContainers(
                composeMessageViewModel.setMessageBody(
                        null,
                        composerBody,
                        true,
                        false,
                        getString(R.string.original_message_divider),
                        getQuoteHeader()
                )
        );
    }

    private String getQuoteHeader() {
        String timestamp = DateUtil.formatDetailedDateTime(this, composeMessageViewModel.getMessageDataResult().getMessageTimestamp());
        return getString(
                R.string.composer_quote_sender_header,
                timestamp,
                composeMessageViewModel.getMessageDataResult().getSenderName(),
                composeMessageViewModel.getMessageDataResult().getSenderEmailAddress()
        );
    }

    private void onFetchEmailKeysEvent(List<FetchPublicKeysResult> results) {
        mSendingPressed = false;
        binding.composerProgressLayout.setVisibility(View.GONE);
        boolean isRetry = false;
        for (FetchPublicKeysResult result : results) {
            if (result instanceof FetchPublicKeysResult.Success) {
                FetchPublicKeysResult.Success success = (FetchPublicKeysResult.Success) result;
                isRetry = isRetry || success.isSendRetryRequired();
                String email = success.getEmail();
                String key = success.getKey();
                Constants.RecipientLocationType location = success.getRecipientsType();
                if (location == Constants.RecipientLocationType.TO) {
                    toRecipientView.setEmailPublicKey(email, key);
                } else if (location == Constants.RecipientLocationType.CC) {
                    ccRecipientView.setEmailPublicKey(email, key);
                } else if (location == Constants.RecipientLocationType.BCC) {
                    bccRecipientView.setEmailPublicKey(email, key);
                }
            } else {
                FetchPublicKeysResult.Failure failure = (FetchPublicKeysResult.Failure) result;
                String errorMessage;
                MessageRecipientView recipientView = toRecipientView;
                Constants.RecipientLocationType location = failure.getRecipientsType();
                if (location == Constants.RecipientLocationType.TO) {
                    recipientView = toRecipientView;
                } else if (location == Constants.RecipientLocationType.CC) {
                    recipientView = ccRecipientView;
                } else if (location == Constants.RecipientLocationType.BCC) {
                    recipientView = bccRecipientView;
                }

                if (failure.getError() instanceof FetchPublicKeysResult.Failure.Error.WithMessage) {
                    String baseErrorMessage = ((FetchPublicKeysResult.Failure.Error.WithMessage) failure.getError()).getMessage();
                    errorMessage = getString(R.string.composer_removing_address_server_error, failure.getEmail(), baseErrorMessage);
                    recipientView.removeObjectForKey(failure.getEmail());

                } else {
                    errorMessage = getString(R.string.composer_removing_address_generic_error, failure.getEmail());
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }

        Timber.v("onFetchEmailKeysEvent size:%d isRetry:%s", results.size(), isRetry);
        if (isRetry) {
            sendMessage(false);
        }
    }

    private void focusRespondInline() {
        NestedScrollView scrollView = binding.composerScrollView;
        scrollView.postDelayed(() -> scrollView.scrollTo(0, respondInlineButton.getBottom()), 1000);
    }

    private void setRespondInlineVisibility(boolean visible) {
        if (visible) {
            respondInlineButton.setVisibility(View.VISIBLE);
            composeMessageViewModel.setIsRespondInlineButtonVisible(true);
            binding.composerRespondInlineButton.setVisibility(View.VISIBLE);
        } else {
            respondInlineButton.setVisibility(View.GONE);
            composeMessageViewModel.setIsRespondInlineButtonVisible(false);
            binding.composerRespondInlineButton.setVisibility(View.GONE);
        }
    }

    private int skipInitial;
    private TextWatcher typingListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
            if (skipInitial < 2) {
                skipInitial++;
                return;
            }
            skipInitial++;
            composeMessageViewModel.autoSaveDraft(text.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private int skipInitialForSubjectField;
    private TextWatcher typingListenerForSubjectField = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
            if (skipInitialForSubjectField < 2) {
                skipInitialForSubjectField++;
                return;
            }
            skipInitialForSubjectField++;
            composeMessageViewModel.autoSaveDraft(messageBodyEditText.getText().toString());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    public void recipientsSelected(@NonNull ArrayList<MessageRecipient> recipients, @Nonnull Constants.RecipientLocationType location) {
        MessageRecipientView recipient = toRecipientView;
        if (location == Constants.RecipientLocationType.CC) {
            recipient = ccRecipientView;
        } else if (location == Constants.RecipientLocationType.BCC) {
            recipient = bccRecipientView;
        }
        addRecipientsToView(recipients, recipient);
    }

    private void onConnectivityEvent(Constants.ConnectionState connectivity) {
        Timber.v("onConnectivityEvent hasConnectivity:%s DoHOngoing:%s", connectivity.name(), isDohOngoing);
        if (!isDohOngoing) {
            if (connectivity != Constants.ConnectionState.CONNECTED) {
                networkSnackBarUtil.getNoConnectionSnackBar(
                        mSnackLayout,
                        mUserManager.requireCurrentLegacyUser(),
                        this,
                        null,
                        binding.composerBottomAppBar.getId(),
                        connectivity == Constants.ConnectionState.NO_INTERNET
                ).show();
            } else {
                networkSnackBarUtil.hideAllSnackBars();
            }
        }
    }

    private void extractMailTo(Intent intent) {
        Uri mailtoUri = intent.getData();
        if (mailtoUri != null && MailToUtils.MAILTO_SCHEME.equals(mailtoUri.getScheme())) {
            MailToData mailToData = composeMessageViewModel.parseMailTo(intent.getDataString());
            addStringRecipientsToView(mailToData.getAddresses(), toRecipientView);
        } else {
            try {
                addRecipientsFromIntent(intent);
            } catch (Exception e) {
                Timber.d(e, "Failed extracting recipients from the intent");
            }
        }
    }

    /**
     * TODO: this method has originally copied from {@link #extractMailTo(Intent)} and edited, instead of edit the original one, for do not break any logic in other places where it's being called
     * Handle {@link Intent#ACTION_VIEW} for populate the relative fields
     */
    private void handleActionView(Intent intent) {
        Uri uri = Objects.requireNonNull(intent.getData());
        String stringUri = uri.toString();
        if (stringUri.startsWith(MailToUtils.MAILTO_SCHEME)) {
            MailToData mailToData = composeMessageViewModel.parseMailTo(stringUri);
            // Set recipient
            addStringRecipientsToView(mailToData.getAddresses(), toRecipientView);
            // Set cc
            if (!mailToData.getCc().isEmpty() || !mailToData.getBcc().isEmpty()) {
                setAdditionalRowVisibility(true);
                mAreAdditionalRowsVisible = true;
            }
            addStringRecipientsToView(mailToData.getCc(), ccRecipientView);
            // Set bcc
            addStringRecipientsToView(mailToData.getBcc(), bccRecipientView);
            // Set subject
            subjectEditText.setText(mailToData.getSubject());
            // Set body
            Editable oldBody = messageBodyEditText.getText();
            Editable newBody = Editable.Factory.getInstance().newEditable(mailToData.getBody());
            messageBodyEditText.setText(newBody.append(oldBody));

        } else {
            try {
                addRecipientsFromIntent(intent);
            } catch (Exception e) {
                Timber.d(e, "Failed extracting recipients from the intent");
            }
        }
    }

    private void addRecipientsFromIntent(Intent intent) {
        String[] emails = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
        addStringRecipientsToView(Arrays.asList(emails), toRecipientView);

        String[] emailsCc = intent.getStringArrayExtra(Intent.EXTRA_CC);
        addStringRecipientsToView(Arrays.asList(emailsCc), ccRecipientView);

        String[] emailsBcc = intent.getStringArrayExtra(Intent.EXTRA_BCC);
        addStringRecipientsToView(Arrays.asList(emailsBcc), bccRecipientView);
    }

    private void handleSendSingleFile(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        String sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (!TextUtils.isEmpty(sharedSubject)) {
            subjectEditText.setText(sharedSubject);
        }
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(sharedText)) {
            String composerText = messageBodyEditText.getText().toString();
            String builder = sharedText + System.getProperty("line.separator") + composerText;
            messageBodyEditText.setText(builder);
        }
        try {
            extractMailTo(intent);
        } catch (Exception e) {
            Timber.d(e, "Handle set text: extracting email");
        }
        handleSendFileUri(uri);
    }

    private void handleSendFileUri(Uri uri) {
        if (uri != null) {
            composerInstanceId = UUID.randomUUID().toString();
            importAttachments(new String[]{uri.toString()});
        }
    }

    void handleSendMultipleFiles(Intent intent) {
        ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (fileUris != null) {
            String[] uriStrings = new String[fileUris.size()];
            for (int i = 0; i < fileUris.size(); i++) {
                uriStrings[i] = fileUris.get(i).toString();
            }
            composerInstanceId = UUID.randomUUID().toString();
            importAttachments(uriStrings);
        }
    }

    void importAttachments(String[] uriStrings) {
        Data data = new Data.Builder()
                .putStringArray(KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY, uriStrings)
                .putString(KEY_INPUT_DATA_COMPOSER_INSTANCE_ID, composerInstanceId)
                .build();
        OneTimeWorkRequest importAttachmentsWork = new OneTimeWorkRequest.Builder(ImportAttachmentsWorker.class)
                .setInputData(data)
                .build();
        WorkManager.getInstance().enqueue(importAttachmentsWork);
        WorkManager.getInstance().getWorkInfoByIdLiveData(importAttachmentsWork.getId()).observe(this, workInfo -> {
            if (workInfo != null) {
                // Get the Event from Worker
                String json = workInfo.getOutputData().getString(composerInstanceId);
                if (json != null) {
                    PostImportAttachmentEvent event = SerializationUtils.deserialize(
                            json, PostImportAttachmentEvent.class
                    );
                    onPostImportAttachmentEvent(event);
                }
                Log.d("PMTAG", "ImportAttachmentsWorker workInfo = " + workInfo.getState());
            }
        });
    }

    @Subscribe
    public void onPostImportAttachmentEvent(PostImportAttachmentEvent event) {
        if (event == null || event.composerInstanceId == null || !event.composerInstanceId.equals(composerInstanceId)) {
            return;
        }

        List<LocalAttachment> attachmentsList = composeMessageViewModel.getMessageDataResult().getAttachmentList();
        boolean alreadyAdded = CollectionsKt.firstOrNull(attachmentsList, localAttachment ->
                localAttachment.getUri().toString().equals(event.uri)
        ) != null;

        if (!alreadyAdded) {
            attachmentsList.add(new LocalAttachment(Uri.parse(event.uri), event.displayName, event.size, event.mimeType));
            renderViews();
            composeMessageViewModel.buildMessage();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProtonMailApplication.getApplication().getBus().register(this);
        ProtonMailApplication.getApplication().getBus().register(composeMessageViewModel);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.setAlarm(this, true);
        if (askForPermission) {
            contactsPermissionHelper.checkPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkDelinquency();
        renderViews();
        composeMessageViewModel.checkConnectivity();
    }

    @Override
    protected void onPause() {
        UiUtil.hideKeyboard(this);
        super.onPause();
    }

    @Override
    protected void contactPermissionGranted() {
        loadPMContacts();
        if (!composeMessageViewModel.getAndroidContactsLoaded()) {
            getSupportLoaderManager().initLoader(LOADER_ID_ANDROID_CONTACTS, null, this);
        }
    }

    @Override
    protected void contactPermissionDenied() {
        loadPMContacts();
    }

    private void onStoragePermissionGranted() {
        ArrayList<LocalAttachment> embeddedAttachmentsList = composeMessageViewModel.getMessageDataResult().getEmbeddedAttachmentsList();
        if (composeMessageViewModel.getMessageDataResult().getShowImages()) {
            // get messageId from one of the attachments and use it to start DownloadEmbeddedAttachmentsWorker
            for (LocalAttachment localAttachment : embeddedAttachmentsList) {
                if (!TextUtils.isEmpty(localAttachment.getMessageId())) {
                    attachmentsWorker.enqueue(
                            localAttachment.getMessageId(),
                            mUserManager.getCurrentUserId(),
                            null
                    );
                    break;
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        askForPermission = true;
        ProtonMailApplication.getApplication().getBus().unregister(this);
        ProtonMailApplication.getApplication().getBus().unregister(composeMessageViewModel);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_ADDITIONAL_ROWS_VISIBLE, mAreAdditionalRowsVisible);
        outState.putString(STATE_DRAFT_ID, composeMessageViewModel.getDraftId());
        if (largeBody) {
            outState.putBoolean(EXTRA_MESSAGE_BODY_LARGE, true);
            mBigContentHolder.setContent(messageBodyEditText.getText().toString());
        } else {
            outState.putString(EXTRA_MESSAGE_BODY, composeMessageViewModel.getMessageDataResult().getInitialMessageContent());
        }
        outState.putString(STATE_ADDED_CONTENT, composeMessageViewModel.getContent(messageBodyEditText.getText().toString()));
        outState.putString(EXTRA_SENDER_NAME, composeMessageViewModel.getMessageDataResult().getSenderName());
        outState.putString(EXTRA_SENDER_ADDRESS, composeMessageViewModel.getMessageDataResult().getSenderEmailAddress());
        outState.putLong(EXTRA_MESSAGE_TIMESTAMP, composeMessageViewModel.getMessageDataResult().getMessageTimestamp());
    }

    @Override
    public void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAreAdditionalRowsVisible = savedInstanceState.getBoolean(STATE_ADDITIONAL_ROWS_VISIBLE);
        String draftId = savedInstanceState.getString(STATE_DRAFT_ID);
        composeMessageViewModel.setDraftId(!TextUtils.isEmpty(draftId) ? draftId : "");
        composeMessageViewModel.setInitialMessageContent(savedInstanceState.getString(EXTRA_MESSAGE_BODY));
    }

    @Override
    public void onBackPressed() {
        if (composeMessageViewModel.isLoadingDraftBody()) {
            Timber.d("Exit composer: do not save and close");
            finishActivity(false);
        } else if (composeMessageViewModel.isDraftEmpty(this)) {
            Timber.d("Exit composer: deleting draft and closing");
            composeMessageViewModel.deleteDraft();
            finishActivity(false);
        } else {
            Timber.d("Exit composer: saving draft");
            UiUtil.hideKeyboard(ComposeMessageActivity.this);
            composeMessageViewModel.setBeforeSaveDraft(true, messageBodyEditText.getText().toString(), UserAction.SAVE_DRAFT_EXIT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose_message, menu);
        setMenuActionsListeners(menu);
        this.menu = menu;
        disableSendButton(true);
        return true;
    }

    private void setMenuActionsListeners(Menu menu) {
        MenuItem item = menu.findItem(R.id.send_message);
        item.getActionView().findViewById(R.id.send_button).setOnClickListener((View view) -> {
            if (!mSendingPressed)
                onOptionSendHandler();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onOptionSendHandler() {
        boolean containsNonPMAndNonPGPRecipients = toRecipientView.includesNonProtonMailAndNonPGPRecipient() || ccRecipientView.includesNonProtonMailAndNonPGPRecipient() || bccRecipientView.includesNonProtonMailAndNonPGPRecipient();
        boolean containsPgpRecipients = toRecipientView.containsPGPRecipient() || ccRecipientView.containsPGPRecipient() || bccRecipientView.containsPGPRecipient();
        boolean showSection1 = bottomAppBar.hasExpiration() && !bottomAppBar.hasPassword() && containsNonPMAndNonPGPRecipients;
        boolean showSection2 = bottomAppBar.hasExpiration() && containsPgpRecipients;
        if (showSection1 && showSection2) {
            List<String> nonProtonMailRecipients = toRecipientView.getNonProtonMailAndNonPGPRecipients();
            nonProtonMailRecipients.addAll(ccRecipientView.getNonProtonMailAndNonPGPRecipients());
            nonProtonMailRecipients.addAll(bccRecipientView.getNonProtonMailAndNonPGPRecipients());
            List<String> pgpRecipients = toRecipientView.getPGPRecipients();
            pgpRecipients.addAll(ccRecipientView.getPGPRecipients());
            pgpRecipients.addAll(bccRecipientView.getPGPRecipients());
            showExpirationTimeError(nonProtonMailRecipients, pgpRecipients);
        } else if (showSection1) {
            // if expiration time is set, without password and there are recipients which are not PM users, we show the popup
            List<String> nonProtonMailRecipients = toRecipientView.getNonProtonMailAndNonPGPRecipients();
            nonProtonMailRecipients.addAll(ccRecipientView.getNonProtonMailAndNonPGPRecipients());
            nonProtonMailRecipients.addAll(bccRecipientView.getNonProtonMailAndNonPGPRecipients());
            showExpirationTimeError(nonProtonMailRecipients, null);
        } else if (showSection2) {
            // we should add condition if the message sent is pgp
            List<String> pgpRecipients = toRecipientView.getPGPRecipients();
            pgpRecipients.addAll(ccRecipientView.getPGPRecipients());
            pgpRecipients.addAll(bccRecipientView.getPGPRecipients());
            showExpirationTimeError(null, pgpRecipients);
        } else {
            sendMessage(false);
        }
    }

    private void sendMessage(boolean sendAnyway) {
        if (isMessageValid(sendAnyway)) {
            if (subjectEditText.getText().toString().equals("")) {
                DialogUtils.Companion.showInfoDialogWithTwoButtons(ComposeMessageActivity.this,
                        getString(R.string.compose),
                        getString(R.string.no_subject),
                        getString(R.string.no),
                        getString(R.string.yes),
                        unit -> {
                            UiUtil.hideKeyboard(this);
                            composeMessageViewModel.finishBuildingMessage(messageBodyEditText.getText().toString());
                            return unit;
                        }, true);
            } else {
                UiUtil.hideKeyboard(this);
                composeMessageViewModel.finishBuildingMessage(messageBodyEditText.getText().toString());
            }
        }
    }

    private void initRecipientsView(final MessageRecipientView recipientsView, ArrayAdapter adapter, final Constants.RecipientLocationType location) {
        recipientsView.setAdapter(adapter);
        recipientsView.allowCollapse(true);
        recipientsView.setSplitChar(RECIPIENT_SEPARATORS);
        recipientsView.setThreshold(1);
        recipientsView.setLocation(location);
        recipientsView.setDeletionStyle(TokenCompleteTextView.TokenDeleteStyle.PartialCompletion);
        recipientsView.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.None);
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.set(recipientsView, R.drawable.cursor_black);
        } catch (Exception e) {
            // NOOP
        }
        recipientsView.setTokenListener(new TokenCompleteTextView.TokenListener<MessageRecipient>() {
            @Override
            public void onTokenAdded(MessageRecipient token) {
                List<String> emailList = new ArrayList<>();
                if (TextUtils.isEmpty(token.getGroup())) {
                    emailList.add(token.getEmailAddress());
                } else {
                    List<MessageRecipient> groupRecipients = token.getGroupRecipients();
                    for (MessageRecipient recipient : groupRecipients) {
                        emailList.add(recipient.getEmailAddress());
                    }
                }

                FetchPublicKeysRequest emailKeysRequest = new FetchPublicKeysRequest(emailList, location, false);
                composeMessageViewModel.startFetchPublicKeys(Collections.singletonList(emailKeysRequest));
                GetSendPreferenceJob.Destination destination = GetSendPreferenceJob.Destination.TO;
                if (recipientsView.equals(ccRecipientView)) {
                    destination = GetSendPreferenceJob.Destination.CC;
                } else if (recipientsView.equals(bccRecipientView)) {
                    destination = GetSendPreferenceJob.Destination.BCC;
                }
                composeMessageViewModel.startSendPreferenceJob(emailList, destination);
            }

            @Override
            public void onTokenRemoved(MessageRecipient token) {

                recipientsView.removeKey(token.getEmailAddress());
                recipientsView.removeToken(token.getEmailAddress());
            }
        });
    }

    private void renderViews() {
        setAdditionalRowVisibility(mAreAdditionalRowsVisible);
        int attachmentsListSize = composeMessageViewModel.getMessageDataResult().getAttachmentList().size();
        bottomAppBar.setAttachmentsCount(attachmentsListSize);
        if (composeMessageViewModel.getMessageDataResult().getSendPreferences() == null) {
            return;
        }
        for (GetSendPreferenceJob.Destination dest : GetSendPreferenceJob.Destination.values()) {
            MessageRecipientView recipientsView = getRecipientView(dest);
            List<MessageRecipient> recipients = recipientsView.getMessageRecipients();
            if (recipients == null) {
                continue;
            }
            for (MessageRecipient recipient : recipients) {
                String email = recipient.getEmailAddress();
                SendPreference preference = composeMessageViewModel.getMessageDataResult().getSendPreferences().get(email);
                if (preference != null) {
                    setRecipientIconAndDescription(preference, recipientsView);
                }
            }
        }
    }

    private void fillMessageFromUserInputs(@NonNull Message message, boolean isDraft) {
        message.setMessageId(composeMessageViewModel.getDraftId());
        String subject = subjectEditText.getText().toString();
        subject = StringExtensionsKt.normalizeString(subject);
        if (TextUtils.isEmpty(subject)) {
            subject = getString(R.string.empty_subject);
        } else {
            subject = subject.replaceAll("\n", " ");
        }
        message.setSubject(subject);
        User user = mUserManager.getCurrentLegacyUser();
        if (!TextUtils.isEmpty(composeMessageViewModel.getMessageDataResult().getAddressId())) {
            message.setAddressID(composeMessageViewModel.getMessageDataResult().getAddressId());
        } else if (user != null) {
            int actionType = composeMessageViewModel.getActionType().ordinal();
            Constants.MessageActionType messageActionType = Constants.MessageActionType.Companion.fromInt(actionType);
            if (messageActionType != Constants.MessageActionType.REPLY && messageActionType != Constants.MessageActionType.REPLY_ALL) {
                try {
                    message.setAddressID(user.getSenderAddressIdByEmail((String) fromAddressSpinner.getSelectedItem()));
                    message.setSenderName(user.getSenderAddressNameByEmail((String) fromAddressSpinner.getSelectedItem()));
                } catch (Exception exc) {
                    Timber.tag("588").e(exc, "Exception on fill message with user inputs");
                }
            } else {
                message.setAddressID(user.getDefaultAddressId());
                message.setSenderName(user.getDisplayName());
            }
        }
        message.setToList(toRecipientView.getMessageRecipients());
        message.setCcList(ccRecipientView.getMessageRecipients());
        message.setBccList(bccRecipientView.getMessageRecipients());
        message.setDecryptedBody(composeMessageViewModel.getMessageDataResult().getContent());
        message.setEmbeddedImagesArray(composeMessageViewModel.getMessageDataResult().getContent());
        message.setMessageEncryption(MessageEncryption.INTERNAL);
        message.setLabelIDs(message.getAllLabelIDs());
        message.setInline(composeMessageViewModel.getMessageDataResult().isRespondInlineChecked());
        if (isDraft) {
            message.setIsRead(true);
            message.addLabels(Arrays.asList(String.valueOf(Constants.MessageLocationType.ALL_DRAFT.getMessageLocationTypeValue()),
                    String.valueOf(Constants.MessageLocationType.ALL_MAIL.getMessageLocationTypeValue()),
                    String.valueOf(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue())));
            message.setLocation(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue());
            message.setTime(ServerTime.currentTimeMillis() / 1000);
            message.setMessageEncryption(MessageEncryption.INTERNAL);
            message.setDownloaded(true);
        }
    }

    private boolean isInvalidRecipientPresent(MessageRecipientView... messageRecipientViews) {

        for (MessageRecipientView messageRecipientView : messageRecipientViews) {
            String invalidRecipient = messageRecipientView.findInvalidRecipient();
            if (!TextUtils.isEmpty(invalidRecipient)) {
                String message = getString(R.string.invalid_email_address, invalidRecipient);
                TextExtensions.showToast(this, message, Toast.LENGTH_LONG, Gravity.CENTER);
                return true;
            }
        }
        return false;
    }

    private boolean mSendingPressed = false;

    private boolean isMessageValid(boolean sendAnyway) {

        toRecipientView.clearFocus();
        ccRecipientView.clearFocus();
        bccRecipientView.clearFocus();
        if (isInvalidRecipientPresent(toRecipientView, ccRecipientView, bccRecipientView)) {
            return false;
        }
        int totalRecipients = toRecipientView.getRecipientCount() + ccRecipientView.getRecipientCount() + bccRecipientView
                .getRecipientCount();
        if (totalRecipients == 0) {
            TextExtensions.showToast(this, R.string.no_recipients_specified, Toast.LENGTH_LONG, Gravity.CENTER);
            return false;
        }
        long attachmentFileSize = composeMessageViewModel.calculateAttachmentFileSize();
        if (attachmentFileSize > Constants.MAX_ATTACHMENT_FILE_SIZE_IN_BYTES) {
            String message = getString(R.string.attachment_limit, Formatter.formatFileSize(this, Constants.MAX_ATTACHMENT_FILE_SIZE_IN_BYTES), Formatter.formatFileSize(this, attachmentFileSize));
            TextExtensions.showToast(this, message, Toast.LENGTH_LONG, Gravity.CENTER);
            return false;
        }
        if (bottomAppBar.hasPassword()) {
            List<String> toMissingKeys = toRecipientView.addressesWithMissingKeys();
            List<String> ccMissingKeys = ccRecipientView.addressesWithMissingKeys();
            List<String> bccMissingKeys = bccRecipientView.addressesWithMissingKeys();
            boolean isValid = true;
            List<FetchPublicKeysRequest> keysRequests = new ArrayList<>();
            if (!toMissingKeys.isEmpty()) {
                keysRequests.add(
                        new FetchPublicKeysRequest(toMissingKeys, Constants.RecipientLocationType.TO, true)
                );
                isValid = false;
            }
            if (!ccMissingKeys.isEmpty()) {
                keysRequests.add(
                        new FetchPublicKeysRequest(ccMissingKeys, Constants.RecipientLocationType.CC, true)
                );
                isValid = false;
            }
            if (!bccMissingKeys.isEmpty()) {
                keysRequests.add(
                        new FetchPublicKeysRequest(bccMissingKeys, Constants.RecipientLocationType.BCC, true)
                );
                isValid = false;
            }
            if (!isValid) {
                if (mNetworkUtil.isConnected()) {
                    composeMessageViewModel.startFetchPublicKeys(keysRequests);
                    binding.composerProgressLayout.setVisibility(View.VISIBLE);
                    mSendingPressed = true;
                } else {
                    // TODO: 3/10/17 update with message can not send
                    TextExtensions.showToast(this, "Please send password encrypted messages when you have connection", Toast.LENGTH_SHORT);
                }
                return false;
            }
        }
        boolean includesNonProtonMailRecipient = includesNonProtonMailRecipient();
        if (!sendAnyway && includesNonProtonMailRecipient && bottomAppBar.hasExpiration() && !bottomAppBar.hasPassword()) {
            TextExtensions.showToast(this, R.string.no_password_specified, Toast.LENGTH_LONG, Gravity.CENTER);
            return false;
        }
        return true;
    }

    private boolean includesNonProtonMailRecipient() {

        return toRecipientView.includesNonProtonMailRecipient()
                || ccRecipientView.includesNonProtonMailRecipient()
                || bccRecipientView.includesNonProtonMailRecipient();
    }

    private void setAdditionalRowVisibility(boolean show) {
        final int icon = show ? R.drawable.ic_proton_chevron_up : R.drawable.ic_proton_chevron_down;
        binding.composerExpandRecipientsButton.setImageResource(icon);
        final int visibility = show ? View.VISIBLE : View.GONE;
        binding.composerExpandedRecipientsGroup.setVisibility(visibility);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ADD_ATTACHMENTS && resultCode == RESULT_OK) {
            Timber.d("ComposeMessageAct.onActivityResult Received add attachment response with result OK");
            askForPermission = false;
            ArrayList<LocalAttachment> resultAttachmentList = data.getParcelableArrayListExtra(AddAttachmentsActivity.EXTRA_ATTACHMENT_LIST);
            ArrayList<LocalAttachment> listToSet = resultAttachmentList != null ? resultAttachmentList : new ArrayList<>();
            composeMessageViewModel.setAttachmentList(listToSet);
            afterAttachmentsAdded();
        } else {
            Timber.d("ComposeMessageAct.onActivityResult Received result not handled. \n" +
                    "Request code = %s\n" +
                    "Result code = %s", requestCode, resultCode);
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void afterAttachmentsAdded() {
        composeMessageViewModel.setBeforeSaveDraft(false, messageBodyEditText.getText().toString());
        renderViews();
    }

    @Subscribe
    public void onSendPreferencesEvent(SendPreferencesEvent event) {

        Map<String, SendPreference> sendPreferenceMap = event.getSendPreferenceMap();
        if (sendPreferenceMap != null) {
            final MessageRecipientView recipientsView = getRecipientView(event.getDestination());
            for (final Map.Entry<String, SendPreference> entry : sendPreferenceMap.entrySet()) {
                SendPreference sendPreference = entry.getValue();
                if (sendPreference == null) {
                    if (event.getStatus() == Status.FAILED) {
                        TextExtensions.showToast(this, String.format(getString(R.string.recipient_error_and_removed), entry.getKey()));
                    } else {
                        TextExtensions.showToast(this, String.format(getString(R.string.recipient_not_found_and_removed), entry.getKey()));
                    }
                    recipientsView.removeToken(entry.getKey());
                    recipientsView.removeObjectForKey(entry.getKey());
                    return;
                }
                boolean arePinnedKeysVerified = sendPreference.isVerified();
                if(sendPreference.hasPinnedKeys()){
                    if(!arePinnedKeysVerified){
                        DialogUtils.Companion.showInfoDialogWithTwoButtons(
                                ComposeMessageActivity.this,
                                getString(R.string.resign_contact_title),
                                String.format(getString(R.string.resign_contact_message), entry.getKey()),
                                getString(R.string.cancel),
                                getString(R.string.yes),
                                unit -> {
                                    recipientsView.removeToken(entry.getKey());
                                    recipientsView.removeObjectForKey(entry.getKey());
                                    return unit;
                                },
                                unit -> {
                                    GetSendPreferenceJob.Destination destination = GetSendPreferenceJob.Destination.TO;
                                    if (recipientsView.equals(ccRecipientView)) {
                                        destination = GetSendPreferenceJob.Destination.CC;
                                    } else if (recipientsView.equals(bccRecipientView)) {
                                        destination = GetSendPreferenceJob.Destination.BCC;
                                    }
                                    composeMessageViewModel.startResignContactJobJob(entry.getKey(), entry.getValue(), destination);
                                    return unit;
                                },
                                false);
                    }else if(!sendPreference.isPublicKeyPinned()){
                        // send with untrusted key popup
                        DialogUtils.Companion.showInfoDialog(
                                ComposeMessageActivity.this,
                                getString(R.string.send_with_untrusted_key_title),
                                String.format(getString(R.string.send_with_untrusted_key_message), entry.getKey()),
                                unit -> unit);
                    }
                }
                setRecipientIconAndDescription(sendPreference, recipientsView);
            }
            recipientsView.setSendPreferenceMap(composeMessageViewModel.getMessageDataResult().getSendPreferences(), bottomAppBar.hasPassword());
        }
    }

    private MessageRecipientView getRecipientView(GetSendPreferenceJob.Destination destination) {

        switch (destination) {
            case TO:
                return toRecipientView;
            case CC:
                return ccRecipientView;
            case BCC:
                return bccRecipientView;
        }
        return toRecipientView;
    }

    @Subscribe
    public void onResignContactEvent(ResignContactEvent event) {

        MessageRecipientView recipientsView = getRecipientView(event.getDestination());
        SendPreference sendPreference = event.getSendPreference();
        if (event.getStatus() == ContactEvent.SUCCESS) {
            setRecipientIconAndDescription(sendPreference, recipientsView);
        } else {
            recipientsView.removeToken(sendPreference.getEmailAddress());
        }
    }

    private void setRecipientIconAndDescription(SendPreference sendPreference, MessageRecipientView recipientView) {

        String email = sendPreference.getEmailAddress();
        SendPreferencesToMessageEncryptionUiModelMapper sendPreferencesMapper = new SendPreferencesToMessageEncryptionUiModelMapper();
        MessageEncryptionUiModel encryptionUiModel = sendPreferencesMapper.toMessageEncryptionUiModel(sendPreference, bottomAppBar.hasPassword());

        composeMessageViewModel.addSendPreferences(sendPreference);
        recipientView.setIconAndDescription(
                email,
                encryptionUiModel.getLockIcon(),
                encryptionUiModel.getLockIconColor(),
                encryptionUiModel.getTooltip(),
                sendPreference.isPGP()
        );
    }

    private void finishActivity(boolean isSaveDraftAndExit) {
        if (isSaveDraftAndExit) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_MESSAGE_ID, composeMessageViewModel.getDraftId()));
        } else {
            setResult(RESULT_OK);
        }
        if (isTaskRoot()) {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
                if (tasks != null && tasks.size() > 0) {
                    tasks.get(0).finishAndRemoveTask();
                }
            }
        } else
            finish();
    }

    @Subscribe
    public void onFetchDraftDetailEvent(FetchDraftDetailEvent event) {
        binding.composerProgressLayout.setVisibility(View.GONE);
        composeMessageViewModel.onLoadingDraftBodyFinished();
        if (event.success) {
            composeMessageViewModel.initSignatures();
            composeMessageViewModel.processSignature();
            onMessageLoaded(event.getMessage(), true, true);
        } else {
            if (!isFinishing()) {
                DialogUtils.Companion.showInfoDialog(
                        ComposeMessageActivity.this,
                        getString(R.string.app_name),
                        getString(R.string.messages_load_failure),
                        false,
                        unit -> {
                            finishActivity(false);
                            return unit;
                        });
            }
        }
    }

    /**
     * Executed when the message is loaded and everything is prepared for rendering it. It can come from the server or from DB
     *
     * @param loadedMessage     the message itself
     * @param renderBody        whether to render the body
     * @param updateAttachments if the attachments should be updated
     */
    private void onMessageLoaded(@Nullable final Message loadedMessage, boolean renderBody, boolean updateAttachments) {
        if (loadedMessage == null || !loadedMessage.isDownloaded()) {
            return;
        } else {
            binding.composerProgressLayout.setVisibility(View.GONE);
        }
        AddressCrypto crypto = Crypto.forAddress(mUserManager, mUserManager.requireCurrentUserId(), new AddressId(loadedMessage.getAddressID()));
        if (updateAttachments) {
            composeMessageViewModel.createLocalAttachments(loadedMessage);
        }
        if (!renderBody) {
            return;
        }
        if (loadedMessage.getToList().size() != 0) {
            toRecipientView.clear();
            addRecipientsToView(loadedMessage.getToList(), toRecipientView);
        }
        if (loadedMessage.getCcList().size() != 0) {
            ccRecipientView.clear();
            addRecipientsToView(loadedMessage.getCcList(), ccRecipientView);
            mAreAdditionalRowsVisible = true;
        }
        if (loadedMessage.getBccList().size() != 0) {
            bccRecipientView.clear();
            addRecipientsToView(loadedMessage.getBccList(), bccRecipientView);
            mAreAdditionalRowsVisible = true;
        }
        subjectEditText.setText(loadedMessage.getSubject());
        String messageBody = loadedMessage.getMessageBody();
        try {
            TextDecryptionResult tct = crypto.decrypt(new CipherText(messageBody));
            messageBody = tct.getDecryptedData();
        } catch (Exception e) {
            Timber.w(e, "Decryption error");
        }
        composeMessageViewModel.setInitialMessageContent(messageBody);
        if (loadedMessage.isInline()) {
            String mimeType = loadedMessage.getMimeType();
            setInlineContent(messageBody, false, mimeType != null && mimeType.equals(Constants.MIME_TYPE_PLAIN_TEXT));
        } else {
            binding.composerQuotedMessageContainer.setVisibility(View.VISIBLE);
            quotedMessageWebView.setVisibility(View.VISIBLE);
            composeMessageViewModel.setIsMessageBodyVisible(true);
            quotedMessageWebView.setFocusable(false);
            quotedMessageWebView.requestFocus();
            String delim = "<div class=\"verticalLine\">";
            String webDelim = "<blockquote class=\"protonmail_quote\"";
            int delimIndex = messageBody.indexOf(delim);
            if (delimIndex == -1) {
                delimIndex = messageBody.indexOf(webDelim);
            }
            String mimeType = loadedMessage.getMimeType();
            if (delimIndex > -1) {
                String composeBody = messageBody.substring(0, delimIndex);
                setInlineContent(composeBody, true, mimeType != null && mimeType.equals(Constants.MIME_TYPE_PLAIN_TEXT));
                messageBody = messageBody.substring(delimIndex);
                composeMessageViewModel.setInitialMessageContent(messageBody);
                composeMessageViewModel.setContent(messageBody);
                setBodyContent(false, mimeType != null && mimeType.equals(Constants.MIME_TYPE_PLAIN_TEXT));
                quotedMessageWebView.setVisibility(View.VISIBLE);
                composeMessageViewModel.setIsMessageBodyVisible(true);
                quotedMessageWebView.setFocusable(false);
                quotedMessageWebView.requestFocus();
                setRespondInlineVisibility(true);
            } else {
                setInlineContent(messageBody, false, mimeType != null && mimeType.equals(Constants.MIME_TYPE_PLAIN_TEXT));
            }
        }
        composeMessageViewModel.onMessageLoaded(loadedMessage);
        renderViews();
        new SaveMassageTask(messageDetailsRepository, loadedMessage).execute();
        new Handler(Looper.getMainLooper()).postDelayed(() -> disableSendButton(false), 500);
    }

    private void setInlineContent(String messageBody, boolean clean, boolean isPlainText) {

        final String originalMessageDividerString = getString(R.string.original_message_divider);
        if (clean && messageBody.contains(originalMessageDividerString)) {
            Spanned secondPart = htmlToSpanned.invoke(messageBody.substring(messageBody.indexOf(originalMessageDividerString)));
            binding.composerQuoteHeaderTextView.setText(secondPart);
            composeMessageViewModel.setQuotedHeader(secondPart);
            messageBody = messageBody.substring(0, messageBody.indexOf(originalMessageDividerString));
        }
        setRespondInlineVisibility(false);
        quotedMessageWebView.setVisibility(View.GONE);
        composeMessageViewModel.setIsMessageBodyVisible(false);
        messageBodyEditText.setVisibility(View.VISIBLE);
        composeMessageViewModel.setContent(messageBody);
        setBodyContent(true, isPlainText);
    }

    private Map<MessageRecipientView, List<MessageRecipient>> pendingRecipients = new HashMap<>();

    private void addStringRecipientsToView(List<String> recipients, MessageRecipientView messageRecipientView) {
        for (String recipient : recipients) {
            if (CommonExtensionsKt.isValidEmail(recipient)) {
                messageRecipientView.addObject(new MessageRecipient("", recipient));
            } else {
                String message = getString(R.string.invalid_email_address_removed, recipient);
                TextExtensions.showToast(this, message);
            }
        }
    }

    private void addRecipientsToView(List<MessageRecipient> recipients, MessageRecipientView messageRecipientView) {
        if (!composeMessageViewModel.getSetupCompleteValue()) {
            pendingRecipients.put(messageRecipientView, recipients);
            return;
        } else {
            messageRecipientView.clear();
        }
        Map<String, List<MessageRecipient>> groupedRecipients = new HashMap<>();
        for (MessageRecipient recipient : recipients) {
            // loop all recipients
            if (!CommonExtensionsKt.isValidEmail(recipient.getEmailAddress())) {
                String message = getString(R.string.invalid_email_address_removed, recipient);
                TextExtensions.showToast(this, message);
                continue;
            }
            String group = recipient.getGroup();
            if (!TextUtils.isEmpty(group)) {
                List<MessageRecipient> groupRecipients = groupedRecipients.get(group);
                if (groupRecipients == null) {
                    groupRecipients = new ArrayList();
                }
                groupRecipients.add(recipient);
                groupedRecipients.put(group, groupRecipients);
            } else {
                messageRecipientView.addObject(new MessageRecipient("", recipient.getEmailAddress()));
            }
        }
        for (Map.Entry<String, List<MessageRecipient>> entry : groupedRecipients.entrySet()) {
            List<MessageRecipient> groupRecipients = entry.getValue();
            String groupName = entry.getKey();
            ContactLabelUiModel group = composeMessageViewModel.getContactGroupByName(groupName);
            if (group != null) {
                String name = String.format(getString(R.string.composer_group_count_of), groupName, group.getContactEmailsCount(), group.getContactEmailsCount());
                if (groupRecipients.size() != group.getContactEmailsCount()) {
                    if (groupRecipients.size() < group.getContactEmailsCount()) {
                        name = String.format(getString(R.string.composer_group_count_of), groupName, groupRecipients.size(), group.getContactEmailsCount());
                    } else {
                        List<MessageRecipient> groupRecipientList = composeMessageViewModel.getContactGroupRecipients(group);
                        Iterator groupRecipientsIterator = groupRecipients.iterator();
                        while (groupRecipientsIterator.hasNext()) {
                            MessageRecipient currentMR = (MessageRecipient) groupRecipientsIterator.next();
                            boolean found = false;
                            for (MessageRecipient groupMR : groupRecipientList) {
                                if (currentMR.getEmailAddress().equals(groupMR.getEmailAddress())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                groupRecipientsIterator.remove();
                                messageRecipientView.addObject(new MessageRecipient("", currentMR.getEmailAddress()));
                            }
                        }
                    }
                }
                MessageRecipient recipient = new MessageRecipient(name, "");
                recipient.setGroup(groupName);
                recipient.setGroupIcon(R.string.contact_group_groups_icon);
                recipient.setGroupColor(Color.parseColor(UiUtil.normalizeColor(group.getColor())));
                recipient.setGroupRecipients(groupRecipients);
                messageRecipientView.addObject(recipient);
            }
        }
    }

    private void setBodyContent(boolean respondInline, boolean isPlainText) {

        String content = composeMessageViewModel.getMessageDataResult().getContent();
        if (!respondInline) {
            String css = AppUtil.readTxt(this, R.raw.css_reset_with_custom_props);
            String darkCss = "";
            if (composeMessageViewModel.isAppInDarkMode(this)) {
                darkCss = AppUtil.readTxt(this, R.raw.css_reset_dark_mode_only);
            }
            Transformer viewportTransformer = new ViewportTransformer(renderDimensionsProvider.getRenderWidth(this), css, darkCss);
            Transformer contentTransformer = new DefaultTransformer()
                    .pipe(viewportTransformer)
                    .pipe(new AbstractTransformer() {
                        @Override
                        public Document transform(Document doc) {
                            return doc;
                        }
                    });
            Document doc = Jsoup.parse(content);
            doc.outputSettings().indentAmount(0).prettyPrint(false);
            doc = contentTransformer.transform(doc);
            content = doc.toString();
            pmWebViewClient.showRemoteResources(composeMessageViewModel.getMessageDataResult().getShowRemoteContent());
            quotedMessageWebView.loadDataWithBaseURL("", content, "text/html", HTTP.UTF_8, "");
        } else {
            final CharSequence bodyText;
            if (isPlainText) {
                bodyText = content;
            } else {
                bodyText = htmlToSpanned.invoke(content);
            }
            messageBodyEditText.setText(bodyText);
        }
    }

    private void setMessageBodyInContainers(ComposeMessageViewModel.MessageBodySetup messageBodySetup) {
        messageBodyEditText.setText(messageBodySetup.getComposeBody());
        binding.composerQuotedMessageContainer.setVisibility(messageBodySetup.getWebViewVisibility() ? View.VISIBLE : View.GONE);
        binding.composerQuoteHeaderTextView.setText(composeMessageViewModel.getMessageDataResult().getQuotedHeader());
        setRespondInlineVisibility(messageBodySetup.getRespondInlineVisibility());
        setBodyContent(messageBodySetup.getRespondInline(), messageBodySetup.isPlainText());
    }

    @Subscribe
    public void onDownloadEmbeddedImagesEvent(DownloadEmbeddedImagesEvent event) {
        if (event.getStatus().equals(Status.SUCCESS)) {
            Timber.v("onDownloadEmbeddedImagesEvent %s", event.getStatus());
            String content = composeMessageViewModel.getMessageDataResult().getContent();
            String css = AppUtil.readTxt(this, R.raw.css_reset_with_custom_props);
            String darkCss = "";
            if (composeMessageViewModel.isAppInDarkMode(this)) {
                darkCss = AppUtil.readTxt(this, R.raw.css_reset_dark_mode_only);
            }
            Transformer contentTransformer = new ViewportTransformer(renderDimensionsProvider.getRenderWidth(this), css, darkCss);
            Document doc = Jsoup.parse(content);
            doc.outputSettings().indentAmount(0).prettyPrint(false);
            doc = contentTransformer.transform(doc);
            content = doc.toString();
            pmWebViewClient.showRemoteResources(composeMessageViewModel.getMessageDataResult().getShowRemoteContent());
            EmbeddedImagesThread mEmbeddedImagesTask = new EmbeddedImagesThread(new WeakReference<>(ComposeMessageActivity.this.quotedMessageWebView), event, content);
            mEmbeddedImagesTask.execute();
        }
    }

    private void loadPMContacts() {
        if (mUserManager.getCurrentLegacyUser().getCombinedContacts()) {
            Set<UserId> userIds = AccountManagerKt.allLoggedInBlocking(accountManager);
            for(UserId userId : userIds) {
                composeMessageViewModel.fetchContactGroups(userId);
            }
        } else {
            composeMessageViewModel.fetchContactGroups(mUserManager.requireCurrentUserId());
        }

        composeMessageViewModel.getContactGroupsResult().observe(this, messageRecipients -> {
            recipientAdapter.setData(messageRecipients);
        });
        composeMessageViewModel.getPmMessageRecipientsResult().observe(this, messageRecipients -> {
            recipientAdapter.setData(messageRecipients);
        });
        composeMessageViewModel.getSetupComplete().observe(this, event -> {
            Boolean setUpComplete = event.getContentIfNotHandled();
            if (setUpComplete != null && setUpComplete) {
                composeMessageViewModel.loadPMContactsIfNeeded();
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            getSupportLoaderManager().initLoader(LOADER_ID_ANDROID_CONTACTS, null, this);
        }
    }

    @Override
    public String getSearchTerm() {
        return "";
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_ID_ANDROID_CONTACTS && data != null) {
            while (data.moveToNext()) {
                fromAndroidCursor(data);
            }
            composeMessageViewModel.getAndroidMessageRecipientsResult().observe(this, messageRecipients -> {
                recipientAdapter.setData(messageRecipients); // no groups
            });
            composeMessageViewModel.onAndroidContactsLoaded();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }

    public void fromAndroidCursor(Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY));
        if (TextUtils.isEmpty(name)) {
            int idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME);
            if (idx >= 0) { // this is workaround for sometimes on some devices we get illegal state exception
                if (cursor.getType(idx) == Cursor.FIELD_TYPE_STRING) {
                    name = cursor.getString(idx);
                }
            }
        }
        if (name == null) {
            name = "";
        }
        String email = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
        if (!TextUtils.isEmpty(email)) {
            composeMessageViewModel.createMessageRecipient(name, email);
        }
    }

    private void showPmChangedDialog(String address) {

        DialogUtils.Companion.warningDialog(ComposeMessageActivity.this, getString(R.string.dont_remind_again), getString(R.string.ok), String.format(getString(R.string.pm_me_changed), address),
                unit -> {
                    defaultSharedPreferences.edit().putBoolean(Constants.Prefs.PREF_PM_ADDRESS_CHANGED, true).apply();
                    return unit;
                });
    }

    private void showExpirationTimeError(List<String> recipientsMissingPassword, List<String> recipientsDisablePgp) {
        UiUtil.buildExpirationTimeErrorDialog(this, recipientsMissingPassword, recipientsDisablePgp, v -> sendMessage(true));
    }

    private class KeyboardManager implements ViewTreeObserver.OnGlobalLayoutListener {
        @Override
        public void onGlobalLayout() {

            ConstraintLayout rootLayout = binding.rootLayout;
            int heightDiff = rootLayout.getRootView().getHeight() - rootLayout.getHeight();
            if (heightDiff > 150) {
                binding.dummyKeyboard.setVisibility(View.VISIBLE);
            } else {
                binding.dummyKeyboard.setVisibility(View.GONE);
            }
        }
    }

    private class ComposeBodyChangeListener implements View.OnKeyListener {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return false;
        }
    }

    private class RespondInlineButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            composeMessageViewModel.setRespondInline(true);
            setRespondInlineVisibility(false);
            quotedMessageWebView.setVisibility(View.GONE);
            composeMessageViewModel.setIsMessageBodyVisible(false);
            quotedMessageWebView.loadData("", "text/html; charset=utf-8", HTTP.UTF_8);
            binding.composerQuoteHeaderTextView.setVisibility(View.GONE);
            String composeContentBuilder = messageBodyEditText.getText().toString() +
                    System.getProperty("line.separator") +
                    binding.composerQuoteHeaderTextView.getText().toString() +
                    htmlToSpanned.invoke(composeMessageViewModel.getMessageDataResult().getInitialMessageContent());
            messageBodyEditText.setText(composeContentBuilder);
        }
    }

    private class MessageTitleEditorActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

            subjectEditText.clearFocus();
            messageBodyEditText.requestFocus();
            return true;
        }
    }

    private boolean allowSpinnerListener = false;

    private class AddressSpinnerGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        private void showCanNotSendDialog(String senderAddress) {
            if (!isFinishing()) {
                DialogUtils.Companion.showInfoDialog(
                        ComposeMessageActivity.this,
                        getString(R.string.info),
                        String.format(getString(R.string.error_can_not_send_from_this_address), senderAddress),
                        unit -> unit);
            }
        }

        @Override
        public void onGlobalLayout() {

            // Here we are just handling the mAddressesSpinner

            final ViewTreeObserver viewTreeObserver = fromAddressSpinner.getViewTreeObserver();
            viewTreeObserver.removeOnGlobalLayoutListener(this);
            skipInitial = 0;
            messageBodyEditText.addTextChangedListener(typingListener);
            subjectEditText.addTextChangedListener(typingListenerForSubjectField);
            fromAddressSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!allowSpinnerListener) {
                        return;
                    }
                    String email = (String) fromAddressSpinner.getItemAtPosition(position);
                    boolean localAttachmentsListEmpty = composeMessageViewModel.getMessageDataResult().getAttachmentList().isEmpty();
                    if (!composeMessageViewModel.isPaidUser() && MessageUtils.INSTANCE.isPmMeAddress(email)) {
                        fromAddressSpinner.setSelection(mSelectedAddressPosition);
                        Snackbar snack = Snackbar.make(binding.rootLayout, String.format(getString(R.string.error_can_not_send_from_this_address), email), Snackbar.LENGTH_LONG);
                        snack.setAnchorView(binding.composerBottomAppBar);
                        View snackView = snack.getView();
                        TextView tv = snackView.findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextColor(getColor(R.color.text_inverted));
                        snack.show();
                    } else {
                        String previousSenderAddressId = composeMessageViewModel.getMessageDataResult().getAddressId();
                        if (TextUtils.isEmpty(previousSenderAddressId)) { // first switch from default address
                            previousSenderAddressId = mUserManager.requireCurrentLegacyUser().getDefaultAddressId();
                        }

                        if (TextUtils.isEmpty(composeMessageViewModel.getOldSenderAddressId()) && !localAttachmentsListEmpty) {
                            composeMessageViewModel.setOldSenderAddressId(previousSenderAddressId); // only set OldSenderAddressId if it was empty in ViewModel
                        }

                        composeMessageViewModel.setSenderAddressIdByEmail(email);
                        Address address = composeMessageViewModel.getAddressById();
                        if (address.getSend() == 0) {
                            fromAddressSpinner.setSelection(mSelectedAddressPosition);
                            showCanNotSendDialog(address.getEmail());
                            return;
                        }

                        String currentMail = messageBodyEditText.getText().toString();
                        String newSignature = composeMessageViewModel.getNewSignature();
                        boolean isNewSignatureEmpty = TextUtils.isEmpty(newSignature) || TextUtils.isEmpty(htmlToSpanned.invoke(newSignature).toString().trim());
                        String signature = composeMessageViewModel.getMessageDataResult().getSignature();
                        Spanned signatureSpanned = htmlToSpanned.invoke(signature);

                        boolean isSignatureEmpty = TextUtils.isEmpty(signature) || TextUtils.isEmpty(signatureSpanned.toString().trim());
                        if (!isNewSignatureEmpty && !isSignatureEmpty) {
                            newSignature = composeMessageViewModel.calculateSignature(newSignature);
                            messageBodyEditText.setText(currentMail.replace(signatureSpanned, htmlToSpanned.invoke(newSignature)));
                            composeMessageViewModel.processSignature(newSignature);
                        } else if (isNewSignatureEmpty && !isSignatureEmpty) {
                            if (currentMail.contains(signature)) {
                                messageBodyEditText.setText(currentMail.replace("\n\n\n" + signature, ""));
                            } else {
                                if (currentMail.contains(htmlToSpanned.invoke(signature.trim()))) {
                                    messageBodyEditText.setText(currentMail.replace("\n\n\n" + htmlToSpanned.invoke(signature.trim()), ""));
                                } else {
                                    messageBodyEditText.setText(currentMail.replace(signatureSpanned, ""));
                                }
                            }
                            composeMessageViewModel.setSignature("");
                        } else if (!isNewSignatureEmpty && isSignatureEmpty) {
                            newSignature = composeMessageViewModel.calculateSignature(newSignature).trim();
                            StringBuilder sb = new StringBuilder(currentMail);
                            int lastIndexSpace = currentMail.indexOf("\n\n\n");
                            if (lastIndexSpace != -1) {
                                sb.insert(lastIndexSpace, "\n\n\n" + newSignature);
                            } else {
                                sb.append("\n\n\n" + newSignature);
                            }
                            messageBodyEditText.setText(htmlToSpanned.invoke(sb.toString().replace("\n", newline)));
                            composeMessageViewModel.processSignature(newSignature);
                        }
                        // Trigger Save Draft after changing sender to ensure attachments are encrypted with the right key
                        composeMessageViewModel.setBeforeSaveDraft(false, messageBodyEditText.getText().toString());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // noop
                }
            });
        }
    }

    private class SendMessageObserver implements Observer<Long> {

        @Override
        public void onChanged(@Nullable Long dbId) {
            @StringRes int sendingToast = R.string.sending_message;
            if (!mNetworkUtil.isConnected()) {
                sendingToast = R.string.sending_message_offline;
            }
            if(dbId == null){
                Timber.d("Error while saving message. DbId is null.");
                TextExtensions.showToast(ComposeMessageActivity.this, R.string.error_saving_try_again);
            } else {
                TextExtensions.showToast(ComposeMessageActivity.this, sendingToast);
                new Handler(Looper.getMainLooper()).postDelayed(() -> finishActivity(false), 500);
            }
        }
    }

    private class AddAttachmentsObserver implements Observer<List<LocalAttachment>> {

        @Override
        public void onChanged(@Nullable List<LocalAttachment> attachments) {
            String draftId = composeMessageViewModel.getDraftId();
            Intent intent = AppUtil.decorInAppIntent(new Intent(ComposeMessageActivity.this, AddAttachmentsActivity.class));
            intent.putExtra(AddAttachmentsActivity.EXTRA_DRAFT_CREATED, !TextUtils.isEmpty(draftId) && !MessageUtils.INSTANCE.isLocalMessageId(draftId));
            intent.putParcelableArrayListExtra(AddAttachmentsActivity.EXTRA_ATTACHMENT_LIST, new ArrayList<>(attachments));
            intent.putExtra(AddAttachmentsActivity.EXTRA_DRAFT_ID, draftId);
            startActivityForResult(intent, REQUEST_CODE_ADD_ATTACHMENTS);
            renderViews();
        }
    }

    private class LoadDraftObserver implements Observer<Message> {
        private final Bundle extras;
        private final Intent intent;

        LoadDraftObserver(Bundle extras, Intent intent) {
            this.extras = extras;
            this.intent = intent;
        }

        @Override
        public void onChanged(@Nullable Message message) {
            onMessageLoaded(message, true, true);
            String content;
            if (extras.getBoolean(EXTRA_MESSAGE_BODY_LARGE, false)) {
                content = mBigContentHolder.getContent();
                mBigContentHolder.setContent(null);
            } else {
                content = extras.getString(EXTRA_MESSAGE_BODY);
            }
            String composerContent = null;
            if (extras.containsKey(STATE_ADDED_CONTENT)) {
                composerContent = extras.getString(STATE_ADDED_CONTENT);
            }
            initialiseMessageBody(intent, extras, content, composerContent);
        }
    }

    private class CheckLocalMessageObserver implements Observer<Event<PostResult>> {

        @Override
        public void onChanged(@Nullable Event<PostResult> postResultEvent) {

            PostResult result = new PostResult("", Status.UNAUTHORIZED);
            if (postResultEvent != null) {
                result = postResultEvent.getContentIfNotHandled();
            }
            if (result != null && result.getStatus() == Status.SUCCESS) {
                composeMessageViewModel.setBeforeSaveDraft(false, messageBodyEditText.getText().toString());
                renderViews();
            }
        }
    }

    private class BuildObserver implements Observer<Event<Message>> {

        @Override
        public void onChanged(@Nullable Event<Message> messageEvent) {

            Message localMessage = messageEvent.getContentIfNotHandled();
            if (localMessage != null) {
                User user = mUserManager.requireCurrentLegacyUser();
                String aliasAddress = composeMessageViewModel.getMessageDataResult().getAddressEmailAlias();
                MessageSender messageSender;
                if (aliasAddress != null && aliasAddress.equals(fromAddressSpinner.getSelectedItem())) { // it's being sent by alias
                    messageSender = new MessageSender(user.getDisplayNameForAddress(composeMessageViewModel.getMessageDataResult().getAddressId()), composeMessageViewModel.getMessageDataResult().getAddressEmailAlias());
                } else {
                    Address nonAliasAddress;
                    try {
                        if (localMessage.getAddressID() != null) {
                            nonAliasAddress = user.getAddressById(localMessage.getAddressID());
                        } else { // fallback to default address if newly composed message has no addressId
                            nonAliasAddress = user.getAddressById(user.getDefaultAddressId());
                        }
                        messageSender = new MessageSender(nonAliasAddress.getDisplayName(), nonAliasAddress.getEmail());
                    } catch (NullPointerException e) {
                        Timber.d(e, "Inside " + this.getClass().getName() + " nonAliasAddress was null");
                        messageSender = new MessageSender("", "");
                    }
                }
                localMessage.setSender(messageSender);

            } else {
                return;
            }
            UserAction userAction = composeMessageViewModel.getActionType();
            if ((userAction == UserAction.SAVE_DRAFT || userAction == UserAction.SAVE_DRAFT_EXIT) && !mSendingInProgress) {
                // draft
                fillMessageFromUserInputs(localMessage, true);
                localMessage.setExpirationTime(0);
                composeMessageViewModel.saveDraft(localMessage);
                new Handler(Looper.getMainLooper()).postDelayed(() -> disableSendButton(false), 500);
                if (userAction == UserAction.SAVE_DRAFT_EXIT) {
                    finishActivity(true);
                }
            } else if (composeMessageViewModel.getActionType() == UserAction.FINISH_EDIT) {
                mSendingInProgress = true;
                //region prepare sending message
                if (!composeMessageViewModel.getMessageDataResult().isPasswordValid()) {
                    TextExtensions.showToast(ComposeMessageActivity.this, R.string.eo_password_not_completed, Toast.LENGTH_LONG, Gravity.CENTER);
                    return;
                }
                fillMessageFromUserInputs(localMessage, false);
                composeMessageViewModel.sendMessage(localMessage);
            }
        }
    }

    public void checkPermissionsAndKeyboardToggle() {
        boolean hasNotGrantedReadMediaPermissions =
                ActivityCompat.checkSelfPermission(ComposeMessageActivity.this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(ComposeMessageActivity.this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(ComposeMessageActivity.this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED;

        if (hasNotGrantedReadMediaPermissions && !askForPermission) {
            UiUtil.hideKeyboard(this);
        } else if (ActivityCompat.checkSelfPermission(ComposeMessageActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                && !askForPermission) {
            UiUtil.hideKeyboard(this);
        } else {
            toRecipientView.requestFocus();
            UiUtil.toggleKeyboard(this, toRecipientView);
        }
    }

    private void disableSendButton(boolean disable) {
        // Find the menu item you want to style
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.send_message);
            ImageButton imageButton = item.getActionView().findViewById(R.id.send_button);
            if (disable) {
                item.setEnabled(false);
                imageButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.shade_40)));
            } else {
                item.setEnabled(true);
                imageButton.setBackgroundTintList(null);
            }
        }
    }
}
