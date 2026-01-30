package com.nicorp.nimetro.presentation.managers;

import android.content.Context;
import android.util.Log;

import com.nicorp.nimetro.domain.entities.StationNotification;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StationNotificationManager {
    private static final String TAG = "StationNotificationManager";
    private Map<String, StationNotification> stationNotifications;

    public StationNotificationManager(Context context) {
        this.stationNotifications = new HashMap<>();
        loadStationNotifications(context);
    }

    public void loadStationNotifications(Context context) {
        try {
            String json = loadJSONFromAsset(context, "station_notifications.json");
            if (json == null) {
                Log.w(TAG, "Failed to load station_notifications.json");
                return;
            }

            JSONObject jsonObject = new JSONObject(json);
            JSONObject stationNotificationsObj = jsonObject.optJSONObject("station_notifications");
            if (stationNotificationsObj == null) {
                return;
            }

            stationNotifications.clear();
            Iterator<String> keys = stationNotificationsObj.keys();
            while (keys.hasNext()) {
                String stationId = keys.next();
                JSONObject notificationObj = stationNotificationsObj.getJSONObject(stationId);
                StationNotification notification = parseStationNotification(notificationObj);
                if (notification != null) {
                    stationNotifications.put(stationId, notification);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading station notifications", e);
        }
    }

    private StationNotification parseStationNotification(JSONObject jsonObject) throws JSONException {
        String typeStr = jsonObject.getString("type");
        StationNotification.NotificationType type = "important".equals(typeStr)
            ? StationNotification.NotificationType.IMPORTANT
            : StationNotification.NotificationType.NORMAL;

        String text = jsonObject.getString("text");
        return new StationNotification(type, text);
    }

    public StationNotification getNotificationForStation(String stationId) {
        return stationNotifications.get(stationId);
    }

    public Map<String, StationNotification> getAllNotifications() {
        return new HashMap<>(stationNotifications);
    }

    private String loadJSONFromAsset(Context context, String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("raw/" + fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e(TAG, "Error loading JSON from assets", ex);
            return null;
        }
        return json;
    }
}

