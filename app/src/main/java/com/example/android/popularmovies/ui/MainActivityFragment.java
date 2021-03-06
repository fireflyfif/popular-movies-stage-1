package com.example.android.popularmovies.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.popularmovies.R;
import com.example.android.popularmovies.adapters.MoviesAdapter;
import com.example.android.popularmovies.models.Movies;
import com.example.android.popularmovies.utilities.FetchMoviesTask;
import com.example.android.popularmovies.utilities.FetchMoviesTask.MainActivityView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by fifiv on 02/02/2018.
 */

public class MainActivityFragment extends Fragment implements
        SharedPreferences.OnSharedPreferenceChangeListener, MainActivityView,
        MoviesAdapter.MovieAdapterOnClickHandler {


    private static final String LOG_TAG = "MainActivityFragment";

    private static final String SAVE_STATE_KEY = "save_state";

    private static final String RECYCLER_VIEW_STATE = "list_state";

    private static final String MOVIE_DETAILS_KEY = "movie_parcel";

    @BindView(R.id.recycler_grid_view)
    RecyclerView mRecyclerGridView;
    @BindView(R.id.loading_indicator)
    ProgressBar mLoadingIndicator;
    @BindView(R.id.error_message_display)
    TextView mErrorMessage;
    private Parcelable savedRecyclerViewState;
    private MoviesAdapter mMoviesAdapter;
    // Declare the movie list as an ArrayList,
    // because Parcelable saves state of ArrayList, but not ListView
    private ArrayList<Movies> mMoviesList;


    // Mandatory empty constructor
    public MainActivityFragment() {
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        Log.v(LOG_TAG, "onViewStateRestored called Now!");
        if (savedInstanceState != null) {
            savedRecyclerViewState = savedInstanceState.getParcelable(RECYCLER_VIEW_STATE);
            mRecyclerGridView.getLayoutManager().onRestoreInstanceState(savedRecyclerViewState);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //((AppCompatActivity)getActivity()).setSupportActionBar(mToolbarMain);

        Log.v(LOG_TAG, "onCreate called Now!");

        if (savedInstanceState == null || !savedInstanceState.containsKey(SAVE_STATE_KEY)) {
            mMoviesList = new ArrayList<>();
        } else {
            mMoviesList = savedInstanceState.getParcelableArrayList(SAVE_STATE_KEY);
            savedRecyclerViewState = savedInstanceState.getParcelable(RECYCLER_VIEW_STATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_activity_main, container, false);

        ButterKnife.bind(this, rootView);

        mMoviesList = new ArrayList<>();

        // Create new instance of GridLayoutManager and set the second argument -
        // columnSpan to have 2 for vertical and 3 for landscape mode
        final GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(),
                getResources().getInteger(R.integer.movie_list_columns));

        mRecyclerGridView.setLayoutManager(gridLayoutManager);
        mRecyclerGridView.setHasFixedSize(true);
        mMoviesAdapter = new MoviesAdapter(getActivity(), mMoviesList, this);

        mRecyclerGridView.setAdapter(mMoviesAdapter);

        // Call onRestoreInstanceState when the data has been reattached to the mRecyclerGridView
        mRecyclerGridView.getLayoutManager().onRestoreInstanceState(savedRecyclerViewState);

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        // Check for Network Connection
        if (haveNetworkConnection()) {
            Log.v(LOG_TAG, "There is an internet connection");
            loadMoviesFromPreferences();
        } else {
            Log.v(LOG_TAG, "There is NO internet connection");
            showErrorMessage();
        }
        return rootView;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(SAVE_STATE_KEY, mMoviesList);

        // Put state of the position of the RecyclerView
        outState.putParcelable(RECYCLER_VIEW_STATE, mRecyclerGridView.getLayoutManager()
                .onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    /**
     * Check for Network Connection
     */
    private boolean haveNetworkConnection() {

        // Get reference to the ConnectivityManager to check for network connectivity
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        boolean haveNetConnection = false;
        // Get details on the currently active default data network
        if (networkInfo != null && networkInfo.isConnected()) {
            haveNetConnection = true;
        }
        return haveNetConnection;
    }

    /**
     * Method that shows error message when there is a problem fetching the data or
     * there is no internet connection
     */
    private void showErrorMessage() {
        // Hide the currently visible data
        mRecyclerGridView.setVisibility(View.INVISIBLE);

        // Show the error message
        mErrorMessage.setVisibility(View.VISIBLE);
    }

    /**
     * Method for loading movies using user's preference sort order
     * for fetching movies asynchronously
     */
    private void loadMoviesFromPreferences() {
        // There is something fishy here
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        // Execute the network call on a separate background thread
        FetchMoviesTask task = new FetchMoviesTask(mMoviesAdapter, this, this);

        // Update the recycler view with the user's preferences.
        String sortOrderKey = getString(R.string.pref_sort_by_key);
        String sortOrderDefault = getString(R.string.pref_sort_by_popular);
        String sortOrder = sharedPreferences.getString(sortOrderKey, sortOrderDefault);

        // Execute fetching the movie data from a background thread
        // sortOrder loads the movie data with the default "popular" sorting setting
        task.execute(sortOrder);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_sort_by_key))) {
            loadMoviesFromPreferences();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        loadMoviesFromPreferences();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void showProgress(boolean visible) {

        // Show Progress Bar if the movies are being fetched from the server,
        // Hide if not
        if (visible) {
            mLoadingIndicator.setVisibility(View.VISIBLE);
        } else {
            mLoadingIndicator.setVisibility(View.INVISIBLE);
        }
    }


    /**
     * Method that handles responses to clicks from the grid of movie posters
     *
     * @param movies Creates an object of Movies
     */
    @Override
    public void onClick(Movies movies) {
        //Movies currentMovie = movies;
        Intent intent = new Intent(getContext(), DetailActivity.class);
        //Movies currentMovie = mMoviesList.get();
        intent.putExtra(MOVIE_DETAILS_KEY, movies);
        getContext().startActivity(intent);
    }
}
