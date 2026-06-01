package com.example.a23110035_23110060;

import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public class PlayerManager {
    private static PlayerManager instance;

    private void notifyService() {
        if (MyApplication.getInstance() != null) {
            Intent intent = new Intent(MyApplication.getInstance(), AudioPlayerService.class);
            intent.setAction(AudioPlayerService.ACTION_UPDATE_NOTIFICATION);
            ContextCompat.startForegroundService(MyApplication.getInstance(), intent);
        }
    }
    private MediaPlayer mediaPlayer;
    private Book currentBook;
    private boolean isPlaying = false;
    private float currentSpeed = 1.0f;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private List<Chapter> chapters = new ArrayList<>();
    private int currentChapterIndex = -1;
    private boolean isPrepared = false;
    
    private Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepTimerRunnable;
    public interface PlayerCallback {
        void onProgress(int currentMs, int totalMs);
        void onStateChange(boolean isPlaying);
    }
    
    private PlayerCallback callback;

    private PlayerManager() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> {
            isPlaying = false;
            if (callback != null) callback.onStateChange(false);
        });
    }

    public static synchronized PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    public void setCallback(PlayerCallback callback) {
        this.callback = callback;
    }

    public void playBook(Book book) {
        if (book == null || book.getAudioUrl() == null || book.getAudioUrl().isEmpty()) {
            return;
        }

        try {
            this.currentBook = book;
            isPrepared = false;
            isPlaying = false;
            if (callback != null) {
                callback.onProgress(0, 0);
                callback.onStateChange(false);
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(book.getAudioUrl());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                setPlaybackSpeed(currentSpeed);
                if (currentChapterIndex >= 0 && chapters != null && !chapters.isEmpty()) {
                    mediaPlayer.seekTo(chapters.get(currentChapterIndex).getStartTime());
                }
                mediaPlayer.start();
                isPlaying = true;
                if (callback != null) callback.onStateChange(true);
                startProgressUpdate();
                notifyService();
            });
        } catch (IOException e) {
            Log.e("PlayerManager", "Error", e);
        }
    }

    public void togglePlayPause() {
        if (currentBook == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
        } else {
            mediaPlayer.start();
            isPlaying = true;
            startProgressUpdate();
        }
        if (callback != null) callback.onStateChange(isPlaying);
        notifyService();
    }

    public void setPlaybackSpeed(float speed) {
        this.currentSpeed = speed;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
            } catch (Exception e) {
                Log.e("PlayerManager", "Speed error", e);
            }
        }
    }

    public void seekBack(int ms) {
        mediaPlayer.seekTo(Math.max(mediaPlayer.getCurrentPosition() - ms, 0));
    }

    public void seekForward(int ms) {
        mediaPlayer.seekTo(Math.min(mediaPlayer.getCurrentPosition() + ms, mediaPlayer.getDuration()));
    }

    public void seekTo(int progress) {
        mediaPlayer.seekTo(progress);
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
        this.currentChapterIndex = 0;
        if (chapters != null && !chapters.isEmpty()) {
            playChapter(currentChapterIndex);
        }
    }

    public List<Chapter> getChapters() { return chapters; }

    public void playChapter(int index) {
        if (chapters == null || index < 0 || index >= chapters.size()) return;
        Chapter chapter = chapters.get(index);
        this.currentChapterIndex = index;
        
        if (chapter.getAudioUrl() == null || chapter.getAudioUrl().isEmpty()) return;

        if (currentBook != null && chapter.getAudioUrl().equals(currentBook.getAudioUrl())) {
             if (isPrepared) {
                 mediaPlayer.seekTo(chapter.getStartTime());
                 if (callback != null) callback.onProgress(chapter.getStartTime(), mediaPlayer.getDuration());
                 if (!isPlaying) togglePlayPause();
             }
        } else {
             try {
                 isPrepared = false;
                 isPlaying = false;
                 if (callback != null) {
                     callback.onProgress(chapter.getStartTime(), 0);
                     callback.onStateChange(false);
                 }
                 mediaPlayer.reset();
                 mediaPlayer.setDataSource(chapter.getAudioUrl());
                 mediaPlayer.prepareAsync();
                 mediaPlayer.setOnPreparedListener(mp -> {
                     isPrepared = true;
                     setPlaybackSpeed(currentSpeed);
                     mediaPlayer.seekTo(chapter.getStartTime());
                     mediaPlayer.start();
                     isPlaying = true;
                     if (callback != null) callback.onStateChange(true);
                     startProgressUpdate();
                     notifyService();
                 });
             } catch (IOException e) {
                 Log.e("PlayerManager", "Error playing chapter", e);
             }
        }
    }

    public void nextChapter() {
        if (chapters != null && currentChapterIndex < chapters.size() - 1) {
            currentChapterIndex++;
            playChapter(currentChapterIndex);
        }
    }

    public void previousChapter() {
        if (chapters != null && currentChapterIndex > 0) {
            currentChapterIndex--;
            playChapter(currentChapterIndex);
        } else if (chapters != null && currentChapterIndex == 0) {
            seekTo(0);
        }
    }
    public void skipNext() { nextChapter(); }

    public void setSleepTimer(int minutes) {
        cancelSleepTimer();
        sleepTimerRunnable = () -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
                if (callback != null) callback.onStateChange(false);
            }
        };
        sleepTimerHandler.postDelayed(sleepTimerRunnable, minutes * 60 * 1000L);
    }

    public void cancelSleepTimer() {
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = null;
        }
    }

    public void release() {
        cancelSleepTimer();
        handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        instance = null;
        if (MyApplication.getInstance() != null) {
            Intent intent = new Intent(MyApplication.getInstance(), AudioPlayerService.class);
            intent.setAction(AudioPlayerService.ACTION_STOP);
            MyApplication.getInstance().startService(intent);
        }
    }

    public boolean isPlaying() { return isPlaying; }
    public Book getCurrentBook() { return currentBook; }
    public int getCurrentPosition() { return mediaPlayer.getCurrentPosition(); }
    public int getDuration() { return mediaPlayer.getDuration(); }

    private void startProgressUpdate() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying && isPrepared) {
                    try {
                        int current = mediaPlayer.getCurrentPosition();
                        int total = mediaPlayer.getDuration();
                        if (callback != null) callback.onProgress(current, total);
                        
                        if (currentChapterIndex >= 0 && currentChapterIndex < chapters.size()) {
                            Chapter currentChapter = chapters.get(currentChapterIndex);
                            if (currentChapter.getEndTime() > 0 && current >= currentChapter.getEndTime()) {
                                nextChapter();
                                return;
                            }
                        }
                        
                        handler.postDelayed(this, 1000);
                    } catch (Exception e) {}
                }
            }
        }, 1000);
    }
}