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

import com.google.android.material.color.MaterialColors;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.models.MapObject;
import com.nicorp.nimetro.data.models.River;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Transfer;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MetroMapView extends View {

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

    public GestureDetector gestureDetector;
    public ScaleGestureDetector scaleGestureDetector;

    public static final float COORDINATE_SCALE_FACTOR = 2.5f;
    private static final float CLICK_RADIUS = 30.0f;
    private static final float TRANSFER_CAPSULE_WIDTH = 40.0f;

    private Bitmap backgroundBitmap;
    private Map<Float, Bitmap> cacheBitmaps = new HashMap<>(); // Кэшированные битмапы для разных уровней детализации
    public boolean needsRedraw = true; // Флаг, указывающий на необходимость перерисовки
    private Matrix transformMatrix; // Матрица трансформации для перемещения и масштабирования
    private Bitmap cacheBitmap; // Битмап для кэширования всей карты

    private boolean isActiveMapMetro = true;

    public MetroMapView(Context context) {
        super(context);
        init();
        translateY = -300;
        translateX = -1600;
        scaleFactor = 0.4f;

        updateTransformMatrix();
        invalidate();
    }

    public MetroMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        translateY = -300;
        translateX = -1600;
        scaleFactor = 0.4f;

        updateTransformMatrix();
        invalidate();
    }

    public MetroMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        translateY = -300;
        translateX = -1600;
        scaleFactor = 0.4f;

        updateTransformMatrix();
        invalidate();
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

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        needsRedraw = true; // Помечаем, что требуется перерисовка
        invalidate();
    }

    private void init() {
        initializePaints();
        initializeGestureDetectors();
        transformMatrix = new Matrix();
        if (isEditMode) {
            loadBackgroundBitmap();
        }
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

        int colorOnSurface = Color.WHITE;

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(20);

        transferPaint = new Paint();
        transferPaint.setColor(Color.DKGRAY);
        transferPaint.setStrokeWidth(10);
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
                needsRedraw = true; // Помечаем, что требуется перерисовка
                invalidate();
                return true;
            }
        });
    }

    public void updateTransformMatrix() {
        Log.d("MetroMapView", "Transform matrix: " + translateX + ", " + translateY + ", " + scaleFactor);
        transformMatrix.reset();
        transformMatrix.postTranslate(translateX, translateY);
        transformMatrix.postScale(scaleFactor, scaleFactor);
    }

    private void loadBackgroundBitmap() {
        Log.d("MetroMapView", "Loading background bitmap...");
        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.metro_map);
    }

    public void clearRoute() {
        this.route = null;
        needsRedraw = true; // Помечаем, что требуется перерисовка
        invalidate();
    }

    public void clearSelectedStations() {
        this.selectedStations = null;
        needsRedraw = true; // Помечаем, что требуется перерисовка
        invalidate();
    }

    public void setData(List<Line> lines, List<Station> stations, List<Transfer> transfers, List<River> rivers, List<MapObject> mapObjects, List<Line> grayedLines, List<Station> grayedStations) {
        this.lines = lines;
        this.stations = stations;
        this.transfers = transfers;
        this.rivers = rivers;
        this.mapObjects = mapObjects;
        this.grayedLines = grayedLines;
        this.grayedStations = grayedStations;
        needsRedraw = true; // Помечаем, что требуется перерисовка
        invalidate();
    }

    public void setRoute(List<Station> route) {
        this.route = route;
        needsRedraw = true; // Помечаем, что требуется перерисовка
        invalidate();
    }

    public void setSelectedStations(List<Station> selectedStations) {
        this.selectedStations = selectedStations;
        needsRedraw = true; // Помечаем, что требуется перерисовка
        invalidate();
    }

    public void setOnStationClickListener(OnStationClickListener listener) {
        this.listener = listener;
    }

    public void setActiveMap(boolean isMetroMap) {
        this.isActiveMapMetro = isMetroMap;
        needsRedraw = true; // Помечаем, что требуется перерисовка
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Если необходима перерисовка кэша
        if (needsRedraw) {
            Log.d("MetroMapView", "Redrawing cache...");
            createCacheBitmap(10000, 10000);
            needsRedraw = false;
        }

        // Выбираем подходящий уровень детализации
        float lodScaleFactor = getLodScaleFactor();
        Bitmap cacheBitmap = cacheBitmaps.get(lodScaleFactor);
        if (cacheBitmap == null) {
            cacheBitmap = createCacheBitmap(6000, 7000);
            cacheBitmaps.put(lodScaleFactor, cacheBitmap);
        }

        // Отрисовываем кэшированный битмап с применением трансформации
        canvas.drawBitmap(cacheBitmap, transformMatrix, null);
    }

    private float getLodScaleFactor() {
        if (scaleFactor < 0.5f) {
            return 0.5f;
        } else if (scaleFactor < 1.0f) {
            return 1.0f;
        } else {
            return 2.0f;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            // Создаем кэш-битмап при изменении размера view
            createCacheBitmap(w, h);
            needsRedraw = true;
        }
    }

    private Bitmap createCacheBitmap(int width, int height) {
        // Освобождаем память от старого битмапа, если он существует
        cacheBitmaps = new HashMap<>();
        cacheBitmap = null;
        Log.d("MetroMapView", "Cache bitmap cleared");

        // Создаем новый битмап для кэширования
        Bitmap cacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas cacheCanvas = new Canvas(cacheBitmap);

        // Отрисовываем все элементы карты в кэш
        drawMapContents(cacheCanvas);

        return cacheBitmap;
    }

    private void drawMapContents(Canvas canvas) {
        // Отрисовка фонового изображения
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }

        if (grayedLines != null && grayedStations != null && !grayedLines.isEmpty() && !grayedStations.isEmpty()) {
            Log.d("MetroMapView", "Station 1: " + grayedStations.get(0).getName());
            drawGrayedMap(canvas);
        }

        if (lines != null && stations != null) {
            Log.d("MetroMapView", "Station 2: " + stations.get(0).getName());
            drawRivers(canvas);
            drawLines(canvas);
            drawTransfers(canvas);
            drawStations(canvas);
            drawRoute(canvas);
            drawMapObjects(canvas);

            if (isEditMode) {
                drawIntermediatePoints(canvas);
            }
        }
    }

    private void drawGrayedMap(Canvas canvas) {
        Set<String> drawnConnections = new HashSet<>();
        Paint grayedLinePaint = new Paint(grayedPaint);
        grayedLinePaint.setStrokeWidth(9);

        Paint grayedStationPaint = new Paint(grayedPaint);
        grayedStationPaint.setStyle(Paint.Style.STROKE);
        grayedStationPaint.setStrokeWidth(7);

        Paint grayedTextPaint = new Paint(grayedPaint);
        grayedTextPaint.setTextSize(20);

        for (Line line : grayedLines) {
            for (Station station : line.getStations()) {
                for (Station.Neighbor neighbor : station.getNeighbors()) {
                    Station neighborStation = findStationById(neighbor.getStation().getId(), grayedStations);
                    if (neighborStation != null && line.getLineIdForStation(neighborStation) != null) {
                        Log.d("MetroMapView", "Drawing connection: " + station.getId() + " - " + neighborStation.getId());
                        String connectionKey = Integer.parseInt( station.getId().split("_")[1]) < Integer.parseInt(neighborStation.getId().split("_")[1])
                                ? station.getId() + "-" + neighborStation.getId()
                                : neighborStation.getId() + "-" + station.getId();

                        if (!drawnConnections.contains(connectionKey)) {
                            drawLineWithIntermediatePoints(canvas, station, neighborStation, line.getLineType(), grayedLinePaint);
                            drawnConnections.add(connectionKey);
                        }
                    }
                }
            }

            if (line.isCircle() && line.getStations().size() > 1) {
                Station firstStation = line.getStations().get(0);
                Station lastStation = line.getStations().get(line.getStations().size() - 1);
                String connectionKey = Integer.parseInt(firstStation.getId().split("_")[1]) < Integer.parseInt(lastStation.getId().split("_")[1])
                        ? firstStation.getId() + "-" + lastStation.getId()
                        : lastStation.getId() + "-" + firstStation.getId();

                if (!drawnConnections.contains(connectionKey)) {
                    drawLineWithIntermediatePoints(canvas, firstStation, lastStation, line.getLineType(), grayedLinePaint);
                    drawnConnections.add(connectionKey);
                }
            }
        }

        for (Station station : grayedStations) {
            float stationX = station.getX() * COORDINATE_SCALE_FACTOR;
            float stationY = station.getY() * COORDINATE_SCALE_FACTOR;

            canvas.drawCircle(stationX, stationY, 10, whitePaint);
            canvas.drawCircle(stationX, stationY, 14, grayedStationPaint);

            drawTextBasedOnPosition(canvas, station.getName(), stationX, stationY, station.getTextPosition(), grayedTextPaint);
        }
    }

    private void drawRivers(Canvas canvas) {
        if (rivers != null) {
            for (River river : rivers) {
                drawRiver(canvas, river);
            }
        }
    }

    private void drawLines(Canvas canvas) {
        Set<String> drawnConnections = new HashSet<>();

        for (Line line : lines) {
            linePaint.setColor(Color.parseColor(line.getColor()));
            for (Station station : line.getStations()) {
                for (Station.Neighbor neighbor : station.getNeighbors()) {
                    Station neighborStation = findStationById(neighbor.getStation().getId(), stations);
                    if (neighborStation != null && line.getLineIdForStation(neighborStation) != null) {
                        String connectionKey = station.getId().compareTo(neighborStation.getId()) < 0
                                ? station.getId() + "-" + neighborStation.getId()
                                : neighborStation.getId() + "-" + station.getId();

                        if (!drawnConnections.contains(connectionKey)) {
                            drawLineWithIntermediatePoints(canvas, station, neighborStation, line.getLineType(), linePaint);
                            drawnConnections.add(connectionKey);
                        }
                    }
                }
            }

            if (line.isCircle() && line.getStations().size() > 1) {
                Station firstStation = line.getStations().get(0);
                Station lastStation = line.getStations().get(line.getStations().size() - 1);
                String connectionKey = firstStation.getId().compareTo(lastStation.getId()) < 0
                        ? firstStation.getId() + "-" + lastStation.getId()
                        : lastStation.getId() + "-" + firstStation.getId();

                if (!drawnConnections.contains(connectionKey)) {
                    drawLineWithIntermediatePoints(canvas, firstStation, lastStation, line.getLineType(), linePaint);
                    drawnConnections.add(connectionKey);
                }
            }
        }
    }

    private Station findStationById(String id, List<Station> stations) {
        for (Station station : stations) {
            if (station.getId().equals(id)) {
                return station;
            }
        }
        return null;
    }

    private void drawTransfers(Canvas canvas) {
        if (transfers == null || transfers.isEmpty()) {
            return;
        }
        for (Transfer transfer : transfers) {
            List<Station> transferStations = transfer.getStations();
            drawTransferConnection(canvas, transferStations, transfer.getType());
        }
    }

    private void drawStations(Canvas canvas) {
        for (Station station : stations) {
            float stationX = station.getX() * COORDINATE_SCALE_FACTOR;
            float stationY = station.getY() * COORDINATE_SCALE_FACTOR;

            canvas.drawCircle(stationX, stationY, 10, whitePaint);
            stationPaint.setColor(Color.parseColor(station.getColor()));
            canvas.drawCircle(stationX, stationY, 14, stationPaint);

            if (selectedStations != null && selectedStations.contains(station)) {
                canvas.drawCircle(stationX, stationY, 20, selectedStationPaint);
            }

            drawTextBasedOnPosition(canvas, station.getName(), stationX, stationY, station.getTextPosition(), textPaint);
        }
    }

    private void drawRoute(Canvas canvas) {
        if (route != null && route.size() > 1) {
            for (int i = 0; i < route.size() - 1; i++) {
                Station station1 = route.get(i);
                Station station2 = route.get(i + 1);
                drawRouteWithIntermediatePoints(canvas, station1, station2);
            }
        }
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

    private void drawRiver(Canvas canvas, River river) {
        List<Point> points = river.getPoints();
        int width = river.getWidth();

        if (points.size() < 2) {
            return;
        }

        Path riverPath = new Path();
        riverPath.moveTo(points.get(0).x * COORDINATE_SCALE_FACTOR, points.get(0).y * COORDINATE_SCALE_FACTOR);
        for (int i = 1; i < points.size(); i++) {
            riverPath.lineTo(points.get(i).x * COORDINATE_SCALE_FACTOR, points.get(i).y * COORDINATE_SCALE_FACTOR);
        }

        float riverLength = calculateRiverLength(points);

        Point startPoint = points.get(0);
        Point endPoint = points.get(points.size() - 1);
        int[] fadeColors = {Color.parseColor("#00000000"), Color.parseColor("#ADD8E6"), Color.parseColor("#ADD8E6"), Color.parseColor("#00000000")};
        float fadeMargin = 40 / riverLength;
        float[] fadePositions = {0.0f, fadeMargin, 1.0f - fadeMargin, 1.0f};

        LinearGradient fadeGradient = new LinearGradient(
                startPoint.x * COORDINATE_SCALE_FACTOR, startPoint.y * COORDINATE_SCALE_FACTOR,
                endPoint.x * COORDINATE_SCALE_FACTOR, endPoint.y * COORDINATE_SCALE_FACTOR,
                fadeColors, fadePositions, Shader.TileMode.CLAMP);

        riverPaint.setShader(fadeGradient);
        riverPaint.setStrokeWidth(width);

        canvas.drawPath(riverPath, riverPaint);
    }

    private float calculateRiverLength(List<Point> points) {
        float riverLength = 0;
        for (int i = 1; i < points.size(); i++) {
            Point p1 = points.get(i - 1);
            Point p2 = points.get(i);
            riverLength += Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
        }
        return riverLength;
    }

    private void drawLineWithIntermediatePoints(Canvas canvas, Station station1, Station station2, String lineType, Paint paint) {
        List<Point> intermediatePoints = station1.getIntermediatePoints(station2);
        if (intermediatePoints == null || intermediatePoints.isEmpty()) {
            if (lineType.equals("double")) {
                drawDoubleLine(canvas, station1, station2, paint);
            } else {
                paint.setStrokeWidth(10);
                canvas.drawLine(station1.getX() * COORDINATE_SCALE_FACTOR, station1.getY() * COORDINATE_SCALE_FACTOR,
                        station2.getX() * COORDINATE_SCALE_FACTOR, station2.getY() * COORDINATE_SCALE_FACTOR, paint);
            }
        } else if (intermediatePoints.size() == 1) {
            float startX = station1.getX() * COORDINATE_SCALE_FACTOR;
            float startY = station1.getY() * COORDINATE_SCALE_FACTOR;
            float endX = intermediatePoints.get(0).x * COORDINATE_SCALE_FACTOR;
            float endY = intermediatePoints.get(0).y * COORDINATE_SCALE_FACTOR;
            canvas.drawLine(startX, startY, endX, endY, paint);
            canvas.drawLine(endX, endY, station2.getX() * COORDINATE_SCALE_FACTOR, station2.getY() * COORDINATE_SCALE_FACTOR, paint);
        } else if (intermediatePoints.size() == 2) {
            Point start = new Point(station1.getX(), station1.getY());
            Point control1 = intermediatePoints.get(0);
            Point control2 = intermediatePoints.get(1);
            Point end = new Point(station2.getX(), station2.getY());
            if (lineType.equals("double")) {
                drawDoubleBezierCurve(canvas, start, control1, control2, end, paint);
            } else {
                drawBezierCurve(canvas, start, control1, control2, end, paint);
            }
        }
    }

    private void drawRouteWithIntermediatePoints(Canvas canvas, Station station1, Station station2) {
        List<Point> intermediatePoints = station1.getIntermediatePoints(station2);
        if (intermediatePoints == null || intermediatePoints.isEmpty()) {
            canvas.drawLine(station1.getX() * COORDINATE_SCALE_FACTOR, station1.getY() * COORDINATE_SCALE_FACTOR,
                    station2.getX() * COORDINATE_SCALE_FACTOR, station2.getY() * COORDINATE_SCALE_FACTOR, routePaint);
        } else if (intermediatePoints.size() == 1) {
            float startX = station1.getX() * COORDINATE_SCALE_FACTOR;
            float startY = station1.getY() * COORDINATE_SCALE_FACTOR;
            float endX = intermediatePoints.get(0).x * COORDINATE_SCALE_FACTOR;
            float endY = intermediatePoints.get(0).y * COORDINATE_SCALE_FACTOR;
            canvas.drawLine(startX, startY, endX, endY, routePaint);
            canvas.drawLine(endX, endY, station2.getX() * COORDINATE_SCALE_FACTOR, station2.getY() * COORDINATE_SCALE_FACTOR, routePaint);
        } else if (intermediatePoints.size() == 2) {
            Point start = new Point(station1.getX(), station1.getY());
            Point control1 = intermediatePoints.get(0);
            Point control2 = intermediatePoints.get(1);
            Point end = new Point(station2.getX(), station2.getY());
            drawBezierCurve(canvas, start, control1, control2, end, routePaint);
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

    private void drawDoubleBezierCurve(Canvas canvas, Point start, Point control1, Point control2, Point end, Paint paint) {
        // Создаем две кривые Безье, смещенные относительно друг друга
        Path path1 = new Path();
        Path path2 = new Path();
        Path fillPath = new Path();

        paint.setStrokeWidth(6);

        // Вычисляем смещение для кривых
        float offset = 2.5f; // Смещение в пикселях (половина ширины линии)
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = dy / length;
        float ny = -dx / length;

        // Точки для первой кривой (смещение вправо)
        float x1Start = (start.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Start = (start.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1Control1 = (control1.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Control1 = (control1.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1Control2 = (control2.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1Control2 = (control2.y + ny * offset) * COORDINATE_SCALE_FACTOR;
        float x1End = (end.x + nx * offset) * COORDINATE_SCALE_FACTOR;
        float y1End = (end.y + ny * offset) * COORDINATE_SCALE_FACTOR;

        // Точки для второй кривой (смещение влево)
        float x2Start = (start.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Start = (start.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2Control1 = (control1.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Control1 = (control1.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2Control2 = (control2.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2Control2 = (control2.y - ny * offset) * COORDINATE_SCALE_FACTOR;
        float x2End = (end.x - nx * offset) * COORDINATE_SCALE_FACTOR;
        float y2End = (end.y - ny * offset) * COORDINATE_SCALE_FACTOR;

        // Первая кривая
        path1.moveTo(x1Start, y1Start);
        path1.cubicTo(x1Control1, y1Control1, x1Control2, y1Control2, x1End, y1End);

        // Вторая кривая
        path2.moveTo(x2Start, y2Start);
        path2.cubicTo(x2Control1, y2Control1, x2Control2, y2Control2, x2End, y2End);

        // Создаем путь для заливки
        fillPath.moveTo(x1Start, y1Start);
        fillPath.cubicTo(x1Control1, y1Control1, x1Control2, y1Control2, x1End, y1End);
        fillPath.lineTo(x2End, y2End);
        fillPath.cubicTo(x2Control2, y2Control2, x2Control1, y2Control1, x2Start, y2Start);
        fillPath.close(); // Замыкаем путь

        // Устанавливаем белую заливку
        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);

        // Заливаем область между кривыми
        canvas.drawPath(fillPath, fillPaint);

        // Рисуем обе кривые
        canvas.drawPath(path1, paint);
        canvas.drawPath(path2, paint);
    }

    private void drawIntermediatePoints(Canvas canvas) {
        Log.d("MetroMapView", "drawIntermediatePoints");
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
            Log.d("MetroMapView", "drawIntermediatePoints: " + point.x + " " + point.y);
            canvas.drawCircle(point.x * COORDINATE_SCALE_FACTOR, point.y * COORDINATE_SCALE_FACTOR, 10, paint);
        }
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

    private void drawTextBasedOnPosition(Canvas canvas, String text, float cx, float cy, int textPosition, Paint paint) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float textWidth = bounds.width();
        float textHeight = bounds.height();

        float offsetX = 0;
        float offsetY = 0;

        switch (textPosition) {
            case 1: // Top
                offsetX = -textWidth / 2;
                offsetY = -textHeight - 15;
                break;
            case 2: // Top Right
                offsetX = 20;
                offsetY = -textHeight - 15;
                break;
            case 3: // Right
                offsetX = 35;
                offsetY = 8;
                break;
            case 4: // Bottom Right
                offsetX = 20;
                offsetY = textHeight + 15;
                break;
            case 5: // Bottom
                offsetY = textHeight + 35;
                offsetX = -textWidth / 2;
                break;
            case 6: // Bottom Left
                offsetX = -textWidth / 2 - 15;
                offsetY = textHeight + 15;
                break;
            case 7: // Left
                offsetX = -textWidth - 35;
                offsetY = 8;
                break;
            case 8: // Top Left
                offsetX = -textWidth - 15;
                offsetY = -textHeight - 15;
                break;
            case 9: // Invisible
                break;
            default: // Center (textPosition == 0)
                offsetX = -textWidth / 2;
                offsetY = textHeight / 2;
                break;
        }

        if (textPosition != 9) {
            // Рисуем белый полупрозрачный фон под текстом
            Paint backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.argb(190, 255, 255, 255)); // Белый цвет с 50% прозрачностью
            backgroundPaint.setStyle(Paint.Style.FILL);

            float paddingX = 10; // Отступы по горизонтали
            float paddingY = 5;  // Отступы по вертикали

            float backgroundLeft = cx + offsetX - paddingX;
            float backgroundTop = cy + offsetY - paddingY - 15;
            float backgroundRight = cx + offsetX + textWidth + paddingX;
            float backgroundBottom = cy + offsetY + textHeight + paddingY - 15;

            canvas.drawRect(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, backgroundPaint);

            // Рисуем текст поверх фона
            canvas.drawText(text, cx + offsetX, cy + offsetY, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = scaleGestureDetector.onTouchEvent(event);
        result = gestureDetector.onTouchEvent(event) || result;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            // Преобразуем координаты касания с учетом трансформации
            float[] point = new float[] {event.getX(), event.getY()};
            Matrix inverseMatrix = new Matrix();
            transformMatrix.invert(inverseMatrix);
            inverseMatrix.mapPoints(point);
            float x = point[0];
            float y = point[1];
            Station clickedStation = findStationAt(x / COORDINATE_SCALE_FACTOR, y / COORDINATE_SCALE_FACTOR);
            if (clickedStation != null && listener != null) {
                Log.d("MetroMapView", "Station clicked: " + clickedStation.getName());
                listener.onStationClick(clickedStation);
                return true;
            } else if (clickedStation == null) {
                Log.d("MetroMapView", "Map clicked");
            } else if (listener == null) {
                Log.d("MetroMapView", "Listener is null");
            }
        }

        return result || super.onTouchEvent(event);
    }

    public Station findStationAt(float x, float y) {
        Log.d("MetroMapView", "Finding station at " + x + ", " + y);
        List<Station> activeStations = stations;
        for (Station station : activeStations) {
            if (Math.abs(station.getX() - x) < CLICK_RADIUS / COORDINATE_SCALE_FACTOR && Math.abs(station.getY() - y) < CLICK_RADIUS / COORDINATE_SCALE_FACTOR) {
                Log.d("MetroMapView", "Found station at " + x + ", " + y + ": " + station.getName());
                return station;
            }
        }
        return null;
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

    private List<PointF> transferConnectionPoints = new ArrayList<>(); // Массив для хранения точек

    private void drawShiftedLine(Canvas canvas, float x1, float y1, float x2, float y2, float centerX, float centerY) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float shiftX = (dy / length) * 20;
        float shiftY = -(dx / length) * 20;

        shiftX = -shiftX;
        shiftY = -shiftY;

        // Сохраняем точки в массив
        transferConnectionPoints.add(new PointF(x1 + shiftX, y1 + shiftY));
        transferConnectionPoints.add(new PointF(x2 + shiftX, y2 + shiftY));

        canvas.drawLine(x1 + shiftX, y1 + shiftY, x2 + shiftX, y2 + shiftY, transferPaint);
    }

    private void drawDashedLine(Canvas canvas, float x1, float y1, float x2, float y2, Paint paint) {
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);

        Paint dashedPaint = new Paint();
        dashedPaint.setColor(paint.getColor());
        dashedPaint.setStyle(Paint.Style.STROKE);
        dashedPaint.setStrokeWidth(paint.getStrokeWidth()-3);
        float density = getResources().getDisplayMetrics().density;
        dashedPaint.setPathEffect(new DashPathEffect(new float[]{density * 2, density * 4}, 0)); // Параметры пунктира

        // Сохраняем точки в массив
        transferConnectionPoints.add(new PointF(x1, y1));
        transferConnectionPoints.add(new PointF(x2, y2));

        canvas.drawPath(path, dashedPaint);
    }

    private void drawHalfColoredLine(Canvas canvas, float x1, float y1, float x2, float y2, String color1, String color2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float shiftX = (dy / length) * 20;
        float shiftY = -(dx / length) * 20;

        shiftX = -shiftX;
        shiftY = -shiftY;
        canvas.drawLine(x1 + shiftX, y1 + shiftY, x2 + shiftX, y2 + shiftY, transferPaint);

        float startX = x1 + shiftX;
        float startY = y1 + shiftY;
        float endX = x2 + shiftX;
        float endY = y2 + shiftY;

        float halfX = (startX + endX) / 2;
        float halfY = (startY + endY) / 2;

        // Сохраняем точки в массив
        transferConnectionPoints.add(new PointF(startX, startY));
        transferConnectionPoints.add(new PointF(halfX, halfY));
        transferConnectionPoints.add(new PointF(endX, endY));

        // Рисуем первую половину линии с цветом color1
        Paint paint1 = new Paint();
        paint1.setColor(Color.parseColor(color2));
        paint1.setStrokeWidth(10);
        canvas.drawLine(startX, startY, halfX, halfY, paint1);

        // Рисуем вторую половину линии с цветом color2
        Paint paint2 = new Paint();
        paint2.setColor(Color.parseColor(color1));
        paint2.setStrokeWidth(10);
        canvas.drawLine(halfX, halfY, endX, endY, paint2);
    }

    private void drawTransferConnection(Canvas canvas, List<Station> stations, String transferType) {
        if (stations == null || stations.size() < 2) {
            return; // Нужно минимум 2 станции для соединения
        }

        // Очищаем массив точек
        transferConnectionPoints.clear();

        // Получаем координаты всех точек и находим центр
        float centerX = 0;
        float centerY = 0;
        float[] coordinates = new float[stations.size() * 2];

        for (int i = 0; i < stations.size(); i++) {
            float x = stations.get(i).getX() * COORDINATE_SCALE_FACTOR;
            float y = stations.get(i).getY() * COORDINATE_SCALE_FACTOR;
            coordinates[i * 2] = x;
            coordinates[i * 2 + 1] = y;
            centerX += x;
            centerY += y;
        }

        centerX /= stations.size();
        centerY /= stations.size();

        // Рисуем линии между всеми точками через центр
        for (int i = 0; i < stations.size(); i++) {
            int nextIndex = (i + 1) % stations.size();
            float x1 = coordinates[i * 2];
            float y1 = coordinates[i * 2 + 1];
            float x2 = coordinates[nextIndex * 2];
            float y2 = coordinates[nextIndex * 2 + 1];

            if (transferType.equals("crossplatform")) {
                // Рисуем половины линии с разными цветами
                drawHalfColoredLine(canvas, x1, y1, x2, y2, stations.get(i).getColor(), stations.get(nextIndex).getColor());
            } else if (transferType.equals("ground")) {
                drawDashedLine(canvas, x1, y1, x2, y2, transferPaint);
            } else {
                drawShiftedLine(canvas, x1, y1, x2, y2, centerX, centerY);
            }
        }

        // Рисуем частичные окружности в каждой точке, если тип не "ground"
        if (!transferType.equals("ground")) {
            for (int i = 0; i < stations.size(); i++) {
                int prevIndex = (i - 1 + stations.size()) % stations.size();
                int nextIndex = (i + 1) % stations.size();

                float currentX = coordinates[i * 2];
                float currentY = coordinates[i * 2 + 1];
                float prevX = coordinates[prevIndex * 2];
                float prevY = coordinates[prevIndex * 2 + 1];
                float nextX = coordinates[nextIndex * 2];
                float nextY = coordinates[nextIndex * 2 + 1];

                if (transferType.equals("crossplatform")) {
                    // Рисуем частичный круг с цветом соседней станции
                    drawPartialCircleWithColor(canvas, currentX, currentY, 20, 6,
                            getAngle(prevX, prevY, currentX, currentY, nextX, nextY), stations.get(nextIndex).getColor());
                } else {
                    drawPartialCircle(canvas, currentX, currentY, 20, 6,
                            getAngle(prevX, prevY, currentX, currentY, nextX, nextY));
                }
            }
        }

        // Заливаем область белым цветом
        fillTransferConnectionArea(canvas);
    }

    private void fillTransferConnectionArea(Canvas canvas) {
        if (transferConnectionPoints.size() < 3) {
            return; // Нужно минимум 3 точки для многоугольника
        }

        // Создаем путь для многоугольника
        Path path = new Path();
        path.moveTo(transferConnectionPoints.get(0).x, transferConnectionPoints.get(0).y);
        for (int i = 1; i < transferConnectionPoints.size(); i++) {
            path.lineTo(transferConnectionPoints.get(i).x, transferConnectionPoints.get(i).y);
        }
        path.close();

        // Заливаем многоугольник белым цветом
        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, fillPaint);
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

    public interface OnStationClickListener {
        void onStationClick(Station station);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Освобождаем память
        for (Bitmap bitmap : cacheBitmaps.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        cacheBitmaps.clear();
    }
}