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
    private String epubUrl;

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
    public String getEpubUrl() { return epubUrl; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public void setLanguage(String language) { this.language = language; }
    public void setRatingAvg(double ratingAvg) { this.ratingAvg = ratingAvg; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public void setPrice(double price) { this.price = price; }
    public void setAudiobook(boolean audiobook) { isAudiobook = audiobook; }
    public void setEbook(boolean ebook) { isEbook = ebook; }
    public void setPremiumOnly(boolean premiumOnly) { isPremiumOnly = premiumOnly; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public void setPublishedYear(int publishedYear) { this.publishedYear = publishedYear; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public void setCategoryNameVi(String categoryNameVi) { this.categoryNameVi = categoryNameVi; }
    public void setCategoryNameEn(String categoryNameEn) { this.categoryNameEn = categoryNameEn; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public void setEpubUrl(String epubUrl) { this.epubUrl = epubUrl; }
}