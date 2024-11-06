package com.nicorp.nimetro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import pl.droidsonroids.gif.GifImageView;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 5500; // Время отображения анимации в миллисекундах (4 секунды)

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        SharedPreferences sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String selectedTheme = sharedPreferences.getString("selected_theme", "light");

        if (selectedTheme.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        GifImageView gifImageView = findViewById(R.id.gifImageView);

        // Загружаем GIF-анимацию
        gifImageView.setImageResource(R.raw.splash_animation);

        // Создаем обработчик для перехода на главную страницу после завершения анимации
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, SPLASH_TIME_OUT);
    }
}