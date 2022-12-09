package com.mich1eal.upscale.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mich1eal.upscale.R;
import com.mich1eal.upscale.BLEWrapper;
import com.mich1eal.upscale.data.DBHelper;
import com.mich1eal.upscale.data.Recipe;
import com.mich1eal.upscale.data.Step;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class Scale extends Activity {

    private static final String TAG = Scale.class.getSimpleName();
    private static BLEWrapper bWrap;
    private static DBHelper db;

    private static Button retryButton, nextButton, lastButton, cancelButton;
    private static TextView statusText, batteryText, weightText, stepCountText, stepTitleText, titleText;
    private static ListView recipeList;
    private static RelativeLayout recipePane;

    // state variables
    private static int currentStep = 0;
    private static ArrayList<Step> steps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scale);

        // Setup db and BLE connections
        db = new DBHelper(getApplicationContext());
        final ArrayList<Recipe> recipes = db.RECIPE.getAllRecipes();

        //Handler is static to prevent memory leaks. See:
        // http://stackoverflow.com/questions/11278875/handlers-and-memory-leaks-in-android
        bWrap = new BLEWrapper(this, new BHandler());

        bWrap.connect();

        statusText = (TextView) findViewById(R.id.control_status);
        weightText = (TextView) findViewById(R.id.weight_text);
        batteryText = (TextView) findViewById(R.id.battery_text);
        stepCountText = (TextView) findViewById(R.id.step_count);
        stepTitleText = (TextView) findViewById(R.id.step_title);
        titleText = (TextView) findViewById(R.id.text_title);

        retryButton = (Button) findViewById(R.id.control_retry);
        nextButton = (Button) findViewById(R.id.button_next);
        lastButton = (Button) findViewById(R.id.button_last);
        cancelButton = (Button) findViewById(R.id.button_cancel);

        recipeList = (ListView) findViewById(R.id.list_recipes);
        recipePane = (RelativeLayout) findViewById(R.id.pane_recipe);

        retryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                bWrap.connect();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                endRecipe();
            }
        });

        ListAdapter recipeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, recipes);

        recipeList.setAdapter(recipeAdapter);
        recipeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Recipe recipe = recipes.get((int)id);
                startRecipe(recipe);
            }
        });
    }

    private static void startRecipe(Recipe recipe){
        // change views to start recipe
        recipeList.setVisibility(View.GONE);
        recipePane.setVisibility(View.VISIBLE);
        titleText.setText(recipe.name);

        steps = db.getStepsForRecipe(recipe.getID());
        for (Step step : steps) {
            Log.d(TAG, step.type);
        }
    }

    private static void endRecipe(){
        // allow user to select another recipe
        recipeList.setVisibility(View.VISIBLE);
        recipePane.setVisibility(View.GONE);
        titleText.setText(R.string.start_text);
    }

    private static void updateWeight(double newWeight){
        weightText.setText("Scale weight reading: " + newWeight);
    }

    private static void changeStep(int newStep) {

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
                if (disable == BLEWrapper.ERROR_BLUETOOTH_DISABLED) msg = R.string.status_disabled;
                //Location permission not granted
                else if (disable == BLEWrapper.ERROR_LOCATION_DISABLED) msg = R.string.status_location_disabled;
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
