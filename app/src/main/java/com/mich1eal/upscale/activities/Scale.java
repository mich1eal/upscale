package com.mich1eal.upscale.activities;

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

public class Scale extends Activity {

    private static final String TAG = Scale.class.getSimpleName();
    private static BLEWrapper bWrap;

    private static Button retryButton;
    private static TextView statusText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scale);

        statusText = (TextView) findViewById(R.id.control_status);
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
        bWrap = new BLEWrapper(this, new BHandler(), false);
    }


    static class BHandler extends Handler
    {
        @Override
        public void handleMessage(Message inputMessage)
        {
            Log.d(TAG, "Message recieved: " + inputMessage.what);
            int msg = R.string.status_error;
            boolean connected = false;
            switch (inputMessage.what)
            {
                case BLEWrapper.STATE_SEARCHING:
                    msg = R.string.status_searching;
                    retryButton.setEnabled(false);
                    break;
                case BLEWrapper.STATE_CONNECTED:
                    msg = R.string.status_connect;
                    retryButton.setEnabled(false);
                    connected = true;
                    break;
                case BLEWrapper.STATE_NO_BLUETOOTH:
                    msg = R.string.status_no_bluetooth;
                    retryButton.setEnabled(true);
                    break;
                case BLEWrapper.STATE_DISCONNECTED:
                    msg = R.string.status_disconnect;
                    retryButton.setEnabled(true);
                    break;
                case BLEWrapper.STATE_FOUND:
                    msg = R.string.status_found;
                    retryButton.setEnabled(false);
                    break;
                default:
                    msg = R.string.status_error;
                    retryButton.setEnabled(true);
            }

            if (connected)
            {
            }
            else
            {
            }

            statusText.setText(msg);
        }
    }

    @Override
    public void onPause()
    {
        bWrap.close();
        super.onPause();
    }

    @Override
    public void onResume()
    {
        if (bWrap != null && bWrap.getState() == bWrap.STATE_DISCONNECTED)
        {
            bWrap.connect();
        }

        super.onResume();
    }
}
