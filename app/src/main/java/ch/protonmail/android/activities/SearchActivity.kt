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
package ch.protonmail.android.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.adapters.messages.MailboxRecyclerViewAdapter
import ch.protonmail.android.api.segments.event.FetchUpdatesJob
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.Constants.MessageLocationType.Companion.fromInt
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.details.presentation.ui.MessageDetailsActivity
import ch.protonmail.android.events.NoResultsEvent
import ch.protonmail.android.events.SearchResultEvent
import ch.protonmail.android.jobs.SearchMessagesJob
import ch.protonmail.android.mailbox.presentation.model.MailboxItemUiModel
import ch.protonmail.android.mailbox.presentation.viewmodel.MailboxViewModel
import ch.protonmail.android.utils.AppUtil
import com.google.android.material.snackbar.Snackbar
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
internal class SearchActivity : BaseActivity() {

    private lateinit var adapter: MailboxRecyclerViewAdapter
    private lateinit var noMessagesView: ConstraintLayout
    private lateinit var progressBar: ProgressBar
    private var scrollStateChanged = false
    private var queryText = ""
    private var currentPage = 0
    private lateinit var searchView: SearchView
    private lateinit var mailboxViewModel: MailboxViewModel

    @Inject
    lateinit var messageDetailsRepository: MessageDetailsRepository

    @Inject
    lateinit var mailboxViewModelProvider: Provider<MailboxViewModel>

    private val startComposeLauncher = registerForActivityResult(StartCompose()) { messageId ->
        messageId?.let {
            val snack = Snackbar.make(
                findViewById(R.id.search_layout),
                R.string.snackbar_message_draft_saved,
                Snackbar.LENGTH_LONG
            )
            snack.setAction(R.string.move_to_trash) {
                mailboxViewModel.moveToFolder(
                    listOf(messageId),
                    mUserManager.requireCurrentUserId(),
                    MessageLocationType.DRAFT,
                    MessageLocationType.TRASH.asLabelIdString()
                )
                Snackbar.make(
                    findViewById(R.id.search_layout),
                    R.string.snackbar_message_draft_moved_to_trash,
                    Snackbar.LENGTH_LONG
                ).show()
            }
            snack.show()
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_search

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mailboxViewModel = mailboxViewModelProvider.get()
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = null
        }
        val messagesListView = findViewById<RecyclerView>(R.id.messages_list_view)
        progressBar = findViewById(R.id.progress_bar)
        noMessagesView = findViewById(R.id.no_messages)
        adapter = MailboxRecyclerViewAdapter(this, null)
        messagesListView.adapter = adapter
        messagesListView.layoutManager = LinearLayoutManager(this)
        messagesListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(view: RecyclerView, scrollState: Int) {
                scrollStateChanged =
                    scrollState == RecyclerView.SCROLL_STATE_DRAGGING ||
                    scrollState == RecyclerView.SCROLL_STATE_SETTLING
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val adapter = recyclerView.adapter
                val lastVisibleItem = layoutManager?.findLastVisibleItemPosition() ?: return
                val lastPosition = adapter?.itemCount?.let { it - 1 } ?: return
                if (scrollStateChanged && lastVisibleItem == lastPosition && dy > 0) {
                    scrollStateChanged = false
                    currentPage++
                    performSearch(true)
                }
            }
        })
        adapter.setItemClick { mailboxUiItem: MailboxItemUiModel ->
            if (isDraft(mailboxUiItem)) {
                startComposeLauncher.launch(
                    StartCompose.Input(
                        messageId = mailboxUiItem.itemId,
                        isInline = mailboxUiItem.messageData?.isInline
                    )
                )
            } else {
                val intent = AppUtil.decorInAppIntent(
                    Intent(this@SearchActivity, MessageDetailsActivity::class.java)
                ).apply {
                    putExtra(MessageDetailsActivity.EXTRA_MESSAGE_OR_CONVERSATION_ID, mailboxUiItem.itemId)
                    putExtra(
                        MessageDetailsActivity.EXTRA_MESSAGE_LOCATION_ID,
                        MessageLocationType.SEARCH.messageLocationTypeValue
                    )
                    putExtra(MessageDetailsActivity.EXTRA_MESSAGE_SUBJECT, mailboxUiItem.subject)
                }
                startActivity(intent)
            }
        }

    }

    private fun showSearchResults(items: List<MailboxItemUiModel>) {
        adapter.submitList(items)
        progressBar.visibility = View.GONE
        adapter.setNewLocation(MessageLocationType.SEARCH)
    }

    override fun onStart() {
        super.onStart()
        ProtonMailApplication.getApplication().bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        ProtonMailApplication.getApplication().bus.unregister(this)
    }

    override fun onResume() {
        super.onResume()
        mJobManager.addJobInBackground(FetchUpdatesJob())
        if (queryText.isNotEmpty()) {
            progressBar.visibility = View.VISIBLE
            Handler().postDelayed({ performSearch(false) }, 1000)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        val searchItem = menu.findItem(R.id.search)
        searchView = searchItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE
        searchView.queryHint = getString(R.string.x_search)
        searchView.onActionViewExpanded()
        searchView.imeOptions =
            EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        searchItem.expandActionView()
        searchView.requestFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                currentPage = 0
                queryText = query
                performSearch(false)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                progressBar.visibility = View.GONE
                noMessagesView.visibility = View.GONE
                return true
            }
        })
        return true
    }

    private fun performSearch(loadMore: Boolean) {
        progressBar.visibility = if (loadMore) View.GONE else View.VISIBLE
        mJobManager.addJobInBackground(SearchMessagesJob(queryText, currentPage))
        searchView.clearFocus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun isDraft(item: MailboxItemUiModel): Boolean {
        val messageLocation = item.messageData?.location?.let { fromInt(it) }
        return messageLocation === MessageLocationType.ALL_DRAFT ||
            messageLocation === MessageLocationType.DRAFT
    }

    @Subscribe
    fun onNoResultsEvent(event: NoResultsEvent) {
        progressBar.visibility = View.GONE
        if (event.page == 0) {
            adapter.submitList(null)
            noMessagesView.visibility = View.VISIBLE
        }
    }

    @Subscribe
    fun onSearchResults(event: SearchResultEvent) {
        val messages = event.results
        val items = mailboxViewModel.messagesToMailboxItemsBlocking(messages)
        showSearchResults(items)
    }
}
