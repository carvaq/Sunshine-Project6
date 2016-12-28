package com.example.android.sunshine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

import static com.example.Constants.KEY_BITMAP;
import static com.example.Constants.KEY_HIGH_TEMP;
import static com.example.Constants.KEY_LOW_TEMP;
import static com.example.Constants.PATH_WEATHER;

public class WeatherListenerService extends WearableListenerService {

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                return;
            }
        }

        for (DataEvent event : dataEvents) {
            DataItem dataItem = event.getDataItem();
            Uri uri = dataItem.getUri();
            String path = uri.getPath();
            if (PATH_WEATHER.equals(path)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap config = dataMapItem.getDataMap();
                double lowTemp = config.getDouble(KEY_LOW_TEMP);
                double highTemp = config.getDouble(KEY_HIGH_TEMP);
                Asset asset = config.getAsset(KEY_BITMAP);
                byte[] assetData = asset.getData();
                Bitmap bitmap = BitmapFactory.decodeByteArray(assetData, 0, assetData.length);
            }
        }
    }

}
