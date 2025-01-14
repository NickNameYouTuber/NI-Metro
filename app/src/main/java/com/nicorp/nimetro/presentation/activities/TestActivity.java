//package com.nicorp.nimetro.presentation.activities;
//
//import android.graphics.Color;
//import android.graphics.PointF;
//import android.os.Bundle;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.nicorp.nimetro.R;
//import com.nicorp.nimetro.presentation.views.AnimatedPathMapView;
//
//import java.util.Arrays;
//import java.util.List;
//
//public class TestActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_test);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        AnimatedPathMapView mapView = findViewById(R.id.mapView);
//        mapView.setSvgDrawable(R.drawable.cross_1_1);
//
//// Вариант 1: массив координат
//        float[] pathPoints = new float[] {
//                200f, 200f,
//                200f, 150f,
//                220f, 120f
//        };
//        mapView.setPath(pathPoints);
//
//// Вариант 2: список точек
//        List<PointF> points = Arrays.asList(
//                new PointF(790f, 1050f),
//                new PointF(720f, 910f),
//                new PointF(1030f, 750f),
//                new PointF(960f, 610f),
//                new PointF(920f, 600f),
//                new PointF(930f, 570f),
//                new PointF(270f, 180f)
//        );
//        mapView.setPath(points);
//
//// Настройка внешнего вида
//        mapView.setPathColor(Color.BLUE);
//        mapView.setStrokeWidth(10f);
//        mapView.setDashIntervals(50f, 30f);
//        mapView.setAnimationDuration(1000); // 2 секунды на цикл
//    }
//}