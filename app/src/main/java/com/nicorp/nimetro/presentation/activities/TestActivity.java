package com.nicorp.nimetro.presentation.activities;

import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.presentation.views.AnimatedPathMapView;

import java.util.Arrays;
import java.util.List;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        AnimatedPathMapView mapView = findViewById(R.id.mapView);
        mapView.setSvgDrawable(R.drawable.cross_2_2);

        List<PointF> points = Arrays.asList(
                new PointF(700f, 450f),
                new PointF(700f, 400f),
                new PointF(600f, 400f),
                new PointF(600f, 800f),
                new PointF(700f, 800f),
                new PointF(700f, 750f)
        );
        mapView.setPath(points);

// Настройка внешнего вида
        mapView.setPathColor(Color.BLUE);
        mapView.setStrokeWidth(10f);
        mapView.setDashIntervals(50f, 30f);
        mapView.setAnimationDuration(1000); // 2 секунды на цикл
    }
}