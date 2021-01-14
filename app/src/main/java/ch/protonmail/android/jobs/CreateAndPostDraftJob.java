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
package ch.protonmail.android.jobs;

import android.text.TextUtils;
import android.util.Base64;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.api.models.DraftBody;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.messages.receive.AttachmentFactory;
import ch.protonmail.android.api.models.messages.receive.MessageFactory;
import ch.protonmail.android.api.models.messages.receive.MessageResponse;
import ch.protonmail.android.api.models.messages.receive.MessageSenderFactory;
import ch.protonmail.android.api.models.messages.receive.ServerMessage;
import ch.protonmail.android.api.models.messages.receive.ServerMessageSender;
import ch.protonmail.android.api.models.room.messages.Attachment;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.models.room.messages.MessageSender;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory;
import ch.protonmail.android.api.models.room.pendingActions.PendingSend;
import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.crypto.AddressCrypto;
import ch.protonmail.android.crypto.Crypto;
import ch.protonmail.android.domain.entity.user.AddressKeys;
import ch.protonmail.android.events.AttachmentFailedEvent;
import ch.protonmail.android.events.DraftCreatedEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;

public class CreateAndPostDraftJob extends ProtonMailBaseJob {

    private static final String TAG_CREATE_AND_POST_DRAFT_JOB = "CreateAndPostDraftJob";
    private static final int CREATE_DRAFT_RETRY_LIMIT = 10;

    private Long mDbMessageId;
    private final String mParentId;
    private final Constants.MessageActionType mActionType;
    private final boolean mUploadAttachments;
    private final List<String> mNewAttachments;
    private final String mOldSenderAddressID;
    private final String oldId;
    private final boolean isTransient;
    private final String mUsername;

    public CreateAndPostDraftJob(@NonNull Long dbMessageId, String localMessageId, String parentId, Constants.MessageActionType actionType,
                                 boolean uploadAttachments, @NonNull List<String> newAttachments, String oldSenderId, boolean isTransient, String username) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_SENDING));
        mDbMessageId = dbMessageId;
        oldId = localMessageId;
        mParentId = parentId;
        mActionType = actionType;
        mUploadAttachments = uploadAttachments;
        mNewAttachments = newAttachments;
        mOldSenderAddressID = oldSenderId;
        this.isTransient = isTransient;
        mUsername = username;
    }

    @Override
    protected int getRetryLimit() {
        return CREATE_DRAFT_RETRY_LIMIT;
    }

    @Override
    protected void onProtonCancel(int cancelReason, @Nullable Throwable throwable) {
        PendingActionsDatabase pendingActionsDatabase = PendingActionsDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        pendingActionsDatabase.deletePendingDraftById(mDbMessageId);
    }

    @Override
    public void onRun() throws Throwable {
        getMessageDetailsRepository().reloadDependenciesForUser(mUsername);
        // first save draft with -ve messageId so it won't overwrite any message
        MessagesDatabase messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        MessagesDatabase searchDatabase = MessagesDatabaseFactory.Companion.getSearchDatabase(getApplicationContext()).getDatabase();
        PendingActionsDatabase pendingActionsDatabase = PendingActionsDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();

        Message message = getMessageDetailsRepository().findMessageByMessageDbId(mDbMessageId);
        PendingSend pendingForSending = pendingActionsDatabase.findPendingSendByDbId(message.getDbId());

        if (pendingForSending != null) {
            return; // sending already pressed and in process, so no need to create draft, it will be created from the post send job
        }

        message.setLocation(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue());
        AttachmentFactory attachmentFactory = new AttachmentFactory();
        MessageSenderFactory messageSenderFactory = new MessageSenderFactory();
        MessageFactory messageFactory = new MessageFactory(attachmentFactory, messageSenderFactory);

        final ServerMessage serverMessage = messageFactory.createServerMessage(message);
        final DraftBody newDraft = new DraftBody(serverMessage);
        Message parentMessage = null;
        if (mParentId != null) {
            newDraft.setParentID(mParentId);
            newDraft.setAction(mActionType.getMessageActionTypeValue());
            if(!isTransient) {
                parentMessage = getMessageDetailsRepository().findMessageById(mParentId);
            } else {
                parentMessage = getMessageDetailsRepository().findSearchMessageById(mParentId);
            }
        }
        String addressId = message.getAddressID();
        String encryptedMessage = message.getMessageBody();
        if (!TextUtils.isEmpty(message.getMessageId())) {
            Message savedMessage = getMessageDetailsRepository().findMessageById(message.getMessageId());
            if (savedMessage != null) {
                encryptedMessage = savedMessage.getMessageBody();
            }
        }
        User user = getUserManager().getUser(mUsername);
        Address senderAddress = user.getAddressById(addressId);
        newDraft.getMessage().setSender(new ServerMessageSender(senderAddress.getDisplayName(), senderAddress.getEmail()));
        AddressCrypto crypto = Crypto.forAddress(getUserManager(), mUsername, message.getAddressID());
        newDraft.getMessage().setBody(encryptedMessage);
        List<Attachment> parentAttachmentList = null;
        if (parentMessage != null) {
            if(!isTransient) {
                parentAttachmentList = parentMessage.attachments(messagesDatabase);
            } else {
                parentAttachmentList = parentMessage.attachments(searchDatabase);
            }
        }
        if (parentAttachmentList != null) {
            updateAttachmentKeyPackets(parentAttachmentList, newDraft, mOldSenderAddressID, senderAddress);
        }
        if (message.getSenderEmail().contains("+")) { // it's being sent by alias
            newDraft.getMessage().setSender(new ServerMessageSender(message.getSenderName(), message.getSenderEmail()));
        }
        final MessageResponse draftResponse = getApi().createDraft(newDraft);
        // on success update draft with messageId

        String newId = draftResponse.getMessageId();
        Message draftMessage = draftResponse.getMessage();
        getApi().markMessageAsRead(new IDList(Collections.singletonList(newId)));
        draftMessage.setDbId(mDbMessageId);
        draftMessage.setToList(message.getToList());
        draftMessage.setCcList(message.getCcList());
        draftMessage.setBccList(message.getBccList());
        draftMessage.setReplyTos(message.getReplyTos());
        draftMessage.setSender(message.getSender());
        draftMessage.setLabelIDs(message.getEventLabelIDs());
        draftMessage.setParsedHeaders(message.getParsedHeaders());
        draftMessage.setDownloaded(true);
        draftMessage.setIsRead(true);
        draftMessage.setNumAttachments(message.getNumAttachments());
        draftMessage.setLocalId(oldId);

        for (Attachment atta : draftMessage.getAttachments()) {
            if (parentAttachmentList != null && !parentAttachmentList.isEmpty()) {
                for (Attachment parentAtta : parentAttachmentList) {
                    if (parentAtta.getKeyPackets().equals(atta.getKeyPackets())) {
                        atta.setInline(parentAtta.getInline());
                    }
                }
            }
        }
        getMessageDetailsRepository().saveMessageInDB(draftMessage);

        pendingForSending = pendingActionsDatabase.findPendingSendByOfflineMessageId(oldId);
        if (pendingForSending != null) {
            pendingForSending.setMessageId(newId);
            pendingActionsDatabase.insertPendingForSend(pendingForSending);
        }
        Message offlineDraft = getMessageDetailsRepository().findMessageById(oldId);
        if (offlineDraft != null) {
            getMessageDetailsRepository().deleteMessage(offlineDraft);
        }

        if (message.getNumAttachments() >= 1 && mUploadAttachments && !mNewAttachments.isEmpty()) {
            List<Attachment> listOfAttachments = new ArrayList<>();
            for (String attachmentId : mNewAttachments) {
                listOfAttachments.add(messagesDatabase.findAttachmentById(attachmentId));
            }
            getJobManager().addJob(new PostCreateDraftAttachmentsJob(newId, oldId, mUploadAttachments, listOfAttachments, crypto, mUsername));
        } else {
            DraftCreatedEvent draftCreatedEvent = new DraftCreatedEvent(message.getMessageId(), oldId, draftMessage);
            AppUtil.postEventOnUi(draftCreatedEvent);
        }
    }

    private void updateAttachmentKeyPackets(List<Attachment> attachmentList, DraftBody draftBody, String oldSenderAddress, Address newSenderAddress) throws Exception {
        if (!TextUtils.isEmpty(oldSenderAddress)) {
            AddressCrypto oldCrypto = Crypto.forAddress(getUserManager(), mUsername, oldSenderAddress);
            AddressKeys newAddressKeys = newSenderAddress.toNewAddress().getKeys();
            String newPublicKey = oldCrypto.buildArmoredPublicKey(newAddressKeys.getPrimaryKey().getPrivateKey());
            for (Attachment attachment : attachmentList) {
                if (mActionType == Constants.MessageActionType.FORWARD ||
                        ((mActionType == Constants.MessageActionType.REPLY || mActionType == Constants.MessageActionType.REPLY_ALL) && attachment.getInline())) {
                    String AttachmentID = attachment.getAttachmentId();
                    String keyPackets = attachment.getKeyPackets();
                    byte[] keyPackage = Base64.decode(keyPackets, Base64.DEFAULT);
                    byte[] sessionKey = oldCrypto.decryptKeyPacket(keyPackage);
                    byte[] newKeyPackage = oldCrypto.encryptKeyPacket(sessionKey, newPublicKey);
                    String newKeyPackets = Base64.encodeToString(newKeyPackage, Base64.NO_WRAP);
                    if (!TextUtils.isEmpty(keyPackets)) {
                        draftBody.getAttachmentKeyPackets().put(AttachmentID, newKeyPackets);
                    }
                }
            }
        } else {
            for (Attachment attachment : attachmentList) {
                if (mActionType == Constants.MessageActionType.FORWARD ||
                        ((mActionType == Constants.MessageActionType.REPLY || mActionType == Constants.MessageActionType.REPLY_ALL) && attachment.getInline())) {
                    String AttachmentID = attachment.getAttachmentId();
                    draftBody.getAttachmentKeyPackets().put(AttachmentID, attachment.getKeyPackets());
                }
            }
        }
    }

    private static class PostCreateDraftAttachmentsJob extends ProtonMailBaseJob {
        private final String mMessageId;
        private final String mOldMessageId;
        private final boolean mUploadAttachments;
        private final List<Attachment> mAttachments;
        private final AddressCrypto mCrypto;
        private final String mUsername;

        PostCreateDraftAttachmentsJob(String messageId, String oldMessageId, boolean uploadAttachments, List<Attachment> attachments, AddressCrypto crypto, String username) {
            super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
            mMessageId = messageId;
            mOldMessageId = oldMessageId;
            mUploadAttachments = uploadAttachments;
            mAttachments = attachments;
            mCrypto = crypto;
            mUsername = username;
        }

        @Override
        public void onRun() throws Throwable {
            PendingActionsDatabase pendingActionsDatabase = PendingActionsDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
            Message message = getMessageDetailsRepository().findMessageById(mMessageId);
            User user = getUserManager().getUser(mUsername);
            if (user == null) {
                pendingActionsDatabase.deletePendingUploadByMessageId(mMessageId, mOldMessageId);
                return;
            }
            if (message != null && mUploadAttachments && (mAttachments != null && mAttachments.size() > 0)) {
                //upload all attachments
                List<Attachment> messageAttachments = message.getAttachments();
                if (messageAttachments != null && mAttachments != null && mAttachments.size() > messageAttachments.size()) {
                    messageAttachments = mAttachments;
                }
                for (Attachment attachment : messageAttachments) {
                    try {
                        String filePath = attachment.getFilePath();
                        if (TextUtils.isEmpty(filePath)) {
                            // TODO: inform user that the attachment is not saved properly
                            continue;
                        }
                        final File file = new File(filePath);
                        if (!URLUtil.isDataUrl(filePath) && !file.exists()) {
                            continue;
                        }
                        if (attachment.isUploaded()) {
                            continue;
                        }
                        attachment.uploadAndSave(getMessageDetailsRepository(), getApi(), mCrypto);
                    } catch (Exception e) {
                        Logger.doLogException(TAG_CREATE_AND_POST_DRAFT_JOB, "error while attaching file: " + attachment.getFilePath(), e);
                        AppUtil.postEventOnUi(new AttachmentFailedEvent(message.getMessageId(), message.getSubject(), attachment.getFileName()));
                    }
                }
            }
            message.setNumAttachments(mAttachments.size());
            PendingSend pendingForSending = pendingActionsDatabase.findPendingSendByDbId(message.getDbId());

            if (pendingForSending == null) {
                getMessageDetailsRepository().saveMessageInDB(message);
            }
            getJobManager().addJob(new FetchMessageDetailJob(message.getMessageId()));
            pendingActionsDatabase.deletePendingUploadByMessageId(mMessageId, mOldMessageId);
            DraftCreatedEvent draftCreatedEvent = new DraftCreatedEvent(message.getMessageId(), mOldMessageId, message);
            AppUtil.postEventOnUi(draftCreatedEvent);
        }

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return RetryConstraint.createExponentialBackoff(runCount, 500);
    }
}
