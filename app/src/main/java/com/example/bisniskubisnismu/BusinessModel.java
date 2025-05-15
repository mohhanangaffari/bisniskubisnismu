package com.example.bisniskubisnismu;

public class BusinessModel {
    private String owner;
    private String title;
    private int imageResId;
    private int avatarResId;

    public BusinessModel(String owner, String title, int imageResId, int avatarResId) {
        this.owner = owner;
        this.title = title;
        this.imageResId = imageResId;
        this.avatarResId = avatarResId;
    }

    public String getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public int getImageResId() {
        return imageResId;
    }

    public int getAvatarResId() {
        return avatarResId;
    }
}
