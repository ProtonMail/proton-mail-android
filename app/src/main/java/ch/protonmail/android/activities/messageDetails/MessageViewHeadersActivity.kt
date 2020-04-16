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
package ch.protonmail.android.activities.messageDetails

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import kotlinx.android.synthetic.main.activity_view_headers.*
import android.text.method.ScrollingMovementMethod

// region constants
const val EXTRA_VIEW_HEADERS = "extra_view_headers"
// endregion

class MessageViewHeadersActivity  : BaseActivity() {

    override fun getLayoutId() = R.layout.activity_view_headers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.message_headers)
        }

        viewHeadersText.movementMethod = ScrollingMovementMethod()
        viewHeadersText.text = intent.getStringExtra(EXTRA_VIEW_HEADERS)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        } else if (id == R.id.share) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, viewHeadersText.text.toString())
                type = "text/plain"
            }
            startActivity(sendIntent)
            return true
        }
        return false
    }
}