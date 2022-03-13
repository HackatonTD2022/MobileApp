package com.example.mobileclient;

import static android.bluetooth.BluetoothAdapter.checkBluetoothAddress;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
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

    @Override
    public void handleResult(Result result) {
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
                    serverUUID = line.substring(line.lastIndexOf(' ') + 1);
                    break;
                case 1:
                    serverAddress = line.substring(line.lastIndexOf(' ') + 1);
                    break;
                case 2:
                    serverName = line.substring(line.lastIndexOf(' ') + 1);
                    break;
            }
            it++;
        }
        scanner.close();

        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(!bluetoothAdapter.isEnabled())
                bluetoothAdapter.enable();

            if(!checkBluetoothAddress(serverAddress)) {
                throw new RuntimeException("Invalid bluetooth address");
            }

            BluetoothDevice server = bluetoothAdapter.getRemoteDevice(serverAddress);

            /*try {
                BluetoothDevice server = bluetoothAdapter.getRemoteDevice(serverAddress);

                BluetoothSocket socket = server.createRfcommSocketToServiceRecord(UUID.fromString(serverUUID));
                socket.connect();

            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }*/

            Intent intent = new Intent(this, AuthActivity.class);
            intent.putExtra("Server", server);
            intent.putExtra("SocketUUID", serverUUID);
            startActivity(intent);

                        //ConnectThread connectThread = new ConnectThread(server, socket);
            //connectThread.start();


        } catch (SecurityException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }

    }

}
