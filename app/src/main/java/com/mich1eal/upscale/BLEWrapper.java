package com.mich1eal.upscale;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Michael on 4/12/2020.
 */
public class BLEWrapper
{
    private static final String TAG = BLEWrapper.class.getSimpleName();


    //Constants
    private static final UUID UUID_SERVICE_SCALE = UUID.fromString("f52ce065-6405-4cc0-a9eb-60e04533fa48");
    private static final UUID UUID_CHAR_WEIGHT = UUID.fromString("bab027df-7e05-4358-9c2c-53596d940bef");
    private static final UUID UUID_CHAR_BATTERY = UUID.fromString("4c1a2005-a529-4574-9eed-96e7e520f9c5");

    //Scan times out after 5 seconds
    private static final int SCAN_PERIOD = 5000;

    public static final int STATE_DISCONNECTED = 3;
    public static final int STATE_SEARCHING = 4;
    public static final int STATE_CONNECTING = 5;
    public static final int STATE_CONNECTED = 6;

    //Bluetooth error type
    public static final int ERROR_NONE = 0;
    public static final int ERROR_BLUETOOTH_DISABLED = 1;
    public static final int ERROR_NOT_SUPPORTED = 2;
    public static final int ERROR_LOCATION_DISABLED = 3;

    public static final int MESSAGE_STATE = 1;
    public static final int MESSAGE_VALUE = 2;

    public static final int VALUE_WEIGHT = 1;
    public static final int VALUE_BATTERY = 2;


    //Exposed status fields
    private int disableType;
    //indicates whether the most recent searched timed out
    private boolean lastSearchTimeout = false;

    //used internally to determine if current search timed out
    private boolean currentSearchTimeout = false;
    private boolean timeoutActive = false;

    private int state;

    private Context context;
    private BluetoothAdapter bAdapter;
    private BluetoothLeScanner bScanner;
    private BluetoothGatt bGatt;
    private BluetoothDevice bDevice;
    private Handler handler;
    private List<ScanFilter> bFilterList;
    private ScanSettings bScanSettings;

    public BLEWrapper(Context context, Handler handler)
    {
        assert(context != null);

        this.context = context;
        this.handler = handler;
        final BluetoothManager bManager = (BluetoothManager) context.getSystemService(context.BLUETOOTH_SERVICE);
        bAdapter = bManager.getAdapter();


        //Set up scan filter
        bFilterList = new ArrayList<>();
        final ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID_SERVICE_SCALE))
                .build();
        bFilterList.add(filter);

        //Set up scan settings
        bScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        setState(STATE_SEARCHING);
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
    public int getDisableType() { return disableType;}
    public boolean getLastSearchTimeout() {return lastSearchTimeout;}

    private void setState(int newState)
    {

        Log.d(TAG, "State commanded to: " + newState);

        if (newState == state) {
            Log.d(TAG, "State is already set, no action.");
            return;
        }

        if (!hasBluetoothEnabled())
        {
            newState = STATE_DISCONNECTED;
        }

        switch(newState){

            case STATE_DISCONNECTED:
                stopScanning();
                if (bDevice != null) disconnectDevice();
                bDevice = null;
                //check to see if we are disconnecting due to a timeout
                lastSearchTimeout = currentSearchTimeout;
                //reset flag
                currentSearchTimeout = false;
                break;

            case STATE_SEARCHING:
                startScan();
                break;

            case STATE_CONNECTING:
                //If we are trying to connect, then scanning is complete
                stopScanning();

                if (bDevice == null) {
                    Log.e(TAG, "Error: bDevice is null");
                    setState(STATE_DISCONNECTED);
                    return;
                }
                else {
                    connectDevice(bDevice);
                }
                break;

            case STATE_CONNECTED:
                break;
        }

        this.state = newState;
        Log.d(TAG, "State set to: " + newState);
        sendStateMessage(newState);
    }

    private boolean hasBLECapability(Context context)
    {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private boolean hasBluetoothEnabled()
    {
        //Device does not supports BLE
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            disableType = ERROR_NOT_SUPPORTED;
            return false;
        }
        //Location permission not granted
        else if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            disableType = ERROR_LOCATION_DISABLED;
            return false;
        }
        //Bluetooth disabled
        else if (bAdapter == null || !bAdapter.isEnabled()) {
            disableType = ERROR_BLUETOOTH_DISABLED;
            return false;
        }
        else
        {
            disableType = ERROR_NONE;
            return true;
        }
    }

    //ScanCallback used in startScan and stopScan
    final ScanCallback bScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "Found a device: " + result.getRssi());

            // The scan has already been filtered, so any device that we find is gauranteed to have a service with our UUID
            bDevice = result.getDevice();
            setState(STATE_CONNECTING);
        }
    };

    private void startScan() {
        timeoutActive = true;
        Handler scanHandler = new Handler();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bScanner = bAdapter.getBluetoothLeScanner();
                bScanner.startScan(bFilterList, bScanSettings, bScanCallback);
            }
        });

        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (timeoutActive)
                {
                    currentSearchTimeout = true;
                    setState(STATE_DISCONNECTED);
                }
            }
        }, SCAN_PERIOD);
    }

    private void stopScanning() {
        timeoutActive = false;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (bScanner != null && hasBluetoothEnabled()) bScanner.stopScan(bScanCallback);
            }
        });
    }

    private final BluetoothGattCallback bGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction = null;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");
                bGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");
                setState(STATE_DISCONNECTED);

            } else {
                Log.e(TAG, "Error, unexpected status");
                setState(STATE_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "GattServices discovered");
                for (BluetoothGattService service : gatt.getServices()) {
                    if (service.getUuid().equals(UUID_SERVICE_SCALE)) {
                        Log.d(TAG, "Found Scale service");
                        setState(STATE_CONNECTED);
                        for (BluetoothGattCharacteristic charact : service.getCharacteristics()) {
                            if (charact.getUuid().equals(UUID_CHAR_WEIGHT)) {
                                Log.d(TAG, "Found Weight Characteristic");
                                gatt.readCharacteristic(charact);
                                gatt.setCharacteristicNotification(charact, true);
                            }
                            else if (charact.getUuid().equals(UUID_CHAR_BATTERY)){
                                Log.d(TAG, "Found Battery Characteristic");
                                gatt.readCharacteristic(charact);
                                gatt.setCharacteristicNotification(charact, true);
                            }
                        }
                    }
                }
            }
            else {
                Log.e(TAG, "Service discovery not successful. Status: " + status);
                setState(STATE_DISCONNECTED);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic Read");

                int charact = -1;
                if(characteristic.getUuid().equals(UUID_CHAR_WEIGHT)) charact = VALUE_WEIGHT;
                else if (characteristic.getUuid().equals(UUID_CHAR_WEIGHT)) charact = VALUE_BATTERY;
                else {
                Log.e(TAG, "Unexpected characteristic read: " + characteristic.getUuid());
                }
                Log.d(TAG, "Read characteristic: " + characteristic.getStringValue(0));


                sendValueMessage(charact, Double.valueOf(characteristic.getStringValue(0)));

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "Received characteristics changed event : " + characteristic.getUuid() + "is now set to: " + characteristic.getStringValue(0));
        }
    };

    private void connectDevice(BluetoothDevice device)
    {
        bGatt = device.connectGatt(context, true, bGattCallback);
    }

    private void disconnectDevice()
    {
        if (bGatt != null) bGatt.close();
        bGatt = null;
    }

    private void sendStateMessage(int state)
    {
        if (handler == null) {
            Log.e(TAG, "Attempted to send message with null handler");
            return;
        }
        //sends a message with "What" field set to MESSAGE_STATE and the current state as arg1
        final Message msg = Message.obtain(handler, MESSAGE_STATE, state, 0);
        handler.sendMessage(msg);
    }

    private void sendValueMessage(int charact, double value)
    {
        if (handler == null) {
            Log.e(TAG, "Attempted to send message with null handler");
            return;
        }
        // sends a message with "What" field set to MESSAGE_VALUE,  arg1 as the character sent, and a double object for char
        final Message msg = Message.obtain(handler, MESSAGE_VALUE, charact, 0, Double.valueOf(value));
        handler.sendMessage(msg);
    }


}
