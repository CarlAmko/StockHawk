package com.udacity.stockhawk.sync;

import android.app.IntentService;
import android.content.Intent;

import timber.log.Timber;


public class QuoteIntentService extends IntentService {

    public QuoteIntentService() {
        super(QuoteIntentService.class.getSimpleName());
    }

    public static final String ACTION_REFRESH_ALL_QUOTES = "refresh-all-quotes";

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("Intent handled");

        switch (intent.getAction()) {
            case ACTION_REFRESH_ALL_QUOTES:
                QuoteSyncJob.getQuotes(getApplicationContext());
                break;
            default:
                break;
        }
    }
}
