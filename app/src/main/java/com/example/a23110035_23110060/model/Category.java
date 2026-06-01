package com.example.a23110035_23110060.model;

public class Category {
    private String id;
    private String nameVi;
    private String nameEn;
    private String iconUrl;
    private int color;

    // For legacy UI
    private int iconResId;
    private String count;
    private int backgroundRes;
    private String badge;

    public Category() {}

    // Minimal constructor for current adapters
    public Category(String nameVi, String iconUrl, int color) {
        this.nameVi = nameVi;
        this.iconUrl = iconUrl;
        this.color = color;
    }

    // Legacy constructor
    public Category(String nameVi, int iconResId, int color) {
        this.nameVi = nameVi;
        this.iconResId = iconResId;
        this.color = color;
    }

    // Full constructor for Database
    public Category(String id, String nameVi, String nameEn, String iconUrl) {
        this.id = id;
        this.nameVi = nameVi;
        this.nameEn = nameEn;
        this.iconUrl = iconUrl;
    }

    // Getters
    public String getId() { return id; }
    public String getNameVi() { return nameVi; }
    public String getName() { return nameVi; } // For legacy support
    public String getNameEn() { return nameEn; }
    public String getIconUrl() { return iconUrl; }
    public int getColor() { return color; }
    public int getIconResId() { return iconResId; }
    public String getCount() { return count; }
    public int getBackgroundRes() { return backgroundRes; }
    public String getBadge() { return badge; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setNameVi(String nameVi) { this.nameVi = nameVi; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
}