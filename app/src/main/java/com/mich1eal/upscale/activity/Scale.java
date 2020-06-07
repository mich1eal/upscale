package com.mich1eal.upscale.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mich1eal.upscale.R;
import com.mich1eal.upscale.BLEWrapper;

import org.w3c.dom.Text;

public class Scale extends Activity {

    private static final String TAG = Scale.class.getSimpleName();
    private static BLEWrapper bWrap;

    private static Button retryButton;
    private static TextView statusText, batteryText, weightText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scale);

        statusText = (TextView) findViewById(R.id.control_status);
        weightText = (TextView) findViewById(R.id.weight_text);
        batteryText = (TextView) findViewById(R.id.battery_text);

        retryButton = (Button) findViewById(R.id.control_retry);

        retryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                bWrap.connect();
            }
        });

        //Handler is static to prevent memory leaks. See:
        // http://stackoverflow.com/questions/11278875/handlers-and-memory-leaks-in-android
        bWrap = new BLEWrapper(this, new BHandler());

        bWrap.connect();
    }


    private static void updateWeight(double newWeight){
        weightText.setText("Scale weight reading: " + newWeight);
    }


    private static void updateBattery(double newBattery){
        batteryText.setText("Scale battery level: " + newBattery*100 + "%");
    }

    private static void handleBLEStateChange(int newState) {
        int msg = R.string.status_error;
        boolean connected = false;

        switch (newState) {
            case BLEWrapper.STATE_SEARCHING:
                msg = R.string.status_searching;
                retryButton.setEnabled(false);
                break;
            case BLEWrapper.STATE_CONNECTED:
                msg = R.string.status_connect;
                retryButton.setEnabled(false);
                connected = true;
                break;
            case BLEWrapper.STATE_DISCONNECTED:
                //There are several explanations for being disconnected
                final int disable = bWrap.getDisableType();
                //Location permission not granted
                if (disable == BLEWrapper.ERROR_LOCATION_DISABLED) msg = R.string.status_location_disabled;
                //Device doesnt have BLE
                else if (disable == BLEWrapper.ERROR_NOT_SUPPORTED) msg = R.string.status_not_available;
                    //Sepcial case where disconnection is due to search timeout
                else if (bWrap.getLastSearchTimeout()) msg = R.string.status_not_found;
                    //Default "not connected" message
                else msg = R.string.status_disconnect;

                retryButton.setEnabled(true);
                weightText.setText(R.string.weight_default);
                batteryText.setText(R.string.battery_default);

                break;
            case BLEWrapper.STATE_CONNECTING:
                msg = R.string.status_found;
                retryButton.setEnabled(false);
                break;
            default:
                msg = R.string.status_error;
                retryButton.setEnabled(true);
        }

        if (connected) {
        }
        else {
        }

        statusText.setText(msg);
    }

    @Override
    public void onPause() {
        super.onPause();
        bWrap.disconnect();
    }

    @Override
    public void onResume(){
        if (bWrap != null && bWrap.getState() == bWrap.STATE_DISCONNECTED) {
            bWrap.connect();
        }

        super.onResume();
    }

    static class BHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            boolean connected = false;
            switch (msg.what) {
                case BLEWrapper.MESSAGE_STATE:
                    handleBLEStateChange(msg.arg1);
                    break;

                case BLEWrapper.MESSAGE_VALUE:
                    if (msg.arg1 == BLEWrapper.VALUE_WEIGHT) {
                        updateWeight(((Double) msg.obj).doubleValue());

                    } else if (msg.arg1 == BLEWrapper.VALUE_BATTERY) {
                        updateBattery(((Double) msg.obj).doubleValue());

                    } else {
                        Log.e(TAG, "Error, unexpected value changed: " + msg.arg1);
                    }


                    break;
                default:
                    Log.e(TAG, "Error, unknown message received");
            }
        }
    }
}
