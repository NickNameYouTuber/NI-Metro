package com.nicorp.nimetro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private Spinner metroMapSpinner;
    private Button saveSettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        metroMapSpinner = findViewById(R.id.metroMapSpinner);
        saveSettingsButton = findViewById(R.id.saveSettingsButton);

        // Загрузка имен карт метро из JSON-файлов
        List<MetroMapItem> metroMapItems = null;
        try {
            metroMapItems = loadMetroMapItems();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Настройка Spinner
        MetroMapAdapter adapter = new MetroMapAdapter(this, metroMapItems);
        metroMapSpinner.setAdapter(adapter);

        saveSettingsButton.setOnClickListener(v -> {
            // Получение выбранной карты метро
            MetroMapItem selectedMapItem = (MetroMapItem) metroMapSpinner.getSelectedItem();

            // Получение выбранной темы
            RadioGroup themeRadioGroup = findViewById(R.id.themeRadioGroup);
            int selectedThemeId = themeRadioGroup.getCheckedRadioButtonId();
            String selectedTheme = selectedThemeId == R.id.lightThemeRadioButton ? "light" : "dark";

            // Сохранение настроек (например, в SharedPreferences)
            SharedPreferences sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("selected_map", selectedMapItem.getName());
            editor.putString("selected_theme", selectedTheme);

            // Сохранение названия файла карты
            editor.putString("selected_map_file", selectedMapItem.getFileName());

            editor.apply();

            // Возврат на предыдущую активность
            Log.d("MainActivity", "Settings button clicked");
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private List<MetroMapItem> loadMetroMapItems() throws IOException {
        List<MetroMapItem> metroMapItems = new ArrayList<>();
        AssetManager assetManager = getAssets();
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