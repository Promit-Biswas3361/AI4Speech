package com.example.ai4speech;

public class GameObject {
    private int id;
    private String name;
    private int imageRes;

    public GameObject(int id, String name, int imageRes) {
        this.id = id;
        this.name = name;
        this.imageRes = imageRes;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getImageRes() {
        return imageRes;
    }
}
