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
package ch.protonmail.android.contacts.groups.list

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AbsListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.fragments.BaseFragment
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.contacts.IContactsListFragmentListener
import ch.protonmail.android.contacts.IContactsFragment
import ch.protonmail.android.contacts.groups.details.ContactGroupDetailsActivity
import ch.protonmail.android.contacts.groups.details.EXTRA_CONTACT_GROUP
import ch.protonmail.android.contacts.list.search.ISearchListenerViewModel
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.ifEmptyElse
import ch.protonmail.android.utils.extensions.ifNullElse
import ch.protonmail.android.utils.extensions.setDefaultIfEmpty
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import ch.protonmail.android.utils.ui.selection.SelectionModeEnum
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_contacts_groups.*
import java.io.Serializable
import javax.inject.Inject

// region constants
private const val TAG_CONTACT_GROUPS_FRAGMENT = "ProtonMail.ContactGroupsFragment"
// endregion

/**
 * Created by kadrikj on 8/24/18.
 */

class ContactGroupsFragment : BaseFragment(), IContactsFragment, AbsListView.MultiChoiceModeListener {

    @Inject
    lateinit var contactGroupsViewModelFactory: ContactGroupsViewModelFactory
    private lateinit var contactGroupsViewModel: ContactGroupsViewModel
    private lateinit var contactGroupsAdapter: ContactsGroupsListAdapter
    private var mActionMode: ActionMode? = null

    val getActionMode get() = mActionMode

    private val listener: IContactsListFragmentListener by lazy {
        context as IContactsListFragmentListener
    }

    override fun onItemCheckedStateChanged(
        mode: ActionMode?,
        position: Int,
        id: Long,
        checked: Boolean
    ) {
        // NOOP
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val menuItemId = item?.itemId

        when (menuItemId) {
            R.id.action_delete -> {
                DialogUtils.showDeleteConfirmationDialog(
                    context!!, getString(R.string.delete),
                    context!!.resources.getQuantityString(
                        R.plurals.are_you_sure_delete_group,
                        contactGroupsAdapter.getSelectedItems!!.toList().size,
                        contactGroupsAdapter.getSelectedItems!!.toList().size))
                {
                    onDelete()
                    mode!!.finish()
                }
            }
        }
        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mActionMode = mode
        UiUtil.setStatusBarColor(
            activity as Activity,
            UiUtil.scaleColor(ContextCompat.getColor(context!!, R.color.dark_purple_statusbar), 1f, true)
        )
        mode!!.menuInflater.inflate(R.menu.contacts_menu, menu)
        menu!!.findItem(R.id.action_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.findItem(R.id.action_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.findItem(R.id.action_sync).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.findItem(R.id.action_convert).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        return true
    }

    override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: Menu): Boolean {
        menu.findItem(R.id.action_delete).isVisible = true
        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_sync).isVisible = false
        menu.findItem(R.id.action_convert).isVisible = false
        return true
    }

    override fun onDestroyActionMode(mode: android.view.ActionMode?) {
        mActionMode!!.finish()
        mActionMode = null
        contactGroupsAdapter.endSelectionMode()

        UiUtil.setStatusBarColor(
            activity as AppCompatActivity,
            ContextCompat.getColor(context!!, R.color.dark_purple_statusbar)

        )

        listener.setTitle(getString(R.string.contacts))
    }

    override fun onContactPermissionChange(hasPermission: Boolean) { }

    override fun getLayoutResourceId() = R.layout.fragment_contacts_groups

    override fun getFragmentKey() = TAG_CONTACT_GROUPS_FRAGMENT

    override fun getSearchListener(): ISearchListenerViewModel = contactGroupsViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        AndroidSupportInjection.inject(this)

        contactGroupsViewModel = ViewModelProviders.of(this, contactGroupsViewModelFactory)
            .get(ContactGroupsViewModel::class.java)
        contactGroupsViewModel.reloadDependencies()
        initAdapter()
    }

    override fun onStart() {
        super.onStart()
        startObserving()
    }

    companion object {

        fun newInstance(): ContactGroupsFragment {
            return ContactGroupsFragment()
        }
    }

    private fun initAdapter() {
        var actionMode: ActionMode? = null

        contactGroupsAdapter = ContactsGroupsListAdapter(
            ArrayList(),
            this::onContactGroupClick,
            this::onWriteToContactGroup,
            this::onContactGroupSelect
        ) { selectionModeEvent ->
            when ( selectionModeEvent ) {

                SelectionModeEnum.STARTED ->
                    actionMode = listener.doStartActionMode(this@ContactGroupsFragment)

                SelectionModeEnum.ENDED -> {
                    actionMode?.finish()
                    actionMode = null
                }
            }
        }

        contactGroupsRecyclerView.layoutManager = LinearLayoutManager(context)
        contactGroupsRecyclerView.adapter = contactGroupsAdapter
    }

    private fun startObserving() {
        contactGroupsViewModel.contactGroupsResult.observe(this, Observer<List<ContactLabel>> {
            it.ifEmptyElse({
                noResults.visibility = VISIBLE
            }, {
                noResults.visibility = GONE
            })
            listener.dataUpdated(1, it?.size ?: 0)
            contactGroupsAdapter.setData(it ?: ArrayList())
        })

        contactGroupsViewModel.contactGroupsError.observe(this, Observer<Event<String>> { event ->
            event?.getContentIfNotHandled()?.let { it ->
                context?.showToast(it.setDefaultIfEmpty(getString(R.string.default_error_message)))
            }
        })
        contactGroupsViewModel.fetchContactGroups(ThreadSchedulers.main())
        contactGroupsViewModel.watchForJoins(ThreadSchedulers.main())
    }

    private fun onContactGroupSelect() {
        val checkedItems = contactGroupsAdapter.getSelectedItems?.size
        checkedItems.ifNullElse({
            listener.setTitle(getString(R.string.contacts))
        }, {
            mActionMode?.title = String.format(getString(R.string.contact_group_selected), checkedItems)
        })
    }

    private fun onContactGroupClick(labelItem: ContactLabel) {
        val detailsIntent = Intent(context, ContactGroupDetailsActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable(EXTRA_CONTACT_GROUP, labelItem)
        detailsIntent.putExtra(EXTRA_CONTACT_GROUP, bundle)
        startActivity(AppUtil.decorInAppIntent(detailsIntent))
    }


    override fun onDelete() {
        contactGroupsViewModel.deleteSelected(contactGroupsAdapter.getSelectedItems!!.toList())
    }

    private fun onWriteToContactGroup(contactGroup: ContactLabel) {
        val composeIntent = Intent(context, ComposeMessageActivity::class.java)
        if (!contactGroupsViewModel.isPaidUser()) {
            context?.showToast(R.string.paid_plan_needed)
            return
        }
        contactGroupsViewModel.contactGroupEmailsResult.observe(this, object : Observer<Event<List<ContactEmail>>> {
            override fun onChanged(event: Event<List<ContactEmail>>?) {
                event?.getContentIfNotHandled()?.let {
                    composeIntent.putExtra(
                        ComposeMessageActivity.EXTRA_TO_RECIPIENT_GROUPS, it.asSequence().map { email ->
                            MessageRecipient(email.name, email.email, contactGroup.name)
                        }.toList() as Serializable
                    )
                    startActivity(AppUtil.decorInAppIntent(composeIntent))

                    contactGroupsViewModel.contactGroupEmailsResult.removeObserver(this)
                }
            }
        })

        contactGroupsViewModel.contactGroupEmailsError.observe(this, Observer<Event<String>> { event ->
            event?.getContentIfNotHandled()?.let {
                context?.showToast(it.setDefaultIfEmpty(getString(R.string.default_error_message)))
            }
        })

        contactGroupsViewModel.getContactGroupEmails(contactGroup)
    }

    override fun onStop() {
        super.onStop()
        val actionMode = this. mActionMode
        if (actionMode != null) {
            actionMode.finish()
            this. mActionMode = null
        }
    }
}
