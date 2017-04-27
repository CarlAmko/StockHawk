package com.udacity.stockhawk.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import com.udacity.stockhawk.R;

/**
 * Created by Carl on 4/25/2017.
 */

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_pref);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        for(int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
            Preference preference = getPreferenceScreen().getPreference(i);
            setPreferenceSummary(preference, preference.getKey());
        }

        // Register self as shared preference change listener.
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setPreferenceSummary(getPreferenceScreen().findPreference(key), key);


    }

    private void setPreferenceSummary(Preference preference, String key) {
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        if(preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference)preference;
            int indexOfValue = listPreference.findIndexOfValue(sharedPreferences.getString(key, null));
            listPreference.setSummary(listPreference.getEntries()[indexOfValue]);
        } else {
            preference.setSummary(sharedPreferences.getString(key, null));
        }
    }
}
