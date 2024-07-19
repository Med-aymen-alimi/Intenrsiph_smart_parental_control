package com.example.prediction;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import android.content.Intent;

public class MyVPNService extends VpnService {
    private Thread mThread;
    private boolean isRunning = false;

    private static Set<String> blockedUrls = new HashSet<>();

    // Method to set blocked URLs in VPN service
    public static void setBlockedUrls(String[] urls) {
        blockedUrls.clear();
        for (String url : urls) {
            blockedUrls.add(url);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mThread = new Thread(() -> {
            try {
                // Configure the VPN
                Builder builder = new Builder();
                builder.addAddress("10.0.0.2", 24); // VPN interface IP address
                builder.addRoute("0.0.0.0", 0); // Route all traffic through the VPN
                builder.addDnsServer("8.8.8.8"); // Set DNS server

                // Establish the VPN interface
                ParcelFileDescriptor vpnInterface = builder.establish();
                if (vpnInterface == null) {
                    Log.e("MyVPNService", "Failed to establish VPN");
                    return;
                }

                FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
                FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());

                isRunning = true;
                ByteBuffer packet = ByteBuffer.allocate(32767); // Allocate buffer for packet reading

                while (isRunning) {
                    int length = in.read(packet.array());
                    if (length > 0) {
                        packet.limit(length);

                        // Check for blocked URLs
                        String payload = new String(packet.array(), 0, length, StandardCharsets.UTF_8);
                        Log.d("MyVPNService", "Payload: " + payload);

                        boolean blockPacket = false;
                        for (String url : blockedUrls) {
                            if (payload.contains(url)) {
                                blockPacket = true;
                                Log.d("MyVPNService", "Blocking URL: " + url);
                                break;
                            }
                        }

                        if (!blockPacket) {
                            out.write(packet.array(), 0, length);
                        } else {
                            Log.d("MyVPNService", "Packet blocked");
                        }
                    }
                    packet.clear();
                }
            } catch (IOException e) {
                Log.e("MyVPNService", "Error in VPN service", e);
            }
        });
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mThread != null) {
            isRunning = false;
            mThread.interrupt();
        }
    }
}
