package com.nicorp.nimetro.presentation.views;

import static com.nicorp.nimetro.presentation.activities.MainActivity.isMetroMap;
import static com.nicorp.nimetro.presentation.activities.MainActivity.isRiverTramMap;
import static com.nicorp.nimetro.presentation.activities.MainActivity.isSuburbanMap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;

import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;

import android.graphics.Typeface;
import android.graphics.Shader;

import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;

import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;


import androidx.core.graphics.ColorUtils;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.color.MaterialColors;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.models.MapObject;
import com.nicorp.nimetro.data.models.River;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Transfer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MetroMapView extends View {

    public enum MapType { SCHEME, SATELLITE }
    
    public enum LineRenderingMethod {
        FIXED_STROKE_WIDTH,    // Path с фиксированным strokeWidth
        POLYGONAL_OUTLINE,      // Полигональная обводка
        PARALLEL_PATHS          // Два параллельных Path с правильным смещением
    }
    
    private static final LineRenderingMethod LINE_RENDERING_METHOD = LineRenderingMethod.POLYGONAL_OUTLINE;
    
    private static final float LINE_WIDTH = 6f;
    private static final float DOUBLE_LINE_WIDTH = 4.5f;
    private static final float DOUBLE_LINE_GAP = 2.5f;
    private static final float BEZIER_SEGMENT_STEP = 0.01f;
    
    public Station selectedStation = null;
    private RectF visibleViewport = new RectF();
    private List<Line> lines;
    private List<Station> stations;
    private List<Station> route;
    private List<Station> selectedStations;
    private List<Transfer> transfers;
    private List<River> rivers;
    private List<MapObject> mapObjects;
    private List<Line> grayedLines;
    private List<Station> grayedStations;
    private List<Line> riverTramLines; // Список линий речного трамвая
    private List<Station> riverTramStations; // Список станций речного трамвая
    private List<Transfer> riverTramTransfers; // Список переходов речного трамвая
    private List<River> riverTramRivers; // Список рек для речного трамвая
    private List<MapObject> riverTramMapObjects; // Список объектов на карте речного трамвая
    private List<Line> suburbanLines; // Список линий речного трамвая
    private List<Station> suburbanStations; // Список станций речного трамвая
    private List<Transfer> suburbanTransfers; // Список переходов речного трамвая
    private List<River> suburbanRivers; // Список рек для речного трамвая
    private List<MapObject> suburbanMapObjects; // Список объектов на карте речного трамвая
    private List<Line> tramLines = new ArrayList<>(); // Список линий трамвая
    private List<Station> tramStations = new ArrayList<>(); // Список станций трамвая
    private List<Transfer> tramTransfers = new ArrayList<>(); // Список переходов трамвая
    private List<River> tramRivers = new ArrayList<>(); // Список рек для трамвая
    private List<MapObject> tramMapObjects = new ArrayList<>(); // Список объектов на карте трамвая

    private boolean isMetroMap = false;
    private boolean isSuburbanMap = false;
    private boolean isRiverTramMap = false;
    private boolean isTramMap = false;

    private Paint riverTramPaint; // Paint для отрисовки линий речного трамвая
    private List<LinePath> riverTramLinesPaths; // Кэш путей для линий речного трамвая
    private Paint linePaint;
    private Paint stationPaint;
    private Paint selectedStationPaint;
    private Paint routePaint;
    private Paint textPaint;
    private Paint whitePaint;
    private Paint transferPaint;
    private Paint stationCenterPaint;
    private Paint riverPaint;
    private Paint grayedPaint;
    private boolean isEditMode = false;
    private OnStationClickListener listener;
    public float scaleFactor = 1.0f;
    private GestureDetector gestureDetector;
    public ScaleGestureDetector scaleGestureDetector;
    public static final float COORDINATE_SCALE_FACTOR = 2.5f;
    private float currentCoordinateScaleFactor = COORDINATE_SCALE_FACTOR;
    
    private float getCoordinateScaleFactor() {
        return isTramMap ? COORDINATE_SCALE_FACTOR * 2.0f : COORDINATE_SCALE_FACTOR;
    }

    private float getAdjustedLineWidth(float baseWidth) {
        if (isTramMap) {
            return baseWidth / 5.0f;
        }
        return baseWidth;
    }

    private static final float CLICK_RADIUS = 30.0f;

    private static final float STATION_NAME_TEXT_SIZE = 22f;
    private Bitmap backgroundBitmap;

    private Bitmap schemeBackgroundBitmap;
    private Bitmap satelliteBackgroundBitmap;
    private MapType currentMapType = MapType.SCHEME;
    private Map<Float, Bitmap> cacheBitmaps = new HashMap<>();
    public boolean needsRedraw = true;
    private Matrix transformMatrix;
    private Bitmap cacheBitmap;
    private Bitmap bufferBitmap;
    private Canvas bufferCanvas;
    private List<PointF> transferConnectionPoints = new ArrayList<>();
    private boolean isSelectionBlocked = false; // Флаг для блокировки выбора станций


    // Theme colors
    private int mapBackgroundColor = Color.WHITE;
    private int mapTextColor = Color.BLACK;
    private int mapStationFillColor = Color.WHITE;
    private int mapTransferColor = Color.DKGRAY;
    private int mapTransferStrokeColor = Color.parseColor("#21212E");
    private int mapGrayedColor = Color.parseColor("#D9D9D9");
    private int mapRiverColor = Color.parseColor("#e3f2f9");
    private int mapRouteColor = Color.YELLOW;
    private int mapSelectedStationColor = Color.GREEN;

    // Конструкторы и инициализация
    public MetroMapView(Context context) {
        super(context);
        init();
    }

    public MetroMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MetroMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        updateThemeColors();
        initializePaints();
        initializeGestureDetectors();
        transformMatrix = new Matrix();
        if (isEditMode) {
            loadBackgroundBitmap();
        }

        // Центрируем карту после того, как View будет готова
        post(new Runnable() {
            @Override
            public void run() {
                if (stations != null && !stations.isEmpty()) {
                    centerMap();
                }
            }
        });
    }


    public void updateThemeColors() {
        Context context = getContext();
        if (context == null) return;

        TypedValue typedValue = new TypedValue();
        
        // Background color - для светлой темы используем белый с легким салатовым оттенком
        if (isDarkTheme()) {
            // Для темной темы используем цвет из темы и немного осветляем его
            if (context.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true)) {
                mapBackgroundColor = ColorUtils.blendARGB(typedValue.data, Color.WHITE, 0.08f);
            } else {
                int base = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.WHITE);
                mapBackgroundColor = ColorUtils.blendARGB(base, Color.WHITE, 0.08f);
            }
        } else {
            // Для светлой темы - белый с легким салатовым оттенком
            mapBackgroundColor = Color.parseColor("#FEFFFE");
        }

        // Text color (onBackground)
        mapTextColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnBackground, Color.BLACK);

        // Station fill color (surface)
        mapStationFillColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.WHITE);

        // Transfer color (outline)
        mapTransferColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline, Color.DKGRAY);
        mapTransferStrokeColor = mapTransferColor;

        // Grayed color - адаптивный серый
        int backgroundColor = mapBackgroundColor;
        int grayedBase = ColorUtils.blendARGB(backgroundColor, mapTextColor, 0.3f);
        mapGrayedColor = grayedBase;

        // River color - адаптивный голубой
        if (isDarkTheme()) {
            mapRiverColor = ColorUtils.blendARGB(mapBackgroundColor, Color.parseColor("#4A90E2"), 0.2f);
        } else {
            mapRiverColor = Color.parseColor("#e3f2f9");
        }

        // Route color - желтый для обеих тем
        mapRouteColor = Color.YELLOW;

        // Selected station color - зеленый для обеих тем
        mapSelectedStationColor = Color.GREEN;
    }

    private boolean isDarkTheme() {
        Context context = getContext();
        if (context == null) return false;
        
        // Проверяем через Configuration
        int nightMode = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            return true;
        }
        
        // Дополнительная проверка через цвет фона
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true)) {
            int backgroundColor = typedValue.data;
            double luminance = ColorUtils.calculateLuminance(backgroundColor);
            return luminance < 0.5;
        }
        
        return false;
    }

    private void initializePaints() {

        // Цвета должны быть обновлены перед вызовом этого метода
        // через updateThemeColors() в init() или onAttachedToWindow()
        
        riverTramPaint = new Paint();
        riverTramPaint.setColor(Color.BLUE); // Установите цвет для речного трамвая
        riverTramPaint.setStrokeWidth(10); // Установите ширину линии
        riverTramPaint.setStyle(Paint.Style.STROKE);

        linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(15);

        stationPaint = new Paint();
        stationPaint.setColor(Color.BLUE);
        stationPaint.setStyle(Paint.Style.STROKE);
        stationPaint.setStrokeWidth(7);

        selectedStationPaint = new Paint();

        selectedStationPaint.setColor(mapSelectedStationColor);
        selectedStationPaint.setStyle(Paint.Style.STROKE);
        selectedStationPaint.setStrokeWidth(5);

        whitePaint = new Paint();

        whitePaint.setColor(mapStationFillColor);
        whitePaint.setStyle(Paint.Style.FILL);

        routePaint = new Paint();

        routePaint.setColor(mapRouteColor);
        routePaint.setStrokeWidth(9);

        textPaint = new Paint();

        textPaint.setColor(mapTextColor);
        textPaint.setTextSize(STATION_NAME_TEXT_SIZE);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);

        transferPaint = new Paint();

        transferPaint.setColor(mapTransferColor);
        transferPaint.setStrokeWidth(6);
        transferPaint.setStyle(Paint.Style.STROKE);

        stationCenterPaint = new Paint();
        stationCenterPaint.setColor(Color.parseColor("#00000000"));
        stationCenterPaint.setStyle(Paint.Style.STROKE);
        stationCenterPaint.setStrokeWidth(7);

        riverPaint = new Paint();

        riverPaint.setColor(mapRiverColor);
        riverPaint.setStyle(Paint.Style.STROKE);
        riverPaint.setStrokeWidth(30);

        float riverCornerRadiusPx = 20f * getResources().getDisplayMetrics().density;
        riverPaint.setPathEffect(new CornerPathEffect(riverCornerRadiusPx));

        grayedPaint = new Paint();

        grayedPaint.setColor(mapGrayedColor);
        grayedPaint.setStrokeWidth(9);

    }

    private VelocityTracker velocityTracker;
    private ValueAnimator inertiaAnimator;
    public float translateX = 0f;
    public float translateY = 0f;
    private float velocityX = 0f;
    private float velocityY = 0f;
    private static final float FOLLOW_FACTOR = 0.85f;
    private static final float FRICTION = 0.9f; // Коэффициент трения
    private static final float MIN_VELOCITY = 0.1f; // Минимальная скорость для остановки

    private boolean isPanning = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        boolean result = scaleGestureDetector.onTouchEvent(event);
        result = gestureDetector.onTouchEvent(event) || result;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Останавливаем текущую анимацию инерции при новом касании
                if (inertiaAnimator != null && inertiaAnimator.isRunning()) {
                    inertiaAnimator.cancel();
                }

                isPanning = false;
                break;

            case MotionEvent.ACTION_UP:
                // Вычисляем финальную скорость
                velocityTracker.computeCurrentVelocity(1000); // в пикселях в секунду
                float finalVelocityX = velocityTracker.getXVelocity() * FOLLOW_FACTOR;
                float finalVelocityY = velocityTracker.getYVelocity() * FOLLOW_FACTOR;

                // Запускаем анимацию инерции
                startInertiaAnimation(finalVelocityX, finalVelocityY);

                // Если маршрут построен, игнорируем клики по станциям
                if (route != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                    return result || super.onTouchEvent(event);
                }


                // Если был жест прокрутки/свайпа — не обрабатываем клик по станции
                if (isPanning) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                    return result || super.onTouchEvent(event);
                }

                // Обработка клика по станции
                updateVisibleViewport();
                float[] point = new float[] {event.getX(), event.getY()};
                Matrix inverseMatrix = new Matrix();
                transformMatrix.invert(inverseMatrix);
                inverseMatrix.mapPoints(point);

                float x = point[0];
                float y = point[1];

                Station clickedStation = findStationAt(
                        x / currentCoordinateScaleFactor,
                        y / currentCoordinateScaleFactor);

                if (clickedStation != null && listener != null) {
                    // Если станция уже выбрана, сбрасываем выбор
                    if (selectedStation != null && selectedStation.equals(clickedStation)) {
                        selectedStation = null;
                    } else {
                        selectedStation = clickedStation;
                    }
                    listener.onStationClick(clickedStation);
                    centerOnStation(clickedStation); // Плавное перемещение к станции
                    velocityTracker.recycle();
                    velocityTracker = null;
                    needsRedraw = true;
                    invalidate();
                    return true;
                } else {
                    // Если нажали в другое место, сбрасываем выбор станции
                    if (selectedStation != null) {
                        selectedStation = null;
                        needsRedraw = true;
                        invalidate();
                    }
                }

                velocityTracker.recycle();
                velocityTracker = null;
                break;
        }

        return result || super.onTouchEvent(event);
    }

    private void startInertiaAnimation(float initialVelocityX, float initialVelocityY) {
        if (Math.abs(initialVelocityX) < MIN_VELOCITY && Math.abs(initialVelocityY) < MIN_VELOCITY) {
            return;
        }

        if (inertiaAnimator != null && inertiaAnimator.isRunning()) {
            inertiaAnimator.cancel();
        }

        velocityX = initialVelocityX;
        velocityY = initialVelocityY;

        inertiaAnimator = ValueAnimator.ofFloat(1f, 0f);
        inertiaAnimator.setDuration(1000); // Длительность затухания
        inertiaAnimator.setInterpolator(new LinearInterpolator());

        inertiaAnimator.addUpdateListener(animation -> {
            // Применяем трение
            velocityX *= FRICTION;
            velocityY *= FRICTION;

            // Обновляем позицию
            translateX += velocityX / scaleFactor / 60; // делим на 60 для нормализации скорости
            translateY += velocityY / scaleFactor / 60;

            // Останавливаем анимацию если скорость слишком мала
            if (Math.abs(velocityX) < MIN_VELOCITY && Math.abs(velocityY) < MIN_VELOCITY) {
                inertiaAnimator.cancel();
                return;
            }

            updateTransformMatrix();
            invalidate();
        });

        inertiaAnimator.start();
    }
    private void initializeGestureDetectors() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // Применяем коэффициент следования для более плавного движения
                velocityX = -distanceX * FOLLOW_FACTOR;
                velocityY = -distanceY * FOLLOW_FACTOR;

                // Обновляем позицию напрямую
                translateX += velocityX / scaleFactor;
                translateY += velocityY / scaleFactor;

                updateTransformMatrix();
                invalidate();

                isPanning = true;
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                // Сбрасываем скорость при новом касании
                velocityX = 0;
                velocityY = 0;
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 2.0f));
                updateTransformMatrix();
                needsRedraw = true;
                invalidate();
                return true;
            }
        });
    }

    public void setData(List<Line> metroLines, List<Station> metroStations, List<Transfer> metroTransfers, List<River> metroRivers, List<MapObject> metroMapObjects,
                        List<Line> suburbanLines, List<Station> suburbanStations, List<Transfer> suburbanTransfers, List<River> suburbanRivers, List<MapObject> suburbanMapObjects,
                        List<Line> riverTramLines, List<Station> riverTramStations, List<Transfer> riverTramTransfers, List<River> riverTramRivers, List<MapObject> riverTramMapObjects,
                        List<Line> tramLines, List<Station> tramStations, List<Transfer> tramTransfers, List<River> tramRivers, List<MapObject> tramMapObjects,
                        boolean isMetroMap, boolean isSuburbanMap, boolean isRiverTramMap, boolean isTramMap) {

        Log.d("MetroMapView", "setData called - isMetroMap: " + isMetroMap + ", isSuburbanMap: " + isSuburbanMap + ", isRiverTramMap: " + isRiverTramMap + ", isTramMap: " + isTramMap);
        Log.d("MetroMapView", "metroLines size: " + (metroLines != null ? metroLines.size() : "null"));
        Log.d("MetroMapView", "metroStations size: " + (metroStations != null ? metroStations.size() : "null"));
        
        // Инициализация всех списков, если они null
        this.lines = metroLines != null ? metroLines : Collections.emptyList();
        this.stations = metroStations != null ? metroStations : Collections.emptyList();
        this.transfers = metroTransfers != null ? metroTransfers : Collections.emptyList();
        this.rivers = metroRivers != null ? metroRivers : Collections.emptyList();
        this.mapObjects = metroMapObjects != null ? metroMapObjects : Collections.emptyList();
        this.suburbanLines = suburbanLines != null ? suburbanLines : Collections.emptyList();
        this.suburbanStations = suburbanStations != null ? suburbanStations : Collections.emptyList();
        this.suburbanTransfers = suburbanTransfers != null ? suburbanTransfers : Collections.emptyList();
        this.suburbanRivers = suburbanRivers != null ? suburbanRivers : Collections.emptyList();
        this.suburbanMapObjects = suburbanMapObjects != null ? suburbanMapObjects : Collections.emptyList();
        this.riverTramLines = riverTramLines != null ? riverTramLines : Collections.emptyList();
        this.riverTramStations = riverTramStations != null ? riverTramStations : Collections.emptyList();
        this.riverTramTransfers = riverTramTransfers != null ? riverTramTransfers : Collections.emptyList();
        this.riverTramRivers = riverTramRivers != null ? riverTramRivers : Collections.emptyList();
        this.riverTramMapObjects = riverTramMapObjects != null ? riverTramMapObjects : Collections.emptyList();
        this.tramLines = tramLines != null ? tramLines : Collections.emptyList();
        this.tramStations = tramStations != null ? tramStations : Collections.emptyList();
        this.tramTransfers = tramTransfers != null ? tramTransfers : Collections.emptyList();
        this.tramRivers = tramRivers != null ? tramRivers : Collections.emptyList();
        this.tramMapObjects = tramMapObjects != null ? tramMapObjects : Collections.emptyList();

        boolean previousIsTramMap = this.isTramMap;
        this.isMetroMap = isMetroMap;
        this.isSuburbanMap = isSuburbanMap;
        this.isRiverTramMap = isRiverTramMap;
        this.isTramMap = isTramMap;
        
        if (previousIsTramMap != isTramMap) {
            float targetScale = getCoordinateScaleFactor();
            animateCoordinateScaleWithMatrix(targetScale);
        } else {
            if (coordinateScaleAnimator == null || !coordinateScaleAnimator.isRunning()) {
                currentCoordinateScaleFactor = getCoordinateScaleFactor();
            }
        }
        
        Log.d("MetroMapView", "After assignment - this.lines size: " + this.lines.size());
        Log.d("MetroMapView", "After assignment - this.stations size: " + this.stations.size());

        // Очистка кэша
        pathCache = new MapPathCache();
        routePathCache = new RoutePathCache();

        // Очистка буфера
        if (bufferBitmap != null) {
            bufferBitmap.recycle();
            bufferBitmap = null;
        }


        // Сохраняем маршрут, чтобы не терять его при обновлении данных (например, при смене вида карты)
        List<Station> savedRoute = route;

        // Установка флага для перерисовки
        needsRedraw = true;

        // Обновление матрицы трансформации
        updateTransformMatrix();


        // Восстанавливаем маршрут и кэш маршрута
        if (savedRoute != null && !savedRoute.isEmpty()) {
            this.route = savedRoute;
            updateRouteCache();
        }

        // Запрос на перерисовку
        invalidate();
    }

    public void setData(List<Line> lines, List<Station> stations, List<Transfer> transfers,
                        List<River> rivers, List<MapObject> mapObjects,
                        List<Line> grayedLines, List<Station> grayedStations) {
        // Clear all path caches first
        pathCache = new MapPathCache();
        routePathCache = new RoutePathCache();

        // Clear all existing collections
        this.lines = new ArrayList<>(lines != null ? lines : Collections.emptyList());
        this.stations = new ArrayList<>(stations != null ? stations : Collections.emptyList());
        this.transfers = new ArrayList<>(transfers != null ? transfers : Collections.emptyList());
        this.rivers = new ArrayList<>(rivers != null ? rivers : Collections.emptyList());
        this.mapObjects = new ArrayList<>(mapObjects != null ? mapObjects : Collections.emptyList());
        this.grayedLines = new ArrayList<>(grayedLines != null ? grayedLines : Collections.emptyList());
        this.grayedStations = new ArrayList<>(grayedStations != null ? grayedStations : Collections.emptyList());

        // Clear the buffer bitmap
        if (bufferBitmap != null) {
            bufferBitmap.recycle();
            bufferBitmap = null;
        }


        // Не сбрасываем маршрут при обновлении данных

        // Force redraw
        needsRedraw = true;

        // Reset transformation if needed
        updateTransformMatrix();


        // Восстанавливаем кэш маршрута, если он есть
        if (route != null && !route.isEmpty()) {
            updateRouteCache();
        }

        // Request complete redraw
        invalidate();
    }

    public void setRoute(List<Station> route) {
        this.route = route;
        isSelectionBlocked = true; // Блокируем выбор станций
        updateRouteCache(); // Очищаем кэш перед обновлением
        needsRedraw = true;
        invalidate();

        if (route != null && route.size() > 1) {
            post(this::fitRouteToTopHalf);
        }
    }

    public void setMapType(MapType mapType) {
        if (mapType == null) return;
        this.currentMapType = mapType;
        // Не сбрасываем маршрут и затемнение — только перерисовка
        needsRedraw = true;
        invalidate();
    }

    public void setSelectedStations(List<Station> selectedStations) {
        this.selectedStations = selectedStations;
        needsRedraw = true;
        invalidate();
    }

    public void setOnStationClickListener(OnStationClickListener listener) {
        this.listener = listener;
    }

    public void clearRoute() {
        this.route = null; // Очищаем текущий маршрут
        isSelectionBlocked = false; // Разблокируем выбор станций
        updateRouteCache(); // Очищаем кэш маршрута
        needsRedraw = true; // Указываем, что нужно перерисовать карту
        invalidate(); // Запускаем перерисовку
    }

    public void clearSelectedStations() {
        this.selectedStations = null;
        needsRedraw = true;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bufferBitmap == null || needsRedraw) {
            createBufferBitmap();
        }

        // Очищаем холст

        canvas.drawColor(mapBackgroundColor);

        // Обновляем кэш, если нужно
        if (needsRedraw) {
            updatePathCache();
        }

        // Применяем трансформацию
        transformMatrix.reset();
        transformMatrix.postTranslate(translateX, translateY);
        transformMatrix.postScale(scaleFactor, scaleFactor);

        // Отрисовываем карту
        drawMapContents(canvas);

    }

    private void createBufferBitmap() {
        if (bufferBitmap != null) {
            bufferBitmap.recycle();
        }
        bufferBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bufferCanvas = new Canvas(bufferBitmap);

        // Fill buffer with theme background
        bufferCanvas.drawColor(mapBackgroundColor);
        needsRedraw = false;
    }

    private class LinePath {
        Path path;
        String color;
        Path innerPath; // Добавляем поле для внутреннего пути
        Paint whitePaint; // Добавляем поле для белой заливки
        Line line; // Добавляем поле для линии (для отображения номера)

        LinePath(Path path, String color) {
            this.path = path;
            this.color = color;
        }

        LinePath(Path path, String color, Path innerPath, Paint whitePaint) {
            this.path = path;
            this.color = color;
            this.innerPath = innerPath;
            this.whitePaint = whitePaint;
        }

        LinePath(Path path, String color, Line line) {
            this.path = path;
            this.color = color;
            this.line = line;
        }

        LinePath(Path path, String color, Path innerPath, Paint whitePaint, Line line) {
            this.path = path;
            this.color = color;
            this.innerPath = innerPath;
            this.whitePaint = whitePaint;
            this.line = line;
        }
    }

    private class StationPath {
        Path path;
        String color;
        int textPosition; // Позиция текста (0-8, 9 - не отображать)
        String stationName; // Название станции

        Float labelX;
        Float labelY;

        StationPath(Path path, String color, int textPosition, String stationName) {

            this(path, color, textPosition, stationName, null, null);
        }

        StationPath(Path path, String color, int textPosition, String stationName, Float labelX, Float labelY) {
            this.path = path;
            this.color = color;
            this.textPosition = textPosition;
            this.stationName = stationName;

            this.labelX = labelX;
            this.labelY = labelY;
        }

        boolean hasCustomLabelPosition() {
            return labelX != null && labelY != null;
        }
    }

    private class MapPathCache {
        List<LinePath> linesPaths = new ArrayList<>();
        List<StationPath> stationsPaths = new ArrayList<>();
        Path transfersPath = new Path();

        Path dashedTransfersPath = new Path();
        Path transfersFillPath = new Path();
        Path transfersFillOverlayMainPath = new Path(); // зелёные (основные по периметру заливки)
        Path transfersFillOverlayAngularPath = new Path(); // фиолетовые (угловые по периметру заливки)
        Path debugMainSegmentsPath = new Path(); // жёлтые основные сегменты (смещённые)
        Path debugConnectionsPath = new Path(); // голубые соединения ближайших точек соседних линий
        Path riversPath = new Path();
        List<PartialCircle> partialCircles = new ArrayList<>();
        Path convexHullPath = new Path(); // Добавляем путь для выпуклой оболочки

        List<StrokeSegment> transferStrokes = new ArrayList<>(); // красные основные сегменты
        List<CrossSegmentStroke> crossStrokes = new ArrayList<>(); // кроссплатформенные цветные сегменты
        boolean isInitialized = false;
    }


    private static class CrossSegmentStroke {
        final PointF a;
        final PointF b;
        final String colorStart;
        final String colorEnd;
        CrossSegmentStroke(PointF a, PointF b, String colorStart, String colorEnd) {
            this.a = a; this.b = b; this.colorStart = colorStart; this.colorEnd = colorEnd;
        }
    }

    private static class StrokeSegment {
        final PointF a;
        final PointF b;
        StrokeSegment(PointF a, PointF b) { this.a = a; this.b = b; }
    }

    private static class RouteLineSegment {
        Line line;
        int startIndex;
        int endIndex;
        Path segmentPath;

        RouteLineSegment(Line line, int startIndex, int endIndex, Path segmentPath) {
            this.line = line;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.segmentPath = segmentPath;
        }
    }

    private static class DirectionArrow {
        Line line;
        float directionX;
        float directionY;
        float distance;
        String edge;
        float nearestStationX;
        float nearestStationY;

        DirectionArrow(Line line, float directionX, float directionY, float distance, String edge, float nearestStationX, float nearestStationY) {
            this.line = line;
            this.directionX = directionX;
            this.directionY = directionY;
            this.distance = distance;
            this.edge = edge;
            this.nearestStationX = nearestStationX;
            this.nearestStationY = nearestStationY;
        }
    }

    // Обновляем класс RoutePathCache, добавляя поля для переходов
    private class RoutePathCache {
        List<LinePath> routeLinesPaths = new ArrayList<>();
        List<StationPath> routeStationsPaths = new ArrayList<>();
        Path transfersPath = new Path();

        Path dashedTransfersPath = new Path();
        List<PartialCircle> partialCircles = new ArrayList<>();

        Path transfersFillPath = new Path();
        Path transfersFillOverlayMainPath = new Path();
        Path transfersFillOverlayAngularPath = new Path();
        Path convexHullPath = new Path();
        List<CrossSegmentStroke> crossStrokes = new ArrayList<>();
        List<StationPath> routeFaintStationsPaths = new ArrayList<>();
        List<RouteLineSegment> routeLineSegments = new ArrayList<>();
        boolean isInitialized = false;
    }

    private List<PointF> calculateConvexHull(List<PointF> points) {
        if (points.size() <= 3) {
            return points;
        }

        // Сортируем точки по координатам
        points.sort((a, b) -> Float.compare(a.x, b.x) != 0 ? Float.compare(a.x, b.x) : Float.compare(a.y, b.y));

        // Верхняя и нижняя части выпуклой оболочки
        List<PointF> upper = new ArrayList<>();
        List<PointF> lower = new ArrayList<>();

        for (PointF point : points) {
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), point) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(point);
        }

        for (int i = points.size() - 1; i >= 0; i--) {
            PointF point = points.get(i);
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), point) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(point);
        }

        // Убираем последнюю точку, так как она дублируется
        upper.remove(upper.size() - 1);
        lower.remove(lower.size() - 1);

        // Объединяем верхнюю и нижнюю части
        upper.addAll(lower);
        return upper;
    }

    private float cross(PointF o, PointF a, PointF b) {
        return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x);
    }

    private class PartialCircle {
        float centerX;
        float centerY;
        float radius;
        float strokeWidth;
        List<Float> angles;
        String color;

        PartialCircle(float centerX, float centerY, float radius, float strokeWidth, List<Float> angles, String color) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
            this.strokeWidth = strokeWidth;
            this.angles = angles;
            this.color = color;
        }
    }
    private MapPathCache pathCache = new MapPathCache();

    private static final int PULSE_ANIMATION_DURATION = 1000; // ms
    private static final float MAX_PULSE_SCALE = 1.3f;
    private ObjectAnimator pulseAnimator;
    private float currentPulseScale = 1.0f;

    private void initPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }

        pulseAnimator = ObjectAnimator.ofFloat(this, "currentPulseScale", 1.0f, MAX_PULSE_SCALE);
        pulseAnimator.setDuration(PULSE_ANIMATION_DURATION);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
    }

    // Add this setter for the animator to work
    public void setCurrentPulseScale(float scale) {
        this.currentPulseScale = scale;
        invalidate();
    }

    private void drawSelectedStation(Canvas canvas, float stationX, float stationY) {
        if (selectedStation == null) return;

        // Outer glow effect
        Paint glowPaint = new Paint();
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setColor(Color.parseColor("#3384c29d")); // Semi-transparent highlight color
        canvas.drawCircle(stationX, stationY, 35 * currentPulseScale, glowPaint);

        // Main highlight circle
        Paint highlightPaint = new Paint();
        highlightPaint.setColor(Color.parseColor("#84c29d"));
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(8);
        highlightPaint.setAntiAlias(true);
        canvas.drawCircle(stationX, stationY, 20, highlightPaint);

        // Inner highlight
        Paint innerHighlightPaint = new Paint();
        innerHighlightPaint.setColor(Color.parseColor("#4084c29d"));
        innerHighlightPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(stationX, stationY, 20, innerHighlightPaint);
    }


    private void drawSelectedStationIndicator(Canvas canvas, float stationX, float stationY) {
        if (selectedStation == null) {
            return;
        }
        Line lineForStation = findLineForStation(selectedStation);
        if (lineForStation == null) {
            return;
        }

        String displayNumber = lineForStation.getLineDisplayNumberForStation(selectedStation);
        if (displayNumber == null || displayNumber.trim().isEmpty()) {
            displayNumber = lineForStation.getdisplayNumber();
        }
        if (displayNumber == null || displayNumber.trim().isEmpty()) {
            displayNumber = lineForStation.getId();
        }
        if (displayNumber == null || displayNumber.trim().isEmpty()) {
            return;
        }

        int lineColor = parseColorSafely(lineForStation.getColor(), mapTextColor);
        String displayShape = lineForStation.getDisplayShape();
        float radius = 40f;
        float verticalSpacing = 30f;
        float shapeCenterY = stationY - radius - verticalSpacing;
        float centerX = stationX;
        float centerY = shapeCenterY;

        Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setStyle(Paint.Style.FILL);
        bubblePaint.setColor(lineColor);

        Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(6f);
        outlinePaint.setColor(mapBackgroundColor);

        if (displayShape != null && displayShape.equals("SQUARE")) {
            RectF squareRect = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            canvas.drawRect(squareRect, bubblePaint);
            canvas.drawRect(squareRect, outlinePaint);
        } else if (displayShape != null && displayShape.equals("PARALLELOGRAM")) {
            float x = radius * 2.0f / 3.0f;
            float longSide = radius * 2.0f;
            float height = x * (float) Math.sqrt(3.0);
            float horizontalOffset = x * 0.5f;
            
            Path parallelogramPath = new Path();
            parallelogramPath.moveTo(centerX - longSide / 2.0f, centerY + height / 2.0f);
            parallelogramPath.lineTo(centerX + longSide / 2.0f, centerY + height / 2.0f);
            parallelogramPath.lineTo(centerX + longSide / 2.0f + horizontalOffset, centerY - height / 2.0f);
            parallelogramPath.lineTo(centerX - longSide / 2.0f + horizontalOffset, centerY - height / 2.0f);
            parallelogramPath.close();
            canvas.drawPath(parallelogramPath, bubblePaint);
            canvas.drawPath(parallelogramPath, outlinePaint);
        } else {
            canvas.drawCircle(centerX, centerY, radius, bubblePaint);
            canvas.drawCircle(centerX, centerY, radius, outlinePaint);
        }

        Paint indicatorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorTextPaint.setColor(mapStationFillColor);
        indicatorTextPaint.setTextAlign(Paint.Align.CENTER);
        Typeface indicatorTypeface = ResourcesCompat.getFont(getContext(), R.font.emyslabaltblack);
        if (indicatorTypeface != null) {
            indicatorTextPaint.setTypeface(indicatorTypeface);
        } else {
            indicatorTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }
        indicatorTextPaint.setTextSize(STATION_NAME_TEXT_SIZE * 1.6f);

        float textX = stationX;
        if (displayShape != null && displayShape.equals("PARALLELOGRAM")) {
            float x = radius * 2.0f / 3.0f;
            float horizontalOffset = x * 0.5f;
            textX = centerX + horizontalOffset / 2.0f;
        }
        float textY = centerY - (indicatorTextPaint.ascent() + indicatorTextPaint.descent()) / 2f;
        canvas.drawText(displayNumber, textX, textY, indicatorTextPaint);
    }

    private LinePath findLinePathForSegment(Station station1, Station station2, List<LinePath> routeLinesPaths) {
        for (LinePath lp : routeLinesPaths) {
            if (lp.line != null) {
                List<Station> lineStations = lp.line.getStations();
                if (lineStations.contains(station1) && lineStations.contains(station2)) {
                    int idx1 = lineStations.indexOf(station1);
                    int idx2 = lineStations.indexOf(station2);
                    if (Math.abs(idx1 - idx2) == 1) {
                        return lp;
                    }
                }
            }
        }
        return null;
    }

    private Path buildContinuousPathForSegment(List<Station> route, int startIndex, int endIndex, Line line) {
        Path continuousPath = new Path();
        
        if (startIndex >= endIndex || startIndex < 0 || endIndex >= route.size()) {
            return continuousPath;
        }
        
        boolean firstPoint = true;
        
        for (int i = startIndex; i <= endIndex; i++) {
            Station station = route.get(i);
            float x = station.getX() * currentCoordinateScaleFactor;
            float y = station.getY() * currentCoordinateScaleFactor;
            
            if (firstPoint) {
                continuousPath.moveTo(x, y);
                firstPoint = false;
            } else {
                if (i > startIndex) {
                    Station prevStation = route.get(i - 1);
                    LinePath lp = findLinePathForSegment(prevStation, station, routePathCache.routeLinesPaths);
                    
                    if (lp != null && lp.path != null) {
                        PathMeasure measure = new PathMeasure(lp.path, false);
                        float length = measure.getLength();
                        
                        if (length > 0) {
                            float[] pos = new float[2];
                            float[] tan = new float[2];
                            
                            for (float t = 5f; t < length; t += 5f) {
                                measure.getPosTan(t, pos, tan);
                                continuousPath.lineTo(pos[0], pos[1]);
                            }
                            
                            measure.getPosTan(length, pos, tan);
                            continuousPath.lineTo(pos[0], pos[1]);
                        } else {
                            continuousPath.lineTo(x, y);
                        }
                    } else {
                        continuousPath.lineTo(x, y);
                    }
                } else {
                    continuousPath.lineTo(x, y);
                }
            }
        }
        
        return continuousPath;
    }

    private void drawRouteLineIndicator(Canvas canvas, float centerX, float centerY, Line line, List<RouteLineSegment> allRouteSegments) {
        if (line == null) {
            return;
        }

        String displayNumber = line.getdisplayNumber();
        if (displayNumber == null || displayNumber.trim().isEmpty()) {
            displayNumber = line.getId();
        }
        if (displayNumber == null || displayNumber.trim().isEmpty()) {
            return;
        }

        int lineColor = parseColorSafely(line.getColor(), mapTextColor);
        String displayShape = line.getDisplayShape();
        float radius = 40f;
        float baseSpacing = 150f;
        float spacing = baseSpacing * scaleFactor;
        float routeLineWidth = 9f;
        float minDistanceFromRoute = radius + routeLineWidth / 2f + 30f;

        boolean isVerticalLine = false;
        if (allRouteSegments != null && !allRouteSegments.isEmpty()) {
            for (RouteLineSegment segment : allRouteSegments) {
                if (segment != null && segment.segmentPath != null) {
                    PathMeasure measure = new PathMeasure(segment.segmentPath, false);
                    if (measure.getLength() > 0) {
                        float[] startPos = new float[2];
                        float[] endPos = new float[2];
                        float[] tan = new float[2];
                        measure.getPosTan(0, startPos, tan);
                        measure.getPosTan(measure.getLength(), endPos, tan);
                        float dx = Math.abs(endPos[0] - startPos[0]);
                        float dy = Math.abs(endPos[1] - startPos[1]);
                        if (dy > dx * 1.5f) {
                            isVerticalLine = true;
                            break;
                        }
                    }
                }
            }
        }

        float[] positions;
        if (isVerticalLine) {
            positions = new float[]{
                centerX + radius + spacing, centerY,
                centerX - radius - spacing, centerY,
                centerX + radius + spacing * 1.2f, centerY,
                centerX - radius - spacing * 1.2f, centerY,
                centerX, centerY - radius - spacing,
                centerX, centerY + radius + spacing
            };
        } else {
            positions = new float[]{
                centerX, centerY - radius - spacing,
                centerX + radius + spacing, centerY,
                centerX, centerY + radius + spacing,
                centerX - radius - spacing, centerY
            };
        }

        float bestX = centerX;
        float bestY = centerY;
        if (isVerticalLine) {
            bestX = centerX + radius + spacing;
            bestY = centerY;
        } else {
            bestY = centerY - radius - spacing;
        }
        float bestScore = -1f;

        for (int i = 0; i < positions.length; i += 2) {
            float testX = positions[i];
            float testY = positions[i + 1];
            
            boolean overlapsRoute = false;
            float minDistToRoute = Float.MAX_VALUE;
            
            for (RouteLineSegment segment : allRouteSegments) {
                if (segment == null) {
                    continue;
                }
                
                boolean lineIntersect = checkLineIntersection(segment, testX, testY, radius, displayShape);
                
                if (lineIntersect) {
                    overlapsRoute = true;
                    break;
                }
                
                PathMeasure measure = new PathMeasure(segment.segmentPath, false);
                float pathLength = measure.getLength();
                
                if (pathLength > 0) {
                    for (float t = 0; t <= pathLength; t += 5f) {
                        float[] pos = new float[2];
                        float[] tan = new float[2];
                        if (measure.getPosTan(t, pos, tan)) {
                            float dist = (float) Math.sqrt(Math.pow(testX - pos[0], 2) + Math.pow(testY - pos[1], 2));
                            if (dist < minDistToRoute) {
                                minDistToRoute = dist;
                            }
                        }
                    }
                }
            }
            
            if (!overlapsRoute) {
                float score = minDistToRoute;
                if (score > bestScore) {
                    bestScore = score;
                    bestX = testX;
                    bestY = testY;
                }
            }
        }
        
        if (bestScore < 0) {
            float maxSpacing = spacing * 2.0f;
            float[] extendedPositions;
            if (isVerticalLine) {
                extendedPositions = new float[]{
                    centerX + radius + maxSpacing, centerY,
                    centerX - radius - maxSpacing, centerY,
                    centerX + radius + maxSpacing * 1.3f, centerY,
                    centerX - radius - maxSpacing * 1.3f, centerY,
                    centerX + radius + maxSpacing, centerY - radius - maxSpacing * 0.5f,
                    centerX - radius - maxSpacing, centerY - radius - maxSpacing * 0.5f,
                    centerX + radius + maxSpacing, centerY + radius + maxSpacing * 0.5f,
                    centerX - radius - maxSpacing, centerY + radius + maxSpacing * 0.5f,
                    centerX, centerY - radius - maxSpacing,
                    centerX, centerY + radius + maxSpacing
                };
            } else {
                extendedPositions = new float[]{
                    centerX, centerY - radius - maxSpacing,
                    centerX + radius + maxSpacing, centerY,
                    centerX, centerY + radius + maxSpacing,
                    centerX - radius - maxSpacing, centerY,
                    centerX - radius - maxSpacing, centerY - radius - maxSpacing,
                    centerX + radius + maxSpacing, centerY - radius - maxSpacing,
                    centerX - radius - maxSpacing, centerY + radius + maxSpacing,
                    centerX + radius + maxSpacing, centerY + radius + maxSpacing
                };
            }
            
            for (int i = 0; i < extendedPositions.length; i += 2) {
                float testX = extendedPositions[i];
                float testY = extendedPositions[i + 1];
                
                boolean overlapsRoute = false;
                
                for (RouteLineSegment segment : allRouteSegments) {
                    if (segment == null) {
                        continue;
                    }
                    
                    boolean lineIntersect = checkLineIntersection(segment, testX, testY, radius, displayShape);
                    
                    if (lineIntersect) {
                        overlapsRoute = true;
                        break;
                    }
                }
                
                if (!overlapsRoute) {
                    bestX = testX;
                    bestY = testY;
                    bestScore = 1f;
                    break;
                }
            }
        }
        
        if (bestScore < 0) {
            return;
        }

        Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setStyle(Paint.Style.FILL);
        bubblePaint.setColor(lineColor);

        Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(6f);
        outlinePaint.setColor(mapBackgroundColor);

        if (displayShape != null && displayShape.equals("SQUARE")) {
            RectF squareRect = new RectF(bestX - radius, bestY - radius, bestX + radius, bestY + radius);
            canvas.drawRect(squareRect, bubblePaint);
            canvas.drawRect(squareRect, outlinePaint);
        } else if (displayShape != null && displayShape.equals("PARALLELOGRAM")) {
            float x = radius * 2.0f / 3.0f;
            float longSide = radius * 2.0f;
            float height = x * (float) Math.sqrt(3.0);
            float horizontalOffset = x * 0.5f;
            
            Path parallelogramPath = new Path();
            parallelogramPath.moveTo(bestX - longSide / 2.0f, bestY + height / 2.0f);
            parallelogramPath.lineTo(bestX + longSide / 2.0f, bestY + height / 2.0f);
            parallelogramPath.lineTo(bestX + longSide / 2.0f + horizontalOffset, bestY - height / 2.0f);
            parallelogramPath.lineTo(bestX - longSide / 2.0f + horizontalOffset, bestY - height / 2.0f);
            parallelogramPath.close();
            canvas.drawPath(parallelogramPath, bubblePaint);
            canvas.drawPath(parallelogramPath, outlinePaint);
        } else {
            canvas.drawCircle(bestX, bestY, radius, bubblePaint);
            canvas.drawCircle(bestX, bestY, radius, outlinePaint);
        }

        Paint indicatorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorTextPaint.setColor(mapStationFillColor);
        indicatorTextPaint.setTextAlign(Paint.Align.CENTER);
        Typeface indicatorTypeface = ResourcesCompat.getFont(getContext(), R.font.emyslabaltblack);
        if (indicatorTypeface != null) {
            indicatorTextPaint.setTypeface(indicatorTypeface);
        } else {
            indicatorTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }
        indicatorTextPaint.setTextSize(STATION_NAME_TEXT_SIZE * 1.6f);

        float textX = bestX;
        if (displayShape != null && displayShape.equals("PARALLELOGRAM")) {
            float x = radius * 2.0f / 3.0f;
            float horizontalOffset = x * 0.5f;
            textX = bestX + horizontalOffset / 2.0f;
        }
        float textY = bestY - (indicatorTextPaint.ascent() + indicatorTextPaint.descent()) / 2f;
        canvas.drawText(displayNumber, textX, textY, indicatorTextPaint);
    }

    private float distanceFromRectToPoint(RectF rect, float x, float y) {
        float dx = Math.max(rect.left - x, Math.max(0, x - rect.right));
        float dy = Math.max(rect.top - y, Math.max(0, y - rect.bottom));
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private boolean isCircleIntersectingLine(float cx, float cy, float radius, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        
        if (dx == 0 && dy == 0) {
            float dist = (float) Math.hypot(cx - x1, cy - y1);
            return dist <= radius;
        }
        
        float t = ((cx - x1) * dx + (cy - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        
        float closestX = x1 + t * dx;
        float closestY = y1 + t * dy;
        
        float dist = (float) Math.hypot(cx - closestX, cy - closestY);
        return dist <= radius;
    }

    private boolean isSquareIntersectingLine(float centerX, float centerY, float radius, float x1, float y1, float x2, float y2) {
        float left = centerX - radius;
        float right = centerX + radius;
        float top = centerY - radius;
        float bottom = centerY + radius;
        
        float[] squareEdges = new float[]{
            left, top, right, top,
            right, top, right, bottom,
            right, bottom, left, bottom,
            left, bottom, left, top
        };
        
        for (int i = 0; i < squareEdges.length; i += 4) {
            if (isCircleIntersectingLine((squareEdges[i] + squareEdges[i + 2]) / 2f,
                                        (squareEdges[i + 1] + squareEdges[i + 3]) / 2f,
                                        radius * 0.1f, x1, y1, x2, y2)) {
                return true;
            }
            
            float edgeX1 = squareEdges[i];
            float edgeY1 = squareEdges[i + 1];
            float edgeX2 = squareEdges[i + 2];
            float edgeY2 = squareEdges[i + 3];
            
            if (linesIntersect(edgeX1, edgeY1, edgeX2, edgeY2, x1, y1, x2, y2)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean isParallelogramIntersectingLine(float centerX, float centerY, float radius, float x1, float y1, float x2, float y2) {
        float x = radius * 2.0f / 3.0f;
        float longSide = radius * 2.0f;
        float height = x * (float) Math.sqrt(3.0);
        float horizontalOffset = x * 0.5f;
        
        float[] vertices = new float[]{
            centerX - longSide / 2.0f, centerY + height / 2.0f,
            centerX + longSide / 2.0f, centerY + height / 2.0f,
            centerX + longSide / 2.0f + horizontalOffset, centerY - height / 2.0f,
            centerX - longSide / 2.0f + horizontalOffset, centerY - height / 2.0f
        };
        
        for (int i = 0; i < vertices.length; i += 2) {
            int nextIdx = (i + 2) % vertices.length;
            float edgeX1 = vertices[i];
            float edgeY1 = vertices[i + 1];
            float edgeX2 = vertices[nextIdx];
            float edgeY2 = vertices[nextIdx + 1];
            
            if (linesIntersect(edgeX1, edgeY1, edgeX2, edgeY2, x1, y1, x2, y2)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean linesIntersect(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        float denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (denom == 0) return false;
        
        float t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        float u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;
        
        return t >= 0 && t <= 1 && u >= 0 && u <= 1;
    }

    private boolean checkLineIntersection(RouteLineSegment segment, float indicatorX, float indicatorY, float indicatorRadius, String displayShape) {
        if (segment == null || route == null) {
            return false;
        }

        if (segment.startIndex < 0 || segment.endIndex >= route.size() || segment.startIndex > segment.endIndex) {
            return false;
        }

        if (segment.endIndex - segment.startIndex < 1) {
            return false;
        }

        float routeLineWidth = 9f;
        float effectiveRadius = indicatorRadius + routeLineWidth / 2f;
        
        for (int i = segment.startIndex; i < segment.endIndex; i++) {
            Station station1 = route.get(i);
            Station station2 = route.get(i + 1);
            
            float x1 = station1.getX() * currentCoordinateScaleFactor;
            float y1 = station1.getY() * currentCoordinateScaleFactor;
            float x2 = station2.getX() * currentCoordinateScaleFactor;
            float y2 = station2.getY() * currentCoordinateScaleFactor;
            
            boolean intersects = false;
            if (displayShape != null && displayShape.equals("SQUARE")) {
                intersects = isSquareIntersectingLine(indicatorX, indicatorY, indicatorRadius, x1, y1, x2, y2);
            } else if (displayShape != null && displayShape.equals("PARALLELOGRAM")) {
                intersects = isParallelogramIntersectingLine(indicatorX, indicatorY, indicatorRadius, x1, y1, x2, y2);
            } else {
                intersects = isCircleIntersectingLine(indicatorX, indicatorY, effectiveRadius, x1, y1, x2, y2);
            }
            
            if (intersects) {
                return true;
            }
        }
        
        return false;
    }

    private boolean hasVisibleActiveLines() {
        if (visibleViewport == null || visibleViewport.isEmpty()) {
            return false;
        }

        List<Line> activeLines = getActiveLines();
        if (activeLines == null || activeLines.isEmpty()) {
            return false;
        }

        float minVisibleLength = 500f;

        for (Line line : activeLines) {
            List<Station> lineStations = line.getStations();
            if (lineStations == null || lineStations.isEmpty()) {
                continue;
            }

            float totalVisibleLength = 0f;

            for (int i = 0; i < lineStations.size() - 1; i++) {
                Station station1 = lineStations.get(i);
                Station station2 = lineStations.get(i + 1);
                
                float x1 = station1.getX();
                float y1 = station1.getY();
                float x2 = station2.getX();
                float y2 = station2.getY();
                
                float dx = x2 - x1;
                float dy = y2 - y1;
                float segmentLength = (float) Math.hypot(dx, dy);
                
                if (segmentLength <= 0) {
                    continue;
                }

                float step = 10f;
                int steps = (int) (segmentLength / step) + 1;
                float visibleSegmentLength = 0f;
                
                for (int j = 0; j <= steps; j++) {
                    float t = (float) j / steps;
                    float segmentX = x1 + t * dx;
                    float segmentY = y1 + t * dy;
                    
                    if (visibleViewport.contains(segmentX, segmentY)) {
                        visibleSegmentLength += step;
                    }
                }
                
                if (visibleSegmentLength > 0) {
                    totalVisibleLength += visibleSegmentLength;
                }
            }

            if (totalVisibleLength >= minVisibleLength) {
                return true;
            }
        }

        return false;
    }

    private List<Line> getActiveLines() {
        if (isMetroMap && lines != null) {
            return lines;
        } else if (isRiverTramMap && riverTramLines != null) {
            return riverTramLines;
        } else if (isTramMap && tramLines != null) {
            return tramLines;
        } else if (isSuburbanMap && suburbanLines != null) {
            return suburbanLines;
        }
        return new ArrayList<>();
    }

    private List<DirectionArrow> findNearestActiveLines(int maxCount) {
        List<DirectionArrow> arrows = new ArrayList<>();
        
        if (visibleViewport == null || visibleViewport.isEmpty()) {
            return arrows;
        }

        List<Line> activeLines = getActiveLines();
        if (activeLines == null || activeLines.isEmpty()) {
            return arrows;
        }

        float viewportCenterX = visibleViewport.centerX();
        float viewportCenterY = visibleViewport.centerY();
        float viewportLeft = visibleViewport.left;
        float viewportRight = visibleViewport.right;
        float viewportTop = visibleViewport.top;
        float viewportBottom = visibleViewport.bottom;

        List<DirectionArrow> candidates = new ArrayList<>();

        for (Line line : activeLines) {
            List<Station> lineStations = line.getStations();
            if (lineStations == null || lineStations.isEmpty()) {
                continue;
            }

            float minDistance = Float.MAX_VALUE;
            Station nearestStation = null;
            String bestEdge = "top";
            float bestDirectionX = 0;
            float bestDirectionY = -1;

            for (Station station : lineStations) {
                float stationX = station.getX();
                float stationY = station.getY();

                if (visibleViewport.contains(stationX, stationY)) {
                    continue;
                }

                float dx = stationX - viewportCenterX;
                float dy = stationY - viewportCenterY;
                float distance = (float) Math.hypot(dx, dy);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearestStation = station;
                    
                    float absDx = Math.abs(dx);
                    float absDy = Math.abs(dy);
                    
                    if (absDx > absDy) {
                        if (dx > 0) {
                            bestEdge = "right";
                            bestDirectionX = 1;
                            bestDirectionY = 0;
                        } else {
                            bestEdge = "left";
                            bestDirectionX = -1;
                            bestDirectionY = 0;
                        }
                    } else {
                        if (dy > 0) {
                            bestEdge = "bottom";
                            bestDirectionX = 0;
                            bestDirectionY = 1;
                        } else {
                            bestEdge = "top";
                            bestDirectionX = 0;
                            bestDirectionY = -1;
                        }
                    }
                }
            }

            if (nearestStation != null && minDistance < Float.MAX_VALUE) {
                float stationDx = nearestStation.getX() - viewportCenterX;
                float stationDy = nearestStation.getY() - viewportCenterY;
                float normalizedDx = stationDx / minDistance;
                float normalizedDy = stationDy / minDistance;
                candidates.add(new DirectionArrow(line, normalizedDx, normalizedDy, minDistance, bestEdge, nearestStation.getX(), nearestStation.getY()));
            }
        }

        candidates.sort((a, b) -> Float.compare(a.distance, b.distance));

        int count = Math.min(maxCount, candidates.size());
        for (int i = 0; i < count; i++) {
            arrows.add(candidates.get(i));
        }

        return arrows;
    }

    private PointF calculateArrowPosition(DirectionArrow arrow, float screenWidth, float screenHeight) {
        float margin = 60f;
        float bottomMargin = 200f;
        float arrowRadius = 35f;
        
        float stationMapX = arrow.nearestStationX;
        float stationMapY = arrow.nearestStationY;
        
        float[] stationScreen = {
            stationMapX * currentCoordinateScaleFactor,
            stationMapY * currentCoordinateScaleFactor
        };
        
        Matrix screenMatrix = new Matrix();
        screenMatrix.postTranslate(translateX, translateY);
        screenMatrix.postScale(scaleFactor, scaleFactor);
        screenMatrix.mapPoints(stationScreen);
        
        float stationScreenX = stationScreen[0];
        float stationScreenY = stationScreen[1];
        
        float x = 0;
        float y = 0;
        
        switch (arrow.edge) {
            case "top":
                x = Math.max(margin + arrowRadius, Math.min(screenWidth - margin - arrowRadius, stationScreenX));
                y = margin;
                break;
            case "bottom":
                x = Math.max(margin + arrowRadius, Math.min(screenWidth - margin - arrowRadius, stationScreenX));
                y = screenHeight - bottomMargin;
                break;
            case "left":
                x = margin;
                y = Math.max(margin + arrowRadius, Math.min(screenHeight - bottomMargin - arrowRadius, stationScreenY));
                break;
            case "right":
                x = screenWidth - margin;
                y = Math.max(margin + arrowRadius, Math.min(screenHeight - bottomMargin - arrowRadius, stationScreenY));
                break;
        }

        return new PointF(x, y);
    }

    private void drawDirectionArrow(Canvas canvas, DirectionArrow arrow, float x, float y) {
        if (arrow == null || arrow.line == null) {
            return;
        }

        String displayNumber = arrow.line.getdisplayNumber();
        if (displayNumber == null || displayNumber.trim().isEmpty()) {
            displayNumber = arrow.line.getId();
        }
        if (displayNumber == null || displayNumber.trim().isEmpty()) {
            return;
        }

        int lineColor = parseColorSafely(arrow.line.getColor(), mapTextColor);
        String displayShape = arrow.line.getDisplayShape();
        float radius = 35f;

        Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setStyle(Paint.Style.FILL);
        bubblePaint.setColor(lineColor);

        Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(6f);
        outlinePaint.setColor(mapBackgroundColor);

        if (displayShape != null && displayShape.equals("SQUARE")) {
            RectF squareRect = new RectF(x - radius, y - radius, x + radius, y + radius);
            canvas.drawRect(squareRect, bubblePaint);
            canvas.drawRect(squareRect, outlinePaint);
        } else if (displayShape != null && displayShape.equals("PARALLELOGRAM")) {
            float xShape = radius * 2.0f / 3.0f;
            float longSide = radius * 2.0f;
            float height = xShape * (float) Math.sqrt(3.0);
            float horizontalOffset = xShape * 0.5f;
            
            Path parallelogramPath = new Path();
            parallelogramPath.moveTo(x - longSide / 2.0f, y + height / 2.0f);
            parallelogramPath.lineTo(x + longSide / 2.0f, y + height / 2.0f);
            parallelogramPath.lineTo(x + longSide / 2.0f + horizontalOffset, y - height / 2.0f);
            parallelogramPath.lineTo(x - longSide / 2.0f + horizontalOffset, y - height / 2.0f);
            parallelogramPath.close();
            canvas.drawPath(parallelogramPath, bubblePaint);
            canvas.drawPath(parallelogramPath, outlinePaint);
        } else {
            canvas.drawCircle(x, y, radius, bubblePaint);
            canvas.drawCircle(x, y, radius, outlinePaint);
        }

        Paint indicatorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorTextPaint.setColor(mapStationFillColor);
        indicatorTextPaint.setTextAlign(Paint.Align.CENTER);
        Typeface indicatorTypeface = ResourcesCompat.getFont(getContext(), R.font.emyslabaltblack);
        if (indicatorTypeface != null) {
            indicatorTextPaint.setTypeface(indicatorTypeface);
        } else {
            indicatorTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }
        indicatorTextPaint.setTextSize(STATION_NAME_TEXT_SIZE * 1.4f);

        float textX = x;
        if (displayShape != null && displayShape.equals("PARALLELOGRAM")) {
            float xShape = radius * 2.0f / 3.0f;
            float horizontalOffset = xShape * 0.5f;
            textX = x + horizontalOffset / 2.0f;
        }
        float textY = y - (indicatorTextPaint.ascent() + indicatorTextPaint.descent()) / 2f;
        canvas.drawText(displayNumber, textX, textY, indicatorTextPaint);

        float arrowLength = 25f;
        float arrowHeadSize = 12f;
        float arrowX = x + arrow.directionX * (radius + arrowLength);
        float arrowY = y + arrow.directionY * (radius + arrowLength);

        Path arrowPath = new Path();
        arrowPath.moveTo(x + arrow.directionX * radius, y + arrow.directionY * radius);
        arrowPath.lineTo(arrowX, arrowY);

        float perpX = -arrow.directionY;
        float perpY = arrow.directionX;
        arrowPath.lineTo(arrowX - perpX * arrowHeadSize - arrow.directionX * arrowHeadSize, 
                         arrowY - perpY * arrowHeadSize - arrow.directionY * arrowHeadSize);
        arrowPath.moveTo(arrowX, arrowY);
        arrowPath.lineTo(arrowX + perpX * arrowHeadSize - arrow.directionX * arrowHeadSize, 
                         arrowY + perpY * arrowHeadSize - arrow.directionY * arrowHeadSize);

        Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(4f);
        arrowPaint.setColor(mapBackgroundColor);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        arrowPaint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(arrowPath, arrowPaint);
    }

    private void drawMapContents(Canvas canvas) {
        updateVisibleViewport();

        // Save initial canvas state
        int mainSaveCount = canvas.save();

        // Apply main transformation once
        canvas.concat(transformMatrix);


        Bitmap bg = (currentMapType == MapType.SCHEME) ? schemeBackgroundBitmap : satelliteBackgroundBitmap;
        if (bg != null) {
            canvas.drawBitmap(bg, 0, 0, null);
        }

        // Initialize or update path cache if needed
        if (!pathCache.isInitialized || needsRedraw) {
            updatePathCache();
        }

        // Draw grayed lines/stations with transform
//        if (grayedLines != null && !grayedLines.isEmpty()) {
            int saveCount = canvas.save();
            drawGrayedMap(canvas);
            canvas.restoreToCount(saveCount);
//        }

        // Draw cached paths
        if (rivers != null) {
            canvas.drawPath(pathCache.riversPath, riverPaint);
        }

        // Draw lines
        for (LinePath linePath : pathCache.linesPaths) {

            linePaint.setColor(parseColorSafely(linePath.color, mapTextColor));
            canvas.drawPath(linePath.path, linePaint);

            if (linePath.innerPath != null) {
                canvas.drawPath(linePath.innerPath, linePath.whitePaint);
            }
        }

//        // Отрисовка линий речного трамвая
//        for (LinePath linePath : riverTramLines) {
//            riverTramPaint.setColor(Color.parseColor(linePath.color));
//            canvas.drawPath(linePath.path, riverTramPaint);
//        }


        // Transfers will be drawn later (after stations) to ensure overlay

        // Skip partial circles for transfers in simplified layer

        // Ground transfers (dashed) should be the lowest layer
        Paint dashedStrokeBottom = new Paint();
        dashedStrokeBottom.setStyle(Paint.Style.STROKE);
        dashedStrokeBottom.setStrokeWidth(6f);
        dashedStrokeBottom.setColor(mapTransferStrokeColor);
        dashedStrokeBottom.setAntiAlias(true);
        dashedStrokeBottom.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
        canvas.drawPath(pathCache.dashedTransfersPath, dashedStrokeBottom);

        // Draw convex hull
        Paint fillPaint = new Paint();

        fillPaint.setColor(mapStationFillColor);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(pathCache.convexHullPath, fillPaint);


        // Stations will be drawn after transfers to stay on top

        // Draw transfers fill on general map (route overlay will darken if active)
        Paint transferFillPaint = new Paint();
        transferFillPaint.setStyle(Paint.Style.FILL);
        transferFillPaint.setColor(mapStationFillColor);
        transferFillPaint.setAntiAlias(true);
        canvas.drawPath(pathCache.transfersFillPath, transferFillPaint);

        // Рисуем обычные линии переходов (walking)
        Paint transferStroke = new Paint();
        transferStroke.setStyle(Paint.Style.STROKE);
        transferStroke.setStrokeWidth(6f);
        transferStroke.setColor(mapTransferStrokeColor);
        transferStroke.setAntiAlias(true);
        canvas.drawPath(pathCache.transfersPath, transferStroke);

        // (ground уже нарисованы самым нижним слоем)

        Paint transferOutline = new Paint();
        transferOutline.setStyle(Paint.Style.STROKE);
        transferOutline.setStrokeWidth(Math.max(transferPaint.getStrokeWidth(), 6f));
        transferOutline.setColor(mapTransferStrokeColor);
        transferOutline.setAntiAlias(true);
        canvas.drawPath(pathCache.transfersFillOverlayMainPath, transferOutline);
        canvas.drawPath(pathCache.transfersFillOverlayAngularPath, transferOutline);

        // Crossplatform arcs: draw colored arcs per station (fallback to transfer color)
        for (PartialCircle pc : pathCache.partialCircles) {
            drawPartialCircleWithColor(canvas,
                    pc.centerX, pc.centerY,
                    pc.radius, pc.strokeWidth,
                    pc.angles, pc.color);
        }

        // Crossplatform colored main strokes with gradient center blend
        for (CrossSegmentStroke s : pathCache.crossStrokes) {
            float cx = (s.a.x + s.b.x) / 2f;
            float cy = (s.a.y + s.b.y) / 2f;
            // Левая половина A->mid: требуется инвертировать — от colorEnd к смешанному
            int startColor = parseColorSafely(s.colorStart, mapTextColor);
            int endColor = parseColorSafely(s.colorEnd, mapTextColor);
            int midBlend = ColorUtils.blendARGB(startColor, endColor, 0.5f);

            Paint p1 = new Paint(Paint.ANTI_ALIAS_FLAG);
            p1.setStyle(Paint.Style.STROKE);
            p1.setStrokeWidth(Math.max(transferPaint.getStrokeWidth(), 6f));
            p1.setShader(new LinearGradient(s.a.x, s.a.y, cx, cy, endColor, midBlend, Shader.TileMode.CLAMP));
            canvas.drawLine(s.a.x, s.a.y, cx, cy, p1);

            // Правая половина mid->B: инвертировать — от смешанного к colorStart
            Paint p2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            p2.setStyle(Paint.Style.STROKE);
            p2.setStrokeWidth(Math.max(transferPaint.getStrokeWidth(), 6f));
            p2.setShader(new LinearGradient(cx, cy, s.b.x, s.b.y, midBlend, startColor, Shader.TileMode.CLAMP));
            canvas.drawLine(cx, cy, s.b.x, s.b.y, p2);
        }

        // Draw stations on top of transfer layer
        for (StationPath stationPath : pathCache.stationsPaths) {
            Paint stationFillPaint = new Paint();

            stationFillPaint.setColor(mapStationFillColor);
            stationFillPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(stationPath.path, stationFillPaint);

            Paint strokePaint = new Paint();

            strokePaint.setColor(parseColorSafely(stationPath.color, mapTextColor));
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(7);
            canvas.drawPath(stationPath.path, strokePaint);


            if (stationPath.textPosition != 9) {
                drawStationText(canvas, stationPath);
            }
        }

        // Draw map objects
        drawMapObjects(canvas);

        if (isEditMode) {
            drawIntermediatePoints(canvas);
        }

        // Draw dark overlay and route if exists
        if (route != null && route.size() > 1) {
            // Apply dark overlay
            applyDarkOverlay(canvas, mainSaveCount);

            // Update route cache if needed
            if (!routePathCache.isInitialized || needsRedraw) {
                updateRouteCache();
            }


            // Наземные переходы (ground) — самый нижний слой в режиме маршрута
            Paint routeDashedStroke = new Paint();
            routeDashedStroke.setStyle(Paint.Style.STROKE);
            routeDashedStroke.setStrokeWidth(6f);
            routeDashedStroke.setColor(mapTransferStrokeColor);
            routeDashedStroke.setAntiAlias(true);
            routeDashedStroke.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
            canvas.drawPath(routePathCache.dashedTransfersPath, routeDashedStroke);

            // Заливки переходов маршрута
            Paint routeFill = new Paint();
            routeFill.setStyle(Paint.Style.FILL);
            routeFill.setColor(mapStationFillColor);
            routeFill.setAntiAlias(true);
            canvas.drawPath(routePathCache.convexHullPath, routeFill);
            canvas.drawPath(routePathCache.transfersFillPath, routeFill);

            // Обычные линии переходов маршрута (walking)
            Paint routeTransferStroke = new Paint();
            routeTransferStroke.setStyle(Paint.Style.STROKE);
            routeTransferStroke.setStrokeWidth(6f);
            routeTransferStroke.setColor(mapTransferStrokeColor);
            routeTransferStroke.setAntiAlias(true);
            canvas.drawPath(routePathCache.transfersPath, routeTransferStroke);

            Paint routeOutline = new Paint();
            routeOutline.setStyle(Paint.Style.STROKE);
            routeOutline.setStrokeWidth(Math.max(transferPaint.getStrokeWidth(), 6f));
            routeOutline.setColor(mapTransferStrokeColor);
            routeOutline.setAntiAlias(true);
            canvas.drawPath(routePathCache.transfersFillOverlayMainPath, routeOutline);
            canvas.drawPath(routePathCache.transfersFillOverlayAngularPath, routeOutline);

            // Crossplatform для маршрута: градиенты
            for (CrossSegmentStroke s : routePathCache.crossStrokes) {
                float cx = (s.a.x + s.b.x) / 2f;
                float cy = (s.a.y + s.b.y) / 2f;
                int startColor = parseColorSafely(s.colorStart, mapTextColor);
                int endColor = parseColorSafely(s.colorEnd, mapTextColor);
                int midBlend = ColorUtils.blendARGB(startColor, endColor, 0.5f);
                Paint p1 = new Paint(Paint.ANTI_ALIAS_FLAG);
                p1.setStyle(Paint.Style.STROKE);
                p1.setStrokeWidth(Math.max(transferPaint.getStrokeWidth(), 6f));
                p1.setShader(new LinearGradient(s.a.x, s.a.y, cx, cy, endColor, midBlend, Shader.TileMode.CLAMP));
                canvas.drawLine(s.a.x, s.a.y, cx, cy, p1);
                Paint p2 = new Paint(Paint.ANTI_ALIAS_FLAG);
                p2.setStyle(Paint.Style.STROKE);
                p2.setStrokeWidth(Math.max(transferPaint.getStrokeWidth(), 6f));
                p2.setShader(new LinearGradient(cx, cy, s.b.x, s.b.y, midBlend, startColor, Shader.TileMode.CLAMP));
                canvas.drawLine(cx, cy, s.b.x, s.b.y, p2);
            }

            // Draw route partial circles
            for (PartialCircle partialCircle : routePathCache.partialCircles) {
                drawPartialCircleWithColor(canvas,
                        partialCircle.centerX, partialCircle.centerY,
                        partialCircle.radius, partialCircle.strokeWidth,
                        partialCircle.angles, partialCircle.color);
            }

            // Draw route lines
            for (LinePath routeLinePath : routePathCache.routeLinesPaths) {

                routePaint.setColor(parseColorSafely(routeLinePath.color, mapTextColor));
                canvas.drawPath(routeLinePath.path, routePaint);

                if (routeLinePath.innerPath != null && routeLinePath.whitePaint != null) {
                    canvas.drawPath(routeLinePath.innerPath, routeLinePath.whitePaint);
                }
            }

            // Draw line indicators for continuous segments
            for (RouteLineSegment segment : routePathCache.routeLineSegments) {
                if (segment.line != null && segment.startIndex >= 0 && segment.endIndex < route.size()) {
                    float sumX = 0f;
                    float sumY = 0f;
                    int count = 0;
                    
                    for (int i = segment.startIndex; i <= segment.endIndex; i++) {
                        Station station = route.get(i);
                        sumX += station.getX() * currentCoordinateScaleFactor;
                        sumY += station.getY() * currentCoordinateScaleFactor;
                        count++;
                    }
                    
                    if (count > 0) {
                        float centerX = sumX / count;
                        float centerY = sumY / count;
                        drawRouteLineIndicator(canvas, centerX, centerY, segment.line, routePathCache.routeLineSegments);
                    }
                }
            }

            // Draw route stations
            for (StationPath routeStationPath : routePathCache.routeStationsPaths) {
                Paint stationFillPaint = new Paint();

                stationFillPaint.setColor(mapStationFillColor);
                stationFillPaint.setStyle(Paint.Style.FILL);
                canvas.drawPath(routeStationPath.path, stationFillPaint);

                Paint strokePaint = new Paint();

                strokePaint.setColor(parseColorSafely(routeStationPath.color, mapTextColor));
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setStrokeWidth(7);
                canvas.drawPath(routeStationPath.path, strokePaint);

                // Отрисовка текста станции маршрута белым цветом
                if (routeStationPath.textPosition != 9) { // 9 - не отображать текст
                    drawRouteStationText(canvas, routeStationPath);

                } else {
                    // Переходная станция: найдём «сестру» с позицией текста != 9 и используем её геометрию
                    Station labelStation = findLabelStationForTransferByName(routeStationPath.stationName);
                    if (labelStation != null) {
                        Path sp = new Path();
                        float sx = labelStation.getX() * currentCoordinateScaleFactor;
                        float sy = labelStation.getY() * currentCoordinateScaleFactor;
                        sp.addCircle(sx, sy, 14, Path.Direction.CW);
                        String fallbackColor = labelStation.getColor();
                        if (fallbackColor == null || fallbackColor.trim().isEmpty()) {
                            fallbackColor = routeStationPath.color;
                        }
                        Float labelXScaled = null;
                        Float labelYScaled = null;
                        if (labelStation.hasLabelCoordinates()) {
                            labelXScaled = labelStation.getLabelX() * currentCoordinateScaleFactor;
                            labelYScaled = labelStation.getLabelY() * currentCoordinateScaleFactor;
                        }
                        StationPath fallback = new StationPath(sp, fallbackColor, labelStation.getTextPosition(), labelStation.getName(), labelXScaled, labelYScaled);
                        drawRouteStationText(canvas, fallback);
                    }
                }
            }

            // Бледные точки станций, входящих в переходы маршрута
            for (StationPath faint : routePathCache.routeFaintStationsPaths) {
                // Заливка как у обычной станции
                Paint faintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
                faintFill.setStyle(Paint.Style.FILL);
                faintFill.setColor(mapStationFillColor);
                canvas.drawPath(faint.path, faintFill);

                // Обводка — осветлённый цвет линии (без прозрачности)
                Paint faintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
                int c = parseColorSafely(faint.color, mapTextColor);
                int light = ColorUtils.blendARGB(c, mapStationFillColor, 0.6f); // высветляем на 60%
                faintStroke.setColor(light);
                faintStroke.setStyle(Paint.Style.STROKE);
                faintStroke.setStrokeWidth(5);
                canvas.drawPath(faint.path, faintStroke);
            }


            if (userPositionStation != null) {
                if (userPositionStation == null) return;

                float stationX = userPositionStation.getX() * currentCoordinateScaleFactor;
                float stationY = userPositionStation.getY() * currentCoordinateScaleFactor;

                // Внешний круг (пульсация)
                Paint pulsePaint = new Paint();
                pulsePaint.setColor(Color.parseColor("#3384c29d")); // Полупрозрачный цвет
                pulsePaint.setStyle(Paint.Style.FILL);
                pulsePaint.setAntiAlias(true);
                canvas.drawCircle(stationX, stationY, 35 * currentPulseScale, pulsePaint);

                // Основной круг (обводка)
                Paint highlightPaint = new Paint();
                highlightPaint.setColor(Color.parseColor("#84c29d")); // Цвет обводки
                highlightPaint.setStyle(Paint.Style.STROKE);
                highlightPaint.setStrokeWidth(8);
                highlightPaint.setAntiAlias(true);
                canvas.drawCircle(stationX, stationY, 20, highlightPaint);

                // Внутренний круг (заливка)
                Paint innerPaint = new Paint();
                innerPaint.setColor(Color.parseColor("#4084c29d")); // Полупрозрачный цвет
                innerPaint.setStyle(Paint.Style.FILL);
                innerPaint.setAntiAlias(true);
                canvas.drawCircle(stationX, stationY, 20, innerPaint);
            }
        }


        if (selectedStation != null) {
            boolean isLastStationInRoute = route != null && route.size() > 0 && selectedStation.getId().equals(route.get(route.size() - 1).getId());
            
            if (!isLastStationInRoute) {
                float stationX = selectedStation.getX() * currentCoordinateScaleFactor;
                float stationY = selectedStation.getY() * currentCoordinateScaleFactor;

                if (pulseAnimator == null || !pulseAnimator.isRunning()) {
                    initPulseAnimation();
                    pulseAnimator.start();
                }

                drawSelectedStation(canvas, stationX, stationY);
                drawSelectedStationIndicator(canvas, stationX, stationY);
            }
        }

        // Restore main canvas state
        canvas.restoreToCount(mainSaveCount);

        if (!hasVisibleActiveLines()) {
            List<DirectionArrow> arrows = findNearestActiveLines(3);
            for (DirectionArrow arrow : arrows) {
                PointF position = calculateArrowPosition(arrow, getWidth(), getHeight());
                drawDirectionArrow(canvas, arrow, position.x, position.y);
            }
        }

        needsRedraw = false;
    }

    private void drawStationText(Canvas canvas, StationPath stationPath) {
        // Create paint for measurements

        Paint textPaint = new Paint(this.textPaint);
        textPaint.setColor(mapTextColor);

        // Get station center coordinates
        RectF bounds = new RectF();
        stationPath.path.computeBounds(bounds, true);
        float stationX = bounds.centerX();
        float stationY = bounds.centerY();
        float textOffset = 50; // Base offset from station

        // Default center position
        float textX = stationX;
        float textY = stationY;
        Paint.Align textAlign = Paint.Align.CENTER;


        boolean hasCustomPosition = stationPath.hasCustomLabelPosition();
        if (hasCustomPosition) {
            textX = stationPath.labelX;
            textY = stationPath.labelY;
            textAlign = Paint.Align.CENTER;
        } else {
            // Adjust position and alignment based on text position
            switch (stationPath.textPosition) {
                case 0: // center
                    textAlign = Paint.Align.CENTER;
                    break;
                case 1: // 12 o'clock

                    textY -= (textOffset - 5f);
                    textAlign = Paint.Align.CENTER;
                    break;
                case 2: // 1:30

                    textX += (textOffset * 0.7f - 5f);
                    textY -= (textOffset * 0.7f - 5f);
                    textAlign = Paint.Align.LEFT;
                    break;
                case 3: // 3 o'clock

                    textX += (textOffset - 5f);
                    textAlign = Paint.Align.LEFT;
                    break;
                case 4: // 4:30

                    textX += (textOffset * 0.7f - 5f);
                    textY += (textOffset * 0.7f - 5f);
                    textAlign = Paint.Align.LEFT;
                    break;
                case 5: // 6 o'clock

                    textY += (textOffset - 5f);
                    textAlign = Paint.Align.CENTER;
                    break;
                case 6: // 7:30

                    textX -= (textOffset * 0.7f - 5f);
                    textY += (textOffset * 0.7f - 5f);
                    textAlign = Paint.Align.RIGHT;
                    break;
                case 7: // 9 o'clock

                    textX -= (textOffset - 5f);
                    textAlign = Paint.Align.RIGHT;
                    break;
                case 8: // 10:30

                    textX -= (textOffset * 0.7f - 5f);
                    textY -= (textOffset * 0.7f - 5f);
                    textAlign = Paint.Align.RIGHT;
                    break;
                default:
                    return; // Don't draw text

            }
        }

        // Apply text alignment
        textPaint.setTextAlign(textAlign);

        // Add small vertical adjustment to center text vertically
        textY += textPaint.getTextSize() / 3;


        Paint outlinePaint = new Paint(textPaint);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(6f);
        outlinePaint.setColor(mapStationFillColor);
        outlinePaint.setStrokeJoin(Paint.Join.ROUND);
        outlinePaint.setStrokeMiter(10f);

        // Текст с обводкой
        textPaint.setColor(mapTextColor);

        String[] lines = stationPath.stationName.split("\\n");
        float lineHeight = textPaint.getTextSize() * 1.2f;
        float totalHeight = lineHeight * lines.length;
        float startY = textY - (totalHeight - lineHeight) / 2f;

        for (int i = 0; i < lines.length; i++) {
            float ly = startY + i * lineHeight;
            canvas.drawText(lines[i], textX, ly, outlinePaint);
            canvas.drawText(lines[i], textX, ly, textPaint);
        }
    }

    private void handleLinkTransfer(Transfer transfer) {
        // Центры переходов
        List<PointF> centers = new ArrayList<>();
        if (transfer.getLinkedTransferIds() != null) {
            for (String transferId : transfer.getLinkedTransferIds()) {
                PointF center = calculateTransferCenter(transferId);
                if (center != null) centers.add(center);
            }
        }

        // Станции-анкеры
        List<String> stationIds = transfer.getLinkedStationIds();
        if (stationIds != null && !stationIds.isEmpty() && !centers.isEmpty()) {
            PointF from = centers.get(0);
            for (String sid : stationIds) {
                Station st = findStationByIdAcrossAll(sid);
                if (st == null) continue;
                float x = st.getX() * currentCoordinateScaleFactor;
                float y = st.getY() * currentCoordinateScaleFactor;
                String type = transfer.getType().toLowerCase();
                if ("ground".equals(type)) {
                    pathCache.dashedTransfersPath.moveTo(from.x, from.y);
                    pathCache.dashedTransfersPath.lineTo(x, y);
                } else {
                    pathCache.transfersPath.moveTo(from.x, from.y);
                    pathCache.transfersPath.lineTo(x, y);
                }
            }
            return;
        }

        if (centers.size() >= 2) {
            PointF firstCenter = centers.get(0);
            for (int i = 1; i < centers.size(); i++) {
                PointF otherCenter = centers.get(i);
                String type = transfer.getType().toLowerCase();
                if ("ground".equals(type)) {
                    pathCache.dashedTransfersPath.moveTo(firstCenter.x, firstCenter.y);
                    pathCache.dashedTransfersPath.lineTo(otherCenter.x, otherCenter.y);
                } else {
                    pathCache.transfersPath.moveTo(firstCenter.x, firstCenter.y);
                    pathCache.transfersPath.lineTo(otherCenter.x, otherCenter.y);
                }
            }
        }
    }

    private PointF calculateTransferCenter(String transferId) {
        // Находим переход по ID среди всех переходов
        for (Transfer t : transfers) {
            if (transferId.equals(t.getId())) {
                List<Station> stations = t.getStations();
                if (stations == null || stations.isEmpty()) continue;
                
                // Вычисляем центр всех станций перехода
                float sumX = 0, sumY = 0;
                for (Station s : stations) {
                    sumX += s.getX() * currentCoordinateScaleFactor;
                    sumY += s.getY() * currentCoordinateScaleFactor;
                }
                return new PointF(sumX / stations.size(), sumY / stations.size());
            }
        }
        return null;
    }

    private void addTransferPathToCache(Transfer transfer) {
        List<Station> stations = transfer.getStations();
        if (stations == null || stations.size() < 2) {
            return;
        }


        // Специальная обработка переходов между переходами (TR_* ссылки)
        if (transfer.isLinkTransfer() && (
                (transfer.getLinkedTransferIds() != null && !transfer.getLinkedTransferIds().isEmpty()) ||
                (transfer.getLinkedStationIds() != null && !transfer.getLinkedStationIds().isEmpty())
        )) {
            handleLinkTransfer(transfer);
            return;
        }

        // Сбрасываем точки для построения единой замкнутой фигуры заливки
        transferConnectionPoints.clear();
        List<PointF[]> mainSegmentsForLog = new ArrayList<>(); // пары точек (x1o,y1o)-(x2o,y2o) для логов

        // Массив для хранения координат станций
        float[] coordinates = new float[stations.size() * 2];
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            float x = station.getX() * currentCoordinateScaleFactor;
            float y = station.getY() * currentCoordinateScaleFactor;
            coordinates[i * 2] = x;
            coordinates[i * 2 + 1] = y;
        }


        // Обработка группового перехода: соединяем ДВЕ группы станций минимальным числом связей
        if ("group".equalsIgnoreCase(transfer.getType()) && stations.size() > 2) {
            // По координатам разобьем на 2 кластера по направлению самой длинной оси (пара самых далеких точек)
            int n = stations.size();
            int iMax = 0, jMax = 1; float maxD2 = -1f;
            for (int i = 0; i < n; i++) {
                float xi = coordinates[i*2], yi = coordinates[i*2+1];
                for (int j = i+1; j < n; j++) {
                    float xj = coordinates[j*2], yj = coordinates[j*2+1];
                    float dx = xj - xi, dy = yj - yi; float d2 = dx*dx + dy*dy;
                    if (d2 > maxD2) { maxD2 = d2; iMax = i; jMax = j; }
                }
            }
            float ax = coordinates[iMax*2], ay = coordinates[iMax*2+1];
            float bx = coordinates[jMax*2], by = coordinates[jMax*2+1];
            float ux = bx - ax, uy = by - ay; float ulen = (float)Math.sqrt(ux*ux + uy*uy);
            if (ulen == 0) ulen = 1f; ux /= ulen; uy /= ulen;
            float cx = (ax + bx) * 0.5f, cy = (ay + by) * 0.5f; // середина оси

            java.util.List<Integer> clusterA = new java.util.ArrayList<>();
            java.util.List<Integer> clusterB = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                float x = coordinates[i*2] - cx, y = coordinates[i*2+1] - cy;
                float proj = x*ux + y*uy;
                if (proj <= 0) clusterA.add(i); else clusterB.add(i);
            }
            if (clusterA.isEmpty() || clusterB.isEmpty()) {
                // резервный случай: разделим пополам по индексу
                clusterA.clear(); clusterB.clear();
                for (int i = 0; i < n; i++) { if (i < n/2) clusterA.add(i); else clusterB.add(i); }
            }

            boolean[] usedB = new boolean[n];
            // используем уже объявленный выше mainSegmentsForLog
            for (int idxA : (clusterA.size() <= clusterB.size() ? clusterA : clusterB)) {
                int bestJ = -1; float bestD2 = Float.MAX_VALUE;
                for (int idxB : (clusterA.size() <= clusterB.size() ? clusterB : clusterA)) {
                    if (usedB[idxB]) continue;
                    float x1 = coordinates[idxA*2], y1 = coordinates[idxA*2+1];
                    float x2 = coordinates[idxB*2], y2 = coordinates[idxB*2+1];
                    float dx = x2 - x1, dy = y2 - y1; float d2 = dx*dx + dy*dy;
                    if (d2 < bestD2) { bestD2 = d2; bestJ = idxB; }
                }
                if (bestJ >= 0) {
                    usedB[bestJ] = true;
                    float x1 = coordinates[idxA*2], y1 = coordinates[idxA*2+1];
                    float x2 = coordinates[bestJ*2], y2 = coordinates[bestJ*2+1];
                    float dx = x2 - x1, dy = y2 - y1; float len = (float)Math.sqrt(dx*dx + dy*dy); if (len == 0) continue;
                    float shiftX = -(dy/len) * 20f; float shiftY = (dx/len) * 20f;
                    float x1o = x1 + shiftX, y1o = y1 + shiftY, x2o = x2 + shiftX, y2o = y2 + shiftY;
                    float dxo = x2o - x1o, dyo = y2o - y1o; float llen = (float)Math.sqrt(dxo*dxo + dyo*dyo);
                    if (llen > 0) {
                        float nx = -dyo/llen, ny = dxo/llen; float half = Math.max(transferPaint.getStrokeWidth()/2f, 3f);
                        float x1a = x1o + nx*half, y1a = y1o + ny*half;
                        float x1b = x1o - nx*half, y1b = y1o - ny*half;
                        float x2a = x2o + nx*half, y2a = y2o + ny*half;
                        float x2b = x2o - nx*half, y2b = y2o - ny*half;
                        transferConnectionPoints.add(new PointF(x1a, y1a));
                        transferConnectionPoints.add(new PointF(x1b, y1b));
                        transferConnectionPoints.add(new PointF(x2a, y2a));
                        transferConnectionPoints.add(new PointF(x2b, y2b));
                        mainSegmentsForLog.add(new PointF[]{new PointF(x1o, y1o), new PointF(x2o, y2o)});
                    }
                }
            }

            if (transferConnectionPoints.size() >= 3) {
                java.util.List<PointF> hull = calculateConvexHull(new java.util.ArrayList<>(transferConnectionPoints));
                if (!hull.isEmpty()) {
                    Path hullPath = new Path(); hullPath.moveTo(hull.get(0).x, hull.get(0).y);
                    for (int i = 1; i < hull.size(); i++) hullPath.lineTo(hull.get(i).x, hull.get(i).y);
                    hullPath.close(); pathCache.transfersFillPath.addPath(hullPath);

                    Path mainOverlay = new Path();
                    for (PointF[] seg : mainSegmentsForLog) { mainOverlay.moveTo(seg[0].x, seg[0].y); mainOverlay.lineTo(seg[1].x, seg[1].y); }
                    pathCache.transfersFillOverlayMainPath.addPath(mainOverlay);
                }
            }
            return;
        }

        // Отрисовка линий перехода
        for (int i = 0; i < stations.size(); i++) {
            int nextIndex = (i + 1) % stations.size();
            float x1 = coordinates[i * 2];
            float y1 = coordinates[i * 2 + 1];
            float x2 = coordinates[nextIndex * 2];
            float y2 = coordinates[nextIndex * 2 + 1];

            switch (transfer.getType().toLowerCase()) {
                case "crossplatform":
                    addHalfColoredLineToCache(x1, y1, x2, y2,
                            stations.get(i).getColor(),
                            stations.get(nextIndex).getColor());

                    // Дублируем логику обычного перехода для построения обводки/заливки
                    float dx_cp = x2 - x1;
                    float dy_cp = y2 - y1;
                    float len_cp = (float) Math.sqrt(dx_cp * dx_cp + dy_cp * dy_cp);
                    if (len_cp > 0) {
                        float shiftX_cp = -(dy_cp / len_cp) * 20f;
                        float shiftY_cp =  (dx_cp / len_cp) * 20f;
                        float x1o_cp = x1 + shiftX_cp;
                        float y1o_cp = y1 + shiftY_cp;
                        float x2o_cp = x2 + shiftX_cp;
                        float y2o_cp = y2 + shiftY_cp;

                        float dxo_cp = x2o_cp - x1o_cp;
                        float dyo_cp = y2o_cp - y1o_cp;
                        float llen_cp = (float) Math.sqrt(dxo_cp * dxo_cp + dyo_cp * dyo_cp);
                        if (llen_cp > 0) {
                            float ux_cp = -dyo_cp / llen_cp;
                            float uy_cp =  dxo_cp / llen_cp;
                            float half_cp = Math.max(transferPaint.getStrokeWidth() / 2f, 3f);

                            float x1a_cp = x1o_cp + ux_cp * half_cp;
                            float y1a_cp = y1o_cp + uy_cp * half_cp;
                            float x1b_cp = x1o_cp - ux_cp * half_cp;
                            float y1b_cp = y1o_cp - uy_cp * half_cp;
                            float x2a_cp = x2o_cp + ux_cp * half_cp;
                            float y2a_cp = y2o_cp + uy_cp * half_cp;
                            float x2b_cp = x2o_cp - ux_cp * half_cp;
                            float y2b_cp = y2o_cp - uy_cp * half_cp;

                            transferConnectionPoints.add(new PointF(x1a_cp, y1a_cp));
                            transferConnectionPoints.add(new PointF(x1b_cp, y1b_cp));
                            transferConnectionPoints.add(new PointF(x2a_cp, y2a_cp));
                            transferConnectionPoints.add(new PointF(x2b_cp, y2b_cp));

                            mainSegmentsForLog.add(new PointF[]{new PointF(x1o_cp, y1o_cp), new PointF(x2o_cp, y2o_cp)});
                            // Сохраняем цветной кросс-сегмент для покраски основными цветами с плавным переходом
                            pathCache.crossStrokes.add(new CrossSegmentStroke(new PointF(x1o_cp, y1o_cp), new PointF(x2o_cp, y2o_cp),
                                    stations.get(i).getColor(), stations.get(nextIndex).getColor()));
                        }
                    }
                    break;
                case "ground":
                    addDashedLineToCache(x1, y1, x2, y2);
                    break;

                case "walking":
                    // Простая прямая линия без смещения и заливки
                    pathCache.transfersPath.moveTo(x1, y1);
                    pathCache.transfersPath.lineTo(x2, y2);
                    break;
                default:
                    addShiftedLineToCache(x1, y1, x2, y2);

                    // Для единой заливки собираем угловые точки узкого квада вокруг смещенной линии
                    float dx = x2 - x1;
                    float dy = y2 - y1;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len > 0) {
                        float shiftX = -(dy / len) * 20f;
                        float shiftY =  (dx / len) * 20f;
                        float x1o = x1 + shiftX;
                        float y1o = y1 + shiftY;
                        float x2o = x2 + shiftX;
                        float y2o = y2 + shiftY;

                        float dxo = x2o - x1o;
                        float dyo = y2o - y1o;
                        float llen = (float) Math.sqrt(dxo * dxo + dyo * dyo);
                        if (llen > 0) {
                            float ux = -dyo / llen;
                            float uy =  dxo / llen;
                            float half = Math.max(transferPaint.getStrokeWidth() / 2f, 3f);

                            float x1a = x1o + ux * half;
                            float y1a = y1o + uy * half;
                            float x1b = x1o - ux * half;
                            float y1b = y1o - uy * half;
                            float x2a = x2o + ux * half;
                            float y2a = y2o + uy * half;
                            float x2b = x2o - ux * half;
                            float y2b = y2o - uy * half;

                            transferConnectionPoints.add(new PointF(x1a, y1a));
                            transferConnectionPoints.add(new PointF(x1b, y1b));
                            transferConnectionPoints.add(new PointF(x2a, y2a));
                            transferConnectionPoints.add(new PointF(x2b, y2b));
                            // Сохраняем основной смещенный сегмент для логов
                            mainSegmentsForLog.add(new PointF[]{new PointF(x1o, y1o), new PointF(x2o, y2o)});
                        }
                    }
                    break;
            }
        }

        // Отрисовка частичных кругов
        if (!transfer.getType().equalsIgnoreCase("ground")) {
            for (int i = 0; i < stations.size(); i++) {
                int prevIndex = (i - 1 + stations.size()) % stations.size();
                int nextIndex = (i + 1) % stations.size();
                float currentX = coordinates[i * 2];
                float currentY = coordinates[i * 2 + 1];
                float prevX = coordinates[prevIndex * 2];
                float prevY = coordinates[prevIndex * 2 + 1];
                float nextX = coordinates[nextIndex * 2];
                float nextY = coordinates[nextIndex * 2 + 1];
                List<Float> angles = getAngle(prevX, prevY, currentX, currentY, nextX, nextY);


                if (angles == null || angles.size() < 2) {
                    continue;
                }
                float sweep = angles.get(0);
                if (Float.isNaN(sweep) || Math.abs(sweep) < 1f || Math.abs(sweep) >= 359f) {
                    continue;
                }
                if (transfer.getType().equalsIgnoreCase("crossplatform")) {
                    pathCache.partialCircles.add(new PartialCircle(
                            currentX, currentY, 20, 6,
                            angles, stations.get(nextIndex).getColor()
                    ));
                } else {
                    pathCache.partialCircles.add(new PartialCircle(
                            currentX, currentY, 20, 6,
                            angles, null
                    ));
                }
            }
        }


        // Создаем единую фигуру заливки (выпуклая оболочка) и строим угловые рёбра как минимальное паросочетание
        if (transferConnectionPoints.size() >= 3) {

            List<PointF> hull = calculateConvexHull(new ArrayList<>(transferConnectionPoints));
            if (!hull.isEmpty()) {
                Path hullPath = new Path();
                hullPath.moveTo(hull.get(0).x, hull.get(0).y);
                for (int i = 1; i < hull.size(); i++) {
                    hullPath.lineTo(hull.get(i).x, hull.get(i).y);
                }
                hullPath.close();
                pathCache.transfersFillPath.addPath(hullPath);

                // Зелёные основные сегменты (по лог-данным)
                Path mainOverlay = new Path();
                Path debugMain = new Path();
                for (PointF[] seg : mainSegmentsForLog) {
                    PointF a = seg[0];
                    PointF b = seg[1];
                    mainOverlay.moveTo(a.x, a.y);
                    mainOverlay.lineTo(b.x, b.y);
                    debugMain.moveTo(a.x, a.y);
                    debugMain.lineTo(b.x, b.y);
                }
                pathCache.transfersFillOverlayMainPath.addPath(mainOverlay);
                pathCache.debugMainSegmentsPath.addPath(debugMain);

                // Угловые рёбра: минимальное паросочетание между всеми 2N конечными точками разных сегментов
                List<PointF> endpoints = new ArrayList<>();
                List<Integer> endpointSeg = new ArrayList<>();
                for (int i = 0; i < mainSegmentsForLog.size(); i++) {
                    PointF[] seg = mainSegmentsForLog.get(i);
                    endpoints.add(seg[0]); endpointSeg.add(i);
                    endpoints.add(seg[1]); endpointSeg.add(i);
                }
                class Pair { int i, j; float d; Pair(int i,int j,float d){this.i=i;this.j=j;this.d=d;} }
                List<Pair> pairs = new ArrayList<>();
                for (int i = 0; i < endpoints.size(); i++) {
                    for (int j = i+1; j < endpoints.size(); j++) {
                        if (!endpointSeg.get(i).equals(endpointSeg.get(j))) {
                            pairs.add(new Pair(i,j, distance2(endpoints.get(i), endpoints.get(j))));
                        }
                    }
                }
                pairs.sort((a,b)-> Float.compare(a.d, b.d));
                boolean[] used = new boolean[endpoints.size()];
                int need = mainSegmentsForLog.size();
                Path angularOverlay = new Path();
                Path debugConn = new Path();

                // Центр фигуры: среднее по вершинам выпуклой оболочки
                float cx = 0f, cy = 0f;
                for (PointF p : hull) { cx += p.x; cy += p.y; }
                if (!hull.isEmpty()) { cx /= hull.size(); cy /= hull.size(); }
                for (Pair p : pairs) {
                    if (need == 0) break;
                    if (used[p.i] || used[p.j]) continue;
                    used[p.i] = used[p.j] = true;
                    PointF u = endpoints.get(p.i);
                    PointF v = endpoints.get(p.j);
                    // Универсальный подход применяется ко всем числам станций

                    // Круговая дуга по углу между основными линиями и хорде (универсально)
                    addCircularArcFromMainLines(
                            angularOverlay, debugConn,
                            u, v,
                            endpointSeg.get(p.i), endpointSeg.get(p.j),
                            mainSegmentsForLog,
                            stations,
                            cx, cy
                    );
                    need--;
                }
                pathCache.transfersFillOverlayAngularPath.addPath(angularOverlay);
                pathCache.debugConnectionsPath.addPath(debugConn);

                // Логи основных смещённых линий
                StringBuilder sb = new StringBuilder();
                sb.append("Переход , ")
                  .append(stations.size())
                  .append(" станций, точки основных линий[");
                for (int i = 0; i < mainSegmentsForLog.size(); i++) {
                    PointF[] seg = mainSegmentsForLog.get(i);
                    int x1i = Math.round(seg[0].x);
                    int y1i = Math.round(seg[0].y);
                    int x2i = Math.round(seg[1].x);
                    int y2i = Math.round(seg[1].y);
                    sb.append("[[").append(x1i).append(",").append(y1i)
                      .append("],[").append(x2i).append(",").append(y2i).append("]]");
                    if (i < mainSegmentsForLog.size() - 1) sb.append(",");
                }
                sb.append("]");
                sb.append(", станции [");
                for (int i = 0; i < stations.size(); i++) {
                    Station s = stations.get(i);
                    int sx = Math.round(s.getX() * currentCoordinateScaleFactor);
                    int sy = Math.round(s.getY() * currentCoordinateScaleFactor);
                    sb.append(s.getName()).append("@[").append(sx).append(",").append(sy).append("]");
                    if (i < stations.size() - 1) sb.append(",");
                }
                sb.append("]");
                Log.d("TransferMainSegments", sb.toString());
            }
        }
    }

    private float distance2(PointF a, PointF b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    private PointF intersectLines(PointF p1, PointF p2, PointF p3, PointF p4) {
        float x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y;
        float x3 = p3.x, y3 = p3.y, x4 = p4.x, y4 = p4.y;
        float denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 1e-6f) return null;
        float px = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denom;
        float py = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denom;
        return new PointF(px, py);
    }

    private float cross2(float ax, float ay, float bx, float by) {
        return ax * by - ay * bx;
    }

    private boolean isOutsideAngleZone(PointF u, float tAx, float tAy, PointF v, float tBx, float tBy, PointF point) {
        // Линии задаём как (u, u + tA) и (v, v - tB)
        PointF p0 = intersectLines(u, new PointF(u.x + tAx, u.y + tAy), v, new PointF(v.x - tBx, v.y - tBy));
        if (p0 == null) return true; // параллельные — считаем вне зоны
        float v1x = tAx, v1y = tAy;
        float v2x = -tBx, v2y = -tBy;
        float vpx = point.x - p0.x, vpy = point.y - p0.y;
        float cross1 = cross2(v1x, v1y, vpx, vpy);
        float cross2v = cross2(v1x, v1y, v2x, v2y);
        float cross3 = cross2(v2x, v2y, vpx, vpy);
        float cross4 = cross2(v2x, v2y, v1x, v1y);
        boolean sameSide1 = Math.signum(cross1) == Math.signum(cross2v);
        boolean sameSide2 = Math.signum(cross3) == Math.signum(cross4);
        return !(sameSide1 && sameSide2);
    }

    private boolean isOutsideAngleZoneBySegments(PointF[] segA, PointF[] segB, PointF point) {
        PointF p0 = intersectLines(segA[0], segA[1], segB[0], segB[1]);
        if (p0 == null) return true;
        float v1x = segA[1].x - segA[0].x;
        float v1y = segA[1].y - segA[0].y;
        float v2x = segB[1].x - segB[0].x;
        float v2y = segB[1].y - segB[0].y;
        float vpx = point.x - p0.x;
        float vpy = point.y - p0.y;
        float cross1 = cross2(v1x, v1y, vpx, vpy);
        float cross2v = cross2(v1x, v1y, v2x, v2y);
        float cross3 = cross2(v2x, v2y, vpx, vpy);
        float cross4 = cross2(v2x, v2y, v1x, v1y);
        boolean sameSide1 = Math.signum(cross1) == Math.signum(cross2v);
        boolean sameSide2 = Math.signum(cross3) == Math.signum(cross4);
        return !(sameSide1 && sameSide2);
    }

    private boolean isBetweenParallelLines(PointF[] segA, PointF[] segB, PointF p) {
        float dirx = segA[1].x - segA[0].x;
        float diry = segA[1].y - segA[0].y;
        float len = (float) Math.hypot(dirx, diry);
        if (len < 1e-6f) return false;
        float nx = -diry / len;
        float ny =  dirx / len;
        float sep = (segB[0].x - segA[0].x) * nx + (segB[0].y - segA[0].y) * ny;
        // если линии почти совпали — считаем не между
        if (Math.abs(sep) < 1e-6f) return false;
        float dA = (p.x - segA[0].x) * nx + (p.y - segA[0].y) * ny;
        float dB = dA - sep;
        return dA * dB < 0f;
    }

    private boolean isPointInQuadrilateralBySegments(PointF[] segA, PointF[] segB, PointF p) {
        float[][] quad = new float[][]{
                new float[]{segA[0].x, segA[0].y},
                new float[]{segA[1].x, segA[1].y},
                new float[]{segB[0].x, segB[0].y},
                new float[]{segB[1].x, segB[1].y}
        };
        return isPointInPolygon(quad, p.x, p.y);
    }

    private boolean isPointInPolygon(float[][] polygon, float x, float y) {
        int n = polygon.length;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            float xi = polygon[i][0], yi = polygon[i][1];
            float xj = polygon[j][0], yj = polygon[j][1];
            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / ((yj - yi) == 0 ? 1e-6f : (yj - yi)) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    private String nearestStationInfo(PointF p, List<Station> transferStations) {
        Station best = null;
        float bestD2 = Float.MAX_VALUE;
        for (Station s : transferStations) {
            float sx = s.getX() * currentCoordinateScaleFactor;
            float sy = s.getY() * currentCoordinateScaleFactor;
            float dx = sx - p.x;
            float dy = sy - p.y;
            float d2 = dx * dx + dy * dy;
            if (d2 < bestD2) { bestD2 = d2; best = s; }
        }
        if (best == null) return "?";
        float sx = best.getX() * currentCoordinateScaleFactor;
        float sy = best.getY() * currentCoordinateScaleFactor;
        return best.getName() + "@(" + sx + "," + sy + ")";
    }

    private void addUniversalAngularBezier(Path angularOverlay, Path debugConn,
                                           PointF u, PointF v,
                                           int segAIdx, int segBIdx,
                                           List<PointF[]> mainSegmentsForLog,
                                           float centerX, float centerY) {
        float chordDx = v.x - u.x;
        float chordDy = v.y - u.y;
        float L = (float) Math.hypot(chordDx, chordDy);
        if (L == 0f) return;

        PointF[] segA = mainSegmentsForLog.get(segAIdx);
        PointF[] segB = mainSegmentsForLog.get(segBIdx);
        PointF sa0 = segA[0], sa1 = segA[1];
        PointF sb0 = segB[0], sb1 = segB[1];

        float tAx, tAy, tBx, tBy;
        if (u == sa0) { tAx = sa1.x - sa0.x; tAy = sa1.y - sa0.y; } else { tAx = sa0.x - sa1.x; tAy = sa0.y - sa1.y; }
        if (v == sb0) { tBx = sb1.x - sb0.x; tBy = sb1.y - sb0.y; } else { tBx = sb0.x - sb1.x; tBy = sb0.y - sb1.y; }

        float tAlen = (float) Math.hypot(tAx, tAy);
        float tBlen = (float) Math.hypot(tBx, tBy);
        if (tAlen == 0f || tBlen == 0f) return;
        tAx /= tAlen; tAy /= tAlen; tBx /= tBlen; tBy /= tBlen;

        float cux = centerX - u.x, cuy = centerY - u.y;
        float cvx = centerX - v.x, cvy = centerY - v.y;
        if (tAx * cux + tAy * cuy > 0f) { tAx = -tAx; tAy = -tAy; }
        if (tBx * cvx + tBy * cvy > 0f) { tBx = -tBx; tBy = -tBy; }

        float cosPhi = -(tAx * tBx + tAy * tBy);
        if (cosPhi > 1f) cosPhi = 1f; else if (cosPhi < -1f) cosPhi = -1f;
        float phi = (float) Math.acos(cosPhi);
        float sinHalf = (float) Math.sin(phi / 2f);
        float d;
        if (sinHalf < 1e-3f) {
            d = 0.5f * L;
        } else {
            float R = L / (2f * sinHalf);
            d = (float) ((4.0 / 3.0) * Math.tan(phi / 4f) * R);
        }
        float dMin = 0.15f * L;
        float dMax = 0.5f * L;
        if (d < dMin) d = dMin; else if (d > dMax) d = dMax;

        float c1x = u.x + tAx * d;
        float c1y = u.y + tAy * d;
        float c2x = v.x + tBx * d;
        float c2y = v.y + tBy * d;

        angularOverlay.moveTo(u.x, u.y);
        angularOverlay.cubicTo(c1x, c1y, c2x, c2y, v.x, v.y);
        debugConn.moveTo(u.x, u.y);
        debugConn.cubicTo(c1x, c1y, c2x, c2y, v.x, v.y);
    }

    private PointF findNearestStationCenter(PointF p, List<Station> transferStations) {
        PointF best = null;
        float bestD2 = Float.MAX_VALUE;
        for (Station s : transferStations) {
            float sx = s.getX() * currentCoordinateScaleFactor;
            float sy = s.getY() * currentCoordinateScaleFactor;
            float dx = sx - p.x;
            float dy = sy - p.y;
            float d2 = dx * dx + dy * dy;
            if (d2 < bestD2) { bestD2 = d2; best = new PointF(sx, sy); }
        }
        return best != null ? best : new PointF(p.x, p.y);
    }

    private void addFilletArcUsingStationCircles(Path angularOverlay, Path debugConn,
                                                 PointF u, PointF v,
                                                 int segAIdx, int segBIdx,
                                                 List<PointF[]> mainSegmentsForLog,
                                                 List<Station> transferStations,
                                                 float centerX, float centerY) {
        // Центры станций, ближайшие к концам
        PointF cA = findNearestStationCenter(u, transferStations);
        PointF cB = findNearestStationCenter(v, transferStations);

        // Касательные из красных сегментов
        PointF[] segA = mainSegmentsForLog.get(segAIdx);
        PointF[] segB = mainSegmentsForLog.get(segBIdx);
        float tAx = (u == segA[0] ? segA[1].x - segA[0].x : segA[0].x - segA[1].x);
        float tAy = (u == segA[0] ? segA[1].y - segA[0].y : segA[0].y - segA[1].y);
        float tBx = (v == segB[0] ? segB[1].x - segB[0].x : segB[0].x - segB[1].x);
        float tBy = (v == segB[0] ? segB[1].y - segB[0].y : segB[0].y - segB[1].y);
        float tAlen = (float) Math.hypot(tAx, tAy);
        float tBlen = (float) Math.hypot(tBx, tBy);
        if (tAlen == 0f || tBlen == 0f) return;
        tAx /= tAlen; tAy /= tAlen; tBx /= tBlen; tBy /= tBlen;

        // Нормали (радиальные направления к точке касания на окружности станции)
        float nAx = -tAy, nAy = tAx; // rotate +90
        float nBx = -tBy, nBy = tBx;
        // Выбор полюса: точка касания должна быть на стороне, обращённой друг к другу (к другой станции)
        float dABx = cB.x - cA.x, dABy = cB.y - cA.y;
        if (nAx * dABx + nAy * dABy < 0f) { nAx = -nAx; nAy = -nAy; }
        if (nBx * (-dABx) + nBy * (-dABy) < 0f) { nBx = -nBx; nBy = -nBy; }

        // Радиус опоры станции: берём как в частичных кругах перехода (20)
        float rStation = 20f;
        float taX = cA.x + nAx * rStation;
        float taY = cA.y + nAy * rStation;
        float tbX = cB.x + nBx * rStation;
        float tbY = cB.y + nBy * rStation;

        // Теперь строим универсальную кубическую Безье между точками касания, с касательными tA, tB
        // Пересчёт под новую пару концов
        PointF ta = new PointF(taX, taY);
        PointF tb = new PointF(tbX, tbY);

        // Ориентация касательных по направлению к другой точке касания
        float chx = tb.x - ta.x, chy = tb.y - ta.y;
        if (tAx * chx + tAy * chy < 0f) { tAx = -tAx; tAy = -tAy; }
        if (tBx * (-chx) + tBy * (-chy) < 0f) { tBx = -tBx; tBy = -tBy; }

        // Угол и длина контролей как в универсальной формуле
        float cosPhi = -(tAx * tBx + tAy * tBy);
        if (cosPhi > 1f) cosPhi = 1f; else if (cosPhi < -1f) cosPhi = -1f;
        float phi = (float) Math.acos(cosPhi);
        float chordDx = tb.x - ta.x;
        float chordDy = tb.y - ta.y;
        float L = (float) Math.hypot(chordDx, chordDy);
        if (L == 0f) return;
        float sinHalf = (float) Math.sin(phi / 2f);
        float d;
        if (sinHalf < 1e-3f) {
            d = 0.5f * L;
        } else {
            float R = L / (2f * sinHalf);
            d = (float) ((4.0 / 3.0) * Math.tan(phi / 4f) * R);
        }
        float dMin = 0.15f * L;
        float dMax = 0.5f * L;
        if (d < dMin) d = dMin; else if (d > dMax) d = dMax;

        float c1x = ta.x + tAx * d;
        float c1y = ta.y + tAy * d;
        float c2x = tb.x + tBx * d;
        float c2y = tb.y + tBy * d;

        angularOverlay.moveTo(ta.x, ta.y);
        angularOverlay.cubicTo(c1x, c1y, c2x, c2y, tb.x, tb.y);
        debugConn.moveTo(ta.x, ta.y);
        debugConn.cubicTo(c1x, c1y, c2x, c2y, tb.x, tb.y);
    }

    private void addCircularArcFromMainLines(Path angularOverlay, Path debugConn,
                                             PointF u, PointF v,
                                             int segAIdx, int segBIdx,
                                             List<PointF[]> mainSegmentsForLog,
                                             List<Station> transferStations,
                                             float centerX, float centerY) {
        // Направления основных линий в концах u и v
        PointF[] segA = mainSegmentsForLog.get(segAIdx);
        PointF[] segB = mainSegmentsForLog.get(segBIdx);
        float tAx = (u == segA[0] ? segA[1].x - segA[0].x : segA[0].x - segA[1].x);
        float tAy = (u == segA[0] ? segA[1].y - segA[0].y : segA[0].y - segA[1].y);
        float tBx = (v == segB[0] ? segB[1].x - segB[0].x : segB[0].x - segB[1].x);
        float tBy = (v == segB[0] ? segB[1].y - segB[0].y : segB[0].y - segB[1].y);
        float tAlen = (float) Math.hypot(tAx, tAy);
        float tBlen = (float) Math.hypot(tBx, tBy);
        if (tAlen == 0f || tBlen == 0f) return;
        tAx /= tAlen; tAy /= tAlen; tBx /= tBlen; tBy /= tBlen;

        // Угол между tA и -tB (схождение к хорде)
        float cosAngle = -(tAx * tBx + tAy * tBy);
        if (cosAngle > 1f) cosAngle = 1f; else if (cosAngle < -1f) cosAngle = -1f;
        float phi = (float) Math.acos(cosAngle); // радианы в [0,pi]
        float chordDx = v.x - u.x;
        float chordDy = v.y - u.y;
        float D = (float) Math.hypot(chordDx, chordDy);
        if (D == 0f) return;

        // Радиус окружности и расстояние от середины хорды до центра окружности
        float sinHalf = (float) Math.sin(phi / 2f);
        if (sinHalf < 1e-4f) {
            // Почти параллельные: выбираем наружную сторону по центроиду и строим короткую дугу Безье
            float mx = (u.x + v.x) / 2f; float my = (u.y + v.y) / 2f;
            float nx = -chordDy / D; float ny = chordDx / D;
            float toCenterX = centerX - mx; float toCenterY = centerY - my;
            // хотим наружу: вершина должна быть вне зоны между основными линиями
            float h = 0.1f * D;
            float cx1 = mx + nx * h; float cy1 = my + ny * h;
            PointF peak1 = new PointF(cx1, cy1);
            boolean outside1 = isOutsideAngleZoneBySegments(segA, segB, peak1);
            if (!outside1) { nx = -nx; ny = -ny; cx1 = mx + nx * h; cy1 = my + ny * h; }
            angularOverlay.moveTo(u.x, u.y);
            angularOverlay.quadTo(cx1, cy1, v.x, v.y);
            debugConn.moveTo(u.x, u.y);
            debugConn.quadTo(cx1, cy1, v.x, v.y);
            // Логируем состав пересадки и геометрию
            StringBuilder sb = new StringBuilder();
            sb.append("ParallelTransfer: stations=");
            for (int ii=0; ii<transferStations.size(); ii++) {
                Station s = transferStations.get(ii);
                sb.append(s.getName()); if (ii < transferStations.size()-1) sb.append(", ");
            }
            sb.append(" | u=(").append(u.x).append(",").append(u.y).append(") v=(").append(v.x).append(",").append(v.y).append(")");
            sb.append(" peakBefore=(").append(peak1.x).append(",").append(peak1.y).append(") outsideBefore=").append(outside1);
            Log.d("ArcFixParallel", sb.toString());
            return;
        }
        float radius = D / (2f * sinHalf);
        float dCenter = (float) (radius * Math.cos(phi / 2f));

        // Нормаль к хорде в сторону центра группы
        float nx = -chordDy / D; float ny = chordDx / D;
        float mx = (u.x + v.x) / 2f; float my = (u.y + v.y) / 2f;
        float toCenterX = centerX - mx; float toCenterY = centerY - my;
        if (nx * toCenterX + ny * toCenterY < 0f) { nx = -nx; ny = -ny; }

        // Центр окружности (первичная сторона)
        float cxArc = mx + nx * dCenter;
        float cyArc = my + ny * dCenter;

        // Углы и sweep
        float startAngle = (float) Math.toDegrees(Math.atan2(u.y - cyArc, u.x - cxArc));
        float endAngle = (float) Math.toDegrees(Math.atan2(v.y - cyArc, v.x - cxArc));
        float sweep = endAngle - startAngle;
        while (sweep > 180f) sweep -= 360f;
        while (sweep < -180f) sweep += 360f;

        // Проверяем вершину дуги; если внутри угла — переносим центр на другую сторону (flip нормали)
        float midAngle = (float) Math.toRadians(startAngle + sweep / 2f);
        float peakX = cxArc + radius * (float) Math.cos(midAngle);
        float peakY = cyArc + radius * (float) Math.sin(midAngle);
        boolean outside = !isPointInQuadrilateralBySegments(segA, segB, new PointF(peakX, peakY));
        StringBuilder sb1 = new StringBuilder();
        sb1.append("ArcOrientation: stations=");
        for (int ii=0; ii<transferStations.size(); ii++) { Station s = transferStations.get(ii); sb1.append(s.getName()); if (ii<transferStations.size()-1) sb1.append(", "); }
        sb1.append(" | peakBefore=(").append(peakX).append(",").append(peakY).append(") outsideBefore=").append(outside)
           .append(" center=(").append(cxArc).append(",").append(cyArc).append(") R=").append(radius)
           .append(" start=").append(startAngle).append(" sweep=").append(sweep);
        Log.d("ArcOrientation", sb1.toString());
        if (!outside) {
            nx = -nx; ny = -ny;
            cxArc = mx + nx * dCenter;
            cyArc = my + ny * dCenter;
            startAngle = (float) Math.toDegrees(Math.atan2(u.y - cyArc, u.x - cxArc));
            endAngle = (float) Math.toDegrees(Math.atan2(v.y - cyArc, v.x - cxArc));
            sweep = endAngle - startAngle;
            while (sweep > 180f) sweep -= 360f;
            while (sweep < -180f) sweep += 360f;
            midAngle = (float) Math.toRadians(startAngle + sweep / 2f);
            peakX = cxArc + radius * (float) Math.cos(midAngle);
            peakY = cyArc + radius * (float) Math.sin(midAngle);
            outside = !isPointInQuadrilateralBySegments(segA, segB, new PointF(peakX, peakY));
            StringBuilder sb2 = new StringBuilder();
            sb2.append("ArcAfterFlip: stations=");
            for (int ii=0; ii<transferStations.size(); ii++) { Station s = transferStations.get(ii); sb2.append(s.getName()); if (ii<transferStations.size()-1) sb2.append(", "); }
            sb2.append(" | peakAfter=(").append(peakX).append(",").append(peakY).append(") outsideAfter=").append(outside)
               .append(" center=(").append(cxArc).append(",").append(cyArc).append(") start=").append(startAngle).append(" sweep=").append(sweep);
            Log.d("ArcAfterFlip", sb2.toString());
            if (!outside) sweep = -sweep;
        }

        // Финальные логи
        float finalMid = (float) Math.toRadians(startAngle + sweep / 2f);
        float finalPeakX = cxArc + radius * (float) Math.cos(finalMid);
        float finalPeakY = cyArc + radius * (float) Math.sin(finalMid);
        boolean finalOutside = !isPointInQuadrilateralBySegments(segA, segB, new PointF(finalPeakX, finalPeakY));
        StringBuilder sb3 = new StringBuilder();
        sb3.append("ArcFinal: stations=");
        for (int ii=0; ii<transferStations.size(); ii++) { Station s = transferStations.get(ii); sb3.append(s.getName()); if (ii<transferStations.size()-1) sb3.append(", "); }
        sb3.append(" | peakFinal=(").append(finalPeakX).append(",").append(finalPeakY).append(") outsideFinal=").append(finalOutside)
           .append(" center=(").append(cxArc).append(",").append(cyArc).append(") start=").append(startAngle).append(" sweep=").append(sweep);
        Log.d("ArcFinal", sb3.toString());

        RectF oval = new RectF(cxArc - radius, cyArc - radius, cxArc + radius, cyArc + radius);
        angularOverlay.moveTo(u.x, u.y);
        angularOverlay.arcTo(oval, startAngle, sweep, false);
        debugConn.moveTo(u.x, u.y);
        debugConn.arcTo(oval, startAngle, sweep, false);
    }

    private void addShiftedLineToCache(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;
        float shiftX = (dy / length) * 20;
        float shiftY = -(dx / length) * 20;
        shiftX = -shiftX;
        shiftY = -shiftY;


        // Build stroke path (center line)
        float x1o = x1 + shiftX;
        float y1o = y1 + shiftY;
        float x2o = x2 + shiftX;
        float y2o = y2 + shiftY;

        Path outline = new Path();
        outline.moveTo(x1o, y1o);
        outline.lineTo(x2o, y2o);
        pathCache.transfersPath.addPath(outline);
        // Сохраняем красный сегмент для дальнейшей классификации угловых/основных
        if (pathCache.transferStrokes != null) {
            pathCache.transferStrokes.add(new StrokeSegment(new PointF(x1o, y1o), new PointF(x2o, y2o)));
        }

        // Build fill quad around the shifted line
        float dxo = x2o - x1o;
        float dyo = y2o - y1o;
        float llen = (float) Math.sqrt(dxo * dxo + dyo * dyo);
        if (llen == 0) {
            Log.d("TransfersFill", "zero length segment, skip fill");
            return;
        }
        float ux = -dyo / llen;
        float uy = dxo / llen;
        float half = Math.max(transferPaint.getStrokeWidth() / 2f, 3f);

        float x1a = x1o + ux * half;
        float y1a = y1o + uy * half;
        float x1b = x1o - ux * half;
        float y1b = y1o - uy * half;
        float x2a = x2o + ux * half;
        float y2a = y2o + uy * half;
        float x2b = x2o - ux * half;
        float y2b = y2o - uy * half;

        Path fill = new Path();
        fill.moveTo(x1a, y1a);
        fill.lineTo(x2a, y2a);
        fill.lineTo(x2b, y2b);
        fill.lineTo(x1b, y1b);
        fill.close();

        pathCache.transfersFillPath.addPath(fill);
        RectF b = new RectF();
        fill.computeBounds(b, true);
        Log.d("TransfersFill", "added quad=" + b.toString());
    }

    private void addDashedLineToCache(float x1, float y1, float x2, float y2) {

        // Для пунктирных линий используем специальный Path
        pathCache.dashedTransfersPath.moveTo(x1, y1);
        pathCache.dashedTransfersPath.lineTo(x2, y2);
    }

    private void addHalfColoredLineToCache(float x1, float y1, float x2, float y2, String color1, String color2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float shiftX = (dy / length) * 20;
        float shiftY = -(dx / length) * 20;
        shiftX = -shiftX;
        shiftY = -shiftY;

        float halfX = (x1 + x2) / 2;
        float halfY = (y1 + y2) / 2;


        // Красим левую половину в color1, правую в color2, в центре создаём плавный переход
        Paint gradPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradPaint1.setStyle(Paint.Style.STROKE);
        gradPaint1.setStrokeWidth(Math.max(transferPaint.getStrokeWidth(), 6f));
        gradPaint1.setShader(new LinearGradient(x1 + shiftX, y1 + shiftY, halfX + shiftX, halfY + shiftY,
                parseColorSafely(color1, mapTextColor), parseColorSafely(color2, mapTextColor), Shader.TileMode.CLAMP));
        Path left = new Path();
        left.moveTo(x1 + shiftX, y1 + shiftY);
        left.lineTo(halfX + shiftX, halfY + shiftY);
        pathCache.transfersFillOverlayMainPath.addPath(left);

        Paint gradPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradPaint2.setStyle(Paint.Style.STROKE);
        gradPaint2.setStrokeWidth(Math.max(transferPaint.getStrokeWidth(), 6f));
        gradPaint2.setShader(new LinearGradient(halfX + shiftX, halfY + shiftY, x2 + shiftX, y2 + shiftY,
                parseColorSafely(color2, mapTextColor), parseColorSafely(color1, mapTextColor), Shader.TileMode.CLAMP));
        Path right = new Path();
        right.moveTo(halfX + shiftX, halfY + shiftY);
        right.lineTo(x2 + shiftX, y2 + shiftY);
        pathCache.transfersFillOverlayMainPath.addPath(right);
    }

    private Line findLineForStation(Station station) {
        for (Line line : lines) {
            if (line.getStations().contains(station)) {
                return line;
            }
        }
        for (Line line : suburbanLines) {
            if (line.getStations().contains(station)) {
                return line;
            }
        }
        for (Line line : riverTramLines) {
            if (line.getStations().contains(station)) {
                return line;
            }
        }
        for (Line line : tramLines) {
            if (line.getStations().contains(station)) {
                return line;
            }
        }
        return null;
    }

    private RoutePathCache routePathCache = new RoutePathCache();

    private void updateRouteCache() {
        if (route == null || route.isEmpty()) {
            routePathCache.routeLinesPaths.clear();
            routePathCache.routeStationsPaths.clear();
            routePathCache.transfersPath.reset();

            routePathCache.dashedTransfersPath.reset();
            routePathCache.partialCircles.clear();

            routePathCache.transfersFillPath.reset();
            routePathCache.transfersFillOverlayMainPath.reset();
            routePathCache.transfersFillOverlayAngularPath.reset();
            routePathCache.convexHullPath.reset();
            routePathCache.crossStrokes.clear();
            routePathCache.isInitialized = false;
            return;
        }

        // Очищаем кэш перед построением нового маршрута
        routePathCache.routeLinesPaths.clear();
        routePathCache.routeStationsPaths.clear();

        routePathCache.routeFaintStationsPaths.clear();
        routePathCache.transfersPath.reset();

        routePathCache.dashedTransfersPath.reset();
        routePathCache.partialCircles.clear();

        routePathCache.transfersFillPath.reset();
        routePathCache.transfersFillOverlayMainPath.reset();
        routePathCache.transfersFillOverlayAngularPath.reset();
        routePathCache.convexHullPath.reset();
        routePathCache.crossStrokes.clear();
        routePathCache.routeLineSegments.clear();

        // Построение нового маршрута
        for (int i = 0; i < route.size() - 1; i++) {
            Station station1 = route.get(i);
            Station station2 = route.get(i + 1);
            Line line = findLineForConnection(station1, station2);

            if (line != null) {

                LinePath lp = buildRouteLinePath(station1, station2, line.getLineType(), line.getColor());
                lp.line = line;
                routePathCache.routeLinesPaths.add(lp);
            } else {

                // Это переход между станциями — построим всё как для общей карты, но в routePathCache
                buildRouteTransferBetween(station1, station2);
            }
        }

        // Группируем станции по линиям для отображения индикаторов
        if (route.size() > 1) {
            int segmentStart = 0;
            Line currentLine = null;
            
            for (int i = 0; i < route.size() - 1; i++) {
                Station station1 = route.get(i);
                Station station2 = route.get(i + 1);
                Line line = findLineForConnection(station1, station2);
                
                if (line != null) {
                    if (currentLine == null || !currentLine.getId().equals(line.getId())) {
                        if (currentLine != null && segmentStart < i) {
                            Path segmentPath = buildContinuousPathForSegment(route, segmentStart, i, currentLine);
                            routePathCache.routeLineSegments.add(new RouteLineSegment(currentLine, segmentStart, i, segmentPath));
                        }
                        currentLine = line;
                        segmentStart = i;
                    }
                } else {
                    if (currentLine != null && segmentStart < i) {
                        Path segmentPath = buildContinuousPathForSegment(route, segmentStart, i, currentLine);
                        routePathCache.routeLineSegments.add(new RouteLineSegment(currentLine, segmentStart, i, segmentPath));
                    }
                    currentLine = null;
                }
            }
            
            if (currentLine != null && segmentStart < route.size() - 1) {
                Path segmentPath = buildContinuousPathForSegment(route, segmentStart, route.size() - 1, currentLine);
                routePathCache.routeLineSegments.add(new RouteLineSegment(currentLine, segmentStart, route.size() - 1, segmentPath));
            }
        }

        // Добавляем станции маршрута в кэш
        for (Station station : route) {
            Path stationPath = new Path();
            stationPath.addCircle(
                    station.getX() * currentCoordinateScaleFactor,
                    station.getY() * currentCoordinateScaleFactor,
                    14,
                    Path.Direction.CW
            );

            Float labelXScaled = null;
            Float labelYScaled = null;
            if (station.hasLabelCoordinates()) {
                labelXScaled = station.getLabelX() * currentCoordinateScaleFactor;
                labelYScaled = station.getLabelY() * currentCoordinateScaleFactor;
            }
            routePathCache.routeStationsPaths.add(new StationPath(stationPath, station.getColor(), station.getTextPosition(), station.getName(), labelXScaled, labelYScaled));
        }

        // Добавим бледные точки всех станций, входящих в переходы маршрута, но не являющихся текущими в route
        Set<String> routeIds = new java.util.HashSet<>();
        for (Station s : route) routeIds.add(s.getId());
        for (Transfer t : transfers) {
            List<Station> ts = t.getStations(); if (ts == null || ts.size() < 2) continue;
            // если переход участвует в маршруте (есть пара соседних станций в route)
            boolean used = false;
            for (int i = 0; i < route.size()-1 && !used; i++) {
                if (ts.contains(route.get(i)) && ts.contains(route.get(i+1))) used = true;
            }
            if (!used) continue;
            for (Station s : ts) {
                if (routeIds.contains(s.getId())) continue;
                Path sp = new Path();
                sp.addCircle(s.getX()*COORDINATE_SCALE_FACTOR, s.getY()*COORDINATE_SCALE_FACTOR, 14, Path.Direction.CW);
                String col = s.getColor();
                Float labelXScaled = null;
                Float labelYScaled = null;
                if (s.hasLabelCoordinates()) {
                    labelXScaled = s.getLabelX() * currentCoordinateScaleFactor;
                    labelYScaled = s.getLabelY() * currentCoordinateScaleFactor;
                }
                routePathCache.routeFaintStationsPaths.add(new StationPath(sp, col, s.getTextPosition(), s.getName(), labelXScaled, labelYScaled));
            }
        }

        routePathCache.isInitialized = true;
    }


    private void handleRouteLinkTransfer(Transfer transfer) {
        // Центры переходов
        List<PointF> centers = new ArrayList<>();
        if (transfer.getLinkedTransferIds() != null) {
            for (String transferId : transfer.getLinkedTransferIds()) {
                PointF center = calculateTransferCenter(transferId);
                if (center != null) centers.add(center);
            }
        }

        // Станции-анкеры
        List<String> stationIds = transfer.getLinkedStationIds();
        if (stationIds != null && !stationIds.isEmpty() && !centers.isEmpty()) {
            PointF from = centers.get(0);
            for (String sid : stationIds) {
                Station st = findStationByIdAcrossAll(sid);
                if (st == null) continue;
                float x = st.getX() * currentCoordinateScaleFactor;
                float y = st.getY() * currentCoordinateScaleFactor;
                String type = transfer.getType().toLowerCase();
                Path p = new Path(); p.moveTo(from.x, from.y); p.lineTo(x, y);
                if ("ground".equals(type)) routePathCache.dashedTransfersPath.addPath(p); else routePathCache.transfersPath.addPath(p);
            }
            return;
        }

        if (centers.size() >= 2) {
            PointF firstCenter = centers.get(0);
            for (int i = 1; i < centers.size(); i++) {
                PointF otherCenter = centers.get(i);
                String type = transfer.getType().toLowerCase();
                Path p = new Path(); p.moveTo(firstCenter.x, firstCenter.y); p.lineTo(otherCenter.x, otherCenter.y);
                if ("ground".equals(type)) routePathCache.dashedTransfersPath.addPath(p); else routePathCache.transfersPath.addPath(p);
            }
        }
    }

    private Station findStationByIdAcrossAll(String id) {
        if (stations != null) {
            for (Station s : stations) if (id.equals(s.getId())) return s;
        }
        if (suburbanStations != null) {
            for (Station s : suburbanStations) if (id.equals(s.getId())) return s;
        }
        if (riverTramStations != null) {
            for (Station s : riverTramStations) if (id.equals(s.getId())) return s;
        }
        return null;
    }

    private void buildRouteTransferBetween(Station a, Station b) {
        Transfer match = null;
        for (Transfer t : transfers) {
            List<Station> ts = t.getStations();
            if (ts != null && ts.contains(a) && ts.contains(b)) {
                match = t;
                break;
            }
        }
        if (match == null) {
            return;
        }

        if (match.isLinkTransfer() && match.getLinkedTransferIds() != null && match.getLinkedTransferIds().size() >= 2) {
            handleRouteLinkTransfer(match);
            return;
        }

        MapPathCache originalCache = pathCache;
        MapPathCache tempCache = new MapPathCache();
        pathCache = tempCache;
        try {
            addTransferPathToCache(match);
        } finally {
            pathCache = originalCache;
        }

        routePathCache.transfersPath.addPath(tempCache.transfersPath);
        routePathCache.dashedTransfersPath.addPath(tempCache.dashedTransfersPath);
        routePathCache.transfersFillPath.addPath(tempCache.transfersFillPath);
        routePathCache.transfersFillOverlayMainPath.addPath(tempCache.transfersFillOverlayMainPath);
        routePathCache.transfersFillOverlayAngularPath.addPath(tempCache.transfersFillOverlayAngularPath);
        routePathCache.convexHullPath.addPath(tempCache.convexHullPath);
        routePathCache.partialCircles.addAll(tempCache.partialCircles);
        routePathCache.crossStrokes.addAll(tempCache.crossStrokes);
    }

    private void drawRouteStationText(Canvas canvas, StationPath routeStationPath) {

        // Для переходной станции с pos==9 саму подпись не рисуем — её рисуем через fallback выше
        if (routeStationPath.textPosition == 9) return;
        // Создаем paint для текста

        Paint textPaint = new Paint(this.textPaint);
        // В режиме маршрута используем светлый текст (инвертируем цвет текста)
        int routeTextColor = ColorUtils.blendARGB(mapTextColor, mapBackgroundColor, 0.5f);
        if (isDarkTheme()) {
            routeTextColor = Color.WHITE; // В темной теме белый текст на маршруте
        } else {
            routeTextColor = Color.BLACK; // В светлой теме черный текст на маршруте
        }
        textPaint.setColor(routeTextColor);

        // Получаем координаты центра станции
        RectF bounds = new RectF();
        routeStationPath.path.computeBounds(bounds, true);
        float stationX = bounds.centerX();
        float stationY = bounds.centerY();
        float textOffset = 50; // Смещение текста от станции

        // Позиционируем текст в зависимости от textPosition
        float textX = stationX;
        float textY = stationY;
        Paint.Align textAlign = Paint.Align.CENTER;


        boolean hasCustomPosition = routeStationPath.hasCustomLabelPosition();
        if (hasCustomPosition) {
            textX = routeStationPath.labelX;
            textY = routeStationPath.labelY;
            textAlign = Paint.Align.CENTER;
        } else {
            switch (routeStationPath.textPosition) {
                case 0: // center
                    textAlign = Paint.Align.CENTER;
                    break;
                case 1: // 12 o'clock

                    textY -= (textOffset - 5f);
                    textAlign = Paint.Align.CENTER;
                    break;
                case 2: // 1:30

                    textX += (textOffset * 0.7f - 5f);
                    textY -= (textOffset * 0.7f - 5f);
                    textAlign = Paint.Align.LEFT;
                    break;
                case 3: // 3 o'clock

                    textX += (textOffset - 5f);
                    textAlign = Paint.Align.LEFT;
                    break;
                case 4: // 4:30

                    textX += (textOffset * 0.7f - 5f);
                    textY += (textOffset * 0.7f - 5f);
                    textAlign = Paint.Align.LEFT;
                    break;
                case 5: // 6 o'clock

                    textY += (textOffset - 5f);
                    textAlign = Paint.Align.CENTER;
                    break;
                case 6: // 7:30

                    textX -= (textOffset * 0.7f - 5f);
                    textY += (textOffset * 0.7f - 5f);
                    textAlign = Paint.Align.RIGHT;
                    break;
                case 7: // 9 o'clock

                    textX -= (textOffset - 5f);
                    textAlign = Paint.Align.RIGHT;
                    break;
                case 8: // 10:30

                    textX -= (textOffset * 0.7f - 5f);
                    textY -= (textOffset * 0.7f - 5f);
                    textAlign = Paint.Align.RIGHT;
                    break;
                default:
                    return; // Не отображать текст

            }
        }

        // Устанавливаем выравнивание текста
        textPaint.setTextAlign(textAlign);

        // Добавляем небольшое вертикальное смещение для центрирования текста
        textY += textPaint.getTextSize() / 3;


        Paint outlinePaint2 = new Paint(textPaint);
        outlinePaint2.setStyle(Paint.Style.STROKE);
        outlinePaint2.setStrokeWidth(6f);
        // Обводка должна контрастировать с текстом
        int outlineColor = isDarkTheme() ? Color.BLACK : Color.WHITE;
        outlinePaint2.setColor(outlineColor);
        outlinePaint2.setStrokeJoin(Paint.Join.ROUND);
        outlinePaint2.setStrokeMiter(10f);

        // Текст с обводкой в режиме маршрута
        textPaint.setColor(routeTextColor);

        String[] lines = routeStationPath.stationName.split("\\n");
        float lineHeight = textPaint.getTextSize() * 1.2f;
        float totalHeight = lineHeight * lines.length;
        float startY = textY - (totalHeight - lineHeight) / 2f;

        for (int i = 0; i < lines.length; i++) {
            float ly = startY + i * lineHeight;
            canvas.drawText(lines[i], textX, ly, outlinePaint2);
            canvas.drawText(lines[i], textX, ly, textPaint);
        }
    }

    // Находим «сестру» станции в переходе, у которой textPosition != 9
    private Station findLabelStationForTransferByName(String stationNameOrId) {
        if (transfers == null) return null;
        // Сначала ищем по ID
        Station byId = findStationByIdAcrossAll(stationNameOrId);
        if (byId != null) {
            for (Transfer t : transfers) {
                List<Station> tStations = t.getStations();
                if (tStations == null) continue;
                boolean has = false;
                for (Station s : tStations) {
                    if (s.getId().equals(byId.getId())) { has = true; break; }
                }
                if (!has) continue;
                for (Station s : tStations) {
                    if (s.getTextPosition() != 9) return s;
                }
            }
        }
        // Потом — по имени
        for (Transfer t : transfers) {
            List<Station> tStations = t.getStations();
            if (tStations == null) continue;
            boolean has = false;
            for (Station s : tStations) {
                if (s.getName().equals(stationNameOrId)) { has = true; break; }
            }
            if (!has) continue;
            for (Station s : tStations) {
                if (s.getTextPosition() != 9) return s;
            }
        }
        return null;
    }

    // Пытаемся найти имя связанной станции (для переходов), где textPosition != 9
    private String findLinkedStationNameForTransfer(String stationNameOrId) {
        if (stations == null || transfers == null) return null;
        // Сначала попробуем по ID
        Station byId = findStationByIdAcrossAll(stationNameOrId);
        if (byId != null) {
            // Найти все станции со схожим именем в пределах одного перехода
            for (Transfer t : transfers) {
                List<Station> tStations = t.getStations();
                if (tStations == null) continue;
                boolean contains = false;
                for (Station s : tStations) {
                    if (s.getId().equals(byId.getId())) { contains = true; break; }
                }
                if (!contains) continue;
                for (Station s : tStations) {
                    if (s.getTextPosition() != 9) {
                        return s.getName();
                    }
                }
            }
        }
        // Иначе, если пришло имя — ищем в переходе первую станцию с позицией != 9
        for (Transfer t : transfers) {
            List<Station> tStations = t.getStations();
            if (tStations == null) continue;
            boolean contains = false;
            for (Station s : tStations) {
                if (s.getName().equals(stationNameOrId)) { contains = true; break; }
            }
            if (!contains) continue;
            for (Station s : tStations) {
                if (s.getTextPosition() != 9) {
                    return s.getName();
                }
            }
        }
        return null;
    }

    private void addRouteTransferPathToCache(Station station1, Station station2) {
        List<Station> stations = Arrays.asList(station1, station2);

        // Массив для хранения координат станций
        float[] coordinates = new float[stations.size() * 2];
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            float x = station.getX() * currentCoordinateScaleFactor;
            float y = station.getY() * currentCoordinateScaleFactor;
            coordinates[i * 2] = x;
            coordinates[i * 2 + 1] = y;
        }

        // Отрисовка линии перехода
        float x1 = coordinates[0];
        float y1 = coordinates[1];
        float x2 = coordinates[2];
        float y2 = coordinates[3];

        // Вычисляем перпендикулярный вектор для смещения
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float shiftX = (dy / length) * 20;
        float shiftY = -(dx / length) * 20;
        shiftX = -shiftX;
        shiftY = -shiftY;

        // Создаем замкнутый четырехугольник
        Path transferPath = new Path();
        transferPath.moveTo(x1 + shiftX, y1 + shiftY);  // Верхняя точка первой линии
        transferPath.lineTo(x2 + shiftX, y2 + shiftY);  // Верхняя точка второй линии
        transferPath.lineTo(x2 - shiftX, y2 - shiftY);  // Нижняя точка второй линии
        transferPath.lineTo(x1 - shiftX, y1 - shiftY);  // Нижняя точка первой линии
        transferPath.close();  // Замыкаем фигуру

        // Добавляем путь в кэш
        routePathCache.transfersPath.addPath(transferPath);

        // Добавляем частичные круги
        for (int i = 0; i < stations.size(); i++) {
            int prevIndex = (i - 1 + stations.size()) % stations.size();
            int nextIndex = (i + 1) % stations.size();
            float currentX = coordinates[i * 2];
            float currentY = coordinates[i * 2 + 1];
            float prevX = coordinates[prevIndex * 2];
            float prevY = coordinates[prevIndex * 2 + 1];
            float nextX = coordinates[nextIndex * 2];
            float nextY = coordinates[nextIndex * 2 + 1];
            List<Float> angles = getAngle(prevX, prevY, currentX, currentY, nextX, nextY);

            if (angles == null || angles.size() < 2) {
                continue;
            }
            float sweep = angles.get(0);
            if (Float.isNaN(sweep) || Math.abs(sweep) < 1f || Math.abs(sweep) >= 359f) {
                continue;
            }

            routePathCache.partialCircles.add(new PartialCircle(
                    currentX, currentY, 20, 6,
                    angles, "#cccccc"  // Используем белый цвет для переходов в маршруте
            ));
        }
    }

    private void updatePathCache() {
        // Очищаем кэш перед обновлением
        pathCache.linesPaths.clear();
        pathCache.stationsPaths.clear();
        pathCache.transfersPath.reset();

        pathCache.dashedTransfersPath.reset();
        pathCache.transfersFillPath.reset();
        pathCache.transfersFillOverlayMainPath.reset();
        pathCache.transfersFillOverlayAngularPath.reset();
        pathCache.debugMainSegmentsPath.reset();
        pathCache.debugConnectionsPath.reset();
        pathCache.transferStrokes.clear();
        pathCache.crossStrokes.clear();
        pathCache.riversPath.reset();
        pathCache.partialCircles.clear();
        pathCache.convexHullPath.reset();

        // Выбираем данные для отрисовки в зависимости от текущей карты
        if (isMetroMap) {
            drawColoredMap(lines, stations, transfers, rivers, mapObjects);
        } else if (isSuburbanMap) {
            drawColoredMap(suburbanLines, suburbanStations, suburbanTransfers, rivers, suburbanMapObjects);
        } else if (isRiverTramMap) {
            drawColoredMap(riverTramLines, riverTramStations, riverTramTransfers, rivers, riverTramMapObjects);
        } else if (isTramMap) {
            drawColoredMap(tramLines, tramStations, tramTransfers, rivers, tramMapObjects);
        }
    }

    private void drawColoredMap(List<Line> lines, List<Station> stations, List<Transfer> transfers, List<River> rivers, List<MapObject> mapObjects) {

        // Null-safety for all inputs
        if (lines == null) lines = java.util.Collections.emptyList();
        if (stations == null) stations = java.util.Collections.emptyList();
        if (transfers == null) transfers = java.util.Collections.emptyList();
        if (rivers == null) rivers = java.util.Collections.emptyList();
        if (mapObjects == null) mapObjects = java.util.Collections.emptyList();
        
        if (coordinateScaleAnimator == null || !coordinateScaleAnimator.isRunning()) {
        currentCoordinateScaleFactor = getCoordinateScaleFactor();
        }
        
        // Очищаем кэш перед отрисовкой
        pathCache.linesPaths.clear();
        pathCache.stationsPaths.clear();
        pathCache.transfersPath.reset();

        pathCache.dashedTransfersPath.reset();
        pathCache.transfersFillPath.reset();
        pathCache.transfersFillOverlayMainPath.reset();
        pathCache.transfersFillOverlayAngularPath.reset();
        pathCache.debugMainSegmentsPath.reset();
        pathCache.debugConnectionsPath.reset();
        pathCache.transferStrokes.clear();
        pathCache.riversPath.reset();
        pathCache.partialCircles.clear();
        pathCache.convexHullPath.reset();

        // Отрисовка рек
            for (River river : rivers) {
                List<Point> points = river.getPoints();

            if (points != null && points.size() >= 2) {
                    Path riverPath = new Path();
                    riverPath.moveTo(points.get(0).x * currentCoordinateScaleFactor, points.get(0).y * currentCoordinateScaleFactor);
                    for (int i = 1; i < points.size(); i++) {
                        riverPath.lineTo(points.get(i).x * currentCoordinateScaleFactor, points.get(i).y * currentCoordinateScaleFactor);
                    }
                    pathCache.riversPath.addPath(riverPath);
            }
        }

        // Отрисовка линий
        Set<String> drawnConnections = new HashSet<>();
        for (Line line : lines) {
            Path linePath = new Path();
            String lineColor = line.getColor();


            java.util.List<Station> lineStations = line.getStations();
            if (lineStations == null) {
                pathCache.linesPaths.add(new LinePath(linePath, lineColor));
                continue;
            }

            for (Station station : lineStations) {
                java.util.List<Station.Neighbor> neighborsList = station.getNeighbors();
                if (neighborsList == null) continue;
                for (Station.Neighbor neighbor : neighborsList) {
                    Station neighborStation = findStationById(neighbor.getStation().getId(), stations);
                    if (neighborStation != null && line.getLineIdForStation(neighborStation) != null) {
                        String connectionKey = station.getId().compareTo(neighborStation.getId()) < 0
                                ? station.getId() + "-" + neighborStation.getId()
                                : neighborStation.getId() + "-" + station.getId();

                        if (!drawnConnections.contains(connectionKey)) {
                            addLinePathToCache(station, neighborStation, line.getLineType(), linePath);
                            drawnConnections.add(connectionKey);
                        }
                    }
                }
            }

            pathCache.linesPaths.add(new LinePath(linePath, lineColor));
        }

        // Отрисовка станций
        for (Station station : stations) {
            float stationX = station.getX() * currentCoordinateScaleFactor;
            float stationY = station.getY() * currentCoordinateScaleFactor;

            Line stationLine = findLineForStation(station);
            String stationColor = stationLine != null ? stationLine.getColor() : "#000000";

            Path stationPath = new Path();
            stationPath.addCircle(stationX, stationY, 14, Path.Direction.CW);


            Float labelXScaled = null;
            Float labelYScaled = null;
            if (station.hasLabelCoordinates()) {
                labelXScaled = station.getLabelX() * currentCoordinateScaleFactor;
                labelYScaled = station.getLabelY() * currentCoordinateScaleFactor;
            }
            pathCache.stationsPaths.add(new StationPath(stationPath, stationColor, station.getTextPosition(), station.getName(), labelXScaled, labelYScaled));
        }

        // Отрисовка переходов
        if (transfers != null) {
            for (Transfer transfer : transfers) {
                addTransferPathToCache(transfer);
            }
        }

//        // Отрисовка объектов
//        if (mapObjects != null) {
//            for (MapObject mapObject : mapObjects) {
//                drawMapObject(mapObject);
//            }
//        }

        pathCache.isInitialized = true;
    }

    private void addLinePathToCache(Station station1, Station station2, String lineType, Path linePath) {
        Station startStation = station1.getId().compareTo(station2.getId()) < 0 ? station1 : station2;
        Station endStation = station1.getId().compareTo(station2.getId()) < 0 ? station2 : station1;
        List<Point> intermediatePoints = startStation.getIntermediatePoints(endStation);

        if (intermediatePoints == null || intermediatePoints.isEmpty()) {
            if (lineType.equals("double")) {
                // Обработка двойной прямой линии
                addDoubleStraightLineToCache(startStation, endStation, linePath);
            } else {
                // Обработка обычной прямой линии
                addQuadrilateralLinePathToCache(startStation, endStation, linePath);
            }
        } else if (intermediatePoints.size() == 1) {
            // Обработка линии с одной промежуточной точкой
            addQuadrilateralWithIntermediatePointPathToCache(startStation, endStation, intermediatePoints.get(0), linePath);
        } else if (intermediatePoints.size() == 2 && lineType.equals("double")) {
            // Обработка двойной кривой линии
            addDoubleQuadrilateralBezierPathToCache(startStation, endStation, intermediatePoints, linePath);
        } else if (intermediatePoints.size() == 2) {
            // Обработка обычной кривой линии
            addQuadrilateralBezierPathToCache(startStation, endStation, intermediatePoints, linePath);
        }
    }

    private void addQuadrilateralLinePathToCache(Station station1, Station station2, Path linePath) {

        if (LINE_RENDERING_METHOD == LineRenderingMethod.POLYGONAL_OUTLINE) {
            // Для POLYGONAL_OUTLINE используем тот же подход, что и для кривых
            Point start = new Point(station1.getX(), station1.getY());
            Point end = new Point(station2.getX(), station2.getY());
            Path outlinePath = createStraightLinePolygonalOutline(start, end, getAdjustedLineWidth(LINE_WIDTH));
            linePath.addPath(outlinePath);
        } else {
            // Для других методов используем простой четырехугольник
            float x1 = station1.getX() * currentCoordinateScaleFactor;
            float y1 = station1.getY() * currentCoordinateScaleFactor;
            float x2 = station2.getX() * currentCoordinateScaleFactor;
            float y2 = station2.getY() * currentCoordinateScaleFactor;
            float dx = x2 - x1;
            float dy = y2 - y1;
            float length = (float) Math.sqrt(dx * dx + dy * dy);

            if (length == 0) return;
            float nx = -dy / length;
            float ny = dx / length;

            float offset = 6f;

            // Создаем четырехугольный путь
            linePath.moveTo(x1 + nx * offset, y1 + ny * offset);
            linePath.lineTo(x2 + nx * offset, y2 + ny * offset);
            linePath.lineTo(x2 - nx * offset, y2 - ny * offset);
            linePath.lineTo(x1 - nx * offset, y1 - ny * offset);
            linePath.close();

        }
    }

    private void addQuadrilateralWithIntermediatePointPathToCache(Station startStation, Station endStation, Point middlePoint, Path linePath) {

        if (LINE_RENDERING_METHOD == LineRenderingMethod.POLYGONAL_OUTLINE) {
            // Для POLYGONAL_OUTLINE используем тот же подход, что и для кривых
            // Создаем полигональный контур для двух сегментов прямой линии
            Point start = new Point(startStation.getX(), startStation.getY());
            Point middle = new Point(middlePoint.x, middlePoint.y);
            Point end = new Point(endStation.getX(), endStation.getY());
            
            Path path = new Path();
            List<PointF> leftPoints = new ArrayList<>();
            List<PointF> rightPoints = new ArrayList<>();
            
            float halfWidth = getAdjustedLineWidth(LINE_WIDTH) / 2;
            
            // Первый сегмент: от start до middle
            float dx1 = middle.x - start.x;
            float dy1 = middle.y - start.y;
            float length1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
            if (length1 > 0) {
                float nx1 = -dy1 / length1;
                float ny1 = dx1 / length1;
                
                for (float t = 0; t <= 1; t += BEZIER_SEGMENT_STEP) {
                    float pointX = start.x + t * (middle.x - start.x);
                    float pointY = start.y + t * (middle.y - start.y);
                    
                    float scaledX = pointX * currentCoordinateScaleFactor;
                    float scaledY = pointY * currentCoordinateScaleFactor;
                    float scaledOffsetX = nx1 * halfWidth * currentCoordinateScaleFactor;
                    float scaledOffsetY = ny1 * halfWidth * currentCoordinateScaleFactor;
                    
                    leftPoints.add(new PointF(scaledX + scaledOffsetX, scaledY + scaledOffsetY));
                    rightPoints.add(new PointF(scaledX - scaledOffsetX, scaledY - scaledOffsetY));
                }
            }
            
            // Второй сегмент: от middle до end
            float dx2 = end.x - middle.x;
            float dy2 = end.y - middle.y;
            float length2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);
            if (length2 > 0) {
                float nx2 = -dy2 / length2;
                float ny2 = dx2 / length2;
                
                for (float t = 0; t <= 1; t += BEZIER_SEGMENT_STEP) {
                    float pointX = middle.x + t * (end.x - middle.x);
                    float pointY = middle.y + t * (end.y - middle.y);
                    
                    float scaledX = pointX * currentCoordinateScaleFactor;
                    float scaledY = pointY * currentCoordinateScaleFactor;
                    float scaledOffsetX = nx2 * halfWidth * currentCoordinateScaleFactor;
                    float scaledOffsetY = ny2 * halfWidth * currentCoordinateScaleFactor;
                    
                    leftPoints.add(new PointF(scaledX + scaledOffsetX, scaledY + scaledOffsetY));
                    rightPoints.add(new PointF(scaledX - scaledOffsetX, scaledY - scaledOffsetY));
                }
            }
            
            if (!leftPoints.isEmpty()) {
                path.moveTo(leftPoints.get(0).x, leftPoints.get(0).y);
                for (int i = 1; i < leftPoints.size(); i++) {
                    path.lineTo(leftPoints.get(i).x, leftPoints.get(i).y);
                }
                for (int i = rightPoints.size() - 1; i >= 0; i--) {
                    path.lineTo(rightPoints.get(i).x, rightPoints.get(i).y);
                }
                path.close();
            }
            
            linePath.addPath(path);
        } else {
            // Для других методов используем простой многоугольник
            float startX = startStation.getX() * currentCoordinateScaleFactor;
            float startY = startStation.getY() * currentCoordinateScaleFactor;
            float middleX = middlePoint.x * currentCoordinateScaleFactor;
            float middleY = middlePoint.y * currentCoordinateScaleFactor;
            float endX = endStation.getX() * currentCoordinateScaleFactor;
            float endY = endStation.getY() * currentCoordinateScaleFactor;


            float offset = 6f;

            // Вычисляем перпендикулярные векторы для обоих сегментов
            float dx1 = middleX - startX;
            float dy1 = middleY - startY;
            float length1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);

            if (length1 == 0) return;
            float nx1 = -dy1 / length1;
            float ny1 = dx1 / length1;

            float dx2 = endX - middleX;
            float dy2 = endY - middleY;
            float length2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);

            if (length2 == 0) return;
            float nx2 = -dy2 / length2;
            float ny2 = dx2 / length2;

            // Создаем замкнутый четырехугольный путь
            linePath.moveTo(startX + nx1 * offset, startY + ny1 * offset);
            linePath.lineTo(middleX + nx1 * offset, middleY + ny1 * offset);
            linePath.lineTo(middleX + nx2 * offset, middleY + ny2 * offset);
            linePath.lineTo(endX + nx2 * offset, endY + ny2 * offset);
            linePath.lineTo(endX - nx2 * offset, endY - ny2 * offset);
            linePath.lineTo(middleX - nx2 * offset, middleY - ny2 * offset);
            linePath.lineTo(middleX - nx1 * offset, middleY - ny1 * offset);
            linePath.lineTo(startX - nx1 * offset, startY - ny1 * offset);
            linePath.close();

        }
    }

    private void addDoubleQuadrilateralBezierPathToCache(Station startStation, Station endStation, List<Point> intermediatePoints, Path linePath) {
        Point start = new Point(startStation.getX(), startStation.getY());
        Point control1 = intermediatePoints.get(0);
        Point control2 = intermediatePoints.get(1);
        Point end = new Point(endStation.getX(), endStation.getY());

        
        switch (LINE_RENDERING_METHOD) {
            case FIXED_STROKE_WIDTH:
                addDoubleQuadrilateralBezierPathToCacheFixedStrokeWidth(startStation, start, control1, control2, end);
                break;
            case POLYGONAL_OUTLINE:
                addDoubleQuadrilateralBezierPathToCachePolygonalOutline(startStation, start, control1, control2, end);
                break;
            case PARALLEL_PATHS:
                addDoubleQuadrilateralBezierPathToCacheParallelPaths(startStation, start, control1, control2, end);
                break;
        }
    }
    
    private void addDoubleQuadrilateralBezierPathToCacheFixedStrokeWidth(Station startStation, Point start, Point control1, Point control2, Point end) {
        float offset = getAdjustedLineWidth(DOUBLE_LINE_WIDTH) / 2 + DOUBLE_LINE_GAP / 2;
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;
        float nx = dy / length;
        float ny = -dx / length;

        // Верхняя кривая
        float x1Start = (start.x + nx * offset) * currentCoordinateScaleFactor;
        float y1Start = (start.y + ny * offset) * currentCoordinateScaleFactor;
        float x1Control1 = (control1.x + nx * offset) * currentCoordinateScaleFactor;
        float y1Control1 = (control1.y + ny * offset) * currentCoordinateScaleFactor;
        float x1Control2 = (control2.x + nx * offset) * currentCoordinateScaleFactor;
        float y1Control2 = (control2.y + ny * offset) * currentCoordinateScaleFactor;
        float x1End = (end.x + nx * offset) * currentCoordinateScaleFactor;
        float y1End = (end.y + ny * offset) * currentCoordinateScaleFactor;

        // Нижняя кривая
        float x2Start = (start.x - nx * offset) * currentCoordinateScaleFactor;
        float y2Start = (start.y - ny * offset) * currentCoordinateScaleFactor;
        float x2Control1 = (control1.x - nx * offset) * currentCoordinateScaleFactor;
        float y2Control1 = (control1.y - ny * offset) * currentCoordinateScaleFactor;
        float x2Control2 = (control2.x - nx * offset) * currentCoordinateScaleFactor;
        float y2Control2 = (control2.y - ny * offset) * currentCoordinateScaleFactor;
        float x2End = (end.x - nx * offset) * currentCoordinateScaleFactor;
        float y2End = (end.y - ny * offset) * currentCoordinateScaleFactor;

        // Создаем замкнутый четырехугольник из верхней и нижней кривых
        Path outerPath = new Path();
        outerPath.moveTo(x1Start, y1Start);
        outerPath.cubicTo(x1Control1, y1Control1, x1Control2, y1Control2, x1End, y1End);
        outerPath.lineTo(x2End, y2End);
        outerPath.cubicTo(x2Control2, y2Control2, x2Control1, y2Control1, x2Start, y2Start);
        outerPath.close();

        // Создаем внутреннюю белую кривую (четырехугольную)
        Path innerPath = new Path();

        float innerOffset = DOUBLE_LINE_GAP / 2;

        // Верхняя внутренняя кривая
        float x1InnerStart = (start.x + nx * innerOffset) * currentCoordinateScaleFactor;
        float y1InnerStart = (start.y + ny * innerOffset) * currentCoordinateScaleFactor;
        float x1InnerControl1 = (control1.x + nx * innerOffset) * currentCoordinateScaleFactor;
        float y1InnerControl1 = (control1.y + ny * innerOffset) * currentCoordinateScaleFactor;
        float x1InnerControl2 = (control2.x + nx * innerOffset) * currentCoordinateScaleFactor;
        float y1InnerControl2 = (control2.y + ny * innerOffset) * currentCoordinateScaleFactor;
        float x1InnerEnd = (end.x + nx * innerOffset) * currentCoordinateScaleFactor;
        float y1InnerEnd = (end.y + ny * innerOffset) * currentCoordinateScaleFactor;

        // Нижняя внутренняя кривая
        float x2InnerStart = (start.x - nx * innerOffset) * currentCoordinateScaleFactor;
        float y2InnerStart = (start.y - ny * innerOffset) * currentCoordinateScaleFactor;
        float x2InnerControl1 = (control1.x - nx * innerOffset) * currentCoordinateScaleFactor;
        float y2InnerControl1 = (control1.y - ny * innerOffset) * currentCoordinateScaleFactor;
        float x2InnerControl2 = (control2.x - nx * innerOffset) * currentCoordinateScaleFactor;
        float y2InnerControl2 = (control2.y - ny * innerOffset) * currentCoordinateScaleFactor;
        float x2InnerEnd = (end.x - nx * innerOffset) * currentCoordinateScaleFactor;
        float y2InnerEnd = (end.y - ny * innerOffset) * currentCoordinateScaleFactor;

        // Создаем замкнутый четырехугольник для внутренней кривой
        innerPath.moveTo(x1InnerStart, y1InnerStart);
        innerPath.cubicTo(x1InnerControl1, y1InnerControl1, x1InnerControl2, y1InnerControl2, x1InnerEnd, y1InnerEnd);
        innerPath.lineTo(x2InnerEnd, y2InnerEnd);
        innerPath.cubicTo(x2InnerControl2, y2InnerControl2, x2InnerControl1, y2InnerControl1, x2InnerStart, y2InnerStart);
        innerPath.close();


        // Устанавливаем цвет для внутренней кривой
        Paint whitePaint = new Paint();

        whitePaint.setColor(mapStationFillColor);
        whitePaint.setStyle(Paint.Style.FILL);

        // Добавляем внешний и внутренний пути в кэш
        pathCache.linesPaths.add(new LinePath(outerPath, startStation.getColor(), innerPath, whitePaint));
    }

    
    private void addDoubleQuadrilateralBezierPathToCachePolygonalOutline(Station startStation, Point start, Point control1, Point control2, Point end) {
        float outerOffset = getAdjustedLineWidth(DOUBLE_LINE_WIDTH) / 2 + DOUBLE_LINE_GAP / 2;
        float innerOffset = DOUBLE_LINE_GAP / 2;
        
        Path outerPath = createDoublePolygonalOutline(start, control1, control2, end, outerOffset);
        Path innerPath = createDoublePolygonalOutline(start, control1, control2, end, innerOffset);
        
        Paint whitePaint = new Paint();
        whitePaint.setColor(mapStationFillColor);
        whitePaint.setStyle(Paint.Style.FILL);
        
        pathCache.linesPaths.add(new LinePath(outerPath, startStation.getColor(), innerPath, whitePaint));
    }
    
    private void addDoubleQuadrilateralBezierPathToCacheParallelPaths(Station startStation, Point start, Point control1, Point control2, Point end) {
        float offset = getAdjustedLineWidth(DOUBLE_LINE_WIDTH) / 2 + DOUBLE_LINE_GAP / 2;
        
        // Вычисляем нормали в ключевых точках
        PointF normalStart = calculateBezierNormal(0, start, control1, control2, end);
        PointF normalEnd = calculateBezierNormal(1, start, control1, control2, end);
        PointF normalControl1 = calculateBezierNormal(0.33f, start, control1, control2, end);
        PointF normalControl2 = calculateBezierNormal(0.67f, start, control1, control2, end);
        
        // Создаем два параллельных пути
        Path path1 = createParallelBezierPath(start, control1, control2, end, normalStart, normalControl1, normalControl2, normalEnd, offset);
        Path path2 = createParallelBezierPath(start, control1, control2, end, normalStart, normalControl1, normalControl2, normalEnd, -offset);
        
        // Создаем заливку между путями через дискретизацию
        Path fillPath = new Path();
        List<PointF> path1Points = new ArrayList<>();
        List<PointF> path2Points = new ArrayList<>();
        
        for (float t = 0; t <= 1; t += BEZIER_SEGMENT_STEP) {
            PointF point = calculateBezierPoint(t, start, control1, control2, end);
            PointF normal = calculateBezierNormal(t, start, control1, control2, end);
            
            float scaledX = point.x * currentCoordinateScaleFactor;
            float scaledY = point.y * currentCoordinateScaleFactor;
            float scaledOffsetX = normal.x * offset * currentCoordinateScaleFactor;
            float scaledOffsetY = normal.y * offset * currentCoordinateScaleFactor;
            
            path1Points.add(new PointF(scaledX + scaledOffsetX, scaledY + scaledOffsetY));
            path2Points.add(new PointF(scaledX - scaledOffsetX, scaledY - scaledOffsetY));
        }
        
        if (!path1Points.isEmpty()) {
            fillPath.moveTo(path1Points.get(0).x, path1Points.get(0).y);
            for (int i = 1; i < path1Points.size(); i++) {
                fillPath.lineTo(path1Points.get(i).x, path1Points.get(i).y);
            }
            for (int i = path2Points.size() - 1; i >= 0; i--) {
                fillPath.lineTo(path2Points.get(i).x, path2Points.get(i).y);
            }
            fillPath.close();
        }
        
        Paint whitePaint = new Paint();
        whitePaint.setColor(mapStationFillColor);
        whitePaint.setStyle(Paint.Style.FILL);
        
        // Используем fillPath как внешний контур, path2 как внутренний
        pathCache.linesPaths.add(new LinePath(fillPath, startStation.getColor(), path2, whitePaint));
    }

    private void addDoubleStraightLineToCache(Station station1, Station station2, Path linePath) {

        if (LINE_RENDERING_METHOD == LineRenderingMethod.POLYGONAL_OUTLINE) {
            // Для POLYGONAL_OUTLINE используем тот же подход, что и для кривых
            Point start = new Point(station1.getX(), station1.getY());
            Point end = new Point(station2.getX(), station2.getY());
            
            float outerOffset = getAdjustedLineWidth(DOUBLE_LINE_WIDTH) / 2 + DOUBLE_LINE_GAP / 2;
            float innerOffset = DOUBLE_LINE_GAP / 2;
            
            Path outerPath = createDoubleStraightLinePolygonalOutline(start, end, outerOffset);
            Path innerPath = createDoubleStraightLinePolygonalOutline(start, end, innerOffset);
            
            Paint whitePaint = new Paint();
            whitePaint.setColor(mapStationFillColor);
            whitePaint.setStyle(Paint.Style.FILL);
            
            pathCache.linesPaths.add(new LinePath(outerPath, station1.getColor(), innerPath, whitePaint));
        } else {
            // Для других методов используем простой четырехугольник
            float x1 = station1.getX() * currentCoordinateScaleFactor;
            float y1 = station1.getY() * currentCoordinateScaleFactor;
            float x2 = station2.getX() * currentCoordinateScaleFactor;
            float y2 = station2.getY() * currentCoordinateScaleFactor;

            // Вычисляем перпендикулярный вектор для смещения
            float dx = x2 - x1;
            float dy = y2 - y1;
            float length = (float) Math.sqrt(dx * dx + dy * dy);

            if (length == 0) return;
            float nx = -dy / length; // Нормаль по оси X
            float ny = dx / length;  // Нормаль по оси Y


            float outerOffset = 6f;

            // Внешние линии
            Path outerPath = new Path();

            outerPath.moveTo(x1 + nx * outerOffset, y1 + ny * outerOffset);
            outerPath.lineTo(x2 + nx * outerOffset, y2 + ny * outerOffset);
            outerPath.lineTo(x2 - nx * outerOffset, y2 - ny * outerOffset);
            outerPath.lineTo(x1 - nx * outerOffset, y1 - ny * outerOffset);
            outerPath.close();


            // Внутренняя линия
            Path innerPath = new Path();
            innerPath.moveTo(x1, y1);
            innerPath.lineTo(x2, y2);


            // Устанавливаем цвет для внутренней линии
            Paint whitePaint = new Paint();

            whitePaint.setColor(mapStationFillColor);
            whitePaint.setStyle(Paint.Style.STROKE);
            whitePaint.setStrokeWidth(6);

            // Добавляем внешний и внутренний пути в кэш
            pathCache.linesPaths.add(new LinePath(outerPath, station1.getColor(), innerPath, whitePaint));

        }
    }

    private void addQuadrilateralBezierPathToCache(Station startStation, Station endStation, List<Point> intermediatePoints, Path linePath) {
        Point start = new Point(startStation.getX(), startStation.getY());
        Point control1 = intermediatePoints.get(0);
        Point control2 = intermediatePoints.get(1);
        Point end = new Point(endStation.getX(), endStation.getY());

        
        switch (LINE_RENDERING_METHOD) {
            case FIXED_STROKE_WIDTH:
                addQuadrilateralBezierPathToCacheFixedStrokeWidth(start, control1, control2, end, linePath);
                break;
            case POLYGONAL_OUTLINE:
                Path outlinePath = createPolygonalOutline(start, control1, control2, end, getAdjustedLineWidth(LINE_WIDTH));
                linePath.addPath(outlinePath);
                break;
            case PARALLEL_PATHS:
                addQuadrilateralBezierPathToCacheFixedStrokeWidth(start, control1, control2, end, linePath);
                break;
        }
    }
    
    private void addQuadrilateralBezierPathToCacheFixedStrokeWidth(Point start, Point control1, Point control2, Point end, Path linePath) {
        float offset = getAdjustedLineWidth(LINE_WIDTH) / 2;
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;
        float nx = dy / length;
        float ny = -dx / length;

        // Верхняя кривая
        float x1Start = (start.x + nx * offset) * currentCoordinateScaleFactor;
        float y1Start = (start.y + ny * offset) * currentCoordinateScaleFactor;
        float x1Control1 = (control1.x + nx * offset) * currentCoordinateScaleFactor;
        float y1Control1 = (control1.y + ny * offset) * currentCoordinateScaleFactor;
        float x1Control2 = (control2.x + nx * offset) * currentCoordinateScaleFactor;
        float y1Control2 = (control2.y + ny * offset) * currentCoordinateScaleFactor;
        float x1End = (end.x + nx * offset) * currentCoordinateScaleFactor;
        float y1End = (end.y + ny * offset) * currentCoordinateScaleFactor;

        // Нижняя кривая
        float x2Start = (start.x - nx * offset) * currentCoordinateScaleFactor;
        float y2Start = (start.y - ny * offset) * currentCoordinateScaleFactor;
        float x2Control1 = (control1.x - nx * offset) * currentCoordinateScaleFactor;
        float y2Control1 = (control1.y - ny * offset) * currentCoordinateScaleFactor;
        float x2Control2 = (control2.x - nx * offset) * currentCoordinateScaleFactor;
        float y2Control2 = (control2.y - ny * offset) * currentCoordinateScaleFactor;
        float x2End = (end.x - nx * offset) * currentCoordinateScaleFactor;
        float y2End = (end.y - ny * offset) * currentCoordinateScaleFactor;

        // Создаем замкнутый четырехугольный путь
        linePath.moveTo(x1Start, y1Start);
        linePath.cubicTo(x1Control1, y1Control1, x1Control2, y1Control2, x1End, y1End);
        linePath.lineTo(x2End, y2End);
        linePath.cubicTo(x2Control2, y2Control2, x2Control1, y2Control1, x2Start, y2Start);
        linePath.close();
    }


    private LinePath buildRouteLinePath(Station station1, Station station2, String lineType, String color) {
        Station startStation = station1.getId().compareTo(station2.getId()) < 0 ? station1 : station2;
        Station endStation = station1.getId().compareTo(station2.getId()) < 0 ? station2 : station1;
        List<Point> intermediatePoints = startStation.getIntermediatePoints(endStation);

        if (intermediatePoints == null || intermediatePoints.isEmpty()) {
            if ("double".equals(lineType)) {
                return buildRouteDoubleStraightLinePath(startStation, endStation, color);
            }
            Path path = new Path();
            addQuadrilateralLinePathToCache(startStation, endStation, path);
            return new LinePath(path, color);
        }

        if (intermediatePoints.size() == 1) {
            Path path = new Path();
            addQuadrilateralWithIntermediatePointPathToCache(startStation, endStation, intermediatePoints.get(0), path);
            return new LinePath(path, color);
        }

        if (intermediatePoints.size() == 2 && "double".equals(lineType)) {
            return buildRouteDoubleBezierPath(startStation, endStation, intermediatePoints, color);
        }

        Path path = new Path();
        addQuadrilateralBezierPathToCache(startStation, endStation, intermediatePoints, path);
        return new LinePath(path, color);
    }

    private LinePath buildRouteDoubleStraightLinePath(Station startStation, Station endStation, String color) {
        MapPathCache originalCache = pathCache;
        MapPathCache tempCache = new MapPathCache();
        pathCache = tempCache;
        try {
            Path dummy = new Path();
            addDoubleStraightLineToCache(startStation, endStation, dummy);
        } finally {
            pathCache = originalCache;
        }
        if (!tempCache.linesPaths.isEmpty()) {
            LinePath generated = tempCache.linesPaths.get(tempCache.linesPaths.size() - 1);
            return cloneLinePath(generated, color);
        }
        Path fallback = new Path();
        addQuadrilateralLinePathToCache(startStation, endStation, fallback);
        return new LinePath(fallback, color);
    }

    private LinePath buildRouteDoubleBezierPath(Station startStation, Station endStation, List<Point> intermediatePoints, String color) {
        MapPathCache originalCache = pathCache;
        MapPathCache tempCache = new MapPathCache();
        pathCache = tempCache;
        try {
            Path dummy = new Path();
            addDoubleQuadrilateralBezierPathToCache(startStation, endStation, intermediatePoints, dummy);
        } finally {
            pathCache = originalCache;
        }
        if (!tempCache.linesPaths.isEmpty()) {
            LinePath generated = tempCache.linesPaths.get(tempCache.linesPaths.size() - 1);
            return cloneLinePath(generated, color);
        }
        Path fallback = new Path();
        addQuadrilateralBezierPathToCache(startStation, endStation, intermediatePoints, fallback);
        return new LinePath(fallback, color);
    }

    private LinePath cloneLinePath(LinePath original, String colorOverride) {
        String effectiveColor = colorOverride != null ? colorOverride : original.color;
        Path mainPath = original.path != null ? new Path(original.path) : new Path();
        if (original.innerPath != null && original.whitePaint != null) {
            Path innerCopy = new Path(original.innerPath);
            Paint whiteCopy = new Paint(original.whitePaint);
            return new LinePath(mainPath, effectiveColor, innerCopy, whiteCopy);
        }
        return new LinePath(mainPath, effectiveColor);
    }

    // Add this method to center the map
    private void centerMap() {
        if (stations == null || stations.isEmpty()) {
            return;
        }

        // Find map bounds
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (Station station : stations) {
            float x = station.getX() * currentCoordinateScaleFactor;
            float y = station.getY() * currentCoordinateScaleFactor;

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        // Calculate map dimensions
        float mapWidth = maxX - minX;
        float mapHeight = maxY - minY;

        // Calculate map center
        float mapCenterX = minX + (mapWidth / 2);
        float mapCenterY = minY + (mapHeight / 2);

        // Calculate view center
        float viewCenterX = getWidth() / 2f;
        float viewCenterY = getHeight() / 2f;
        Log.d("centerMap", "viewCenterX: " + viewCenterX + ", viewCenterY: " + viewCenterY);
        Log.d("centerMap", "mapCenterX: " + mapCenterX + ", mapCenterY: " + mapCenterY);

        // Calculate required translation to center map
        translateX = viewCenterX - mapCenterX;
        translateY = viewCenterY - mapCenterY;

        updateTransformMatrix();
        invalidate();
    }

    private void drawMapObjects(Canvas canvas) {
        if (mapObjects != null) {
            for (MapObject mapObject : mapObjects) {
                drawMapObject(canvas, mapObject);
            }
        }
    }

    private void drawMapObject(Canvas canvas, MapObject mapObject) {
        float objectX = mapObject.getPosition().x * currentCoordinateScaleFactor;
        float objectY = mapObject.getPosition().y * currentCoordinateScaleFactor;
        Paint objectPaint = new Paint();
        objectPaint.setColor(Color.BLACK);
        objectPaint.setStyle(Paint.Style.FILL);
        objectPaint.setTextSize(20);
        if (mapObject.getType().equals("airport")) {
            canvas.drawText("✈", objectX - 12, objectY + 12, objectPaint);
        } else if (mapObject.getType().equals("train_station")) {
            canvas.drawText("🚂", objectX - 12, objectY + 12, objectPaint);
        }
        canvas.drawText(mapObject.getdisplayNumber(), objectX + 40, objectY, objectPaint);
    }

    private void drawGrayedMap(Canvas canvas) {
        Set<String> drawnConnections = new HashSet<>();
        Paint grayedLinePaint = new Paint(grayedPaint);
        grayedLinePaint.setStrokeWidth(9);

        // Отрисовка серых линий для пригорода, если выбрана карта метро, речного трамвая или трамваев
        if (!isSuburbanMap && suburbanLines != null) {
            for (Line line : suburbanLines) {
                drawGrayedLines(canvas, line, drawnConnections, grayedLinePaint, suburbanStations);
            }
        }

        // Отрисовка серых линий для речного трамвая, если выбрана карта метро, пригорода или трамваев
        if (!isRiverTramMap && riverTramLines != null) {
            for (Line line : riverTramLines) {
                drawGrayedLines(canvas, line, drawnConnections, grayedLinePaint, riverTramStations);
            }
        }

        // Отрисовка серых линий для метро, если выбрана карта пригорода, речного трамвая или трамваев
        if (!isMetroMap && lines != null) {
            for (Line line : lines) {
                drawGrayedLines(canvas, line, drawnConnections, grayedLinePaint, stations);
            }
        }

        // Отрисовка серых линий для трамваев, если выбрана карта метро, пригорода или речного трамвая
        if (!isTramMap && tramLines != null) {
            for (Line line : tramLines) {
                drawGrayedLines(canvas, line, drawnConnections, grayedLinePaint, tramStations);
            }
        }
    }

    private void drawGrayedLines(Canvas canvas, Line line,
                                 Set<String> drawnConnections, Paint grayedLinePaint, List<Station> grayedStations) {

        if (line == null) return;
        List<Station> lineStations = line.getStations();
        if (lineStations == null) return;

        for (Station station : lineStations) {
            List<Station.Neighbor> neighborsList = (station != null) ? station.getNeighbors() : null;
            if (neighborsList == null) continue;
            for (Station.Neighbor neighbor : neighborsList) {
                Station neighborStation = findStationById(

                        neighbor.getStation().getId(), lineStations);
                if (neighborStation != null &&
                        line.getLineIdForStation(neighborStation) != null) {
                    String connectionKey = station.getId().compareTo(
                            neighborStation.getId()) < 0
                            ? station.getId() + "-" + neighborStation.getId()
                            : neighborStation.getId() + "-" + station.getId();
                    if (!drawnConnections.contains(connectionKey)) {
                        drawLineWithIntermediatePoints(canvas, station,
                                neighborStation, line.getLineType(), grayedLinePaint);
                        drawnConnections.add(connectionKey);
                    }
                }
            }
        }

        // Обработка круговых линий

        if (line.isCircle() && lineStations.size() > 1) {
            Station firstStation = lineStations.get(0);
            Station lastStation = lineStations.get(
                    lineStations.size() - 1);
            String connectionKey = firstStation.getId().compareTo(
                    lastStation.getId()) < 0
                    ? firstStation.getId() + "-" + lastStation.getId()
                    : lastStation.getId() + "-" + firstStation.getId();
            if (!drawnConnections.contains(connectionKey)) {
                drawLineWithIntermediatePoints(canvas, firstStation,
                        lastStation, line.getLineType(), grayedLinePaint);
                drawnConnections.add(connectionKey);
            }
        }
    }

    // Аниматоры для translateX, translateY и scale
    private ValueAnimator translateXAnimator;
    private ValueAnimator translateYAnimator;
    private ValueAnimator scaleAnimator;
    private static final float TARGET_SCALE = 1.5f;
    private static final long MOVEMENT_DURATION = 1000;
    private static final long SCALE_DURATION = 400;
    private static final long COORDINATE_SCALE_ANIMATION_DURATION = 800;

    private ValueAnimator coordinateScaleAnimator;
    private float coordinateScaleCompensation = 1.0f;

    private Station userPositionStation = null;
    private Paint userPositionPaint;

    public void updateUserPosition(Station station) {
        this.userPositionStation = station;
        Log.d("UserPosition", "updateUserPosition: " + station.getName());

        // Запускаем анимацию пульсации, если она еще не запущена
        if (pulseAnimator == null || !pulseAnimator.isRunning()) {
            initPulseAnimation();
            pulseAnimator.start();
        }

        invalidate(); // Перерисовываем карту
    }

    public void centerOnStation(Station station) {
        if (station == null) return;

        // Останавливаем текущие анимации
        cancelRunningAnimations();

        // Получаем координаты станции в масштабированных координатах
        float stationX = station.getX() * currentCoordinateScaleFactor;
        float stationY = station.getY() * currentCoordinateScaleFactor;

        // Вычисляем текущие координаты станции на экране
        float[] stationScreenCoords = {stationX, stationY};
        transformMatrix.mapPoints(stationScreenCoords);

        // Сохраняем начальные значения
        final float startTranslateX = translateX;
        final float startTranslateY = translateY;
        final float startScale = scaleFactor;

        // Вычисляем целевые значения translateX и translateY
        float targetTranslateX = translateX + (getWidth() / 2f - stationScreenCoords[0]) / scaleFactor;
        float targetTranslateY = translateY + (getHeight() / 2f - stationScreenCoords[1] - 200) / scaleFactor;

        // Создаем AnimatorSet для последовательной анимации
        AnimatorSet animatorSet = new AnimatorSet();

        // Создаем набор анимаций для перемещения
        translateXAnimator = createAnimator(startTranslateX, targetTranslateX, value -> {
            translateX = value;
            updateTransformMatrix();
        }, MOVEMENT_DURATION);

        translateYAnimator = createAnimator(startTranslateY, targetTranslateY, value -> {
            translateY = value;
            updateTransformMatrix();
        }, MOVEMENT_DURATION);

        // Создаем анимацию масштабирования
        scaleAnimator = createAnimator(startScale, TARGET_SCALE, value -> {
            scaleFactor = value;
            updateTransformMatrix();
        }, SCALE_DURATION);

        // Создаем набор параллельных анимаций для перемещения
        AnimatorSet movementAnimations = new AnimatorSet();
        movementAnimations.playTogether(translateXAnimator, translateYAnimator);

        // Настраиваем последовательное выполнение: сначала перемещение, затем масштабирование
        animatorSet.playSequentially(
                movementAnimations
        );

        // Добавляем слушатель для обновления отрисовки
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                invalidate();
            }
        });

        // Запускаем анимацию
        animatorSet.start();
    }

    private ValueAnimator createAnimator(float startValue, float endValue, AnimatorUpdateListener updateListener, long duration) {
        ValueAnimator animator = ValueAnimator.ofFloat(startValue, endValue);
        animator.setDuration(duration);

        // Используем разные интерполяторы для перемещения и масштабирования
        if (duration == MOVEMENT_DURATION) {
            // Для перемещения используем более плавную кривую
            animator.setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f));
        } else {
            // Для масштабирования используем другую кривую
            animator.setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f));
        }

        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            updateListener.onAnimationUpdate(value);
            invalidate();
        });

        return animator;
    }

    private void cancelRunningAnimations() {
        if (translateXAnimator != null && translateXAnimator.isRunning()) {
            translateXAnimator.cancel();
        }
        if (translateYAnimator != null && translateYAnimator.isRunning()) {
            translateYAnimator.cancel();
        }
        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }
        if (coordinateScaleAnimator != null && coordinateScaleAnimator.isRunning()) {
            coordinateScaleAnimator.cancel();
            coordinateScaleCompensation = 1.0f;
            updateTransformMatrix();
        }
    }

    private void animateCoordinateScaleWithMatrix(float targetScale) {
        float startScale = currentCoordinateScaleFactor;
        float scaleRatio = targetScale / startScale;

        if (coordinateScaleAnimator != null && coordinateScaleAnimator.isRunning()) {
            coordinateScaleAnimator.cancel();
        }

        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        final float centerScreenX = getWidth() / 2f;
        final float centerScreenY = getHeight() / 2f;

        float centerWorldX = (centerScreenX - translateX) / scaleFactor;
        float centerWorldY = (centerScreenY - translateY) / scaleFactor;

        final float centerWorldXOriginal = centerWorldX / startScale;
        final float centerWorldYOriginal = centerWorldY / startScale;

        coordinateScaleAnimator = ValueAnimator.ofFloat(1.0f, scaleRatio);
        coordinateScaleAnimator.setDuration(COORDINATE_SCALE_ANIMATION_DURATION);
        coordinateScaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        coordinateScaleAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            currentCoordinateScaleFactor = startScale * progress;
            coordinateScaleCompensation = 1.0f / progress;

            float newCenterWorldXScaled = centerWorldXOriginal * currentCoordinateScaleFactor;
            float newCenterWorldYScaled = centerWorldYOriginal * currentCoordinateScaleFactor;

            translateX = centerScreenX - newCenterWorldXScaled * scaleFactor;
            translateY = centerScreenY - newCenterWorldYScaled * scaleFactor;

            updateTransformMatrix();
            updatePathCache();
            needsRedraw = true;
            invalidate();
        });

        coordinateScaleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                float finalCenterWorldXScaled = centerWorldXOriginal * targetScale;
                float finalCenterWorldYScaled = centerWorldYOriginal * targetScale;

                translateX = centerScreenX - finalCenterWorldXScaled * scaleFactor;
                translateY = centerScreenY - finalCenterWorldYScaled * scaleFactor;

                currentCoordinateScaleFactor = targetScale;
                coordinateScaleCompensation = 1.0f;
                updateTransformMatrix();
            }
        });

        coordinateScaleAnimator.start();
    }

    private void fitRouteToTopHalf() {
        if (route == null || route.isEmpty()) return;

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Station s : route) {
            float x = s.getX() * currentCoordinateScaleFactor;
            float y = s.getY() * currentCoordinateScaleFactor;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        float routeWidth = Math.max(1f, maxX - minX);
        float routeHeight = Math.max(1f, maxY - minY);

        float viewW = getWidth();
        float viewH = getHeight();
        if (viewW <= 0 || viewH <= 0) return;

        // Верхняя половина экрана с внутренними отступами в screen px
        float padding = 40f; // экранные px
        float targetLeft = padding;
        float targetTop = padding;
        float targetRight = viewW - padding;
        float targetBottom = viewH * 0.5f - padding;
        float targetW = Math.max(1f, targetRight - targetLeft);
        float targetH = Math.max(1f, targetBottom - targetTop);

        // Рассчитываем масштаб, чтобы весь bbox маршрута влез в целевую область
        float sx = targetW / routeWidth;
        float sy = targetH / routeHeight;
        float desiredScale = Math.min(sx, sy) * 0.95f; // небольшой запас
        float targetScale = Math.max(0.1f, Math.min(2.0f, desiredScale));

        // Центр bbox маршрута (в мировых координатах)
        float routeCenterX = (minX + maxX) / 2f;
        float routeCenterY = (minY + maxY) / 2f;

        // Экранный центр целевой области
        float targetScreenX = (targetLeft + targetRight) / 2f;
        float targetScreenY = (targetTop + targetBottom) / 2f;

        // С учётом порядка преобразований (Translate -> Scale): (routeCenter + T) * S = targetCenter
        float targetTranslateX = targetScreenX / targetScale - routeCenterX;
        float targetTranslateY = targetScreenY / targetScale - routeCenterY;

        cancelRunningAnimations();
        final float startTx = translateX;
        final float startTy = translateY;
        final float startScale = scaleFactor;

        AnimatorSet set = new AnimatorSet();
        translateXAnimator = createAnimator(startTx, targetTranslateX, v -> { translateX = v; updateTransformMatrix(); }, MOVEMENT_DURATION);
        translateYAnimator = createAnimator(startTy, targetTranslateY, v -> { translateY = v; updateTransformMatrix(); }, MOVEMENT_DURATION);
        scaleAnimator = createAnimator(startScale, targetScale, v -> { scaleFactor = v; updateTransformMatrix(); }, SCALE_DURATION);
        AnimatorSet move = new AnimatorSet();
        move.playTogether(translateXAnimator, translateYAnimator, scaleAnimator);
        set.playTogether(move);
        set.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { invalidate(); }
        });
        set.start();
    }

    private interface AnimatorUpdateListener {
        void onAnimationUpdate(float value);
    }

    private void drawIntermediatePoints(Canvas canvas) {
        for (Station station : stations) {
            if (station.getIntermediatePoints() != null) {
                for (Map.Entry<Station, List<Point>> entry : station.getIntermediatePoints().entrySet()) {
                    List<Point> intermediatePoints = entry.getValue();
                    drawIntermediatePoints(canvas, intermediatePoints, stationPaint);
                }
            }
        }
    }

    private void drawIntermediatePoints(Canvas canvas, List<Point> intermediatePoints, Paint paint) {
        for (Point point : intermediatePoints) {
            canvas.drawCircle(point.x * currentCoordinateScaleFactor, point.y * currentCoordinateScaleFactor, 10, paint);
        }
    }

    private void drawLineWithIntermediatePoints(Canvas canvas, Station station1, Station station2, String lineType, Paint paint) {
        Station startStation = station1.getId().compareTo(station2.getId()) < 0 ? station1 : station2;
        Station endStation = station1.getId().compareTo(station2.getId()) < 0 ? station2 : station1;
        List<Point> intermediatePoints = startStation.getIntermediatePoints(endStation);
        paint.setAntiAlias(true);
        
        if (intermediatePoints == null || intermediatePoints.isEmpty()) {
            if (lineType.equals("double")) {
                Point start = new Point(startStation.getX(), startStation.getY());
                Point end = new Point(endStation.getX(), endStation.getY());
                
                float outerOffset = getAdjustedLineWidth(DOUBLE_LINE_WIDTH) / 2 + DOUBLE_LINE_GAP / 2;
                float innerOffset = DOUBLE_LINE_GAP / 2;
                
                Path outerPath = createDoubleStraightLinePolygonalOutline(start, end, outerOffset);
                Path innerPath = createDoubleStraightLinePolygonalOutline(start, end, innerOffset);
                
                Paint fillPaint = new Paint();
                fillPaint.setColor(mapStationFillColor);
                fillPaint.setStyle(Paint.Style.FILL);
                fillPaint.setAntiAlias(true);
                
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(outerPath, paint);
                canvas.drawPath(innerPath, fillPaint);
            } else {
                Point start = new Point(startStation.getX(), startStation.getY());
                Point end = new Point(endStation.getX(), endStation.getY());
                Path path = createStraightLinePolygonalOutline(start, end, getAdjustedLineWidth(LINE_WIDTH));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(path, paint);
            }
        } else if (intermediatePoints.size() == 1) {
            Point start = new Point(startStation.getX(), startStation.getY());
            Point middle = new Point(intermediatePoints.get(0).x, intermediatePoints.get(0).y);
            Point end = new Point(endStation.getX(), endStation.getY());
            
            Path path = new Path();
            List<PointF> leftPoints = new ArrayList<>();
            List<PointF> rightPoints = new ArrayList<>();
            
            float halfWidth = getAdjustedLineWidth(LINE_WIDTH) / 2;
            
            float dx1 = middle.x - start.x;
            float dy1 = middle.y - start.y;
            float length1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
            if (length1 > 0) {
                float nx1 = -dy1 / length1;
                float ny1 = dx1 / length1;
                
                for (float t = 0; t <= 1; t += BEZIER_SEGMENT_STEP) {
                    float pointX = start.x + t * (middle.x - start.x);
                    float pointY = start.y + t * (middle.y - start.y);
                    
                    float scaledX = pointX * currentCoordinateScaleFactor;
                    float scaledY = pointY * currentCoordinateScaleFactor;
                    float scaledOffsetX = nx1 * halfWidth * currentCoordinateScaleFactor;
                    float scaledOffsetY = ny1 * halfWidth * currentCoordinateScaleFactor;
                    
                    leftPoints.add(new PointF(scaledX + scaledOffsetX, scaledY + scaledOffsetY));
                    rightPoints.add(new PointF(scaledX - scaledOffsetX, scaledY - scaledOffsetY));
                }
            }
            
            float dx2 = end.x - middle.x;
            float dy2 = end.y - middle.y;
            float length2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);
            if (length2 > 0) {
                float nx2 = -dy2 / length2;
                float ny2 = dx2 / length2;
                
                for (float t = 0; t <= 1; t += BEZIER_SEGMENT_STEP) {
                    float pointX = middle.x + t * (end.x - middle.x);
                    float pointY = middle.y + t * (end.y - middle.y);
                    
                    float scaledX = pointX * currentCoordinateScaleFactor;
                    float scaledY = pointY * currentCoordinateScaleFactor;
                    float scaledOffsetX = nx2 * halfWidth * currentCoordinateScaleFactor;
                    float scaledOffsetY = ny2 * halfWidth * currentCoordinateScaleFactor;
                    
                    leftPoints.add(new PointF(scaledX + scaledOffsetX, scaledY + scaledOffsetY));
                    rightPoints.add(new PointF(scaledX - scaledOffsetX, scaledY - scaledOffsetY));
                }
            }
            
            if (!leftPoints.isEmpty()) {
                path.moveTo(leftPoints.get(0).x, leftPoints.get(0).y);
                for (int i = 1; i < leftPoints.size(); i++) {
                    path.lineTo(leftPoints.get(i).x, leftPoints.get(i).y);
                }
                for (int i = rightPoints.size() - 1; i >= 0; i--) {
                    path.lineTo(rightPoints.get(i).x, rightPoints.get(i).y);
                }
                path.close();
            }
            
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, paint);
        } else if (intermediatePoints.size() == 2) {
            Point start = new Point(startStation.getX(), startStation.getY());
            Point control1 = intermediatePoints.get(0);
            Point control2 = intermediatePoints.get(1);
            Point end = new Point(endStation.getX(), endStation.getY());
            if (lineType.equals("double")) {
                drawDoubleBezierCurve(canvas, start, control1, control2, end, paint);
            } else {
                drawBezierCurve(canvas, start, control1, control2, end, paint);
            }
        }
    }

    private void drawDoubleLine(Canvas canvas, Station station1, Station station2, Paint paint) {
        float x1 = station1.getX() * currentCoordinateScaleFactor;
        float y1 = station1.getY() * currentCoordinateScaleFactor;
        float x2 = station2.getX() * currentCoordinateScaleFactor;
        float y2 = station2.getY() * currentCoordinateScaleFactor;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = dx / length;
        float ny = dy / length;
        float perpX = -ny * 5;
        float perpY = nx * 5;
        Paint whitePaint = new Paint();

        whitePaint.setColor(mapStationFillColor);
        whitePaint.setStrokeWidth(6);
        canvas.drawLine(x1 + perpX, y1 + perpY, x2 + perpX, y2 + perpY, paint);
        canvas.drawLine(x1 - perpX, y1 - perpY, x2 - perpX, y2 - perpY, paint);
        canvas.drawLine(x1, y1, x2, y2, whitePaint);
    }

    private void drawBezierCurve(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {

        switch (LINE_RENDERING_METHOD) {
            case FIXED_STROKE_WIDTH:
                drawBezierCurveFixedStrokeWidth(canvas, start, control1, control2, end, paint);
                break;
            case POLYGONAL_OUTLINE:
                drawBezierCurvePolygonalOutline(canvas, start, control1, control2, end, paint);
                break;
            case PARALLEL_PATHS:
                drawBezierCurveParallelPaths(canvas, start, control1, control2, end, paint);
                break;
        }
    }
    
    private void drawBezierCurveFixedStrokeWidth(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {
        Path path = new Path();
        paint.setStyle(Paint.Style.STROKE);

        paint.setStrokeWidth(getAdjustedLineWidth(LINE_WIDTH));
        path.moveTo(start.x * currentCoordinateScaleFactor, start.y * currentCoordinateScaleFactor);
        path.cubicTo(
                control1.x * currentCoordinateScaleFactor, control1.y * currentCoordinateScaleFactor,
                control2.x * currentCoordinateScaleFactor, control2.y * currentCoordinateScaleFactor,
                end.x * currentCoordinateScaleFactor, end.y * currentCoordinateScaleFactor
        );
        canvas.drawPath(path, paint);
    }

    
    private void drawBezierCurvePolygonalOutline(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {
        Path outlinePath = createPolygonalOutline(start, control1, control2, end, getAdjustedLineWidth(LINE_WIDTH));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(outlinePath, paint);
    }
    
    private void drawBezierCurveParallelPaths(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {
        // Для одинарных линий вариант 3 аналогичен варианту 1
        drawBezierCurveFixedStrokeWidth(canvas, start, control1, control2, end, paint);
    }
    
    private Path createPolygonalOutline(Point start, Point control1, Point control2, Point end, float width) {
        Path path = new Path();
        List<PointF> leftPoints = new ArrayList<>();
        List<PointF> rightPoints = new ArrayList<>();
        
        float halfWidth = width / 2;
        
        for (float t = 0; t <= 1; t += BEZIER_SEGMENT_STEP) {
            PointF point = calculateBezierPoint(t, start, control1, control2, end);
            PointF normal = calculateBezierNormal(t, start, control1, control2, end);
            
            float scaledX = point.x * currentCoordinateScaleFactor;
            float scaledY = point.y * currentCoordinateScaleFactor;
            float scaledOffsetX = normal.x * halfWidth * currentCoordinateScaleFactor;
            float scaledOffsetY = normal.y * halfWidth * currentCoordinateScaleFactor;
            
            leftPoints.add(new PointF(scaledX + scaledOffsetX, scaledY + scaledOffsetY));
            rightPoints.add(new PointF(scaledX - scaledOffsetX, scaledY - scaledOffsetY));
        }
        
        if (!leftPoints.isEmpty()) {
            path.moveTo(leftPoints.get(0).x, leftPoints.get(0).y);
            for (int i = 1; i < leftPoints.size(); i++) {
                path.lineTo(leftPoints.get(i).x, leftPoints.get(i).y);
            }
            for (int i = rightPoints.size() - 1; i >= 0; i--) {
                path.lineTo(rightPoints.get(i).x, rightPoints.get(i).y);
            }
            path.close();
        }
        
        return path;
    }
    
    private Path createStraightLinePolygonalOutline(Point start, Point end, float width) {
        Path path = new Path();
        List<PointF> leftPoints = new ArrayList<>();
        List<PointF> rightPoints = new ArrayList<>();
        
        float halfWidth = width / 2;
        
        // Вычисляем нормаль для прямой линии (постоянна для всей линии)
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0) return path;
        
        float nx = -dy / length;
        float ny = dx / length;
        
        // Дискретизируем прямую линию так же, как кривую Безье
        for (float t = 0; t <= 1; t += BEZIER_SEGMENT_STEP) {
            // Точка на прямой линии
            float pointX = start.x + t * (end.x - start.x);
            float pointY = start.y + t * (end.y - start.y);
            
            float scaledX = pointX * currentCoordinateScaleFactor;
            float scaledY = pointY * currentCoordinateScaleFactor;
            float scaledOffsetX = nx * halfWidth * currentCoordinateScaleFactor;
            float scaledOffsetY = ny * halfWidth * currentCoordinateScaleFactor;
            
            leftPoints.add(new PointF(scaledX + scaledOffsetX, scaledY + scaledOffsetY));
            rightPoints.add(new PointF(scaledX - scaledOffsetX, scaledY - scaledOffsetY));
        }
        
        if (!leftPoints.isEmpty()) {
            path.moveTo(leftPoints.get(0).x, leftPoints.get(0).y);
            for (int i = 1; i < leftPoints.size(); i++) {
                path.lineTo(leftPoints.get(i).x, leftPoints.get(i).y);
            }
            for (int i = rightPoints.size() - 1; i >= 0; i--) {
                path.lineTo(rightPoints.get(i).x, rightPoints.get(i).y);
            }
            path.close();
        }
        
        return path;
    }
    
    private Path createDoubleStraightLinePolygonalOutline(Point start, Point end, float offset) {
        Path path = new Path();
        List<PointF> leftPoints = new ArrayList<>();
        List<PointF> rightPoints = new ArrayList<>();
        
        // Вычисляем нормаль для прямой линии (постоянна для всей линии)
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0) return path;
        
        float nx = -dy / length;
        float ny = dx / length;
        
        // Дискретизируем прямую линию так же, как кривую Безье
        for (float t = 0; t <= 1; t += BEZIER_SEGMENT_STEP) {
            // Точка на прямой линии
            float pointX = start.x + t * (end.x - start.x);
            float pointY = start.y + t * (end.y - start.y);
            
            float scaledX = pointX * currentCoordinateScaleFactor;
            float scaledY = pointY * currentCoordinateScaleFactor;
            float scaledOffsetX = nx * offset * currentCoordinateScaleFactor;
            float scaledOffsetY = ny * offset * currentCoordinateScaleFactor;
            
            leftPoints.add(new PointF(scaledX + scaledOffsetX, scaledY + scaledOffsetY));
            rightPoints.add(new PointF(scaledX - scaledOffsetX, scaledY - scaledOffsetY));
        }
        
        if (!leftPoints.isEmpty()) {
            path.moveTo(leftPoints.get(0).x, leftPoints.get(0).y);
            for (int i = 1; i < leftPoints.size(); i++) {
                path.lineTo(leftPoints.get(i).x, leftPoints.get(i).y);
            }
            for (int i = rightPoints.size() - 1; i >= 0; i--) {
                path.lineTo(rightPoints.get(i).x, rightPoints.get(i).y);
            }
            path.close();
        }
        
        return path;
    }

    private void drawDoubleBezierCurve(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {

        switch (LINE_RENDERING_METHOD) {
            case FIXED_STROKE_WIDTH:
                drawDoubleBezierCurveFixedStrokeWidth(canvas, start, control1, control2, end, paint);
                break;
            case POLYGONAL_OUTLINE:
                drawDoubleBezierCurvePolygonalOutline(canvas, start, control1, control2, end, paint);
                break;
            case PARALLEL_PATHS:
                drawDoubleBezierCurveParallelPaths(canvas, start, control1, control2, end, paint);
                break;
        }
    }
    
    private void drawDoubleBezierCurveFixedStrokeWidth(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {
        Path path1 = new Path();
        Path path2 = new Path();
        Path fillPath = new Path();

        paint.setStrokeWidth(getAdjustedLineWidth(DOUBLE_LINE_WIDTH));
        float offset = getAdjustedLineWidth(DOUBLE_LINE_WIDTH) / 2 + DOUBLE_LINE_GAP / 2;
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;
        float nx = dy / length;
        float ny = -dx / length;
        float x1Start = (start.x + nx * offset) * currentCoordinateScaleFactor;
        float y1Start = (start.y + ny * offset) * currentCoordinateScaleFactor;
        float x1Control1 = (control1.x + nx * offset) * currentCoordinateScaleFactor;
        float y1Control1 = (control1.y + ny * offset) * currentCoordinateScaleFactor;
        float x1Control2 = (control2.x + nx * offset) * currentCoordinateScaleFactor;
        float y1Control2 = (control2.y + ny * offset) * currentCoordinateScaleFactor;
        float x1End = (end.x + nx * offset) * currentCoordinateScaleFactor;
        float y1End = (end.y + ny * offset) * currentCoordinateScaleFactor;
        float x2Start = (start.x - nx * offset) * currentCoordinateScaleFactor;
        float y2Start = (start.y - ny * offset) * currentCoordinateScaleFactor;
        float x2Control1 = (control1.x - nx * offset) * currentCoordinateScaleFactor;
        float y2Control1 = (control1.y - ny * offset) * currentCoordinateScaleFactor;
        float x2Control2 = (control2.x - nx * offset) * currentCoordinateScaleFactor;
        float y2Control2 = (control2.y - ny * offset) * currentCoordinateScaleFactor;
        float x2End = (end.x - nx * offset) * currentCoordinateScaleFactor;
        float y2End = (end.y - ny * offset) * currentCoordinateScaleFactor;
        path1.moveTo(x1Start, y1Start);
        path1.cubicTo(x1Control1, y1Control1, x1Control2, y1Control2, x1End, y1End);
        path2.moveTo(x2Start, y2Start);
        path2.cubicTo(x2Control1, y2Control1, x2Control2, y2Control2, x2End, y2End);
        fillPath.moveTo(x1Start, y1Start);
        fillPath.cubicTo(x1Control1, y1Control1, x1Control2, y1Control2, x1End, y1End);
        fillPath.lineTo(x2End, y2End);
        fillPath.cubicTo(x2Control2, y2Control2, x2Control1, y2Control1, x2Start, y2Start);
        fillPath.close();
        Paint fillPaint = new Paint();

        fillPaint.setColor(mapStationFillColor);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(fillPath, fillPaint);

        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path1, paint);
        canvas.drawPath(path2, paint);
    }

    
    private void drawDoubleBezierCurvePolygonalOutline(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {
        float outerOffset = getAdjustedLineWidth(DOUBLE_LINE_WIDTH) / 2 + DOUBLE_LINE_GAP / 2;
        float innerOffset = DOUBLE_LINE_GAP / 2;
        
        Path outerPath = createDoublePolygonalOutline(start, control1, control2, end, outerOffset);
        Path innerPath = createDoublePolygonalOutline(start, control1, control2, end, innerOffset);
        
        Paint fillPaint = new Paint();
        fillPaint.setColor(mapStationFillColor);
        fillPaint.setStyle(Paint.Style.FILL);
        
        // Рисуем внешний контур цветом линии
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(outerPath, paint);
        
        // Рисуем внутренний контур белым цветом для создания зазора
        canvas.drawPath(innerPath, fillPaint);
    }
    
    private void drawDoubleBezierCurveParallelPaths(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {
        // Для варианта 3 используем более точное вычисление нормалей в контрольных точках
        float offset = getAdjustedLineWidth(DOUBLE_LINE_WIDTH) / 2 + DOUBLE_LINE_GAP / 2;
        
        // Вычисляем нормали в ключевых точках
        PointF normalStart = calculateBezierNormal(0, start, control1, control2, end);
        PointF normalEnd = calculateBezierNormal(1, start, control1, control2, end);
        PointF normalControl1 = calculateBezierNormal(0.33f, start, control1, control2, end);
        PointF normalControl2 = calculateBezierNormal(0.67f, start, control1, control2, end);
        
        // Создаем два параллельных пути с правильным смещением
        Path path1 = createParallelBezierPath(start, control1, control2, end, normalStart, normalControl1, normalControl2, normalEnd, offset);
        Path path2 = createParallelBezierPath(start, control1, control2, end, normalStart, normalControl1, normalControl2, normalEnd, -offset);
        
        // Создаем заливку между путями через дискретизацию
        Path fillPath = new Path();
        List<PointF> path1Points = new ArrayList<>();
        List<PointF> path2Points = new ArrayList<>();
        
        for (float t = 0; t <= 1; t += BEZIER_SEGMENT_STEP) {
            PointF point = calculateBezierPoint(t, start, control1, control2, end);
            PointF normal = calculateBezierNormal(t, start, control1, control2, end);
            
            float scaledX = point.x * currentCoordinateScaleFactor;
            float scaledY = point.y * currentCoordinateScaleFactor;
            float scaledOffsetX = normal.x * offset * currentCoordinateScaleFactor;
            float scaledOffsetY = normal.y * offset * currentCoordinateScaleFactor;
            
            path1Points.add(new PointF(scaledX + scaledOffsetX, scaledY + scaledOffsetY));
            path2Points.add(new PointF(scaledX - scaledOffsetX, scaledY - scaledOffsetY));
        }
        
        if (!path1Points.isEmpty()) {
            fillPath.moveTo(path1Points.get(0).x, path1Points.get(0).y);
            for (int i = 1; i < path1Points.size(); i++) {
                fillPath.lineTo(path1Points.get(i).x, path1Points.get(i).y);
            }
            for (int i = path2Points.size() - 1; i >= 0; i--) {
                fillPath.lineTo(path2Points.get(i).x, path2Points.get(i).y);
            }
            fillPath.close();
        }
        
        Paint fillPaint = new Paint();
        fillPaint.setColor(mapStationFillColor);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(fillPath, fillPaint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(getAdjustedLineWidth(DOUBLE_LINE_WIDTH));
        canvas.drawPath(path1, paint);
        canvas.drawPath(path2, paint);
    }
    
    private Path createDoublePolygonalOutline(Point start, Point control1, Point control2, Point end, float offset) {
        Path path = new Path();
        List<PointF> leftPoints = new ArrayList<>();
        List<PointF> rightPoints = new ArrayList<>();
        
        float halfWidth = getAdjustedLineWidth(LINE_WIDTH) / 2;
        
        for (float t = 0; t <= 1; t += BEZIER_SEGMENT_STEP) {
            PointF point = calculateBezierPoint(t, start, control1, control2, end);
            PointF normal = calculateBezierNormal(t, start, control1, control2, end);
            
            float scaledX = point.x * currentCoordinateScaleFactor;
            float scaledY = point.y * currentCoordinateScaleFactor;
            // offset уже включает половину ширины линии, поэтому используем его напрямую
            float scaledOffsetX = normal.x * offset * currentCoordinateScaleFactor;
            float scaledOffsetY = normal.y * offset * currentCoordinateScaleFactor;
            
            leftPoints.add(new PointF(scaledX + scaledOffsetX, scaledY + scaledOffsetY));
            rightPoints.add(new PointF(scaledX - scaledOffsetX, scaledY - scaledOffsetY));
        }
        
        if (!leftPoints.isEmpty()) {
            path.moveTo(leftPoints.get(0).x, leftPoints.get(0).y);
            for (int i = 1; i < leftPoints.size(); i++) {
                path.lineTo(leftPoints.get(i).x, leftPoints.get(i).y);
            }
            for (int i = rightPoints.size() - 1; i >= 0; i--) {
                path.lineTo(rightPoints.get(i).x, rightPoints.get(i).y);
            }
            path.close();
        }
        
        return path;
    }
    
    private Path createParallelBezierPath(Point start, Point control1, Point control2, Point end, 
                                          PointF normalStart, PointF normalControl1, PointF normalControl2, PointF normalEnd, 
                                          float offset) {
        Path path = new Path();
        
        float x1Start = (start.x + normalStart.x * offset) * currentCoordinateScaleFactor;
        float y1Start = (start.y + normalStart.y * offset) * currentCoordinateScaleFactor;
        float x1Control1 = (control1.x + normalControl1.x * offset) * currentCoordinateScaleFactor;
        float y1Control1 = (control1.y + normalControl1.y * offset) * currentCoordinateScaleFactor;
        float x1Control2 = (control2.x + normalControl2.x * offset) * currentCoordinateScaleFactor;
        float y1Control2 = (control2.y + normalControl2.y * offset) * currentCoordinateScaleFactor;
        float x1End = (end.x + normalEnd.x * offset) * currentCoordinateScaleFactor;
        float y1End = (end.y + normalEnd.y * offset) * currentCoordinateScaleFactor;
        
        path.moveTo(x1Start, y1Start);
        path.cubicTo(x1Control1, y1Control1, x1Control2, y1Control2, x1End, y1End);
        
        return path;
    }

    private void drawPartialCircle(Canvas canvas, float centerX, float centerY, float radius, float strokeWidth, List<Float> angles) {
        Paint circleOutlinePaint = new Paint();
        circleOutlinePaint.setColor(transferPaint.getColor());
        circleOutlinePaint.setStyle(Paint.Style.STROKE);
        circleOutlinePaint.setStrokeWidth(strokeWidth);
        float startAngle = 0;
        float sweepAngle = 0;
        if (angles != null) {
            startAngle = angles.get(1);
            sweepAngle = angles.get(0);
        }
        RectF rectF = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        canvas.drawArc(rectF, startAngle, sweepAngle, false, circleOutlinePaint);
    }


    // Вспомогательные методы для работы с кривыми Безье
    
    /**
     * Вычисляет точку на кубической кривой Безье в параметре t (0-1)
     * B(t) = (1-t)³P₀ + 3(1-t)²tP₁ + 3(1-t)t²P₂ + t³P₃
     */
    private PointF calculateBezierPoint(float t, Point p0, Point p1, Point p2, Point p3) {
        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;
        
        float x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x;
        float y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y;
        
        return new PointF(x, y);
    }
    
    /**
     * Вычисляет касательную (производную) кубической кривой Безье в параметре t
     * B'(t) = 3(1-t)²(P₁-P₀) + 6(1-t)t(P₂-P₁) + 3t²(P₃-P₂)
     */
    private PointF calculateBezierTangent(float t, Point p0, Point p1, Point p2, Point p3) {
        float u = 1 - t;
        float uu = u * u;
        float tt = t * t;
        
        float dx = 3 * uu * (p1.x - p0.x) + 6 * u * t * (p2.x - p1.x) + 3 * tt * (p3.x - p2.x);
        float dy = 3 * uu * (p1.y - p0.y) + 6 * u * t * (p2.y - p1.y) + 3 * tt * (p3.y - p2.y);
        
        return new PointF(dx, dy);
    }
    
    /**
     * Вычисляет нормаль (перпендикуляр к касательной) в точке на кривой Безье
     */
    private PointF calculateBezierNormal(float t, Point p0, Point p1, Point p2, Point p3) {
        PointF tangent = calculateBezierTangent(t, p0, p1, p2, p3);
        float length = (float) Math.sqrt(tangent.x * tangent.x + tangent.y * tangent.y);
        if (length == 0) {
            return new PointF(0, 0);
        }
        // Нормаль: перпендикуляр к касательной (-dy, dx)
        float nx = -tangent.y / length;
        float ny = tangent.x / length;
        return new PointF(nx, ny);
    }
    
    /**
     * Нормализует вектор
     */
    private PointF normalize(PointF vector) {
        float length = (float) Math.sqrt(vector.x * vector.x + vector.y * vector.y);
        if (length == 0) {
            return new PointF(0, 0);
        }
        return new PointF(vector.x / length, vector.y / length);
    }
    
    /**
     * Вычисляет нормаль в конкретной точке кривой Безье
     */
    private PointF calculateNormalAtPoint(Point point, Point control1, Point control2, Point end) {
        // Для упрощения используем нормаль от начальной точки
        // В варианте 3 это будет улучшено
        float dx = end.x - point.x;
        float dy = end.y - point.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0) {
            return new PointF(0, 0);
        }
        return new PointF(-dy / length, dx / length);
    }

    private int parseColorSafely(String color, int fallbackColor) {
        if (color == null) {
            return fallbackColor;
        }
        String trimmed = color.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return fallbackColor;
        }
        try {
            return Color.parseColor(trimmed);
        } catch (IllegalArgumentException ex) {
            Log.w("MetroMapView", "Invalid color string: " + trimmed, ex);
            return fallbackColor;
        }
    }

    private void drawPartialCircleWithColor(Canvas canvas, float centerX, float centerY, float radius, float strokeWidth, List<Float> angles, String color) {

        if (angles == null || angles.size() < 2) {
            return;
        }
        Paint circleOutlinePaint = new Paint();

        circleOutlinePaint.setColor(parseColorSafely(color, mapTransferColor));
        circleOutlinePaint.setStyle(Paint.Style.STROKE);
        circleOutlinePaint.setStrokeWidth(strokeWidth);

        float sweepAngle = angles.get(0);
        if (Float.isNaN(sweepAngle) || Math.abs(sweepAngle) < 1f || Math.abs(sweepAngle) >= 359f) {
            return;
        }
        float startAngle = angles.get(1);
        RectF rectF = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        canvas.drawArc(rectF, startAngle, sweepAngle, false, circleOutlinePaint);
    }

    // Метод для отрисовки тёмного оверлея
    private void applyDarkOverlay(Canvas canvas, int saveCount) {
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(150, 0, 0, 0)); // Полупрозрачный черный цвет
        overlayPaint.setStyle(Paint.Style.FILL);

        // Восстанавливаем состояние canvas до исходного
        canvas.restoreToCount(saveCount);
        // Сохраняем новое состояние
        saveCount = canvas.save();

        // Рисуем затемнение на всей области canvas
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

        // Применяем трансформацию для последующей отрисовки маршрута
        canvas.concat(transformMatrix);
    }

    // Методы для работы с геометрией и видимостью
    public void updateTransformMatrix() {
        transformMatrix.reset();
        transformMatrix.postTranslate(translateX, translateY);
        transformMatrix.postScale(scaleFactor, scaleFactor);
        if (coordinateScaleCompensation != 1.0f) {
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            transformMatrix.postScale(coordinateScaleCompensation, coordinateScaleCompensation, centerX, centerY);
        }
        updateVisibleViewport();
    }

    private void updateVisibleViewport() {
        float[] points = {
                0, 0,
                getWidth(), getHeight()
        };

        // Convert screen coordinates to map coordinates
        Matrix inverse = new Matrix();
        transformMatrix.invert(inverse);
        inverse.mapPoints(points);

        // Добавляем отступы к видимой области
        float padding = 100; // Отступ в пикселях
        visibleViewport.set(
                (points[0] / currentCoordinateScaleFactor) - padding,
                (points[1] / currentCoordinateScaleFactor) - padding,
                (points[2] / currentCoordinateScaleFactor) + padding,
                (points[3] / currentCoordinateScaleFactor) + padding
        );
    }

    public Station findStationAt(float x, float y) {
        List<Station> activeStations = getActiveStations();
        for (Station station : activeStations) {
            // Проверяем, попадает ли точка в область станции
            float stationX = station.getX();
            float stationY = station.getY();
            float radius = 20; // Радиус области вокруг станции

            if (Math.abs(stationX - x) < radius && Math.abs(stationY - y) < radius) {
                return station;
            }
        }
        return null;
    }

    private List<Station> getActiveStations() {
        if (isMetroMap) {
            return stations;
        } else if (isSuburbanMap) {
            return suburbanStations;
        } else if (isRiverTramMap) {
            return riverTramStations;
        } else if (isTramMap) {
            return tramStations;
        }
        return Collections.emptyList(); // Возвращаем пустой список, если ни одна карта не активна
    }

    // Вспомогательные методы
    private Station findStationById(String id, List<Station> stations) {
        for (Station station : stations) {
            if (station.getId().equals(id)) {
                return station;
            }
        }
        return null;
    }

    private Line findLineForConnection(Station station1, Station station2) {
        for (Line line : lines) {
            if (line.getStations().contains(station1) && line.getStations().contains(station2)) {
                return line;
            }
        }
        for (Line line : suburbanLines) {
            if (line.getStations().contains(station1) && line.getStations().contains(station2)) {
                return line;
            }
        }
        for (Line line : riverTramLines) {
            if (line.getStations().contains(station1) && line.getStations().contains(station2)) {
                return line;
            }
        }
        for (Line line : tramLines) {
            if (line.getStations().contains(station1) && line.getStations().contains(station2)) {
                return line;
            }
        }
        return null;
    }

    public static List<Float> getAngle(double x1, double y1, double x2, double y2, double x3, double y3) {
        double dx1 = x2 - x1;
        double dy1 = y2 - y1;
        double dx2 = x3 - x2;
        double dy2 = y3 - y2;
        double length1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
        double length2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

        if (length1 < 1e-3 || length2 < 1e-3) {
            return null;
        }
        double shift1X = -(dy1 / length1) * 20;
        double shift1Y = (dx1 / length1) * 20;
        double shift2X = -(dy2 / length2) * 20;
        double shift2Y = (dx2 / length2) * 20;
        double point1X = x2 + shift1X;
        double point1Y = y2 + shift1Y;
        double point2X = x2 + shift2X;
        double point2Y = y2 + shift2Y;
        double vector1X = point1X - x2;
        double vector1Y = point1Y - y2;
        double vector2X = point2X - x2;
        double vector2Y = point2Y - y2;
        double angle = Math.atan2(vector2Y, vector2X) - Math.atan2(vector1Y, vector1X);
        if (angle < 0) {
            angle += 2 * Math.PI;
        }

        float sweep = (float) (360 - (angle * 180 / Math.PI));
        float start = (float) ((Math.atan2(vector2Y, vector2X) * 180 / Math.PI));
        if (Float.isNaN(sweep) || Float.isNaN(start)) {
            return null;
        }
        return new ArrayList<>(Arrays.asList(sweep, start));
    }

    // Методы для работы с ресурсами
    private void loadBackgroundBitmap() {

        schemeBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.metro_map);
        // Заглушка: если есть спутниковая подложка, подставить сюда
        // satelliteBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.metro_satellite);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            bufferBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bufferCanvas = new Canvas(bufferBitmap);
            transformMatrix = new Matrix();
            needsRedraw = true;
        }
    }


    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateThemeColors();
        initializePaints();
        needsRedraw = true;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Обновляем цвета при присоединении к окну, когда контекст точно готов
        updateThemeColors();
        initializePaints();
        needsRedraw = true;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (Bitmap bitmap : cacheBitmaps.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        cacheBitmaps.clear();
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        needsRedraw = true;
        invalidate();
    }

    // Интерфейсы и геттеры/сеттеры
    public interface OnStationClickListener {
        void onStationClick(Station station);
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public float getTranslateX() {
        return translateX;
    }

    public float getTranslateY() {
        return translateY;
    }

    public void setTranslateX(float v) {
        translateX = v;
        updateTransformMatrix();
        needsRedraw = true;
        invalidate();
    }

    public void setTranslateY(float v) {
        translateY = v;
        updateTransformMatrix();
        needsRedraw = true;
        invalidate();
    }

    public Matrix getTransformMatrix() {
        return transformMatrix;
    }
}