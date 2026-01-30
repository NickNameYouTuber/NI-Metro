package com.nicorp.nimetro.data.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.nicorp.nimetro.data.api.ApiClient;
import com.nicorp.nimetro.data.api.ApiService;
import com.nicorp.nimetro.data.api.dto.ApiNotification;
import com.nicorp.nimetro.data.exceptions.ApiException;
import com.nicorp.nimetro.data.repositories.LocalNotificationsCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class NotificationSyncService {
    private static final String TAG = "NotificationSyncService";
    private static final String PREFS_NAME = "notification_sync_prefs";
    private static final String KEY_NOTIFICATIONS_LAST_SYNC = "notifications_last_sync";

    private final Context context;
    private final ApiService apiService;
    private final LocalNotificationsCache localCache;

    public NotificationSyncService(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiClient.getInstance(context).getApiService();
        this.localCache = new LocalNotificationsCache(context);
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public JSONArray syncNotifications() throws ApiException {
        if (!isOnline()) {
            Log.w(TAG, "No internet connection, cannot sync notifications");
            throw new ApiException(0, "No internet connection");
        }

        try {
            Call<List<ApiNotification>> call = apiService.getNotifications(null, null);
            Response<List<ApiNotification>> response = call.execute();

            if (!response.isSuccessful()) {
                throw new ApiException(response.code(), "Failed to fetch notifications: " + response.message());
            }

            List<ApiNotification> apiNotifications = response.body();
            if (apiNotifications == null) {
                throw new ApiException(response.code(), "Empty response from API");
            }

            JSONArray notificationsArray = convertApiNotificationsToJson(apiNotifications);
            localCache.saveNotifications(notificationsArray);

            android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putLong(KEY_NOTIFICATIONS_LAST_SYNC, System.currentTimeMillis()).apply();

            Log.d(TAG, "Notifications synced: " + notificationsArray.length() + " notifications");
            return notificationsArray;

        } catch (IOException e) {
            Log.e(TAG, "Network error syncing notifications", e);
            throw new ApiException(0, "Network error: " + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, "Error converting notifications", e);
            throw new ApiException(0, "Error converting notifications: " + e.getMessage());
        }
    }

    private JSONArray convertApiNotificationsToJson(List<ApiNotification> apiNotifications) throws JSONException {
        JSONArray result = new JSONArray();

        for (ApiNotification apiNotif : apiNotifications) {
            if (apiNotif.getIsActive() != null && !apiNotif.getIsActive()) {
                continue;
            }

            JSONObject notificationObj = new JSONObject();
            notificationObj.put("id", apiNotif.getId());
            notificationObj.put("type", apiNotif.getType());

            JSONObject triggerObj = new JSONObject();
            String triggerType = apiNotif.getTriggerType();
            
            if ("STATION".equals(triggerType)) {
                triggerObj.put("type", "station");
                triggerObj.put("station_id", apiNotif.getTriggerStationId());
            } else if ("LINE".equals(triggerType)) {
                triggerObj.put("type", "line");
                triggerObj.put("line_id", apiNotif.getTriggerLineId());
            } else if ("DATE_RANGE".equals(triggerType)) {
                triggerObj.put("type", "date_range");
            } else {
                triggerObj.put("type", "once");
            }

            if (apiNotif.getTriggerDateStart() != null && apiNotif.getTriggerDateEnd() != null) {
                JSONObject dateRangeObj = new JSONObject();
                dateRangeObj.put("start", apiNotif.getTriggerDateStart());
                dateRangeObj.put("end", apiNotif.getTriggerDateEnd());
                triggerObj.put("date_range", dateRangeObj);
            }

            notificationObj.put("trigger", triggerObj);

            JSONObject contentObj = new JSONObject();
            if (apiNotif.getContentImageUrl() != null || apiNotif.getContentImageResource() != null) {
                if (apiNotif.getContentImageUrl() != null) {
                    contentObj.put("image_url", apiNotif.getContentImageUrl());
                }
                if (apiNotif.getContentImageResource() != null) {
                    contentObj.put("image_resource", apiNotif.getContentImageResource());
                }
                if (apiNotif.getContentCaption() != null) {
                    contentObj.put("caption", apiNotif.getContentCaption());
                }
            } else {
                contentObj.put("text", apiNotif.getContentText() != null ? apiNotif.getContentText() : "");
            }

            notificationObj.put("content", contentObj);
            result.put(notificationObj);
        }

        return result;
    }

    public JSONArray getNotificationsFromCache() {
        return localCache.getNotifications();
    }

    public long getLastSyncTime() {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_NOTIFICATIONS_LAST_SYNC, 0);
    }

    public boolean hasCachedNotifications() {
        return localCache.hasNotifications();
    }
}

