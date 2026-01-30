package com.nicorp.nimetro.data.repositories;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LocalMapCache {
    private static final String TAG = "LocalMapCache";
    private static final String MAPS_DIR = "maps";
    private final Context context;
    private final File mapsDirectory;
    private final Gson gson;

    public LocalMapCache(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        
        // Создаем директорию для кеша карт
        mapsDirectory = new File(this.context.getFilesDir(), MAPS_DIR);
        if (!mapsDirectory.exists()) {
            boolean created = mapsDirectory.mkdirs();
            if (!created) {
                Log.e(TAG, "Failed to create maps directory");
            }
        }
    }

    public boolean saveMap(String mapId, JSONObject mapData) {
        if (mapId == null || mapData == null) {
            Log.e(TAG, "Cannot save map: mapId or mapData is null");
            return false;
        }

        try {
            File mapFile = new File(mapsDirectory, mapId + ".json");
            String jsonString = mapData.toString();
            
            FileOutputStream fos = new FileOutputStream(mapFile);
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.close();
            
            Log.d(TAG, "Map saved: " + mapId);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving map: " + mapId, e);
            return false;
        }
    }

    public JSONObject getMap(String mapId) {
        if (mapId == null) {
            return null;
        }

        try {
            File mapFile = new File(mapsDirectory, mapId + ".json");
            if (!mapFile.exists()) {
                Log.d(TAG, "Map file not found: " + mapId);
                return null;
            }

            FileInputStream fis = new FileInputStream(mapFile);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            return new JSONObject(jsonString);
        } catch (Exception e) {
            Log.e(TAG, "Error loading map: " + mapId, e);
            return null;
        }
    }

    public boolean hasMap(String mapId) {
        if (mapId == null) {
            return false;
        }
        File mapFile = new File(mapsDirectory, mapId + ".json");
        return mapFile.exists() && mapFile.length() > 0;
    }

    public boolean deleteMap(String mapId) {
        if (mapId == null) {
            return false;
        }

        try {
            File mapFile = new File(mapsDirectory, mapId + ".json");
            if (mapFile.exists()) {
                boolean deleted = mapFile.delete();
                Log.d(TAG, "Map deleted: " + mapId + " (" + deleted + ")");
                return deleted;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting map: " + mapId, e);
            return false;
        }
    }

    public List<String> getCachedMapIds() {
        List<String> mapIds = new ArrayList<>();
        
        try {
            File[] files = mapsDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        String fileName = file.getName();
                        String mapId = fileName.substring(0, fileName.length() - 5); // Убираем ".json"
                        mapIds.add(mapId);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting cached map IDs", e);
        }

        return mapIds;
    }

    public long getMapLastModified(String mapId) {
        if (mapId == null) {
            return 0;
        }

        File mapFile = new File(mapsDirectory, mapId + ".json");
        if (mapFile.exists()) {
            return mapFile.lastModified();
        }
        return 0;
    }

    public void clearAllMaps() {
        try {
            File[] files = mapsDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        file.delete();
                    }
                }
            }
            Log.d(TAG, "All maps cleared from cache");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing maps cache", e);
        }
    }
}

