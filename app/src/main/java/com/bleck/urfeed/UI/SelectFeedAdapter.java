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
        private static AppCompatActivity mActivity;
        static final String TAG = "UrFeed";

        private boolean payment;
        // The helper object
        private IabHelper mHelper;
        // Provides purchase notification while this app is running
        IabBroadcastReceiver mBroadcastReceiver;

        // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
        static final String SKU_STANDARD = "standard";

        // (arbitrary) request code for the purchase flow
        static final int RC_REQUEST = 10001;

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
        public SelectFeedAdapter(AppCompatActivity activity, List<FeedModel> myDataset) {
            mActivity = activity;
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public SelectFeedAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {

            // load payment data
            loadData();

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

                    if (payment) {
                        // item clicked
                        Intent i = new Intent(mContext, MainActivity.class);
                        i.putExtra("com.bleck.feedurl", mDataset.get(position).getLink());
                        mContext.startActivity(i);
                    }
                    else{
                        // launch the gas purchase UI flow.
                        // We will be notified of completion via mPurchaseFinishedListener
                        setWaitScreen(true);
                        Log.d(TAG, "Launching purchase flow for gas.");

                        /* TODO: for security, generate your payload here for verification. See the comments on
                         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
                         *        an empty string, but on a production app you should carefully generate this. */
                        String payload = "";

                        try {
                            mHelper.launchPurchaseFlow(mActivity, SKU_STANDARD, RC_REQUEST,
                                    mPurchaseFinishedListener, payload);
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            complain("Error launching purchase flow. Another async operation in progress.");
                            setWaitScreen(false);
                        }
                    }
                }
            });
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }

        void complain(String message) {
            Log.e(TAG, "**** TrivialDrive Error: " + message);
            alert("Error: " + message);
        }
        void alert(String message) {
            AlertDialog.Builder bld = new AlertDialog.Builder(mContext);
            bld.setMessage(message);
            bld.setNeutralButton("OK", null);
            Log.d(TAG, "Showing alert dialog: " + message);
            bld.create().show();
        }

        // Callback for when a purchase is finished
        IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
            public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

                // if we were disposed of in the meantime, quit.
                if (mHelper == null) return;

                if (result.isFailure()) {
                    complain("Error purchasing: " + result);
                    setWaitScreen(false);
                    return;
                }
                if (!verifyDeveloperPayload(purchase)) {
                    complain("Error purchasing. Authenticity verification failed.");
                    setWaitScreen(false);
                    return;
                }

                Log.d(TAG, "Purchase successful.");

                if (purchase.getSku().equals(SKU_STANDARD)) {
                    // bought 1/4 tank of gas. So consume it.
                    Log.d(TAG, "Purchase is standard. Starting standard consumption.");
                    try {
                        mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                    } catch (IabHelper.IabAsyncInProgressException e) {
                        complain("Error consuming gas. Another async operation in progress.");
                        setWaitScreen(false);
                        return;
                    }
                }
            }
        };

        // Called when consumption is complete
        IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
            public void onConsumeFinished(Purchase purchase, IabResult result) {
                Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

                // if we were disposed of in the meantime, quit.
                if (mHelper == null) return;

                // We know this is the "gas" sku because it's the only one we consume,
                // so we don't check which sku was consumed. If you have more than one
                // sku, you probably should check...
                if (result.isSuccess()) {
                    // successfully consumed, so we apply the effects of the item in our
                    // game world's logic, which in our case means filling the gas tank a bit
                    Log.d(TAG, "Consumption successful. Provisioning.");
                    payment = true;
                    saveData();
                    alert("You got now a subscription to UrFeed!");
                }
                else {
                    complain("Error while consuming: " + result);
                }
                setWaitScreen(false);
                Log.d(TAG, "End consumption flow.");
            }
        };

        /** Verifies the developer payload of a purchase. */
        boolean verifyDeveloperPayload(Purchase p) {
            String payload = p.getDeveloperPayload();

            /*
             * TODO: verify that the developer payload of the purchase is correct. It will be
             * the same one that you sent when initiating the purchase.
             *
             * WARNING: Locally generating a random string when starting a purchase and
             * verifying it here might seem like a good approach, but this will fail in the
             * case where the user purchases an item on one device and then uses your app on
             * a different device, because on the other device you will not have access to the
             * random string you originally generated.
             *
             * So a good developer payload has these characteristics:
             *
             * 1. If two different users purchase an item, the payload is different between them,
             *    so that one user's purchase can't be replayed to another user.
             *
             * 2. The payload must be such that you can verify it even when the app wasn't the
             *    one who initiated the purchase flow (so that items purchased by the user on
             *    one device work on other devices owned by the user).
             *
             * Using your own server to store and verify developer payloads across app
             * installations is recommended.
             */

            return true;
        }

        void saveData() {

            /*
             * WARNING: on a real application, we recommend you save data in a secure way to
             * prevent tampering. For simplicity in this sample, we simply store the data using a
             * SharedPreferences.
             */

            SharedPreferences.Editor spe = getPreferences(mContext.MODE_PRIVATE).edit();
            spe.putBoolean("standard", payment);
            spe.apply();
            Log.d(TAG, "Saved data: payment = " + String.valueOf(payment));
        }

        void loadData() {
            SharedPreferences sp = getPreferences(mContext.MODE_PRIVATE);
            payment = sp.getBoolean("payment", false);
            Log.d(TAG, "Loaded data: payment = " + String.valueOf(payment));
        }

        // Enables or disables the "please wait" screen.
        void setWaitScreen(boolean set) {
            mActivity.findViewById(R.id.my_recycler_view).setVisibility(set ? View.GONE : View.VISIBLE);
            mActivity.findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
        }

        public SharedPreferences getPreferences(int mode) {
            return mContext.getSharedPreferences(mActivity.getLocalClassName(), mode);
        }
}
