package org.zacsn.signal_dectect.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Manager for playing sound effects during device scanning.
 * Provides two types of sounds:
 * - Normal scanning sound: Calm beep sound (low frequency)
 * - Alert sound: Sharp, urgent alarm sound (high frequency, for Apple devices)
 * 
 * Uses AudioTrack to generate sine wave tones.
 */
public class SoundEffectManager {
    
    private static final String TAG = "SoundEffectManager";
    
    private final Context context;
    private final AudioManager audioManager;
    private Handler handler;
    private boolean isInitialized = false;
    private boolean isAlertMode = false;
    private boolean isPlaying = false;
    
    // Runnable for repeating sounds
    private Runnable soundRunnable;
    
    // Sound parameters
    private static final int SAMPLE_RATE = 44100;
    private static final int NORMAL_FREQUENCY = 440;  // A4 note - calm
    private static final int ALERT_FREQUENCY = 1000;  // High pitch - urgent
    private static final int NORMAL_DURATION_MS = 200;
    private static final int ALERT_DURATION_MS = 150;
    private static final int NORMAL_INTERVAL_MS = 1000; // 1 second interval
    private static final int ALERT_INTERVAL_MS = 300;   // 0.3 second interval
    
    public SoundEffectManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
        initialize();
    }
    
    /**
     * Initialize the sound manager.
     */
    private void initialize() {
        try {
            // Check if audio is available
            if (audioManager == null) {
                Log.e(TAG, "AudioManager is null");
                return;
            }
            
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            
            Log.d(TAG, "Current volume: " + currentVolume + "/" + maxVolume);
            
            isInitialized = true;
            Log.i(TAG, "SoundEffectManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SoundEffectManager", e);
        }
    }

    
    /**
     * Generate a sine wave tone.
     */
    private byte[] generateTone(int frequency, int durationMs) {
        int numSamples = (int) (durationMs * SAMPLE_RATE / 1000.0);
        byte[] generatedSound = new byte[2 * numSamples];
        
        double[] sample = new double[numSamples];
        
        // Generate sine wave
        for (int i = 0; i < numSamples; i++) {
            sample[i] = Math.sin(2 * Math.PI * i / (SAMPLE_RATE / (double) frequency));
        }
        
        // Convert to 16-bit PCM
        int idx = 0;
        for (double dVal : sample) {
            short val = (short) (dVal * 32767);
            generatedSound[idx++] = (byte) (val & 0x00ff);
            generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        
        return generatedSound;
    }
    
    /**
     * Play a tone using AudioTrack.
     */
    private void playTone(int frequency, int durationMs) {
        try {
            byte[] generatedSound = generateTone(frequency, durationMs);
            
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            
            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build();
            
            int bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
            
            AudioTrack audioTrack = new AudioTrack(
                    audioAttributes,
                    audioFormat,
                    Math.max(bufferSize, generatedSound.length),
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
            );
            
            audioTrack.write(generatedSound, 0, generatedSound.length);
            audioTrack.play();
            
            // Release after playing
            handler.postDelayed(() -> {
                audioTrack.stop();
                audioTrack.release();
            }, durationMs + 100);
            
            Log.d(TAG, "Played tone: " + frequency + "Hz for " + durationMs + "ms");
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing tone", e);
        }
    }

    
    /**
     * Start playing normal scanning sound (calm beep).
     * Plays in a loop until stopped or switched to alert mode.
     */
    public void startNormalScanSound() {
        if (!isInitialized) {
            Log.w(TAG, "Cannot play normal scan sound - not initialized");
            return;
        }
        
        Log.i(TAG, "Starting normal scan sound...");
        stopAllSounds();
        isAlertMode = false;
        isPlaying = true;
        
        // Create repeating sound runnable
        soundRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && !isAlertMode) {
                    playTone(NORMAL_FREQUENCY, NORMAL_DURATION_MS);
                    handler.postDelayed(this, NORMAL_INTERVAL_MS);
                }
            }
        };
        
        handler.post(soundRunnable);
        Log.i(TAG, "Normal scan sound started");
    }
    
    /**
     * Switch to alert sound (sharp, urgent alarm).
     * Called when an Apple device is detected.
     */
    public void switchToAlertSound() {
        if (!isInitialized) {
            Log.w(TAG, "Cannot play alert sound - not initialized");
            return;
        }
        
        if (isAlertMode) {
            Log.d(TAG, "Already in alert mode");
            return;
        }
        
        Log.i(TAG, "Switching to alert sound...");
        stopAllSounds();
        isAlertMode = true;
        isPlaying = true;
        
        // Create repeating alert sound runnable
        soundRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && isAlertMode) {
                    playTone(ALERT_FREQUENCY, ALERT_DURATION_MS);
                    handler.postDelayed(this, ALERT_INTERVAL_MS);
                }
            }
        };
        
        handler.post(soundRunnable);
        Log.i(TAG, "Alert sound started");
    }
    
    /**
     * Stop all sound playback.
     */
    public void stopAllSounds() {
        if (soundRunnable != null) {
            handler.removeCallbacks(soundRunnable);
            soundRunnable = null;
        }
        isPlaying = false;
        isAlertMode = false;
        Log.i(TAG, "All sounds stopped");
    }
    
    /**
     * Check if currently in alert mode.
     */
    public boolean isAlertMode() {
        return isAlertMode;
    }
    
    /**
     * Check if any sound is currently playing.
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * Release resources.
     * Should be called when the manager is no longer needed.
     */
    public void release() {
        Log.i(TAG, "Releasing SoundEffectManager...");
        stopAllSounds();
        isInitialized = false;
        Log.i(TAG, "SoundEffectManager released");
    }
}
