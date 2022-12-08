package com.mich1eal.upscale.data;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;

/**
 * Created by msmil on 12/17/2020.
 */

public class RecipeTable extends Table<Recipe>{
    private static final String TAG = RecipeTable.class.getSimpleName();

    //db fields
    public static final String FIELD_UNIT_ID = "unit_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESC = "description";
    public static final String FIELD_SERVINGS = "servings";
    public static final String FIELD_UNIT_COUNT = "unit_count";
    public static final String FIELD_WEIGHT = "weight";

    private static RecipeTable Inst;

    public RecipeTable(DBHelper db) {
        super(db, "recipes");
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

    public ArrayList<Recipe> getAllRecipes() {
        Cursor c = getAll();

        // iterate through cursor and add all to output
        ArrayList<Recipe> recipes = new ArrayList<Recipe>();
        while (c.moveToNext()) {
            final long _id = c.getLong(0);
            //final long unitId = c.getLong(c.getColumnIndex(FIELD_UNIT_ID));
            final String name = c.getString(c.getColumnIndex(FIELD_NAME));
            final String description = c.getString(c.getColumnIndex(FIELD_DESC));
            final int servings = c.getInt(c.getColumnIndex(FIELD_SERVINGS));
            final int unitCount = c.getInt(c.getColumnIndex(FIELD_UNIT_COUNT));
            final double weight = c.getDouble(c.getColumnIndex(FIELD_WEIGHT));

            recipes.add(new Recipe(_id, name, description, servings, unitCount, weight));
        }

        c.close();

        return recipes;
    }
}
