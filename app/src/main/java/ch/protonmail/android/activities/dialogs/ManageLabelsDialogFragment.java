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

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.adapters.LabelColorsAdapter;
import ch.protonmail.android.adapters.LabelsAdapter;
import ch.protonmail.android.api.models.room.messages.Label;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel;
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModelFactory;
import ch.protonmail.android.views.ThreeStateCheckBox;
import dagger.hilt.android.AndroidEntryPoint;

import static ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState;

@AndroidEntryPoint
public class ManageLabelsDialogFragment extends AbstractDialogFragment implements AdapterView.OnItemClickListener {

    public static final String ARGUMENT_CHECKED_LABELS = "ch.protonmail.android.ARG_CHECKED_LABELS";
    public static final String ARGUMENT_NUMBER_SELECTED_MESSAGES = "ch.protonmail.android.ARG_NUMBER_SELECTED_MESSAGES";
    public static final String ARGUMENT_MESSAGE_IDS = "ch.protonmail.android.ARG_MESSAGE_IDS";

    @Nullable
    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;
    @BindView(R.id.add_label_container)
    View mAddLabelContainer;
    @BindView(R.id.label_name)
    EditText mLabelName;
    @BindView(R.id.labels_list_view)
    ListView mList;
    @BindView(R.id.labels_grid_view)
    GridView mColorsGrid;
    @BindView(R.id.done)
    Button mDone;
    @BindView(R.id.labels_dialog_title)
    TextView mTitle;
    @BindView(R.id.also_archive)
    ThreeStateCheckBox mArchiveCheckbox;
    @BindView(R.id.archive_container)
    View mArchiveContainer;
    @BindView(R.id.no_labels)
    View mNoLabelsView;
    @BindView(R.id.list_divider)
    View mListDivider;

    @Inject
    protected ManageLabelsDialogViewModelFactory manageLabelsDialogViewModelFactory;

    private ILabelsChangeListener mLabelStateChangeListener;
    private ILabelCreationListener mLabelCreationListener;
    private LabelsAdapter mAdapter;
    private LabelColorsAdapter mColorsAdapter;
    private List<LabelsAdapter.LabelItem> mLabels;
    private List<String> mCheckedLabels;
    private HashMap<String, Integer> mAllLabelsMap;
    private List<String> mMessageIds;
    private String mSelectedNewLabelColor;
    private int[] mColorOptions;
    private int mCurrentSelection = -1;
    private ManageLabelsDialogViewModel viewModel;

    AdapterView.OnItemLongClickListener labelItemLongClick = (parent, view, position, id) -> false;

    /**
     * Instantiates a new fragment of this class.
     *
     * @param checkedLabels pass null if you do not need labels checked
     * @return new instance of {@link ManageLabelsDialogFragment}
     */
    public static ManageLabelsDialogFragment newInstance(
            Set<String> checkedLabels,
            HashMap<String, Integer> numberOfMessagesSelected,
            ArrayList<String> messageIds
    ) {
        ManageLabelsDialogFragment fragment = new ManageLabelsDialogFragment();
        Bundle extras = new Bundle();
        extras.putStringArrayList(ARGUMENT_CHECKED_LABELS, new ArrayList<>(checkedLabels));
        extras.putSerializable(ARGUMENT_NUMBER_SELECTED_MESSAGES, numberOfMessagesSelected);
        extras.putStringArrayList(ARGUMENT_MESSAGE_IDS, messageIds);
        fragment.setArguments(extras);
        return fragment;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.ManageLabelsFragment";
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mLabelStateChangeListener = (ILabelsChangeListener) context;
            mLabelCreationListener = (ILabelCreationListener) context;
        } catch (ClassCastException e) {
            // not throwing error since the user of this fragment:
            // * is not forced to listen for labels state change
            // * may not want to create new labels
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.dialog_fragment_manage_labels;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = manageLabelsDialogViewModelFactory.create(ManageLabelsDialogViewModel.class);
        viewModel.getViewState().observe(this, this::viewStateChanged);

        Bundle extras = getArguments();
        if (extras != null && extras.containsKey(ARGUMENT_CHECKED_LABELS)) {
            mCheckedLabels = getArguments().getStringArrayList(ARGUMENT_CHECKED_LABELS);
            mAllLabelsMap = (HashMap<String, Integer>) getArguments().getSerializable(ARGUMENT_NUMBER_SELECTED_MESSAGES);
            mMessageIds = getArguments().getStringArrayList(ARGUMENT_MESSAGE_IDS);
        } else {
            mCheckedLabels = new ArrayList<>();
            mMessageIds = null;
        }
    }

    @Override
    protected void initUi(final View rootView) {
        mList.setOnItemLongClickListener(labelItemLongClick);
        mColorsGrid.setOnItemClickListener(this);
        mArchiveCheckbox.getButton().numberOfStates = 2;
        MessagesDatabase messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(getContext().getApplicationContext()).getDatabase();
        messagesDatabase.getAllLabels().observe(this,new LabelsObserver());
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);

                Window window = getDialog().getWindow();
                if (window != null) {
                    window.setGravity(Gravity.CENTER);
                }
            }
        });

        mLabelName.addTextChangedListener(newLabelWatcher);
        setDialogTitle();
        setDoneTitle();
    }

    @Override
    protected int getStyleResource() {
        return R.style.AppTheme_Dialog_Labels;
    }

    @OnClick(R.id.close)
    public void onCloseClicked() {
        dismissAllowingStateLoss();
    }

    @OnClick(R.id.done)
    public void onDoneClicked() {
        viewModel.onDoneClicked(
                isCreatingNewLabel(),
                mSelectedNewLabelColor,
                getCheckedLabels(),
                mArchiveCheckbox.getState(),
                mLabelName.getText().toString(),
                mLabels
        );
    }

    private TextWatcher newLabelWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // NOOP
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            viewModel.onTextChanged(mLabelName.getText().toString(), isCreatingNewLabel());
        }

        @Override
        public void afterTextChanged(Editable s) {
            // NOOP
            mDone.setClickable(true);
        }
    };

    private boolean isCreatingNewLabel() {
        return mColorsGrid.getVisibility() == View.VISIBLE;
    }

    private void setDoneTitle() {
        mDone.setText(getString(R.string.label_apply));
    }

    private void setDialogTitle() {
        mTitle.setText(getString(R.string.labels_title_apply));
    }

    private void viewStateChanged(ViewState viewState) {
        if (viewState instanceof ViewState.ShowMissingColorError) {
            showToast(R.string.please_choose_color);
        }

        if (viewState instanceof ViewState.ShowMissingNameError) {
            showToast(R.string.label_name_empty);
        }

        if (viewState instanceof ViewState.ShowLabelNameDuplicatedError) {
            showToast(R.string.label_name_duplicate);
        }

        if (viewState instanceof ViewState.ShowLabelCreatedEvent) {
            showLabelCreated((ViewState.ShowLabelCreatedEvent) viewState);
        }

        if (viewState instanceof ViewState.ShowApplicableLabelsThresholdExceededError) {
            showApplicableLabelsThresholdError(
                    (ViewState.ShowApplicableLabelsThresholdExceededError) viewState
            );
        }

        if (viewState instanceof ViewState.SelectedLabelsChangedEvent) {
            dispatchLabelsCheckedEvent();
        }

        if (viewState instanceof ViewState.SelectedLabelsArchiveEvent) {
            dispatchLabelsCheckedArchiveEvent();
        }

        if (viewState instanceof ViewState.HideLabelsView) {
            dismissAllowingStateLoss();
        }

        if (viewState instanceof ViewState.ShowLabelCreationViews) {
            showLabelCreationViews();
        }

        if (viewState instanceof ViewState.HideLabelCreationViews) {
            hideLabelCreationViews();
        }
    }

    private void hideLabelCreationViews() {
        mColorsGrid.setVisibility(View.GONE);
        mList.setVisibility(View.VISIBLE);
        mLabelName.setVisibility(View.VISIBLE);
        UiUtil.hideKeyboard(getActivity(), mLabelName);
        setDoneTitle();
        setDialogTitle();
        mCurrentSelection = -1;
    }

    private void showLabelCreationViews() {
        showLabelColorsGrid();
        selectRandomColor();
        mAddLabelContainer.setVisibility(View.VISIBLE);
        mDone.setText(getString(R.string.label_add));
        mTitle.setText(getString(R.string.labels_title_add));
    }

    private void dispatchLabelsCheckedArchiveEvent() {
        mLabelStateChangeListener.onLabelsChecked(
                getCheckedLabels(),
                mMessageIds == null ? null : getUnchangedLabels(),
                mMessageIds,
                mMessageIds
        );
    }

    private void dispatchLabelsCheckedEvent() {
        mLabelStateChangeListener.onLabelsChecked(
                getCheckedLabels(),
                mMessageIds == null ? null : getUnchangedLabels(),
                mMessageIds
        );
    }

    private void showApplicableLabelsThresholdError(
            ViewState.ShowApplicableLabelsThresholdExceededError error
    ) {
        Toast.makeText(
                getContext(),
                String.format(getString(R.string.max_labels_selected), error.getMaxLabelsAllowed()),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showToast(@StringRes int messageId) {
        Toast.makeText(getContext(), messageId, Toast.LENGTH_SHORT).show();
    }

    private void showLabelCreated(ViewState.ShowLabelCreatedEvent labelCreatedEvent) {
        String labelName = labelCreatedEvent.getLabelName();
        mColorsGrid.setVisibility(View.GONE);
        mLabelName.setText("");
        mList.setVisibility(View.VISIBLE);
        UiUtil.hideKeyboard(getActivity(), mLabelName);

        if (mLabelCreationListener != null) {
            mLabelCreationListener.onLabelCreated(labelName, mSelectedNewLabelColor);
        }

        setDoneTitle();
        setDialogTitle();
    }


    class LabelsObserver implements Observer<List<Label>> {
        @Override
        public void onChanged(@Nullable List<Label> labels) {
            mLabels = new ArrayList<>();
            if (labels == null) {
                labels = new ArrayList<>();
            }
            for (Label label : labels) {
                if (!label.getExclusive()) {
                    mLabels.add(fromLabel(label));
                }
            }
            if (isAdded()) {
                mAdapter = new LabelsAdapter(getActivity(), mLabels);
                mList.setAdapter(mAdapter);
                if (mLabels.size() == 0) {
                    mNoLabelsView.setVisibility(View.VISIBLE);
                    mListDivider.setVisibility(View.GONE);
                    mDone.setClickable(false);
                } else {
                    mNoLabelsView.setVisibility(View.GONE);
                    mListDivider.setVisibility(View.VISIBLE);
                    mDone.setClickable(true);
                }
            }
        }
    }

    private LabelsAdapter.LabelItem fromLabel(Label label) {
        LabelsAdapter.LabelItem labelItem =
                new LabelsAdapter.LabelItem(mCheckedLabels != null && mCheckedLabels.contains(label.getId()));
        int numberSelectedMessages = 0;
        if (mAllLabelsMap != null && mAllLabelsMap.containsKey(label.getId())) {
            numberSelectedMessages = mAllLabelsMap.get(label.getId());
        }
        if (mMessageIds == null || numberSelectedMessages == mMessageIds.size() || numberSelectedMessages == 0) {
            labelItem.states = 2;
        } else {
            labelItem.states = 3;
            labelItem.isUnchanged = true;
            labelItem.isAttached = false;
        }
        labelItem.labelId = label.getId();
        labelItem.name = label.getName();
        labelItem.color = label.getColor();
        labelItem.display = label.getDisplay();
        labelItem.order = label.getOrder();
        labelItem.numberOfSelectedMessages = numberSelectedMessages;

        return labelItem;
    }

    private List<String> getCheckedLabels() {
        List<String> checkedLabelIds = new ArrayList<>();
        List<LabelsAdapter.LabelItem> labelItems = mAdapter.getAllItems();
        for (LabelsAdapter.LabelItem item : labelItems) {
            if (item.isAttached) {
                checkedLabelIds.add(item.labelId);
            }
        }
        return checkedLabelIds;
    }

    private List<String> getUnchangedLabels() {
        List<String> unchangedLabelIds = new ArrayList<>();
        List<LabelsAdapter.LabelItem> labelItems = mAdapter.getAllItems();
        for (LabelsAdapter.LabelItem item : labelItems) {
            if (item.isUnchanged) {
                unchangedLabelIds.add(item.labelId);
            }
        }
        return unchangedLabelIds;
    }

    private void showLabelColorsGrid() {
        mColorOptions = getResources().getIntArray(R.array.label_colors);
        mColorsAdapter = new LabelColorsAdapter(getActivity(), mColorOptions, R.layout.label_color_item);
        mColorsGrid.setAdapter(mColorsAdapter);
        mColorsGrid.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int colorId = mColorOptions[position];
        mSelectedNewLabelColor = String.format("#%06X", 0xFFFFFF & colorId);
        mColorsAdapter.setChecked(position);
        UiUtil.hideKeyboard(getActivity(), mLabelName);
    }

    private void selectRandomColor() {
        if (mCurrentSelection == -1) {
            Random random = new Random();
            mCurrentSelection = random.nextInt(mColorOptions.length);
            int colorId = mColorOptions[mCurrentSelection];
            mSelectedNewLabelColor = String.format("#%06X", 0xFFFFFF & colorId);
            mColorsAdapter.setChecked(mCurrentSelection);
        }
    }

    public interface ILabelsChangeListener {
        void onLabelsChecked(List<String> checkedLabelIds, List<String> unchangedLabels, List<String> messageIds);
        void onLabelsChecked(List<String> checkedLabelIds, List<String> unchangedLabels, List<String> messageIds, List<String> messagesToArchive);
    }

    public interface ILabelCreationListener {
        void onLabelCreated(String labelName, String color);
        void onLabelsDeleted(List<String> checkedLabelIds);
    }
}
