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

import static ch.protonmail.android.attachments.ImportAttachmentsWorkerKt.KEY_INPUT_DATA_DELETE_ORIGINAL_FILE_BOOLEAN;
import static ch.protonmail.android.attachments.ImportAttachmentsWorkerKt.KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.adapters.AttachmentListAdapter;
import ch.protonmail.android.attachments.AttachmentsViewModel;
import ch.protonmail.android.attachments.AttachmentsViewState;
import ch.protonmail.android.attachments.ImportAttachmentsWorker;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.data.local.model.Attachment;
import ch.protonmail.android.data.local.model.LocalAttachment;
import ch.protonmail.android.events.DownloadedAttachmentEvent;
import ch.protonmail.android.events.PostImportAttachmentEvent;
import ch.protonmail.android.events.PostImportAttachmentFailureEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.DateUtil;
import ch.protonmail.android.utils.DownloadUtils;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;
import dagger.hilt.android.AndroidEntryPoint;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import timber.log.Timber;

@AndroidEntryPoint
public class AddAttachmentsActivity extends BaseStoragePermissionActivity implements AttachmentListAdapter.IAttachmentListener {

    private static final String TAG_ADD_ATTACHMENTS_ACTIVITY = "AddAttachmentsActivity";
    public static final String EXTRA_ATTACHMENT_LIST = "EXTRA_ATTACHMENT_LIST";
    public static final String EXTRA_DRAFT_ID = "EXTRA_DRAFT_ID";
    public static final String EXTRA_DRAFT_CREATED = "EXTRA_DRAFT_CREATED";
    private static final String ATTACHMENT_MIME_TYPE = "*/*";
    private static final int REQUEST_CODE_ATTACH_FILE = 1;
    private static final int REQUEST_CODE_TAKE_PHOTO = 2;
    private static final String STATE_PHOTO_PATH = "STATE_PATH_TO_PHOTO";

    private AttachmentListAdapter mAdapter;
    @BindView(R.id.progress_layout)
    View mProgressLayout;
    @BindView(R.id.processing_attachment_layout)
    View mProcessingAttachmentLayout;
    @BindView(R.id.no_attachments)
    View mNoAttachmentsView;
    @BindView(R.id.num_attachments)
    TextView mNumAttachmentsView;
    @BindView(R.id.attachment_list)
    ListView mListView;

    @Inject
    WorkManager workManager;
    @Inject
    DownloadUtils downloadUtils;

    private String mPathToPhoto;
    private String mDraftId;
    private boolean mDraftCreated;
    private List<Uri> mAttachFileWithoutPermission;
    private String mAttachTakePhotoWithoutPermission;
    private boolean openGallery = false;
    private boolean openCamera = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_add_attachments;
    }

    @Override
    public void onHasPermission(Constants.PermissionType type) {
        if (type == Constants.PermissionType.STORAGE) {
            super.onHasPermission(type);
            mHasStoragePermission = true;
        }
    }

    @Override
    public void onPermissionDenied(Constants.PermissionType type) {
        super.onPermissionDenied(type);
        openCamera = false;
        openGallery = false;
        DialogUtils.Companion.showInfoDialog(AddAttachmentsActivity.this, getString(R.string.need_permissions_title),
                getString(R.string.need_storage_permissions_add_attachment_text), unit -> unit);
    }

    @Override
    public void onPermissionConfirmed(Constants.PermissionType type) {
        super.onPermissionConfirmed(type);
        if (openGallery) {
            openGallery();
        }
        if (openCamera) {
            openCamera();
        }
    }

    @Override
    protected void storagePermissionGranted() {
        mHasStoragePermission = true;
        if (mAttachFileWithoutPermission != null && mAttachFileWithoutPermission.size() > 0) {
            mProcessingAttachmentLayout.setVisibility(View.VISIBLE);

            String[] uriStrings = new String[mAttachFileWithoutPermission.size()];
            for (int i = 0; i < mAttachFileWithoutPermission.size(); i++) {
                uriStrings[i] = mAttachFileWithoutPermission.get(i).toString();
            }

            Data workerData = new Data.Builder()
                    .putStringArray(KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY, uriStrings)
                    .build();

            OneTimeWorkRequest importAttachmentsWork = new OneTimeWorkRequest.Builder(ImportAttachmentsWorker.class)
                    .setInputData(workerData)
                    .build();

            workManager.enqueue(importAttachmentsWork);

            mAttachFileWithoutPermission = null;
        }
        if (!TextUtils.isEmpty(mAttachTakePhotoWithoutPermission)) {
            mProcessingAttachmentLayout.setVisibility(View.VISIBLE);
            handleTakePhotoRequest(mAttachTakePhotoWithoutPermission);
            mAttachTakePhotoWithoutPermission = null;
        }
    }

    @Override
    protected boolean checkForPermissionOnStartup() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.add_attachment);
        }

        Intent intent = getIntent();
        ArrayList<LocalAttachment> attachmentList = intent.getParcelableArrayListExtra(EXTRA_ATTACHMENT_LIST);
        if (attachmentList == null) {
            attachmentList = new ArrayList<>();
        }
        mDraftId = intent.getStringExtra(EXTRA_DRAFT_ID);
        mDraftCreated = intent.getBooleanExtra(EXTRA_DRAFT_CREATED, true);
        int attachmentsCount = attachmentList.size();
        int totalEmbeddedImages = countEmbeddedImages(attachmentList);
        updateAttachmentsCount(attachmentsCount, totalEmbeddedImages);
        if (mDraftCreated) {
            mProgressLayout.setVisibility(View.GONE);
        } else {
            mProgressLayout.setVisibility(View.VISIBLE);
        }
        mAdapter = new AttachmentListAdapter(this, attachmentList, totalEmbeddedImages,
                workManager);
        mListView.setAdapter(mAdapter);

        AttachmentsViewModel viewModel = new ViewModelProvider(this).get(AttachmentsViewModel.class);
        viewModel.init();

        viewModel.getViewState().observe(this, this::viewStateChanged);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_attachments, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.take_photo).setVisible(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA));
        menu.findItem(R.id.attach_file).setVisible(mDraftCreated);
        menu.findItem(R.id.take_photo).setVisible(mDraftCreated);
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DRAFT_ID, mDraftId);
        ArrayList<LocalAttachment> currentAttachments = mAdapter.getData();
        intent.putParcelableArrayListExtra(EXTRA_ATTACHMENT_LIST, currentAttachments);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.attach_file: {
                openGallery = true;
                if (mHasStoragePermission != null && mHasStoragePermission) {
                    return openGallery();
                } else {
                    storagePermissionHelper.checkPermission();
                    return false;
                }
            }
            case R.id.take_photo: {
                openCamera = true;
                if (mHasStoragePermission != null && mHasStoragePermission) {
                    return openCamera();
                } else {
                    storagePermissionHelper.checkPermission();
                    return false;
                }
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isAttachmentsSizeAllowed(long newAttachmentSie) {
        if (mAdapter == null) return false;

        long currentAttachmentsSize =
                CollectionsKt.sumOfLong(CollectionsKt.map(mAdapter.getData(), LocalAttachment::getSize));

        return currentAttachmentsSize + newAttachmentSie < Constants.MAX_ATTACHMENT_FILE_SIZE_IN_BYTES;
    }

    private boolean isAttachmentsCountAllowed() {
        return mAdapter != null && mAdapter.getCount() < Constants.MAX_ATTACHMENTS;
    }

    @Subscribe
    public void onPostImportAttachmentEvent(PostImportAttachmentEvent event) {
        mProcessingAttachmentLayout.setVisibility(View.GONE);
        mNoAttachmentsView.setVisibility(View.GONE);
        LocalAttachment newAttachment = new LocalAttachment(Uri.parse(event.uri), event.displayName, event.size, event.mimeType);
        ArrayList<LocalAttachment> currentAttachments = mAdapter.getData();
        boolean alreadyExists = false;
        for (LocalAttachment localAttachment : currentAttachments) {
            String localAttachmentDisplayName = localAttachment.getDisplayName();
            boolean isEmpty = TextUtils.isEmpty(localAttachmentDisplayName);
            if (!isEmpty && localAttachmentDisplayName.equals(newAttachment.getDisplayName())) {
                alreadyExists = true;
            }
        }
        if (alreadyExists) {
            TextExtensions.showToast(AddAttachmentsActivity.this, R.string.attachment_exists, Toast.LENGTH_SHORT);
            return;
        }
        currentAttachments.add(newAttachment);
        int totalEmbeddedImages = countEmbeddedImages(currentAttachments);
        mAdapter.updateData(new ArrayList<>(currentAttachments), totalEmbeddedImages);
        int attachments = currentAttachments.size();
        updateAttachmentsCount(attachments, totalEmbeddedImages);
    }

    @Subscribe
    public void onPostImportAttachmentFailureEvent(PostImportAttachmentFailureEvent event) {
        mProcessingAttachmentLayout.setVisibility(View.GONE);
        TextExtensions.showToast(this, R.string.problem_selecting_file);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (mHasStoragePermission == null || !mHasStoragePermission) {

            if (requestCode == REQUEST_CODE_ATTACH_FILE) {
                mAttachFileWithoutPermission = new ArrayList<>();
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        mAttachFileWithoutPermission.add(item.getUri());
                    }
                } else {
                    mAttachFileWithoutPermission.add(data.getData());
                }

            } else if (requestCode == REQUEST_CODE_TAKE_PHOTO) {
                mAttachTakePhotoWithoutPermission = mPathToPhoto;
            }

            storagePermissionHelper.checkPermission();
            return;
        }

        if (requestCode == REQUEST_CODE_ATTACH_FILE) {
            handleAttachFileRequest(data.getData(), data.getClipData());

        } else if (requestCode == REQUEST_CODE_TAKE_PHOTO) {
            handleTakePhotoRequest(mPathToPhoto);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_PHOTO_PATH, mPathToPhoto);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mPathToPhoto = savedInstanceState.getString(STATE_PHOTO_PATH);
    }

    @Subscribe
    public void onDownloadAttachmentEvent(DownloadedAttachmentEvent event) {
        //once attachment has been downloaded
        if (event.getStatus().equals(Status.SUCCESS)) {
            downloadUtils.viewAttachment(event.getFilename(), event.getAttachmentUri());
            TextExtensions.showToast(this, String.format(getString(R.string.attachment_download_success), event.getFilename()), Toast.LENGTH_SHORT);
        } else {
            TextExtensions.showToast(this, String.format(getString(R.string.attachment_download_failed), event.getFilename()), Toast.LENGTH_SHORT);
        }
    }

    private void viewStateChanged(AttachmentsViewState viewState) {
        if (viewState instanceof AttachmentsViewState.MissingConnectivity) {
            onMessageReady();
        }

        if (viewState instanceof AttachmentsViewState.UpdateAttachments) {
            onMessageReady();
            updateDisplayedAttachments(
                    ((AttachmentsViewState.UpdateAttachments) viewState).getAttachments()
            );
        }
    }

    private void onMessageReady() {
        mDraftCreated = true;
        mProgressLayout.setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    private void updateDisplayedAttachments(List<Attachment> attachments) {
        List<LocalAttachment> localAttachments = new ArrayList<>(
                LocalAttachment.Companion.createLocalAttachmentList(attachments)
        );
        int totalEmbeddedImages = countEmbeddedImages(localAttachments);
        mAdapter.updateData(new ArrayList(localAttachments), totalEmbeddedImages);
    }


    private void handleAttachFileRequest(Uri uri, ClipData clipData) {
        String[] uris = null;

        String uriString = uri != null ? uri.toString() : null;
        if (uriString != null) {
            uris = new String[]{uriString};
        }

        if (clipData != null) {
            uris = new String[clipData.getItemCount()];
            for (int i = 0; i < clipData.getItemCount(); i++) {
                uris[i] = clipData.getItemAt(i).getUri().toString();
            }
        }

        if (uris != null) {

            // region Check whether the size of the attachments is within bounds
            final AtomicLong incrementalSize = new AtomicLong(0);
            List<String> sizeComplaintUrisList = ArraysKt.filter(uris, fileUriString -> {
                Uri fileUri = Uri.parse(fileUriString);

                try {
                    ParcelFileDescriptor fileDescriptor = getContentResolver().openFileDescriptor(fileUri, "r");
                    if (fileDescriptor == null) {
                        return false;
                    }
                    final long fileSize = fileDescriptor.getStatSize();
                    return isAttachmentsSizeAllowed(incrementalSize.addAndGet(fileSize));

                } catch (FileNotFoundException e) {
                    return false;
                }
            });

            if (sizeComplaintUrisList.size() < uris.length) {
                TextExtensions.showToast(this, R.string.max_attachments_size_reached);
            }
            // endregion

            if (sizeComplaintUrisList.size() > 0) {
                mProcessingAttachmentLayout.setVisibility(View.VISIBLE);

                String[] sizeComplaintUris = new String[sizeComplaintUrisList.size()];
                sizeComplaintUrisList.toArray(sizeComplaintUris);
                Data workerData = new Data.Builder()
                        .putStringArray(KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY, sizeComplaintUris)
                        .build();

                OneTimeWorkRequest importAttachmentsWork = new OneTimeWorkRequest.Builder(ImportAttachmentsWorker.class)
                        .setInputData(workerData)
                        .build();

                workManager.enqueue(importAttachmentsWork);
            }
        }
    }

    private void handleTakePhotoRequest(String path) {
        if (!TextUtils.isEmpty(path)) {

            File file = new File(path);

            // Check whether the size of the attachment is within bounds
            if (!isAttachmentsSizeAllowed(file.length())) {
                TextExtensions.showToast(this, R.string.max_attachments_size_reached);
                return;
            }

            mProcessingAttachmentLayout.setVisibility(View.VISIBLE);

            Uri uri = Uri.fromFile(file);
            Data data = new Data.Builder()
                    .putStringArray(KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY, new String[]{uri.toString()})
                    .putBoolean(KEY_INPUT_DATA_DELETE_ORIGINAL_FILE_BOOLEAN, true)
                    .build();

            OneTimeWorkRequest importAttachmentsWork = new OneTimeWorkRequest.Builder(ImportAttachmentsWorker.class)
                    .setInputData(data)
                    .build();

            workManager.enqueue(importAttachmentsWork);

            workManager.getWorkInfoByIdLiveData(importAttachmentsWork.getId()).observe(this, workInfo -> {
                if (workInfo != null) {
                    Timber.d("ImportAttachmentsWorker workInfo = " + workInfo.getState());
                }
            });

        } else {
            TextExtensions.showToast(this, R.string.attaching_photo_failed, Toast.LENGTH_LONG, Gravity.CENTER);
        }
    }

    @Override
    public void onAttachmentDeleted(int remainingAttachments, int embeddedImagesCount) {
        updateAttachmentsCount(remainingAttachments, embeddedImagesCount);
    }

    @Override
    public void askStoragePermission() {
        storagePermissionHelper.checkPermission();
    }

    private void updateAttachmentsCount(int totalAttachmentsCount, int totalEmbeddedImagesCount) {
        if (totalAttachmentsCount == 0 && totalEmbeddedImagesCount == 0) {
            mNoAttachmentsView.postDelayed(() -> mNoAttachmentsView.setVisibility(View.VISIBLE), 350);
            mNumAttachmentsView.setVisibility(View.GONE);
        } else {
            int normalAttachments = totalAttachmentsCount - totalEmbeddedImagesCount;
            if (normalAttachments > 0) {
                mNumAttachmentsView.setText(getResources().getQuantityString(R.plurals.attachments, normalAttachments, normalAttachments));
                mNumAttachmentsView.setVisibility(View.VISIBLE);
            } else {
                mNumAttachmentsView.setText(getString(R.string.no_attachments));
                mNumAttachmentsView.setVisibility(View.VISIBLE);
            }
        }
    }

    //TODO extract logic to separate class as it is not dependant on activity
    private int countEmbeddedImages(List<LocalAttachment> attachments) {
        int embeddedImages = 0;
        for (LocalAttachment localAttachment : attachments) {
            if (localAttachment.isEmbeddedImage()) {
                embeddedImages++;
            }
        }
        return embeddedImages;
    }

    private boolean openGallery() {
        if (!isAttachmentsCountAllowed()) {
            TextExtensions.showToast(this, R.string.max_attachments_reached);
            return true;
        }
        Intent target = new Intent(Intent.ACTION_GET_CONTENT);
        target.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        target.addCategory(Intent.CATEGORY_OPENABLE);
        target.setType(ATTACHMENT_MIME_TYPE);
        Intent intent = Intent.createChooser(target, getString(R.string.select_file));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_ATTACH_FILE);
        } else {
            TextExtensions.showToast(this, R.string.no_application_found);
        }
        return true;
    }

    private boolean openCamera() {
        if (!isAttachmentsCountAllowed()) {
            TextExtensions.showToast(this, R.string.max_attachments_reached);
            return true;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            String timestamp = DateUtil.generateTimestamp();
            timestamp = timestamp.replace("-", "");
            timestamp = timestamp.replaceAll("[^A-Za-z0-9]", "");
            File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try {
                if (timestamp.length() < 3) {
                    Random random = new Random();
                    int number = random.nextInt(99999) + 100;
                    timestamp = timestamp + String.valueOf(number);
                }
                File file = File.createTempFile(timestamp, ".jpg", directory);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(AddAttachmentsActivity.this, getApplicationContext().getPackageName() + ".provider", file));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mPathToPhoto = file.getAbsolutePath();
                startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
            } catch (IOException ioe) {
                Logger.doLogException(TAG_ADD_ATTACHMENTS_ACTIVITY,
                        "Exception creating temporary file for photo", ioe);
                TextExtensions.showToast(this, R.string.problem_taking_photo);
            }
        }
        return true;
    }
}
