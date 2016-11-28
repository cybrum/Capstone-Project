
package com.udacity.nanodegree.mystockhealth.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.udacity.nanodegree.mystockhealth.R;
import com.udacity.nanodegree.mystockhealth.data.QuoteColumns;
import com.udacity.nanodegree.mystockhealth.data.QuoteProvider;
import com.udacity.nanodegree.mystockhealth.rest.QuoteCursorAdapter;
import com.udacity.nanodegree.mystockhealth.rest.RecyclerViewItemClickListener;
import com.udacity.nanodegree.mystockhealth.gesture.ItemTouchHelperCallback;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.udacity.nanodegree.mystockhealth.ui.StockDetailActivity;
import com.udacity.nanodegree.mystockhealth.ui.StockIntentService;
import com.udacity.nanodegree.mystockhealth.ui.StockTaskService;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StockListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, RecyclerViewItemClickListener.OnItemClickListener {

    public static final int CHANGE_UNITS_DOLLARS = 0;
    public static final int CHANGE_UNITS_PERCENTAGES = 1;
    private static final int CURSOR_LOADER_ID = 0;
    private final String EXTRA_CHANGE_UNITS = "EXTRA_CHANGE_UNITS";
    private final String EXTRA_ADD_DIALOG_OPENED = "EXTRA_ADD_DIALOG_OPENED";

    public static final String ANONYMOUS = "anonymous";
    public static final int RC_SIGN_IN = 1;

    private int mChangeUnits = CHANGE_UNITS_DOLLARS;
    private QuoteCursorAdapter mAdapter;
    private boolean mTwoPane;
    private MaterialDialog mDialog;


    private EditText mQuantity;
    private EditText mPurchasedValue;
    private EditText mStockSymbol;
    //Firebase instances
    private FirebaseAuth mFirebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private ChildEventListener mChildEventListener;

    private String mUsername;
    private String quantity, purchase, symbol = "";

    @Bind(R.id.stock_list)
    RecyclerView mRecyclerView;
    @Bind(R.id.empty_state_no_connection)
    View mEmptyStateNoConnection;
    @Bind(R.id.empty_state_no_stocks)
    View mEmptyStateNoStocks;
    @Bind(R.id.progress)
    ProgressBar mProgressBar;
    @Bind(R.id.coordinator_layout)
    CoordinatorLayout mCoordinatorLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsername = ANONYMOUS;
        setContentView(R.layout.activity_stock_list);
        ButterKnife.bind(this);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("stocks");


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is signed in
                    onSignedInInitialize(user.getDisplayName());
                } else {
                    // User is signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(
                                            AuthUI.EMAIL_PROVIDER,
                                            AuthUI.GOOGLE_PROVIDER)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayShowTitleEnabled(true);
//        }
        Toolbar mToolbar = (Toolbar)findViewById(R.id.toolbar );
        setSupportActionBar(mToolbar );

        if (findViewById(R.id.stock_detail_container) != null) {
            mTwoPane = true;
        }

        if (savedInstanceState == null) {
            // The intent service is for executing immediate pulls from the Yahoo API
            // GCMTaskService can only schedule tasks, they cannot execute immediately
            Intent stackServiceIntent = new Intent(this, StockIntentService.class);
            // Run the initialize task service so that some stocks appear upon an empty database
            stackServiceIntent.putExtra(StockIntentService.EXTRA_TAG, StockIntentService.ACTION_INIT);
            if (isNetworkAvailable()) {
                startService(stackServiceIntent);
            } else {
                Snackbar.make(mCoordinatorLayout, getString(R.string.no_internet_connection),
                        Snackbar.LENGTH_LONG).show();
            }
        } else {
            mChangeUnits = savedInstanceState.getInt(EXTRA_CHANGE_UNITS);
            if (savedInstanceState.getBoolean(EXTRA_ADD_DIALOG_OPENED, false)) {
                showDialogForAddingStock();
            }
        }

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this, this));

        mAdapter = new QuoteCursorAdapter(this, null, mChangeUnits);
        mRecyclerView.setAdapter(mAdapter);

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        // Create a periodic task to pull stocks once every hour after the app has been opened.
        // This is so Widget data stays up to date.
        PeriodicTask periodicTask = new PeriodicTask.Builder()
                .setService(StockTaskService.class)
                .setPeriod(/* 1h */ 60 * 60)
                .setFlex(/* 10s */ 10)
                .setTag(StockTaskService.TAG_PERIODIC)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .build();
        // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
        // are updated.
        GcmNetworkManager.getInstance(this).schedule(periodicTask);

    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        detachDatabaseReadListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.stocks_activity, menu);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_CHANGE_UNITS, mChangeUnits);
        if (mDialog != null) {
            outState.putBoolean(EXTRA_ADD_DIALOG_OPENED, mDialog.isShowing());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            case R.id.action_change_units: {
                if (mChangeUnits == CHANGE_UNITS_DOLLARS) {
                    mChangeUnits = CHANGE_UNITS_PERCENTAGES;
                } else {
                    mChangeUnits = CHANGE_UNITS_DOLLARS;
                }
                mAdapter.setChangeUnits(mChangeUnits);
                mAdapter.notifyDataSetChanged();
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        mEmptyStateNoConnection.setVisibility(View.GONE);
        mEmptyStateNoStocks.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.QUANTITY, QuoteColumns.PURCHASE_COST, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mProgressBar.setVisibility(View.GONE);
        mAdapter.swapCursor(data);

        if (mAdapter.getItemCount() == 0) {
            if (!isNetworkAvailable()) {
                mEmptyStateNoConnection.setVisibility(View.VISIBLE);
            } else {
                mEmptyStateNoStocks.setVisibility(View.VISIBLE);
            }
        } else {
            mEmptyStateNoConnection.setVisibility(View.GONE);
            mEmptyStateNoStocks.setVisibility(View.GONE);
        }

        if (!isNetworkAvailable()) {
            Snackbar.make(mCoordinatorLayout, getString(R.string.offline),
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.try_again, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, StockListActivity.this);
                }
            }).show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.fab)
    public void showDialogForAddingStock() {
        if (isNetworkAvailable()) {
            final View customView;
            boolean wrapInScrollView = true;
            customView = LayoutInflater.from(StockListActivity.this).inflate(R.layout.add_dialog_layout, null);
            mDialog = new MaterialDialog.Builder(this)
                    .title(R.string.add_symbol)
                    .customView(customView, wrapInScrollView)
                    .autoDismiss(true)
                    .positiveText(R.string.add)
                    .negativeText(R.string.disagree)
                    .build();
            mDialog.show();
            View view = mDialog.getCustomView();
            mQuantity = (EditText) view.findViewById(R.id.quatity);
            mPurchasedValue = (EditText) view.findViewById(R.id.purchased_value);
            mStockSymbol = (EditText) view.findViewById(R.id.addSymbol);
            View positive = mDialog.getActionButton(DialogAction.POSITIVE);
            positive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if( mQuantity.getText().toString().length() == 0 ) {
                        //mQuantity.setError("Quantity is required!");
                        quantity ="0";
                    } else {
                        quantity = mQuantity.getText().toString();
                    }
                    if( mPurchasedValue.getText().toString().length() == 0 ) {
                        //mPurchasedValue.setError("Purchase Value is required!");
                        purchase ="0.00";
                    } else {
                        purchase = mPurchasedValue.getText().toString();
                    }
                    if( mStockSymbol.getText().toString().length() == 0 ) {
                        //mStockSymbol.setError("Symbol is required!");
                        symbol ="";
                    } else {
                        symbol = mStockSymbol.getText().toString();
                    }
                    addStockQuote(symbol.replaceAll("\\s", "").toUpperCase(), quantity, purchase);
                    mDialog.dismiss();
                }
            });

        } else {
            Snackbar.make(mCoordinatorLayout, getString(R.string.no_internet_connection),
                    Snackbar.LENGTH_LONG).setAction(R.string.try_again, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDialogForAddingStock();
                }
            }).show();
        }
    }

    @Override
    public void onItemClick(View v, int position) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(StockDetailFragment.ARG_SYMBOL, mAdapter.getSymbol(position));
            StockDetailFragment fragment = new StockDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.stock_detail_container, fragment)
                    .commit();
        } else {
            Context context = v.getContext();
            Intent intent = new Intent(context, StockDetailActivity.class);
            intent.putExtra(StockDetailFragment.ARG_SYMBOL, mAdapter.getSymbol(position));
            context.startActivity(intent);
        }
    }

    private void addStockQuote(final String stockQuote, final String quantity, final String value) {
        // On FAB click, receive user input. Make sure the stock doesn't already exist
        // in the DB and proceed accordingly.
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                Cursor cursor = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns.SYMBOL},
                        QuoteColumns.SYMBOL + "= ?",
                        new String[]{stockQuote},
                        null);
                if (cursor != null) {
                    cursor.close();
                    return cursor.getCount() != 0;
                }
                return Boolean.FALSE;
            }

            @Override
            protected void onPostExecute(Boolean stockAlreadySaved) {
                if (stockAlreadySaved) {
                    Snackbar.make(mCoordinatorLayout, R.string.stock_already_saved,
                            Snackbar.LENGTH_LONG).show();
                } else {
                    StockEntry stockEntry = new StockEntry(symbol, Integer.parseInt(quantity), Double.parseDouble(purchase));
                    if(!symbol.isEmpty()) {
                        mMessagesDatabaseReference.push().setValue(stockEntry);
                    }

                    Intent stockIntentService = new Intent(StockListActivity.this,
                            StockIntentService.class);
                    stockIntentService.putExtra(StockIntentService.EXTRA_TAG, StockIntentService.ACTION_ADD);
                    stockIntentService.putExtra(StockIntentService.EXTRA_SYMBOL, stockQuote);
                    startService(stockIntentService);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, R.string.signed_in, Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, R.string.signin_cancelled, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    StockEntry stockEntry = dataSnapshot.getValue(StockEntry.class);
                    //Create an Adaptor and Sync with SQLiteDB
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }
}
