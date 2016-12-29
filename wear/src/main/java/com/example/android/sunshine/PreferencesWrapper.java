package com.example.android.sunshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Carla
 * Date: 28/12/2016
 * Project: Sunshine-Project6
 */

public class PreferencesWrapper {
    private static final String KEY_HIGH_TEMP = "mHighTemp";
    private static final String KEY_LOW_TEMP = "mLowTemp";
    private static final String KEY_BITMAP = "bitmap";

    private SharedPreferences mSharedPreferences;


    public PreferencesWrapper(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void saveHighTemperature(int highTemp) {
        mSharedPreferences.edit().putInt(KEY_HIGH_TEMP, highTemp).apply();
    }

    public void saveLowTemperature(int lowTemp) {
        mSharedPreferences.edit().putInt(KEY_LOW_TEMP, lowTemp).apply();
    }

    public void saveWeatherIcon(String icon) {
        if (icon != null) {
            mSharedPreferences.edit()
                    .putString(KEY_BITMAP, icon)
                    .apply();
        }
    }

    public int getHighTemperature() {
        return mSharedPreferences.getInt(KEY_HIGH_TEMP, 0);
    }

    public int getLowTemperature() {
        return mSharedPreferences.getInt(KEY_LOW_TEMP, 0);
    }

    public String getWeatherIcon() {
        return mSharedPreferences.getString(KEY_BITMAP, null);
    }

}
