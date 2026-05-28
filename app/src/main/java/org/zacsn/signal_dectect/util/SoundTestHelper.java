package org.zacsn.signal_dectect.util;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Helper class for testing sound playback.
 * Provides simple methods to test if audio system is working.
 */
public class SoundTestHelper {
    
    private static final String TAG = "SoundTestHelper";
    
    /**
     * Play a simple test beep to verify audio is working.
     * This is a blocking call that plays a 1-second beep.
     * 
     * @param context Application context
     * @return true if sound played successfully, false otherwise
     */
    public static boolean playTestBeep(Context context) {
        Log.i(TAG, "Playing test beep...");
        
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            Log.e(TAG, "AudioManager is null");
            return false;
        }
        
        // Check volume
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.i(TAG, "Current volume: " + volume + "/" + maxVolume);
        
        if (volume == 0) {
            Log.w(TAG, "Volume is 0 - sound will not be audible!");
        }
        
        AudioTrack audioTrack = null;
        try {
            // Generate a 440Hz tone for 1 second
            int sampleRate = 44100;
            int duration = 1; // seconds
            int numSamples = duration * sampleRate;
            byte[] generatedSound = new byte[2 * numSamples];
            
            // Generate sine wave
            for (int i = 0; i < numSamples; i++) {
                double sample = Math.sin(2 * Math.PI * i / (sampleRate / 440.0));
                short val = (short) (sample * 32767);
                generatedSound[i * 2] = (byte) (val & 0x00ff);
                generatedSound[i * 2 + 1] = (byte) ((val & 0xff00) >>> 8);
            }
            
            // Create AudioTrack
            int bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );

            
            Log.i(TAG, "Min buffer size: " + bufferSize);
            
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(bufferSize, generatedSound.length),
                    AudioTrack.MODE_STATIC
            );
            
            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "AudioTrack not initialized - state: " + audioTrack.getState());
                return false;
            }
            
            // Write audio data
            int written = audioTrack.write(generatedSound, 0, generatedSound.length);
            Log.i(TAG, "Wrote " + written + " bytes to AudioTrack");
            
            // Request audio focus
            int focusResult = audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            );
            
            if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.w(TAG, "Audio focus not granted");
            }
            
            // Play
            audioTrack.play();
            Log.i(TAG, "Playing test beep...");
            
            // Wait for playback to complete
            Thread.sleep(1100);
            
            audioTrack.stop();
            Log.i(TAG, "Test beep completed");
            
            // Abandon audio focus
            audioManager.abandonAudioFocus(null);
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing test beep", e);
            return false;
        } finally {
            if (audioTrack != null) {
                try {
                    audioTrack.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing AudioTrack", e);
                }
            }
        }
    }
    
    /**
     * Run a comprehensive audio test in a background thread.
     * Results are logged to Logcat.
     * 
     * @param context Application context
     */
    public static void runAudioTest(Context context) {
        new Thread(() -> {
            Log.i(TAG, "========== Audio Test Start ==========");
            
            // Run diagnostics
            AudioDiagnostics.runDiagnostics(context);
            
            // Play test beep
            boolean success = playTestBeep(context);
            
            if (success) {
                Log.i(TAG, "✓ Audio test PASSED - sound system is working");
            } else {
                Log.e(TAG, "✗ Audio test FAILED - check logs for details");
            }
            
            Log.i(TAG, "========== Audio Test End ==========");
        }).start();
    }
}
