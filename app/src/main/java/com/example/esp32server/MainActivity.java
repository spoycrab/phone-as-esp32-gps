package com.example.esp32server;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String DEFAULT_IP = "192.168.155.221";
    private FusedLocationProviderClient fusedLocationClient;
    private EditText editTextIp;
    private TextView textViewStatus;
    private TextView textViewError;
    private TextView textViewLocation;

    private BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            textViewStatus.setText("Status: " + status);
        }
    };

    private BroadcastReceiver errorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String error = intent.getStringExtra("error");
            if (error != null && !error.isEmpty()) {
                textViewError.setText("Error: " + error);
            } else {
                textViewError.setText("Error: None");
            }
        }
    };

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = intent.getDoubleExtra("latitude", 0.0);
            double longitude = intent.getDoubleExtra("longitude", 0.0);
            String locationText = "Latitude: " + latitude + ", Longitude: " + longitude;
            textViewLocation.setText(locationText);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextIp = findViewById(R.id.editTextIp);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewError = findViewById(R.id.textViewError);
        textViewLocation = findViewById(R.id.textViewLocation);
        Button buttonStartService = findViewById(R.id.buttonStartService);
        Button buttonStopService = findViewById(R.id.buttonStopService);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        buttonStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermission();
            }
        });

        buttonStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationService();
            }
        });

        registerReceiver(statusReceiver, new IntentFilter("LocationServiceStatus"));
        registerReceiver(errorReceiver, new IntentFilter("LocationServiceError"));
        registerReceiver(locationReceiver, new IntentFilter("LocationServiceLocation"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(statusReceiver);
        unregisterReceiver(errorReceiver);
        unregisterReceiver(locationReceiver);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // Solicitar a permissão
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.FOREGROUND_SERVICE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permissão já concedida, iniciar o serviço
            startLocationService();
        }
    }

    private void startLocationService() {
        String ip = editTextIp.getText().toString().trim();
        if (ip.isEmpty()) {
            ip = DEFAULT_IP;
        }

        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.putExtra("ESP32_IP", ip);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
        textViewStatus.setText("Status: Stopped");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Chama a implementação da superclasse
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
