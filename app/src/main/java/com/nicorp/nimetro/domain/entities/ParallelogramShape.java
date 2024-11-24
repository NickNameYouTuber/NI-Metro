package com.nicorp.nimetro.domain.entities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.Shape;

public class ParallelogramShape extends Shape {
    private final int color;

    public ParallelogramShape(int color) {
        this.color = color;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        Path path = new Path();
        path.moveTo(getWidth() / 4, 15); // Верхний правый угол
        path.lineTo(getWidth() - 5, 15); // Верхний левый угол
        path.lineTo(getWidth() * 3 / 4, getHeight() - 15); // Нижний левый угол
        path.lineTo(0 + 5, getHeight() - 15); // Нижний правый угол
        path.close();

        Paint strokePaint = new Paint(paint);
        strokePaint.setStyle(Paint.Style.FILL);
        strokePaint.setColor(color);
        strokePaint.setStrokeWidth(10); // Ширина обводки
        canvas.drawPath(path, strokePaint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(color);
        strokePaint.setStrokeWidth(10); // Ширина обводки
        canvas.drawPath(path, strokePaint);
    }
}
