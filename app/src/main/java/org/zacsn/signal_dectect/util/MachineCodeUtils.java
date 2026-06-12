package org.zacsn.signal_dectect.util;

import android.content.Context;
import android.provider.Settings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

public final class MachineCodeUtils {
    private static final String FALLBACK_MACHINE_CODE = "fallback_machine_code";

    private MachineCodeUtils() {
    }

    public static String getMachineCode(Context context) {
        String androidId = "";
        try {
            androidId = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
        } catch (Exception ignored) {
            androidId = "";
        }

        if (androidId == null || androidId.trim().isEmpty()
                || "9774d56d682e549c".equalsIgnoreCase(androidId)) {
            return getFallbackMachineCode(context);
        }

        String source = context.getPackageName() + ":" + androidId.trim().toLowerCase(Locale.US);
        return "MC-" + sha256(source).substring(0, 16).toUpperCase(Locale.US);
    }

    private static String getFallbackMachineCode(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(
                "SignalDetectSession",
                Context.MODE_PRIVATE
        );
        String existing = prefs.getString(FALLBACK_MACHINE_CODE, null);
        if (existing != null && !existing.trim().isEmpty()) {
            return existing;
        }

        String generated = "MC-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase(Locale.US);
        prefs.edit().putString(FALLBACK_MACHINE_CODE, generated).apply();
        return generated;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8))
                    .toString()
                    .replace("-", "");
        }
    }
}
