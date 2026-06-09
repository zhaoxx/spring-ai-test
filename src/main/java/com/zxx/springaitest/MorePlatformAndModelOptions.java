package com.zxx.springaitest;

public class MorePlatformAndModelOptions {

    private String plaform;
    private String model;
    private Double temperature;

    public MorePlatformAndModelOptions(String plaform, String model, Double temperature) {
        this.plaform = plaform;
        this.model = model;
        this.temperature = temperature;
    }

    public String getPlaform() {
        return plaform;
    }

    public void setPlaform(String plaform) {
        this.plaform = plaform;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
}
