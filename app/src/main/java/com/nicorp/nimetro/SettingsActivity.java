package com.nicorp.nimetro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private TextView currentMetroMapName;
    private ImageView currentMetroMapIcon;
    private RadioGroup themeRadioGroup;
    private SharedPreferences sharedPreferences;
    private LinearLayout currentMetroMapLayout;
    private androidx.appcompat.widget.Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        currentMetroMapName = findViewById(R.id.currentMetroMapName);
        currentMetroMapIcon = findViewById(R.id.currentMetroMapIcon);
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        currentMetroMapLayout = findViewById(R.id.currentMetroMapLayout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(intent);});

        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);

        // Загрузка текущей выбранной карты метро
        String selectedMapFileName = sharedPreferences.getString("selected_map_file", "metromap_1.json");
        MetroMapItem currentMapItem = loadMetroMapItem(selectedMapFileName);
        if (currentMapItem != null) {
            currentMetroMapName.setText(currentMapItem.getName());
            Glide.with(this).load(currentMapItem.getIconUrl()).into(currentMetroMapIcon);
        }

        // Загрузка текущей выбранной темы
        String selectedTheme = sharedPreferences.getString("selected_theme", "light");
        if (selectedTheme.equals("light")) {
            themeRadioGroup.check(R.id.lightThemeRadioButton);
        } else {
            themeRadioGroup.check(R.id.darkThemeRadioButton);
        }

        // Установка слушателя для изменения темы
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String theme = checkedId == R.id.lightThemeRadioButton ? "light" : "dark";
            saveTheme(theme);
        });

        currentMetroMapLayout.setOnClickListener(v -> onCurrentMetroMapClick(v));
    }

    public void onCurrentMetroMapClick(View view) {
        // Открытие диалога для выбора карты метро
        SelectMetroMapDialog dialog = new SelectMetroMapDialog(this);
        dialog.show();
    }

    public void updateCurrentMetroMap(MetroMapItem selectedItem) {
        if (selectedItem != null) {
            currentMetroMapName.setText(selectedItem.getName());
            Glide.with(this).load(selectedItem.getIconUrl()).into(currentMetroMapIcon);

            // Сохранение выбранной карты метро в SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("selected_map", selectedItem.getName());
            editor.putString("selected_map_file", selectedItem.getFileName());
            editor.apply();
        }
    }

    private void saveTheme(String theme) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("selected_theme", theme);
        editor.apply();
    }

    private MetroMapItem loadMetroMapItem(String fileName) {
        try (InputStream is = getAssets().open("raw/" + fileName)) {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            String json = new String(buffer, "UTF-8");
            JSONObject jsonObject = new JSONObject(json);
            JSONObject infoObject = jsonObject.getJSONObject("info");
            String mapName = infoObject.getString("name");
            String country = infoObject.getString("country");
            String iconUrl = infoObject.getString("icon");
            return new MetroMapItem(country, mapName, iconUrl, fileName);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        // Возврат на предыдущую активность
        super.onBackPressed();
        Log.d("MainActivity", "Settings button clicked");
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void onBackButtonClick(View view) {
        // Возврат на предыдущую активность
        Log.d("MainActivity", "Settings button clicked");
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}