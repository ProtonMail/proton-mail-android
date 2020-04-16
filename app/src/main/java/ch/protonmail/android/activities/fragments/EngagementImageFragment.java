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
package ch.protonmail.android.activities.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;
import ch.protonmail.android.R;
import ch.protonmail.android.utils.extensions.TextExtensions;

/**
 * Created by dkadrikj on 12/11/15.
 */
public class EngagementImageFragment extends Fragment {
    private static final String ARGUMENT_IMAGE_RESOURCE_ID = "image_resource_id";
    private static final String ARGUMENT_TITLE_RESOURCE_ID = "title_resource_id";
    private static final String ARGUMENT_SUBTITLE_RESOURCE_ID = "subtitle_resource_id";
    private static final String ARGUMENT_IS_SUPPORT = "is_support";

    @BindView(R.id.engagement_image)
    ImageView mImageView;
    @BindView(R.id.engagement_title)
    TextView mTitleView;
    @BindView(R.id.engagement_subtitle)
    TextView mSubtitleView;
    private int imageResourceId;
    private int titleResourceId;
    private int subtitleResourceId;
    private boolean isSupport;

    public static EngagementImageFragment newInstance(int imageResourceId, int titleResourceId,
                                  int subtitleResourceId, boolean isSupport){
        EngagementImageFragment engagementImageFragment = new EngagementImageFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARGUMENT_IMAGE_RESOURCE_ID, imageResourceId);
        bundle.putInt(ARGUMENT_TITLE_RESOURCE_ID, titleResourceId);
        bundle.putInt(ARGUMENT_SUBTITLE_RESOURCE_ID, subtitleResourceId);
        bundle.putBoolean(ARGUMENT_IS_SUPPORT, isSupport);
        engagementImageFragment.setArguments(bundle);
        return engagementImageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            imageResourceId = getArguments().getInt(ARGUMENT_IMAGE_RESOURCE_ID);
            titleResourceId = getArguments().getInt(ARGUMENT_TITLE_RESOURCE_ID);
            subtitleResourceId = getArguments().getInt(ARGUMENT_SUBTITLE_RESOURCE_ID);
            isSupport = getArguments().getBoolean(ARGUMENT_IS_SUPPORT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int resource = R.layout.fragment_engagement_page;
        if (isSupport) {
            resource = R.layout.fragment_engagement_page_support;
        }
        View rootView = inflater.inflate(resource, container, false);
        ButterKnife.bind(this, rootView);
        mImageView.setImageResource(imageResourceId);
        mTitleView.setText(getString(titleResourceId));
        mSubtitleView.setText(getString(subtitleResourceId));
        return rootView;
    }

    @Optional
    @OnClick(R.id.support)
    public void onSupportClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.support_protonmail)));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            TextExtensions.showToast(getContext(), R.string.no_browser_found, Toast.LENGTH_SHORT);
        }
    }
}
