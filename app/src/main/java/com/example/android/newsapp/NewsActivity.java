package com.example.android.newsapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class NewsActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<List<Story>> {

    public static final String LOG_TAG = Story.class.getName();

    /**
     * Constant value for the story loader ID.
     */
    private static final int STORY_LOADER_ID = 1;

    /**
     * Base URL for fetching data from the Guardian API (without the query)
     */
    private static final String GUARDIAN_NEWS_BASE_URL =
            "http://content.guardianapis.com/search?q=";

    /**
     * URL ending that specifies the max number of entries to return
     */
    private static final String GUARDIAN_URL_ENDING = "&api-key=test";

    /**
     * Adapter for the list of stories
     */
    private StoryAdapter mAdapter;

    /**
     * TextView that is displayed when the list is empty
     */
    private TextView mEmptyStateTextView;

    /**
     * Progress bar
     */
    private ProgressBar mProgressBar;

    /**
     * Search query field
     */
    private EditText mQueryField;

    /**
     * ListView with news stories
     */
    private ListView storyListView;

    /**
     * Story list instance state
     */
    private Parcelable mListInstanceState;

    /**
     * List of stories to persist between activities
     */
    private ArrayList<Parcelable> mStories;

    /**
     * Helper method for hiding the keyboard
     */
    private static void hideKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news_activity);

        // Retrieve saved data if available
        if (savedInstanceState != null) {
            mListInstanceState = savedInstanceState.getParcelable("book_list");
            mStories = savedInstanceState.getParcelableArrayList("books");
        }

        // Set touch listeners on views that will hide the keyboard when not needed
        setupUI(findViewById(R.id.main_layout));
        // Find a reference to the {@link ListView} in the layout
        storyListView = (ListView) findViewById(R.id.list);
        // Set empty view
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_text_view);
        storyListView.setEmptyView(mEmptyStateTextView);
        // Hide progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        // Search query Edit Text
        mQueryField = (EditText) findViewById(R.id.text);

        // Set empty state text to display the intro text
        if (connectionOk()) {
            mEmptyStateTextView.setText(R.string.intro_text);
        } else {
            mEmptyStateTextView.setText(R.string.no_internet_connection);
        }

        // Restore data if available
        if (mStories != null && !mStories.isEmpty()) {
            mAdapter = new StoryAdapter(this, (List) mStories);
        } else {
            // Otherwise create a new adapter that takes an empty list of stories as input
            mAdapter = new StoryAdapter(this, new ArrayList<Story>());
        }

        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        storyListView.setAdapter(mAdapter);

        // Restore position
        if (mListInstanceState != null) {
            storyListView.onRestoreInstanceState(mListInstanceState);
        }

        // Set an item click listener on the ListView, which sends an intent to a web browser
        // to open a website with more information about the selected book.
        storyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (connectionOk()) {
                    // Find the current book that was clicked on
                    Story currentStory = mAdapter.getItem(position);
                    // Convert the String URL into a URI object (to pass into the Intent constructor)
                    Uri storyUri = Uri.parse(currentStory.getUrl());
                    // Create a new intent to view the book URI
                    Intent websiteIntent = new Intent(Intent.ACTION_VIEW, storyUri);
                    // Send the intent to launch a new activity
                    startActivity(websiteIntent);
                } else {
                    Toast.makeText(NewsActivity.this, "No Internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        final ImageButton mSearchButton = (ImageButton) findViewById(R.id.btn_search);
        // Set click listener on search button
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mAdapter.clear();
                mQueryField.setCursorVisible(false);
                populateUI(mQueryField.getText().toString());
            }
        });

        // Touch listener that shows cursor when the query Edit Text is touched
        mQueryField.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mQueryField.setCursorVisible(true);
                return false;
            }
        });

        // Perform the search when the "Done" key is pressed
        mQueryField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard(NewsActivity.this);
                    mSearchButton.performClick();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Load data and display it
     */
    private void populateUI(String query) {
        if (connectionOk()) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getSupportLoaderManager();
            if (!query.equals("")) {
                String mRequestUrl = buildUrl(query);
                Bundle args = new Bundle();
                args.putString("url", mRequestUrl);
                if (loaderManager.getLoader(STORY_LOADER_ID) == null) {
                    // Initialize the loader. Pass in the int ID constant defined above and pass in null for
                    // the bundle. Pass in the NewsActivity activity for the LoaderCallbacks parameter
                    loaderManager.initLoader(STORY_LOADER_ID, args, NewsActivity.this);
                } else {
                    // Restart the loader
                    loaderManager.restartLoader(STORY_LOADER_ID, args, NewsActivity.this);
                }
            }
        } else {
            // Set empty state text to display "No internet connection."
            mEmptyStateTextView.setText(R.string.no_internet_connection);
            // Hide progress bar
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public Loader<List<Story>> onCreateLoader(int i, Bundle args) {
        // Change empty state text
        mEmptyStateTextView.setText(R.string.loading);
        // Show progress bar
        mProgressBar.setVisibility(View.VISIBLE);
        return new StoryLoader(this, args.getString("url"));
    }

    @Override
    public void onLoadFinished(Loader<List<Story>> loader, List<Story> stories) {
        // If there is a valid list of {@link Story}s, then add them to the adapter's
        // data set. This will trigger the ListView to update.
        if (stories != null && !stories.isEmpty()) {
            mAdapter.addAll(stories);
        } else {
            // Set empty state text to display "No stories found."
            mEmptyStateTextView.setText(R.string.no_stories_found);
        }
        // Hide progress bar
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<List<Story>> loader) {
        // Loader reset, so we can clear out our existing data.
        mAdapter.clear();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // persist adapter items
        List<Story> stories = mAdapter.getItems();
        outState.putParcelableArrayList("stories", new ArrayList<>(stories));

        // persist position
        outState.putParcelable("book_list", storyListView.onSaveInstanceState());

    }

    private boolean connectionOk() {
        // Check if the device is connected to the Internet
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Build a URL string from user input
     */
    private String buildUrl(String query) {
        String url = GUARDIAN_NEWS_BASE_URL;
        try {
            url += URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("NewsActivity", "Error creating URL");
        }
        url += GUARDIAN_URL_ENDING;
        return url;
    }

    /**
     * Set up touch listener for non-text box views to hide keyboard and cursor
     */
    private void setupUI(View view) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideKeyboard(NewsActivity.this);
                    return false;
                }
            });
        }
        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }
}