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

    private void drawMapContents(Canvas canvas) {
        updateVisibleViewport();

        // Save initial canvas state
        int mainSaveCount = canvas.save();

        // Apply main transformation once
        canvas.concat(transformMatrix);

        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }

        // Draw grayed lines/stations with transform
        if (grayedLines != null && !grayedLines.isEmpty()) {
            int saveCount = canvas.save();
            drawGrayedMap(canvas);
            canvas.restoreToCount(saveCount);
        }

        // Draw active map elements with transform
        if (lines != null && stations != null) {
            int saveCount = canvas.save();
            drawRivers(canvas);
            drawLines(canvas);


            drawTransfers(canvas);
            drawStations(canvas);


            drawMapObjects(canvas);

            if (isEditMode) {
                drawIntermediatePoints(canvas);
            }
            canvas.restoreToCount(saveCount);
        }

        // Draw route overlay with transform
        if (route != null && route.size() > 1) {
            int saveCount = canvas.save();
            applyDarkOverlay(canvas, saveCount);
            drawRoute(canvas, saveCount);
            canvas.restoreToCount(saveCount);
        }

        // Restore main canvas state
        canvas.restoreToCount(mainSaveCount);

        updateVisibleViewport();
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

    private void drawLines(Canvas canvas) {
        Set<String> drawnConnections = new HashSet<>();

        for (Line line : lines) {
            Log.d("drawLines", "line: " + line.getName());
            linePaint.setColor(Color.parseColor(line.getColor()));

            for (Station station : line.getStations()) {
                float stationX = station.getX();
                float stationY = station.getY();

                for (Station.Neighbor neighbor : station.getNeighbors()) {
                    Station neighborStation = findStationById(
                            neighbor.getStation().getId(), stations);

                    if (neighborStation != null &&
                            line.getLineIdForStation(neighborStation) != null) {

                        float neighborX = neighborStation.getX();
                        float neighborY = neighborStation.getY();

                        // Only draw if line segment is visible
                        if (isLineVisible(stationX, stationY, neighborX, neighborY)) {
                            String connectionKey = station.getId().compareTo(
                                    neighborStation.getId()) < 0
                                    ? station.getId() + "-" + neighborStation.getId()
                                    : neighborStation.getId() + "-" + station.getId();

                            if (!drawnConnections.contains(connectionKey)) {
                                drawLineWithIntermediatePoints(canvas, station,
                                        neighborStation, line.getLineType(), linePaint);
                                drawnConnections.add(connectionKey);
                            }
                        }
                    }
                }
            }
        }
    }

    private void drawStations(Canvas canvas) {
        for (Station station : stations) {
            float stationX = station.getX();
            float stationY = station.getY();
            Log.d("drawStations", "station: " + station.getName());
            Log.d("drawStations", "stationX: " + stationX + ", stationY: " + stationY);

            // Отрисовываем только видимые станции
            if (isPointVisible(stationX, stationY)) {
                float screenX = stationX * COORDINATE_SCALE_FACTOR;
                float screenY = stationY * COORDINATE_SCALE_FACTOR;
                Log.d("drawStations", "COORDINATE_SCALE_FACTOR: " + COORDINATE_SCALE_FACTOR);
                Log.d("drawStations", "screenX: " + screenX + ", screenY: " + screenY);

                canvas.drawCircle(screenX, screenY, 10, whitePaint);
                stationPaint.setColor(Color.parseColor(station.getColor()));
                canvas.drawCircle(screenX, screenY, 14, stationPaint);

                if (selectedStations != null && selectedStations.contains(station)) {
                    canvas.drawCircle(screenX, screenY, 20, selectedStationPaint);
                }

                drawTextBasedOnPosition(canvas, station.getName(),
                        screenX, screenY, station.getTextPosition(),
                        textPaint, false);
            }
        }
    }

    private void drawTransfers(Canvas canvas) {

        if (transfers == null || transfers.isEmpty()) {
            return;
        }
        for (Transfer transfer : transfers) {
            drawTransferConnection(canvas, transfer.getStations(), transfer.getType());
        }
    }

    private void drawRivers(Canvas canvas) {
        if (rivers != null) {
            for (River river : rivers) {
                drawRiver(canvas, river);
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

    private void drawRoute(Canvas canvas, int routeSaveCount) {
        if (route != null && route.size() > 1) {
            for (int i = 0; i < route.size() - 1; i++) {
                Station station1 = route.get(i);
                Station station2 = route.get(i + 1);
                routePaint.setColor(Color.parseColor(station1.getColor()));
                routePaint.setStrokeWidth(15);
                Line line = findLineForConnection(station1, station2);
                if (line != null) {
                    drawRouteWithIntermediatePoints(canvas, station1, station2, line.getLineType());
                } else {
                    drawTransferBetweenStations(canvas, station1, station2);
                }
                float stationX = station1.getX() * COORDINATE_SCALE_FACTOR;
                float stationY = station1.getY() * COORDINATE_SCALE_FACTOR;
                canvas.drawCircle(stationX, stationY, 10, whitePaint);
                stationPaint.setColor(Color.parseColor(station1.getColor()));
                canvas.drawCircle(stationX, stationY, 14, stationPaint);
                if (selectedStations != null && selectedStations.contains(station1)) {
                    canvas.drawCircle(stationX, stationY, 20, selectedStationPaint);
                }
                drawTextBasedOnPosition(canvas, station1.getName(), stationX, stationY, station1.getTextPosition(), textPaint, true);
            }
            Station lastStation = route.get(route.size() - 1);
            float lastStationX = lastStation.getX() * COORDINATE_SCALE_FACTOR;
            float lastStationY = lastStation.getY() * COORDINATE_SCALE_FACTOR;
            canvas.drawCircle(lastStationX, lastStationY, 10, whitePaint);
            stationPaint.setColor(Color.parseColor(lastStation.getColor()));
            canvas.drawCircle(lastStationX, lastStationY, 14, stationPaint);
            if (selectedStations != null && selectedStations.contains(lastStation)) {
                canvas.drawCircle(lastStationX, lastStationY, 20, selectedStationPaint);
            }
            drawTextBasedOnPosition(canvas, lastStation.getName(), lastStationX, lastStationY, lastStation.getTextPosition(), textPaint, true);
        }
    }

    private void drawTransferBetweenStations(Canvas canvas, Station station1, Station station2) {
        Transfer transfer = findTransferBetweenStations(station1, station2);
        if (transfer != null) {
            drawTransferConnection(canvas, Arrays.asList(station1, station2), transfer.getType());
        } else {
            drawDashedLine(canvas,
                    station1.getX() * COORDINATE_SCALE_FACTOR,
                    station1.getY() * COORDINATE_SCALE_FACTOR,
                    station2.getX() * COORDINATE_SCALE_FACTOR,
                    station2.getY() * COORDINATE_SCALE_FACTOR,
                    transferPaint);
        }
    }

    private void drawRouteWithIntermediatePoints(Canvas canvas, Station station1, Station station2, String lineType) {
        Station startStation = station1.getId().compareTo(station2.getId()) < 0 ? station1 : station2;
        Station endStation = station1.getId().compareTo(station2.getId()) < 0 ? station2 : station1;
        List<Point> intermediatePoints = startStation.getIntermediatePoints(endStation);
        if (intermediatePoints == null || intermediatePoints.isEmpty()) {
            if (lineType.equals("double")) {
                drawDoubleLine(canvas, startStation, endStation, routePaint);
            } else {
                canvas.drawLine(
                        startStation.getX() * COORDINATE_SCALE_FACTOR,
                        startStation.getY() * COORDINATE_SCALE_FACTOR,
                        endStation.getX() * COORDINATE_SCALE_FACTOR,
                        endStation.getY() * COORDINATE_SCALE_FACTOR,
                        routePaint
                );
            }
        } else if (intermediatePoints.size() == 1) {
            float startX = startStation.getX() * COORDINATE_SCALE_FACTOR;
            float startY = startStation.getY() * COORDINATE_SCALE_FACTOR;
            float endX = intermediatePoints.get(0).x * COORDINATE_SCALE_FACTOR;
            float endY = intermediatePoints.get(0).y * COORDINATE_SCALE_FACTOR;
            canvas.drawLine(startX, startY, endX, endY, routePaint);
            canvas.drawLine(endX, endY, endStation.getX() * COORDINATE_SCALE_FACTOR, endStation.getY() * COORDINATE_SCALE_FACTOR, routePaint);
        } else if (intermediatePoints.size() == 2) {
            Point start = new Point(startStation.getX(), startStation.getY());
            Point control1 = intermediatePoints.get(0);
            Point control2 = intermediatePoints.get(1);
            Point end = new Point(endStation.getX(), endStation.getY());
            if (lineType.equals("double")) {
                drawDoubleBezierCurve(canvas, start, control1, control2, end, routePaint);
            } else {
                drawBezierCurve(canvas, start, control1, control2, end, routePaint);
            }
        }
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
//        if ((!isActiveMapMetro && isInGrayedLines(lines) && isInGrayedLines(grayedLines))) {
//            Paint grayedStationPaint = new Paint(grayedPaint);
//            grayedStationPaint.setStyle(Paint.Style.STROKE);
//            grayedStationPaint.setStrokeWidth(7);
//            Paint grayedTextPaint = new Paint(grayedPaint);
//            grayedTextPaint.setTextSize(20);
//
//            for (Station station : grayedStations) {
//                float stationX = station.getX() * COORDINATE_SCALE_FACTOR;
//                float stationY = station.getY() * COORDINATE_SCALE_FACTOR;
//                canvas.drawCircle(stationX, stationY, 10, whitePaint);
//                canvas.drawCircle(stationX, stationY, 14, grayedStationPaint);
//                drawTextBasedOnPosition(canvas, station.getName(),
//                        stationX, stationY, station.getTextPosition(),
//                        grayedTextPaint, false);
//            }
//        }
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

    private void drawTextBasedOnPosition(Canvas canvas, String text, float cx, float cy, int textPosition, Paint paint, boolean isRouteText) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float textWidth = bounds.width();
        float textHeight = bounds.height();
        float offsetX = 0;
        float offsetY = 0;
        switch (textPosition) {
            case 1:
                offsetX = -textWidth / 2;
                offsetY = -textHeight - 15;
                break;
            case 2:
                offsetX = 20;
                offsetY = -textHeight - 15;
                break;
            case 3:
                offsetX = 35;
                offsetY = 8;
                break;
            case 4:
                offsetX = 20;
                offsetY = textHeight + 15;
                break;
            case 5:
                offsetY = textHeight + 35;
                offsetX = -textWidth / 2;
                break;
            case 6:
                offsetX = -textWidth / 2 - 15;
                offsetY = textHeight + 15;
                break;
            case 7:
                offsetX = -textWidth - 35;
                offsetY = 8;
                break;
            case 8:
                offsetX = -textWidth - 15;
                offsetY = -textHeight - 15;
                break;
            case 9:
                break;
            default:
                offsetX = -textWidth / 2;
                offsetY = textHeight / 2;
                break;
        }
        if (textPosition != 9) {
            Paint backgroundPaint = new Paint();
            if (isRouteText) {
                backgroundPaint.setColor(Color.parseColor("#696969"));
            } else {
                backgroundPaint.setColor(Color.argb(190, 255, 255, 255));
            }
            backgroundPaint.setStyle(Paint.Style.FILL);
            float paddingX = 10;
            float paddingY = 5;
            float backgroundLeft = cx + offsetX - paddingX;
            float backgroundTop = cy + offsetY - paddingY - 15;
            float backgroundRight = cx + offsetX + textWidth + paddingX;
            float backgroundBottom = cy + offsetY + textHeight + paddingY - 15;
            canvas.drawRect(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, backgroundPaint);
            if (isRouteText) {
                paint.setColor(Color.WHITE);
            } else {
                paint.setColor(Color.BLACK);
            }
            canvas.drawText(text, cx + offsetX, cy + offsetY, paint);
        }
    }

    private void drawTransferConnection(Canvas canvas, List<Station> stations, String transferType) {
        if (stations == null || stations.size() < 2) {
            return;
        }
        transferConnectionPoints = new ArrayList<>();
        float centerX = 0;
        float centerY = 0;
        float[] coordinates = new float[stations.size() * 2];
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            float x = station.getX() * COORDINATE_SCALE_FACTOR;
            float y = station.getY() * COORDINATE_SCALE_FACTOR;
            coordinates[i * 2] = x;
            coordinates[i * 2 + 1] = y;
            centerX += x;
            centerY += y;
        }
        centerX /= stations.size();
        centerY /= stations.size();
        Paint currentPaint = new Paint(transferPaint);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeWidth(10);
        currentPaint.setColor(Color.BLACK);
        for (int i = 0; i < stations.size(); i++) {
            int nextIndex = (i + 1) % stations.size();
            float x1 = coordinates[i * 2];
            float y1 = coordinates[i * 2 + 1];
            float x2 = coordinates[nextIndex * 2];
            float y2 = coordinates[nextIndex * 2 + 1];
            switch (transferType.toLowerCase()) {
                case "crossplatform":
                    drawHalfColoredLine(canvas, x1, y1, x2, y2,
                            stations.get(i).getColor(),
                            stations.get(nextIndex).getColor());
                    break;
                case "ground":
                    drawDashedLine(canvas, x1, y1, x2, y2, currentPaint);
                    break;
                default:
                    drawShiftedLine(canvas, x1, y1, x2, y2, centerX, centerY);
                    break;
            }
        }
        if (!transferConnectionPoints.isEmpty()) {
            fillTransferConnectionArea(canvas);
        }
        if (!transferType.equalsIgnoreCase("ground")) {
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
                if (transferType.equalsIgnoreCase("crossplatform")) {
                    drawPartialCircleWithColor(canvas, currentX, currentY, 20, 6,
                            angles, stations.get(nextIndex).getColor());
                } else {
                    drawPartialCircle(canvas, currentX, currentY, 20, 6, angles);
                }
            }
        }
    }

    private void drawShiftedLine(Canvas canvas, float x1, float y1, float x2, float y2, float centerX, float centerY) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float shiftX = (dy / length) * 20;
        float shiftY = -(dx / length) * 20;
        shiftX = -shiftX;
        shiftY = -shiftY;
        transferConnectionPoints.add(new PointF(x1 + shiftX, y1 + shiftY));
        transferConnectionPoints.add(new PointF(x2 + shiftX, y2 + shiftY));
        transferPaint.setColor(Color.BLACK);
        transferPaint.setStrokeWidth(10);
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
        dashedPaint.setPathEffect(new DashPathEffect(new float[]{density * 2, density * 4}, 0));
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
        transferConnectionPoints.add(new PointF(startX, startY));
        transferConnectionPoints.add(new PointF(halfX, halfY));
        transferConnectionPoints.add(new PointF(endX, endY));
        Paint paint1 = new Paint();
        paint1.setColor(Color.parseColor(color2));
        paint1.setStrokeWidth(10);
        canvas.drawLine(startX, startY, halfX, halfY, paint1);
        Paint paint2 = new Paint();
        paint2.setColor(Color.parseColor(color1));
        paint2.setStrokeWidth(10);
        canvas.drawLine(halfX, halfY, endX, endY, paint2);
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

    private void fillTransferConnectionArea(Canvas canvas) {
        if (transferConnectionPoints.size() < 3) {
            return;
        }
        Path path = new Path();
        path.moveTo(transferConnectionPoints.get(0).x, transferConnectionPoints.get(0).y);
        for (int i = 1; i < transferConnectionPoints.size(); i++) {
            path.lineTo(transferConnectionPoints.get(i).x, transferConnectionPoints.get(i).y);
        }
        path.close();
        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, fillPaint);
    }

    private void applyDarkOverlay(Canvas canvas, int overlaySaveCount) {
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(150, 0, 0, 0));
        overlayPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), overlayPaint);
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

    private boolean isPointVisible(float x, float y) {
        return visibleViewport.contains(x, y);
    }

    private boolean isLineVisible(float x1, float y1, float x2, float y2) {
        // Check if either endpoint is visible
        if (isPointVisible(x1, y1) || isPointVisible(x2, y2)) {
            return true;
        }

        // Check if line intersects viewport
        return lineIntersectsRect(x1, y1, x2, y2,
                visibleViewport.left, visibleViewport.top,
                visibleViewport.right, visibleViewport.bottom);
    }

    private boolean lineIntersectsRect(float x1, float y1, float x2, float y2,
                                       float rectLeft, float rectTop,
                                       float rectRight, float rectBottom) {
        // Cohen-Sutherland algorithm for line-rectangle intersection
        int code1 = computeOutCode(x1, y1, rectLeft, rectTop, rectRight, rectBottom);
        int code2 = computeOutCode(x2, y2, rectLeft, rectTop, rectRight, rectBottom);

        while (true) {
            if ((code1 | code2) == 0) return true;  // Line is inside
            if ((code1 & code2) != 0) return false; // Line is outside

            return true; // Line intersects
        }
    }

    private int computeOutCode(float x, float y, float rectLeft, float rectTop,
                               float rectRight, float rectBottom) {
        int code = 0;
        if (x < rectLeft) code |= 1;
        if (x > rectRight) code |= 2;
        if (y < rectTop) code |= 4;
        if (y > rectBottom) code |= 8;
        return code;
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

    private Transfer findTransferBetweenStations(Station station1, Station station2) {
        if (transfers == null) {
            return null;
        }
        for (Transfer transfer : transfers) {
            if (transfer.getStations().contains(station1) && transfer.getStations().contains(station2)) {
                return transfer;
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

    private float calculateRiverLength(List<Point> points) {
        float riverLength = 0;
        for (int i = 1; i < points.size(); i++) {
            Point p1 = points.get(i - 1);
            Point p2 = points.get(i);
            riverLength += Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
        }
        return riverLength;
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