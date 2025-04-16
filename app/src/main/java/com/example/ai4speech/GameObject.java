package com.example.ai4speech;

public class GameObject {
    private int id;
    private String name;
    private int imageRes;
    private int audioRes; // New field for audio pronunciation resource

    public GameObject(int id, String name, int imageRes, int audioRes) {
        this.id = id;
        this.name = name;
        this.imageRes = imageRes;
        this.audioRes = audioRes;
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

    public int getAudioRes() {
        return audioRes;
    }
}