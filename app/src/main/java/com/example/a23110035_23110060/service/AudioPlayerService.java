package com.example.a23110035_23110060.service;

import com.example.a23110035_23110060.view.activity.MainActivity;

import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.model.Book;
import com.example.a23110035_23110060.controller.PlayerManager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

public class AudioPlayerService extends Service implements AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY_PAUSE = "action_play_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";
    public static final String ACTION_UPDATE_NOTIFICATION = "action_update_notification";

    private static final String CHANNEL_ID = "audio_player_channel";
    private static final int NOTIFICATION_ID = 101;

    private MediaSessionCompat mediaSession;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private BroadcastReceiver noisyReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initMediaSession();
        initAudioManager();
        registerNoisyReceiver();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Audio Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Playing audio in background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "AudioPlayerService");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                PlayerManager.getInstance().togglePlayPause();
            }

            @Override
            public void onPause() {
                PlayerManager.getInstance().togglePlayPause();
            }

            @Override
            public void onSkipToNext() {
                PlayerManager.getInstance().skipNext();
            }

            @Override
            public void onSkipToPrevious() {
                PlayerManager.getInstance().previousChapter();
            }
        });
        mediaSession.setActive(true);
    }

    private void initAudioManager() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    private void registerNoisyReceiver() {
        noisyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                    if (PlayerManager.getInstance().isPlaying()) {
                        PlayerManager.getInstance().togglePlayPause();
                    }
                }
            }
        };
        registerReceiver(noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    private boolean requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            int res = audioManager.requestAudioFocus(audioFocusRequest);
            return res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            int res = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            return res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (PlayerManager.getInstance().isPlaying()) {
                    PlayerManager.getInstance().togglePlayPause();
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (!PlayerManager.getInstance().isPlaying() && PlayerManager.getInstance().getCurrentBook() != null) {
                    PlayerManager.getInstance().togglePlayPause();
                }
                break;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_PLAY_PAUSE:
                    PlayerManager.getInstance().togglePlayPause();
                    break;
                case ACTION_NEXT:
                    PlayerManager.getInstance().skipNext();
                    break;
                case ACTION_PREVIOUS:
                    PlayerManager.getInstance().previousChapter();
                    break;
                case ACTION_STOP:
                    stopForeground(true);
                    stopSelf();
                    break;
                case ACTION_UPDATE_NOTIFICATION:
                    updateNotification();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    public void updateNotification() {
        Book book = PlayerManager.getInstance().getCurrentBook();
        if (book == null) {
            stopForeground(true);
            return;
        }

        boolean isPlaying = PlayerManager.getInstance().isPlaying();
        if (isPlaying) {
            requestAudioFocus();
        }

        // Update MediaSession state
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        
        stateBuilder.setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, 
                              PlayerManager.getInstance().getCurrentPosition(), 1.0f);
        mediaSession.setPlaybackState(stateBuilder.build());

        // Update MediaSession metadata
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, book.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, book.getAuthorName());
        mediaSession.setMetadata(metadataBuilder.build());

        // Intents
        Intent playPauseIntent = new Intent(this, AudioPlayerService.class).setAction(ACTION_PLAY_PAUSE);
        PendingIntent pendingPlayPause = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(this, AudioPlayerService.class).setAction(ACTION_NEXT);
        PendingIntent pendingNext = PendingIntent.getService(this, 1, nextIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent prevIntent = new Intent(this, AudioPlayerService.class).setAction(ACTION_PREVIOUS);
        PendingIntent pendingPrev = PendingIntent.getService(this, 2, prevIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        Intent stopIntent = new Intent(this, AudioPlayerService.class).setAction(ACTION_STOP);
        PendingIntent pendingStop = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Open App Intent
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingOpenApp = PendingIntent.getActivity(this, 4, openAppIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_headphones)
                .setContentTitle(book.getTitle())
                .setContentText(book.getAuthorName())
                .setContentIntent(pendingOpenApp)
                .setDeleteIntent(pendingStop)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()));

        builder.addAction(R.drawable.ic_skip_previous, "Previous", pendingPrev);

        if (isPlaying) {
            builder.addAction(R.drawable.ic_pause, "Pause", pendingPlayPause);
        } else {
            builder.addAction(R.drawable.ic_play, "Play", pendingPlayPause);
        }

        builder.addAction(R.drawable.ic_skip_next, "Next", pendingNext);
        builder.addAction(R.drawable.ic_close, "Close", pendingStop);

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (noisyReceiver != null) {
            unregisterReceiver(noisyReceiver);
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
        abandonAudioFocus();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
