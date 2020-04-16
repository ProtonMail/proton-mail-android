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

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.DisplayMetrics;
import android.view.View;

import com.viewpagerindicator.CirclePageIndicator;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.fragments.EngagementImageFragment;

/**
 * Created by dkadrikj on 12/11/15.
 */
public class EngagementActivity extends BaseActivity {

    @BindView(R.id.engagement_pager)
    ViewPager mViewPager;
    @BindView(R.id.pager_indicator)
    CirclePageIndicator mCirclePageIndicator;

    private int[] images;
    private int[] titles;
    private int[] subtitles;
    private EngagementAdapter adapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_engagement;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.PopupTheme);
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        initData();
        adapter = new EngagementAdapter(getSupportFragmentManager());
        mViewPager.setOffscreenPageLimit(5);
        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == (images.length - 1)) {
                    mCirclePageIndicator.setVisibility(View.GONE);
                } else {
                    mCirclePageIndicator.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mCirclePageIndicator.setViewPager(mViewPager);
        float density = getResources().getDisplayMetrics().density;
        mCirclePageIndicator.setFillColor(getResources().getColor(R.color.icon_purple));
        mCirclePageIndicator.setPageColor(getResources().getColor(R.color.white));
        mCirclePageIndicator.setStrokeColor(getResources().getColor(R.color.blue));
        mCirclePageIndicator.setRadius(5 * density);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int) (width * 0.99f), (int) (height * 0.95f));
    }

    @Override
    public void onBackPressed() {
        exitTour();
        super.onBackPressed();
    }

    @OnClick(R.id.button_close)
    public void onCloseClicked() {
        exitTour();
    }

    private void initData() {
        images = new int[]{
                R.drawable.welcome,
                R.drawable.swipe,
                R.drawable.labels,
                R.drawable.encryption,
                R.drawable.expire,
                R.drawable.help
        };

        titles = new int[]{
                R.string.engagement_title_welcome,
                R.string.engagement_title_swipe,
                R.string.engagement_title_labels,
                R.string.engagement_title_encryption,
                R.string.engagement_title_expire,
                R.string.engagement_title_help
        };

        subtitles = new int[]{
                R.string.engagement_subtitle_welcome,
                R.string.engagement_subtitle_swipe,
                R.string.engagement_subtitle_labels,
                R.string.engagement_subtitle_encryption,
                R.string.engagement_subtitle_expire,
                R.string.engagement_subtitle_help
        };
    }

    private void exitTour() {
        mUserManager.engagementDone();
        finish();
    }

    public class EngagementAdapter extends FragmentStatePagerAdapter {

        EngagementAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return images.length;
        }

        @Override
        public Fragment getItem(int position) {
            return EngagementImageFragment.newInstance(images[position], titles[position], subtitles[position], false);
        }
    }
}
