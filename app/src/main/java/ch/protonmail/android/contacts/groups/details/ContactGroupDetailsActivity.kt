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
package ch.protonmail.android.contacts.groups.details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.contacts.groups.ContactGroupEmailsAdapter
import ch.protonmail.android.contacts.groups.edit.ContactGroupEditCreateActivity
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.RecyclerViewEmptyViewSupport
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_contact_group_details.*
import kotlinx.android.synthetic.main.content_contact_group_details.*
import kotlinx.android.synthetic.main.content_contact_group_details.contactEmailsRecyclerView
import kotlinx.android.synthetic.main.content_contact_group_details.noResults
import kotlinx.android.synthetic.main.content_edit_create_contact_group_header.*
import timber.log.Timber
import javax.inject.Inject

const val EXTRA_CONTACT_GROUP = "extra_contact_group"

@AndroidEntryPoint
class ContactGroupDetailsActivity : BaseActivity() {

    @Inject
    lateinit var app: ProtonMailApplication

    private lateinit var contactGroupEmailsAdapter: ContactGroupEmailsAdapter
    private val contactGroupDetailsViewModel: ContactGroupDetailsViewModel by viewModels()

    override fun getLayoutId() = R.layout.activity_contact_group_details

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(animToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initAdapter()
        startObserving()
        val bundle = intent?.getBundleExtra(EXTRA_CONTACT_GROUP)
        contactGroupDetailsViewModel.setData(bundle?.getParcelable(EXTRA_CONTACT_GROUP))

        initFilterView()
        editFab.setOnClickListener {
            val intent = Intent(this, ContactGroupEditCreateActivity::class.java)
            intent.putExtra(EXTRA_CONTACT_GROUP, contactGroupDetailsViewModel.getData() as Parcelable)
            startActivity(AppUtil.decorInAppIntent(intent))
        }
    }

    override fun onStart() {
        super.onStart()
        app.bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        app.bus.unregister(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.delete_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun initAdapter() {
        contactGroupEmailsAdapter = ContactGroupEmailsAdapter(this, ArrayList(), null)
        with(contactGroupEmailsAdapter) {
            registerAdapterDataObserver(
                RecyclerViewEmptyViewSupport(
                    contactEmailsRecyclerView,
                    noResults
                )
            )
        }
        with(contactEmailsRecyclerView) {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ContactGroupDetailsActivity)
            adapter = contactGroupEmailsAdapter
        }
    }

    private fun initCollapsingToolbar(
        color: Int,
        name: String,
        membersCount: Int
    ) {

        groupColor.bind(
            isSelectedActive = false,
            isMultiselectActive = false,
            circleColor = color
        )
    }

    private fun setTitle(name: String?, membersCount: Int) {
        supportActionBar?.title = name?.let {
            formatTitle(name, membersCount)
        } ?: ""
    }

    private fun formatTitle(name: String?, emailsCount: Int): String =
        String.format(
            getString(R.string.contact_group_toolbar_title),
            name,
            resources.getQuantityString(
                R.plurals.contact_group_members,
                emailsCount,
                emailsCount
            )
        )

    private fun startObserving() {
        contactGroupDetailsViewModel.contactGroupEmailsResult.observe(this) { list ->
            Timber.v("New contacts emails list size: ${list.size}")
            contactGroupEmailsAdapter.setData(list ?: ArrayList())
            if (list != null && TextUtils.isEmpty(filterView.text.toString())) {
                setTitle(contactGroupDetailsViewModel.getData()?.name, list.size)
            }
        }

        contactGroupDetailsViewModel.contactGroupEmailsEmpty.observe(this) {
            contactGroupEmailsAdapter.setData(ArrayList())
        }

        contactGroupDetailsViewModel.setupUIData.observe(this) { contactLabel ->
            initCollapsingToolbar(contactLabel.color, contactLabel.name, contactLabel.contactEmailsCount)
        }

        contactGroupDetailsViewModel.deleteGroupStatus.observe(this) {
            it?.getContentIfNotHandled()?.let { status ->
                Timber.v("deleteGroupStatus received $status")
                when (status) {
                    ContactGroupDetailsViewModel.Status.SUCCESS -> {
                        saveLastInteraction()
                        finish()
                        showToast(resources.getQuantityString(R.plurals.group_deleted, 1))
                    }
                    ContactGroupDetailsViewModel.Status.ERROR ->
                        showToast(status.message ?: getString(R.string.error))
                }

            }
        }
    }

    private fun initFilterView() {
        filterView.apply {
            UiUtil.setTextViewDrawableColor(this@ContactGroupDetailsActivity, this, R.color.lead_gray)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun afterTextChanged(editable: Editable?) {
                    contactGroupDetailsViewModel.doFilter(filterView.text.toString())
                }
            }
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> consume { onBackPressed() }
        R.id.action_delete -> consume {
            DialogUtils.showDeleteConfirmationDialog(
                this, getString(R.string.delete),
                resources.getQuantityString(R.plurals.are_you_sure_delete_group, 1)
            ) {
                contactGroupDetailsViewModel.delete()
            }
        }
        else -> super.onOptionsItemSelected(item)
    }

    private inline fun consume(f: () -> Unit): Boolean {
        f()
        return true
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        saveLastInteraction()
        finish()
    }
}
