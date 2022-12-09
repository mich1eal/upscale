package com.mich1eal.upscale.data;

/**
 * Created by msmil on 12/8/2022.
 */

public class Step {
    public long id;
    public long recipeId;
    public int stepOrder;
    public int seconds;
    public double weight;
    public String type;
    public String ingredient;


    Step(long id, long recipeId, int stepOrder, int seconds, double weight, String type, String ingredient){
        this.id = id;
        this.recipeId = recipeId;
        this.stepOrder = stepOrder;
        this.seconds = seconds;
        this.weight = weight;
        this.type = type;
        this.ingredient = ingredient;
    }

}
