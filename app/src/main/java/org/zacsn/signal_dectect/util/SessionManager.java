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
    private static final String KEY_MACHINE_CODE = "machine_code";
    private static final String KEY_MAX_MACHINE_BINDINGS = "max_machine_bindings";

    private final Context context;
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void createLoginSession(String username) {
        prefs.edit()
             .putBoolean(KEY_IS_LOGGED_IN, true)
             .putString(KEY_USERNAME, username)
             .putString(KEY_MACHINE_CODE, getMachineCode())
             .apply();
    }

    public void createLoginSession(String username, String userId, String nickname, String token, String validUntil) {
        createLoginSession(username, userId, nickname, token, validUntil, getMachineCode());
    }

    public void createLoginSession(String username, String userId, String nickname, String token, String validUntil, String machineCode) {
        createLoginSession(username, userId, nickname, token, validUntil, machineCode, 1);
    }

    public void createLoginSession(String username, String userId, String nickname, String token, String validUntil, String machineCode, int maxMachineBindings) {
        prefs.edit()
             .putBoolean(KEY_IS_LOGGED_IN, true)
             .putString(KEY_USERNAME, username)
             .putString(KEY_USER_ID, userId)
             .putString(KEY_NICKNAME, nickname)
             .putString(KEY_TOKEN, token)
             .putString(KEY_VALID_UNTIL, validUntil)
             .putString(KEY_MACHINE_CODE, normalizeMachineCode(machineCode))
             .putInt(KEY_MAX_MACHINE_BINDINGS, Math.max(1, maxMachineBindings))
             .apply();
    }

    public void updateAuthorizationInfo(String userId, String nickname, String validUntil) {
        updateAuthorizationInfo(userId, nickname, validUntil, getMachineCode());
    }

    public void updateAuthorizationInfo(String userId, String nickname, String validUntil, String machineCode) {
        updateAuthorizationInfo(userId, nickname, validUntil, machineCode, getMaxMachineBindings());
    }

    public void updateAuthorizationInfo(String userId, String nickname, String validUntil, String machineCode, int maxMachineBindings) {
        prefs.edit()
             .putString(KEY_USER_ID, userId)
             .putString(KEY_NICKNAME, nickname)
             .putString(KEY_VALID_UNTIL, validUntil)
             .putString(KEY_MACHINE_CODE, normalizeMachineCode(machineCode))
             .putInt(KEY_MAX_MACHINE_BINDINGS, Math.max(1, maxMachineBindings))
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
        return getMachineCode();
    }

    public String getMachineCode() {
        String machineCode = MachineCodeUtils.getMachineCode(context);
        prefs.edit().putString(KEY_MACHINE_CODE, machineCode).apply();
        return machineCode;
    }

    public String getBoundMachineCode() {
        String machineCode = prefs.getString(KEY_MACHINE_CODE, "");
        if (machineCode == null || machineCode.trim().isEmpty()) {
            return getMachineCode();
        }
        return machineCode;
    }

    public String getValidUntil() {
        String validUntil = prefs.getString(KEY_VALID_UNTIL, null);
        if (validUntil == null) {
            validUntil = "2099-12-31";
            prefs.edit().putString(KEY_VALID_UNTIL, validUntil).apply();
        }
        return validUntil;
    }

    public int getMaxMachineBindings() {
        return prefs.getInt(KEY_MAX_MACHINE_BINDINGS, 1);
    }

    public void logout() {
        prefs.edit()
             .putBoolean(KEY_IS_LOGGED_IN, false)
             .remove(KEY_USER_ID)
             .remove(KEY_NICKNAME)
             .remove(KEY_TOKEN)
             .remove(KEY_VALID_UNTIL)
             .remove(KEY_MAX_MACHINE_BINDINGS)
             .apply();
    }

    private String normalizeMachineCode(String machineCode) {
        if (machineCode == null || machineCode.trim().isEmpty()) {
            return getMachineCode();
        }
        return machineCode.trim().toUpperCase(java.util.Locale.US);
    }
}
