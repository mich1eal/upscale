package com.mich1eal.upscale.data;

import android.content.ContentValues;

/**
 * Created by msmil on 12/17/2020.
 */

public class RecipeTable extends Table<Recipe>{
    private static final String TAG = RecipeTable.class.getSimpleName();

    private static RecipeTable Inst;

    public RecipeTable(DBHelper db)
    {
        super(db, "RecipeTable");
        Inst = this;
    }

    @Override
    protected String getSQL() {
        return null;
    }

    @Override
    protected ContentValues getContentValues(Recipe object) {
        return null;
    }
}
