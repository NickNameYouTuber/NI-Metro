package com.nicorp.nimetro;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ExpandableListView;
import androidx.annotation.NonNull;
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
    private ExpandableListView metroMapListView;
    private EditText searchEditText;
    private MetroMapExpandableListAdapter adapter;
    private List<MetroMapGroup> metroMapGroups;

    public SelectMetroMapDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_select_metro_map);

        metroMapListView = findViewById(R.id.metroMapListView);
        searchEditText = findViewById(R.id.searchEditText);

        try {
            metroMapGroups = loadMetroMapGroups();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        adapter = new MetroMapExpandableListAdapter(context, metroMapGroups);
        metroMapListView.setAdapter(adapter);

        metroMapListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            MetroMapItem selectedItem = (MetroMapItem) adapter.getChild(groupPosition, childPosition);
            if (selectedItem != null) {
                // Обновление текущей выбранной карты метро
                ((SettingsActivity) context).updateCurrentMetroMap(selectedItem);
                dismiss();
            }
            return false;
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

    private List<MetroMapGroup> loadMetroMapGroups() throws IOException {
        List<MetroMapGroup> metroMapGroups = new ArrayList<>();
        Map<String, List<MetroMapItem>> countryMap = new HashMap<>();

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
                    countryMap.computeIfAbsent(country, k -> new ArrayList<>()).add(metroMapItem);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        for (Map.Entry<String, List<MetroMapItem>> entry : countryMap.entrySet()) {
            metroMapGroups.add(new MetroMapGroup(entry.getKey(), entry.getValue()));
        }

        return metroMapGroups;
    }

    private void filterMetroMaps(String query) {
        List<MetroMapGroup> filteredGroups = new ArrayList<>();

        for (MetroMapGroup group : metroMapGroups) {
            List<MetroMapItem> filteredItems = new ArrayList<>();
            for (MetroMapItem item : group.getMetroMapItems()) {
                if (item.getName().toLowerCase().contains(query.toLowerCase()) || item.getCountry().toLowerCase().contains(query.toLowerCase())) {
                    filteredItems.add(item);
                }
            }
            if (!filteredItems.isEmpty()) {
                filteredGroups.add(new MetroMapGroup(group.getCountry(), filteredItems));
            }
        }

        adapter = new MetroMapExpandableListAdapter(context, filteredGroups);
        metroMapListView.setAdapter(adapter);
    }
}