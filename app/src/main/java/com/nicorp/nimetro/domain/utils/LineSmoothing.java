package com.nicorp.nimetro.domain.utils;

import android.graphics.Point;

import com.nicorp.nimetro.domain.entities.Station;

import java.util.ArrayList;
import java.util.List;

public class LineSmoothing {
    private static final int SMOOTHING_SEGMENTS = 16; // Количество сегментов для сглаживания
    private static final float TENSION = 0.5f; // Параметр натяжения для управления кривизной

    /**
     * Создает список промежуточных точек для сглаживания линии между двумя станциями
     */
    public static List<Point> smoothLine(Station station1, Station station2, List<Point> controlPoints) {
        List<Point> smoothPoints = new ArrayList<>();

        // Если нет контрольных точек, создаем прямую линию с промежуточными точками
        if (controlPoints == null || controlPoints.isEmpty()) {
            return createSmoothDirectLine(
                    new Point(station1.getX(), station1.getY()),
                    new Point(station2.getX(), station2.getY())
            );
        }

        // Добавляем начальную точку
        controlPoints.add(0, new Point(station1.getX(), station1.getY()));
        // Добавляем конечную точку
        controlPoints.add(new Point(station2.getX(), station2.getY()));

        // Проходим по всем сегментам между контрольными точками
        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Point p1 = controlPoints.get(i);
            Point p2 = controlPoints.get(i + 1);

            // Находим точки для сглаживания текущего сегмента
            List<Point> segmentPoints = smoothSegment(p1, p2, i > 0 ? controlPoints.get(i - 1) : null,
                    i < controlPoints.size() - 2 ? controlPoints.get(i + 2) : null);

            // Добавляем точки сегмента, кроме последней (чтобы избежать дублирования)
            smoothPoints.addAll(segmentPoints.subList(0, segmentPoints.size() - 1));
        }

        // Добавляем последнюю точку
        smoothPoints.add(controlPoints.get(controlPoints.size() - 1));

        return smoothPoints;
    }

    /**
     * Создает промежуточные точки для прямой линии
     */
    private static List<Point> createSmoothDirectLine(Point start, Point end) {
        List<Point> points = new ArrayList<>();
        points.add(start);

        // Добавляем промежуточные точки для создания плавного перехода
        for (int i = 1; i < SMOOTHING_SEGMENTS; i++) {
            float t = (float) i / SMOOTHING_SEGMENTS;
            float x = start.x + (end.x - start.x) * t;
            float y = start.y + (end.y - start.y) * t;
            points.add(new Point((int)x, (int)y));
        }

        points.add(end);
        return points;
    }

    /**
     * Сглаживает отдельный сегмент линии с учетом соседних точек
     */
    private static List<Point> smoothSegment(Point p1, Point p2, Point prevPoint, Point nextPoint) {
        List<Point> points = new ArrayList<>();

        // Вычисляем векторы направления
        float dx = p2.x - p1.x;
        float dy = p2.y - p1.y;

        // Рассчитываем контрольные векторы для сглаживания
        float controlX1 = 0, controlY1 = 0, controlX2 = 0, controlY2 = 0;

        if (prevPoint != null) {
            controlX1 = (p2.x - prevPoint.x) * TENSION;
            controlY1 = (p2.y - prevPoint.y) * TENSION;
        } else {
            controlX1 = dx * TENSION;
            controlY1 = dy * TENSION;
        }

        if (nextPoint != null) {
            controlX2 = (nextPoint.x - p1.x) * TENSION;
            controlY2 = (nextPoint.y - p1.y) * TENSION;
        } else {
            controlX2 = dx * TENSION;
            controlY2 = dy * TENSION;
        }

        // Создаем промежуточные точки с использованием метода Катмулла-Рома
        for (int i = 0; i <= SMOOTHING_SEGMENTS; i++) {
            float t = (float) i / SMOOTHING_SEGMENTS;
            float t2 = t * t;
            float t3 = t2 * t;

            // Интерполяция положения точки
            float x = (2 * t3 - 3 * t2 + 1) * p1.x +
                    (t3 - 2 * t2 + t) * controlX1 +
                    (-2 * t3 + 3 * t2) * p2.x +
                    (t3 - t2) * controlX2;

            float y = (2 * t3 - 3 * t2 + 1) * p1.y +
                    (t3 - 2 * t2 + t) * controlY1 +
                    (-2 * t3 + 3 * t2) * p2.y +
                    (t3 - t2) * controlY2;

            points.add(new Point((int)x, (int)y));
        }

        return points;
    }
}