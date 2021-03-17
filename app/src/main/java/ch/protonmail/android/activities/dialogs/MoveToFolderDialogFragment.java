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
package ch.protonmail.android.activities.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.adapters.FoldersAdapter;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.data.local.MessageDao;
import ch.protonmail.android.data.local.MessageDatabase;
import ch.protonmail.android.data.local.model.Label;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MoveToFolderDialogFragment extends AbstractDialogFragment implements AdapterView.OnItemClickListener {

    @Inject
    UserManager userManager;

    private static final String ARGUMENT_MESSAGE_LOCATION = "ch.protonmail.android.ARG_LOCATION";

    @BindView(R.id.folders_list_view)
    ListView mList;
    @BindView(R.id.no_folders)
    View mNoFoldersView;
    private IMoveMessagesListener mMoveMessagesListener;
    private Constants.MessageLocationType mMailboxLocation;
    private FoldersAdapter mAdapter;

    /**
     * Instantiates a new fragment of this class.
     *
     * @return new instance of {@link MoveToFolderDialogFragment}
     */
    public static MoveToFolderDialogFragment newInstance(Constants.MessageLocationType mailboxLocation) {
        MoveToFolderDialogFragment fragment = new MoveToFolderDialogFragment();
        Bundle extras = new Bundle();
        extras.putInt(ARGUMENT_MESSAGE_LOCATION, mailboxLocation.getMessageLocationTypeValue());
        fragment.setArguments(extras);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mMoveMessagesListener = (IMoveMessagesListener) activity;
        } catch (ClassCastException e) {
            // not throwing error, since the user of this dialog is not obligated to listen for
            // labels state change
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getArguments();
        if (extras != null) {
            mMailboxLocation = Constants.MessageLocationType.Companion.fromInt(extras.getInt(ARGUMENT_MESSAGE_LOCATION));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        getDialog().setCanceledOnTouchOutside(true);
        return rootView;
    }

    private List<FoldersAdapter.FolderItem> prepareUI() {
        return buildDefaultFolders(mMailboxLocation != Constants.MessageLocationType.INBOX, mMailboxLocation != Constants.MessageLocationType.ARCHIVE,
                mMailboxLocation != Constants.MessageLocationType.SPAM, mMailboxLocation != Constants.MessageLocationType.TRASH && mMailboxLocation != Constants.MessageLocationType.DRAFT);
    }

    private FoldersAdapter.FolderItem createFolderItem(String labelId, String labelName,
                                                       String color,@DrawableRes int drawable) {
        Label inbox = new Label(labelId,labelName,color);
        return fromLabel(inbox, drawable);
    }

    //TODO refactor whole method including single call place
    private List<FoldersAdapter.FolderItem> buildDefaultFolders(boolean showInbox, boolean showArchive, boolean showSpam, boolean showTrash) {
        List<FoldersAdapter.FolderItem> defaultFoldersList = new ArrayList<>();
        final String defaultColor = "#555555";
        //TODO create custom class and use list not to duplicate defaultFoldersList.add(...)
        if (showInbox) {
            final String labelId = String.valueOf(Constants.MessageLocationType.INBOX.getMessageLocationTypeValue());
            final String labelName = getString(R.string.inbox);
            final int labelDrawable = R.drawable.inbox;
            defaultFoldersList.add(createFolderItem(labelId, labelName, defaultColor, labelDrawable));
        }

        if (showArchive) {
            final String labelId = String.valueOf(Constants.MessageLocationType.ARCHIVE.getMessageLocationTypeValue());
            final String labelName = getString(R.string.archive);
            final int labelDrawable = R.drawable.archive;
            defaultFoldersList.add(createFolderItem(labelId, labelName, defaultColor, labelDrawable));
        }

        if (showSpam) {
            final String labelId = String.valueOf(Constants.MessageLocationType.SPAM.getMessageLocationTypeValue());
            final String labelName = getString(R.string.spam);
            final int labelDrawable = R.drawable.spam;
            defaultFoldersList.add(createFolderItem(labelId, labelName, defaultColor, labelDrawable));
        }

        if (showTrash) {
            final String labelId = String.valueOf(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue());
            final String labelName = getString(R.string.trash);
            final int labelDrawable = R.drawable.trash;
            defaultFoldersList.add(createFolderItem(labelId, labelName, defaultColor, labelDrawable));
        }

        return defaultFoldersList;
    }

    private FoldersAdapter.FolderItem buildNewFolder() {
        // TODO extract strings
        Label newFolderLabel = new Label("100", getString(R.string.create_new_folder), "#555555");
        return fromLabel(newFolderLabel,-1);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        final Activity activity = getActivity();
        if (activity instanceof DialogInterface.OnDismissListener) {
            ((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FoldersAdapter.FolderItem selectedFolder = mAdapter.getItem(position);
        String selectedFolderId = selectedFolder.labelId;
        // TODO extract strings
        if (selectedFolderId.equals("100")) {
            mMoveMessagesListener.showFoldersManager();
        } else {
            mMoveMessagesListener.move(selectedFolderId);
            dismissAllowingStateLoss();
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.dialog_fragment_move_messages;
    }

    @Override
    protected void initUi(final View rootView) {
        mList.setOnItemLongClickListener(labelItemLongClick);
        mList.setOnItemClickListener(this);

        final MessageDao messageDao = MessageDatabase.Companion
                .getInstance(requireContext().getApplicationContext(), userManager.requireCurrentUserId())
                .getDao();
        messageDao.getAllLabels().observe(this, new LabelsObserver());
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT > 15) {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);

                Window window = getDialog().getWindow();
                window.setGravity(Gravity.CENTER);
            }
        });
    }

    @Override
    protected int getStyleResource() {
        return R.style.AppTheme_Dialog_Labels;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.MoveToFolderFragment";
    }

    AdapterView.OnItemLongClickListener labelItemLongClick = (parent, view, position, id) -> false;

    private FoldersAdapter.FolderItem fromLabel(Label label,@DrawableRes int drawable) {
        FoldersAdapter.FolderItem folderItem = new FoldersAdapter.FolderItem();
        folderItem.labelId = label.getId();
        folderItem.name = label.getName();
        folderItem.color = label.getColor();
        folderItem.display = label.getDisplay();
        folderItem.order = label.getOrder();
        folderItem.icon = drawable;
        return folderItem;
    }

    public interface IMoveMessagesListener {
        void move(String folderId);
        void showFoldersManager();
    }

    private class LabelsObserver implements Observer<List<Label>> {
        @Override
        public void onChanged(@Nullable List<Label> labels) {
            if (labels == null) {
                return;
            }
            List<FoldersAdapter.FolderItem> folders = new ArrayList<>();
            if (isAdded()) {
                folders.addAll(prepareUI());
                for (Label label : labels) {
                    if (label.getExclusive()) {
                        folders.add(fromLabel(label,0));
                    }
                }
            }
            folders.add(buildNewFolder());
            mAdapter = new FoldersAdapter(getActivity(), folders);
            mList.setAdapter(mAdapter);
            mNoFoldersView.setVisibility(folders.size() == 0 ? View.VISIBLE : View.GONE);
        }
    }
}
