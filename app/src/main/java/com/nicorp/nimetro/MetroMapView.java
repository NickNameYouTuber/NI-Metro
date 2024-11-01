package com.nicorp.nimetro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.List;

public class MetroMapView extends View {

    private List<Line> lines;
    private List<Station> stations;
    private List<Station> route;
    private List<Station> selectedStations;

    private Paint linePaint;
    private Paint stationPaint;
    private Paint selectedStationPaint;
    private Paint routePaint;
    private Paint textPaint;
    private Paint whitePaint;

    private OnStationClickListener listener;

    private float scaleFactor = 1.0f;
    private float translateX = 0.0f;
    private float translateY = 0.0f;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private static final float COORDINATE_SCALE_FACTOR = 2.0f;
    private static final float CLICK_RADIUS = 30.0f;

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

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(20);

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

    public void setData(List<Line> lines, List<Station> stations) {
        this.lines = lines;
        this.stations = stations;
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
            // Draw lines
            for (Line line : lines) {
                linePaint.setColor(Color.parseColor(line.getColor()));
                for (int i = 0; i < line.getStations().size() - 1; i++) {
                    Station station1 = line.getStations().get(i);
                    Station station2 = line.getStations().get(i + 1);
                    drawLineWithIntermediatePoints(canvas, station1, station2);
                }

                // Draw circle line if isCircle is true
                if (line.isCircle() && line.getStations().size() > 1) {
                    Station firstStation = line.getStations().get(0);
                    Station lastStation = line.getStations().get(line.getStations().size() - 1);
                    drawLineWithIntermediatePoints(canvas, firstStation, lastStation);
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

                // Draw additional outline if the station is selected
                if (selectedStations != null && selectedStations.contains(station)) {
                    canvas.drawCircle(stationX, stationY, 20, selectedStationPaint);
                }

                // Draw text label based on textPosition
                drawTextBasedOnPosition(canvas, station.getName(), stationX, stationY, station.getTextPosition(), textPaint);
            }

            // Draw route
            if (route != null && route.size() > 1) {
                for (int i = 0; i < route.size() - 1; i++) {
                    Station station1 = route.get(i);
                    Station station2 = route.get(i + 1);
                    drawRouteWithIntermediatePoints(canvas, station1, station2);
                }
            }
        }

        canvas.restore();
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
                offsetY = textHeight + 10;
                offsetX = -textWidth / 2;
                break;
            case 6: // Bottom Left
                offsetX = -textWidth / 2 - 10;
                offsetY = textHeight + 10;
                break;
            case 7: // Left
                offsetX = -textWidth - 10;
                break;
            case 8: // Top Left
                offsetX = -textWidth / 2 - 10;
                offsetY = -textHeight - 10;
                break;
            default: // Center (textPosition == 0)
                offsetX = -textWidth / 2;
                offsetY = textHeight / 2;
                break;
        }

        canvas.drawText(text, cx + offsetX, cy + offsetY, paint);
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

    private Station findStationAt(float x, float y) {
        for (Station station : stations) {
            if (Math.abs(station.getX() - x) < CLICK_RADIUS / COORDINATE_SCALE_FACTOR && Math.abs(station.getY() - y) < CLICK_RADIUS / COORDINATE_SCALE_FACTOR) {
                return station;
            }
        }
        return null;
    }

    public interface OnStationClickListener {
        void onStationClick(Station station);
    }
}