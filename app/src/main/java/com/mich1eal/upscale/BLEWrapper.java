package com.mich1eal.upscale;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Michael on 4/12/2020.
 */
public class BLEWrapper
{
    private static final String TAG = BLEWrapper.class.getSimpleName();


    private static final UUID uuid = UUID.fromString("f52ce065-6405-4cc0-a9eb-60e04533fa48");

    //Scan times out after 5 seconds
    private static final int SCAN_PERIOD = 5000;

    public static final int STATE_DISABLED = 2;
    public static final int STATE_DISCONNECTED = 3;
    public static final int STATE_SEARCHING = 4;
    public static final int STATE_CONNECTING = 5;
    public static final int STATE_CONNECTED = 6;

    public boolean hasBLE = false;
    public boolean searchTimeout = false;

    private int state;


    private Context context;
    private BluetoothAdapter bAdapter;
    private BluetoothLeScanner bScanner;
    private BluetoothGatt bGatt;
    private BluetoothDevice bDevice;
    private Handler handler;


    final ScanCallback bScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "Found a device: " + result.getDevice().getName());
            result.getRssi();
            setState(STATE_CONNECTING);

            //if we find a valid device, connect to it
            if (false)
            {
                bDevice = result.getDevice();
                setState(STATE_CONNECTING);
            }

        }
    };

    private final BluetoothGattCallback bGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction = null;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");
                setState(STATE_CONNECTED);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");
                setState(STATE_DISCONNECTED);

            } else {
                Log.e(TAG, "Error, unexpected status");
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.d(TAG, "GattServices discovered");

            for (BluetoothGattService service : gatt.getServices()) {
                Log.d(TAG, "Found service with UIUD: " + service.getUuid());
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic Read");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "Received characteristics changed event : " + characteristic.getUuid() + "is now set to: " + characteristic.getStringValue(0));
        }


    };

    public BLEWrapper(Context context, Handler handler)
    {
        assert(context != null);

        this.context = context;
        this.handler = handler;
        final BluetoothManager bManager = (BluetoothManager) context.getSystemService(context.BLUETOOTH_SERVICE);
        bAdapter = bManager.getAdapter();
        bScanner = bAdapter.getBluetoothLeScanner();
        if (!hasBluetooth()) setState(STATE_DISABLED);
    }

    public void connect()
    {
        setState(STATE_SEARCHING);
    }

    public void disconnect()
    {
        setState(STATE_DISCONNECTED);
    }

    public int getState()
    {
        return state;
    }

    private void setState(int newState)
    {
        if (newState == state) {
            Log.d(TAG, "Set state: " + state + ". State is already set, no action.");
            return;
        }

        this.state = newState;
        Log.d(TAG, "setState: " + newState);
        if (handler != null) {
            handler.sendEmptyMessage(newState);
        }

        switch(newState){
            case STATE_DISABLED:
                break;
            case STATE_DISCONNECTED:
                if (bScanner != null) stopScanning();
                if (bDevice != null) disconnectDevice();
                bDevice = null;
                break;
            case STATE_SEARCHING:
                if (!hasBluetooth()) {setState(STATE_DISABLED);}
                else startScan();
                break;
            case STATE_CONNECTING:
                //If we are trying to connect, then scanning is complete
                if (bScanner != null) stopScanning();

                if (bDevice == null) {
                    Log.e(TAG, "Error: bDevice is null");
                    setState(STATE_DISCONNECTED);
                }
                else {
                    connectDevice(bDevice);
                }
                break;
            case STATE_CONNECTED:
                break;
        }
    }

    private boolean hasBluetooth()
    {
        return (bAdapter != null && bAdapter.isEnabled());
    }

    private void startScan() {
        Log.d(TAG, "Scan is starting");
        Handler scanHandler = new Handler();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bScanner.startScan(bScanCallback);
            }
        });

        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setState(STATE_DISCONNECTED);
            }
        }, SCAN_PERIOD);
    }

    private void stopScanning() {
        Log.d(TAG, "Scan is stopping");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bScanner.stopScan(bScanCallback);
            }
        });
    }

    private void connectDevice(BluetoothDevice device)
    {
        bGatt = device.connectGatt(context, true, bGattCallback);
        //bGatt.setCharacteristicNotification(bCharacteristic);
    }

    private void disconnectDevice()
    {
        if (bGatt != null) bGatt.close();
        bGatt = null;
    }


}
