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

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.MetroMapItem;
import com.nicorp.nimetro.presentation.activities.SettingsActivity;
import com.nicorp.nimetro.presentation.adapters.MetroMapAdapter;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectMetroMapDialog extends Dialog {

    private Context context;
    private RecyclerView metroMapRecyclerView;
    private EditText searchEditText;
    private MetroMapAdapter adapter;
    private List<MetroMapItem> metroMapItems;
    private Map<String, List<MetroMapItem>> groupedMetroMapItems;

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

        try {
            metroMapItems = loadMetroMapItems();
            groupedMetroMapItems = groupMetroMapItemsByCountry(metroMapItems);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<Object> items = new ArrayList<>();
        for (Map.Entry<String, List<MetroMapItem>> entry : groupedMetroMapItems.entrySet()) {
            items.add(entry.getKey());
            items.addAll(entry.getValue());
        }

        adapter = new MetroMapAdapter(context, items);
        metroMapRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        metroMapRecyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((position, item) -> {
            if (item != null) {
                // Обновление текущей выбранной карты метро
                ((SettingsActivity) context).updateCurrentMetroMap(item);
                dismiss();
            }
        });

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

    private List<MetroMapItem> loadMetroMapItems() throws IOException {
        List<MetroMapItem> metroMapItems = new ArrayList<>();

        String[] jsonFiles = context.getAssets().list("raw");
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
                    String iconUrl = infoObject.getString("icon");

                    MetroMapItem metroMapItem = new MetroMapItem(country, mapName, iconUrl, fileName);
                    metroMapItems.add(metroMapItem);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return metroMapItems;
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