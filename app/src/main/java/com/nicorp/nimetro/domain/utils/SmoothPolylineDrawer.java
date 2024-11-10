package com.nicorp.nimetro.domain.utils;

import static com.nicorp.nimetro.presentation.views.MetroMapView.COORDINATE_SCALE_FACTOR;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import com.nicorp.nimetro.domain.entities.Station;

import java.util.ArrayList;
import java.util.List;

public class SmoothPolylineDrawer {

    /**
     * Рисует плавную кривую линию, проходящую через заданные точки, на Canvas.
     *
     * @param canvas Canvas, на котором будет рисоваться кривая
     * @param stations Список станций, определяющих траекторию кривой
     * @param paint Объект Paint, содержащий настройки для рисования
     */
    public static void drawSmoothPolyline(Canvas canvas, List<Station> stations, Paint paint) {
        if (stations == null || stations.size() < 2) {
            return;
        }

        List<PointF> points = new ArrayList<>();
        for (Station station : stations) {
            points.add(new PointF(station.getX() * COORDINATE_SCALE_FACTOR, station.getY() * COORDINATE_SCALE_FACTOR));
        }

        // Настраиваем Paint для рисования линии
        paint.setStyle(Paint.Style.STROKE);

        // Создаем путь
        Path path = new Path();
        path.moveTo(points.get(0).x, points.get(0).y);

        if (points.size() == 2) {
            // Если только две точки, рисуем прямую линию
            path.lineTo(points.get(1).x, points.get(1).y);
        } else {
            // Для каждого сегмента между точками
            for (int i = 0; i < points.size() - 1; i++) {
                PointF current = points.get(i);
                PointF next = points.get(i + 1);

                // Вычисляем контрольные точки для кубической кривой Безье
                PointF control1 = new PointF();
                PointF control2 = new PointF();

                // Определяем длину сегмента
                float segmentLength = (float) Math.hypot(next.x - current.x, next.y - current.y);

                // Коэффициент сглаживания (можно регулировать для изменения степени сглаживания)
                float smoothing = 0.2f;

                if (i > 0) {
                    // Предыдущая точка существует
                    PointF prev = points.get(i - 1);

                    // Вычисляем направляющий вектор для первой контрольной точки
                    float dx = next.x - prev.x;
                    float dy = next.y - prev.y;

                    control1.x = current.x + (dx * smoothing);
                    control1.y = current.y + (dy * smoothing);
                } else {
                    // Для первой точки используем текущий сегмент
                    control1.x = current.x + (next.x - current.x) * smoothing;
                    control1.y = current.y + (next.y - current.y) * smoothing;
                }

                if (i < points.size() - 2) {
                    // Следующая точка существует
                    PointF nextNext = points.get(i + 2);

                    // Вычисляем направляющий вектор для второй контрольной точки
                    float dx = nextNext.x - current.x;
                    float dy = nextNext.y - current.y;

                    control2.x = next.x - (dx * smoothing);
                    control2.y = next.y - (dy * smoothing);
                } else {
                    // Для последнего сегмента используем текущий сегмент
                    control2.x = next.x - (next.x - current.x) * smoothing;
                    control2.y = next.y - (next.y - current.y) * smoothing;
                }

                // Рисуем кубическую кривую Безье
                path.cubicTo(
                        control1.x, control1.y,
                        control2.x, control2.y,
                        next.x, next.y
                );
            }
        }

        // Рисуем путь
        canvas.drawPath(path, paint);
    }
}