package com.nicorp.nimetro.presentation.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
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
import java.util.List;
import java.util.Map;

/**
 * Custom view for displaying a metro map. Handles drawing of lines, stations,
 * transfers, rivers, and map objects. Also supports touch events for zooming,
 * panning, and station selection.
 */
public class MetroMapView extends View {

    private List<Line> lines;
    private List<Station> stations;
    private List<Station> route;
    private List<Station> selectedStations;
    private List<Transfer> transfers;
    private List<River> rivers;
    private List<MapObject> mapObjects;

    private Paint linePaint;
    private Paint stationPaint;
    private Paint selectedStationPaint;
    private Paint routePaint;
    private Paint textPaint;
    private Paint whitePaint;
    private Paint transferPaint;
    private Paint stationCenterPaint;
    private Paint riverPaint;

    private boolean isEditMode = false;

    private OnStationClickListener listener;

    public float scaleFactor = 1.0f;
    private float translateX = 0.0f;
    private float translateY = 0.0f;

    private GestureDetector gestureDetector;
    public ScaleGestureDetector scaleGestureDetector;

    public static final float COORDINATE_SCALE_FACTOR = 2.5f;
    private static final float CLICK_RADIUS = 30.0f;
    private static final float TRANSFER_CAPSULE_WIDTH = 40.0f;

    private Bitmap backgroundBitmap;

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

    public float getScaleFactor() {
        return scaleFactor;
    }

    public float getTranslateX() {
        return translateX;
    }

    public float getTranslateY() {
        return translateY;
    }

    /**
     * Sets the edit mode of the view.
     * @param editMode
     */
    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        invalidate();
    }

    /**
     * Initializes the view with default settings and gesture detectors.
     */
    private void init() {
        initializePaints();
        initializeGestureDetectors();
        if (isEditMode) {
            loadBackgroundBitmap();
        }
    }

    /**
     * Initializes the paint objects for drawing various elements.
     */
    private void initializePaints() {
        linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(9);

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

        int colorOnSurface = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK);

        textPaint = new Paint();
        textPaint.setColor(colorOnSurface);
        textPaint.setTextSize(20);

        transferPaint = new Paint();
        transferPaint.setColor(Color.DKGRAY);
        transferPaint.setStrokeWidth(5);
        transferPaint.setStyle(Paint.Style.STROKE);

        stationCenterPaint = new Paint();
        stationCenterPaint.setColor(Color.parseColor("#00000000"));
        stationCenterPaint.setStyle(Paint.Style.STROKE);
        stationCenterPaint.setStrokeWidth(7);

        riverPaint = new Paint();
        riverPaint.setColor(Color.parseColor("#CCE0EA"));
        riverPaint.setStyle(Paint.Style.STROKE);
        riverPaint.setStrokeWidth(10);
    }

    /**
     * Initializes the gesture detectors for handling touch events.
     */
    private void initializeGestureDetectors() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                translateX -= distanceX / scaleFactor;
                translateY -= distanceY / scaleFactor;
                invalidate();
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 2.0f));
                invalidate();
                return true;
            }
        });
    }

    /**
     * Loads the background bitmap from the resources.
     */
    private void loadBackgroundBitmap() {
        Log.d("MetroMapView", "Loading background bitmap...");
        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.metro_map);
    }

    /**
     * Clears the current route.
     */
    public void clearRoute() {
        this.route = null;
        invalidate();
    }

    /**
     * Clears the current selected stations.
     */
    public void clearSelectedStations() {
        this.selectedStations = null;
        invalidate();
    }

    /**
     * Sets the data for the metro map.
     *
     * @param lines The list of lines.
     * @param stations The list of stations.
     * @param transfers The list of transfers.
     * @param rivers The list of rivers.
     * @param mapObjects The list of map objects.
     */
    public void setData(List<Line> lines, List<Station> stations, List<Transfer> transfers, List<River> rivers, List<MapObject> mapObjects) {
        this.lines = lines;
        this.stations = stations;
        this.transfers = transfers;
        this.rivers = rivers;
        this.mapObjects = mapObjects;
        invalidate();
    }

    /**
     * Sets the current route.
     *
     * @param route The list of stations representing the route.
     */
    public void setRoute(List<Station> route) {
        this.route = route;
        invalidate();
    }

    /**
     * Sets the current selected stations.
     *
     * @param selectedStations The list of selected stations.
     */
    public void setSelectedStations(List<Station> selectedStations) {
        this.selectedStations = selectedStations;
        invalidate();
    }

    /**
     * Sets the listener for station click events.
     *
     * @param listener The listener to set.
     */
    public void setOnStationClickListener(OnStationClickListener listener) {
        this.listener = listener;
    }

    /**
     * Draws the metro map on the canvas.
     *
     * @param canvas The canvas to draw on.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        // Draw the background bitmap
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }

        if (lines != null && stations != null) {
            drawRivers(canvas);
            drawLines(canvas);
            drawTransfers(canvas);
            drawStations(canvas);
            drawRoute(canvas);
            drawMapObjects(canvas);

            // Рисуем промежуточные точки только в режиме редактирования
            if (isEditMode) {
                drawIntermediatePoints(canvas);
            }
        }

        canvas.restore();
    }

    /**
     * Draws the rivers on the canvas.
     *
     * @param canvas The canvas to draw on.
     */
    private void drawRivers(Canvas canvas) {
        if (rivers != null) {
            for (River river : rivers) {
                drawRiver(canvas, river);
            }
        }
    }

    /**
     * Draws the lines on the canvas.
     *
     * @param canvas The canvas to draw on.
     */
    private void drawLines(Canvas canvas) {
        for (Line line : lines) {
            linePaint.setColor(Color.parseColor(line.getColor()));
            for (int i = 0; i < line.getStations().size() - 1; i++) {
                Station station1 = line.getStations().get(i);
                Station station2 = line.getStations().get(i + 1);
                drawLineWithIntermediatePoints(canvas, station1, station2, line.getLineType());
            }

            if (line.isCircle() && line.getStations().size() > 1) {
                Station firstStation = line.getStations().get(0);
                Station lastStation = line.getStations().get(line.getStations().size() - 1);
                drawLineWithIntermediatePoints(canvas, firstStation, lastStation, line.getLineType());
            }
        }
    }

    /**
     * Draws the transfers on the canvas.
     *
     * @param canvas The canvas to draw on.
     */
    private void drawTransfers(Canvas canvas) {
        for (Transfer transfer : transfers) {
            List<Station> transferStations = transfer.getStations();
            if (transferStations.size() == 2) {
                drawTransferConnection(canvas, transferStations.get(0), transferStations.get(1));
            } else if (transferStations.size() == 3) {
                drawTransferConnectionTriangle(canvas, transferStations.get(0), transferStations.get(1), transferStations.get(2));
            } else if (transferStations.size() == 4) {
                drawTransferConnectionQuad(canvas, transferStations.get(0), transferStations.get(1), transferStations.get(2), transferStations.get(3));
            }
        }
    }

    /**
     * Draws the stations on the canvas.
     *
     * @param canvas The canvas to draw on.
     */
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

    /**
     * Draws the route on the canvas.
     *
     * @param canvas The canvas to draw on.
     */
    private void drawRoute(Canvas canvas) {
        if (route != null && route.size() > 1) {
            for (int i = 0; i < route.size() - 1; i++) {
                Station station1 = route.get(i);
                Station station2 = route.get(i + 1);
                drawRouteWithIntermediatePoints(canvas, station1, station2);
            }
        }
    }

    /**
     * Draws the map objects on the canvas.
     *
     * @param canvas The canvas to draw on.
     */
    private void drawMapObjects(Canvas canvas) {
        if (mapObjects != null) {
            for (MapObject mapObject : mapObjects) {
                drawMapObject(canvas, mapObject);
            }
        }
    }

    /**
     * Draws a map object on the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param mapObject The map object to draw.
     */
    private void drawMapObject(Canvas canvas, MapObject mapObject) {
        float objectX = mapObject.getPosition().x * COORDINATE_SCALE_FACTOR;
        float objectY = mapObject.getPosition().y * COORDINATE_SCALE_FACTOR;

        Paint objectPaint = new Paint();
        objectPaint.setColor(Color.BLACK);
        objectPaint.setStyle(Paint.Style.FILL);
        objectPaint.setTextSize(24);

        if (mapObject.getType().equals("airport")) {
            canvas.drawText("✈", objectX - 12, objectY + 12, objectPaint);
        } else if (mapObject.getType().equals("train_station")) {
            canvas.drawText("🚂", objectX - 12, objectY + 12, objectPaint);
        }

        canvas.drawText(mapObject.getDisplayName(), objectX + 20, objectY, objectPaint);
    }

    /**
     * Draws a river on the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param river The river to draw.
     */
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
        float fadeMargin = 20 / riverLength;
        float[] fadePositions = {0.0f, fadeMargin, 1.0f - fadeMargin, 1.0f};

        LinearGradient fadeGradient = new LinearGradient(
                startPoint.x * COORDINATE_SCALE_FACTOR, startPoint.y * COORDINATE_SCALE_FACTOR,
                endPoint.x * COORDINATE_SCALE_FACTOR, endPoint.y * COORDINATE_SCALE_FACTOR,
                fadeColors, fadePositions, Shader.TileMode.CLAMP);

        riverPaint.setShader(fadeGradient);
        riverPaint.setStrokeWidth(width);

        canvas.drawPath(riverPath, riverPaint);
    }

    /**
     * Calculates the length of a river.
     *
     * @param points The list of points representing the river.
     * @return The length of the river.
     */
    private float calculateRiverLength(List<Point> points) {
        float riverLength = 0;
        for (int i = 1; i < points.size(); i++) {
            Point p1 = points.get(i - 1);
            Point p2 = points.get(i);
            riverLength += Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
        }
        return riverLength;
    }

    /**
     * Draws a line with intermediate points on the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param station1 The first station.
     * @param station2 The second station.
     * @param lineType The type of the line.
     */
    private void drawLineWithIntermediatePoints(Canvas canvas, Station station1, Station station2, String lineType) {
        List<Point> intermediatePoints = station1.getIntermediatePoints(station2);
        if (intermediatePoints == null || intermediatePoints.isEmpty()) {
            if (lineType.equals("double")) {
                drawDoubleLine(canvas, station1, station2);
            } else {
                canvas.drawLine(station1.getX() * COORDINATE_SCALE_FACTOR, station1.getY() * COORDINATE_SCALE_FACTOR,
                        station2.getX() * COORDINATE_SCALE_FACTOR, station2.getY() * COORDINATE_SCALE_FACTOR, linePaint);
            }
        } else if (intermediatePoints.size() == 1) {
            float startX = station1.getX() * COORDINATE_SCALE_FACTOR;
            float startY = station1.getY() * COORDINATE_SCALE_FACTOR;
            float endX = intermediatePoints.get(0).x * COORDINATE_SCALE_FACTOR;
            float endY = intermediatePoints.get(0).y * COORDINATE_SCALE_FACTOR;
            canvas.drawLine(startX, startY, endX, endY, linePaint);
            canvas.drawLine(endX, endY, station2.getX() * COORDINATE_SCALE_FACTOR, station2.getY() * COORDINATE_SCALE_FACTOR, linePaint);
        } else if (intermediatePoints.size() == 2) {
            Point start = new Point(station1.getX(), station1.getY());
            Point control1 = intermediatePoints.get(0);
            Point control2 = intermediatePoints.get(1);
            Point end = new Point(station2.getX(), station2.getY());
            drawBezierCurve(canvas, start, control1, control2, end, linePaint);
        }
    }

    /**
     * Draws a route with intermediate points on the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param station1 The first station.
     * @param station2 The second station.
     */
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

    /**
     * Draws a double line on the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param station1 The first station.
     * @param station2 The second station.
     */
    private void drawDoubleLine(Canvas canvas, Station station1, Station station2) {
        float x1 = station1.getX() * COORDINATE_SCALE_FACTOR;
        float y1 = station1.getY() * COORDINATE_SCALE_FACTOR;
        float x2 = station2.getX() * COORDINATE_SCALE_FACTOR;
        float y2 = station2.getY() * COORDINATE_SCALE_FACTOR;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        float nx = dx / length;
        float ny = dy / length;
        float perpX = -ny * 5; // Смещение для второй линии
        float perpY = nx * 5;

        Paint whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStrokeWidth(6);

        canvas.drawLine(x1 + perpX, y1 + perpY, x2 + perpX, y2 + perpY, linePaint);
        canvas.drawLine(x1 - perpX, y1 - perpY, x2 - perpX, y2 - perpY, linePaint);

        canvas.drawLine(x1 , y1 , x2 , y2, whitePaint);
    }

    /**
     * Interpolates points to create a smooth curve.
     *
     * @param points The list of points to interpolate.
     * @return The list of interpolated points.
     */
    private List<Point> interpolatePoints(List<Point> points) {
        int n = points.size();
        double[] x = new double[n];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = i;
            y[i] = points.get(i).y;
        }

        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction splineX = interpolator.interpolate(x, x);
        PolynomialSplineFunction splineY = interpolator.interpolate(x, y);

        List<Point> interpolatedPoints = new ArrayList<>();
        for (double t = 0; t <= n - 1; t += 0.1) {
            double interpolatedX = splineX.value(t);
            double interpolatedY = splineY.value(t);
            interpolatedPoints.add(new Point((int) interpolatedX, (int) interpolatedY));
        }

        return interpolatedPoints;
    }

    /**
     * Draws intermediate points on the canvas.
     *
     * @param canvas The canvas to draw on.
     */
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

    /**
     * Draws intermediate points on the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param intermediatePoints The list of intermediate points.
     * @param paint The paint to use for drawing.
     */
    private void drawIntermediatePoints(Canvas canvas, List<Point> intermediatePoints, Paint paint) {
        for (Point point : intermediatePoints) {
            canvas.drawCircle(point.x * COORDINATE_SCALE_FACTOR, point.y * COORDINATE_SCALE_FACTOR, 10, paint);
        }
    }

    /**
     * Draws a Bezier curve on the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param start The start point.
     * @param control1 The first control point.
     * @param control2 The second control point.
     * @param end The end point.
     * @param paint The paint to use for drawing.
     */
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

    /**
     * Draws text based on the specified position.
     *
     * @param canvas The canvas to draw on.
     * @param text The text to draw.
     * @param cx The x-coordinate of the center.
     * @param cy The y-coordinate of the center.
     * @param textPosition The position of the text.
     * @param paint The paint to use for drawing.
     */
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
                offsetY = -textHeight - 10;
                break;
            case 2: // Top Right
                offsetX = 20;
                offsetY = -textHeight - 10;
                break;
            case 3: // Right
                offsetX = 30;
                offsetY = 8;
                break;
            case 4: // Bottom Right
                offsetX = 20;
                offsetY = textHeight + 10;
                break;
            case 5: // Bottom
                offsetY = textHeight + 30;
                offsetX = -textWidth / 2;
                break;
            case 6: // Bottom Left
                offsetX = -textWidth / 2 - 10;
                offsetY = textHeight + 10;
                break;
            case 7: // Left
                offsetX = -textWidth - 30;
                break;
            case 8: // Top Left
                offsetX = -textWidth - 10;
                offsetY = -textHeight - 10;
                break;
            case 9: // Invisible
                break;
            default: // Center (textPosition == 0)
                offsetX = -textWidth / 2;
                offsetY = textHeight / 2;
                break;
        }

        if (textPosition != 9) {
            canvas.drawText(text, cx + offsetX, cy + offsetY, paint);
        }
    }

    /**
     * Handles touch events for zooming, panning, and station selection.
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = scaleGestureDetector.onTouchEvent(event);
        result = gestureDetector.onTouchEvent(event) || result;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = (event.getX() - translateX) / scaleFactor;
            float y = (event.getY() - translateY) / scaleFactor;
            Station clickedStation = findStationAt(x / COORDINATE_SCALE_FACTOR, y / COORDINATE_SCALE_FACTOR);
            if (clickedStation != null && listener != null) {
                listener.onStationClick(clickedStation);
                return true;
            }
        }

        return result || super.onTouchEvent(event);
    }

    /**
     * Finds a station at the specified coordinates.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The station at the specified coordinates, or null if not found.
     */
    public Station findStationAt(float x, float y) {
        for (Station station : stations) {
            if (Math.abs(station.getX() - x) < CLICK_RADIUS / COORDINATE_SCALE_FACTOR && Math.abs(station.getY() - y) < CLICK_RADIUS / COORDINATE_SCALE_FACTOR) {
                Log.d("MetroMapView", "Found station at " + x + ", " + y + ": " + station.getName());
                return station;
            }
        }
        return null;
    }

    /**
     * Draws a transfer connection between two stations.
     *
     * @param canvas The canvas to draw on.
     * @param station1 The first station.
     * @param station2 The second station.
     */
    private void drawTransferConnection(Canvas canvas, Station station1, Station station2) {
        float x1 = station1.getX() * COORDINATE_SCALE_FACTOR;
        float y1 = station1.getY() * COORDINATE_SCALE_FACTOR;
        float x2 = station2.getX() * COORDINATE_SCALE_FACTOR;
        float y2 = station2.getY() * COORDINATE_SCALE_FACTOR;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        float nx = dx / length;
        float ny = dy / length;
        float perpX = -ny * (TRANSFER_CAPSULE_WIDTH / 2);
        float perpY = nx * (TRANSFER_CAPSULE_WIDTH / 2);

        Path capsulePath = new Path();
        capsulePath.moveTo(x1 + perpX, y1 + perpY);
        capsulePath.lineTo(x2 + perpX, y2 + perpY);

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        RectF endCircle = new RectF(
                x2 - TRANSFER_CAPSULE_WIDTH / 2,
                y2 - TRANSFER_CAPSULE_WIDTH / 2,
                x2 + TRANSFER_CAPSULE_WIDTH / 2,
                y2 + TRANSFER_CAPSULE_WIDTH / 2
        );
        capsulePath.arcTo(endCircle, angle - 90, 180, false);

        capsulePath.moveTo(x2 - perpX, y2 - perpY);
        capsulePath.lineTo(x1 - perpX, y1 - perpY);

        RectF startCircle = new RectF(
                x1 - TRANSFER_CAPSULE_WIDTH / 2,
                y1 - TRANSFER_CAPSULE_WIDTH / 2,
                x1 + TRANSFER_CAPSULE_WIDTH / 2,
                y1 + TRANSFER_CAPSULE_WIDTH / 2
        );
        capsulePath.arcTo(startCircle, angle + 90, 180, false);

        capsulePath.close();

        Paint capsuleFillPaint = new Paint(transferPaint);
        capsuleFillPaint.setStyle(Paint.Style.FILL);
        capsuleFillPaint.setColor(Color.WHITE);

        canvas.drawPath(capsulePath, capsuleFillPaint);
        canvas.drawPath(capsulePath, transferPaint);
    }

    /**
     * Draws a shifted line on the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param x1 The x-coordinate of the first point.
     * @param y1 The y-coordinate of the first point.
     * @param x2 The x-coordinate of the second point.
     * @param y2 The y-coordinate of the second point.
     * @param centerX The x-coordinate of the center.
     * @param centerY The y-coordinate of the center.
     */
    private void drawShiftedLine(Canvas canvas, float x1, float y1, float x2, float y2, float centerX, float centerY) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float shiftX = (dy / length) * 20;
        float shiftY = -(dx / length) * 20;

        shiftX = -shiftX;
        shiftY = -shiftY;

        canvas.drawLine(x1 + shiftX, y1 + shiftY, x2 + shiftX, y2 + shiftY, transferPaint);
    }

    /**
     * Draws a partial circle on the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param centerX The x-coordinate of the center.
     * @param centerY The y-coordinate of the center.
     * @param radius The radius of the circle.
     * @param strokeWidth The stroke width.
     * @param angles The list of angles.
     */
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

    /**
     * Draws a transfer connection between three stations.
     *
     * @param canvas The canvas to draw on.
     * @param station1 The first station.
     * @param station2 The second station.
     * @param station3 The third station.
     */
    private void drawTransferConnectionTriangle(Canvas canvas, Station station1, Station station2, Station station3) {
        float x1 = station1.getX() * COORDINATE_SCALE_FACTOR;
        float y1 = station1.getY() * COORDINATE_SCALE_FACTOR;
        float x2 = station2.getX() * COORDINATE_SCALE_FACTOR;
        float y2 = station2.getY() * COORDINATE_SCALE_FACTOR;
        float x3 = station3.getX() * COORDINATE_SCALE_FACTOR;
        float y3 = station3.getY() * COORDINATE_SCALE_FACTOR;

        float centerX = (x1 + x2 + x3) / 3;
        float centerY = (y1 + y2 + y3) / 3;

        drawShiftedLine(canvas, x1, y1, x2, y2, centerX, centerY);
        drawShiftedLine(canvas, x2, y2, x3, y3, centerX, centerY);
        drawShiftedLine(canvas, x3, y3, x1, y1, centerX, centerY);

        drawPartialCircle(canvas, x1, y1, 20, 5, getAngle(x3, y3, x1, y1, x2, y2));
        drawPartialCircle(canvas, x2, y2, 20, 5, getAngle(x1, y1, x2, y2, x3, y3));
        drawPartialCircle(canvas, x3, y3, 20, 5, getAngle(x2, y2, x3, y3, x1, y1));
    }

    /**
     * Draws a transfer connection between four stations.
     *
     * @param canvas The canvas to draw on.
     * @param station1 The first station.
     * @param station2 The second station.
     * @param station3 The third station.
     * @param station4 The fourth station.
     */
    private void drawTransferConnectionQuad(Canvas canvas, Station station1, Station station2, Station station3, Station station4) {
        float x1 = station1.getX() * COORDINATE_SCALE_FACTOR;
        float y1 = station1.getY() * COORDINATE_SCALE_FACTOR;
        float x2 = station2.getX() * COORDINATE_SCALE_FACTOR;
        float y2 = station2.getY() * COORDINATE_SCALE_FACTOR;
        float x3 = station3.getX() * COORDINATE_SCALE_FACTOR;
        float y3 = station3.getY() * COORDINATE_SCALE_FACTOR;
        float x4 = station4.getX() * COORDINATE_SCALE_FACTOR;
        float y4 = station4.getY() * COORDINATE_SCALE_FACTOR;

        float centerX = (x1 + x2 + x3 + x4) / 4;
        float centerY = (y1 + y2 + y3 + y4) / 4;

        drawShiftedLine(canvas, x1, y1, x2, y2, centerX, centerY);
        drawShiftedLine(canvas, x2, y2, x3, y3, centerX, centerY);
        drawShiftedLine(canvas, x3, y3, x4, y4, centerX, centerY);
        drawShiftedLine(canvas, x4, y4, x1, y1, centerX, centerY);

        drawPartialCircle(canvas, x1, y1, 20, 5, getAngle(x4, y4, x1, y1, x2, y2));
        drawPartialCircle(canvas, x2, y2, 20, 5, getAngle(x1, y1, x2, y2, x3, y3));
        drawPartialCircle(canvas, x3, y3, 20, 5, getAngle(x2, y2, x3, y3, x4, y4));
        drawPartialCircle(canvas, x4, y4, 20, 5, getAngle(x3, y3, x4, y4, x1, y1));
    }

    /**
     * Calculates the angle between three points.
     *
     * @param x1 The x-coordinate of the first point.
     * @param y1 The y-coordinate of the first point.
     * @param x2 The x-coordinate of the second point.
     * @param y2 The y-coordinate of the second point.
     * @param x3 The x-coordinate of the third point.
     * @param y3 The y-coordinate of the third point.
     * @return The list of angles.
     */
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

    /**
     * Sets the translation in the x-axis.
     *
     * @param v The translation value.
     */
    public void setTranslateX(float v) {
        translateX = v;
        invalidate();
    }

    /**
     * Sets the translation in the y-axis.
     *
     * @param v The translation value.
     */
    public void setTranslateY(float v) {
        translateY = v;
        invalidate();
    }

    /**
     * Interface for handling station click events.
     */
    public interface OnStationClickListener {
        void onStationClick(Station station);
    }
}