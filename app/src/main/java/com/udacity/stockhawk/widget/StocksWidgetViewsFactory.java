package com.udacity.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.preference.PreferenceManager;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.ui.StockDetailsActivity;

import java.util.Locale;

import butterknife.BindView;
import timber.log.Timber;

/**
 * Created by Carl on 4/26/2017.
 */

public class StocksWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Cursor data;
    private Context context;

    private StocksWidgetViewsFactory() {}
    public StocksWidgetViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {}

    @Override
    public void onDataSetChanged() {
        if(data != null) {
            data.close();
        }

        final long identityToken = Binder.clearCallingIdentity();
        data = context.getContentResolver().query(Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, Contract.Quote.COLUMN_SYMBOL);

        Binder.restoreCallingIdentity(identityToken);
    }

    @Override
    public void onDestroy() {
        if(data != null) {
            data.close();
            data = null;
        }
    }

    @Override
    public int getCount() {
        return data == null ? 0 : data.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if(data == null ||
                position == AdapterView.INVALID_POSITION ||
                !data.moveToPosition(position)) {
            return null;
        }

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.stocks_widget_list_item);

        // Set stock symbol.
        final String stockSymbol = data.getString(Contract.Quote.POSITION_SYMBOL);
        remoteViews.setTextViewText(R.id.widget_symbol, stockSymbol);
        Timber.d("Widget view received data for symbol %s.", stockSymbol);

        // Set stock price.
        final Locale userLocale = context.getResources().getConfiguration().locale;
        final float stockPrice = data.getFloat(Contract.Quote.POSITION_PRICE);
        remoteViews.setTextViewText(R.id.widget_price, "$" + String.format(userLocale, "%.2f", stockPrice));
        Timber.d("Widget view received price data %.2f for symbol %s.", stockPrice, stockSymbol);

        // Check which display mode the user has saved as preference.
        boolean bDisplayAsAbsolute = PrefUtils.getDisplayMode(context).equals(context.getString(R.string.pref_display_mode_absolute_key));
        float change = bDisplayAsAbsolute ? data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE) :
                data.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

        // Set stock change background color and positive/negative text.
        if(change > 0) {
            remoteViews.setImageViewResource(R.id.widget_change, R.drawable.ic_arrow_upward_24dp);
        } else {
            remoteViews.setImageViewResource(R.id.widget_change, R.drawable.ic_arrow_downward_24dp);
        }

        final Intent fillInIntent = new Intent();
        fillInIntent.putExtra(StockDetailsActivity.STOCK_SYMBOL_EXTRA, stockSymbol);
        remoteViews.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(context.getPackageName(), R.layout.stocks_widget_list_item);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if(data.moveToPosition(position)) {
            return data.getLong(Contract.Quote.POSITION_ID);
        }

        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
