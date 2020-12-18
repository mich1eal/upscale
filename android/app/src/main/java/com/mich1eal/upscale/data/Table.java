package com.mich1eal.upscale.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by msmil on 12/17/2020.
 */

public abstract class Table<T extends DBResource>
{
    protected DBHelper helper;
    protected String tableName;

    public static final String _ID = " _id ";
    public static final String NUM = " INTEGER ";
    public static final String TXT = " TEXT ";
    public static final String DLM = ", ";
    public static final String SLCT = " SELECT ";
    public static final String ALL = " * ";
    public static final String FROM = " FROM ";
    public static final String WHRE = " WHERE ";
    public static final String EQUALS = " = ";
    public static final String PARAM = " ? ";
    public static final String AND = " AND ";
    public static final String BETWEEN = " BETWEEN ";
    public static final String IN = " IN ";
    public static final String NO_NULL = " IS NOT NULL ";
    public static final String DESCENDING = " DESC ";
    public static final String SORT = " ORDER BY ";
    public final String FIND_BY_ID;

    private boolean isInitialized = false;

    public Table(DBHelper db, String name)
    {
        this.helper = db;
        tableName = name;

        FIND_BY_ID = SLCT + ALL + FROM + tableName + WHRE + _ID + EQUALS;
    }

    protected final String getTableName()
    {
        return tableName;
    }

    protected abstract String getSQL();

    protected abstract ContentValues getContentValues(T object);

    public final void onCreate(Context context, SQLiteDatabase db)
    {
        Log.d(tableName, "Initializing...");

        final String tableSQL = getSQL();

        Log.d(tableName, tableSQL);

        db.execSQL(tableSQL);
        initializeValues(context, db);
        isInitialized = true;
    }

    protected void initializeValues(Context context, SQLiteDatabase db)
    {
        // override me
    }

    protected final long init_insert(SQLiteDatabase db, T object)
    {
        final long _id = db.insert(tableName, null, getContentValues(object));
        object.setID(_id);
        return _id;
    }

    protected final long insert(T object)
    {
        final long _id = helper.getDB().insert(tableName, null, getContentValues(object));
        object.setID(_id);
        return _id;
    }

    protected final long update(T object)
    {
        final String sql = _ID + EQUALS + PARAM;
        final String[] params = new String[]{String.valueOf(object.getID())};
        final long _id = helper.getDB().update(tableName, getContentValues(object), sql, params);
        return _id;
    }

    public final Cursor getAll()
    {
        return helper.getDB().rawQuery(SLCT + ALL + FROM + tableName, null);
    }

    public final void delete(long _id)
    {
        helper.getDB().delete(tableName, _ID + EQUALS + _id, null);
    }

    public final void delete(T object)
    {
        delete(object.getID());
    }

    protected final void clearAll()
    {
        helper.getDB().delete(tableName, "1", null);
    }

}