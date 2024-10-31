package com.nicorp.nimetro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    private Paint linePaint;
    private Paint stationPaint;
    private Paint routePaint;
    private Paint textPaint;

    private OnStationClickListener listener;

    private float scaleFactor = 1.0f;
    private float translateX = 0.0f;
    private float translateY = 0.0f;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

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
        linePaint.setStrokeWidth(5);

        stationPaint = new Paint();
        stationPaint.setColor(Color.BLUE);

        routePaint = new Paint();
        routePaint.setColor(Color.YELLOW);
        routePaint.setStrokeWidth(8);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30);

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
                    canvas.drawLine(station1.getX(), station1.getY(), station2.getX(), station2.getY(), linePaint);
                }
            }

            // Draw stations
            for (Station station : stations) {
                canvas.drawCircle(station.getX(), station.getY(), 10, stationPaint);
                drawTextCentered(canvas, station.getName(), station.getX(), station.getY() + 30, textPaint);
            }

            // Draw route
            if (route != null && route.size() > 1) {
                for (int i = 0; i < route.size() - 1; i++) {
                    Station station1 = route.get(i);
                    Station station2 = route.get(i + 1);
                    canvas.drawLine(station1.getX(), station1.getY(), station2.getX(), station2.getY(), routePaint);
                }
            }
        }

        canvas.restore();
    }

    private void drawTextCentered(Canvas canvas, String text, float cx, float cy, Paint paint) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        canvas.drawText(text, cx - bounds.exactCenterX(), cy - bounds.exactCenterY(), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = scaleGestureDetector.onTouchEvent(event);
        result = gestureDetector.onTouchEvent(event) || result;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX() / scaleFactor - translateX;
            float y = event.getY() / scaleFactor - translateY;
            Station clickedStation = findStationAt(x, y);
            if (clickedStation != null && listener != null) {
                listener.onStationClick(clickedStation);
                return true;
            }
        }

        return result || super.onTouchEvent(event);
    }

    private Station findStationAt(float x, float y) {
        for (Station station : stations) {
            if (Math.abs(station.getX() - x) < 20 && Math.abs(station.getY() - y) < 20) {
                return station;
            }
        }
        return null;
    }

    public interface OnStationClickListener {
        void onStationClick(Station station);
    }
}