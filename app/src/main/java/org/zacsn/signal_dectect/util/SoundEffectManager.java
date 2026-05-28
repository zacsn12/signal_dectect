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
    
    // Reusable AudioTrack instances
    private AudioTrack normalTrack;
    private AudioTrack alertTrack;
    
    // Runnable for repeating sounds
    private Runnable soundRunnable;
    
    // Audio focus
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private boolean hasAudioFocus = false;
    
    // Sound parameters
    private static final int SAMPLE_RATE = 44100;
    
    // Normal scan sound - faster, more active feeling
    private static final int NORMAL_FREQUENCY = 800;      // Higher pitch for better attention
    private static final int NORMAL_DURATION_MS = 100;    // Shorter beep (was 200ms)
    private static final int NORMAL_INTERVAL_MS = 500;    // Faster interval (was 1000ms)
    
    // Alert sound - urgent, attention-grabbing
    private static final int ALERT_FREQUENCY = 1200;      // Very high pitch for urgency
    private static final int ALERT_DURATION_MS = 120;     // Short sharp beep
    private static final int ALERT_INTERVAL_MS = 200;     // Very fast (was 300ms)
    
    public SoundEffectManager(Context context) {
        Log.i(TAG, "SoundEffectManager constructor called");
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
        
        Log.i(TAG, "Setting up audio focus listener");
        // Setup audio focus listener
        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                Log.d(TAG, "Audio focus changed: " + focusChange);
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        hasAudioFocus = false;
                        stopAllSounds();
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        hasAudioFocus = true;
                        break;
                }
            }
        };
        
        Log.i(TAG, "Running diagnostics");
        // Run diagnostics
        try {
            AudioDiagnostics.runDiagnostics(context);
        } catch (Exception e) {
            Log.e(TAG, "Error running diagnostics", e);
        }
        
        Log.i(TAG, "Calling initialize()");
        initialize();
        Log.i(TAG, "SoundEffectManager constructor completed");
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
            
            // Warn if volume is too low
            if (currentVolume == 0) {
                Log.w(TAG, "Media volume is 0 - sound will not be audible!");
            } else if (currentVolume < maxVolume / 4) {
                Log.w(TAG, "Media volume is low (" + currentVolume + "/" + maxVolume + ")");
            }
            
            // Calculate buffer size
            int bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
            Log.d(TAG, "Minimum buffer size: " + bufferSize);
            
            // Initialize normal track using MODE_STREAM (more reliable than MODE_STATIC)
            byte[] normalSound = generateTone(NORMAL_FREQUENCY, NORMAL_DURATION_MS);
            Log.d(TAG, "Normal sound buffer size: " + normalSound.length);
            
            normalTrack = new AudioTrack(
                    AudioManager.STREAM_ALARM,  // Use ALARM stream to bypass silent mode
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM  // Use STREAM mode instead of STATIC
            );
            
            Log.d(TAG, "Normal track created, state: " + normalTrack.getState());
            
            if (normalTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                normalTrack.setVolume(1.0f);
                Log.i(TAG, "Normal track initialized successfully");
            } else {
                Log.e(TAG, "Failed to initialize normalTrack - state: " + normalTrack.getState());
                return;
            }
            
            // Initialize alert track using MODE_STREAM
            byte[] alertSound = generateTone(ALERT_FREQUENCY, ALERT_DURATION_MS);
            Log.d(TAG, "Alert sound buffer size: " + alertSound.length);
            
            alertTrack = new AudioTrack(
                    AudioManager.STREAM_ALARM,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM  // Use STREAM mode instead of STATIC
            );
            
            if (alertTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                int written = alertTrack.write(alertSound, 0, alertSound.length);
                Log.d(TAG, "Alert track: wrote " + written + " bytes");
                alertTrack.setVolume(1.0f);
                alertTrack.setLoopPoints(0, alertSound.length / 2, 0); // No loop, play once
                Log.i(TAG, "Alert track initialized successfully");
            } else {
                Log.e(TAG, "Failed to initialize alertTrack - state: " + alertTrack.getState());
                // Clean up normal track if alert track fails
                if (normalTrack != null) {
                    normalTrack.release();
                    normalTrack = null;
                }
                return;
            }
            
            isInitialized = true;
            Log.i(TAG, "SoundEffectManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SoundEffectManager", e);
            // Clean up on error
            if (normalTrack != null) {
                try {
                    normalTrack.release();
                } catch (Exception ex) {
                    // Ignore
                }
                normalTrack = null;
            }
            if (alertTrack != null) {
                try {
                    alertTrack.release();
                } catch (Exception ex) {
                    // Ignore
                }
                alertTrack = null;
            }
        }
    }
    
    /**
     * Request audio focus for playback.
     */
    private boolean requestAudioFocus() {
        if (audioManager == null) {
            Log.e(TAG, "Cannot request audio focus - AudioManager is null");
            return false;
        }
        
        if (hasAudioFocus) {
            Log.d(TAG, "Already have audio focus");
            return true;
        }
        
        int result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        );
        
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        
        if (hasAudioFocus) {
            Log.i(TAG, "Audio focus granted");
        } else {
            Log.w(TAG, "Audio focus denied");
        }
        
        return hasAudioFocus;
    }
    
    /**
     * Abandon audio focus.
     */
    private void abandonAudioFocus() {
        if (audioManager != null && hasAudioFocus) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
            hasAudioFocus = false;
            Log.d(TAG, "Audio focus abandoned");
        }
    }
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
     * Play normal tone.
     */
    private void playNormalTone() {
        try {
            if (normalTrack == null) {
                Log.e(TAG, "normalTrack is null");
                return;
            }
            
            int state = normalTrack.getState();
            int playState = normalTrack.getPlayState();
            
            if (state == AudioTrack.STATE_INITIALIZED) {
                // Generate and write audio data
                byte[] normalSound = generateTone(NORMAL_FREQUENCY, NORMAL_DURATION_MS);
                
                // Start playing if not already playing
                if (playState != AudioTrack.PLAYSTATE_PLAYING) {
                    normalTrack.play();
                }
                
                // Write audio data
                int written = normalTrack.write(normalSound, 0, normalSound.length);
                Log.d(TAG, "Played normal tone, wrote " + written + " bytes");
            } else {
                Log.e(TAG, "normalTrack is not initialized - state: " + state);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing normal tone", e);
        }
    }
    
    /**
     * Play alert tone.
     */
    private void playAlertTone() {
        try {
            if (alertTrack == null) {
                Log.e(TAG, "alertTrack is null");
                return;
            }
            
            int state = alertTrack.getState();
            int playState = alertTrack.getPlayState();
            
            if (state == AudioTrack.STATE_INITIALIZED) {
                // Generate and write audio data
                byte[] alertSound = generateTone(ALERT_FREQUENCY, ALERT_DURATION_MS);
                
                // Start playing if not already playing
                if (playState != AudioTrack.PLAYSTATE_PLAYING) {
                    alertTrack.play();
                }
                
                // Write audio data
                int written = alertTrack.write(alertSound, 0, alertSound.length);
                Log.d(TAG, "Played alert tone, wrote " + written + " bytes");
            } else {
                Log.e(TAG, "alertTrack is not initialized - state: " + state);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing alert tone", e);
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
        
        // Request audio focus
        if (!requestAudioFocus()) {
            Log.w(TAG, "Failed to get audio focus - sound may not play properly");
        }
        
        stopAllSounds();
        isAlertMode = false;
        isPlaying = true;
        
        // Create repeating sound runnable
        soundRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && !isAlertMode) {
                    playNormalTone();
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
        
        // Request audio focus if we don't have it
        if (!hasAudioFocus) {
            if (!requestAudioFocus()) {
                Log.w(TAG, "Failed to get audio focus - sound may not play properly");
            }
        }
        
        stopAllSounds();
        isAlertMode = true;
        isPlaying = true;
        
        // Create repeating alert sound runnable
        soundRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && isAlertMode) {
                    playAlertTone();
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
        
        if (normalTrack != null) {
            try {
                normalTrack.stop();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (alertTrack != null) {
            try {
                alertTrack.stop();
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Abandon audio focus when stopping
        abandonAudioFocus();
        
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
        abandonAudioFocus();
        isInitialized = false;
        
        if (normalTrack != null) {
            try {
                normalTrack.stop();
                normalTrack.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing normalTrack", e);
            }
            normalTrack = null;
        }
        
        if (alertTrack != null) {
            try {
                alertTrack.stop();
                alertTrack.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing alertTrack", e);
            }
            alertTrack = null;
        }
        Log.i(TAG, "SoundEffectManager released");
    }
}
