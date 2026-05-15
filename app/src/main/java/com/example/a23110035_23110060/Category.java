package com.example.a23110035_23110060;

public class Category {
    private String name;
    private int iconResId;
    private int color;

    public Category(String name, int iconResId, int color) {
        this.name = name;
        this.iconResId = iconResId;
        this.color = color;
    }

    public String getName() { return name; }
    public int getIconResId() { return iconResId; }
    public int getColor() { return color; }
}