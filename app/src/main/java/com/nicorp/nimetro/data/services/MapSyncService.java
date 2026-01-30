package com.nicorp.nimetro.data.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.nicorp.nimetro.data.api.ApiClient;
import com.nicorp.nimetro.data.api.ApiService;
import com.nicorp.nimetro.data.api.dto.ApiMapData;
import com.nicorp.nimetro.data.api.dto.ApiMapItem;
import com.nicorp.nimetro.data.exceptions.ApiException;
import com.nicorp.nimetro.data.repositories.LocalMapCache;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Response;

public class MapSyncService {
    private static final String TAG = "MapSyncService";
    private static final String PREFS_NAME = "map_sync_prefs";
    private static final String KEY_MAPS_LAST_SYNC = "maps_last_sync";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    private final Context context;
    private final ApiService apiService;
    private final LocalMapCache localMapCache;
    private final Gson gson;
    private final SharedPreferences prefs;

    public MapSyncService(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiClient.getInstance(context).getApiService();
        this.localMapCache = new LocalMapCache(context);
        this.gson = new Gson();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public List<com.nicorp.nimetro.domain.entities.MetroMapItem> syncMapsList() throws ApiException {
        if (!isOnline()) {
            Log.w(TAG, "No internet connection, cannot sync maps list");
            throw new ApiException(0, "No internet connection");
        }

        try {
            Call<List<ApiMapItem>> call = apiService.getMaps();
            Response<List<ApiMapItem>> response = call.execute();

            if (!response.isSuccessful()) {
                throw new ApiException(response.code(), "Failed to fetch maps: " + response.message());
            }

            List<ApiMapItem> apiMaps = response.body();
            if (apiMaps == null) {
                throw new ApiException(response.code(), "Empty response from API");
            }

            // Сохраняем timestamp синхронизации
            prefs.edit().putLong(KEY_MAPS_LAST_SYNC, System.currentTimeMillis()).apply();

            // Конвертируем ApiMapItem в MetroMapItem
            List<com.nicorp.nimetro.domain.entities.MetroMapItem> metroMapItems = new ArrayList<>();
            for (ApiMapItem apiItem : apiMaps) {
                com.nicorp.nimetro.domain.entities.MetroMapItem item = new com.nicorp.nimetro.domain.entities.MetroMapItem(
                    apiItem.getCountry(),
                    apiItem.getName(),
                    apiItem.getIconUrl(),
                    apiItem.getFileName()
                );
                metroMapItems.add(item);
                
                if (apiItem.getUpdatedAt() != null) {
                    mapsUpdatedAtCache.put(apiItem.getFileName(), apiItem.getUpdatedAt());
                }
            }

            // Сохраняем список в SharedPreferences для быстрого доступа
            saveMapsListToCache(apiMaps, metroMapItems);

            Log.d(TAG, "Maps list synced: " + metroMapItems.size() + " maps");
            return metroMapItems;

        } catch (IOException e) {
            Log.e(TAG, "Network error syncing maps list", e);
            throw new ApiException(0, "Network error: " + e.getMessage());
        }
    }

    public JSONObject downloadMap(String mapId) throws ApiException {
        if (!isOnline()) {
            Log.w(TAG, "No internet connection, cannot download map");
            throw new ApiException(0, "No internet connection");
        }

        try {
            UUID uuid = UUID.fromString(mapId);
            Call<ApiMapData> call = apiService.getMapById(uuid);
            Response<ApiMapData> response = call.execute();

            if (!response.isSuccessful()) {
                throw new ApiException(response.code(), "Failed to fetch map: " + response.message());
            }

            ApiMapData apiMap = response.body();
            if (apiMap == null || apiMap.getData() == null) {
                throw new ApiException(response.code(), "Empty map data from API");
            }

            // Конвертируем Map<String, Object> в JSONObject
            JsonElement jsonElement = gson.toJsonTree(apiMap.getData());
            String jsonString = gson.toJson(jsonElement);
            JSONObject mapData = new JSONObject(jsonString);

            // Сохраняем в локальный кеш
            localMapCache.saveMap(mapId, mapData);

            Log.d(TAG, "Map downloaded and cached: " + mapId);
            return mapData;

        } catch (IOException e) {
            Log.e(TAG, "Network error downloading map", e);
            throw new ApiException(0, "Network error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error downloading map", e);
            throw new ApiException(0, "Error: " + e.getMessage());
        }
    }

    public JSONObject downloadMapByFileName(String fileName) throws ApiException {
        if (!isOnline()) {
            Log.w(TAG, "No internet connection, cannot download map");
            throw new ApiException(0, "No internet connection");
        }

        try {
            Call<ApiMapData> call = apiService.getMapByFileName(fileName);
            Response<ApiMapData> response = call.execute();

            if (!response.isSuccessful()) {
                throw new ApiException(response.code(), "Failed to fetch map: " + response.message());
            }

            ApiMapData apiMap = response.body();
            if (apiMap == null || apiMap.getData() == null || apiMap.getId() == null) {
                throw new ApiException(response.code(), "Empty map data from API");
            }

            String mapId = apiMap.getId().toString();
            
            // Конвертируем Map<String, Object> в JSONObject
            JsonElement jsonElement = gson.toJsonTree(apiMap.getData());
            String jsonString = gson.toJson(jsonElement);
            JSONObject mapData = new JSONObject(jsonString);

            // Сохраняем в локальный кеш по mapId
            localMapCache.saveMap(mapId, mapData);

            Log.d(TAG, "Map downloaded and cached by fileName: " + fileName + " (id: " + mapId + ")");
            return mapData;

        } catch (IOException e) {
            Log.e(TAG, "Network error downloading map", e);
            throw new ApiException(0, "Network error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error downloading map", e);
            throw new ApiException(0, "Error: " + e.getMessage());
        }
    }

    public boolean updateMapIfNeeded(String mapId, String serverUpdatedAt) {
        if (!isOnline()) {
            Log.d(TAG, "No internet connection, skipping update check");
            return false;
        }

        try {
            // Парсим дату обновления с сервера
            Date serverDate = parseDate(serverUpdatedAt);
            if (serverDate == null) {
                Log.w(TAG, "Could not parse server date: " + serverUpdatedAt);
                return false;
            }

            // Получаем локальную дату обновления
            long localLastModified = localMapCache.getMapLastModified(mapId);
            Date localDate = new Date(localLastModified);

            // Если серверная версия новее - обновляем
            if (serverDate.after(localDate)) {
                Log.d(TAG, "Map update available: " + mapId);
                downloadMap(mapId);
                return true;
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking map update", e);
            return false;
        }
    }

    private Date parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        try {
            // Пробуем разные форматы
            SimpleDateFormat[] formats = {
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
            };

            for (SimpleDateFormat format : formats) {
                try {
                    return format.parse(dateString);
                } catch (ParseException e) {
                    // Пробуем следующий формат
                }
            }

            Log.w(TAG, "Could not parse date: " + dateString);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
            return null;
        }
    }

    public long getLastSyncTime() {
        return prefs.getLong(KEY_MAPS_LAST_SYNC, 0);
    }

    private Map<String, String> mapsUpdatedAtCache = new HashMap<>();

    private void saveMapsListToCache(List<ApiMapItem> apiItems, List<com.nicorp.nimetro.domain.entities.MetroMapItem> items) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < items.size(); i++) {
                com.nicorp.nimetro.domain.entities.MetroMapItem item = items.get(i);
                JSONObject jsonItem = new JSONObject();
                jsonItem.put("country", item.getCountry());
                jsonItem.put("name", item.getName());
                jsonItem.put("iconUrl", item.getIconUrl());
                jsonItem.put("fileName", item.getFileName());
                
                if (i < apiItems.size()) {
                    ApiMapItem apiItem = apiItems.get(i);
                    if (apiItem.getUpdatedAt() != null) {
                        jsonItem.put("updatedAt", apiItem.getUpdatedAt());
                        mapsUpdatedAtCache.put(item.getFileName(), apiItem.getUpdatedAt());
                    }
                }
                
                jsonArray.put(jsonItem);
            }

            JSONObject wrapper = new JSONObject();
            wrapper.put("maps", jsonArray);
            wrapper.put("cached_at", System.currentTimeMillis());

            String jsonString = wrapper.toString();
            File cacheFile = new File(context.getFilesDir(), "maps_list_cache.json");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.close();

        } catch (Exception e) {
            Log.e(TAG, "Error saving maps list to cache", e);
        }
    }

    public String getMapUpdatedAt(String fileName) {
        if (mapsUpdatedAtCache.containsKey(fileName)) {
            return mapsUpdatedAtCache.get(fileName);
        }
        
        try {
            File cacheFile = new File(context.getFilesDir(), "maps_list_cache.json");
            if (!cacheFile.exists()) {
                return null;
            }

            FileInputStream fis = new FileInputStream(cacheFile);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONObject wrapper = new JSONObject(jsonString);
            JSONArray jsonArray = wrapper.getJSONArray("maps");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonItem = jsonArray.getJSONObject(i);
                if (fileName.equals(jsonItem.optString("fileName", null))) {
                    String updatedAt = jsonItem.optString("updatedAt", null);
                    if (updatedAt != null) {
                        mapsUpdatedAtCache.put(fileName, updatedAt);
                        return updatedAt;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting map updatedAt from cache", e);
        }
        
        return null;
    }

    public List<com.nicorp.nimetro.domain.entities.MetroMapItem> getMapsListFromCache() {
        try {
            File cacheFile = new File(context.getFilesDir(), "maps_list_cache.json");
            if (!cacheFile.exists()) {
                return null;
            }

            FileInputStream fis = new FileInputStream(cacheFile);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONObject wrapper = new JSONObject(jsonString);
            JSONArray jsonArray = wrapper.getJSONArray("maps");

            List<com.nicorp.nimetro.domain.entities.MetroMapItem> items = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonItem = jsonArray.getJSONObject(i);
                com.nicorp.nimetro.domain.entities.MetroMapItem item = new com.nicorp.nimetro.domain.entities.MetroMapItem(
                    jsonItem.getString("country"),
                    jsonItem.getString("name"),
                    jsonItem.optString("iconUrl", null),
                    jsonItem.getString("fileName")
                );
                items.add(item);
                
                String updatedAt = jsonItem.optString("updatedAt", null);
                if (updatedAt != null) {
                    mapsUpdatedAtCache.put(item.getFileName(), updatedAt);
                }
            }

            return items;
        } catch (Exception e) {
            Log.e(TAG, "Error loading maps list from cache", e);
            return null;
        }
    }

    public com.nicorp.nimetro.domain.entities.MetroMapItem findMoscowMetro(List<com.nicorp.nimetro.domain.entities.MetroMapItem> items, String defaultCountry, String defaultNameContains) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        for (com.nicorp.nimetro.domain.entities.MetroMapItem item : items) {
            if (defaultCountry != null && defaultCountry.equals(item.getCountry())) {
                if (defaultNameContains != null && item.getName() != null && 
                    item.getName().toLowerCase().contains(defaultNameContains.toLowerCase())) {
                    return item;
                }
            }
        }

        // Если не нашли по точному совпадению, возвращаем первый российский
        for (com.nicorp.nimetro.domain.entities.MetroMapItem item : items) {
            if ("Россия".equals(item.getCountry()) || "Russia".equals(item.getCountry())) {
                return item;
            }
        }

        // Иначе возвращаем первый элемент
        return items.get(0);
    }
}

