package com.example.a23110035_23110060;

import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;

public class PlayerManager {
    private static PlayerManager instance;
    private MediaPlayer mediaPlayer;
    private Book currentBook;
    private boolean isPlaying = false;
    private float currentSpeed = 1.0f;
    private Handler handler = new Handler(Looper.getMainLooper());
    
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
            mediaPlayer.reset();
            mediaPlayer.setDataSource(book.getAudioUrl());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                setPlaybackSpeed(currentSpeed);
                mediaPlayer.start();
                isPlaying = true;
                if (callback != null) callback.onStateChange(true);
                startProgressUpdate();
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

    public void nextChapter() {}
    public void previousChapter() {}
    public void skipNext() { nextChapter(); }

    public boolean isPlaying() { return isPlaying; }
    public Book getCurrentBook() { return currentBook; }
    public int getCurrentPosition() { return mediaPlayer.getCurrentPosition(); }
    public int getDuration() { return mediaPlayer.getDuration(); }

    private void startProgressUpdate() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    try {
                        int current = mediaPlayer.getCurrentPosition();
                        int total = mediaPlayer.getDuration();
                        if (callback != null) callback.onProgress(current, total);
                        handler.postDelayed(this, 1000);
                    } catch (Exception e) {}
                }
            }
        }, 1000);
    }
}