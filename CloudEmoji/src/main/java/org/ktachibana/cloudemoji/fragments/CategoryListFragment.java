package org.ktachibana.cloudemoji.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import org.ktachibana.cloudemoji.R;
import org.ktachibana.cloudemoji.adapters.CategoryListAdapter;
import org.ktachibana.cloudemoji.databases.FavoritesDataSource;
import org.ktachibana.cloudemoji.helpers.RepoXmlParser;
import org.ktachibana.cloudemoji.interfaces.OnCopyToClipBoardListener;
import org.ktachibana.cloudemoji.interfaces.OnExceptionListener;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import java.sql.SQLException;

/**
 * Fragment that holds a list of strings for one category
 * This module uses ActionBar-PullToRefresh from https://github.com/chrisbanes/ActionBar-PullToRefresh
 */
public class CategoryListFragment extends Fragment {

    public static final String CAT_KEY = "category";
    private RepoXmlParser.Category cat;
    private FavoritesDataSource favoritesDataSource;
    private OnRefreshStartedListener refreshCallback;
    private OnExceptionListener exceptionCallback;
    private OnCopyToClipBoardListener copyCallback;

    /**
     * Activities implementing this interface should handle what happens when layout is pulled
     */
    public interface OnRefreshStartedListener {
        /**
         * Handles what happens if a layout is pulled
         *
         * @param layout Layout being pulled
         */
        public void onRefreshStarted(PullToRefreshLayout layout);
    }

    public static CategoryListFragment newInstance(RepoXmlParser.Category cat) {
        CategoryListFragment fragment = new CategoryListFragment();
        Bundle args = new Bundle();
        args.putSerializable(CAT_KEY, cat);
        fragment.setArguments(args);
        return fragment;
    }

    public CategoryListFragment() {
        // Required constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            refreshCallback = (OnRefreshStartedListener) activity;
            exceptionCallback = (OnExceptionListener) activity;
            copyCallback = (OnCopyToClipBoardListener) activity;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cat = (RepoXmlParser.Category) getArguments().getSerializable(CAT_KEY);
        }
        favoritesDataSource = new FavoritesDataSource(getActivity().getBaseContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate rootView
        View rootView = inflater.inflate(R.layout.list_view_pull_to_refresh, container, false);

        // Setup pullToRefreshLayout
        final PullToRefreshLayout pullToRefreshLayout = (PullToRefreshLayout) rootView.findViewById(R.id.pullToRefreshLayout);
        ActionBarPullToRefresh.from(getActivity()).allChildrenArePullable().listener(new OnRefreshListener() {
            @Override
            public void onRefreshStarted(View view) {
                refreshCallback.onRefreshStarted(pullToRefreshLayout);
            }
        }).setup(pullToRefreshLayout);

        // Setup listView
        ListView listView = (ListView) rootView.findViewById(R.id.pullToRefreshListView);
        listView.setAdapter(new CategoryListAdapter(getActivity().getBaseContext(), cat));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RepoXmlParser.Entry entry = (RepoXmlParser.Entry) parent.getAdapter().getItem(position);
                copyCallback.copyToClipBoard(entry.string);
            }
        });
        registerForContextMenu(listView);

        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(0, 0, 0, R.string.add_to_fav);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            // Get string and note from view
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            View rootView = info.targetView;
            RepoXmlParser.Entry entry = CategoryListAdapter.getEntryFromView(rootView);
            String string = entry.string;
            String note = entry.note;

            // Add to db
            try {
                favoritesDataSource.open();
                if (favoritesDataSource.addEntry(favoritesDataSource.createEntry(string, note))) {
                    Toast.makeText(getActivity().getBaseContext(), getString(R.string.added_to_fav), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity().getBaseContext(), getString(R.string.already_added_to_fav), Toast.LENGTH_SHORT).show();
                }
                favoritesDataSource.close();
            } catch (SQLException e) {
                exceptionCallback.onException(e);
            }
        }
        return true;
    }

}