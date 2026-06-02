package com.example.a23110035_23110060.controller;

import com.example.a23110035_23110060.MyApplication;

import com.example.a23110035_23110060.model.Book;
import com.example.a23110035_23110060.service.AudioPlayerService;
import com.example.a23110035_23110060.model.Chapter;

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
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private List<Chapter> chapters = new ArrayList<>();
    private int currentChapterIndex = -1;
    private boolean isPrepared = false;
    
    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
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
        playBook(book, -1);
    }

    public void playBook(Book book, int startPositionMs) {
        if (book == null || book.getAudioUrl() == null || book.getAudioUrl().isEmpty()) {
            return;
        }

        try {
            this.currentBook = book;
            
            // Save last played book ID
            if (MyApplication.getInstance() != null) {
                new SessionManager(MyApplication.getInstance()).setLastPlayedBookId(book.getId());
            }

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
                if (startPositionMs >= 0) {
                    startPlayingFrom(startPositionMs);
                } else {
                    fetchLastProgressAndPlay();
                }
            });
        } catch (IOException e) {
            Log.e("PlayerManager", "Error", e);
        }
    }

    private void fetchLastProgressAndPlay() {
        if (currentBook == null || MyApplication.getInstance() == null) {
            mediaPlayer.start();
            isPlaying = true;
            if (callback != null) callback.onStateChange(true);
            startProgressUpdate();
            notifyService();
            return;
        }

        SessionManager session = new SessionManager(MyApplication.getInstance());
        String userId = session.getUserId();
        String token = session.getAccessToken();
        if (userId == null || token == null) {
            mediaPlayer.start();
            isPlaying = true;
            if (callback != null) callback.onStateChange(true);
            startProgressUpdate();
            notifyService();
            return;
        }

        String url = com.example.a23110035_23110060.BuildConfig.SUPABASE_URL + "/rest/v1/progress?user_id=eq." + userId + "&book_id=eq." + currentBook.getId() + "&select=last_page_number";
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("apikey", com.example.a23110035_23110060.BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        NetworkClient.getClient(MyApplication.getInstance()).newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                startPlayingFrom(0);
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        org.json.JSONArray arr = new org.json.JSONArray(response.body().string());
                        if (arr.length() > 0) {
                            int lastPosition = arr.getJSONObject(0).optInt("last_page_number", 0);
                            startPlayingFrom(lastPosition);
                            return;
                        }
                    } catch (Exception e) {}
                }
                startPlayingFrom(0);
            }
        });
    }

    private void startPlayingFrom(int position) {
        handler.post(() -> {
            mediaPlayer.seekTo(position);
            mediaPlayer.start();
            isPlaying = true;
            if (callback != null) callback.onStateChange(true);
            startProgressUpdate();
            notifyService();
        });
    }

    public void autoSaveProgress() {
        if (currentBook == null || mediaPlayer == null || !isPrepared || MyApplication.getInstance() == null) return;
        
        SessionManager session = new SessionManager(MyApplication.getInstance());
        String userId = session.getUserId();
        String token = session.getAccessToken();
        if (userId == null || token == null) return;

        int currentPos = mediaPlayer.getCurrentPosition();
        int total = mediaPlayer.getDuration();
        int percent = total > 0 ? (int) (((float) currentPos / total) * 100) : 0;

        String url = com.example.a23110035_23110060.BuildConfig.SUPABASE_URL + "/rest/v1/progress?on_conflict=user_id,book_id";
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            json.put("user_id", userId);
            json.put("book_id", currentBook.getId());
            json.put("last_page_number", currentPos); // use last_page_number as milliseconds for audio
            json.put("percent_complete", percent);
        } catch (Exception e) {}

        okhttp3.RequestBody body = okhttp3.RequestBody.create(json.toString(), okhttp3.MediaType.parse("application/json"));
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("apikey", com.example.a23110035_23110060.BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build();

        NetworkClient.getClient(MyApplication.getInstance()).newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, IOException e) {}
            @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) {}
        });
    }

    public void togglePlayPause() {
        if (currentBook == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            autoSaveProgress();
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
        // Do not force playChapter(0) here, it overwrites the progress state
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
        autoSaveProgress();
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
    public int getCurrentPosition() { return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0; }
    public int getDuration() { return mediaPlayer != null ? mediaPlayer.getDuration() : 0; }
    
    public String getCurrentChapterTitle() {
        if (chapters != null && !chapters.isEmpty()) {
            int current = getCurrentPosition();
            for (int i = 0; i < chapters.size(); i++) {
                Chapter c = chapters.get(i);
                if (current >= c.getStartTime() && (c.getEndTime() <= 0 || current < c.getEndTime())) {
                    currentChapterIndex = i; // update internal index
                    if (c.getTitle() != null && !c.getTitle().isEmpty()) return c.getTitle();
                }
            }
            // fallback if out of bounds but index is valid
            if (currentChapterIndex >= 0 && currentChapterIndex < chapters.size()) {
                String title = chapters.get(currentChapterIndex).getTitle();
                if (title != null && !title.isEmpty()) return title;
            }
        }
        return "PLAYING FROM LIBRARY";
    }

    private int progressSaveCounter = 0;

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
                        
                        progressSaveCounter++;
                        if (progressSaveCounter >= 10) {
                            autoSaveProgress();
                            progressSaveCounter = 0;
                        }

                        if (chapters != null && currentChapterIndex >= 0 && currentChapterIndex < chapters.size()) {
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