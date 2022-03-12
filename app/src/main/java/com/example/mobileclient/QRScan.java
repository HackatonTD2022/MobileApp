package com.example.mobileclient;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.zxing.Result;


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QRScan extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        // Programmatically initialize the scanner view
        mScannerView = new ZXingScannerView(this);
        // Set the scanner view as the content view
        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register ourselves as a handler for scan results.
        mScannerView.setResultHandler(this);
        // Start camera on resume
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop camera on pause
        mScannerView.stopCamera();
    }

    List<BluetoothDevice> btDevices = new ArrayList<>();

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btDevices.add(device);
            }
        }
    };

    @Override
    public void handleResult(Result result) {
        // Do something with the result here
        // Prints scan results

        //Toast.makeText(this, result.getText(), Toast.LENGTH_LONG).show();

        //If you would like to resume scanning, call this method below:
        //mScannerView.resumeCameraPreview(this);
        //Intent intent = new Intent();
        //intent.putExtra("TEST", result.getText());
        //setResult(RESULT_OK, intent);
        //finish();

        String serverName = null;
        String serverAddress = null;
        String serverUUID = null;
        int it = 0;

        Scanner scanner = new Scanner(result.getText());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            switch (it) {
                case 0:
                    serverUUID = line.substring(line.lastIndexOf(' '));
                    break;
                case 1:
                    serverAddress = line.substring(line.lastIndexOf(' '));
                    break;
                case 2:
                    serverName = line.substring(line.lastIndexOf(' '));
                    break;
            }
            it++;
        }
        scanner.close();

        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(!bluetoothAdapter.isEnabled())
                bluetoothAdapter.enable();
            bluetoothAdapter.startDiscovery();

            // Register for broadcasts when a device is discovered.
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);
            boolean flag = true;

            BluetoothDevice server = null;

            while (flag) {
                for(BluetoothDevice device : btDevices) {
                    if(device.getName().equals(serverName)) {
                        Toast.makeText(this, "Server found", Toast.LENGTH_LONG).show();
                        server = device;
                        flag = false;
                        break;
                    }
                }
                Thread.sleep(100);
            }

        } catch (SecurityException | InterruptedException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }

    }

}
