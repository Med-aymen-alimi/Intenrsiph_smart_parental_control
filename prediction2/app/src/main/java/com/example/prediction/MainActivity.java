package com.example.prediction;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor linearAccelerationSensor;
    private Sensor rotationVectorSensor;
    private TextView linearAccelTextView;
    private TextView rotationTextView;
    private Button startButton;
    private boolean isMeasuring = false;
    private Handler handler = new Handler();

    private ArrayList<Float> ro_x_values = new ArrayList<>();
    private ArrayList<Float> ro_y_values = new ArrayList<>();
    private ArrayList<Float> ro_z_values = new ArrayList<>();
    private ArrayList<Float> ro_mag_values = new ArrayList<>();
    private ArrayList<Float> la_x_values = new ArrayList<>();
    private ArrayList<Float> la_y_values = new ArrayList<>();
    private ArrayList<Float> la_z_values = new ArrayList<>();
    private ArrayList<Float> la_mag_values = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Setting up UI insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize TextViews
        linearAccelTextView = findViewById(R.id.linearAccelTextView);
        rotationTextView = findViewById(R.id.rotationTextView);
        startButton = findViewById(R.id.startButton);

        // Initialize SensorManager and Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        // Set up start button click listener
        startButton.setOnClickListener(v -> {
            if (!isMeasuring) {
                startMeasuring();
            }
        });
    }

    private void startMeasuring() {
        isMeasuring = true;
        startButton.setEnabled(false);

        // Register listeners for linear acceleration and rotation vector
        sensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Clear previous data
        ro_x_values.clear();
        ro_y_values.clear();
        ro_z_values.clear();
        ro_mag_values.clear();
        la_x_values.clear();
        la_y_values.clear();
        la_z_values.clear();
        la_mag_values.clear();

        // Stop measuring after 25 seconds
        handler.postDelayed(this::stopMeasuring, 30000);
    }

    private void stopMeasuring() {
        isMeasuring = false;
        startButton.setEnabled(true);

        // Unregister listeners
        sensorManager.unregisterListener(this);

        // Calculate and display statistics
        calculateAndDisplayStatistics();
    }

    private void calculateAndDisplayStatistics() {
        float ro_y_max = Collections.max(ro_y_values);
        float ro_y_min = Collections.min(ro_y_values);
        float ro_y_mean = calculateMean(ro_y_values);

        float la_z_mean = calculateMean(la_z_values);

        float ro_x_max = Collections.max(ro_x_values);
        float ro_x_min = Collections.min(ro_x_values);
        float ro_z_max = Collections.max(ro_z_values);

        float ro_x_mean = calculateMean(ro_x_values);
        float la_x_mean = calculateMean(la_x_values);
        float ro_z_min = Collections.min(ro_z_values);

        float ro_z_mean = calculateMean(ro_z_values);
        float ro_mag_mean = calculateMean(ro_mag_values);
        float ro_mag_min = Collections.min(ro_mag_values);

        float ro_mag_max = Collections.max(ro_mag_values);
        float la_mag_mean = calculateMean(la_mag_values);
        float la_y_mean = calculateMean(la_y_values);

        float la_x_min = Collections.min(la_x_values);
        float la_mag_min = Collections.min(la_mag_values);
        float ro_mag_std = calculateStandardDeviation(ro_mag_values, ro_mag_mean);
        float ro_mag_rmse = calculateRMSE(ro_mag_values);

        String result = String.format("Rotation Vector:\n" +
                        "ro_y_max: %.2f\nro_y_min: %.2f\nro_y_mean: %.2f\n" +
                        "la_z_mean: %.2f\n" +
                        "ro_x_max: %.2f\nro_x_min: %.2f\nro_z_max: %.2f\n" +
                        "ro_x_mean: %.2f\nla_x_mean: %.2f\nro_z_min: %.2f\n" +
                        "ro_z_mean: %.2f\nro_mag_mean: %.2f\nro_mag_min: %.2f\n" +
                        "ro_mag_max: %.2f\nla_mag_mean: %.2f\nla_y_mean: %.2f\n" +
                        "la_x_min: %.2f\nla_mag_min: %.2f\nro_mag_std: %.2f\n" +
                        "ro_mag_rmse: %.2f",
                ro_y_max, ro_y_min, ro_y_mean,
                la_z_mean,
                ro_x_max, ro_x_min, ro_z_max,
                ro_x_mean, la_x_mean, ro_z_min,
                ro_z_mean, ro_mag_mean, ro_mag_min,
                ro_mag_max, la_mag_mean, la_y_mean,
                la_x_min, la_mag_min, ro_mag_std, ro_mag_rmse);

        rotationTextView.setText(result);
    }

    private float calculateMean(ArrayList<Float> values) {
        float sum = 0;
        for (float value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private float calculateStandardDeviation(ArrayList<Float> values, float mean) {
        float sum = 0;
        for (float value : values) {
            sum += Math.pow(value - mean, 2);
        }
        return (float) Math.sqrt(sum / values.size());
    }

    private float calculateRMSE(ArrayList<Float> values) {
        float sum = 0;
        for (float value : values) {
            sum += value * value;
        }
        return (float) Math.sqrt(sum / values.size());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isMeasuring) return;

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float x_acceleration = event.values[0];
            float y_acceleration = event.values[1];
            float z_acceleration = event.values[2];
            float mag_acceleration = (float) Math.sqrt(x_acceleration * x_acceleration + y_acceleration * y_acceleration + z_acceleration * z_acceleration);

            la_x_values.add(x_acceleration);
            la_y_values.add(y_acceleration);
            la_z_values.add(z_acceleration);
            la_mag_values.add(mag_acceleration);

            linearAccelTextView.setText("Linear Acceleration\nx: " + x_acceleration + "\ny: " + y_acceleration + "\nz: " + z_acceleration + "\nmag: " + mag_acceleration);
        } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float x_rotation = event.values[0];
            float y_rotation = event.values[1];
            float z_rotation = event.values[2];
            float mag_rotation = (float) Math.sqrt(x_rotation * x_rotation + y_rotation * y_rotation + z_rotation * z_rotation);

            ro_x_values.add(x_rotation);
            ro_y_values.add(y_rotation);
            ro_z_values.add(z_rotation);
            ro_mag_values.add(mag_rotation);

            rotationTextView.setText("Rotation Vector\nx: " + x_rotation + "\ny: " + y_rotation + "\nz: " + z_rotation + "\nmag: " + mag_rotation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Do something if sensor accuracy changes
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isMeasuring) {
            stopMeasuring();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMeasuring) {
            startMeasuring();
        }
    }
}
