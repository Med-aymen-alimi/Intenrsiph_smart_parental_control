package com.example.prediction;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorHandler.SensorDataListener {

    private static final long RECORD_DURATION = 10000; // 10 seconds

    private SensorHandler sensorHandler;
    private Handler handler;

    private List<float[]> accelData = new ArrayList<>();
    private List<float[]> gyroData = new ArrayList<>();
    private List<float[]> gravityData = new ArrayList<>();
    private List<float[]> linearAccelData = new ArrayList<>();
    private List<float[]> magneticFieldData = new ArrayList<>();
    private List<float[]> rotationData = new ArrayList<>();

    private Button startButton;
    private Button stopButton;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setting up UI insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the sensor handler with MainActivity as the listener
        sensorHandler = new SensorHandler(this, this);

        // Initialize handler
        handler = new Handler(Looper.getMainLooper());

        // Set up the start button
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> startSensorRecording());

        // Set up the stop button
        stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(v -> stopSensorRecording());

        // Check permissions
        checkPermissions();
    }

    private void startSensorRecording() {
        if (isRecording) return;

        isRecording = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        // Clear previous data
        accelData.clear();
        gyroData.clear();
        gravityData.clear();
        linearAccelData.clear();
        magneticFieldData.clear();
        rotationData.clear();

        sensorHandler.registerSensors();
    }

    private void stopSensorRecording() {
        if (!isRecording) return;

        isRecording = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);

        sensorHandler.unregisterSensors();
        calculateStatistics();
    }

    private void calculateStatistics() {
        calculateAndDisplayStatistics(accelData, "Accelerometer");
        calculateAndDisplayStatistics(gyroData, "Gyroscope");
        calculateAndDisplayStatistics(gravityData, "Gravity");
        calculateAndDisplayStatistics(linearAccelData, "Linear Acceleration");
        calculateAndDisplayStatistics(magneticFieldData, "Magnetic Field");
        calculateAndDisplayStatistics(rotationData, "Rotation Vector");
    }

    private void calculateAndDisplayStatistics(List<float[]> data, String sensorName) {
        if (data.isEmpty()) return;

        float[] minValues = new float[3];
        float[] maxValues = new float[3];
        float[] sumValues = new float[3];
        float[] sqSumValues = new float[3];

        for (int i = 0; i < 3; i++) {
            minValues[i] = Float.MAX_VALUE;
            maxValues[i] = Float.MIN_VALUE;
        }

        for (float[] values : data) {
            for (int i = 0; i < 3; i++) {
                if (values[i] < minValues[i]) minValues[i] = values[i];
                if (values[i] > maxValues[i]) maxValues[i] = values[i];
                sumValues[i] += values[i];
                sqSumValues[i] += values[i] * values[i];
            }
        }

        int count = data.size();
        float[] meanValues = new float[3];
        float[] stdDevValues = new float[3];
        float[] rmseValues = new float[3];

        for (int i = 0; i < 3; i++) {
            meanValues[i] = sumValues[i] / count;
            stdDevValues[i] = (float) Math.sqrt((sqSumValues[i] / count) - (meanValues[i] * meanValues[i]));
            rmseValues[i] = (float) Math.sqrt(sqSumValues[i] / count);
        }

        // Save the calculated statistics to a text file
        saveStatisticsToFile(sensorName, minValues, maxValues, meanValues, stdDevValues, rmseValues);
        test();
    }
    private void test()
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();
        user.put("first", "Aymen");
        user.put("last", "Lovelace");
        user.put("born", 1815);

// Add a new document with a generated ID
        db.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }
    private void saveStatisticsToFile(String sensorName, float[] minValues, float[] maxValues, float[] meanValues, float[] stdDevValues, float[] rmseValues) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        File externalDir = getExternalFilesDir(null);
        System.out.println("------------------------creating folder----------------");
        if (externalDir != null) {
            String fileName = externalDir.getAbsolutePath() + "/" + sensorName + "_Statistics.txt";
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                System.out.println("------------------------sensor measurements saving----------------");
                String content = sensorName + " Statistics:\n";
                content += "Min: x: " + minValues[0] + " y: " + minValues[1] + " z: " + minValues[2] + "\n";
                content += "Max: x: " + maxValues[0] + " y: " + maxValues[1] + " z: " + maxValues[2] + "\n";
                content += "Mean: x: " + meanValues[0] + " y: " + meanValues[1] + " z: " + meanValues[2] + "\n";
                content += "Std Dev: x: " + stdDevValues[0] + " y: " + stdDevValues[1] + " z: " + stdDevValues[2] + "\n";
                content += "RMSE: x: " + rmseValues[0] + " y: " + rmseValues[1] + " z: " + rmseValues[2] + "\n";
                fos.write(content.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onSensorDataChanged(SensorEvent event) {
        float[] values = event.values.clone();
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accelData.add(values.clone());
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroData.add(values.clone());
                break;
            case Sensor.TYPE_GRAVITY:
                gravityData.add(values.clone());
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                linearAccelData.add(values.clone());
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticFieldData.add(values.clone());
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                rotationData.add(values.clone());
                break;
        }
    }
}
