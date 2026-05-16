package com.example.a23110035_23110060;

public class Category {
    private String name;
    private int iconResId;
    private int color;

    // Các field mới cho giao diện Discovery
    private String count;
    private int backgroundRes;
    private String badge;

    // Constructor cũ để không làm lỗi code hiện tại
    public Category(String name, int iconResId, int color) {
        this.name = name;
        this.iconResId = iconResId;
        this.color = color;
    }

    // Constructor mới cho giao diện Discovery
    public Category(String name, String count, int backgroundRes, String badge) {
        this.name = name;
        this.count = count;
        this.backgroundRes = backgroundRes;
        this.badge = badge;
    }

    // Getter cũ
    public String getName() { return name; }
    public int getIconResId() { return iconResId; }
    public int getColor() { return color; }

    // Getter mới
    public String getCount() { return count; }
    public int getBackgroundRes() { return backgroundRes; }
    public String getBadge() { return badge; }
}