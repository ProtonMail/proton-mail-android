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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.otto.Subscribe;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Provider;

import ch.protonmail.android.R;
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity;
import ch.protonmail.android.activities.mailbox.InvalidateSearchDatabase;
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository;
import ch.protonmail.android.adapters.messages.MailboxRecyclerViewAdapter;
import ch.protonmail.android.api.segments.event.FetchUpdatesJob;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.details.presentation.MessageDetailsActivity;
import ch.protonmail.android.events.NoResultsEvent;
import ch.protonmail.android.jobs.SearchMessagesJob;
import ch.protonmail.android.mailbox.presentation.MailboxViewModel;
import ch.protonmail.android.mailbox.presentation.model.MailboxUiItem;
import ch.protonmail.android.utils.AppUtil;
import dagger.hilt.android.AndroidEntryPoint;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING;
import static ch.protonmail.android.core.Constants.MessageLocationType;

@AndroidEntryPoint
public class SearchActivity extends BaseActivity {

    private MailboxRecyclerViewAdapter mAdapter;
    private TextView noMessagesView;
    private ProgressBar mProgressBar;
    private boolean mScrollStateChanged = false;
    private String mQueryText = "";
    private int mCurrentPage;
    private SearchView searchView = null;

    @Inject
    MessageDetailsRepository messageDetailsRepository;
    @Inject
    Provider<MailboxViewModel> mailboxViewModelProvider;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_search;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MailboxViewModel mailboxViewModel = mailboxViewModelProvider.get();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(null);
        }

        RecyclerView messagesListView = findViewById(R.id.messages_list_view);

        mProgressBar = findViewById(R.id.progress_bar);
        noMessagesView = findViewById(R.id.no_messages);

        mAdapter = new MailboxRecyclerViewAdapter(this, null);

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

        mAdapter.setItemClick(mailboxUiItem -> {
            if (isDraft(mailboxUiItem)) {
                Intent intent = AppUtil.decorInAppIntent(new Intent(SearchActivity.this, ComposeMessageActivity.class));
                intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, mailboxUiItem.getItemId());
                intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_RESPONSE_INLINE, mailboxUiItem.getMessageData().isInline());
                startActivity(intent);
            } else {
                Intent intent = AppUtil.decorInAppIntent(new Intent(SearchActivity.this, MessageDetailsActivity.class));
                intent.putExtra(MessageDetailsActivity.EXTRA_MESSAGE_ID, mailboxUiItem.getItemId());
                intent.putExtra(MessageDetailsActivity.EXTRA_TRANSIENT_MESSAGE, true);
                startActivity(intent);
            }
            return null;
        });

        mailboxViewModel.getMailboxItems(
                MessageLocationType.SEARCH,
                "",
                false,
                UUID.randomUUID().toString(),
                false
        ).observe(this, state -> {
            mAdapter.addAll(state.getItems());
            setLoadingMore(false);
            mProgressBar.setVisibility(View.GONE);
            mAdapter.setNewLocation(MessageLocationType.SEARCH);
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

        searchView = (SearchView) searchItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint(getString(R.string.x_search));
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

    private boolean isDraft(MailboxUiItem item) {
        MessageLocationType messageLocation = MessageLocationType.Companion.fromInt(item.getMessageData().getLocation());
        return messageLocation == MessageLocationType.ALL_DRAFT ||
                messageLocation == MessageLocationType.DRAFT;
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
