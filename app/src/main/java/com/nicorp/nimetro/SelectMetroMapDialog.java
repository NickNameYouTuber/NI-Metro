package com.nicorp.nimetro;

import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SelectMetroMapDialog extends Dialog {

    private Context context;
    private ListView metroMapListView;
    private EditText searchEditText;
    private MetroMapAdapter adapter;
    private List<MetroMapItem> metroMapItems;

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
            metroMapItems = loadMetroMapItems();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        adapter = new MetroMapAdapter(context, metroMapItems);
        metroMapListView.setAdapter(adapter);

        metroMapListView.setOnItemClickListener((parent, view, position, id) -> {
            MetroMapItem selectedItem = adapter.getItem(position);
            if (selectedItem != null) {
                // Обновление текущей выбранной карты метро
                ((SettingsActivity) context).updateCurrentMetroMap(selectedItem);
                dismiss();
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private List<MetroMapItem> loadMetroMapItems() throws IOException {
        List<MetroMapItem> metroMapItems = new ArrayList<>();
        AssetManager assetManager = context.getAssets();
        String[] jsonFiles = assetManager.list("raw"); // Получаем список файлов в папке assets/raw

        if (jsonFiles != null) {
            for (String fileName : jsonFiles) {
                if (fileName.startsWith("metromap_") && fileName.endsWith(".json")) {
                    try (InputStream is = assetManager.open("raw/" + fileName)) {
                        int size = is.available();
                        byte[] buffer = new byte[size];
                        is.read(buffer);

                        String json = new String(buffer, "UTF-8");
                        JSONObject jsonObject = new JSONObject(json);
                        JSONObject infoObject = jsonObject.getJSONObject("info");
                        String mapName = infoObject.getString("name");
                        String country = infoObject.getString("country");
                        String iconUrl = infoObject.getString("icon");

                        metroMapItems.add(new MetroMapItem(country, mapName, iconUrl, fileName));
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return metroMapItems;
    }
}