package com.bleck.urfeed.UI;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.bleck.urfeed.Modelo.FeedModel;
import com.bleck.urfeed.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class SelectFeed extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private List<FeedModel> feedModelList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_feed);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        feedModelList = initializeFeed();
        // specify an adapter (see also next example)
        mAdapter = new SelectFeedAdapter(feedModelList);
        mRecyclerView.setAdapter(mAdapter);
    }


    private List<FeedModel> initializeFeed(){
        List<FeedModel> fml = new ArrayList<>();
        fml.add(new FeedModel("Forbes","http://www.forbes.com/most-popular/feed/",""));
        fml.add(new FeedModel("Hipertextual","http://feed.hipertextual.com/",""));
        fml.add(new FeedModel("Xataka","http://feeds.weblogssl.com/xataka2",""));
        return fml;
    }
}