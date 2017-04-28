package com.udacity.stockhawk.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.util.DeviceUtility;
import com.udacity.stockhawk.util.ThemeUtility;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;


public class AddStockDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Stock> {

    private static final int FETCH_SINGLE_QUOTE_LOADER = 9;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.dialog_stock)
    EditText stock;

    @BindView(R.id.pb_stock_lookup)
    ProgressBar stockLoadingBar;

    @BindView(R.id.tv_stock_summary)
    TextView stockSummaryTextView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View custom = inflater.inflate(R.layout.add_stock_dialog, null);

        ButterKnife.bind(this, custom);
        builder.setView(custom);

        builder.setMessage(getString(R.string.dialog_title));
        builder.setPositiveButton(getString(R.string.dialog_add),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        addStock();
                    }
                });
        builder.setNegativeButton(getString(R.string.dialog_cancel), null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // Turn off submit button until input is validated.
                disablePositiveButton();

                // Set accent text color.
                stockSummaryTextView.setTextColor(ThemeUtility.getAttributeFromCurrentTheme(AddStockDialog.this.getActivity(), R.attr.colorAccent));

                // Set negative button text color.
                final int enabledColorId = ThemeUtility.getAttributeFromCurrentTheme(getActivity(), R.attr.colorAccent);
                ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(enabledColorId);

                // If RTL enabled, pull views to the right.
                if(DeviceUtility.isRTL(getActivity())) {
                    ((TextView)alertDialog.findViewById(getResources().getIdentifier("alertTitle", "id", "android"))).setGravity(Gravity.END);
                    ((TextView)alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.END);
                    stock.setGravity(Gravity.END);
                    stockSummaryTextView.setGravity(Gravity.END);
                }
            }
        });

        // Add text change listener for looking up stock symbol.
        stock.addTextChangedListener(new TextWatcher() {

            Handler handler = new Handler(Looper.getMainLooper());
            Runnable stockRunnable;
            // Delay for 500ms between symbol checks to buffer user inputting rapidly.
            final long delayDuration = 500;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Disable positive button.
                disablePositiveButton();

                // Hide stock summary.
                stockSummaryTextView.setVisibility(View.GONE);

                // Show loading bar.
                stockLoadingBar.setVisibility(View.VISIBLE);

                handler.removeCallbacks(stockRunnable);
                stockRunnable = new Runnable() {
                    @Override
                    public void run() {
                        // Catch for firing after input delay interval and dialog was closed.
                        if(getActivity() == null) return;

                        // Reset and restart loader for checking symbol validity.
                        if(!isInputEmpty()) {
                            getLoaderManager().destroyLoader(FETCH_SINGLE_QUOTE_LOADER);
                            getLoaderManager().restartLoader(FETCH_SINGLE_QUOTE_LOADER, null, AddStockDialog.this);
                        } else {
                            // Hide loading bar.
                            stockLoadingBar.setVisibility(View.GONE);
                        }
                    }
                };

                handler.postDelayed(stockRunnable, delayDuration);
            }
        });

        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return alertDialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Ensure we stop the loader if the dialog loses focus while a load is occurring.
        getLoaderManager().destroyLoader(FETCH_SINGLE_QUOTE_LOADER);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        onCancel(dialog);
    }

    private void addStock() {
        Activity parent = getActivity();
        if (parent instanceof MainActivity) {
            ((MainActivity) parent).addStock(stock.getText().toString());
        }
        dismissAllowingStateLoss();
    }

    private boolean isInputEmpty() {
        return TextUtils.isEmpty(stock.getText());
    }

    @Override
    public Loader<Stock> onCreateLoader(int id, Bundle args) {
        // Ensure we're creating a single-quote loader.
        if(id == FETCH_SINGLE_QUOTE_LOADER) {
            AsyncTaskLoader<Stock> stockLoader = new AsyncTaskLoader<Stock>(getActivity()) {
                @Override
                public Stock loadInBackground() {
                    try {
                        return YahooFinance.get(stock.getText().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            stockLoader.forceLoad();
            return stockLoader;
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Stock> loader, Stock data) {
        // Hide loading bar.
        stockLoadingBar.setVisibility(View.GONE);

        // Show stock summary.
        stockSummaryTextView.setVisibility(View.VISIBLE);

        // Stock is valid.
        if(data != null && !TextUtils.isEmpty(data.getName())) {
            stockSummaryTextView.setText(String.format(getString(R.string.dialog_valid_stock_format), data.getName()));

            // Enable positive button.
            enablePositiveButton();
        } else {
            // Stock is not valid.
            if(!isInputEmpty()) {
                stockSummaryTextView.setText(String.format(getString(R.string.dialog_invalid_stock_format), stock.getText().toString()));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        loader.reset();
    }

    private void enablePositiveButton() {
        final AlertDialog alertDialog = getAlertDialog();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        final int enabledColorId = ThemeUtility.getAttributeFromCurrentTheme(getActivity(), R.attr.colorAccent);
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(enabledColorId);
    }

    private void disablePositiveButton() {
        final AlertDialog alertDialog = getAlertDialog();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
    }

    private AlertDialog getAlertDialog() {
        return (AlertDialog)getDialog();
    }
}
