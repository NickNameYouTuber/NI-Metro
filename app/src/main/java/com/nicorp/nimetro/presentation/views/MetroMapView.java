package com.nicorp.nimetro.presentation.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.models.MapObject;
import com.nicorp.nimetro.data.models.River;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Transfer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MetroMapView extends View {
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
    private float translateX = 0.0f;
    private float translateY = 0.0f;
    private GestureDetector gestureDetector;
    public ScaleGestureDetector scaleGestureDetector;
    public static final float COORDINATE_SCALE_FACTOR = 2.5f;
    private static final float CLICK_RADIUS = 30.0f;
    private Bitmap backgroundBitmap;
    private Map<Float, Bitmap> cacheBitmaps = new HashMap<>();
    public boolean needsRedraw = true;
    private Matrix transformMatrix;
    private Bitmap cacheBitmap;
    private Bitmap bufferBitmap;
    private Canvas bufferCanvas;
    private boolean isActiveMapMetro = true;
    private List<PointF> transferConnectionPoints = new ArrayList<>();

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

    private void initializePaints() {
        linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(15);

        stationPaint = new Paint();
        stationPaint.setColor(Color.BLUE);
        stationPaint.setStyle(Paint.Style.STROKE);
        stationPaint.setStrokeWidth(7);

        selectedStationPaint = new Paint();
        selectedStationPaint.setColor(Color.GREEN);
        selectedStationPaint.setStyle(Paint.Style.STROKE);
        selectedStationPaint.setStrokeWidth(5);

        whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Paint.Style.FILL);

        routePaint = new Paint();
        routePaint.setColor(Color.YELLOW);
        routePaint.setStrokeWidth(9);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(20);

        transferPaint = new Paint();
        transferPaint.setColor(Color.DKGRAY);
        transferPaint.setStrokeWidth(6);
        transferPaint.setStyle(Paint.Style.STROKE);

        stationCenterPaint = new Paint();
        stationCenterPaint.setColor(Color.parseColor("#00000000"));
        stationCenterPaint.setStyle(Paint.Style.STROKE);
        stationCenterPaint.setStrokeWidth(7);

        riverPaint = new Paint();
        riverPaint.setColor(Color.parseColor("#CCE0EA"));
        riverPaint.setStyle(Paint.Style.STROKE);
        riverPaint.setStrokeWidth(10);

        grayedPaint = new Paint();
        grayedPaint.setColor(Color.parseColor("#D9D9D9"));
        grayedPaint.setStrokeWidth(9);
    }

    private void initializeGestureDetectors() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                translateX -= distanceX / scaleFactor;
                translateY -= distanceY / scaleFactor;
                updateTransformMatrix();
                invalidate();
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

    // Методы для работы с данными
    public void setData(List<Line> lines, List<Station> stations, List<Transfer> transfers,
                        List<River> rivers, List<MapObject> mapObjects,
                        List<Line> grayedLines, List<Station> grayedStations) {
        this.lines = lines;
        this.stations = stations;
        this.transfers = transfers;
        this.rivers = rivers;
        this.mapObjects = mapObjects;
        this.grayedLines = grayedLines;
        this.grayedStations = grayedStations;
        needsRedraw = true;
        invalidate();
    }

    public void setRoute(List<Station> route) {
        this.route = route;
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

    public void setActiveMap(boolean isMetroMap) {
        this.isActiveMapMetro = isMetroMap;
        updateTransformMatrix(); // Обновляем матрицу трансформации
        needsRedraw = true;
        invalidate();
    }

    public void clearRoute() {
        this.route = null;
        needsRedraw = true;
        invalidate();
    }

    public void clearSelectedStations() {
        this.selectedStations = null;
        needsRedraw = true;
        invalidate();
    }

    // Методы для работы с отрисовкой
    @Override
    protected void onDraw(Canvas canvas) {
        if (bufferBitmap == null || needsRedraw) {
            createBufferBitmap();
        }
        transformMatrix.reset();
        transformMatrix.postTranslate(translateX, translateY);
        transformMatrix.postScale(scaleFactor, scaleFactor);
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, transformMatrix, null);
        }
        drawMapContents(canvas);
    }

    private void createBufferBitmap() {
        if (bufferBitmap != null) {
            bufferBitmap.recycle();
        }
        bufferBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bufferCanvas = new Canvas(bufferBitmap);
        needsRedraw = false;
    }

    private class LinePath {
        Path path;
        String color;
        Path innerPath; // Добавляем поле для внутреннего пути
        Paint whitePaint; // Добавляем поле для белой заливки

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
    }

    private class StationPath {
        Path path;
        String color;

        StationPath(Path path, String color) {
            this.path = path;
            this.color = color;
        }
    }
    private class MapPathCache {
        List<LinePath> linesPaths = new ArrayList<>();
        List<StationPath> stationsPaths = new ArrayList<>();
        Path transfersPath = new Path();
        Path riversPath = new Path();
        List<PartialCircle> partialCircles = new ArrayList<>();
        Path convexHullPath = new Path(); // Добавляем путь для выпуклой оболочки
        boolean isInitialized = false;
    }

    // Обновляем класс RoutePathCache, добавляя поля для переходов
    private class RoutePathCache {
        List<LinePath> routeLinesPaths = new ArrayList<>();
        List<StationPath> routeStationsPaths = new ArrayList<>();
        Path transfersPath = new Path();
        List<PartialCircle> partialCircles = new ArrayList<>();
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

    // Замените метод drawMapContents на:
    // Обновленный метод drawMapContents
    private void drawMapContents(Canvas canvas) {
        updateVisibleViewport();

        // Save initial canvas state
        int mainSaveCount = canvas.save();

        // Apply main transformation once
        canvas.concat(transformMatrix);

        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }

        // Initialize or update path cache if needed
        if (!pathCache.isInitialized || needsRedraw) {
            updatePathCache();
        }

        // Draw grayed lines/stations with transform
        if (grayedLines != null && !grayedLines.isEmpty()) {
            int saveCount = canvas.save();
            drawGrayedMap(canvas);
            canvas.restoreToCount(saveCount);
        }

        // Draw cached paths
        if (rivers != null) {
            canvas.drawPath(pathCache.riversPath, riverPaint);
        }

        // Draw lines
        for (LinePath linePath : pathCache.linesPaths) {
            linePaint.setColor(Color.parseColor(linePath.color));
            canvas.drawPath(linePath.path, linePaint);

            if (linePath.innerPath != null) {
                canvas.drawPath(linePath.innerPath, linePath.whitePaint);
            }
        }

        // Draw transfers
        canvas.drawPath(pathCache.transfersPath, transferPaint);

        // Draw partial circles
        for (PartialCircle partialCircle : pathCache.partialCircles) {
            if (partialCircle.color != null) {
                drawPartialCircleWithColor(canvas,
                        partialCircle.centerX, partialCircle.centerY,
                        partialCircle.radius, partialCircle.strokeWidth,
                        partialCircle.angles, partialCircle.color);
            } else {
                drawPartialCircle(canvas,
                        partialCircle.centerX, partialCircle.centerY,
                        partialCircle.radius, partialCircle.strokeWidth,
                        partialCircle.angles);
            }
        }

        // Draw convex hull
        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(pathCache.convexHullPath, fillPaint);

        // Draw stations
        for (StationPath stationPath : pathCache.stationsPaths) {
            Paint stationFillPaint = new Paint();
            stationFillPaint.setColor(Color.WHITE);
            stationFillPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(stationPath.path, stationFillPaint);

            Paint strokePaint = new Paint();
            strokePaint.setColor(Color.parseColor(stationPath.color));
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(7);
            canvas.drawPath(stationPath.path, strokePaint);
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

            // Draw route transfers
            Paint whiteTransferPaint = new Paint(transferPaint);
            whiteTransferPaint.setColor(Color.LTGRAY);
            whiteTransferPaint.setStrokeWidth(6);
            canvas.drawPath(routePathCache.transfersPath, whiteTransferPaint);

            // Draw route partial circles
            for (PartialCircle partialCircle : routePathCache.partialCircles) {
                drawPartialCircleWithColor(canvas,
                        partialCircle.centerX, partialCircle.centerY,
                        partialCircle.radius, partialCircle.strokeWidth,
                        partialCircle.angles, partialCircle.color);
            }

            // Draw route lines
            for (LinePath routeLinePath : routePathCache.routeLinesPaths) {
                routePaint.setColor(Color.parseColor(routeLinePath.color));
                canvas.drawPath(routeLinePath.path, routePaint);
            }

            // Draw route stations
            for (StationPath routeStationPath : routePathCache.routeStationsPaths) {
                Paint stationFillPaint = new Paint();
                stationFillPaint.setColor(Color.WHITE);
                stationFillPaint.setStyle(Paint.Style.FILL);
                canvas.drawPath(routeStationPath.path, stationFillPaint);

                Paint strokePaint = new Paint();
                strokePaint.setColor(Color.parseColor(routeStationPath.color));
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setStrokeWidth(7);
                canvas.drawPath(routeStationPath.path, strokePaint);
            }
        }

        // Restore main canvas state
        canvas.restoreToCount(mainSaveCount);

        needsRedraw = false;
    }

    private void addTransferPathToCache(Transfer transfer) {
        List<Station> stations = transfer.getStations();
        if (stations == null || stations.size() < 2) {
            return;
        }

        // Массив для хранения координат станций
        float[] coordinates = new float[stations.size() * 2];
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            float x = station.getX() * COORDINATE_SCALE_FACTOR;
            float y = station.getY() * COORDINATE_SCALE_FACTOR;
            coordinates[i * 2] = x;
            coordinates[i * 2 + 1] = y;
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
                    break;
                case "ground":
                    addDashedLineToCache(x1, y1, x2, y2);
                    break;
                default:
                    addShiftedLineToCache(x1, y1, x2, y2);
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

        // Создание выпуклой оболочки для заливки
        if (transferConnectionPoints.size() >= 3) {
            List<PointF> convexHullPoints = calculateConvexHull(transferConnectionPoints);
            if (!convexHullPoints.isEmpty()) {
                pathCache.convexHullPath.moveTo(convexHullPoints.get(0).x, convexHullPoints.get(0).y);
                for (int i = 1; i < convexHullPoints.size(); i++) {
                    pathCache.convexHullPath.lineTo(convexHullPoints.get(i).x, convexHullPoints.get(i).y);
                }
                pathCache.convexHullPath.close();
            }
        }
    }

    private void addShiftedLineToCache(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float shiftX = (dy / length) * 20;
        float shiftY = -(dx / length) * 20;
        shiftX = -shiftX;
        shiftY = -shiftY;

        pathCache.transfersPath.moveTo(x1 + shiftX, y1 + shiftY);
        pathCache.transfersPath.lineTo(x2 + shiftX, y2 + shiftY);
    }

    private void addDashedLineToCache(float x1, float y1, float x2, float y2) {
        pathCache.transfersPath.moveTo(x1, y1);
        pathCache.transfersPath.lineTo(x2, y2);
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

        pathCache.transfersPath.moveTo(x1 + shiftX, y1 + shiftY);
        pathCache.transfersPath.lineTo(halfX + shiftX, halfY + shiftY);

        pathCache.transfersPath.moveTo(halfX + shiftX, halfY + shiftY);
        pathCache.transfersPath.lineTo(x2 + shiftX, y2 + shiftY);
    }

    private Line findLineForStation(Station station) {
        for (Line line : lines) {
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
            routePathCache.partialCircles.clear();
            routePathCache.isInitialized = false;
            return;
        }

        routePathCache.routeLinesPaths.clear();
        routePathCache.routeStationsPaths.clear();
        routePathCache.transfersPath.reset();
        routePathCache.partialCircles.clear();

        for (int i = 0; i < route.size() - 1; i++) {
            Station station1 = route.get(i);
            Station station2 = route.get(i + 1);
            Line line = findLineForConnection(station1, station2);

            if (line != null) {
                Path routeLinePath = new Path();
                addLinePathToCache(station1, station2, line.getLineType(), routeLinePath);
                routePathCache.routeLinesPaths.add(new LinePath(routeLinePath, line.getColor()));
            } else {
                // Это переход между станциями
                addRouteTransferPathToCache(station1, station2);
            }
        }

        // Добавляем станции маршрута в кэш
        for (Station station : route) {
            Path stationPath = new Path();
            stationPath.addCircle(
                    station.getX() * COORDINATE_SCALE_FACTOR,
                    station.getY() * COORDINATE_SCALE_FACTOR,
                    14,
                    Path.Direction.CW
            );
            routePathCache.routeStationsPaths.add(new StationPath(stationPath, station.getColor()));
        }

        routePathCache.isInitialized = true;
    }

    private void addRouteTransferPathToCache(Station station1, Station station2) {
        List<Station> stations = Arrays.asList(station1, station2);

        // Массив для хранения координат станций
        float[] coordinates = new float[stations.size() * 2];
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            float x = station.getX() * COORDINATE_SCALE_FACTOR;
            float y = station.getY() * COORDINATE_SCALE_FACTOR;
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

            routePathCache.partialCircles.add(new PartialCircle(
                    currentX, currentY, 20, 6,
                    angles, "#cccccc"  // Используем белый цвет для переходов в маршруте
            ));
        }
    }

    // Добавьте новый метод для обновления кэша путей:
    private void updatePathCache() {
        // Очищаем все пути
        pathCache.linesPaths.clear();
        pathCache.stationsPaths.clear();
        pathCache.transfersPath.reset();
        pathCache.riversPath.reset();
        pathCache.partialCircles.clear();
        pathCache.convexHullPath.reset(); // Очищаем путь выпуклой оболочки

        // Кэшируем пути для рек
        if (rivers != null) {
            for (River river : rivers) {
                List<Point> points = river.getPoints();
                if (points.size() >= 2) {
                    Path riverPath = new Path();
                    riverPath.moveTo(points.get(0).x * COORDINATE_SCALE_FACTOR,
                            points.get(0).y * COORDINATE_SCALE_FACTOR);
                    for (int i = 1; i < points.size(); i++) {
                        riverPath.lineTo(points.get(i).x * COORDINATE_SCALE_FACTOR,
                                points.get(i).y * COORDINATE_SCALE_FACTOR);
                    }
                    pathCache.riversPath.addPath(riverPath);
                }
            }
        }

        // Кэшируем пути для линий метро
        Set<String> drawnConnections = new HashSet<>();
        for (Line line : lines) {
            Path linePath = new Path();
            String lineColor = line.getColor();

            for (Station station : line.getStations()) {
                for (Station.Neighbor neighbor : station.getNeighbors()) {
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

        // Кэшируем пути для станций с цветом линии
        for (Station station : stations) {
            float stationX = station.getX() * COORDINATE_SCALE_FACTOR;
            float stationY = station.getY() * COORDINATE_SCALE_FACTOR;

            // Находим линию, к которой принадлежит станция
            Line stationLine = findLineForStation(station);
            String stationColor = stationLine != null ? stationLine.getColor() : "#000000";

            // Создаем путь для станции
            Path stationPath = new Path();
            stationPath.addCircle(stationX, stationY, 14, Path.Direction.CW);

            // Добавляем путь станции в кэш с цветом линии
            pathCache.stationsPaths.add(new StationPath(stationPath, stationColor));
        }

        // Кэшируем пути для переходов, частичных кругов и выпуклой оболочки
        if (transfers != null) {
            for (Transfer transfer : transfers) {
                addTransferPathToCache(transfer);
            }
        }

        // Рассчитываем выпуклую оболочку для заливки переходов
        List<PointF> convexHullPoints = calculateConvexHull(transferConnectionPoints);
        if (!convexHullPoints.isEmpty()) {
            pathCache.convexHullPath.moveTo(convexHullPoints.get(0).x, convexHullPoints.get(0).y);
            for (int i = 1; i < convexHullPoints.size(); i++) {
                pathCache.convexHullPath.lineTo(convexHullPoints.get(i).x, convexHullPoints.get(i).y);
            }
            pathCache.convexHullPath.close();
        }

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
        float x1 = station1.getX() * COORDINATE_SCALE_FACTOR;
        float y1 = station1.getY() * COORDINATE_SCALE_FACTOR;
        float x2 = station2.getX() * COORDINATE_SCALE_FACTOR;
        float y2 = station2.getY() * COORDINATE_SCALE_FACTOR;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = -dy / length;
        float ny = dx / length;
        float offset = 6f; // Установите одинаковую ширину для всех линий

        // Создаем четырехугольный путь
        linePath.moveTo(x1 + nx * offset, y1 + ny * offset);
        linePath.lineTo(x2 + nx * offset, y2 + ny * offset);
        linePath.lineTo(x2 - nx * offset, y2 - ny * offset);
        linePath.lineTo(x1 - nx * offset, y1 - ny * offset);
        linePath.close();
    }

    private void addQuadrilateralWithIntermediatePointPathToCache(Station startStation, Station endStation, Point middlePoint, Path linePath) {
        float startX = startStation.getX() * COORDINATE_SCALE_FACTOR;
        float startY = startStation.getY() * COORDINATE_SCALE_FACTOR;
        float middleX = middlePoint.x * COORDINATE_SCALE_FACTOR;
        float middleY = middlePoint.y * COORDINATE_SCALE_FACTOR;
        float endX = endStation.getX() * COORDINATE_SCALE_FACTOR;
        float endY = endStation.getY() * COORDINATE_SCALE_FACTOR;

        float offset = 6f; // Установите одинаковую ширину для всех линий

        // Вычисляем перпендикулярные векторы для обоих сегментов
        float dx1 = middleX - startX;
        float dy1 = middleY - startY;
        float length1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
        float nx1 = -dy1 / length1;
        float ny1 = dx1 / length1;

        float dx2 = endX - middleX;
        float dy2 = endY - middleY;
        float length2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);
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

    private void addDoubleQuadrilateralBezierPathToCache(Station startStation, Station endStation, List<Point> intermediatePoints, Path linePath) {
        Point start = new Point(startStation.getX(), startStation.getY());
        Point control1 = intermediatePoints.get(0);
        Point control2 = intermediatePoints.get(1);
        Point end = new Point(endStation.getX(), endStation.getY());

        float offset = 3f; // Смещение для верхней и нижней кривых
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = dy / length;
        float ny = -dx / length;

        // Верхняя кривая
        float x1Start = (start.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Start = (start.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1Control1 = (control1.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Control1 = (control1.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1Control2 = (control2.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Control2 = (control2.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1End = (end.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1End = (end.y + ny * offset) * COORDINATE_SCALE_FACTOR;

        // Нижняя кривая
        float x2Start = (start.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Start = (start.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2Control1 = (control1.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Control1 = (control1.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2Control2 = (control2.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Control2 = (control2.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2End = (end.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2End = (end.y - ny * offset) * COORDINATE_SCALE_FACTOR;

        // Создаем замкнутый четырехугольник из верхней и нижней кривых
        Path outerPath = new Path();
        outerPath.moveTo(x1Start, y1Start);
        outerPath.cubicTo(x1Control1, y1Control1, x1Control2, y1Control2, x1End, y1End);
        outerPath.lineTo(x2End, y2End);
        outerPath.cubicTo(x2Control2, y2Control2, x2Control1, y2Control1, x2Start, y2Start);
        outerPath.close();

        // Создаем внутреннюю белую кривую (четырехугольную)
        Path innerPath = new Path();
        float innerOffset = offset / 2; // Смещение для внутренней кривой (половина от внешнего)

        // Верхняя внутренняя кривая
        float x1InnerStart = (start.x + nx * innerOffset) * COORDINATE_SCALE_FACTOR;
        float y1InnerStart = (start.y + ny * innerOffset) * COORDINATE_SCALE_FACTOR;
        float x1InnerControl1 = (control1.x + nx * innerOffset) * COORDINATE_SCALE_FACTOR;
        float y1InnerControl1 = (control1.y + ny * innerOffset) * COORDINATE_SCALE_FACTOR;
        float x1InnerControl2 = (control2.x + nx * innerOffset) * COORDINATE_SCALE_FACTOR;
        float y1InnerControl2 = (control2.y + ny * innerOffset) * COORDINATE_SCALE_FACTOR;
        float x1InnerEnd = (end.x + nx * innerOffset) * COORDINATE_SCALE_FACTOR;
        float y1InnerEnd = (end.y + ny * innerOffset) * COORDINATE_SCALE_FACTOR;

        // Нижняя внутренняя кривая
        float x2InnerStart = (start.x - nx * innerOffset) * COORDINATE_SCALE_FACTOR;
        float y2InnerStart = (start.y - ny * innerOffset) * COORDINATE_SCALE_FACTOR;
        float x2InnerControl1 = (control1.x - nx * innerOffset) * COORDINATE_SCALE_FACTOR;
        float y2InnerControl1 = (control1.y - ny * innerOffset) * COORDINATE_SCALE_FACTOR;
        float x2InnerControl2 = (control2.x - nx * innerOffset) * COORDINATE_SCALE_FACTOR;
        float y2InnerControl2 = (control2.y - ny * innerOffset) * COORDINATE_SCALE_FACTOR;
        float x2InnerEnd = (end.x - nx * innerOffset) * COORDINATE_SCALE_FACTOR;
        float y2InnerEnd = (end.y - ny * innerOffset) * COORDINATE_SCALE_FACTOR;

        // Создаем замкнутый четырехугольник для внутренней кривой
        innerPath.moveTo(x1InnerStart, y1InnerStart);
        innerPath.cubicTo(x1InnerControl1, y1InnerControl1, x1InnerControl2, y1InnerControl2, x1InnerEnd, y1InnerEnd);
        innerPath.lineTo(x2InnerEnd, y2InnerEnd);
        innerPath.cubicTo(x2InnerControl2, y2InnerControl2, x2InnerControl1, y2InnerControl1, x2InnerStart, y2InnerStart);
        innerPath.close();

        // Устанавливаем цвет для внутренней кривой (белый)
        Paint whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Paint.Style.FILL); // Заливка белым цветом
        whitePaint.setStrokeWidth(6); // Ширина линии

        // Добавляем внешний и внутренний пути в кэш
        pathCache.linesPaths.add(new LinePath(outerPath, startStation.getColor(), innerPath, whitePaint));
    }

    private void addDoubleStraightLineToCache(Station station1, Station station2, Path linePath) {
        float x1 = station1.getX() * COORDINATE_SCALE_FACTOR;
        float y1 = station1.getY() * COORDINATE_SCALE_FACTOR;
        float x2 = station2.getX() * COORDINATE_SCALE_FACTOR;
        float y2 = station2.getY() * COORDINATE_SCALE_FACTOR;

        // Вычисляем перпендикулярный вектор для смещения
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = -dy / length; // Нормаль по оси X
        float ny = dx / length;  // Нормаль по оси Y

        float offset = 6f; // Смещение для двойной линии

        // Внешние линии
        Path outerPath = new Path();
        outerPath.moveTo(x1 + nx * offset, y1 + ny * offset);
        outerPath.lineTo(x2 + nx * offset, y2 + ny * offset);
        outerPath.lineTo(x2 - nx * offset, y2 - ny * offset);
        outerPath.lineTo(x1 - nx * offset, y1 - ny * offset);
        outerPath.close();

        // Внутренняя белая линия
        Path innerPath = new Path();
        innerPath.moveTo(x1, y1);
        innerPath.lineTo(x2, y2);

        // Устанавливаем цвет для внутренней линии (белый)
        Paint whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Paint.Style.STROKE);
        whitePaint.setStrokeWidth(6);

        // Добавляем внешний и внутренний пути в кэш
        pathCache.linesPaths.add(new LinePath(outerPath, station1.getColor(), innerPath, whitePaint));
    }

    private void addQuadrilateralBezierPathToCache(Station startStation, Station endStation, List<Point> intermediatePoints, Path linePath) {
        Point start = new Point(startStation.getX(), startStation.getY());
        Point control1 = intermediatePoints.get(0);
        Point control2 = intermediatePoints.get(1);
        Point end = new Point(endStation.getX(), endStation.getY());

        float offset = 3f; // Уменьшаем offset до 2.5f для соответствия с QuadrilateralLine
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = dy / length;
        float ny = -dx / length;

        // Верхняя кривая
        float x1Start = (start.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Start = (start.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1Control1 = (control1.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Control1 = (control1.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1Control2 = (control2.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Control2 = (control2.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1End = (end.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1End = (end.y + ny * offset) * COORDINATE_SCALE_FACTOR;

        // Нижняя кривая
        float x2Start = (start.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Start = (start.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2Control1 = (control1.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Control1 = (control1.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2Control2 = (control2.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Control2 = (control2.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2End = (end.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2End = (end.y - ny * offset) * COORDINATE_SCALE_FACTOR;

        // Создаем замкнутый четырехугольный путь
        linePath.moveTo(x1Start, y1Start);
        linePath.cubicTo(x1Control1, y1Control1, x1Control2, y1Control2, x1End, y1End);
        linePath.lineTo(x2End, y2End);
        linePath.cubicTo(x2Control2, y2Control2, x2Control1, y2Control1, x2Start, y2Start);
        linePath.close();
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
            float x = station.getX() * COORDINATE_SCALE_FACTOR;
            float y = station.getY() * COORDINATE_SCALE_FACTOR;

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

    private boolean isInGrayedLines(List<Line> linesToCheck) {
        if (grayedLines == null || linesToCheck == null) {
            return false;
        }

        // Проверяем, являются ли линии частью grayedLines
        Set<String> grayedLineIds = new HashSet<>();
        for (Line line : grayedLines) {
            grayedLineIds.add(line.getId());
        }

        for (Line line : linesToCheck) {
            if (!grayedLineIds.contains(line.getId())) {
                return false;
            }
        }
        return true;
    }

    private void drawMapObjects(Canvas canvas) {
        if (mapObjects != null) {
            for (MapObject mapObject : mapObjects) {
                drawMapObject(canvas, mapObject);
            }
        }
    }

    private void drawMapObject(Canvas canvas, MapObject mapObject) {
        float objectX = mapObject.getPosition().x * COORDINATE_SCALE_FACTOR;
        float objectY = mapObject.getPosition().y * COORDINATE_SCALE_FACTOR;
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

        // Отрисовка серых линий
        for (Line line : grayedLines) {
            drawGrayedLines(canvas, line, drawnConnections, grayedLinePaint);
        }

        Log.d("drawGrayedMap", "grayedLines: " + grayedLines.size());
        Log.d("drawGrayedMap", "lines: " + lines.size());
        Log.d("drawGrayedMap", "isActiveMapMetro: " + isActiveMapMetro);
        Log.d("drawGrayedMap", "isInGrayedLines(lines): " + isInGrayedLines(lines));
        Log.d("drawGrayedMap", "isInGrayedLines(grayedLines): " + isInGrayedLines(grayedLines));
        // Отрисовка станций только если это не активная карта
    }

    private void drawGrayedLines(Canvas canvas, Line line,
                                 Set<String> drawnConnections, Paint grayedLinePaint) {
        for (Station station : line.getStations()) {
            for (Station.Neighbor neighbor : station.getNeighbors()) {
                Station neighborStation = findStationById(
                        neighbor.getStation().getId(), grayedStations);
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
        if (line.isCircle() && line.getStations().size() > 1) {
            Station firstStation = line.getStations().get(0);
            Station lastStation = line.getStations().get(
                    line.getStations().size() - 1);
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
            canvas.drawCircle(point.x * COORDINATE_SCALE_FACTOR, point.y * COORDINATE_SCALE_FACTOR, 10, paint);
        }
    }

    private void drawLineWithIntermediatePoints(Canvas canvas, Station station1, Station station2, String lineType, Paint paint) {
        Station startStation = station1.getId().compareTo(station2.getId()) < 0 ? station1 : station2;
        Station endStation = station1.getId().compareTo(station2.getId()) < 0 ? station2 : station1;
        List<Point> intermediatePoints = startStation.getIntermediatePoints(endStation);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        if (intermediatePoints == null || intermediatePoints.isEmpty()) {
            if (lineType.equals("double")) {
                drawDoubleLine(canvas, startStation, endStation, paint);
            } else {
                canvas.drawLine(
                        startStation.getX() * COORDINATE_SCALE_FACTOR,
                        startStation.getY() * COORDINATE_SCALE_FACTOR,
                        endStation.getX() * COORDINATE_SCALE_FACTOR,
                        endStation.getY() * COORDINATE_SCALE_FACTOR,
                        paint
                );
            }
        } else if (intermediatePoints.size() == 1) {
            float startX = startStation.getX() * COORDINATE_SCALE_FACTOR;
            float startY = startStation.getY() * COORDINATE_SCALE_FACTOR;
            float middleX = intermediatePoints.get(0).x * COORDINATE_SCALE_FACTOR;
            float middleY = intermediatePoints.get(0).y * COORDINATE_SCALE_FACTOR;
            float endX = endStation.getX() * COORDINATE_SCALE_FACTOR;
            float endY = endStation.getY() * COORDINATE_SCALE_FACTOR;
            canvas.drawLine(startX, startY, middleX, middleY, paint);
            canvas.drawLine(middleX, middleY, endX, endY, paint);
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
        float x1 = station1.getX() * COORDINATE_SCALE_FACTOR;
        float y1 = station1.getY() * COORDINATE_SCALE_FACTOR;
        float x2 = station2.getX() * COORDINATE_SCALE_FACTOR;
        float y2 = station2.getY() * COORDINATE_SCALE_FACTOR;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = dx / length;
        float ny = dy / length;
        float perpX = -ny * 5;
        float perpY = nx * 5;
        Paint whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStrokeWidth(6);
        canvas.drawLine(x1 + perpX, y1 + perpY, x2 + perpX, y2 + perpY, paint);
        canvas.drawLine(x1 - perpX, y1 - perpY, x2 - perpX, y2 - perpY, paint);
        canvas.drawLine(x1, y1, x2, y2, whitePaint);
    }

    private void drawBezierCurve(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {
        Path path = new Path();
        paint.setStyle(Paint.Style.STROKE);
        path.moveTo(start.x * COORDINATE_SCALE_FACTOR, start.y * COORDINATE_SCALE_FACTOR);
        path.cubicTo(
                control1.x * COORDINATE_SCALE_FACTOR, control1.y * COORDINATE_SCALE_FACTOR,
                control2.x * COORDINATE_SCALE_FACTOR, control2.y * COORDINATE_SCALE_FACTOR,
                end.x * COORDINATE_SCALE_FACTOR, end.y * COORDINATE_SCALE_FACTOR
        );
        canvas.drawPath(path, paint);
    }

    private void drawDoubleBezierCurve(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {
        Path path1 = new Path();
        Path path2 = new Path();
        Path fillPath = new Path();
        paint.setStrokeWidth(6);
        float offset = 2.5f;
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = dy / length;
        float ny = -dx / length;
        float x1Start = (start.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Start = (start.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1Control1 = (control1.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Control1 = (control1.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1Control2 = (control2.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Control2 = (control2.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1End = (end.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1End = (end.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2Start = (start.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Start = (start.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2Control1 = (control1.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Control1 = (control1.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2Control2 = (control2.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Control2 = (control2.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2End = (end.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2End = (end.y - ny * offset) * COORDINATE_SCALE_FACTOR;
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
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(path1, paint);
        canvas.drawPath(path2, paint);
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

    private void drawPartialCircleWithColor(Canvas canvas, float centerX, float centerY, float radius, float strokeWidth, List<Float> angles, String color) {
        Paint circleOutlinePaint = new Paint();
        circleOutlinePaint.setColor(Color.parseColor(color));
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
                (points[0] / COORDINATE_SCALE_FACTOR) - padding,
                (points[1] / COORDINATE_SCALE_FACTOR) - padding,
                (points[2] / COORDINATE_SCALE_FACTOR) + padding,
                (points[3] / COORDINATE_SCALE_FACTOR) + padding
        );
    }

    // Методы для работы с жестами и событиями
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = scaleGestureDetector.onTouchEvent(event);
        result = gestureDetector.onTouchEvent(event) || result;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            updateVisibleViewport();
            float[] point = new float[] {event.getX(), event.getY()};
            Matrix inverseMatrix = new Matrix();
            transformMatrix.invert(inverseMatrix);
            inverseMatrix.mapPoints(point);

            float x = point[0];
            float y = point[1];

            Station clickedStation = findStationAt(
                    x / COORDINATE_SCALE_FACTOR,
                    y / COORDINATE_SCALE_FACTOR);

            if (clickedStation != null && listener != null) {
                listener.onStationClick(clickedStation);
                return true;
            }
        }

        return result || super.onTouchEvent(event);
    }

    public Station findStationAt(float x, float y) {
        List<Station> activeStations = stations;
        for (Station station : activeStations) {
            if (Math.abs(station.getX() - x) < CLICK_RADIUS / COORDINATE_SCALE_FACTOR && Math.abs(station.getY() - y) < CLICK_RADIUS / COORDINATE_SCALE_FACTOR) {
                return station;
            }
        }
        return null;
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
        for (Line line : grayedLines) {
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
        return new ArrayList<>(Arrays.asList((float) (360 - (angle * 180 / Math.PI)), (float) ((Math.atan2(vector2Y, vector2X) * 180 / Math.PI))));
    }

    // Методы для работы с ресурсами
    private void loadBackgroundBitmap() {
        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.metro_map);
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

