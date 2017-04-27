package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.util.DeviceUtility;
import com.udacity.stockhawk.util.ThemeUtility;

import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

public class StockDetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<HistoricalQuote>> {

    private final static String TAG = StockDetailsActivity.class.getSimpleName();
    private static final int STOCK_HISTORY_LOADER_ID = 302;
    private final int NUMBER_OF_MONTHS = 12;

    public static final String STOCK_SYMBOL_EXTRA = "stock-symbol";
    private String stockSymbol;

    @BindView(R.id.tv_stock_symbol)
    TextView stockSymbolTextView;

    @BindView(R.id.pb_stock_details)
    ProgressBar detailsProgressBar;

    @BindView(R.id.lc_stock_graph)
    LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load theme based on preferences.
        PrefUtils.setThemeDisplay(this);
        setContentView(R.layout.activity_stock_details);
        ButterKnife.bind(this);

        Intent incomingIntent = getIntent();

        if(incomingIntent.hasExtra(STOCK_SYMBOL_EXTRA)) {
            stockSymbol = incomingIntent.getStringExtra(STOCK_SYMBOL_EXTRA);
            stockSymbolTextView.setText(stockSymbol);
        }
        else {
            stockSymbol = null;
            Log.e(TAG, "onCreate: StockDetailsActivity transition intent did not have stock symbol attached!");
            return;
        }

        getSupportLoaderManager().restartLoader(STOCK_HISTORY_LOADER_ID, null, this);
    }

    @Override
    public Loader<List<HistoricalQuote>> onCreateLoader(int id, Bundle args) {
        // Ensure loader requested is stock history loader.
        if(id == STOCK_HISTORY_LOADER_ID) {
            Loader<List<HistoricalQuote>> stockHistoryLoader = new AsyncTaskLoader<List<HistoricalQuote>>(getApplicationContext()) {
                @Override
                public List<HistoricalQuote> loadInBackground() {
                    try {
                        Calendar from = Calendar.getInstance();
                        Calendar to = Calendar.getInstance();
                        from.add(Calendar.MONTH, -(NUMBER_OF_MONTHS - 1));
                        return YahooFinance.get(StockDetailsActivity.this.stockSymbol).getHistory(from, to, Interval.MONTHLY);
                    } catch (IOException e) {
                        Log.e(TAG, "loadInBackground: " + e.getMessage());
                    }
                    return null;
                }
            };
            stockHistoryLoader.forceLoad();
            return stockHistoryLoader;
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<HistoricalQuote>> loader, List<HistoricalQuote> data) {
        // Hide progress bar.
        detailsProgressBar.setVisibility(View.GONE);

        // Show chart views.
        stockSymbolTextView.setVisibility(View.VISIBLE);
        lineChart.setVisibility(View.VISIBLE);

        Log.d(TAG, "onLoadFinished: " + data.toString());
        List<Entry> dataEntries = new ArrayList<>();

        // Sort the data as chronologically ascending up to the current month.
        Collections.reverse(data);

        // Since LTR is more common, check for this orientation first.
        if(!DeviceUtility.isRTL(this)) {
            for (int i = 0; i < data.size(); ++i) {
                dataEntries.add(new Entry(i, data.get(i).getClose().floatValue()));
            }
        }
        // If device is configured as RTL, the data needs to be filled in backwards order.
        else {
            for (int i = data.size() - 1; i > 0; --i) {
                dataEntries.add(new Entry(data.size() - 1 - i, data.get(i).getClose().floatValue()));
            }
        }

        Collections.sort(dataEntries, new EntryXComparator());
        Log.d(TAG, "onLoadFinished: " + dataEntries);

        // Create a data set from the quote history.
        LineDataSet dataSet = new LineDataSet(dataEntries, null);

        // Cache theme-specific colors.
        final int themeTextColor = ThemeUtility.getAttributeFromCurrentTheme(this, android.R.attr.textColor);
        dataSet.setValueTextColor(themeTextColor);
        stockSymbolTextView.setTextColor(themeTextColor);

        // Enable and set gradient fill.
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(getResources().getDrawable(R.drawable.fade_accent));

        // Set draw mode to rounded bezier.
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setCubicIntensity(0.4f);

        // Disable legend.
        dataSet.setForm(Legend.LegendForm.NONE);

        // Disable graph point selection.
        dataSet.setHighlightEnabled(false);

        // Make graph value text larger.
        dataSet.setValueTextSize(12);

        // Style X axis.
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(themeTextColor);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(NUMBER_OF_MONTHS - 1);
        xAxis.setDrawGridLines(false);

        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int boundedValue;
                // Since LTR is more common, check for this orientation first.
                if(!DeviceUtility.isRTL(StockDetailsActivity.this)) {
                    // Bind the entry value to (NUMBER_OF_MONTHS - 1) - value months before the current month.
                    // Ex: If value is 0, this is 11 months before the current month.
                    boundedValue = ((int) value + Calendar.getInstance().get(Calendar.MONTH) + NUMBER_OF_MONTHS + 1) % NUMBER_OF_MONTHS;
                } else {
                    // If RTL is enabled, each bounded value is reversed.
                    boundedValue = (((int)value - ((NUMBER_OF_MONTHS - Calendar.getInstance().get(Calendar.MONTH) + ((int)value * 2)) % NUMBER_OF_MONTHS)) + NUMBER_OF_MONTHS) % NUMBER_OF_MONTHS;
                }

                return new DateFormatSymbols().getShortMonths()[boundedValue];
            }
        };
        xAxis.setValueFormatter(formatter);

        // Style left Y axis.
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setTextColor(themeTextColor);
        yAxis.setDrawGridLines(false);

        // Turn off right Y axis.
        lineChart.getAxisRight().setEnabled(false);

        // Clear description on chart.
        Description description = new Description();
        description.setText("");
        lineChart.setDescription(description);

        // Assign the fetched data and render.
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    @Override
    public void onLoaderReset(Loader<List<HistoricalQuote>> loader) {
        loader.reset();
    }
}
