package com.bleck.urfeed.UI;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bleck.urfeed.Modelo.FeedModel;
import com.bleck.urfeed.R;
import com.bleck.urfeed.util.IabBroadcastReceiver;
import com.bleck.urfeed.util.IabHelper;
import com.bleck.urfeed.util.IabResult;
import com.bleck.urfeed.util.Purchase;

import java.util.List;

/**
 * Created by Carlos on 26/06/2016.
 */
public class SelectFeedAdapter extends RecyclerView.Adapter<SelectFeedAdapter.ViewHolder> {
        private List<FeedModel> mDataset;
        private static Context mContext;
        private static SelectFeed mActivity;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView mTextView;
            public ImageView mImageView;
            public ViewHolder(View v) {
                super(v);
                mTextView = (TextView) v.findViewById(R.id.title_text);
                mImageView = (ImageView) v.findViewById(R.id.bck_image);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public SelectFeedAdapter(List<FeedModel> myDataset) {
            mDataset = myDataset;
        }
        public SelectFeedAdapter(SelectFeed activity, List<FeedModel> myDataset) {
            mDataset = myDataset;
            mActivity = activity;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public SelectFeedAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {

            mContext = parent.getContext();
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_selectfeed_cardview, parent, false);
            // set the view's size, margins, paddings and layout parameters
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.mTextView.setText(mDataset.get(position).getName());

            holder.mImageView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {

                    mActivity.onClickFeed(position, mDataset);
                }
            });
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
}
