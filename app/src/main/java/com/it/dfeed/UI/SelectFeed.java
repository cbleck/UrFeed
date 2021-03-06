package com.it.dfeed.UI;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.it.dfeed.Modelo.FeedModel;
import com.it.dfeed.R;
import com.it.dfeed.util.IabBroadcastReceiver;
import com.it.dfeed.util.IabHelper;
import com.it.dfeed.util.IabResult;
import com.it.dfeed.util.Inventory;
import com.it.dfeed.util.Purchase;

import java.util.ArrayList;
import java.util.List;

public class SelectFeed extends AppCompatActivity implements IabBroadcastReceiver.IabBroadcastListener {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private List<FeedModel> feedModelList;

    static final String TAG = "UrFeed";

    private boolean payment1, payment2, payment3, payment4, paymentpro;
    // The helper object
    private IabHelper mHelper;
    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;

    // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
    static final String SKU_STANDARD1 = "standard1";
    static final String SKU_STANDARD2 = "standard2";
    static final String SKU_STANDARD3 = "standard3";
    static final String SKU_STANDARD4 = "standard4";
    static final String SKU_PRO = "pro";

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_feed);

        // load game data
        loadData();

        /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjIrHnhd7/vZmmCCGyvuZdl3pg1URRnckwer5pVy1S3m6H/FnbapJfcbRv2H7dEoFoypns6NHr7dFxCPMDK1mt7zEH8Tpoz+Lka76fJZLycCxMQhChGiAHPmncELX0067iKpXSCYzqQdjfS1HaFwvwJXy49+ExDAiCvIFV66OuwOoVrqwakDdZTUn+BN9yVuL3qDktgf7ae1rbTR85kBAC4EN5S7Hm8R3XjiAhG1tOpcIXt/lVdW85WAbSv7GVe/nomeTGjztntW8bcJaKGNl4oWBqsdBAESVWzPz1q9eRedbWBNNUTNlfLEBRZEoJ/HN6L67FULgT54swUWbTCowUwIDAQAB";

        // Some sanity checks to see if the developer (that's you!) really followed the
        // instructions to run this sample (don't put these checks on your app!)
        if (base64EncodedPublicKey.contains("CONSTRUCT_YOUR")) {
            throw new RuntimeException("Please put your app's public key in MainActivity.java. See README.");
        }
        if (getPackageName().startsWith("com.example")) {
            throw new RuntimeException("Please change the sample's package name! See README.");
        }

        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(SelectFeed.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        feedModelList = initializeFeed();
        // specify an adapter (see also next example)
        mAdapter = new SelectFeedAdapter(this, feedModelList);
        mRecyclerView.setAdapter(mAdapter);
    }


    private List<FeedModel> initializeFeed(){
        List<FeedModel> fml = new ArrayList<>();
        fml.add(new FeedModel("Forbes","http://www.forbes.com/most-popular/feed/",Integer.toString(R.drawable.forbes)));
        fml.add(new FeedModel("Hipertextual","http://feed.hipertextual.com/",Integer.toString(R.drawable.hipertextual)));
        fml.add(new FeedModel("Xataka","http://feeds.weblogssl.com/xataka2",Integer.toString(R.drawable.mashable)));
        fml.add(new FeedModel("Mashable","http://feeds.mashable.com/Mashable",Integer.toString(R.drawable.xataka)));
        fml.add(new FeedModel("Wired","http://www.wired.com/feed/",Integer.toString(R.drawable.wired)));
        return fml;
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?

            // Check for gas delivery -- if we own gas, we should fill up the tank immediately
            Purchase standard1Purchase = inventory.getPurchase(SKU_STANDARD1);
            Purchase standard2Purchase = inventory.getPurchase(SKU_STANDARD2);
            Purchase standard3Purchase = inventory.getPurchase(SKU_STANDARD3);
            Purchase standard4Purchase = inventory.getPurchase(SKU_STANDARD3);
            Purchase proPurchase = inventory.getPurchase(SKU_PRO);
            if (standard1Purchase != null && verifyDeveloperPayload(standard1Purchase)) {
                Log.d(TAG, "We have payement1. Consuming it.");
                try {
                    mHelper.consumeAsync(inventory.getPurchase(SKU_STANDARD1), mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming payment1. Another async operation in progress.");
                }
                return;
            }
            else if (standard2Purchase != null && verifyDeveloperPayload(standard2Purchase)) {
                Log.d(TAG, "We have payement2. Consuming it.");
                try {
                    mHelper.consumeAsync(inventory.getPurchase(SKU_STANDARD2), mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming payment2. Another async operation in progress.");
                }
                return;
            }
            else if (standard3Purchase != null && verifyDeveloperPayload(standard3Purchase)) {
                Log.d(TAG, "We have payement3. Consuming it.");
                try {
                    mHelper.consumeAsync(inventory.getPurchase(SKU_STANDARD3), mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming payment3. Another async operation in progress.");
                }
                return;
            }
            else if (standard4Purchase != null && verifyDeveloperPayload(standard4Purchase)) {
                Log.d(TAG, "We have payement4. Consuming it.");
                try {
                    mHelper.consumeAsync(inventory.getPurchase(SKU_STANDARD4), mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming payment4. Another async operation in progress.");
                }
                return;
            }
            else if (proPurchase != null && verifyDeveloperPayload(proPurchase)) {
                Log.d(TAG, "We have payementpro. Consuming it.");
                try {
                    mHelper.consumeAsync(inventory.getPurchase(SKU_PRO), mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming payementpro. Another async operation in progress.");
                }
                return;
            }
            setWaitScreen(false);
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
    }


    public void onClickFeed(int position, List<FeedModel> mDataset){

        if(position == 0) {
            if (payment1) {
                // item clicked
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra("com.bleck.feedname", mDataset.get(position).getName());
                i.putExtra("com.bleck.feedurl", mDataset.get(position).getLink());
                i.putExtra("com.bleck.feedimg", Integer.parseInt(mDataset.get(position).getBckImg()));
                startActivity(i);
            } else {
                // launch the gas purchase UI flow.
                // We will be notified of completion via mPurchaseFinishedListener
                setWaitScreen(true);
                Log.d(TAG, "Launching purchase flow for payment1.");

                        /* TODO: for security, generate your payload here for verification. See the comments on
                         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
                         *        an empty string, but on a production app you should carefully generate this. */
                String payload = "";

                try {
                    mHelper.launchPurchaseFlow(this, SKU_STANDARD1, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error launching purchase flow. Another async operation in progress.");
                    setWaitScreen(false);
                }
            }
        }
        if(position == 1) {
            if (payment2) {
                // item clicked
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra("com.bleck.feedname", mDataset.get(position).getName());
                i.putExtra("com.bleck.feedurl", mDataset.get(position).getLink());
                i.putExtra("com.bleck.feedimg", Integer.parseInt(mDataset.get(position).getBckImg()));
                startActivity(i);
            } else {
                // launch the gas purchase UI flow.
                // We will be notified of completion via mPurchaseFinishedListener
                setWaitScreen(true);
                Log.d(TAG, "Launching purchase flow for payment2.");

                        /* TODO: for security, generate your payload here for verification. See the comments on
                         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
                         *        an empty string, but on a production app you should carefully generate this. */
                String payload = "";

                try {
                    mHelper.launchPurchaseFlow(this, SKU_STANDARD2, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error launching purchase flow. Another async operation in progress.");
                    setWaitScreen(false);
                }
            }
        }
        if(position == 2) {
            if (payment3) {
                // item clicked
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra("com.bleck.feedname", mDataset.get(position).getName());
                i.putExtra("com.bleck.feedurl", mDataset.get(position).getLink());
                i.putExtra("com.bleck.feedimg", Integer.parseInt(mDataset.get(position).getBckImg()));
                startActivity(i);
            } else {
                // launch the gas purchase UI flow.
                // We will be notified of completion via mPurchaseFinishedListener
                setWaitScreen(true);
                Log.d(TAG, "Launching purchase flow for payment3.");

                        /* TODO: for security, generate your payload here for verification. See the comments on
                         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
                         *        an empty string, but on a production app you should carefully generate this. */
                String payload = "";

                try {
                    mHelper.launchPurchaseFlow(this, SKU_STANDARD3, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error launching purchase flow. Another async operation in progress.");
                    setWaitScreen(false);
                }
            }
        }
        if(position == 3) {
            if (payment4) {
                // item clicked
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra("com.bleck.feedname", mDataset.get(position).getName());
                i.putExtra("com.bleck.feedurl", mDataset.get(position).getLink());
                i.putExtra("com.bleck.feedimg", Integer.parseInt(mDataset.get(position).getBckImg()));
                startActivity(i);
            } else {
                // launch the gas purchase UI flow.
                // We will be notified of completion via mPurchaseFinishedListener
                setWaitScreen(true);
                Log.d(TAG, "Launching purchase flow for payment4.");

                        /* TODO: for security, generate your payload here for verification. See the comments on
                         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
                         *        an empty string, but on a production app you should carefully generate this. */
                String payload = "";

                try {
                    mHelper.launchPurchaseFlow(this, SKU_STANDARD4, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error launching purchase flow. Another async operation in progress.");
                    setWaitScreen(false);
                }
            }
        }
        if(position == 4) {
            if (paymentpro) {
                // item clicked
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra("com.bleck.feedname", mDataset.get(position).getName());
                i.putExtra("com.bleck.feedurl", mDataset.get(position).getLink());
                i.putExtra("com.bleck.feedimg", Integer.parseInt(mDataset.get(position).getBckImg()));
                startActivity(i);
            } else {
                // launch the gas purchase UI flow.
                // We will be notified of completion via mPurchaseFinishedListener
                setWaitScreen(true);
                Log.d(TAG, "Launching purchase flow for paymentpro.");

                        /* TODO: for security, generate your payload here for verification. See the comments on
                         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
                         *        an empty string, but on a production app you should carefully generate this. */
                String payload = "";

                try {
                    mHelper.launchPurchaseFlow(this, SKU_PRO, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error launching purchase flow. Another async operation in progress.");
                    setWaitScreen(false);
                }
            }
        }

    }


    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
        alert("Error: " + message);
    }
    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
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

            if (purchase.getSku().equals(SKU_STANDARD1)) {
                // bought 1/4 tank of gas. So consume it.
                Log.d(TAG, "Purchase is standard. Starting standard1 consumption.");
                try {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming standard1. Another async operation in progress.");
                    setWaitScreen(false);
                    return;
                }
            }
            if (purchase.getSku().equals(SKU_STANDARD2)) {
                // bought 1/4 tank of gas. So consume it.
                Log.d(TAG, "Purchase is standard. Starting standard2 consumption.");
                try {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming standard2. Another async operation in progress.");
                    setWaitScreen(false);
                    return;
                }
            }
            if (purchase.getSku().equals(SKU_STANDARD3)) {
                // bought 1/4 tank of gas. So consume it.
                Log.d(TAG, "Purchase is standard. Starting standard3 consumption.");
                try {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming standard3. Another async operation in progress.");
                    setWaitScreen(false);
                    return;
                }
            }
            if (purchase.getSku().equals(SKU_STANDARD4)) {
                // bought 1/4 tank of gas. So consume it.
                Log.d(TAG, "Purchase is standard. Starting standard4 consumption.");
                try {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming standard4. Another async operation in progress.");
                    setWaitScreen(false);
                    return;
                }
            }
            if (purchase.getSku().equals(SKU_PRO)) {
                // bought 1/4 tank of gas. So consume it.
                Log.d(TAG, "Purchase is standard. Starting pro consumption.");
                try {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming pro. Another async operation in progress.");
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

                if(purchase.getSku().equals(SKU_STANDARD1))
                    payment1 = true;
                if(purchase.getSku().equals(SKU_STANDARD2))
                    payment2 = true;
                if(purchase.getSku().equals(SKU_STANDARD3))
                    payment3 = true;
                if(purchase.getSku().equals(SKU_STANDARD4))
                    payment4 = true;
                if(purchase.getSku().equals(SKU_PRO))
                    paymentpro = true;
                saveData();
                alert("You got now access to UrFeed! <"+purchase.getSku()+">");
            }
            else {
                complain("Error while consuming: " + result);
            }
            setWaitScreen(false);
            Log.d(TAG, "End consumption flow.");
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

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

        SharedPreferences.Editor spe = getPreferences(MODE_PRIVATE).edit();
        spe.putBoolean("payment1", payment1);
        spe.putBoolean("payment2", payment2);
        spe.putBoolean("payment3", payment3);
        spe.putBoolean("payment4", payment4);
        spe.putBoolean("paymentpro", paymentpro);
        spe.apply();
        Log.d(TAG, "Saved data: payment1 = " + String.valueOf(payment1));
        Log.d(TAG, "Saved data: payment2 = " + String.valueOf(payment2));
        Log.d(TAG, "Saved data: payment3 = " + String.valueOf(payment3));
        Log.d(TAG, "Saved data: payment4 = " + String.valueOf(payment4));
        Log.d(TAG, "Saved data: paymentpro = " + String.valueOf(paymentpro));
    }

    void loadData() {
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        payment1 = sp.getBoolean("payment1", false);
        payment2 = sp.getBoolean("payment2", false);
        payment3 = sp.getBoolean("payment3", false);
        payment4 = sp.getBoolean("payment4", false);
        paymentpro = sp.getBoolean("paymentpro", false);
        Log.d(TAG, "Loaded data: payment1 = " + String.valueOf(payment1));
        Log.d(TAG, "Loaded data: payment2 = " + String.valueOf(payment2));
        Log.d(TAG, "Loaded data: payment3 = " + String.valueOf(payment3));
        Log.d(TAG, "Loaded data: payment4 = " + String.valueOf(payment4));
        Log.d(TAG, "Loaded data: paymentpro = " + String.valueOf(paymentpro));
    }

    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
        findViewById(R.id.my_recycler_view).setVisibility(set ? View.GONE : View.VISIBLE);
        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }

    public SharedPreferences getPreferences(int mode) {
        return getSharedPreferences(getLocalClassName(), mode);
    }

    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }
}