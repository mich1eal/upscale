package com.mich1eal.upscale.data;

import android.util.Log;

/**
 * Created by msmil on 12/17/2020.
 */

public abstract class DBResource {
    private long _id = -1L;
    private static final String TAG = DBResource.class.getSimpleName();

    public final long getID() {
        return _id;
    }

    public final void setID(long _id) {
        if (_id == -1L)  Log.e(TAG, "Failed to set the _id on a DBResource as it already has an _id");
        onSetID(this._id = _id);
    }

    protected void onSetID(long _id) {}

    public abstract String getListName();
}