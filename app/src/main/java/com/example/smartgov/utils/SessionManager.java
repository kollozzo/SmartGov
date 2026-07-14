package com.example.smartgov.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SessionManager {

    private static final String PREF_NAME = "SmartGovSyncPrefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD_HASH = "password_hash";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = this.sharedPreferences.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_JWT_TOKEN, token);
        editor.apply();
    }

    public String fetchAuthToken() {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null);
    }

    public void saveUsername(String username) {
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public String fetchUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    public boolean isLoggedIn() {
        return fetchAuthToken() != null;
    }

    // ────────── Offline credentials support ──────────

    public void saveCredentials(String username, String password) {
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD_HASH, hashPassword(password));
        editor.apply();
    }

    public boolean hasLocalCredentials() {
        return sharedPreferences.getString(KEY_PASSWORD_HASH, null) != null;
    }

    public boolean checkCredentialsLocally(String username, String password) {
        String savedHash = sharedPreferences.getString(KEY_PASSWORD_HASH, null);
        String savedUsername = sharedPreferences.getString(KEY_USERNAME, null);
        if (savedHash == null || savedUsername == null) return false;
        return savedUsername.equals(username) && savedHash.equals(hashPassword(password));
    }

    public void clearSession() {
        editor.remove(KEY_JWT_TOKEN);
        editor.apply();
    }

    public void clearAll() {
        editor.clear();
        editor.apply();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }
}
