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

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import ch.protonmail.android.R;
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.activities.mailbox.InvalidateSearchDatabase;
import ch.protonmail.android.activities.messageDetails.MessageDetailsActivity;
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository;
import ch.protonmail.android.adapters.messages.MessagesRecyclerViewAdapter;
import ch.protonmail.android.api.segments.event.FetchUpdatesJob;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.data.ContactsRepository;
import ch.protonmail.android.data.local.MessageDao;
import ch.protonmail.android.data.local.MessageDatabase;
import ch.protonmail.android.data.local.PendingActionDao;
import ch.protonmail.android.data.local.PendingActionDatabase;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.events.LogoutEvent;
import ch.protonmail.android.events.NoResultsEvent;
import ch.protonmail.android.jobs.SearchMessagesJob;
import ch.protonmail.android.utils.AppUtil;
import dagger.hilt.android.AndroidEntryPoint;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING;
import static ch.protonmail.android.core.Constants.MessageLocationType;

@AndroidEntryPoint
public class SearchActivity extends BaseActivity {

    private PendingActionDao pendingActionDao;

    private MessagesRecyclerViewAdapter mAdapter;
    private TextView noMessagesView;
    private ProgressBar mProgressBar;
    private boolean mScrollStateChanged = false;
    private String mQueryText = "";
    private int mCurrentPage;
    private SearchView searchView = null;
    private MessageDao searchDao;

    @Inject
    MessageDetailsRepository messageDetailsRepository;
    @Inject
    ContactsRepository contactsRepository;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_search;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchDao = MessageDatabase.Companion
                .getSearchDatabase(getApplicationContext(), mUserManager.requireCurrentUserId())
                .getDao();
        pendingActionDao = PendingActionDatabase.Companion
                .getInstance(getApplicationContext(), mUserManager.requireCurrentUserId())
                .getDao();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(null);
        }

        RecyclerView messagesListView = findViewById(R.id.messages_list_view);

        mProgressBar = findViewById(R.id.progress_bar);
        noMessagesView = findViewById(R.id.no_messages);

        mAdapter = new MessagesRecyclerViewAdapter(this,null);

        contactsRepository.findAllContactsEmailsAsync().observe(this, contactEmails -> mAdapter.setContactsList(contactEmails));

        messagesListView.setAdapter(mAdapter);
        messagesListView.setLayoutManager(new LinearLayoutManager(this));
        messagesListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView view, int scrollState) {
                mScrollStateChanged = (scrollState == SCROLL_STATE_DRAGGING || scrollState == SCROLL_STATE_SETTLING);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                RecyclerView.Adapter adapter = recyclerView.getAdapter();

                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                int lastPosition = adapter.getItemCount() - 1;
                if (mScrollStateChanged && lastVisibleItem == lastPosition && dy > 0) {
                    mScrollStateChanged = false;
                    setLoadingMore(true);
                    mCurrentPage++;
                    doSearch(false);
                }
            }

        });

        mAdapter.setItemClick(message -> {
            if (isDraftMessage(message)) {
                Intent intent = AppUtil.decorInAppIntent(new Intent(SearchActivity.this, ComposeMessageActivity.class));
                intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, message.getMessageId());
                intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_RESPONSE_INLINE, message.isInline());
                startActivity(intent);
            } else {
                Intent intent = AppUtil.decorInAppIntent(new Intent(SearchActivity.this, MessageDetailsActivity.class));
                intent.putExtra(MessageDetailsActivity.EXTRA_MESSAGE_ID, message.getMessageId());
                intent.putExtra(MessageDetailsActivity.EXTRA_TRANSIENT_MESSAGE, true);
                startActivity(intent);
            }
            return null;
        });

        messageDetailsRepository.getAllSearchMessages().observe(this, messages -> {
            if (messages != null) {
                mAdapter.clear();
                mAdapter.addAll(messages);
                setLoadingMore(false);
                mProgressBar.setVisibility(View.GONE);
                mAdapter.setNewLocation(MessageLocationType.SEARCH);
            }
        });

        messageDetailsRepository.getAllLabels().observe(this, labels -> {
            if (labels != null) {
                mAdapter.setLabels(labels);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProtonMailApplication.getApplication().getBus().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ProtonMailApplication.getApplication().getBus().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mJobManager.addJobInBackground(new FetchUpdatesJob());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new InvalidateSearchDatabase(messageDetailsRepository.getSearchDatabaseDao()).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.search);

        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint(getString(R.string.search_messages));
        searchView.onActionViewExpanded();
        searchView.setImeOptions(
                EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        searchItem.expandActionView();
        searchView.requestFocus();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mCurrentPage = 0;
                boolean newSearch = !mQueryText.equals(query);
                mQueryText = query;
                setLoadingMore(false);
                mProgressBar.setVisibility(View.VISIBLE);
                doSearch(newSearch);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mProgressBar.setVisibility(View.GONE);
                noMessagesView.setVisibility(View.GONE);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doSearch(boolean newSearch) {
        mJobManager.addJobInBackground(new SearchMessagesJob(mQueryText, mCurrentPage, newSearch));
    }

    private boolean isDraftMessage(Message message) {
        MessageLocationType messageLocation = MessageLocationType.Companion.fromInt(message.getLocation());
        return messageLocation == MessageLocationType.ALL_DRAFT ||
                messageLocation == MessageLocationType.DRAFT;
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
    }

    @Subscribe
    public void onNoResultsEvent(NoResultsEvent event) {
        setLoadingMore(false);
        mProgressBar.setVisibility(View.GONE);
        if (event.getPage() == 0) {
            mAdapter.clear();
            noMessagesView.setVisibility(View.VISIBLE);
        }
    }

    private void setLoadingMore(boolean loadingMore) {
        mAdapter.setIncludeFooter(loadingMore);
    }

}
