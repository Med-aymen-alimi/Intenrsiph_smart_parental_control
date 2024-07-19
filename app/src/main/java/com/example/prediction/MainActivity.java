package com.example.prediction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {
    private Interpreter tflite;
    private TextView resultTextView;
    private BroadcastReceiver sensorDataReceiver;
    private SharedPreferences sharedPreferences;
    private boolean isVPNRunning = false; // Flag to track VPN service state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);
        Button startButton = findViewById(R.id.startButton);
        sharedPreferences = getSharedPreferences("com.example.prediction", MODE_PRIVATE);

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Model loading failed", Toast.LENGTH_SHORT).show();
            return;
        }

        sensorDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                float[] calculatedValues = intent.getFloatArrayExtra("calculatedValues");
                if (calculatedValues != null) {
                    float[] predictionResult = makePrediction(calculatedValues);
                    String result = (predictionResult[0] > 0.5) ? "Adult" : "Kid";
                    sharedPreferences.edit().putString("predictionResult", result).apply();
                    resultTextView.setText("Prediction Result: " + result);
                    handleVPNActivation(result);
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.example.prediction.SENSOR_VALUES");
        registerReceiver(sensorDataReceiver, filter);

        startButton.setOnClickListener(v -> {
            // Start sensor service
            Intent sensorServiceIntent = new Intent(MainActivity.this, SensorService.class);
            startService(sensorServiceIntent);
        });
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("modeltf_2.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private float[] makePrediction(float[] inputValues) {
        float[][] outputValues = new float[1][1];
        tflite.run(inputValues, outputValues);
        return outputValues[0];
    }

    private void handleVPNActivation(String result) {
        if ("Kid".equals(result)) {
            // Activate VPN service for Kids
            startVPNService();
            MyVPNService.setBlockedUrls(new String[]{"www.facebook.com", "www.youtube.com"});
            Toast.makeText(this, "VPN for parental control activated", Toast.LENGTH_SHORT).show();
        } else {
            // Deactivate VPN service for Adults
            stopVPNService();
        }
    }

    private void startVPNService() {
        if (!isVPNRunning) {
            Intent vpnIntent = new Intent(MainActivity.this, MyVPNService.class);
            startService(vpnIntent);
            isVPNRunning = true;
        }
    }

    private void stopVPNService() {
        if (isVPNRunning) {
            Intent vpnIntent = new Intent(MainActivity.this, MyVPNService.class);
            stopService(vpnIntent);
            isVPNRunning = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(sensorDataReceiver);
    }
}
