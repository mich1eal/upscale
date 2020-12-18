package com.mich1eal.upscale.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by msmil on 12/17/2020.
 */

public class DBHelper
        extends SQLiteOpenHelper{
    private static final String TAG = DBHelper.class.getSimpleName();

    public final RecipeTable RECIPE;

    public final Context context;

    public static final String DB_NAME = "UPSCALE_DB";

    public static final int DB_VERSION = 0;

    public DBHelper(Context context)
    {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;

        this.RECIPE = new RecipeTable(this);

        //forces db to update itself if necessary.
        getWritableDatabase().close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public SQLiteDatabase getDB() {
        return getWritableDatabase();
    }
}
