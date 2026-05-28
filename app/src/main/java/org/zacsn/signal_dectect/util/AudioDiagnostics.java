package org.zacsn.signal_dectect.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.util.Log;

/**
 * Diagnostic utility for audio system.
 * Helps identify audio playback issues.
 */
public class AudioDiagnostics {
    
    private static final String TAG = "AudioDiagnostics";
    
    /**
     * Run comprehensive audio diagnostics.
     */
    public static void runDiagnostics(Context context) {
        Log.i(TAG, "========== Audio Diagnostics Start ==========");
        
        // Check AudioManager
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            Log.e(TAG, "AudioManager is NULL!");
            return;
        }
        
        // Check volume levels
        checkVolumeLevels(audioManager);
        
        // Check audio mode
        checkAudioMode(audioManager);
        
        // Check ringer mode
        checkRingerMode(audioManager);
        
        // Check audio focus
        checkAudioFocus(audioManager);
        
        // Test AudioTrack creation
        testAudioTrackCreation();
        
        Log.i(TAG, "========== Audio Diagnostics End ==========");
    }

    
    private static void checkVolumeLevels(AudioManager audioManager) {
        Log.i(TAG, "--- Volume Levels ---");
        
        int musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int musicMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.i(TAG, "Music: " + musicVolume + "/" + musicMaxVolume + 
              " (" + (musicVolume * 100 / musicMaxVolume) + "%)");
        
        int alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        int alarmMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        Log.i(TAG, "Alarm: " + alarmVolume + "/" + alarmMaxVolume);
        
        int notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        int notificationMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        Log.i(TAG, "Notification: " + notificationVolume + "/" + notificationMaxVolume);
        
        if (musicVolume == 0) {
            Log.w(TAG, "WARNING: Music volume is 0 - sound will not be audible!");
        }
    }
    
    private static void checkAudioMode(AudioManager audioManager) {
        Log.i(TAG, "--- Audio Mode ---");
        int mode = audioManager.getMode();
        String modeStr;
        switch (mode) {
            case AudioManager.MODE_NORMAL:
                modeStr = "NORMAL";
                break;
            case AudioManager.MODE_RINGTONE:
                modeStr = "RINGTONE";
                break;
            case AudioManager.MODE_IN_CALL:
                modeStr = "IN_CALL";
                break;
            case AudioManager.MODE_IN_COMMUNICATION:
                modeStr = "IN_COMMUNICATION";
                break;
            default:
                modeStr = "UNKNOWN (" + mode + ")";
        }
        Log.i(TAG, "Audio Mode: " + modeStr);
    }

    
    private static void checkRingerMode(AudioManager audioManager) {
        Log.i(TAG, "--- Ringer Mode ---");
        int ringerMode = audioManager.getRingerMode();
        String ringerModeStr;
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_NORMAL:
                ringerModeStr = "NORMAL";
                break;
            case AudioManager.RINGER_MODE_SILENT:
                ringerModeStr = "SILENT";
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                ringerModeStr = "VIBRATE";
                break;
            default:
                ringerModeStr = "UNKNOWN (" + ringerMode + ")";
        }
        Log.i(TAG, "Ringer Mode: " + ringerModeStr);
        
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            Log.w(TAG, "WARNING: Device is in silent mode - some sounds may not play!");
        }
    }
    
    private static void checkAudioFocus(AudioManager audioManager) {
        Log.i(TAG, "--- Audio Focus ---");
        // Note: We can't directly check if we have audio focus without requesting it
        Log.i(TAG, "Audio focus must be requested when playing sound");
    }

    
    private static void testAudioTrackCreation() {
        Log.i(TAG, "--- AudioTrack Test ---");
        
        try {
            int sampleRate = 44100;
            int minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
            
            Log.i(TAG, "Minimum buffer size: " + minBufferSize + " bytes");
            
            if (minBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                Log.e(TAG, "ERROR: Bad value for AudioTrack parameters");
                return;
            }
            
            if (minBufferSize == AudioTrack.ERROR) {
                Log.e(TAG, "ERROR: Unable to query hardware for output properties");
                return;
            }
            
            // Try to create a test AudioTrack
            AudioTrack testTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM
            );
            
            int state = testTrack.getState();
            if (state == AudioTrack.STATE_INITIALIZED) {
                Log.i(TAG, "SUCCESS: AudioTrack created successfully");
                Log.i(TAG, "Sample rate: " + testTrack.getSampleRate());
                Log.i(TAG, "Channel count: " + testTrack.getChannelCount());
                Log.i(TAG, "Audio format: " + testTrack.getAudioFormat());
            } else {
                Log.e(TAG, "ERROR: AudioTrack not initialized - state: " + state);
            }
            
            testTrack.release();
            
        } catch (Exception e) {
            Log.e(TAG, "ERROR: Exception creating AudioTrack", e);
        }
    }
}
