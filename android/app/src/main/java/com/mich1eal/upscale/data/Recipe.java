package com.mich1eal.upscale.data;

/**
 * Created by msmil on 12/17/2020.
 */

public class Recipe extends DBResource{

    @Override
    public String getListName()
    {
        return this.toString();
    }


}
