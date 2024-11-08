package com.nicorp.nimetro.presentation.views;

import android.content.Context;
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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.google.android.material.color.MaterialColors;
import com.nicorp.nimetro.data.models.MapObject;
import com.nicorp.nimetro.data.models.River;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Transfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MetroMapView extends View {

    private List<Line> lines;
    private List<Station> stations;
    private List<Station> route;
    private List<Station> selectedStations;
    private List<Transfer> transfers;
    private List<River> rivers; // Добавлен список рек
    private List<MapObject> mapObjects; // Добавлен список объектов

    private Paint linePaint;
    private Paint stationPaint;
    private Paint selectedStationPaint;
    private Paint routePaint;
    private Paint textPaint;
    private Paint whitePaint;
    private Paint transferPaint;
    private Paint stationCenterPaint;
    private Paint riverPaint; // Добавлен Paint для рек

    private OnStationClickListener listener;

    private float scaleFactor = 1.0f;
    private float translateX = 0.0f;
    private float translateY = 0.0f;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    public static final float COORDINATE_SCALE_FACTOR = 2.5f;
    private static final float CLICK_RADIUS = 30.0f;
    private static final float TRANSFER_CAPSULE_WIDTH = 40.0f;

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

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(7);

        stationPaint = new Paint();
        stationPaint.setColor(Color.BLUE);
        stationPaint.setStyle(Paint.Style.STROKE);
        stationPaint.setStrokeWidth(6);

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
        stationCenterPaint.setStrokeWidth(5);

        riverPaint = new Paint(); // Инициализация Paint для рек
        riverPaint.setColor(Color.parseColor("#CCE0EA")); // Бледно-голубой цвет
        riverPaint.setStyle(Paint.Style.STROKE);
        riverPaint.setStrokeWidth(10); // Ширина реки

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
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 2.0f));
                invalidate();
                return true;
            }
        });
    }

    public void clearRoute() {
        this.route = null;
        invalidate();
    }

    public void clearSelectedStations() {
        this.selectedStations = null;
        invalidate();
    }

    public void setData(List<Line> lines, List<Station> stations, List<Transfer> transfers, List<River> rivers, List<MapObject> mapObjects) {
        this.lines = lines;
        this.stations = stations;
        this.transfers = transfers;
        this.rivers = rivers;
        this.mapObjects = mapObjects; // Установка списка объектов
        invalidate();
    }
    public void setRoute(List<Station> route) {
        this.route = route;
        invalidate();
    }

    public void setSelectedStations(List<Station> selectedStations) {
        this.selectedStations = selectedStations;
        invalidate();
    }

    public void setOnStationClickListener(OnStationClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        if (lines != null && stations != null) {
            // Draw rivers first
            if (rivers != null) {
                for (River river : rivers) {
                    drawRiver(canvas, river);
                }
            }

            // Draw lines
            for (Line line : lines) {
                linePaint.setColor(Color.parseColor(line.getColor()));
                for (int i = 0; i < line.getStations().size() - 1; i++) {
                    Station station1 = line.getStations().get(i);
                    Station station2 = line.getStations().get(i + 1);
                    drawLineWithIntermediatePoints(canvas, station1, station2);
                }

                if (line.isCircle() && line.getStations().size() > 1) {
                    Station firstStation = line.getStations().get(0);
                    Station lastStation = line.getStations().get(line.getStations().size() - 1);
                    drawLineWithIntermediatePoints(canvas, firstStation, lastStation);
                }
            }

            // Draw transfers between stations
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

            // Draw stations
            for (Station station : stations) {
                float stationX = station.getX() * COORDINATE_SCALE_FACTOR;
                float stationY = station.getY() * COORDINATE_SCALE_FACTOR;

                // Draw white center of the station
                canvas.drawCircle(stationX, stationY, 10, whitePaint);

                // Draw colored outline of the station
                stationPaint.setColor(Color.parseColor(station.getColor()));
                canvas.drawCircle(stationX, stationY, 14, stationPaint);

                if (selectedStations != null && selectedStations.contains(station)) {
                    canvas.drawCircle(stationX, stationY, 20, selectedStationPaint);
                }

                drawTextBasedOnPosition(canvas, station.getName(), stationX, stationY,
                        station.getTextPosition(), textPaint);
            }

            // Draw route
            if (route != null && route.size() > 1) {
                for (int i = 0; i < route.size() - 1; i++) {
                    Station station1 = route.get(i);
                    Station station2 = route.get(i + 1);
                    drawRouteWithIntermediatePoints(canvas, station1, station2);
                }
            }

            // Draw map objects
            if (mapObjects != null) {
                for (MapObject mapObject : mapObjects) {
                    drawMapObject(canvas, mapObject);
                }
            }
        }

        canvas.restore();
    }

    private void drawMapObject(Canvas canvas, MapObject mapObject) {
        float objectX = mapObject.getPosition().x * COORDINATE_SCALE_FACTOR;
        float objectY = mapObject.getPosition().y * COORDINATE_SCALE_FACTOR;

        Paint objectPaint = new Paint();
        objectPaint.setColor(Color.BLACK);
        objectPaint.setStyle(Paint.Style.FILL);
        objectPaint.setTextSize(24);

        // Draw the object icon based on its type
        if (mapObject.getType().equals("airport")) {
            canvas.drawText("✈", objectX - 12, objectY + 12, objectPaint);
        } else if (mapObject.getType().equals("train_station")) {
            canvas.drawText("🚂", objectX - 12, objectY + 12, objectPaint);
        }

        // Draw the object name
        canvas.drawText(mapObject.getDisplayName(), objectX + 20, objectY, objectPaint);
    }

    private void drawRiver(Canvas canvas, River river) {
        List<Point> points = river.getPoints();
        int width = river.getWidth();

        if (points.size() < 2) {
            return;
        }

        // Create the river path
        Path riverPath = new Path();
        riverPath.moveTo(points.get(0).x * COORDINATE_SCALE_FACTOR, points.get(0).y * COORDINATE_SCALE_FACTOR);
        for (int i = 1; i < points.size(); i++) {
            riverPath.lineTo(points.get(i).x * COORDINATE_SCALE_FACTOR, points.get(i).y * COORDINATE_SCALE_FACTOR);
        }

        // Calculate the river length
        float riverLength = 0;
        for (int i = 1; i < points.size(); i++) {
            Point p1 = points.get(i - 1);
            Point p2 = points.get(i);
            riverLength += Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
        }

        // Set up a symmetrical fade-out gradient on both ends
        Point startPoint = points.get(0);
        Point endPoint = points.get(points.size() - 1);
        int[] fadeColors = {Color.parseColor("#00000000"), Color.parseColor("#ADD8E6"), Color.parseColor("#ADD8E6"), Color.parseColor("#00000000")};
        float fadeMargin = 20 / riverLength; // Margin for fade-out at both ends
        float[] fadePositions = {0.0f, fadeMargin, 1.0f - fadeMargin, 1.0f};

        LinearGradient fadeGradient = new LinearGradient(
                startPoint.x * COORDINATE_SCALE_FACTOR, startPoint.y * COORDINATE_SCALE_FACTOR,
                endPoint.x * COORDINATE_SCALE_FACTOR, endPoint.y * COORDINATE_SCALE_FACTOR,
                fadeColors, fadePositions, Shader.TileMode.CLAMP);

        riverPaint.setShader(fadeGradient);
        riverPaint.setStrokeWidth(width);

        // Draw the river path with the gradient
        canvas.drawPath(riverPath, riverPaint);
    }

    private void drawLineWithIntermediatePoints(Canvas canvas, Station station1, Station station2) {
        List<Point> intermediatePoints = station1.getIntermediatePoints(station2);
        if (intermediatePoints == null || intermediatePoints.isEmpty()) {
            canvas.drawLine(station1.getX() * COORDINATE_SCALE_FACTOR, station1.getY() * COORDINATE_SCALE_FACTOR,
                    station2.getX() * COORDINATE_SCALE_FACTOR, station2.getY() * COORDINATE_SCALE_FACTOR, linePaint);
        } else {
            float startX = station1.getX() * COORDINATE_SCALE_FACTOR;
            float startY = station1.getY() * COORDINATE_SCALE_FACTOR;
            for (Point point : intermediatePoints) {
                float endX = point.x * COORDINATE_SCALE_FACTOR;
                float endY = point.y * COORDINATE_SCALE_FACTOR;
                canvas.drawLine(startX, startY, endX, endY, linePaint);
                startX = endX;
                startY = endY;
            }
            canvas.drawLine(startX, startY, station2.getX() * COORDINATE_SCALE_FACTOR, station2.getY() * COORDINATE_SCALE_FACTOR, linePaint);
        }
    }

    private void drawRouteWithIntermediatePoints(Canvas canvas, Station station1, Station station2) {
        List<Point> intermediatePoints = station1.getIntermediatePoints(station2);
        if (intermediatePoints == null || intermediatePoints.isEmpty()) {
            canvas.drawLine(station1.getX() * COORDINATE_SCALE_FACTOR, station1.getY() * COORDINATE_SCALE_FACTOR,
                    station2.getX() * COORDINATE_SCALE_FACTOR, station2.getY() * COORDINATE_SCALE_FACTOR, routePaint);
        } else {
            float startX = station1.getX() * COORDINATE_SCALE_FACTOR;
            float startY = station1.getY() * COORDINATE_SCALE_FACTOR;
            for (Point point : intermediatePoints) {
                float endX = point.x * COORDINATE_SCALE_FACTOR;
                float endY = point.y * COORDINATE_SCALE_FACTOR;
                canvas.drawLine(startX, startY, endX, endY, routePaint);
                startX = endX;
                startY = endY;
            }
            canvas.drawLine(startX, startY, station2.getX() * COORDINATE_SCALE_FACTOR, station2.getY() * COORDINATE_SCALE_FACTOR, routePaint);
        }
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = scaleGestureDetector.onTouchEvent(event);
        result = gestureDetector.onTouchEvent(event) || result;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX() / scaleFactor - translateX;
            float y = event.getY() / scaleFactor - translateY;
            Station clickedStation = findStationAt(x / COORDINATE_SCALE_FACTOR, y / COORDINATE_SCALE_FACTOR);
            if (clickedStation != null && listener != null) {
                listener.onStationClick(clickedStation);
                return true;
            }
        }

        return result || super.onTouchEvent(event);
    }

    public Station findStationAt(float x, float y) {
        for (Station station : stations) {
            if (Math.abs(station.getX() - x) < CLICK_RADIUS / COORDINATE_SCALE_FACTOR && Math.abs(station.getY() - y) < CLICK_RADIUS / COORDINATE_SCALE_FACTOR) {
                return station;
            }
        }
        return null;
    }

    private void drawTransferConnection(Canvas canvas, Station station1, Station station2) {
        float x1 = station1.getX() * COORDINATE_SCALE_FACTOR;
        float y1 = station1.getY() * COORDINATE_SCALE_FACTOR;
        float x2 = station2.getX() * COORDINATE_SCALE_FACTOR;
        float y2 = station2.getY() * COORDINATE_SCALE_FACTOR;

        System.out.println("drawTransferConnection: " + x1 + " " + y1 + " " + x2 + " " + y2);

        // Вычисляем вектор направления и его длину
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        // Нормализуем вектор и создаем перпендикулярный вектор для смещения
        float nx = dx / length;
        float ny = dy / length;
        float perpX = -ny * (TRANSFER_CAPSULE_WIDTH / 2);
        float perpY = nx * (TRANSFER_CAPSULE_WIDTH / 2);

        // Создаем путь для капсулы
        Path capsulePath = new Path();

        // Начинаем с верхней точки первой станции
        capsulePath.moveTo(x1 + perpX, y1 + perpY);

        // Рисуем верхнюю линию
        capsulePath.lineTo(x2 + perpX, y2 + perpY);

        // Вычисляем угол направления линии в градусах
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        // Рисуем полукруг на конце
        RectF endCircle = new RectF(
                x2 - TRANSFER_CAPSULE_WIDTH/2,
                y2 - TRANSFER_CAPSULE_WIDTH/2,
                x2 + TRANSFER_CAPSULE_WIDTH/2,
                y2 + TRANSFER_CAPSULE_WIDTH/2
        );
        capsulePath.arcTo(endCircle, angle - 90, 180, false);

        capsulePath.moveTo(x2 - perpX, y2 - perpY);
        // Рисуем нижнюю линию обратно
        capsulePath.lineTo(x1 - perpX, y1 - perpY);

        // Рисуем полукруг на начале
        RectF startCircle = new RectF(
                x1 - TRANSFER_CAPSULE_WIDTH/2,
                y1 - TRANSFER_CAPSULE_WIDTH/2,
                x1 + TRANSFER_CAPSULE_WIDTH/2,
                y1 + TRANSFER_CAPSULE_WIDTH/2
        );
        capsulePath.arcTo(startCircle, angle + 90, 180, false);

        // Закрываем путь
        capsulePath.close();

        // Создаем Paint для заливки капсулы
        Paint capsuleFillPaint = new Paint(transferPaint);
        capsuleFillPaint.setStyle(Paint.Style.FILL);
        capsuleFillPaint.setColor(Color.WHITE);

        // Рисуем заливку и обводку
        canvas.drawPath(capsulePath, capsuleFillPaint);
        canvas.drawPath(capsulePath, transferPaint);
    }

    private void drawShiftedLine(Canvas canvas, float x1, float y1, float x2, float y2, float centerX, float centerY) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float shiftX = (dy / length) * 20;
        float shiftY = -(dx / length) * 20;

        // Смещение наружу от центра
        shiftX = -shiftX;
        shiftY = -shiftY;


        canvas.drawLine(x1 + shiftX, y1 + shiftY, x2 + shiftX, y2 + shiftY, transferPaint);
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

        System.out.println("startAngle: " + 0 + ", sweepAngle: " + sweepAngle);
        RectF rectF = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        canvas.drawArc(rectF, startAngle, sweepAngle, false, circleOutlinePaint);
    }

    private void drawTransferConnectionTriangle(Canvas canvas, Station station1, Station station2, Station station3) {
        float x1 = station1.getX() * COORDINATE_SCALE_FACTOR;
        float y1 = station1.getY() * COORDINATE_SCALE_FACTOR;
        float x2 = station2.getX() * COORDINATE_SCALE_FACTOR;
        float y2 = station2.getY() * COORDINATE_SCALE_FACTOR;
        float x3 = station3.getX() * COORDINATE_SCALE_FACTOR;
        float y3 = station3.getY() * COORDINATE_SCALE_FACTOR;

        // Calculate the center of the triangle
        float centerX = (x1 + x2 + x3) / 3;
        float centerY = (y1 + y2 + y3) / 3;

        // Draw lines shifted by 15 pixels from the center
        drawShiftedLine(canvas, x1, y1, x2, y2, centerX, centerY);
        drawShiftedLine(canvas, x2, y2, x3, y3, centerX, centerY);
        drawShiftedLine(canvas, x3, y3, x1, y1, centerX, centerY);

        // Draw partial circles at each station
        drawPartialCircle(canvas, x1, y1, 20, 5, getAngle(x3, y3, x1, y1, x2, y2));
        drawPartialCircle(canvas, x2, y2, 20, 5, getAngle(x1, y1, x2, y2, x3, y3));
        drawPartialCircle(canvas, x3, y3, 20, 5, getAngle(x2, y2, x3, y3, x1, y1));
    }

    private void drawTransferConnectionQuad(Canvas canvas, Station station1, Station station2, Station station3, Station station4) {
        float x1 = station1.getX() * COORDINATE_SCALE_FACTOR;
        float y1 = station1.getY() * COORDINATE_SCALE_FACTOR;
        float x2 = station2.getX() * COORDINATE_SCALE_FACTOR;
        float y2 = station2.getY() * COORDINATE_SCALE_FACTOR;
        float x3 = station3.getX() * COORDINATE_SCALE_FACTOR;
        float y3 = station3.getY() * COORDINATE_SCALE_FACTOR;
        float x4 = station4.getX() * COORDINATE_SCALE_FACTOR;
        float y4 = station4.getY() * COORDINATE_SCALE_FACTOR;

        // Calculate the center of the quad
        float centerX = (x1 + x2 + x3 + x4) / 4;
        float centerY = (y1 + y2 + y3 + y4) / 4;

        // Draw lines shifted by 15 pixels from the center
        drawShiftedLine(canvas, x1, y1, x2, y2, centerX, centerY);
        drawShiftedLine(canvas, x2, y2, x3, y3, centerX, centerY);
        drawShiftedLine(canvas, x3, y3, x4, y4, centerX, centerY);
        drawShiftedLine(canvas, x4, y4, x1, y1, centerX, centerY);

        // Draw partial circles at each station
        drawPartialCircle(canvas, x1, y1, 20, 5, getAngle(x4, y4, x1, y1, x2, y2));
        drawPartialCircle(canvas, x2, y2, 20, 5, getAngle(x1, y1, x2, y2, x3, y3));
        drawPartialCircle(canvas, x3, y3, 20, 5, getAngle(x2, y2, x3, y3, x4, y4));
        drawPartialCircle(canvas, x4, y4, 20, 5, getAngle(x3, y3, x4, y4, x1, y1));
    }

    public static List<Float> getAngle(double x1, double y1, double x2, double y2, double x3, double y3) {
        // Векторы между точками
        double dx1 = x2 - x1;
        double dy1 = y2 - y1;
        double dx2 = x3 - x2;
        double dy2 = y3 - y2;

        // Длины векторов
        double length1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
        double length2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

        // Вычисляем смещения для обеих линий
        double shift1X = -(dy1 / length1) * 20;
        double shift1Y = (dx1 / length1) * 20;
        double shift2X = -(dy2 / length2) * 20;
        double shift2Y = (dx2 / length2) * 20;

        // Координаты конечных точек смещенных линий около x2,y2
        double point1X = x2 + shift1X;
        double point1Y = y2 + shift1Y;
        double point2X = x2 + shift2X;
        double point2Y = y2 + shift2Y;

        // Вектора от центра (x2,y2) к смещенным точкам
        double vector1X = point1X - x2;
        double vector1Y = point1Y - y2;
        double vector2X = point2X - x2;
        double vector2Y = point2Y - y2;

        // Вычисляем угол между векторами
        double angle = Math.atan2(vector2Y, vector2X) - Math.atan2(vector1Y, vector1X);

        // Нормализуем угол в диапазон [0, 2π]
        if (angle < 0) {
            angle += 2 * Math.PI;
        }

        System.out.println("Angle: " + ((Math.atan2(vector2Y, vector2X) * 180 / Math.PI)));
        System.out.println("Shifted angle: " + ((Math.atan2(vector1Y, vector1X) * 180 / Math.PI)));
        System.out.println("Converted angle to degrees from atan2: " + (360 - (angle * 180 / Math.PI)));

        return new ArrayList<>(Arrays.asList((float) (360 - (angle * 180 / Math.PI)), (float) ((Math.atan2(vector2Y, vector2X) * 180 / Math.PI))));
    }

    public void setTranslateX(float v) {
        translateX = v;
        invalidate();
    }

    public void setTranslateY(float v) {
        translateY = v;
        invalidate();
    }

    public interface OnStationClickListener {
        void onStationClick(Station station);
    }
}