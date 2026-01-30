package com.nicorp.nimetro.data.repositories;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class LocalNotificationsCache {
    private static final String TAG = "LocalNotificationsCache";
    private static final String NOTIFICATIONS_FILE = "notifications_cache.json";
    private final Context context;
    private final File notificationsFile;

    public LocalNotificationsCache(Context context) {
        this.context = context.getApplicationContext();
        this.notificationsFile = new File(this.context.getFilesDir(), NOTIFICATIONS_FILE);
    }

    public boolean saveNotifications(JSONArray notifications) {
        if (notifications == null) {
            Log.e(TAG, "Cannot save notifications: notifications is null");
            return false;
        }

        try {
            JSONObject wrapper = new JSONObject();
            wrapper.put("general_notifications", notifications);
            wrapper.put("cached_at", System.currentTimeMillis());
            
            String jsonString = wrapper.toString();
            
            FileOutputStream fos = new FileOutputStream(notificationsFile);
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.close();
            
            Log.d(TAG, "Notifications saved to cache");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving notifications to cache", e);
            return false;
        }
    }

    public JSONArray getNotifications() {
        try {
            if (!notificationsFile.exists()) {
                Log.d(TAG, "Notifications cache file not found");
                return null;
            }

            FileInputStream fis = new FileInputStream(notificationsFile);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONObject wrapper = new JSONObject(jsonString);
            
            if (wrapper.has("general_notifications")) {
                return wrapper.getJSONArray("general_notifications");
            }
            
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error loading notifications from cache", e);
            return null;
        }
    }

    public long getCacheTimestamp() {
        try {
            if (!notificationsFile.exists()) {
                return 0;
            }

            FileInputStream fis = new FileInputStream(notificationsFile);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONObject wrapper = new JSONObject(jsonString);
            
            if (wrapper.has("cached_at")) {
                return wrapper.getLong("cached_at");
            }
            
            return notificationsFile.lastModified();
        } catch (Exception e) {
            Log.e(TAG, "Error getting cache timestamp", e);
            return 0;
        }
    }

    public boolean hasNotifications() {
        return notificationsFile.exists() && notificationsFile.length() > 0;
    }

    public void clearCache() {
        try {
            if (notificationsFile.exists()) {
                boolean deleted = notificationsFile.delete();
                Log.d(TAG, "Notifications cache cleared: " + deleted);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing notifications cache", e);
        }
    }
}

