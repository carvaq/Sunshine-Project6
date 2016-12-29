/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Base64;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.example.android.R;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFaceService extends CanvasWatchFaceService {

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFaceService.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final String FORMAT_AMBIENT = "%02d:%02d";
        final String FORMAT_HOUR = "%02d:";
        final String FORMAT_MINUTE = "%02d";
        final String FORMAT_TEMPERATURE = "%d°";

        final Handler mUpdateTimeHandler = new EngineHandler(this);

        Paint mBitmapPaint;
        Paint mBackgroundPaint;
        Paint mTextPaintBold;
        Paint mTextPaintNormal;
        Paint mTextPaintSecondary;
        Paint mTextPaintAmbient;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        boolean mAmbient;
        boolean mRegisteredTimeZoneReceiver = false;

        Calendar mCalendar;
        SimpleDateFormat mSimpleDateFormat;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        float mXOffset;
        float mYOffset;

        float mTimeSize;
        float mDateSize;
        float mTemperatureSize;

        String mLowTemp;
        String mHighTemp;
        Bitmap mWeatherIcon;
        Rect mBitmapSrcRect = new Rect();
        Rect mBitmapDstRect = new Rect();

        PreferencesWrapper mPreferencesWrapper;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SunshineWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.background));

            mTextPaintBold = createTextPaint(R.color.textColorPrimary, Typeface.DEFAULT_BOLD);
            mTextPaintNormal = createTextPaint(R.color.textColorPrimary, Typeface.DEFAULT);
            mTextPaintSecondary = createTextPaint(R.color.textColorSecondary, Typeface.DEFAULT);
            mTextPaintAmbient = createTextPaint(R.color.textColorPrimary, Typeface.MONOSPACE);
            mTextPaintAmbient.setStyle(Paint.Style.STROKE);

            mCalendar = Calendar.getInstance();
            mSimpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());

            mBitmapPaint = new Paint();
            mPreferencesWrapper = new PreferencesWrapper(getApplicationContext());
            updateWeather();
        }

        private void updateWeather() {
            //http://stackoverflow.com/questions/19556433/saving-byte-array-using-sharedpreferences
            String icon = mPreferencesWrapper.getWeatherIcon();
            if (icon != null) {
                byte[] array = Base64.decode(icon, Base64.DEFAULT);
                mWeatherIcon = BitmapFactory.decodeByteArray(array, 0, array.length);
                mBitmapSrcRect.set(0, 0, mWeatherIcon.getWidth(), mWeatherIcon.getHeight());
            }
            mHighTemp = String.format(Locale.getDefault(), FORMAT_TEMPERATURE, mPreferencesWrapper.getHighTemperature());
            mLowTemp = String.format(Locale.getDefault(), FORMAT_TEMPERATURE, mPreferencesWrapper.getLowTemperature());
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(ContextCompat.getColor(getApplicationContext(), textColor));
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();

            mXOffset = resources.getDimension(isRound
                    ? R.dimen.x_offset_round : R.dimen.x_offset);
            mTimeSize = resources.getDimension(isRound
                    ? R.dimen.text_size_time_round : R.dimen.text_size_time);

            mDateSize = resources.getDimension(isRound
                    ? R.dimen.text_size_date_round : R.dimen.text_size_date);

            mTemperatureSize = resources.getDimension(isRound
                    ? R.dimen.text_size_temperature_round : R.dimen.text_size_temperature);

            mTextPaintAmbient.setTextSize(mTimeSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaintAmbient.setAntiAlias(!inAmbientMode);
                }
                updateWeather();
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            float centerX = canvas.getWidth() / 2;
            //    canvas.drawLine(centerX - 2, 0, centerX + 2, canvas.getHeight(), mTextPaintBold);

            if (mAmbient) {
                String text = String.format(Locale.getDefault(), FORMAT_AMBIENT, mCalendar.get(Calendar.HOUR_OF_DAY),
                        mCalendar.get(Calendar.MINUTE));
                float measuredText = mTextPaintAmbient.measureText(text);
                float yPos = (canvas.getHeight() - mTimeSize) / 2;
                canvas.drawText(text, centerX - measuredText / 2, yPos, mTextPaintAmbient);
            } else {
                //draw hour
                String hour = String.format(Locale.getDefault(), FORMAT_HOUR, mCalendar.get(Calendar.HOUR_OF_DAY));
                mTextPaintBold.setTextSize(mTimeSize);
                float measuredHourText = mTextPaintBold.measureText(hour);
                canvas.drawText(hour, centerX - measuredHourText, mYOffset, mTextPaintBold);

                //draw minute
                String minute = String.format(Locale.getDefault(), FORMAT_MINUTE, mCalendar.get(Calendar.MINUTE));
                mTextPaintNormal.setTextSize(mTimeSize);
                canvas.drawText(minute, centerX, mYOffset, mTextPaintNormal);

                //draw date
                String date = mSimpleDateFormat.format(mCalendar.getTime()).toUpperCase();
                mTextPaintSecondary.setTextSize(mDateSize);
                float measuredDateText = mTextPaintSecondary.measureText(date);
                canvas.drawText(date, centerX - measuredDateText / 2,
                        mYOffset + 3 * mTimeSize / 4, mTextPaintSecondary);

                //draw temperature
                float yPos = mYOffset + 2 * mTimeSize;

                mTextPaintBold.setTextSize(mTemperatureSize);
                float measuredHighTempText = mTextPaintBold.measureText(mHighTemp);
                canvas.drawText(mHighTemp, centerX - measuredHighTempText / 2, yPos, mTextPaintBold);

                float highTempSeparator = measuredHighTempText * 3 / 4;

                mTextPaintSecondary.setTextSize(mTemperatureSize);
                canvas.drawText(mLowTemp, centerX + highTempSeparator, yPos, mTextPaintSecondary);

                float newWidth = (mTimeSize / mWeatherIcon.getHeight()) * mWeatherIcon.getWidth();
                mBitmapDstRect.right = (int) (centerX - highTempSeparator);
                mBitmapDstRect.top = (int) (yPos - (2 * mTimeSize / 3));
                mBitmapDstRect.bottom = (int) (yPos + mTimeSize / 3);
                mBitmapDstRect.left = (int) (centerX - highTempSeparator - newWidth);
                canvas.drawBitmap(mWeatherIcon, mBitmapSrcRect, mBitmapDstRect, mBitmapPaint);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = UPDATE_RATE_MS
                        - (timeMs % UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
