package org.zacsn.signal_dectect.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "SignalDetectSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_VALID_UNTIL = "valid_until";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void createLoginSession(String username) {
        prefs.edit()
             .putBoolean(KEY_IS_LOGGED_IN, true)
             .putString(KEY_USERNAME, username)
             .apply();
    }

    public void createLoginSession(String username, String userId, String nickname, String token, String validUntil) {
        prefs.edit()
             .putBoolean(KEY_IS_LOGGED_IN, true)
             .putString(KEY_USERNAME, username)
             .putString(KEY_USER_ID, userId)
             .putString(KEY_NICKNAME, nickname)
             .putString(KEY_TOKEN, token)
             .putString(KEY_VALID_UNTIL, validUntil)
             .apply();
    }

    public void updateAuthorizationInfo(String userId, String nickname, String validUntil) {
        prefs.edit()
             .putString(KEY_USER_ID, userId)
             .putString(KEY_NICKNAME, nickname)
             .putString(KEY_VALID_UNTIL, validUntil)
             .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "admin");
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getNickname() {
        return prefs.getString(KEY_NICKNAME, getUsername());
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, "123456");
    }

    public void updatePassword(String newPassword) {
        prefs.edit().putString(KEY_PASSWORD, newPassword).apply();
    }

    public String getSerialNumber() {
        String sn = prefs.getString("serial_number", null);
        if (sn == null) {
            sn = "SN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            prefs.edit().putString("serial_number", sn).apply();
        }
        return sn;
    }

    public String getValidUntil() {
        String validUntil = prefs.getString(KEY_VALID_UNTIL, null);
        if (validUntil == null) {
            validUntil = "2099-12-31";
            prefs.edit().putString(KEY_VALID_UNTIL, validUntil).apply();
        }
        return validUntil;
    }

    public void logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply();
    }
}
