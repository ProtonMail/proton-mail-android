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
package ch.protonmail.android.activities.composeMessage;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.otto.Subscribe;
import com.tokenautocomplete.TokenCompleteTextView;

import org.apache.http.protocol.HTTP;
import org.jetbrains.annotations.NotNull;
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
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.AddAttachmentsActivity;
import ch.protonmail.android.activities.BaseContactsActivity;
import ch.protonmail.android.activities.fragments.HumanVerificationCaptchaDialogFragment;
import ch.protonmail.android.activities.guest.FirstActivity;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.activities.mailbox.MailboxActivity;
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository;
import ch.protonmail.android.adapters.MessageRecipientViewAdapter;
import ch.protonmail.android.api.AccountManager;
import ch.protonmail.android.api.models.MessageRecipient;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.enumerations.MessageEncryption;
import ch.protonmail.android.api.models.room.contacts.ContactLabel;
import ch.protonmail.android.api.models.room.messages.LocalAttachment;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.models.room.messages.MessageSender;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker;
import ch.protonmail.android.attachments.ImportAttachmentsWorker;
import ch.protonmail.android.compose.ComposeMessageViewModel;
import ch.protonmail.android.compose.ComposeMessageViewModelFactory;
import ch.protonmail.android.compose.recipients.GroupRecipientsDialogFragment;
import ch.protonmail.android.contacts.PostResult;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.crypto.AddressCrypto;
import ch.protonmail.android.crypto.CipherText;
import ch.protonmail.android.crypto.Crypto;
import ch.protonmail.android.events.AttachmentFailedEvent;
import ch.protonmail.android.events.ContactEvent;
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent;
import ch.protonmail.android.events.DraftCreatedEvent;
import ch.protonmail.android.events.FetchDraftDetailEvent;
import ch.protonmail.android.events.FetchMessageDetailEvent;
import ch.protonmail.android.events.HumanVerifyOptionsEvent;
import ch.protonmail.android.events.LogoutEvent;
import ch.protonmail.android.events.MessageSavedEvent;
import ch.protonmail.android.events.PostImportAttachmentEvent;
import ch.protonmail.android.events.PostLoadContactsEvent;
import ch.protonmail.android.events.ResignContactEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.contacts.SendPreferencesEvent;
import ch.protonmail.android.events.user.MailSettingsEvent;
import ch.protonmail.android.events.verification.PostHumanVerificationEvent;
import ch.protonmail.android.jobs.contacts.GetSendPreferenceJob;
import ch.protonmail.android.tasks.EmbeddedImagesThread;
import ch.protonmail.android.usecase.model.FetchPublicKeysRequest;
import ch.protonmail.android.usecase.model.FetchPublicKeysResult;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.DateUtil;
import ch.protonmail.android.utils.Event;
import ch.protonmail.android.utils.HTMLTransformer.AbstractTransformer;
import ch.protonmail.android.utils.HTMLTransformer.DefaultTransformer;
import ch.protonmail.android.utils.HTMLTransformer.Transformer;
import ch.protonmail.android.utils.HTMLTransformer.ViewportTransformer;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.MailTo;
import ch.protonmail.android.utils.MailToUtils;
import ch.protonmail.android.utils.MessageUtils;
import ch.protonmail.android.utils.ServerTime;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.crypto.TextDecryptionResult;
import ch.protonmail.android.utils.extensions.CommonExtensionsKt;
import ch.protonmail.android.utils.extensions.SerializationUtils;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;
import ch.protonmail.android.utils.ui.locks.ComposerLockIcon;
import ch.protonmail.android.views.ComposeEditText;
import ch.protonmail.android.views.MessageExpirationView;
import ch.protonmail.android.views.MessagePasswordButton;
import ch.protonmail.android.views.MessageRecipientView;
import ch.protonmail.android.views.PMWebView;
import ch.protonmail.android.views.PMWebViewClient;
import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import timber.log.Timber;

import static ch.protonmail.android.attachments.ImportAttachmentsWorkerKt.KEY_INPUT_DATA_COMPOSER_INSTANCE_ID;
import static ch.protonmail.android.attachments.ImportAttachmentsWorkerKt.KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY;
import static ch.protonmail.android.settings.pin.ValidatePinActivityKt.EXTRA_ATTACHMENT_IMPORT_EVENT;
import static ch.protonmail.android.settings.pin.ValidatePinActivityKt.EXTRA_DRAFT_CREATED_EVENT;
import static ch.protonmail.android.settings.pin.ValidatePinActivityKt.EXTRA_DRAFT_DETAILS_EVENT;
import static ch.protonmail.android.settings.pin.ValidatePinActivityKt.EXTRA_MESSAGE_DETAIL_EVENT;

@AndroidEntryPoint
public class ComposeMessageActivity
        extends BaseContactsActivity
        implements MessagePasswordButton.OnMessagePasswordChangedListener,
        MessageExpirationView.OnMessageExpirationChangedListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        HumanVerificationCaptchaDialogFragment.IHumanVerificationListener,
        GroupRecipientsDialogFragment.IGroupRecipientsListener {
    //region extras
    private static final String TAG_COMPOSE_MESSAGE_ACTIVITY = "ComposeMessageActivity";
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
    public static final String EXTRA_MESSAGE_ENCRYPTED = "message_encrypted";
    public static final String EXTRA_MESSAGE_ATTACHMENTS = "message_attachments";
    public static final String EXTRA_MESSAGE_EMBEDDED_ATTACHMENTS = "message_attachments_embedded";
    public static final String EXTRA_MESSAGE_TIMESTAMP = "message_timestamp";
    public static final String EXTRA_SENDER_NAME = "sender_name";
    public static final String EXTRA_SENDER_ADDRESS = "sender_address";
    public static final String EXTRA_MESSAGE_RESPONSE_INLINE = "response_inline";
    public static final String EXTRA_MESSAGE_ADDRESS_ID = "address_id";
    public static final String EXTRA_MESSAGE_ADDRESS_EMAIL_ALIAS = "address_email_alias";
    public static final String EXTRA_REPLY_FROM_GCM = "reply_from_gcm";
    public static final String EXTRA_LOAD_IMAGES = "load_images";
    public static final String EXTRA_LOAD_REMOTE_CONTENT = "load_remote_content";
    public static final String EXTRA_VERIFY = "verify";
    private static final int REQUEST_CODE_ADD_ATTACHMENTS = 1;
    private static final String STATE_ATTACHMENT_LIST = "attachment_list";
    private static final String STATE_ADDITIONAL_ROWS_VISIBLE = "additional_rows_visible";
    private static final String STATE_DIRTY = "dirty";
    private static final String STATE_DRAFT_ID = "draft_id";
    private static final String STATE_ADDED_CONTENT = "added_content";
    private static final char[] RECIPIENT_SEPARATORS = {',', ';', ' '};
    public static final String EXTRA_MESSAGE_IS_TRANSIENT = "is_transient";
    //endregion
    //region views
    @BindView(R.id.root_layout)
    View mRootLayout;
    @BindView(R.id.scroll_view)
    ScrollView mScrollView;
    @BindView(R.id.cc_row)
    View mCcRowView;
    @BindView(R.id.cc_row_divider)
    View mCcRowDividerView;
    @BindView(R.id.bcc_row)
    View mBccRowView;
    @BindView(R.id.bcc_row_divider)
    View mBccRowDividerView;
    @BindView(R.id.message_expiration_view)
    MessageExpirationView mMessageExpirationView;
    @BindView(R.id.set_message_password)
    MessagePasswordButton mSetMessagePasswordButton;
    @BindView(R.id.set_message_expiration)
    ImageButton mSetMessageExpirationImageButton;
    @BindView(R.id.attachment_count)
    TextView mAttachmentCountTextView;
    @BindView(R.id.to_recipients)
    MessageRecipientView mToRecipientsView;
    @BindView(R.id.cc_recipients)
    MessageRecipientView mCcRecipientsView;
    @BindView(R.id.bcc_recipients)
    MessageRecipientView mBccRecipientsView;
    @BindView(R.id.show_additional_rows)
    ImageButton mShowAdditionalRowsImageButton;
    @BindView(R.id.message_title)
    EditText mMessageTitleEditText;
    @BindView(R.id.scroll_parent)
    View mScrollParentView;
    @BindView(R.id.human_verification)
    View mHumanVerificationView;
    @BindView(R.id.progress)
    View mProgressView;
    @BindView(R.id.progress_spinner)
    View mProgressSpinner;
    @BindView(R.id.message_web_view_container)
    LinearLayout mWebViewContainer;
    @BindView(R.id.message_body)
    ComposeEditText mComposeBodyEditText;
    @BindView(R.id.button_respond_inline)
    Button mRespondInlineButton;
    @BindView(R.id.respond_inline_layout)
    View mRespondInlineLayout;
    @BindView(R.id.quoted_header)
    TextView mQuotedHeaderTextView;
    @BindView(R.id.dummy_keyboard)
    View mDummyKeyboardView;
    @BindView(R.id.addresses_spinner)
    Spinner mAddressesSpinner;
    @BindView(R.id.scroll_content)
    View mScrollContentView;
    //endregion
    private WebView mMessageBody;
    private PMWebViewClient pmWebViewClient;
    private HumanVerificationCaptchaDialogFragment mHumanVerificationDialogFragment;
    final String newline = "<br>";
    private MessageRecipientViewAdapter mMessageRecipientViewAdapter;
    private boolean mAreAdditionalRowsVisible;

    private int mSelectedAddressPosition = 0;
    private boolean askForPermission;
    private boolean mHumanVerifyInProgress;

    private String mAction;
    private boolean mUpdateDraftPmMeChanged;
    private boolean largeBody;

    @Inject
    ComposeMessageViewModelFactory composeMessageViewModelFactory;
    private ComposeMessageViewModel composeMessageViewModel;
    @Inject
    MessageDetailsRepository messageDetailsRepository;

    String composerInstanceId;

    Menu menu;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_compose_message;
    }

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
        if (!checkIfUserLoggedIn()) {
            return;
        }
        setUpActionBar();
        loadMailSettings();
        composeMessageViewModel = ViewModelProviders.of(this, composeMessageViewModelFactory).get(ComposeMessageViewModel.class);
        composeMessageViewModel.init(mHtmlProcessor);
        observeSetup();

        mToRecipientsView.performBestGuess(false);
        mCcRecipientsView.performBestGuess(false);
        mBccRecipientsView.performBestGuess(false);

        mRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new KeyboardManager());

        mMessageRecipientViewAdapter = new MessageRecipientViewAdapter(this);
        initRecipientsView(mToRecipientsView, mMessageRecipientViewAdapter, Constants.RecipientLocationType.TO);
        initRecipientsView(mCcRecipientsView, mMessageRecipientViewAdapter, Constants.RecipientLocationType.CC);
        initRecipientsView(mBccRecipientsView, mMessageRecipientViewAdapter, Constants.RecipientLocationType.BCC);
        mMessageTitleEditText.setSelection(mMessageTitleEditText.getText().length(), mMessageTitleEditText.getText().length());

        mMessageBody = new PMWebView(this);
        pmWebViewClient = new PMWebViewClient(mUserManager, this, true);
        mMessageBody.setWebViewClient(pmWebViewClient);
        mMessageBody.requestDisallowInterceptTouchEvent(true);

        final WebSettings webSettings = mMessageBody.getSettings();
        webSettings.setAllowFileAccess(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setGeolocationEnabled(false);
        webSettings.setSavePassword(false);
        webSettings.setJavaScriptEnabled(false);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setPluginState(WebSettings.PluginState.OFF);

        mWebViewContainer.addView(mMessageBody);

        mComposeBodyEditText.setOnKeyListener(new ComposeBodyChangeListener());
        mScrollParentView.setOnTouchListener(new ParentViewScrollListener());
        mRespondInlineButton.setOnClickListener(new RespondInlineButtonClickListener());
        mMessageTitleEditText.setOnEditorActionListener(new MessageTitleEditorActionListener());

        Intent intent = getIntent();
        mAction = intent.getAction();
        final String type = intent.getType();
        Bundle extras = intent.getExtras();
        if (savedInstanceState == null) {
            initialiseActivityOnFirstStart(intent, extras, type);
        } else {
            initialiseActivityOnFirstStart(intent, savedInstanceState, type);
            setRespondInlineVisibility(!TextUtils.isEmpty(mComposeBodyEditText.getText()));
        }
        try {
            if (Arrays.asList(Constants.MessageActionType.FORWARD, Constants.MessageActionType.REPLY, Constants.MessageActionType.REPLY_ALL)
                    .contains(composeMessageViewModel.get_actionId()) || extras.getBoolean(EXTRA_MAIL_TO)) {
                // upload attachments if using pgp/mime
                composeMessageViewModel.setBeforeSaveDraft(composeMessageViewModel.getMessageDataResult().isPGPMime(), mComposeBodyEditText.getText().toString());
            }
        } catch (Exception exc) {
            Timber.tag("588").e(exc, "Exception on create (upload attachments)");

        }

        mAddressesSpinner.getBackground().setColorFilter(getResources().getColor(R.color.new_purple), PorterDuff.Mode.SRC_ATOP);
        List<String> senderAddresses = composeMessageViewModel.getSenderAddresses();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item_black, senderAddresses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAddressesSpinner.setAdapter(adapter);

        if (!TextUtils.isEmpty(composeMessageViewModel.getMessageDataResult().getAddressId())) {
            mSelectedAddressPosition = composeMessageViewModel.getPositionByAddressId();
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
            composeMessageViewModel.setSenderAddressIdByEmail(mAddressesSpinner.getAdapter().getItem(mSelectedAddressPosition).toString());
            final SharedPreferences prefs = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
            boolean dialogShowed = prefs.getBoolean(Constants.Prefs.PREF_PM_ADDRESS_CHANGED, false);
            if (!dialogShowed && !isFinishing()) {
                showPmChangedDialog(senderAddresses.get(mSelectedAddressPosition));
            }
            mUpdateDraftPmMeChanged = true;
        }

        mAddressesSpinner.setSelection(mSelectedAddressPosition);

        mAddressesSpinner.getViewTreeObserver().addOnGlobalLayoutListener(new AddressSpinnerGlobalLayoutListener());
        askForPermission = true;
        composeMessageViewModel.startGetAvailableDomainsJob();

        if (composeMessageViewModel.getVerify()) {
            mHumanVerificationView.setVisibility(View.VISIBLE);
            composeMessageViewModel.startFetchHumanVerificationOptionsJob();
            mProgressView.setVisibility(View.VISIBLE);
        }

        composeMessageViewModel.setSignature(composeMessageViewModel.getSignatureByEmailAddress((String) mAddressesSpinner.getSelectedItem()));
    }

    private void observeSetup() {

        composeMessageViewModel.getSetupComplete().observe(this, booleanEvent -> {
            for (Map.Entry<MessageRecipientView, List<MessageRecipient>> entry : pendingRecipients.entrySet()) {
                addRecipientsToView(entry.getValue(), entry.getKey());
            }
            allowSpinnerListener = true;
        });

        composeMessageViewModel.getCloseComposer().observe(this, booleanEvent -> {
            finishActivity();
        });

        composeMessageViewModel.getDeleteResult().observe(ComposeMessageActivity.this, new CheckLocalMessageObserver());
        composeMessageViewModel.getOpenAttachmentsScreenResult().observe(ComposeMessageActivity.this, new AddAttachmentsObserver());
        composeMessageViewModel.getMessageDraftResult().observe(ComposeMessageActivity.this, new OnDraftCreatedObserver(TextUtils.isEmpty(mAction)));
        composeMessageViewModel.getSavingDraftComplete().observe(this, event -> {
            if (event != null) {
                DraftCreatedEvent draftEvent = event.getContentIfNotHandled();
                onDraftCreatedEvent(draftEvent);
            }
        });

        composeMessageViewModel.getDbIdWatcher().observe(ComposeMessageActivity.this, new SendMessageObserver());

        composeMessageViewModel.getFetchMessageDetailsEvent().observe(this, messageBuilderDataEvent -> {
            try {
                mProgressView.setVisibility(View.GONE);
                MessageBuilderData messageBuilderData = messageBuilderDataEvent.getContentIfNotHandled();
                if (messageBuilderData != null) {
                    String mimeType = messageBuilderData.getMessage().getMimeType();
                    setMessageBodyInContainers(composeMessageViewModel.setMessageBody
                            (messageBuilderData.getDecryptedMessage(), false,
                                    mimeType != null && mimeType.equals(Constants.MIME_TYPE_PLAIN_TEXT), getString(R.string.sender_name_address),
                                    getString(R.string.original_message_divider),
                                    getString(R.string.reply_prefix_on),
                                    DateUtil.formatDetailedDateTime(this, composeMessageViewModel.getMessageDataResult().getMessageTimestamp())));
                }
                composeMessageViewModel.setBeforeSaveDraft(false, mComposeBodyEditText.getText().toString());
            } catch (Exception exc) {
                Timber.tag("588").e(exc, "Exception on fetch message details event");
            }
        });

        composeMessageViewModel.getFetchKeyDetailsResult().observe(
                this,
                this::onFetchEmailKeysEvent
        );

        composeMessageViewModel.getBuildingMessageCompleted().observe(this, new BuildObserver());

        composeMessageViewModel.getHasConnectivity().observe(this, this::onConnectivityEvent);
    }

    private void initialiseActivityOnFirstStart(Intent intent, Bundle extras, String type) {
        composeMessageViewModel.getLoadingDraftResult().observe(ComposeMessageActivity.this, new LoadDraftObserver(extras, intent, type));
        if (extras != null) {
            composeMessageViewModel.prepareMessageData(extras.getBoolean(EXTRA_PGP_MIME, false), extras.getString(EXTRA_MESSAGE_ADDRESS_ID, ""), extras.getString(EXTRA_MESSAGE_ADDRESS_EMAIL_ALIAS), extras.getBoolean(EXTRA_MESSAGE_IS_TRANSIENT));
            boolean verify = extras.getBoolean(EXTRA_VERIFY, false);
            composeMessageViewModel.setShowImages(extras.getBoolean(EXTRA_LOAD_IMAGES, false));
            composeMessageViewModel.setShowRemoteContent(extras.getBoolean(EXTRA_LOAD_REMOTE_CONTENT, false));
            boolean replyFromGcm = extras.getBoolean(EXTRA_REPLY_FROM_GCM, false);
            String messageId = extras.getString(EXTRA_MESSAGE_ID);
            if (TextUtils.isEmpty(messageId)) {
                messageId = extras.getString(STATE_DRAFT_ID);
            }
            if (!TextUtils.isEmpty(messageId) && !replyFromGcm) {
                // already saved draft trying to edit here
                composeMessageViewModel.setDraftId(messageId);
                mProgressView.setVisibility(View.VISIBLE);
                mProgressSpinner.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(composeMessageViewModel.getMessageDataResult().getAddressId())) {
                    mSelectedAddressPosition = composeMessageViewModel.getPositionByAddressId();
                }
                mAddressesSpinner.setSelection(mSelectedAddressPosition);

                composeMessageViewModel.setupEditDraftMessage(verify, messageId, getString(R.string.composer_group_count_of));
                composeMessageViewModel.findDraftMessageById();
            } else {
                // composing new message here
                if (extras.containsKey(EXTRA_TO_RECIPIENTS) || extras.containsKey(EXTRA_TO_RECIPIENT_GROUPS)) {
                    List<MessageRecipient> recipientGroups = (List<MessageRecipient>) extras.getSerializable(EXTRA_TO_RECIPIENT_GROUPS);
                    if (recipientGroups != null && recipientGroups.size() > 0) {
                        addRecipientsToView(recipientGroups, mToRecipientsView);
                    }
                    String[] recipientEmails = extras.getStringArray(EXTRA_TO_RECIPIENTS);
                    if (recipientEmails != null && recipientEmails.length > 0) {
                        addRecipientsToView(new ArrayList<>(Arrays.asList(recipientEmails)), mToRecipientsView);
                    }
                    mComposeBodyEditText.requestFocus();
                } else {
                    checkPermissionsAndKeyboardToggle();
                }
                if (extras.containsKey(EXTRA_CC_RECIPIENTS)) {
                    String[] recipientEmails = extras.getStringArray(EXTRA_CC_RECIPIENTS);
                    addRecipientsToView(new ArrayList<>(Arrays.asList(recipientEmails)), mCcRecipientsView);
                    mAreAdditionalRowsVisible = true;
                    focusRespondInline();
                }
                composeMessageViewModel.setIsDirty(true);
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
                mMessageTitleEditText.setText(messageTitle);

                Constants.MessageActionType messageActionType = (Constants.MessageActionType) extras.getSerializable(EXTRA_ACTION_ID);
                composeMessageViewModel.setupComposingNewMessage(verify, messageActionType != null ? messageActionType : Constants.MessageActionType.NONE,
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

                initialiseMessageBody(intent, extras, type, content, composerContent);
            }
        } else {
            composeMessageViewModel.prepareMessageData(false, "", "", false);
            initialiseMessageBody(intent, null, type, null, null);
        }
    }

    private void initialiseMessageBody(Intent intent, Bundle extras, String type, String content, String composerContent) {
        if (extras != null && (!TextUtils.isEmpty(content) || (!TextUtils.isEmpty(composerContent) && extras.getBoolean(EXTRA_MAIL_TO)))) {
            // forward, reply, reply all here
            try {
                composeMessageViewModel.setMessageTimestamp(extras.getLong(EXTRA_MESSAGE_TIMESTAMP));
                String senderName = extras.getString(EXTRA_SENDER_NAME);
                String senderAddress = extras.getString(EXTRA_SENDER_ADDRESS);

                composeMessageViewModel.setSender(senderName != null ? senderName : "", senderAddress != null ? senderAddress : "");

                setMessageBodyInContainers(composeMessageViewModel.setMessageBody(composerContent, content, true,
                        composeMessageViewModel.getMessageDataResult().isPGPMime(),
                        getString(R.string.sender_name_address),
                        getString(R.string.original_message_divider),
                        getString(R.string.reply_prefix_on),
                        DateUtil.formatDetailedDateTime(this, composeMessageViewModel.getMessageDataResult().getMessageTimestamp())));
            } catch (Exception exc) {
                Timber.tag("588").e(exc, "Exception on initialise message body");
            }
        } else if (extras != null && extras.containsKey(EXTRA_MESSAGE_ID) && extras.getBoolean(EXTRA_REPLY_FROM_GCM, false)) {
            // reply from notification here
            composeMessageViewModel.setMessageTimestamp(extras.getLong(EXTRA_MESSAGE_TIMESTAMP));
            composeMessageViewModel.setSender(extras.getString(EXTRA_SENDER_NAME, ""), extras.getString(EXTRA_SENDER_ADDRESS, ""));
            composeMessageViewModel.startFetchMessageDetailJob(extras.getString(EXTRA_MESSAGE_ID, ""));
            setMessageBody();
        } else if (extras == null || !extras.containsKey(EXTRA_MESSAGE_ID)) {
            // compose new message here
            composeMessageViewModel.setBeforeSaveDraft(false, mComposeBodyEditText.getText().toString());
            setMessageBody();
        }
        if (Intent.ACTION_SEND.equals(mAction) && type != null) {
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
        setMessageBodyInContainers(composeMessageViewModel.setMessageBody("", true, false,
                getString(R.string.sender_name_address),
                getString(R.string.original_message_divider),
                getString(R.string.reply_prefix_on),
                DateUtil.formatDetailedDateTime(this, composeMessageViewModel.getMessageDataResult().getMessageTimestamp())));
    }

    private void setUpActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("");
        }
    }

    private boolean checkIfUserLoggedIn() {
        if (!mUserManager.isLoggedIn()) {
            TextExtensions.showToast(this, R.string.need_to_be_logged_in);
            Class activityToRun;
            if (mUserManager != null && mUserManager.isEngagementShown()) {
                activityToRun = LoginActivity.class;
            } else {
                activityToRun = FirstActivity.class;
            }
            startActivity(AppUtil.decorInAppIntent(new Intent(this, activityToRun)));
            finishActivity();
            return false;
        }
        return true;
    }

    private void onFetchEmailKeysEvent(List<FetchPublicKeysResult> results) {
        mSendingPressed = false;
        mProgressView.setVisibility(View.GONE);
        boolean isRetry = false;
        for (FetchPublicKeysResult result : results) {
            isRetry = isRetry || result.isSendRetryRequired();
            Map<String, String> keys = result.getKeysMap();
            Constants.RecipientLocationType location = result.getRecipientsType();
            if (location == Constants.RecipientLocationType.TO) {
                mToRecipientsView.setEmailPublicKey(keys);
            } else if (location == Constants.RecipientLocationType.CC) {
                mCcRecipientsView.setEmailPublicKey(keys);
            } else if (location == Constants.RecipientLocationType.BCC) {
                mBccRecipientsView.setEmailPublicKey(keys);
            }
        }

        Timber.v("onFetchEmailKeysEvent size:%d isRetry:%s", results.size(), isRetry);
        if (isRetry) {
            sendMessage(false);
        }
    }

    @Subscribe
    public void onHumanVerifyOptionsEvent(HumanVerifyOptionsEvent event) {
        UiUtil.hideKeyboard(this);
        mHumanVerifyInProgress = true;
        List<String> verificationMethods = event.getVerifyMethods();
        if (verificationMethods.size() > 1) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mHumanVerificationDialogFragment, "fragment_human_verification")
                    .commitAllowingStateLoss();
        } else {
            Constants.TokenType method = Constants.TokenType.Companion.fromString(verificationMethods.get(0));
            if (method == Constants.TokenType.CAPTCHA) {
                verificationOptionChose(Constants.TokenType.CAPTCHA, event.getToken());
            }
        }
    }

    @Subscribe
    public void onPostHumanVerificationEvent(PostHumanVerificationEvent event) {
        if (mHumanVerificationDialogFragment != null && mHumanVerificationDialogFragment.isAdded()) {
            mHumanVerificationDialogFragment.dismiss();
        }
        if (event.getStatus() == Status.SUCCESS) {
            UiUtil.hideKeyboard(this);
            composeMessageViewModel.finishBuildingMessage(mComposeBodyEditText.getText().toString());
        }
    }

    private void focusRespondInline() {
        new Handler().postDelayed(() -> mScrollView.scrollTo(0, mRespondInlineButton.getBottom()), 1000);
    }

    private void setRespondInlineVisibility(boolean visible) {
        if (visible) {
            mRespondInlineButton.setVisibility(View.VISIBLE);
            composeMessageViewModel.setIsRespondInlineButtonVisible(true);
            mRespondInlineLayout.setVisibility(View.VISIBLE);
        } else {
            mRespondInlineButton.setVisibility(View.GONE);
            composeMessageViewModel.setIsRespondInlineButtonVisible(false);
            mRespondInlineLayout.setVisibility(View.GONE);
        }
    }

    private int skipInitial;
    private TextWatcher typingListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (skipInitial < 2) {
                skipInitial++;
                return;
            }
            skipInitial++;
            composeMessageViewModel.setIsDirty(true);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    public boolean hasConnectivity() {
        return true;
    }

    @Override
    public void verify(Constants.TokenType tokenType, String token) {
        mHumanVerifyInProgress = false;
        composeMessageViewModel.startPostHumanVerification(tokenType, token);
    }

    @Override
    public void viewLoaded() {
        UiUtil.hideKeyboard(this);
    }

    @Override
    public void verificationOptionChose(Constants.TokenType tokenType, String token) {
        if (tokenType.equals(Constants.TokenType.CAPTCHA)) {
            mHumanVerificationDialogFragment = HumanVerificationCaptchaDialogFragment.newInstance(token);
        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, mHumanVerificationDialogFragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void recipientsSelected(@NonNull ArrayList<MessageRecipient> recipients, @Nonnull Constants.RecipientLocationType location) {
        MessageRecipientView recipient = mToRecipientsView;
        if (location == Constants.RecipientLocationType.CC) {
            recipient = mCcRecipientsView;
        } else if (location == Constants.RecipientLocationType.BCC) {
            recipient = mBccRecipientsView;
        }
        addRecipientsToView(recipients, recipient);
    }

    @NotNull
    private Function0<Unit> onConnectivityCheckRetry() {
        return () -> {
            networkSnackBarUtil.getCheckingConnectionSnackBar(mSnackLayout, null).show();
            composeMessageViewModel.checkConnectivityDelayed();
            return null;
        };
    }

    private void onConnectivityEvent(boolean hasConnectivity) {
        Timber.v("onConnectivityEvent hasConnectivity:%s DoHOngoing:%s", hasConnectivity, isDohOngoing);
        if (!isDohOngoing) {
            if (!hasConnectivity) {
                networkSnackBarUtil.getNoConnectionSnackBar(
                        mSnackLayout,
                        mUserManager.getUser(),
                        this,
                        onConnectivityCheckRetry(),
                        null
                ).show();
            } else {
                networkSnackBarUtil.hideAllSnackBars();
            }
        }
    }

    private void extractMailTo(Intent intent) {
        Uri mailtoUri = intent.getData();
        if (mailtoUri != null && MailToUtils.MAILTO_SCHEME.equals(mailtoUri.getScheme())) {
            MailTo mailTo = MailToUtils.parseIntent(intent);
            ArrayList<String> recipients = new ArrayList<>(mailTo.getAddresses());
            addRecipientsToView(recipients, mToRecipientsView);
        } else {
            try {
                ArrayList<String> emails = (ArrayList<String>) intent.getSerializableExtra(Intent.EXTRA_EMAIL);
                addRecipientsToView(emails, mToRecipientsView);
            } catch (Exception e) {
                Logger.doLogException(TAG_COMPOSE_MESSAGE_ACTIVITY, "Extract mail to getting extra email", e);
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
            MailTo mailTo = MailToUtils.parseIntent(intent);
            // Set recipient
            ArrayList<String> recipients = new ArrayList<>(mailTo.getAddresses());
            addRecipientsToView(recipients, mToRecipientsView);
            // Set cc
            ArrayList<String> ccRecipients = new ArrayList<>(mailTo.getCc());
            addRecipientsToView(ccRecipients, mCcRecipientsView);
            // Set subject
            mMessageTitleEditText.setText(mailTo.getSubject());
            // Set body
            Editable oldBody = mComposeBodyEditText.getText();
            Editable newBody = Editable.Factory.getInstance().newEditable(mailTo.getBody());
            mComposeBodyEditText.setText(newBody.append(oldBody));

        } else {
            try {
                ArrayList<String> emails = (ArrayList<String>) intent.getSerializableExtra(Intent.EXTRA_EMAIL);
                addRecipientsToView(emails, mToRecipientsView);
            } catch (Exception e) {
                Logger.doLogException(TAG_COMPOSE_MESSAGE_ACTIVITY, "Extract mail to getting extra email", e);
            }
        }
    }

    private void handleSendSingleFile(Intent intent) {
        SpannableStringBuilder contentToShareBuilder = new SpannableStringBuilder();
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        String sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (!TextUtils.isEmpty(sharedSubject)) {
            mMessageTitleEditText.setText(sharedSubject);
        }
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(sharedText)) {
            String composerText = mComposeBodyEditText.getText().toString();
            String builder = sharedText + System.getProperty("line.separator") + composerText;
            mComposeBodyEditText.setText(builder);
        }
        String contentToShare = contentToShareBuilder.toString();
        if (!TextUtils.isEmpty(contentToShare)) {
            String composerText = mComposeBodyEditText.getText().toString();
            String contentString = contentToShare + System.getProperty("line.separator") + composerText;
            Spannable contentSpannable = new SpannableString(contentString);
            Linkify.addLinks(contentSpannable, Linkify.ALL);
            mComposeBodyEditText.setText(contentSpannable);
            setRespondInlineVisibility(false);
            composeMessageViewModel.setBeforeSaveDraft(false, mComposeBodyEditText.getText().toString());
        }
        try {
            extractMailTo(intent);
        } catch (Exception e) {
            Logger.doLogException(TAG_COMPOSE_MESSAGE_ACTIVITY, "Handle set text: extracting email", e);
        }
        handleSendFileUri(uri);
    }

    private void handleSendFileUri(Uri uri) {
        if (uri != null) {
            composerInstanceId = UUID.randomUUID().toString();
            Data data = new Data.Builder()
                    .putStringArray(KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY, new String[]{uri.toString()})
                    .putString(KEY_INPUT_DATA_COMPOSER_INSTANCE_ID, composerInstanceId)
                    .build();
            OneTimeWorkRequest importAttachmentsWork = new OneTimeWorkRequest.Builder(ImportAttachmentsWorker.class)
                    .setInputData(data)
                    .build();
            WorkManager workManager = WorkManager.getInstance();
            workManager.enqueue(importAttachmentsWork);

            // Observe the Worker with a LiveData, because result will be received when the
            // Activity will back in foreground, since an EventBut event would be lost while in
            // Background
            workManager.getWorkInfoByIdLiveData(importAttachmentsWork.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {

                            // Get the Event from Worker
                            String json = workInfo.getOutputData().getString(composerInstanceId);
                            if (json != null) {
                                PostImportAttachmentEvent event = SerializationUtils.deserialize(
                                        json, PostImportAttachmentEvent.class
                                );
                                onPostImportAttachmentEvent(event);
                            }
                        }
                    });
            composeMessageViewModel.setIsDirty(true);
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
                    Log.d("PMTAG", "ImportAttachmentsWorker workInfo = " + workInfo.getState());
                }
            });
        }
    }

    @Subscribe
    public void onMailSettingsEvent(MailSettingsEvent event) {
        loadMailSettings();
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finishActivity();
    }

    @Subscribe
    public void onAttachmentFailedEvent(AttachmentFailedEvent event) {
        TextExtensions.showToast(this, getString(R.string.attachment_failed) + " " + event.getMessageSubject() + " " + event.getAttachmentName(), Toast.LENGTH_SHORT);
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
            composeMessageViewModel.setIsDirty(true);
            attachmentsList.add(new LocalAttachment(Uri.parse(event.uri), event.displayName, event.size, event.mimeType));
            renderViews();
        }
    }

    private void onDraftCreatedEvent(final DraftCreatedEvent event) {
        String draftId = composeMessageViewModel.getDraftId();
        if (event == null || !draftId.equals(event.getOldMessageId())) {
            return;
        }
        composeMessageViewModel.onDraftCreated(event);
        if (mUpdateDraftPmMeChanged) {
            composeMessageViewModel.setBeforeSaveDraft(true, mComposeBodyEditText.getText().toString());
            mUpdateDraftPmMeChanged = false;
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
        composeMessageViewModel.insertPendingDraft();
//        mToRecipientsView.invalidateRecipients();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkDelinquency();
        renderViews();
        AppUtil.clearNotifications(this);
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
                    DownloadEmbeddedAttachmentsWorker.Companion.enqueue(
                            localAttachment.getMessageId(),
                            mUserManager.getUsername(),
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
        composeMessageViewModel.removePendingDraft();
        askForPermission = true;
        ProtonMailApplication.getApplication().getBus().unregister(this);
        ProtonMailApplication.getApplication().getBus().unregister(composeMessageViewModel);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_ATTACHMENT_LIST, new ArrayList<>(composeMessageViewModel.getMessageDataResult().getAttachmentList()));
        outState.putBoolean(STATE_ADDITIONAL_ROWS_VISIBLE, mAreAdditionalRowsVisible);
        outState.putBoolean(STATE_DIRTY, composeMessageViewModel.getMessageDataResult().isDirty());
        outState.putString(STATE_DRAFT_ID, composeMessageViewModel.getDraftId());
        if (largeBody) {
            outState.putBoolean(EXTRA_MESSAGE_BODY_LARGE, true);
            mBigContentHolder.setContent(mComposeBodyEditText.getText().toString());
        } else {
            outState.putString(EXTRA_MESSAGE_BODY, composeMessageViewModel.getMessageDataResult().getInitialMessageContent());
        }
        outState.putString(STATE_ADDED_CONTENT, composeMessageViewModel.getContent(mComposeBodyEditText.getText().toString()));
        outState.putString(EXTRA_SENDER_NAME, composeMessageViewModel.getMessageDataResult().getSenderName());
        outState.putString(EXTRA_SENDER_ADDRESS, composeMessageViewModel.getMessageDataResult().getSenderEmailAddress());
        outState.putLong(EXTRA_MESSAGE_TIMESTAMP, composeMessageViewModel.getMessageDataResult().getMessageTimestamp());
    }

    @Override
    public void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        composeMessageViewModel.setAttachmentList(savedInstanceState.getParcelableArrayList(STATE_ATTACHMENT_LIST));
        mAreAdditionalRowsVisible = savedInstanceState.getBoolean(STATE_ADDITIONAL_ROWS_VISIBLE);
        String draftId = savedInstanceState.getString(STATE_DRAFT_ID);
        composeMessageViewModel.setDraftId(!TextUtils.isEmpty(draftId) ? draftId : "");
        composeMessageViewModel.setInitialMessageContent(savedInstanceState.getString(EXTRA_MESSAGE_BODY));
        // Dirty flag should be reset only once message queue has completed the setting of saved tokens
        mToRecipientsView.post(() -> composeMessageViewModel.setIsDirty(savedInstanceState.getBoolean(STATE_DIRTY)));
    }

    @Override
    public void onBackPressed() {
        saveLastInteraction();
        showDraftDialog();
    }

    private void showDraftDialog() {
        if (!isFinishing()) {
            DialogUtils.Companion.showInfoDialogWithThreeButtons(
                    ComposeMessageActivity.this,
                    getString(R.string.compose),
                    getString(R.string.save_message_as_draft),
                    getString(R.string.no),
                    getString(R.string.yes),
                    getString(R.string.cancel),
                    unit -> {
                        String draftId = composeMessageViewModel.getDraftId();
                        if (!TextUtils.isEmpty(draftId)) {
                            composeMessageViewModel.deleteDraft();
                        }
                        mComposeBodyEditText.setIsDirty(false);
                        finishActivity();
                        return unit;
                    },
                    unit -> {
                        UiUtil.hideKeyboard(ComposeMessageActivity.this);
                        composeMessageViewModel.setBeforeSaveDraft(true, mComposeBodyEditText.getText().toString(), UserAction.SAVE_DRAFT_EXIT);
                        return unit;
                    },
                    false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.compose_message_menu, menu);
        this.menu = menu;
        disableSendButton(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.send_message:
                if (mHumanVerifyInProgress) {
                    return true;
                }
                if (mSendingPressed) {
                    return true;
                }
                onOptionSendHandler();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onOptionSendHandler() {
        boolean containsNonPMAndNonPGPRecipients = mToRecipientsView.includesNonProtonMailAndNonPGPRecipient() || mCcRecipientsView.includesNonProtonMailAndNonPGPRecipient() || mBccRecipientsView.includesNonProtonMailAndNonPGPRecipient();
        boolean containsPgpRecipients = mToRecipientsView.containsPGPRecipient() || mCcRecipientsView.containsPGPRecipient() || mBccRecipientsView.containsPGPRecipient();
        boolean showSection1 = mMessageExpirationView.getExpirationTime() > 0 && !mSetMessagePasswordButton.isPasswordSet() && containsNonPMAndNonPGPRecipients;
        boolean showSection2 = mMessageExpirationView.getExpirationTime() > 0 && containsPgpRecipients;
        if (showSection1 && showSection2) {
            List<String> nonProtonMailRecipients = mToRecipientsView.getNonProtonMailAndNonPGPRecipients();
            nonProtonMailRecipients.addAll(mCcRecipientsView.getNonProtonMailAndNonPGPRecipients());
            nonProtonMailRecipients.addAll(mBccRecipientsView.getNonProtonMailAndNonPGPRecipients());
            List<String> pgpRecipients = mToRecipientsView.getPGPRecipients();
            pgpRecipients.addAll(mCcRecipientsView.getPGPRecipients());
            pgpRecipients.addAll(mBccRecipientsView.getPGPRecipients());
            showExpirationTimeError(nonProtonMailRecipients, pgpRecipients);
        } else if (showSection1) {
            // if expiration time is set, without password and there are recipients which are not PM users, we show the popup
            List<String> nonProtonMailRecipients = mToRecipientsView.getNonProtonMailAndNonPGPRecipients();
            nonProtonMailRecipients.addAll(mCcRecipientsView.getNonProtonMailAndNonPGPRecipients());
            nonProtonMailRecipients.addAll(mBccRecipientsView.getNonProtonMailAndNonPGPRecipients());
            showExpirationTimeError(nonProtonMailRecipients, null);
        } else if (showSection2) {
            // we should add condition if the message sent is pgp
            List<String> pgpRecipients = mToRecipientsView.getPGPRecipients();
            pgpRecipients.addAll(mCcRecipientsView.getPGPRecipients());
            pgpRecipients.addAll(mBccRecipientsView.getPGPRecipients());
            showExpirationTimeError(null, pgpRecipients);
        } else {
            sendMessage(false);
        }
    }

    private void sendMessage(boolean sendAnyway) {
        if (isMessageValid(sendAnyway)) {
            if (mMessageTitleEditText.getText().toString().equals("")) {
                DialogUtils.Companion.showInfoDialogWithTwoButtons(ComposeMessageActivity.this,
                        getString(R.string.compose),
                        getString(R.string.no_subject),
                        getString(R.string.no),
                        getString(R.string.yes),
                        unit -> {
                            UiUtil.hideKeyboard(this);
                            composeMessageViewModel.finishBuildingMessage(mComposeBodyEditText.getText().toString());
                            ProtonMailApplication.getApplication().resetDraftCreated();
                            return unit;
                        }, true);
            } else {
                UiUtil.hideKeyboard(this);
                composeMessageViewModel.finishBuildingMessage(mComposeBodyEditText.getText().toString());
                ProtonMailApplication.getApplication().resetDraftCreated();
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
                    emailList.add(token.getAddress());
                } else {
                    List<MessageRecipient> groupRecipients = token.getGroupRecipients();
                    for (MessageRecipient recipient : groupRecipients) {
                        emailList.add(recipient.getAddress());
                    }
                }

                FetchPublicKeysRequest emailKeysRequest = new FetchPublicKeysRequest(emailList, location, false);
                composeMessageViewModel.startFetchPublicKeys(Collections.singletonList(emailKeysRequest));
                GetSendPreferenceJob.Destination destination = GetSendPreferenceJob.Destination.TO;
                if (recipientsView.equals(mCcRecipientsView)) {
                    destination = GetSendPreferenceJob.Destination.CC;
                } else if (recipientsView.equals(mBccRecipientsView)) {
                    destination = GetSendPreferenceJob.Destination.BCC;
                }
                composeMessageViewModel.setIsDirty(true);
                composeMessageViewModel.startSendPreferenceJob(emailList, destination);
            }

            @Override
            public void onTokenRemoved(MessageRecipient token) {

                composeMessageViewModel.setIsDirty(true);
                recipientsView.removeKey(token.getAddress());
                recipientsView.removeToken(token.getAddress());
            }
        });
    }

    private void renderViews() {
        setAdditionalRowVisibility(mAreAdditionalRowsVisible);
        mSetMessagePasswordButton.setImageLevel(mSetMessagePasswordButton.isPasswordSet() ? 1 : 0);
        mSetMessageExpirationImageButton.setImageLevel(mMessageExpirationView.getExpirationTime() > 0 ? 1 : 0);
        int attachmentsListSize = composeMessageViewModel.getMessageDataResult().getAttachmentList().size();
        mAttachmentCountTextView.setText(String.valueOf(attachmentsListSize));
        mAttachmentCountTextView.setVisibility(attachmentsListSize > 0 ? View.VISIBLE : View.GONE);
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
                String email = recipient.getAddress();
                SendPreference preference = composeMessageViewModel.getMessageDataResult().getSendPreferences().get(email);
                if (preference != null) {
                    setRecipientIconAndDescription(preference, recipientsView);
                }
            }
        }
    }

    private void fillMessageFromUserInputs(@NonNull Message message, boolean isDraft) {
        message.setMessageId(composeMessageViewModel.getDraftId());
        String subject = mMessageTitleEditText.getText().toString();
        if (TextUtils.isEmpty(subject)) {
            subject = getString(R.string.empty_subject);
        } else {
            subject = subject.replaceAll("\n", " ");
        }
        message.setSubject(subject);
        User user = mUserManager.getUser();
        if (!TextUtils.isEmpty(composeMessageViewModel.getMessageDataResult().getAddressId())) {
            message.setAddressID(composeMessageViewModel.getMessageDataResult().getAddressId());
        } else if (user != null) {
            int actionType = composeMessageViewModel.getActionType().ordinal();
            Constants.MessageActionType messageActionType = Constants.MessageActionType.Companion.fromInt(actionType);
            if (messageActionType != Constants.MessageActionType.REPLY && messageActionType != Constants.MessageActionType.REPLY_ALL) {
                try {
                    message.setAddressID(user.getSenderAddressIdByEmail((String) mAddressesSpinner.getSelectedItem()));
                    message.setSenderName(user.getSenderAddressNameByEmail((String) mAddressesSpinner.getSelectedItem()));
                } catch (Exception exc) {
                    Timber.tag("588").e(exc, "Exception on fill message with user inputs");
                }
            } else {
                message.setAddressID(user.getAddressId());
                message.setSenderName(user.getDisplayName());
            }
        }
        message.setToList(mToRecipientsView.getMessageRecipients());
        message.setCcList(mCcRecipientsView.getMessageRecipients());
        message.setBccList(mBccRecipientsView.getMessageRecipients());
        message.setDecryptedBody(composeMessageViewModel.getMessageDataResult().getContent());
        message.setEmbeddedImagesArray(composeMessageViewModel.getMessageDataResult().getContent());
        message.setIsEncrypted(MessageEncryption.INTERNAL);
        message.setLabelIDs(message.getAllLabelIDs());
        message.setInline(composeMessageViewModel.getMessageDataResult().isRespondInlineChecked());
        if (isDraft) {
            message.setIsRead(true);
            message.setLabelIDs(Arrays.asList(String.valueOf(Constants.MessageLocationType.ALL_DRAFT.getMessageLocationTypeValue()),
                    String.valueOf(Constants.MessageLocationType.ALL_MAIL.getMessageLocationTypeValue()),
                    String.valueOf(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue())));
            message.setLocation(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue());
            message.setTime(ServerTime.currentTimeMillis() / 1000);
            message.setIsEncrypted(MessageEncryption.INTERNAL);
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

        mToRecipientsView.clearFocus();
        mCcRecipientsView.clearFocus();
        mBccRecipientsView.clearFocus();
        if (isInvalidRecipientPresent(mToRecipientsView, mCcRecipientsView, mBccRecipientsView)) {
            return false;
        }
        int totalRecipients = mToRecipientsView.getRecipientCount() + mCcRecipientsView.getRecipientCount() + mBccRecipientsView
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
        if (mSetMessagePasswordButton.isPasswordSet()) {
            List<String> toMissingKeys = mToRecipientsView.addressesWithMissingKeys();
            List<String> ccMissingKeys = mCcRecipientsView.addressesWithMissingKeys();
            List<String> bccMissingKeys = mBccRecipientsView.addressesWithMissingKeys();
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
                    mProgressView.setVisibility(View.VISIBLE);
                    mProgressSpinner.setVisibility(View.VISIBLE);
                    mSendingPressed = true;
                } else {
                    // TODO: 3/10/17 update with message can not send
                    TextExtensions.showToast(this, "Please send password encrypted messages when you have connection", Toast.LENGTH_SHORT);
                }
                return false;
            }
        }
        boolean includesNonProtonMailRecipient = includesNonProtonMailRecipient();
        if (!sendAnyway && includesNonProtonMailRecipient && mMessageExpirationView.getExpirationTime() > 0 && !mSetMessagePasswordButton.isPasswordSet()) {
            TextExtensions.showToast(this, R.string.no_password_specified, Toast.LENGTH_LONG, Gravity.CENTER);
            return false;
        }
        return true;
    }

    private boolean includesNonProtonMailRecipient() {

        return mToRecipientsView.includesNonProtonMailRecipient()
                || mCcRecipientsView.includesNonProtonMailRecipient()
                || mBccRecipientsView.includesNonProtonMailRecipient();
    }

    @OnClick(R.id.show_additional_rows)
    void toggleShowAdditionalRowsVisibility() {

        mAreAdditionalRowsVisible = !mAreAdditionalRowsVisible;
        setAdditionalRowVisibility(mAreAdditionalRowsVisible);
    }

    private void setAdditionalRowVisibility(boolean show) {

        mShowAdditionalRowsImageButton.setImageResource(show ? R.drawable.minus_compose : R.drawable.plus_compose);
        final int visibility = show ? View.VISIBLE : View.GONE;
        mCcRowView.setVisibility(visibility);
        mCcRowDividerView.setVisibility(visibility);
        mBccRowView.setVisibility(visibility);
        mBccRowDividerView.setVisibility(visibility);
    }

    @OnClick(R.id.to)
    public void onToClicked() {
        mToRecipientsView.requestFocus();
        UiUtil.toggleKeyboard(this, mToRecipientsView);
    }

    @OnClick(R.id.set_message_expiration)
    public void onSetMessageExpiration() {
        mMessageExpirationView.show();
    }

    private boolean addingMoreAttachments = false;

    @OnClick(R.id.add_attachments)
    public void onAddAttachments() {
        addingMoreAttachments = true;
        UiUtil.hideKeyboard(this);
        composeMessageViewModel.openAttachmentsScreen();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ADD_ATTACHMENTS && resultCode == RESULT_OK) {
            Timber.d("ComposeMessageAct.onActivityResult Received add attachment response with result OK");
            askForPermission = false;
            addingMoreAttachments = false;
            ArrayList<LocalAttachment> resultAttachmentList = data.getParcelableArrayListExtra(AddAttachmentsActivity.EXTRA_ATTACHMENT_LIST);
            ArrayList<LocalAttachment> listToSet = resultAttachmentList != null ? resultAttachmentList : new ArrayList<>();
            composeMessageViewModel.setAttachmentList(listToSet);
            composeMessageViewModel.setIsDirty(true);
            String draftId = data.getStringExtra(AddAttachmentsActivity.EXTRA_DRAFT_ID);
            String oldDraftId = composeMessageViewModel.getDraftId();
            if (!TextUtils.isEmpty(draftId) && !draftId.equals(oldDraftId)) {
                composeMessageViewModel.setDraftId(draftId);
                afterAttachmentsAdded();
            } else if (!TextUtils.isEmpty(oldDraftId)) {
                afterAttachmentsAdded();
            }
            composeMessageViewModel.setIsDirty(true);
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_VALIDATE_PIN) {
            // region pin results
            if (data.hasExtra(EXTRA_ATTACHMENT_IMPORT_EVENT)) {
                Object attachmentExtra = data.getSerializableExtra(EXTRA_ATTACHMENT_IMPORT_EVENT);
                if (attachmentExtra instanceof PostImportAttachmentEvent) {
                    onPostImportAttachmentEvent((PostImportAttachmentEvent) attachmentExtra);
                }
                composeMessageViewModel.setBeforeSaveDraft(false, mComposeBodyEditText.getText().toString());
            } else if (data.hasExtra(EXTRA_MESSAGE_DETAIL_EVENT) || data.hasExtra(EXTRA_DRAFT_DETAILS_EVENT) || data.hasExtra(EXTRA_DRAFT_CREATED_EVENT)) {
                FetchMessageDetailEvent messageDetailEvent = (FetchMessageDetailEvent) data.getSerializableExtra(EXTRA_MESSAGE_DETAIL_EVENT);
                FetchDraftDetailEvent draftDetailEvent = (FetchDraftDetailEvent) data.getSerializableExtra(EXTRA_DRAFT_DETAILS_EVENT);
                DraftCreatedEvent draftCreatedEvent = (DraftCreatedEvent) data.getSerializableExtra(EXTRA_DRAFT_CREATED_EVENT);
                if (messageDetailEvent != null) {
                    composeMessageViewModel.onFetchMessageDetailEvent(messageDetailEvent);
                }
                if (draftDetailEvent != null) {
                    onFetchDraftDetailEvent(draftDetailEvent);
                }
                if (draftCreatedEvent != null) {
                    onDraftCreatedEvent(draftCreatedEvent);
                }
            }
            mToRecipientsView.requestFocus();
            UiUtil.toggleKeyboard(this, mToRecipientsView);
            super.onActivityResult(requestCode, resultCode, data);
            // endregion
        } else {
            Timber.w("ComposeMessageAct.onActivityResult Received result not handled", requestCode, resultCode);
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void afterAttachmentsAdded() {
        composeMessageViewModel.setBeforeSaveDraft(false, mComposeBodyEditText.getText().toString());
        composeMessageViewModel.setIsDirty(true);
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
                if (!sendPreference.isPrimaryPinned() && sendPreference.hasPinnedKeys()) {
                    // send with untrusted key popup
                    DialogUtils.Companion.showInfoDialog(
                            ComposeMessageActivity.this,
                            getString(R.string.send_with_untrusted_key_title),
                            String.format(getString(R.string.send_with_untrusted_key_message), entry.getKey()),
                            unit -> unit);
                }
                boolean isVerified = sendPreference.isVerified();
                if (!isVerified) {
                    // send
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
                                if (recipientsView.equals(mCcRecipientsView)) {
                                    destination = GetSendPreferenceJob.Destination.CC;
                                } else if (recipientsView.equals(mBccRecipientsView)) {
                                    destination = GetSendPreferenceJob.Destination.BCC;
                                }
                                composeMessageViewModel.startResignContactJobJob(entry.getKey(), entry.getValue(), destination);
                                return unit;
                            },
                            false);
                }
                if (isVerified) {
                    setRecipientIconAndDescription(sendPreference, recipientsView);
                }
            }
            recipientsView.setSendPreferenceMap(composeMessageViewModel.getMessageDataResult().getSendPreferences(), mSetMessagePasswordButton.isPasswordSet());
        }
    }

    private MessageRecipientView getRecipientView(GetSendPreferenceJob.Destination destination) {

        switch (destination) {
            case TO:
                return mToRecipientsView;
            case CC:
                return mCcRecipientsView;
            case BCC:
                return mBccRecipientsView;
        }
        return mToRecipientsView;
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
        ComposerLockIcon lock = new ComposerLockIcon(sendPreference, mSetMessagePasswordButton.isPasswordSet());

        composeMessageViewModel.addSendPreferences(sendPreference);
        recipientView.setIconAndDescription(email, lock.getIcon(), lock.getColor(), lock.getTooltip(), sendPreference.isPGP());
    }

    @Subscribe
    public void onMessageSavedEvent(MessageSavedEvent event) {
        if (event.status == Status.NO_NETWORK) {
            TextExtensions.showToast(this, R.string.no_network_queued);
        }
        finishActivity();
    }

    private void finishActivity() {
        setResult(RESULT_OK);
        saveLastInteraction();
        finish();
        if (!TextUtils.isEmpty(mAction)) {
            Intent home = AppUtil.decorInAppIntent(new Intent(this, MailboxActivity.class));
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
        }
    }

    @Subscribe
    public void onPostLoadContactsEvent(PostLoadContactsEvent event) {
        mMessageRecipientViewAdapter.setData(event.recipients);
    }

    @Subscribe
    public void onFetchDraftDetailEvent(FetchDraftDetailEvent event) {
        mProgressView.setVisibility(View.GONE);
        if (event.success) {
            composeMessageViewModel.initSignatures();
            composeMessageViewModel.processSignature();
            onMessageLoaded(event.getMessage(), true, true);
        } else {
            if (!isFinishing()) {
                DialogUtils.Companion.showInfoDialog(ComposeMessageActivity.this, getString(R.string.app_name), getString(R.string.messages_load_failure),
                        unit -> {
                            onBackPressed();
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
            mScrollContentView.setVisibility(View.VISIBLE);
            mProgressView.setVisibility(View.GONE);
        }
        AddressCrypto crypto = Crypto.forAddress(mUserManager, mUserManager.getUsername(), loadedMessage.getAddressID());
        if (updateAttachments) {
            composeMessageViewModel.createLocalAttachments(loadedMessage);
        }
        if (!renderBody) {
            return;
        }
        if (loadedMessage.getToList().size() != 0) {
            mToRecipientsView.clear();
            addRecipientsToView(loadedMessage.getToList(), mToRecipientsView);
        }
        if (loadedMessage.getCcList().size() != 0) {
            mCcRecipientsView.clear();
            addRecipientsToView(loadedMessage.getCcList(), mCcRecipientsView);
            mAreAdditionalRowsVisible = true;
        }
        if (loadedMessage.getBccList().size() != 0) {
            mBccRecipientsView.clear();
            addRecipientsToView(loadedMessage.getBccList(), mBccRecipientsView);
            mAreAdditionalRowsVisible = true;
        }
        mMessageTitleEditText.setText(loadedMessage.getSubject());
        String messageBody = loadedMessage.getMessageBody();
        try {
            TextDecryptionResult tct = crypto.decrypt(new CipherText(messageBody));
            messageBody = tct.getDecryptedData();
        } catch (Exception e) {
            Timber.e(e, "Decryption error");
        }
        composeMessageViewModel.setInitialMessageContent(messageBody);
        if (loadedMessage.isInline()) {
            String mimeType = loadedMessage.getMimeType();
            setInlineContent(messageBody, false, mimeType != null && mimeType.equals(Constants.MIME_TYPE_PLAIN_TEXT));
        } else {
            mWebViewContainer.setVisibility(View.VISIBLE);
            mMessageBody.setVisibility(View.VISIBLE);
            composeMessageViewModel.setIsMessageBodyVisible(true);
            mMessageBody.setFocusable(false);
            mMessageBody.requestFocus();
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
                mMessageBody.setVisibility(View.VISIBLE);
                composeMessageViewModel.setIsMessageBodyVisible(true);
                mMessageBody.setFocusable(false);
                mMessageBody.requestFocus();
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
            Spanned secondPart = UiUtil.fromHtml(messageBody.substring(messageBody.indexOf(originalMessageDividerString)));
            mQuotedHeaderTextView.setText(secondPart);
            composeMessageViewModel.setQuotedHeader(secondPart);
            messageBody = messageBody.substring(0, messageBody.indexOf(originalMessageDividerString));
        }
        setRespondInlineVisibility(false);
        mMessageBody.setVisibility(View.GONE);
        composeMessageViewModel.setIsMessageBodyVisible(false);
        mComposeBodyEditText.setVisibility(View.VISIBLE);
        composeMessageViewModel.setContent(messageBody);
        setBodyContent(true, isPlainText);
    }

    @Override
    public void onMessagePasswordChanged() {

        renderViews();
    }

    @Override
    public void onMessageExpirationChanged() {

        renderViews();
    }

    private void addRecipientsToView(ArrayList<String> recipients, MessageRecipientView messageRecipientView) {
        for (String recipient : recipients) {
            if (CommonExtensionsKt.isValidEmail(recipient)) {
                messageRecipientView.addObject(new MessageRecipient("", recipient));
            } else {
                String message = getString(R.string.invalid_email_address_removed, recipient);
                TextExtensions.showToast(this, message);
            }
        }
    }

    private Map<MessageRecipientView, List<MessageRecipient>> pendingRecipients = new HashMap<>();

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
            if (!CommonExtensionsKt.isValidEmail(recipient.getAddress())) {
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
                messageRecipientView.addObject(new MessageRecipient("", recipient.getAddress()));
            }
        }
        for (Map.Entry<String, List<MessageRecipient>> entry : groupedRecipients.entrySet()) {
            List<MessageRecipient> groupRecipients = entry.getValue();
            String groupName = entry.getKey();
            ContactLabel group = composeMessageViewModel.getContactGroupByName(groupName);
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
                                if (currentMR.getAddress().equals(groupMR.getAddress())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                groupRecipientsIterator.remove();
                                messageRecipientView.addObject(new MessageRecipient("", currentMR.getAddress()));
                            }
                        }
                    }
                }
                MessageRecipient recipient = new MessageRecipient(name, "");
                recipient.setGroup(groupName);
                recipient.setGroupIcon( R.string.contact_group_groups_icon);
                recipient.setGroupColor(Color.parseColor(UiUtil.normalizeColor(group.getColor())));
                recipient.setGroupRecipients(groupRecipients);
                messageRecipientView.addObject(recipient);
            }
        }
    }

    private void setBodyContent(boolean respondInline, boolean isPlainText) {

        String content = composeMessageViewModel.getMessageDataResult().getContent();
        if (!respondInline) {
            String css = AppUtil.readTxt(this, R.raw.editor);
            Transformer viewportTransformer = new ViewportTransformer(UiUtil.getRenderWidth(getWindowManager()), css);
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
            pmWebViewClient.blockRemoteResources(!composeMessageViewModel.getMessageDataResult().getShowRemoteContent());
            mMessageBody.loadDataWithBaseURL("", content, "text/html", HTTP.UTF_8, "");
        } else {
            final CharSequence bodyText;
            if (isPlainText) {
                bodyText = content;
            } else {
                bodyText = UiUtil.fromHtml(content);
            }
            mComposeBodyEditText.setText(bodyText);
        }
    }

    private void setMessageBodyInContainers(ComposeMessageViewModel.MessageBodySetup messageBodySetup) {
        mComposeBodyEditText.setText(messageBodySetup.getComposeBody());
        mWebViewContainer.setVisibility(messageBodySetup.getWebViewVisibility() ? View.VISIBLE : View.GONE);
        mQuotedHeaderTextView.setText(composeMessageViewModel.getMessageDataResult().getQuotedHeader());
        setRespondInlineVisibility(messageBodySetup.getRespondInlineVisibility());
        setBodyContent(messageBodySetup.getRespondInline(), messageBodySetup.isPlainText());
    }

    @Subscribe
    public void onDownloadEmbeddedImagesEvent(DownloadEmbeddedImagesEvent event) {
        if (event.getStatus().equals(Status.SUCCESS)) {
            Timber.v("onDownloadEmbeddedImagesEvent %s", event.getStatus());
            String content = composeMessageViewModel.getMessageDataResult().getContent();
            String css = AppUtil.readTxt(this, R.raw.editor);
            Transformer contentTransformer = new ViewportTransformer(UiUtil.getRenderWidth(getWindowManager()), css);
            Document doc = Jsoup.parse(content);
            doc.outputSettings().indentAmount(0).prettyPrint(false);
            doc = contentTransformer.transform(doc);
            content = doc.toString();
            pmWebViewClient.blockRemoteResources(!composeMessageViewModel.getMessageDataResult().getShowRemoteContent());
            EmbeddedImagesThread mEmbeddedImagesTask = new EmbeddedImagesThread(new WeakReference<>(ComposeMessageActivity.this.mMessageBody), event, content);
            mEmbeddedImagesTask.execute();
        }
    }

    private boolean mSendingInProgress;

    private void loadPMContacts() {
        if (mUserManager.getUser().getCombinedContacts()) {
            composeMessageViewModel.getMergedContactsLiveData().observe(this, messageRecipients -> {
                mMessageRecipientViewAdapter.setData(messageRecipients);
            });
            List<String> usernames = AccountManager.Companion.getInstance(this).getLoggedInUsers();
            for (String username : usernames) {
                composeMessageViewModel.fetchContactGroups(username);
            }
            composeMessageViewModel.loadPMContacts();
        } else {

            composeMessageViewModel.getContactGroupsResult().observe(this, messageRecipients -> {
                mMessageRecipientViewAdapter.setData(messageRecipients);
            });
            composeMessageViewModel.fetchContactGroups(mUserManager.getUsername());
            composeMessageViewModel.getPmMessageRecipientsResult().observe(this, messageRecipients -> {
                mMessageRecipientViewAdapter.setData(messageRecipients);
            });
            composeMessageViewModel.loadPMContacts();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                getSupportLoaderManager().initLoader(LOADER_ID_ANDROID_CONTACTS, null, this);
            }
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
                mMessageRecipientViewAdapter.setData(messageRecipients); // no groups
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

        DialogUtils.Companion.warningDialog(ComposeMessageActivity.this, getString(R.string.dont_remind_again), getString(R.string.okay), String.format(getString(R.string.pm_me_changed), address),
                unit -> {
                    final SharedPreferences prefs = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
                    prefs.edit().putBoolean(Constants.Prefs.PREF_PM_ADDRESS_CHANGED, true).apply();
                    return unit;
                });
    }

    private void showExpirationTimeError(List<String> recipientsMissingPassword, List<String> recipientsDisablePgp) {
        UiUtil.buildExpirationTimeErrorDialog(this, recipientsMissingPassword, recipientsDisablePgp, v -> sendMessage(true));
    }

    private class KeyboardManager implements ViewTreeObserver.OnGlobalLayoutListener {
        @Override
        public void onGlobalLayout() {

            int heightDiff = mRootLayout.getRootView().getHeight() - mRootLayout.getHeight();
            if (heightDiff > 150) {
                mDummyKeyboardView.setVisibility(View.VISIBLE);
            } else {
                mDummyKeyboardView.setVisibility(View.GONE);
            }
        }
    }

    private class ComposeBodyChangeListener implements View.OnKeyListener {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            if (keyCode != KeyEvent.KEYCODE_BACK) {
                composeMessageViewModel.setIsDirty(true);
            }
            return false;
        }
    }

    private class ParentViewScrollListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            mComposeBodyEditText.setFocusableInTouchMode(true);
            mComposeBodyEditText.requestFocus();
            return false;
        }
    }

    private class RespondInlineButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            composeMessageViewModel.setRespondInline(true);
            setRespondInlineVisibility(false);
            mMessageBody.setVisibility(View.GONE);
            composeMessageViewModel.setIsMessageBodyVisible(false);
            mMessageBody.loadData("", "text/html; charset=utf-8", HTTP.UTF_8);
            String composeContentBuilder = mComposeBodyEditText.getText().toString() + System.getProperty("line.separator") + mQuotedHeaderTextView.getText().toString() +
                    UiUtil.fromHtml((composeMessageViewModel.getMessageDataResult().getInitialMessageContent()));
            mComposeBodyEditText.setText(composeContentBuilder);
        }
    }

    private class MessageTitleEditorActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

            mMessageTitleEditText.clearFocus();
            mComposeBodyEditText.requestFocus();
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

            final ViewTreeObserver viewTreeObserver = mAddressesSpinner.getViewTreeObserver();
            viewTreeObserver.removeOnGlobalLayoutListener(this);
            skipInitial = 0;
            mComposeBodyEditText.addTextChangedListener(typingListener);
            mMessageTitleEditText.addTextChangedListener(typingListener);
            mAddressesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!allowSpinnerListener) {
                        return;
                    }
                    composeMessageViewModel.setIsDirty(true);
                    String email = (String) mAddressesSpinner.getItemAtPosition(position);
                    boolean localAttachmentsListEmpty = composeMessageViewModel.getMessageDataResult().getAttachmentList().isEmpty();
                    if (!composeMessageViewModel.isPaidUser() && MessageUtils.INSTANCE.isPmMeAddress(email)) {
                        mAddressesSpinner.setSelection(mSelectedAddressPosition);
                        Snackbar snack = Snackbar.make(mRootLayout, String.format(getString(R.string.error_can_not_send_from_this_address), email), Snackbar.LENGTH_LONG);
                        View snackView = snack.getView();
                        TextView tv = snackView.findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextColor(Color.WHITE);
                        snack.show();
                    } else {
                        String previousSenderAddressId = composeMessageViewModel.getMessageDataResult().getAddressId();
                        if (TextUtils.isEmpty(previousSenderAddressId)) { // first switch from default address
                            previousSenderAddressId = mUserManager.getUser().getDefaultAddress().getID();
                        }

                        if (TextUtils.isEmpty(composeMessageViewModel.getOldSenderAddressId()) && !localAttachmentsListEmpty) {
                            composeMessageViewModel.setOldSenderAddressId(previousSenderAddressId); // only set OldSenderAddressId if it was empty in ViewModel
                        }

                        composeMessageViewModel.setSenderAddressIdByEmail(email);
                        Address address = composeMessageViewModel.getAddressById();
                        if (address.getSend() == 0) {
                            mAddressesSpinner.setSelection(mSelectedAddressPosition);
                            showCanNotSendDialog(address.getEmail());
                            return;
                        }
                        String currentMail = mComposeBodyEditText.getText().toString();
                        String newSignature = composeMessageViewModel.getNewSignature();
                        boolean newSignatureEmpty = TextUtils.isEmpty(newSignature) || TextUtils.isEmpty(UiUtil.fromHtml(newSignature).toString().trim());
                        boolean signatureEmpty = TextUtils.isEmpty(composeMessageViewModel.getMessageDataResult().getSignature()) || TextUtils.isEmpty(UiUtil.fromHtml(composeMessageViewModel.getMessageDataResult().getSignature()).toString().trim());
                        if (!newSignatureEmpty && !signatureEmpty) {
                            newSignature = composeMessageViewModel.calculateSignature(newSignature);
                            mComposeBodyEditText.setText(currentMail.replace(UiUtil.fromHtml(composeMessageViewModel.getMessageDataResult().getSignature()), UiUtil.fromHtml(newSignature)));
                            composeMessageViewModel.processSignature(newSignature);
                        } else if (newSignatureEmpty && !signatureEmpty) {
                            if (currentMail.contains(composeMessageViewModel.getMessageDataResult().getSignature())) {
                                mComposeBodyEditText.setText(currentMail.replace("\n\n\n" + composeMessageViewModel.getMessageDataResult().getSignature(), ""));
                            } else {
                                if (currentMail.contains(UiUtil.fromHtml(composeMessageViewModel.getMessageDataResult().getSignature().trim()))) {
                                    mComposeBodyEditText.setText(currentMail.replace("\n\n\n" + UiUtil.fromHtml(composeMessageViewModel.getMessageDataResult().getSignature().trim()), ""));
                                } else {
                                    mComposeBodyEditText.setText(currentMail.replace(UiUtil.fromHtml(composeMessageViewModel.getMessageDataResult().getSignature()), ""));
                                }
                            }
                            composeMessageViewModel.setSignature("");
                        } else if (!newSignatureEmpty && signatureEmpty) {
                            newSignature = composeMessageViewModel.calculateSignature(newSignature).trim();
                            StringBuilder sb = new StringBuilder(currentMail);
                            int lastIndexSpace = currentMail.indexOf("\n\n\n");
                            if (lastIndexSpace != -1) {
                                sb.insert(lastIndexSpace, "\n\n\n" + newSignature);
                            } else {
                                sb.append("\n\n\n" + newSignature);
                            }
                            mComposeBodyEditText.setText(UiUtil.fromHtml(sb.toString().replace("\n", newline)));
                            composeMessageViewModel.processSignature(newSignature);
                        }
//                        composeMessageViewModel.setBeforeSaveDraft(true, mComposeBodyEditText.getText().toString());
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
                Timber.w("Error while saving message. DbId is null.");
                TextExtensions.showToast(ComposeMessageActivity.this, R.string.error_saving_try_again);
            } else {
                TextExtensions.showToast(ComposeMessageActivity.this, sendingToast);
                new Handler(Looper.getMainLooper()).postDelayed(ComposeMessageActivity.this::finishActivity, 500);
            }
        }
    }

    private class OnDraftCreatedObserver implements Observer<Message> {
        private final boolean updateAttachments;

        OnDraftCreatedObserver(boolean updateAttachments) {
            this.updateAttachments = updateAttachments;
        }

        @Override
        public void onChanged(@Nullable Message message) {
            onMessageLoaded(message, false, updateAttachments && composeMessageViewModel.getMessageDataResult().getAttachmentList().isEmpty());
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
        private final String type;

        LoadDraftObserver(Bundle extras, Intent intent, String type) {
            this.extras = extras;
            this.intent = intent;
            this.type = type;
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
            initialiseMessageBody(intent, extras, type, content, composerContent);
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
                composeMessageViewModel.setBeforeSaveDraft(false, mComposeBodyEditText.getText().toString());
                composeMessageViewModel.setIsDirty(true);
                renderViews();
            }
        }
    }

    private class BuildObserver implements Observer<Event<Message>> {

        @Override
        public void onChanged(@Nullable Event<Message> messageEvent) {

            Message localMessage = messageEvent.getContentIfNotHandled();
            if (localMessage != null) {
                composeMessageViewModel.setOfflineDraftSaved(false);

                String aliasAddress = composeMessageViewModel.getMessageDataResult().getAddressEmailAlias();
                MessageSender messageSender;
                if (aliasAddress != null && aliasAddress.equals(mAddressesSpinner.getSelectedItem())) { // it's being sent by alias
                    messageSender = new MessageSender(mUserManager.getUser().getDisplayNameForAddress(composeMessageViewModel.getMessageDataResult().getAddressId()), composeMessageViewModel.getMessageDataResult().getAddressEmailAlias());
                } else {
                    Address nonAliasAddress;
                    try {
                        if (localMessage.getAddressID() != null) {
                            nonAliasAddress = mUserManager.getUser().getAddressById(localMessage.getAddressID());
                        } else { // fallback to default address if newly composed message has no addressId
                            nonAliasAddress = mUserManager.getUser().getDefaultAddress();
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
                composeMessageViewModel.saveDraft(localMessage, composeMessageViewModel.getParentId(), mNetworkUtil.isConnected());
                new Handler(Looper.getMainLooper()).postDelayed(() -> disableSendButton(false), 500);
                if (userAction == UserAction.SAVE_DRAFT_EXIT) {
                    finishActivity();
                }
            } else if (composeMessageViewModel.getActionType() == UserAction.FINISH_EDIT) {
                mSendingInProgress = true;
                //region prepare sending message
                composeMessageViewModel.setMessagePassword(mSetMessagePasswordButton.getMessagePassword(), mSetMessagePasswordButton.getPasswordHint(), mSetMessagePasswordButton.isValid(), mMessageExpirationView.getExpirationTime(), mRespondInlineButton.getVisibility() == View.VISIBLE);
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
        if (ActivityCompat.checkSelfPermission(ComposeMessageActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && !askForPermission) {
            UiUtil.hideKeyboard(this);
        } else if (ActivityCompat.checkSelfPermission(ComposeMessageActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                && !askForPermission) {
            UiUtil.hideKeyboard(this);
        } else {
            mToRecipientsView.requestFocus();
            UiUtil.toggleKeyboard(this, mToRecipientsView);
        }
    }

    private void disableSendButton(boolean disable) {
        // Find the menu item you want to style
        if (menu != null) {
            MenuItem item = menu.getItem(0);
            if (disable) {
                item.setEnabled(false);
                item.getIcon().setColorFilter(getResources().getColor(R.color.white_30), PorterDuff.Mode.MULTIPLY);
            } else {
                item.setEnabled(true);
                item.getIcon().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.MULTIPLY);
            }
        }
    }
}
