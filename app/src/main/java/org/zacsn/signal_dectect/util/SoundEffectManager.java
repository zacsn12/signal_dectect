package org.zacsn.signal_dectect.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import org.zacsn.signal_dectect.R;

/**
 * Plays scan feedback sounds from packaged audio files.
 */
public class SoundEffectManager {

    private static final String TAG = "SoundEffectManager";

    private final Context context;
    private final AudioManager audioManager;
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener;

    private AudioFocusRequest audioFocusRequest;
    private MediaPlayer mediaPlayer;
    private boolean playing;
    private boolean alertMode;
    private boolean hasAudioFocus;
    private float appVolume = 1.0f;

    public SoundEffectManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        this.focusChangeListener = focusChange -> {
            Log.d(TAG, "Audio focus changed: " + focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                stopAllSounds();
            }
        };
    }

    public boolean startNormalScanSound() {
        Log.i(TAG, "Start normal scan sound");
        return startSound(R.raw.normal_scan, false);
    }

    public boolean switchToAlertSound() {
        Log.i(TAG, "Switch to Apple alert sound");
        return startSound(R.raw.apple_alert, true);
    }

    public void stopAllSounds() {
        Log.i(TAG, "Stop all sounds");
        playing = false;
        alertMode = false;
        releaseMediaPlayer();
        abandonAudioFocus();
    }

    public boolean isPlaying() {
        return playing && mediaPlayer != null;
    }

    public boolean isAlertMode() {
        return alertMode;
    }

    public void release() {
        stopAllSounds();
    }

    public void setVolume(float volume) {
        appVolume = Math.max(0.0f, Math.min(1.0f, volume));
        applyPlayerVolume();
        Log.i(TAG, "App sound volume=" + appVolume);
    }

    private boolean startSound(int rawResId, boolean alert) {
        releaseMediaPlayer();
        requestAudioFocus();
        ensureAudibleVolume();

        try {
            mediaPlayer = new MediaPlayer();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            AssetFileDescriptor afd = context.getResources().openRawResourceFd(rawResId);
            if (afd == null) {
                Log.e(TAG, "Unable to open raw sound resource: " + rawResId);
                playing = false;
                alertMode = false;
                releaseMediaPlayer();
                return false;
            }

            try {
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            } finally {
                afd.close();
            }

            mediaPlayer.setLooping(true);
            applyPlayerVolume();
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error, what=" + what + ", extra=" + extra);
                stopAllSounds();
                return true;
            });
            mediaPlayer.prepare();
            preferBuiltInSpeaker(mediaPlayer);
            mediaPlayer.start();
            logRoutedDevice(mediaPlayer);

            playing = true;
            alertMode = alert;
            Log.i(TAG, "MediaPlayer sound started, alert=" + alert);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to play sound resource: " + rawResId, e);
            playing = false;
            alertMode = false;
            releaseMediaPlayer();
            return false;
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer == null) {
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        } catch (Exception e) {
            Log.d(TAG, "Ignoring MediaPlayer stop failure", e);
        }

        try {
            mediaPlayer.release();
        } catch (Exception e) {
            Log.e(TAG, "MediaPlayer release failed", e);
        } finally {
            mediaPlayer = null;
        }
    }

    private void applyPlayerVolume() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(appVolume, appVolume);
        }
    }

    private void requestAudioFocus() {
        if (audioManager == null || hasAudioFocus) {
            return;
        }

        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build();
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            );
        }

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if (!hasAudioFocus) {
            Log.w(TAG, "Audio focus was not granted");
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null || !hasAudioFocus) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
            audioFocusRequest = null;
        } else {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
        hasAudioFocus = false;
    }

    private void ensureAudibleVolume() {
        if (audioManager == null) {
            return;
        }

        int musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.i(TAG, "Media volume=" + musicVolume + "/" + musicMax);

        if (musicVolume == 0) {
            Log.w(TAG, "Media volume is 0; sound will not be audible");
        }
    }

    private void preferBuiltInSpeaker(MediaPlayer player) {
        if (audioManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        AudioDeviceInfo[] outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : outputDevices) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                boolean preferred = player.setPreferredDevice(device);
                Log.i(TAG, "Preferred built-in speaker result=" + preferred + ", id=" + device.getId());
                return;
            }
        }
        Log.w(TAG, "Built-in speaker output device not found");
    }

    private void logRoutedDevice(MediaPlayer player) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        AudioDeviceInfo routedDevice = player.getRoutedDevice();
        if (routedDevice != null) {
            Log.i(TAG, "MediaPlayer routed device type=" + routedDevice.getType() + ", id=" + routedDevice.getId());
        } else {
            Log.i(TAG, "MediaPlayer routed device is not available yet");
        }
    }
}
