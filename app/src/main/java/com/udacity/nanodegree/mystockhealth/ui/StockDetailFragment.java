/*
 * Copyright 2016.  Dmitry Malkovich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.udacity.nanodegree.mystockhealth.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.nanodegree.mystockhealth.R;
import com.udacity.nanodegree.mystockhealth.data.QuoteColumns;
import com.udacity.nanodegree.mystockhealth.data.QuoteHistoricalDataColumns;
import com.udacity.nanodegree.mystockhealth.data.QuoteProvider;
import com.udacity.nanodegree.mystockhealth.news.NewsActivity;
import com.udacity.nanodegree.mystockhealth.ui.StockDetailActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * A fragment representing a single Stock detail screen.
 * This fragment is either contained in a {@link StockListActivity}
 * in two-pane mode (on tablets) or a {@link StockDetailActivity}
 * on handsets.
 */
public class StockDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        TabHost.OnTabChangeListener {

    @SuppressWarnings("unused")
    public static String LOG_TAG = StockDetailFragment.class.getSimpleName();
    public static final String ARG_SYMBOL = "ARG_SYMBOL";
    public static final String EXTRA_CURRENT_TAB = "EXTRA_CURRENT_TAB";
    private static final int CURSOR_LOADER_ID = 1;
    private static final int CURSOR_LOADER_ID_FOR_LINE_CHART = 2;

    private String mSymbol;
    private String mSelectedTab;

    @Bind(R.id.stock_symbol)
    TextView mSymbolView;
    @Bind(R.id.stock_bidprice)
    TextView mEbitdaView;
    @Bind(android.R.id.tabhost)
    TabHost mTabHost;
    @Bind(R.id.stock_chart)
    LineChartView mChart;
    @Bind(R.id.stock_change)
    TextView mChange;
    @Bind(android.R.id.tabcontent)
    View mTabContent;

    public StockDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_SYMBOL)) {
            mSymbol = getArguments().getString(ARG_SYMBOL);
        }

        if (getActionBar() != null) {
            getActionBar().setElevation(0);
            if (getActivity() instanceof StockDetailActivity) {
                getActionBar().setTitle("");
            }
        }
        if (savedInstanceState == null) {
            mSelectedTab = getString(R.string.stock_detail_tab1);
        } else {
            mSelectedTab = savedInstanceState.getString(EXTRA_CURRENT_TAB);
        }

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        getLoaderManager().initLoader(CURSOR_LOADER_ID_FOR_LINE_CHART, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.stock_detail, container, false);
        ButterKnife.bind(this, rootView);
        setupTabs();
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_CURRENT_TAB, mSelectedTab);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == CURSOR_LOADER_ID) {
            return new CursorLoader(getContext(), QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP,
                            QuoteColumns.NAME},
                    QuoteColumns.SYMBOL + " = \"" + mSymbol + "\"",
                    null, null);
        } else if (id == CURSOR_LOADER_ID_FOR_LINE_CHART) {

            String sortOrder = QuoteColumns._ID + " ASC LIMIT 5";
            if (mSelectedTab.equals(getString(R.string.stock_detail_tab2))) {
                sortOrder = QuoteColumns._ID + " ASC LIMIT 14";
            } else if (mSelectedTab.equals(getString(R.string.stock_detail_tab3))) {
                sortOrder = QuoteColumns._ID + " ASC";
            }

            return new CursorLoader(getContext(), QuoteProvider.QuotesHistoricData.CONTENT_URI,
                    new String[]{QuoteHistoricalDataColumns._ID, QuoteHistoricalDataColumns.SYMBOL,
                            QuoteHistoricalDataColumns.BIDPRICE, QuoteHistoricalDataColumns.DATE},
                    QuoteHistoricalDataColumns.SYMBOL + " = \"" + mSymbol + "\"",
                    null, sortOrder);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == CURSOR_LOADER_ID && data != null && data.moveToFirst()) {

            String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
            mSymbolView.setText(getString(R.string.stock_detail_tab_header, symbol));

            String ebitda = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));
            mEbitdaView.setText(ebitda);

            String name = data.getString(data.getColumnIndex(QuoteColumns.NAME));
            if (getActionBar() != null) {
                getActionBar().setTitle(name);
            }

            String change = data.getString(data.getColumnIndex(QuoteColumns.CHANGE));
            String percentChange = data.getString(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
            String mixedChange = change + " (" + percentChange + ")";
            mChange.setText(mixedChange);

        } else if (loader.getId() == CURSOR_LOADER_ID_FOR_LINE_CHART && data != null &&
                data.moveToFirst()) {
            updateChart(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Nothing to do
    }

    @Override
    public void onTabChanged(String tabId) {
        mSelectedTab = tabId;
        getLoaderManager().restartLoader(CURSOR_LOADER_ID_FOR_LINE_CHART, null, this);
    }

    @Nullable
    private ActionBar getActionBar() {
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            return activity.getSupportActionBar();
        }
        return null;
    }

    private void setupTabs() {
        mTabHost.setup();

        TabHost.TabSpec tabSpec;
        tabSpec = mTabHost.newTabSpec(getString(R.string.stock_detail_tab1));
        tabSpec.setIndicator(getString(R.string.stock_detail_tab1));
        tabSpec.setContent(android.R.id.tabcontent);
        mTabHost.addTab(tabSpec);

        tabSpec = mTabHost.newTabSpec(getString(R.string.stock_detail_tab2));
        tabSpec.setIndicator(getString(R.string.stock_detail_tab2));
        tabSpec.setContent(android.R.id.tabcontent);
        mTabHost.addTab(tabSpec);

        tabSpec = mTabHost.newTabSpec(getString(R.string.stock_detail_tab3));
        tabSpec.setIndicator(getString(R.string.stock_detail_tab3));
        tabSpec.setContent(android.R.id.tabcontent);
        mTabHost.addTab(tabSpec);

        mTabHost.setOnTabChangedListener(this);

        if (mSelectedTab.equals(getString(R.string.stock_detail_tab2))) {
            mTabHost.setCurrentTab(1);
        } else if (mSelectedTab.equals(getString(R.string.stock_detail_tab3))) {
            mTabHost.setCurrentTab(2);
        } else {
            mTabHost.setCurrentTab(0);
        }
    }

    private void updateChart(Cursor data) {

        List<AxisValue> axisValuesX = new ArrayList<>();
        List<PointValue> pointValues = new ArrayList<>();

        int counter = -1;
        do {
            counter++;

            String date = data.getString(data.getColumnIndex(
                    QuoteHistoricalDataColumns.DATE));
            String bidPrice = data.getString(data.getColumnIndex(
                    QuoteHistoricalDataColumns.BIDPRICE));

            // We have to show chart in right order.
            int x = data.getCount() - 1 - counter;

            // Point for line chart (date, price).
            PointValue pointValue = new PointValue(x, Float.valueOf(bidPrice));
            pointValue.setLabel(date);
            pointValues.add(pointValue);

            // Set labels for x-axis (we have to reduce its number to avoid overlapping text).
            if (counter != 0 && counter % (data.getCount() / 3) == 0) {
                AxisValue axisValueX = new AxisValue(x);
                axisValueX.setLabel(date);
                axisValuesX.add(axisValueX);
            }

        } while (data.moveToNext());

        // Prepare data for chart
        Line line = new Line(pointValues).setColor(Color.WHITE).setCubic(false);
        List<Line> lines = new ArrayList<>();
        lines.add(line);
        LineChartData lineChartData = new LineChartData();
        lineChartData.setLines(lines);

        // Init x-axis
        Axis axisX = new Axis(axisValuesX);
        axisX.setHasLines(true);
        axisX.setMaxLabelChars(4);
        lineChartData.setAxisXBottom(axisX);

        // Init y-axis
        Axis axisY = new Axis();
        axisY.setAutoGenerated(true);
        axisY.setHasLines(true);
        axisY.setMaxLabelChars(4);
        lineChartData.setAxisYLeft(axisY);

        // Update chart with new data.
        mChart.setInteractive(false);
        mChart.setLineChartData(lineChartData);

        // Show chart
        mChart.setVisibility(View.VISIBLE);
        mTabContent.setVisibility(View.VISIBLE);
    }
//    @Override
//    public void onClick(View view) {
//        String symbol=StockDetailActivity.this.getIntent().getExtras().getString("Symbol");
//        Intent i=new Intent(this,NewsActivity.class);
//        i.putExtra("Symbol",symbol);
//        Toast.makeText(StockDetailActivity.this.getApplicationContext(),(String)symbol,Toast.LENGTH_SHORT).show();
//        startActivity(i);
//    }
}
