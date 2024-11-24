package com.nicorp.nimetro.domain.entities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.shapes.Shape;

public class CircleShape extends Shape {
    private final int color;

    public CircleShape(int color) {
        this.color = color;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        float radius = getWidth() / 2f;
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Вторая обводка цвета линии
        paint.setColor(color);
        canvas.drawCircle(centerX, centerY, radius, paint);

        // Первая обводка цвета линии
        paint.setColor(color);
        canvas.drawCircle(centerX, centerY, radius * 0.80f, paint);

        // Белый круг в центре
        paint.setColor(color);
        canvas.drawCircle(centerX, centerY, radius * 0.70f, paint);
    }
}
