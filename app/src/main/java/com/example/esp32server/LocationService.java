package com.example.esp32server;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String esp32Ip;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        esp32Ip = intent.getStringExtra("ESP32_IP");
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Sending location to ESP32")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent);

        startForeground(1, notificationBuilder.build());
        sendStatusUpdate("Running");

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000); // Intervalo de 1 segundo
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        sendLocationToESP32(location.getLatitude(), location.getLongitude());
                        sendLocationToMainActivity(location.getLatitude(), location.getLongitude());
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        }
    }

    private void sendLocationToESP32(double latitude, double longitude) {
        new SendLocationTask().execute(latitude, longitude);
    }

    private void sendLocationToMainActivity(double latitude, double longitude) {
        Intent intent = new Intent("LocationServiceLocation");
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        sendBroadcast(intent);
    }

    private void sendStatusUpdate(String status) {
        Intent intent = new Intent("LocationServiceStatus");
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    private void sendErrorMessage(String error) {
        Intent intent = new Intent("LocationServiceError");
        intent.putExtra("error", error);
        sendBroadcast(intent);
    }

    private void clearErrorMessage() {
        Intent intent = new Intent("LocationServiceError");
        intent.putExtra("error", "");
        sendBroadcast(intent);
    }

    private class SendLocationTask extends AsyncTask<Double, Void, Void> {
        @Override
        protected Void doInBackground(Double... params) {
            double latitude = params[0];
            double longitude = params[1];

            try {
                URL url = new URL("http://" + esp32Ip + "/location");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                String jsonInputString = "{\"latitude\": " + latitude + ", \"longitude\": " + longitude + "}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                System.out.println("Response: "+ code);
                if (code != 200) {
                    sendErrorMessage("Failed to connect: " + code);
                } else {
                    clearErrorMessage(); // Clear error message if successful
                }

            } catch (Exception e) {
                sendErrorMessage(e.getMessage());
            }

            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        sendStatusUpdate("Stopped");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
