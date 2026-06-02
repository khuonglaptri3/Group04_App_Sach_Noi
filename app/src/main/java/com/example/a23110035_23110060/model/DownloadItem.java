package com.example.a23110035_23110060.model;

public class DownloadItem {
    private String bookId;
    private String title;
    private String fileType; // "audio" or "ebook"
    private String coverUrl;
    private long sizeBytes;

    public DownloadItem(String bookId, String title, String fileType, String coverUrl, long sizeBytes) {
        this.bookId = bookId;
        this.title = title;
        this.fileType = fileType;
        this.coverUrl = coverUrl;
        this.sizeBytes = sizeBytes;
    }

    public String getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getFileType() { return fileType; }
    public String getCoverUrl() { return coverUrl; }
    public long getSizeBytes() { return sizeBytes; }
}
