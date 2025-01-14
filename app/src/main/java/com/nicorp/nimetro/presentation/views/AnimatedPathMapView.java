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
    private float strokeWidth = 8f;
    private int pathColor = Color.RED;
    private float[] intervals = new float[]{40f, 20f}; // dash and gap lengths
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
        pathPaint.setStrokeWidth(strokeWidth);
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
                if (viewWidth > 0 && viewHeight > 0) {
                    svgDrawable.setBounds(0, 0, viewWidth, viewHeight);
                }
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
        if (svgDrawable != null) {
            svgDrawable.setBounds(0, 0, w, h);
        }
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
        if (viewWidth > 0 && viewHeight > 0 && svgDrawable != null) {
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
        if (viewWidth > 0 && viewHeight > 0 && svgDrawable != null) {
            setPathInternal(points);
        } else {
            postInvalidate();
        }
    }

    private void setPathInternal(float[] points) {
        float scaleX = (float) viewWidth / svgIntrinsicWidth;
        float scaleY = (float) viewHeight / svgIntrinsicHeight;

        animatedPath.reset();
        animatedPath.moveTo(points[0] * scaleX, points[1] * scaleY);
        for (int i = 2; i < points.length; i += 2) {
            animatedPath.lineTo(points[i] * scaleX, points[i + 1] * scaleY);
        }
        startAnimation();
    }

    private void setPathInternal(List<PointF> points) {
        float scaleX = (float) viewWidth / svgIntrinsicWidth;
        float scaleY = (float) viewHeight / svgIntrinsicHeight;

        animatedPath.reset();
        PointF firstPoint = points.get(0);
        animatedPath.moveTo(firstPoint.x * scaleX, firstPoint.y * scaleY);
        for (int i = 1; i < points.size(); i++) {
            PointF point = points.get(i);
            animatedPath.lineTo(point.x * scaleX, point.y * scaleY);
        }
        startAnimation();
    }

    private void startAnimation() {
        if (animator != null) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(0f, intervals[0] + intervals[1]);
        animator.setDuration(1000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            float phase = (float) animation.getAnimatedValue();
            if (isReversed) {
                phase = -phase;
            }
            pathPaint.setPathEffect(new DashPathEffect(intervals, phase));
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
        this.strokeWidth = width;
        pathPaint.setStrokeWidth(width);
        invalidate();
    }

    public void setDashIntervals(float dash, float gap) {
        this.intervals = new float[]{dash, gap};
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
}