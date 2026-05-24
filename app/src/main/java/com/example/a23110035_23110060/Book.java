package com.example.a23110035_23110060;

public class Book {
    private String id;
    private String title;
    private String description;
    private String coverUrl;
    private String bannerUrl;
    private String language;
    private double ratingAvg;
    private int ratingCount;
    private double price;
    private boolean isAudiobook;
    private boolean isEbook;
    private boolean isPremiumOnly;
    private int durationSeconds;
    private int pageCount;
    private int publishedYear;
    private String authorId;
    private String authorName;
    private String categoryId;
    private String categoryNameVi;
    private String categoryNameEn;
    private String audioUrl;

    public Book() {}

    public Book(String id, String title, String authorName, String coverUrl, boolean isPremiumOnly) {
        this.id = id;
        this.title = title;
        this.authorName = authorName;
        this.coverUrl = coverUrl;
        this.isPremiumOnly = isPremiumOnly;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCoverUrl() { return coverUrl; }
    public String getBannerUrl() { return bannerUrl; }
    public String getLanguage() { return language; }
    public double getRatingAvg() { return ratingAvg; }
    public int getRatingCount() { return ratingCount; }
    public double getPrice() { return price; }
    public boolean isAudiobook() { return isAudiobook; }
    public boolean isEbook() { return isEbook; }
    public boolean isPremiumOnly() { return isPremiumOnly; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getPageCount() { return pageCount; }
    public int getPublishedYear() { return publishedYear; }
    public String getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getCategoryId() { return categoryId; }
    public String getCategoryNameVi() { return categoryNameVi; }
    public String getCategoryNameEn() { return categoryNameEn; }
    public String getAudioUrl() { return audioUrl; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setPremiumOnly(boolean premiumOnly) { isPremiumOnly = premiumOnly; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
}