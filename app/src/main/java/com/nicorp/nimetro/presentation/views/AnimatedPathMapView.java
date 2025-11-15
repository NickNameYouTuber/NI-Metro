package com.nicorp.nimetro.presentation.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.List;

public class AnimatedPathMapView extends View {

    private VectorDrawable svgDrawable;
    private Path animatedPath;
    private Paint pathPaint;
    private ValueAnimator animator;
    // Нормализованные значения в координатах 1000x1000
    private static final float LOGICAL_SIZE = 1000f;
    private float normalizedStrokeWidth = 8f;
    private int pathColor = Color.RED;
    private float[] normalizedIntervals = new float[]{40f, 20f};
    // Текущая матрица привязки логической сетки к View
    private float contentScale = 1f;
    private float contentOffsetX = 0f;
    private float contentOffsetY = 0f;
    private float[] scaledIntervals = new float[]{40f, 20f};
    private boolean isReversed = false;
    private int viewWidth, viewHeight;
    private int svgIntrinsicWidth, svgIntrinsicHeight;
    private float[] pathPointsArray;
    private List<PointF> pathPointsList;
    private boolean hasPathPoints = false;

    public AnimatedPathMapView(Context context) {
        super(context);
        init();
    }

    public AnimatedPathMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Initialize Paint for the path
        pathPaint = new Paint();
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(1f);
        pathPaint.setColor(pathColor);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setAntiAlias(true);

        // Create an empty Path for the animation
        animatedPath = new Path();
    }

    public void setSvgDrawable(@DrawableRes int svgResourceId) {
        try {
            // Load SVG as VectorDrawable
            svgDrawable = (VectorDrawable) AppCompatResources.getDrawable(getContext(), svgResourceId);
            if (svgDrawable != null) {
                svgIntrinsicWidth = svgDrawable.getIntrinsicWidth();
                svgIntrinsicHeight = svgDrawable.getIntrinsicHeight();
                updateDrawableBounds();
            }
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        updateTransform();
        updateDrawableBounds();
        if (hasPathPoints) {
            if (pathPointsArray != null) {
                setPathInternal(pathPointsArray);
            } else if (pathPointsList != null) {
                setPathInternal(pathPointsList);
            }
        }
    }

    public void setPath(float[] points) {
        if (points.length < 4 || points.length % 2 != 0) {
            throw new IllegalArgumentException("Points array must contain at least 2 points (4 coordinates) and have even length");
        }
        this.pathPointsArray = points.clone();
        this.pathPointsList = null;
        this.hasPathPoints = true;
        if (viewWidth > 0 && viewHeight > 0) {
            setPathInternal(points);
        } else {
            postInvalidate();
        }
    }

    public void setPath(List<PointF> points) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("Points list must contain at least 2 points");
        }
        this.pathPointsList = new ArrayList<>(points);
        this.pathPointsArray = null;
        this.hasPathPoints = true;
        if (viewWidth > 0 && viewHeight > 0) {
            setPathInternal(points);
        } else {
            postInvalidate();
        }
    }

    private void setPathInternal(float[] points) {
        animatedPath.reset();
        animatedPath.moveTo(points[0] * contentScale + contentOffsetX, points[1] * contentScale + contentOffsetY);
        for (int i = 2; i < points.length; i += 2) {
            animatedPath.lineTo(points[i] * contentScale + contentOffsetX, points[i + 1] * contentScale + contentOffsetY);
        }
        startAnimation();
    }

    private void setPathInternal(List<PointF> points) {
        animatedPath.reset();
        PointF firstPoint = points.get(0);
        animatedPath.moveTo(firstPoint.x * contentScale + contentOffsetX, firstPoint.y * contentScale + contentOffsetY);
        for (int i = 1; i < points.size(); i++) {
            PointF point = points.get(i);
            animatedPath.lineTo(point.x * contentScale + contentOffsetX, point.y * contentScale + contentOffsetY);
        }
        startAnimation();
    }

    private void startAnimation() {
        if (animator != null) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(0f, scaledIntervals[0] + scaledIntervals[1]);
        animator.setDuration(1000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            float phase = (float) animation.getAnimatedValue();
            if (isReversed) {
                phase = -phase;
            }
            pathPaint.setPathEffect(new DashPathEffect(scaledIntervals, phase));
            invalidate();
        });

        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (svgDrawable != null) {
            svgDrawable.draw(canvas);
        }

        canvas.drawPath(animatedPath, pathPaint);
    }

    public void setPathColor(@ColorInt int color) {
        this.pathColor = color;
        pathPaint.setColor(color);
        invalidate();
    }

    public void setStrokeWidth(float width) {
        this.normalizedStrokeWidth = width;
        updatePaintScale();
        invalidate();
    }

    public void setDashIntervals(float dash, float gap) {
        this.normalizedIntervals = new float[]{dash, gap};
        updatePaintScale();
        startAnimation();
    }

    public void setReversed(boolean reversed) {
        this.isReversed = reversed;
        startAnimation();
    }

    public void setAnimationDuration(long durationMs) {
        if (animator != null) {
            animator.setDuration(durationMs);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private void updateTransform() {
        if (viewWidth <= 0 || viewHeight <= 0) return;
        contentScale = Math.min(viewWidth, viewHeight) / LOGICAL_SIZE;
        float targetW = LOGICAL_SIZE * contentScale;
        float targetH = LOGICAL_SIZE * contentScale;
        contentOffsetX = (viewWidth - targetW) / 2f;
        contentOffsetY = (viewHeight - targetH) / 2f;
        updatePaintScale();
    }

    private void updatePaintScale() {
        float effectiveStroke = Math.max(1f, normalizedStrokeWidth * contentScale);
        pathPaint.setStrokeWidth(effectiveStroke);
        scaledIntervals = new float[]{Math.max(1f, normalizedIntervals[0] * contentScale), Math.max(1f, normalizedIntervals[1] * contentScale)};
    }

    private void updateDrawableBounds() {
        if (svgDrawable == null) return;
        // Рисуем фон внутри той же логической области 1000x1000 по центру без искажений сетки
        if (viewWidth <= 0 || viewHeight <= 0) return;
        // Используем ту же область, что и контент 1000x1000
        int left = (int) Math.floor(contentOffsetX);
        int top = (int) Math.floor(contentOffsetY);
        int right = (int) Math.ceil(contentOffsetX + LOGICAL_SIZE * contentScale);
        int bottom = (int) Math.ceil(contentOffsetY + LOGICAL_SIZE * contentScale);
        svgDrawable.setBounds(left, top, right, bottom);
    }
}