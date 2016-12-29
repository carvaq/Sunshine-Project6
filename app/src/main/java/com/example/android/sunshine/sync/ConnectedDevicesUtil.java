package com.example.android.sunshine.sync;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.example.android.sunshine.BuildConfig;
import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.NotificationUtils;
import com.example.android.sunshine.utilities.SunshineDateUtils;
import com.example.android.sunshine.utilities.SunshineWeatherUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

import static com.example.Constants.KEY_BITMAP;
import static com.example.Constants.KEY_HIGH_TEMP;
import static com.example.Constants.KEY_LOW_TEMP;
import static com.example.Constants.PATH_WEATHER;

/**
 * Created by Carla
 * Date: 28/12/2016
 * Project: Sunshine-Project6
 */

public class ConnectedDevicesUtil {
    private static final String TAG = ConnectedDevicesUtil.class.getSimpleName();

    private static GoogleApiClient mGoogleApiClient;

    public static void notifyDevices(Context context) {

        /* Build the URI for today's weather in order to show up to date data in notification */
        Uri todaysWeatherUri = WeatherContract.WeatherEntry
                .buildWeatherUriWithDate(SunshineDateUtils.normalizeDate(System.currentTimeMillis()));

        /*
         * The MAIN_FORECAST_PROJECTION array passed in as the second parameter is defined in our WeatherContract
         * class and is used to limit the columns returned in our cursor.
         */
        Cursor todayWeatherCursor = context.getContentResolver().query(
                todaysWeatherUri,
                NotificationUtils.WEATHER_NOTIFICATION_PROJECTION,
                null,
                null,
                null);

        /*
         * If todayWeatherCursor is empty, moveToFirst will return false. If our cursor is not
         * empty, we want to show the notification.
         */
        if (todayWeatherCursor != null && todayWeatherCursor.moveToFirst()) {
            initializeApiClient(context);
            mGoogleApiClient.blockingConnect();

            /* Weather ID as returned by API, used to identify the icon to be used */
            int weatherId = todayWeatherCursor.getInt(NotificationUtils.INDEX_WEATHER_ID);
            double high = todayWeatherCursor.getDouble(NotificationUtils.INDEX_MAX_TEMP);
            double low = todayWeatherCursor.getDouble(NotificationUtils.INDEX_MIN_TEMP);

            int resourceId = SunshineWeatherUtils
                    .getSmallArtResourceIdForWeatherCondition(weatherId);

            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
            String icon = Base64.encodeToString(toByteArray(bitmap), Base64.DEFAULT);

            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_WEATHER);
            putDataMapRequest.getDataMap().putDouble(KEY_HIGH_TEMP, high);
            putDataMapRequest.getDataMap().putDouble(KEY_LOW_TEMP, low);
            putDataMapRequest.getDataMap().putString(KEY_BITMAP, icon);

            if (BuildConfig.DEBUG) {
                putDataMapRequest.getDataMap().putLong("Time", System.currentTimeMillis());
            }

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();

            Log.d(TAG, "Generating DataItem: " + request);
            if (!mGoogleApiClient.isConnected()) {
                return;
            }

            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                        + dataItemResult.getStatus().getStatusCode());
                            }
                            mGoogleApiClient.disconnect();
                        }
                    });
        }
    }

    private static void initializeApiClient(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {

                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                    }
                })
                .build();
    }

    private static byte[] toByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        closeQuietly(stream);
        return byteArray;
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException while closing closeable.", e);
        }
    }
}
