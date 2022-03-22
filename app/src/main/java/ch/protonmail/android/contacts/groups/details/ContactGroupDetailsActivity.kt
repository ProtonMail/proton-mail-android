/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.contacts.groups.details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.activities.StartCompose
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.contacts.groups.ContactGroupEmailsAdapter
import ch.protonmail.android.contacts.groups.edit.ContactGroupEditCreateActivity
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.RecyclerViewEmptyViewSupport
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_contact_group_details.*
import kotlinx.android.synthetic.main.content_contact_group_details.*
import timber.log.Timber
import javax.inject.Inject

const val EXTRA_CONTACT_GROUP = "extra_contact_group"

@AndroidEntryPoint
class ContactGroupDetailsActivity : BaseActivity() {

    private lateinit var groupName: String
    private var groups: List<ContactEmail> = emptyList()

    @Inject
    lateinit var app: ProtonMailApplication

    private lateinit var contactGroupEmailsAdapter: ContactGroupEmailsAdapter
    private val contactGroupDetailsViewModel: ContactGroupDetailsViewModel by viewModels()

    private val startComposeLauncher = registerForActivityResult(StartCompose()) { messageId ->
        messageId?.let {
            val snack = Snackbar.make(
                findViewById(R.id.contact_group_details_layout),
                R.string.snackbar_message_draft_saved,
                Snackbar.LENGTH_LONG
            )
            snack.setAction(R.string.move_to_trash) {
                contactGroupDetailsViewModel.moveDraftToTrash(messageId)
                Snackbar.make(
                    findViewById(R.id.contact_group_details_layout),
                    R.string.snackbar_message_draft_moved_to_trash,
                    Snackbar.LENGTH_LONG
                ).show()
            }
            snack.show()
        }
    }

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

        contact_group_details_send_message.setOnClickListener {
            onWriteToContacts()
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
        menuInflater.inflate(R.menu.menu_contact_group_details, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun onWriteToContacts() {
        if (groups.isNotEmpty()) {
            val recipientGroup = groups.asSequence().map { email ->
                MessageRecipient(email.name, email.email, groupName)
            }.toList()
            startComposeLauncher.launch(StartCompose.Input(toRecipientGroups = recipientGroup))
        } else {
            showToast(R.string.email_empty, Toast.LENGTH_SHORT)
        }
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
        color: Int
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
            groups = list
        }

        contactGroupDetailsViewModel.contactGroupEmailsEmpty.observe(this) {
            contactGroupEmailsAdapter.setData(ArrayList())
        }

        contactGroupDetailsViewModel.setupUIData.observe(this) { contactLabel ->
            groupName = contactLabel.name
            initCollapsingToolbar(contactLabel.color)
            setTitle(contactLabel.name, contactLabel.contactEmailsCount)
        }

        contactGroupDetailsViewModel.deleteGroupStatus.observe(this) {
            it?.getContentIfNotHandled()?.let { status ->
                Timber.v("deleteGroupStatus received $status")
                when (status) {
                    ContactGroupDetailsViewModel.Status.SUCCESS -> {
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
//            UiUtil.setTextViewDrawableColor(this@ContactGroupDetailsActivity, this, R.color.lead_gray)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.contact_group_details_delete -> {
                DialogUtils.showDeleteConfirmationDialog(
                    this, getString(R.string.delete),
                    resources.getQuantityString(R.plurals.are_you_sure_delete_group, 1)
                ) {
                    contactGroupDetailsViewModel.delete()
                }
            }
            R.id.contact_group_details_edit -> {
                val intent = Intent(this, ContactGroupEditCreateActivity::class.java)
                intent.putExtra(EXTRA_CONTACT_GROUP, contactGroupDetailsViewModel.getData() as Parcelable)
                startActivity(AppUtil.decorInAppIntent(intent))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}
