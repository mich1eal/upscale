package com.mich1eal.upscale.data;

/**
 * Created by msmil on 12/17/2020.
 */

public class Recipe extends DBResource{

    public String name;
    public String description;
    public int servings;
    public int unitCount;
    public double weight;

    Recipe(long id, String name, String description, int servings, int unitCount, double weight){
        this(name, description, servings, unitCount, weight);
        setID(id);
    }

    Recipe(String name, String description, int servings, int unitCount, double weight){
        this.name = name;
        this.description = description;
        this.servings = servings;
        this.unitCount = unitCount;
        this.weight = weight;
    }

    @Override
    public String getListName()
    {
        return this.toString();
    }

}
