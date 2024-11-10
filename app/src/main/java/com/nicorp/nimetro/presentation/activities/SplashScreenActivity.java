package com.nicorp.nimetro.presentation.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.nicorp.nimetro.R;

import pl.droidsonroids.gif.GifImageView;

/**
 * Splash screen activity that displays a GIF animation and then transitions to the main activity.
 */
@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 5500; // Time to display the splash screen in milliseconds (5.5 seconds)

    /**
     * Called when the activity is first created. Initializes the splash screen and sets up the transition to the main activity.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        initializeTheme();
        initializeGifAnimation();
        setupTransitionToMainActivity();
    }

    /**
     * Initializes the application theme based on the selected theme in SharedPreferences.
     */
    private void initializeTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String selectedTheme = sharedPreferences.getString("selected_theme", "light");

        if (selectedTheme.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    /**
     * Initializes and displays the GIF animation.
     */
    @SuppressLint("ResourceType")
    private void initializeGifAnimation() {
        GifImageView gifImageView = findViewById(R.id.gifImageView);

        // Загружаем GIF-анимацию
        gifImageView.setImageResource(R.raw.splash_animation);
    }

    /**
     * Sets up the transition to the main activity after the splash screen duration.
     */
    private void setupTransitionToMainActivity() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, SPLASH_TIME_OUT);
    }
}