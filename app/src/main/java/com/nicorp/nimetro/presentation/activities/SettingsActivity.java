package com.nicorp.nimetro.presentation.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.MetroMapItem;
import com.nicorp.nimetro.presentation.dialogs.SelectMetroMapDialog;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;

/**
 * Activity responsible for managing application settings, including metro map selection and theme settings.
 */
public class SettingsActivity extends AppCompatActivity {

    private TextView currentMetroMapName;
    private ImageView currentMetroMapIcon;
    private RadioGroup themeRadioGroup;
    private SharedPreferences sharedPreferences;
    private LinearLayout currentMetroMapLayout;
    private androidx.appcompat.widget.Toolbar toolbar;

    /**
     * Called when the activity is first created. Initializes the UI components and sets up event listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeUIComponents();
        setupToolbar();
        loadCurrentSettings();
        setupEventListeners();
    }

    /**
     * Initializes the UI components.
     */
    private void initializeUIComponents() {
        currentMetroMapName = findViewById(R.id.currentMetroMapName);
        currentMetroMapIcon = findViewById(R.id.currentMetroMapIcon);
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        currentMetroMapLayout = findViewById(R.id.currentMetroMapLayout);
    }

    /**
     * Sets up the toolbar with a back button.
     */
    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> navigateToMainActivity());
    }

    /**
     * Loads the current settings from SharedPreferences and updates the UI.
     */
    private void loadCurrentSettings() {
        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);

        String selectedMapFileName = sharedPreferences.getString("selected_map_file", "metromap_1.json");
        MetroMapItem currentMapItem = loadMetroMapItem(selectedMapFileName);
        if (currentMapItem != null) {
            updateCurrentMetroMap(currentMapItem);
        }

        String selectedTheme = sharedPreferences.getString("selected_theme", "light");
        setTheme(selectedTheme);
    }

    /**
     * Sets up event listeners for UI components.
     */
    private void setupEventListeners() {
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String theme = checkedId == R.id.lightThemeRadioButton ? "light" : "dark";
            saveTheme(theme);
        });

        currentMetroMapLayout.setOnClickListener(v -> onCurrentMetroMapClick(v));
    }

    /**
     * Handles the click event on the current metro map layout.
     *
     * @param view The view that was clicked.
     */
    public void onCurrentMetroMapClick(View view) {
        SelectMetroMapDialog dialog = new SelectMetroMapDialog(this);
        dialog.show();
    }

    /**
     * Updates the current metro map display with the selected metro map item.
     *
     * @param selectedItem The selected metro map item.
     */
    public void updateCurrentMetroMap(MetroMapItem selectedItem) {
        if (selectedItem != null) {
            currentMetroMapName.setText(selectedItem.getName());
            Glide.with(this).load(selectedItem.getIconUrl()).into(currentMetroMapIcon);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("selected_map", selectedItem.getName());
            editor.putString("selected_map_file", selectedItem.getFileName());
            editor.apply();
        }
    }

    /**
     * Saves the selected theme to SharedPreferences and updates the application theme.
     *
     * @param theme The selected theme ("light" or "dark").
     */
    private void saveTheme(String theme) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        setTheme(theme);
        editor.putString("selected_theme", theme);
        editor.apply();
    }

    /**
     * Sets the application theme based on the selected theme.
     *
     * @param theme The selected theme ("light" or "dark").
     */
    private void setTheme(String theme) {
        if (theme.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            themeRadioGroup.check(R.id.lightThemeRadioButton);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            themeRadioGroup.check(R.id.darkThemeRadioButton);
        }
    }

    /**
     * Loads the metro map item from the specified file.
     *
     * @param fileName The name of the file containing the metro map data.
     * @return The loaded metro map item, or null if an error occurred.
     */
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

    /**
     * Handles the back button press to navigate back to the main activity.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToMainActivity();
    }

    /**
     * Handles the back button click to navigate back to the main activity.
     *
     * @param view The view that was clicked.
     */
    public void onBackButtonClick(View view) {
        navigateToMainActivity();
    }

    /**
     * Navigates to the main activity and finishes the current activity.
     */
    private void navigateToMainActivity() {
        Log.d("MainActivity", "Settings button clicked");
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}