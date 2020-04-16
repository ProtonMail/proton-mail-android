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
package ch.protonmail.android.activities.guest;

import android.content.Intent;
import android.os.Bundle;

import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.BaseActivity;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 11/12/15.
 */
public class FirstActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_first;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @OnClick(R.id.sign_in)
    public void onSignInClicked() {
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
    }

    @OnClick(R.id.create_account)
    public void onCreateAccountClicked() {
        Intent intent = AppUtil.decorInAppIntent(new Intent(this, CreateAccountActivity.class));
        intent.putExtra(CreateAccountActivity.EXTRA_WINDOW_SIZE, getWindow().getDecorView().getHeight());
        startActivity(intent);
    }
}
