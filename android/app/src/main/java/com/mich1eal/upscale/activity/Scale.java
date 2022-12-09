package com.mich1eal.upscale.activity;

import android.app.Activity;
import android.app.IntentService;
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
import android.widget.SeekBar;
import android.widget.TextView;

import com.mich1eal.upscale.R;
import com.mich1eal.upscale.BLEWrapper;
import com.mich1eal.upscale.data.DBHelper;
import com.mich1eal.upscale.data.Recipe;
import com.mich1eal.upscale.data.Step;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Locale;

public class Scale extends Activity {

    private static final String TAG = Scale.class.getSimpleName();
    private static BLEWrapper bWrap;
    private static DBHelper db;

    private static Button retryButton, nextButton, lastButton, cancelButton, tareButton, timerStartButton, timerResetButton;
    private static TextView statusText, batteryText, weightText, stepCountText, stepTitleText, stepSubtitleText, titleText, timerText, scaleText;
    private static ListView recipeList;
    private static RelativeLayout recipePane, weightPane, timerPane;

    private static SeekBar weightSeek;

    // state variables
    private static long currentStep = 0;
    private static ArrayList<Step> steps;

    // stopwatch constants
    private static boolean timerRunning = false;
    private static int fullTimerSeconds = 0;
    private static int timerSeconds = 0;

    // scale constants
    private static double currentWeight = 0;
    private static double goalWeight = 1;
    private static double tare = 0;

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
        stepSubtitleText = (TextView) findViewById(R.id.step_subtitle);
        titleText = (TextView) findViewById(R.id.text_title);
        timerText = (TextView) findViewById(R.id.timer);
        scaleText = (TextView) findViewById(R.id.weight_weight);

        retryButton = (Button) findViewById(R.id.control_retry);
        nextButton = (Button) findViewById(R.id.button_next);
        lastButton = (Button) findViewById(R.id.button_last);
        cancelButton = (Button) findViewById(R.id.button_cancel);
        tareButton = (Button) findViewById(R.id.weight_tare);
        timerStartButton = (Button) findViewById(R.id.timer_start_toggle);
        timerResetButton = (Button) findViewById(R.id.timer_reset);

        recipeList = (ListView) findViewById(R.id.list_recipes);

        recipePane = (RelativeLayout) findViewById(R.id.pane_recipe);
        timerPane = (RelativeLayout) findViewById(R.id.timer_view);
        weightPane = (RelativeLayout) findViewById(R.id.scale_view);

        weightSeek = (SeekBar) findViewById(R.id.weight_seek);
        weightSeek.setSecondaryProgress(75);

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
        nextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                changeStep(currentStep + 1);
            }
        });
        lastButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                changeStep(currentStep - 1);
            }
        });

        timerStartButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                toggleTimer(!timerRunning);
            }
        });

        timerResetButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                toggleTimer(false);
                resetTimer();
            }
        });

        tareButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                tare = currentWeight;
                updateWeight(currentWeight);
            }
        });

        lastButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                changeStep(currentStep - 1);
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
        changeStep(0);
    }

    private static void endRecipe(){
        // allow user to select another recipe
        recipeList.setVisibility(View.VISIBLE);
        recipePane.setVisibility(View.GONE);
        titleText.setText(R.string.start_text);

        currentStep = 0;
        steps = null;
    }

    private static void updateWeight(double newWeight){
        weightText.setText("Scale weight reading: " + newWeight);

        // if weight pane is visible, update it too
        if (weightPane.getVisibility() != View.GONE) {
            currentWeight = newWeight;

            double compensatedWeight = currentWeight - tare;

            scaleText.setText("" + (int) compensatedWeight);
            weightSeek.setProgress((int) (75 * compensatedWeight / goalWeight));
        }
    }

    private static String getTimerString(int seconds){
        int mins = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }

    private static void resetTimer() {
        timerSeconds = fullTimerSeconds;
        timerText.setText(getTimerString(timerSeconds));
    }

    private static void toggleTimer(boolean start){
        timerRunning = start;

        timerStartButton.setText(timerRunning ? R.string.timer_start_stop : R.string.timer_start_start);

        if (timerRunning) {
            final Handler handle = new Handler();
            handle.post(new Runnable() {
                @Override
                public void run() {
                    if (timerRunning && timerSeconds > 0) {
                        timerText.setText(getTimerString(timerSeconds));
                        timerSeconds--;
                        handle.postDelayed(this, 1000);
                    }
                }
            });
        }
    }

    private static void changeStep(long newStep) {
        Log.d(TAG, "STEP CHANGE. WAS " + (int) currentStep + " IS " + (int) newStep);
        currentStep = newStep;

        //stop timer
        toggleTimer(false);

        // ####### handle step number #######
        if (newStep >= steps.size()){
            // if greater than number of steps we're done
            endRecipe();
            return;
        }
        else if (newStep == steps.size() - 1){
            nextButton.setText(R.string.button_next_final);
        }
        else {
            nextButton.setText(R.string.button_next);
        }

        // disable last button if we're at step 1
        if (newStep == 0) {
            lastButton.setEnabled(false);
        }
        else {
            lastButton.setEnabled(true);
        }

        Step step = steps.get((int) newStep);

        String stepTitle = "";

        switch (step.type) {
            case "add":
                stepTitle = "Add " + (int) step.weight + " grams of " + step.ingredient+ ".";

                break;
            case "boil":
                stepTitle = "Bring to a boil.";

                break;
            case "let boil":
                String timeString;
                if (step.seconds % 60 == 0) {
                    Log.e(TAG, "" + step.seconds);
                    timeString = "" + (int) (step.seconds / 60.0) + " minutes";
                }
                else {
                    timeString = "" + (int) (step.seconds / 60.0) + " minutes and " + step.seconds % 60 + " seconds";
                }
                stepTitle = "Simmer for " + timeString + ".";

                break;
            case "let rest":
                String timeString2;
                if (step.seconds % 60 == 0) {
                    timeString2 = "" + (int) (step.seconds / 60.0) + " minutes";
                }
                else {
                    timeString2 = "" + (int) (step.seconds / 60.0) + " minutes and " + step.seconds % 60 + " seconds";
                }
                stepTitle = "Let rest for for " + timeString2 + ".";
                break;

            case "fluff":
                stepTitle = "Fluff with a fork.";
                break;
            case "serve":
                stepTitle = "Serve and enjoy!";
                break;
        }
        // Update text fields
        stepCountText.setText("Step " + (newStep + 1) + " of " + steps.size());
        stepTitleText.setText("Step " + (newStep + 1) + ":");
        stepSubtitleText.setText(stepTitle);

        // Show weight/timer views
        if (step.weight != 0) {
            goalWeight = step.weight;
            timerPane.setVisibility(View.GONE);
            weightPane.setVisibility(View.VISIBLE);
        }
        else if (step.seconds != 0) {
            fullTimerSeconds = step.seconds;
            resetTimer();
            timerPane.setVisibility(View.VISIBLE);
            weightPane.setVisibility(View.GONE);
        }
        else {
            timerPane.setVisibility(View.GONE);
            weightPane.setVisibility(View.GONE);
        }

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
