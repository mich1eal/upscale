package com.mich1eal.upscale.data;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by msmil on 12/17/2020.
 */

public class DBHelper
        extends SQLiteOpenHelper{
    private static final String TAG = DBHelper.class.getSimpleName();

    public static final String DB_NAME = "upscale.db";
    public static String DB_PATH = "/data/data/com.mich1eal.upscale/databases/";
    public static String FULL_PATH = DB_PATH + DB_NAME;

    public static final int DB_VERSION = 1;

    public final Context context;

    public final RecipeTable RECIPE;

    public DBHelper(Context context)
    {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;

        this.RECIPE = new RecipeTable(this);

        // if the db doesn't exist in filesystem, copy it from assets
        if (!dbExists()){
            Log.d(TAG, "Database does not exist, copying");
            this.getReadableDatabase();
            try {
                copyDB();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        else {
            Log.d(TAG, "Database already exists");
        }
        //forces db to update itself if necessary.
        getWritableDatabase().close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //RECIPE.onCreate(context, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public SQLiteDatabase getDB() {
        return getWritableDatabase();
    }

    private boolean dbExists() {
        // Tries to open DB from file location, returns true if successful

        SQLiteDatabase tempDB = null;
        try {
            tempDB = SQLiteDatabase.openDatabase(FULL_PATH, null, SQLiteDatabase.OPEN_READWRITE);

        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
        }
        if (tempDB != null)
            tempDB.close();
        return tempDB != null;
    }

    public void copyDB() throws IOException {
        //copy database from assets folder to file location
        try {
            InputStream myInput = context.getAssets().open(DB_NAME);
            OutputStream myOutput = new FileOutputStream(FULL_PATH);

            byte[] buffer = new byte[1024];
            int length;

            while((length = myInput.read(buffer)) > 0){
                myOutput.write(buffer, 0, length);
            }

            myOutput.flush();
            myOutput.close();
            myInput.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public ArrayList<Step> getStepsForRecipe(long recipe) {

        String query = "SELECT * FROM recipe_steps WHERE recipe_id = " + recipe + " ORDER BY step_order;";
        Cursor c = getDB().rawQuery(query, null);

        // iterate through cursor and add all to output
        ArrayList<Step> steps = new ArrayList<Step>();
        while (c.moveToNext()) {
            final long id = c.getLong(0);
            final int stepOrder = c.getInt(c.getColumnIndex("step_order"));
            final int seconds = c.getInt(c.getColumnIndex("seconds"));
            final double weight = c.getDouble(c.getColumnIndex("weight"));
            final String type = c.getString(c.getColumnIndex("type"));
            final String ingredient = c.getString(c.getColumnIndex("ingredient"));

            steps.add(new Step(id, recipe, stepOrder, seconds, weight, type, ingredient));
        }

        c.close();
        return steps;
    }

}
