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

package ch.protonmail.android.activities.guest

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import butterknife.OnClick
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.feature.account.AccountViewModel
import ch.protonmail.android.utils.AppUtil

class FirstActivity : BaseActivity() {

    private val accountViewModel: AccountViewModel by viewModels()

    override fun getLayoutId(): Int = R.layout.activity_first

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountViewModel.register(this)
    }

    @OnClick(R.id.sign_in)
    fun onSignInClicked() {
        /*
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
        */
        accountViewModel.startLoginWorkflow()
    }

    @OnClick(R.id.create_account)
    fun onCreateAccountClicked() {
        val intent = AppUtil.decorInAppIntent(Intent(this, CreateAccountActivity::class.java))
        intent.putExtra(CreateAccountActivity.EXTRA_WINDOW_SIZE, window.decorView.height)
        startActivity(intent)
    }
}
