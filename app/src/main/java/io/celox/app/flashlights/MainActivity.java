/*
 * Copyright (c) 2017 Martin Pfeffer
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

package io.celox.app.flashlights;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.pepperonas.aespreferences.AesPrefs;

import java.util.List;
import java.util.UUID;

import io.celox.app.flashlights.utils.AesConst;
import io.celox.app.flashlights.utils.Utils;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int PERMISSION_INITIAL_REQUEST = 0;
    private static final int REQUEST_ENABLE_BT = 1;

    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_COUNTER_UUID = UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private android.bluetooth.BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG, "onConnectionStateChange: ");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT client. Attempting to start service discovery.");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT client.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.i(TAG, "---onServicesDiscovered---");

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onServicesDiscovered: discovering services failed.");
                return;
            }

            for (BluetoothGattService gattService : gatt.getServices()) {
                Log.i(TAG, "" + gatt.getDevice().getName() + "=" + gattService.getUuid().toString());

                if (gattService.getUuid().toString().equals("0000ffe5-0000-1000-8000-00805f9b34fb")) {
                    BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
                    scanner.stopScan(mScanCallback);

                    Log.i(TAG, "LED found...");
                    mCharacteristic = gattService.getCharacteristic(UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb"));

                    String command = "FFFFFF";
                    String originalString = "56" + command + "00F0AA";
                    byte[] b = Utils.hexStringToByteArray(originalString);
                    mCharacteristic.setValue(b);
                    mBluetoothGatt.writeCharacteristic(mCharacteristic);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicRead: ");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicWrite: " + Utils.byteToHexStr(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, "onCharacteristicChanged: ");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.i(TAG, "onReliableWriteCompleted: ");
        }
    };

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // scanned with report delay > 0. This will never be called.
            Log.i(TAG, "onScanResult: " + result.toString());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (!results.isEmpty()) {
                ScanResult result = results.get(0);
                BluetoothDevice device = result.getDevice();
                // TODO: 09.11.17 22:38 list devices here
                String deviceAddress = device.getAddress();
                device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                String deviceName = device.getName();
                // device detected, we can automatically connect to it and stop the scan
                Log.i(TAG, "onBatchScanResults: " + deviceName + "=" + deviceAddress);
                mBluetoothGatt = device.connectGatt(MainActivity.this, false, mGattCallback);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "onScanFailed: " + errorCode);
        }
    };
    private BluetoothGattCharacteristic mCharacteristic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ensureRuntimePermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mBluetoothGatt.close();
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: ", e);
        }
    }

    private void startApp() {
        Log.i(TAG, "Starting app...");

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();

        // receive a list of found devices every second
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setReportDelay(1000)
                .build();

        //        ScanFilter scanFilter = new ScanFilter.Builder()
        //                .setServiceUuid(new ParcelUuid(SERVICE_UUID)).build();
        //        scanner.startScan(Collections.singletonList(scanFilter), settings, mScanCallback);
        scanner.startScan(null, settings, mScanCallback);
    }

    private void ensureRuntimePermissions() {
        AesPrefs.initBoolean(AesConst.PERMISSIONS_GRANTED, false);
        if (!AesPrefs.getBoolean(AesConst.PERMISSIONS_GRANTED, false)) {
            String[] permissions = new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
            };
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_INITIAL_REQUEST);
        } else {
            startApp();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // user doesn't want bluetooth enabled
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_INITIAL_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    AesPrefs.putBoolean(AesConst.PERMISSIONS_GRANTED, true);
                } else {
                    AesPrefs.putBoolean(AesConst.PERMISSIONS_GRANTED, false);
                    Toast.makeText(MainActivity.this, "Permission denied...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send:
                try {
                    String command = ((TextView) findViewById(R.id.tv_command)).getText().toString();
                    String originalString = "56" + command + "00F0AA";
                    byte[] b = Utils.hexStringToByteArray(originalString);
                    mCharacteristic.setValue(b);
                    mBluetoothGatt.writeCharacteristic(mCharacteristic);
                } catch (Exception e) {
                    Log.e(TAG, "onClick: ", e);
                }
                break;
        }
    }
}
