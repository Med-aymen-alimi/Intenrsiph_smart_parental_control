package com.example.prediction;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor rotationSensor, linearAccelerationSensor;
    private final int MEASUREMENT_DURATION = 15000; // 15 seconds
    private long startTime;
    private List<Float> roYValues, laZValues, roXValues, roZValues, laXValues, roMagValues, laMagValues, laYValues;
    private static final String CHANNEL_ID = "SensorServiceChannel";

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        roYValues = new ArrayList<>();
        laZValues = new ArrayList<>();
        roXValues = new ArrayList<>();
        roZValues = new ArrayList<>();
        laXValues = new ArrayList<>();
        roMagValues = new ArrayList<>();
        laMagValues = new ArrayList<>();
        laYValues = new ArrayList<>();

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sensor Service")
                .setContentText("Collecting sensor data...")

                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        startTime = System.currentTimeMillis();

        new Handler().postDelayed(this::calculateAndSendResult, MEASUREMENT_DURATION);

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Add sensor values to respective lists
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            roYValues.add(event.values[1]);
            roXValues.add(event.values[0]);
            roZValues.add(event.values[2]);
            roMagValues.add(calculateMagnitude(event.values));
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            laZValues.add(event.values[2]);
            laXValues.add(event.values[0]);
            laYValues.add(event.values[1]);
            laMagValues.add(calculateMagnitude(event.values));
        }
    }

    private void calculateAndSendResult() {
        float roYMax = Collections.max(roYValues);
        float roYMin = Collections.min(roYValues);
        float roYMean = calculateMean(roYValues);
        float laZMean = calculateMean(laZValues);
        float roXMax = Collections.max(roXValues);
        float roXMin = Collections.min(roXValues);
        float roZMax = Collections.max(roZValues);
        float roXMean = calculateMean(roXValues);
        float laXMean = calculateMean(laXValues);
        float roZMin = Collections.min(roZValues);
        float roZMean = calculateMean(roZValues);
        float roMagMean = calculateMean(roMagValues);
        float roMagMin = Collections.min(roMagValues);
        float roMagMax = Collections.max(roMagValues);
        float laMagMean = calculateMean(laMagValues);
        float laYMean = calculateMean(laYValues);
        float laXMin = Collections.min(laXValues);
        float laMagMin = Collections.min(laMagValues);
        float roMagStd = calculateStandardDeviation(roMagValues);
        float roMagRmse = calculateRMSE(roMagValues);

        float[] calculatedValues = {
                roYMax, roYMin, roYMean, laZMean, roXMax, roXMin, roZMax, roXMean, laXMean, roZMin,
                roZMean, roMagMean, roMagMin, roMagMax, laMagMean, laYMean, laXMin, laMagMin,
                roMagStd, roMagRmse
        };

        // Broadcast calculated values
        Intent resultIntent = new Intent("com.example.prediction.SENSOR_VALUES");
        resultIntent.putExtra("calculatedValues", calculatedValues);
        sendBroadcast(resultIntent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private float calculateMean(List<Float> values) {
        float sum = 0;
        for (float value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private float calculateStandardDeviation(List<Float> values) {
        float mean = calculateMean(values);
        float sum = 0;
        for (float value : values) {
            sum += Math.pow(value - mean, 2);
        }
        return (float) Math.sqrt(sum / values.size());
    }

    private float calculateRMSE(List<Float> values) {
        float sum = 0;
        for (float value : values) {
            sum += Math.pow(value, 2);
        }
        return (float) Math.sqrt(sum / values.size());
    }

    private float calculateMagnitude(float[] values) {
        return (float) Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
