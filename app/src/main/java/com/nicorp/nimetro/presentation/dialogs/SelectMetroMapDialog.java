package com.nicorp.nimetro.presentation.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.MetroMapItem;
import com.nicorp.nimetro.presentation.activities.SettingsActivity;
import com.nicorp.nimetro.presentation.adapters.MetroMapAdapter;
import com.nicorp.nimetro.data.services.MapSyncService;
import com.nicorp.nimetro.data.exceptions.ApiException;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectMetroMapDialog extends Dialog {

    private static final String TAG = "SelectMetroMapDialog";
    private Context context;
    private RecyclerView metroMapRecyclerView;
    private EditText searchEditText;
    private MetroMapAdapter adapter;
    private List<MetroMapItem> metroMapItems;
    private Map<String, List<MetroMapItem>> groupedMetroMapItems;
    private MapSyncService mapSyncService;
    private ProgressBar progressBar;
    private TextView loadingText;

    public SelectMetroMapDialog(@NonNull Context context) {
        super(context, R.style.FullScreenDialogStyle);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_select_metro_map);

        metroMapRecyclerView = findViewById(R.id.metroMapRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        progressBar = null;
        loadingText = null;

        mapSyncService = new MapSyncService(context);

        showLoading(true);
        
        new Thread(() -> {
            try {
                List<MetroMapItem> items = loadMetroMapItemsFromApi();
                if (items == null || items.isEmpty()) {
                    items = loadMetroMapItemsFromCache();
                }
                
                if (items == null || items.isEmpty()) {
                    items = loadMetroMapItemsFromAssets();
                }
                
                final List<MetroMapItem> finalItems = items != null ? items : new ArrayList<>();
                
                runOnUiThread(() -> {
                    metroMapItems = finalItems;
                    groupedMetroMapItems = groupMetroMapItemsByCountry(metroMapItems);
                    
                    List<Object> adapterItems = new ArrayList<>();
                    for (Map.Entry<String, List<MetroMapItem>> entry : groupedMetroMapItems.entrySet()) {
                        adapterItems.add(entry.getKey());
                        adapterItems.addAll(entry.getValue());
                    }
                    
                    adapter = new MetroMapAdapter(context, adapterItems);
                    metroMapRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                    metroMapRecyclerView.setAdapter(adapter);
                    
                    adapter.setOnItemClickListener((position, item) -> {
                        if (item != null) {
                            ((SettingsActivity) context).updateCurrentMetroMap(item);
                            downloadMapIfNeeded(item);
                            dismiss();
                        }
                    });
                    
                    showLoading(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading metro map items", e);
                runOnUiThread(() -> {
                    List<MetroMapItem> fallbackItems = loadMetroMapItemsFromAssets();
                    metroMapItems = fallbackItems != null ? fallbackItems : new ArrayList<>();
                    groupedMetroMapItems = groupMetroMapItemsByCountry(metroMapItems);
                    
                    List<Object> adapterItems = new ArrayList<>();
                    for (Map.Entry<String, List<MetroMapItem>> entry : groupedMetroMapItems.entrySet()) {
                        adapterItems.add(entry.getKey());
                        adapterItems.addAll(entry.getValue());
                    }
                    
                    adapter = new MetroMapAdapter(context, adapterItems);
                    metroMapRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                    metroMapRecyclerView.setAdapter(adapter);
                    
                    adapter.setOnItemClickListener((position, item) -> {
                        if (item != null) {
                            ((SettingsActivity) context).updateCurrentMetroMap(item);
                            downloadMapIfNeeded(item);
                            dismiss();
                        }
                    });
                    
                    showLoading(false);
                });
            }
        }).start();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMetroMaps(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (loadingText != null) {
            loadingText.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (metroMapRecyclerView != null) {
            metroMapRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private List<MetroMapItem> loadMetroMapItemsFromApi() {
        try {
            if (mapSyncService.isOnline()) {
                List<MetroMapItem> items = mapSyncService.syncMapsList();
                if (items != null && !items.isEmpty()) {
                    saveMetroMapItemsToCache(items);
                    return items;
                }
            }
        } catch (ApiException e) {
            Log.e(TAG, "Failed to load maps from API", e);
        } catch (Exception e) {
            Log.e(TAG, "Error loading maps from API", e);
        }
        return null;
    }

    private List<MetroMapItem> loadMetroMapItemsFromCache() {
        try {
            List<MetroMapItem> items = mapSyncService.getMapsListFromCache();
            if (items != null && !items.isEmpty()) {
                return items;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading maps from cache", e);
        }
        return null;
    }

    private void saveMetroMapItemsToCache(List<MetroMapItem> items) {
        try {
            mapSyncService.getMapsListFromCache();
        } catch (Exception e) {
            Log.e(TAG, "Error saving maps to cache", e);
        }
    }

    private List<MetroMapItem> loadMetroMapItemsFromAssets() {
        List<MetroMapItem> metroMapItems = new ArrayList<>();

        try {
            String[] jsonFiles = context.getAssets().list("raw");
            if (jsonFiles == null) {
                return metroMapItems;
            }
            
            for (String fileName : jsonFiles) {
                if (fileName.startsWith("metromap_") && fileName.endsWith(".json")) {
                    try (InputStream is = context.getAssets().open("raw/" + fileName)) {
                        int size = is.available();
                        byte[] buffer = new byte[size];
                        is.read(buffer);
                        String json = new String(buffer, "UTF-8");
                        JSONObject jsonObject = new JSONObject(json);
                        JSONObject infoObject = jsonObject.getJSONObject("info");
                        String mapName = infoObject.getString("name");
                        String country = infoObject.getString("country");
                        String iconUrl = infoObject.optString("icon", null);

                        MetroMapItem metroMapItem = new MetroMapItem(country, mapName, iconUrl, fileName);
                        metroMapItems.add(metroMapItem);
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Error loading map from assets: " + fileName, e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing assets", e);
        }

        return metroMapItems;
    }

    private void runOnUiThread(Runnable runnable) {
        ((android.app.Activity) context).runOnUiThread(runnable);
    }

    private void downloadMapIfNeeded(MetroMapItem item) {
        new Thread(() -> {
            try {
                String mapId = item.getFileName().replace(".json", "");
                com.nicorp.nimetro.data.repositories.LocalMapCache cache = new com.nicorp.nimetro.data.repositories.LocalMapCache(context);
                
                if (!cache.hasMap(mapId) && mapSyncService.isOnline()) {
                    try {
                        mapSyncService.downloadMapByFileName(item.getFileName());
                    } catch (ApiException e) {
                        Log.e(TAG, "Failed to download map: " + item.getFileName(), e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error downloading map", e);
            }
        }).start();
    }

    private Map<String, List<MetroMapItem>> groupMetroMapItemsByCountry(List<MetroMapItem> metroMapItems) {
        Map<String, List<MetroMapItem>> groupedItems = new HashMap<>();

        for (MetroMapItem item : metroMapItems) {
            String country = item.getCountry();
            if (!groupedItems.containsKey(country)) {
                groupedItems.put(country, new ArrayList<>());
            }
            groupedItems.get(country).add(item);
        }

        return groupedItems;
    }

    private void filterMetroMaps(String query) {
        List<MetroMapItem> filteredItems = new ArrayList<>();

        for (MetroMapItem item : metroMapItems) {
            if (item.getName().toLowerCase().contains(query.toLowerCase()) || item.getCountry().toLowerCase().contains(query.toLowerCase())) {
                filteredItems.add(item);
            }
        }

        groupedMetroMapItems = groupMetroMapItemsByCountry(filteredItems);
        List<Object> items = new ArrayList<>();
        for (Map.Entry<String, List<MetroMapItem>> entry : groupedMetroMapItems.entrySet()) {
            items.add(entry.getKey());
            items.addAll(entry.getValue());
        }
        adapter.updateData(items);
    }
}