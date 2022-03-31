package com.szfission.wear.demo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 网格统计图
 * Created by Harlan on 1/10/2018.
 */
public class GridChartView extends View {

    private int xLineColor;     // X轴线颜色
    private int yLineColor;     // Y轴线颜色
    private int xTextColor;     // X轴标题字颜色
    private int yTextColor;     // Y轴标题字颜色
    private int gridLineColor;  // 网格线颜色
    private int centerLineColor; // 内容线颜色，即连接线

    private float xLineSize;        // X轴线大小
    private float xTextSize;        // X轴标题字大小
    private float yLineSize;        // Y轴线大小
    private float yTextSize;        // Y轴标题字大小
    private float gridLineSize;     // 网格线大小
    private float yTextMarginRight; // Y轴标题与Y轴的间距
    private float xTextMarginTop;   // X轴标题与X轴的间距
    private float xItemWidth;       // 单位宽度
    private float centerLineSize;               // 内容线大小
    private float startAutoMovingMarginRight;   // 内容到最右则距离开始自动移动
    private int maxValue;                       // 最大值
    private float centerMarginTop;              // 内容和厅部的距离
    private int unitValue;                      // 单位值

    private int height;
    private int width;

    private float yTextWidth;

    private float itemValueHeight; // 每个值对应Y轴高度

    private Paint xLinePaint;
    private Paint yLinePaint;
    private Paint xTextPaint;
    private Paint yTextPaint;
    private Paint gridLinePaint;
    private Paint centerLinePaint;

    private int yLineNumber;
    private float itemHeight;
    private float lineEndY;

    private List<Integer> values;

    private float touchStarX = 0f;
    private float moveWidth = 0f;
    private float minMoveWidth= 0f;

    private float yLineStartX = 0f; // Y轴X座标

    private final int unitTime = 20;

    private boolean autoMoving = false;

    private Long touchUpTime;

    public GridChartView(Context context) {
        super(context);
    }

    public GridChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GridChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.GridChartView);
        xLineColor = typedArray.getColor(R.styleable.GridChartView_xLineColor, Color.BLACK);
        yLineColor = typedArray.getColor(R.styleable.GridChartView_yLineColor, Color.BLACK);
        xTextColor = typedArray.getColor(R.styleable.GridChartView_xTextColor, Color.BLACK);
        yTextColor = typedArray.getColor(R.styleable.GridChartView_yTextColor, Color.BLACK);
        gridLineColor = typedArray.getColor(R.styleable.GridChartView_gridLineColor, Color.BLACK);
        centerLineColor = typedArray.getColor(R.styleable.GridChartView_centerLineColor, Color.BLACK);

        xLineSize = typedArray.getDimension(R.styleable.GridChartView_xLineSize, 10);
        xTextSize = typedArray.getDimension(R.styleable.GridChartView_xTextSize, 10);
        yLineSize = typedArray.getDimension(R.styleable.GridChartView_yLineSize, 10);
        yTextSize = typedArray.getDimension(R.styleable.GridChartView_yTextSize, 10);
        gridLineSize = typedArray.getDimension(R.styleable.GridChartView_gridLineSize, 10);
        yTextMarginRight = typedArray.getDimension(R.styleable.GridChartView_yTextMarginRight, 10);
        xTextMarginTop = typedArray.getDimension(R.styleable.GridChartView_xTextMarginTop, 10);
        xItemWidth = typedArray.getDimension(R.styleable.GridChartView_xItemWidth, 10);
        centerLineSize = typedArray.getDimension(R.styleable.GridChartView_centerLineSize, 10);
        startAutoMovingMarginRight = typedArray.getDimension(R.styleable.GridChartView_startAutoMovingMarginRight, 50);
        centerMarginTop = typedArray.getDimension(R.styleable.GridChartView_centerMarginTop, 50);

        maxValue = typedArray.getInt(R.styleable.GridChartView_maxValue, 300);
        unitValue = typedArray.getInt(R.styleable.GridChartView_unitValue, 50);

        initPaint();
    }

    private void initPaint() {
        xLinePaint = new Paint();
        xLinePaint.setColor(xLineColor);
        xLinePaint.setStrokeWidth(xLineSize);
        xLinePaint.setAntiAlias(true);

        yLinePaint = new Paint();
        yLinePaint.setColor(yLineColor);
        yLinePaint.setAntiAlias(true);
        yLinePaint.setStrokeWidth(yLineSize);

        xTextPaint = new Paint();
        xTextPaint.setColor(xTextColor);
        xTextPaint.setTextSize(xTextSize);
        xTextPaint.setAntiAlias(true);

        yTextPaint = new Paint();
        yTextPaint.setColor(yTextColor);
        yTextPaint.setTextSize(yTextSize);
        yTextPaint.setAntiAlias(true);

        gridLinePaint = new Paint();
        gridLinePaint.setColor(gridLineColor);
        gridLinePaint.setTextSize(gridLineSize);
        gridLinePaint.setAntiAlias(true);

        centerLinePaint = new Paint();
        centerLinePaint.setColor(centerLineColor);
        centerLinePaint.setStrokeWidth(centerLineSize);
        centerLinePaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
        String title = String.valueOf(maxValue);
        yTextWidth = yTextPaint.measureText(title);
        yLineNumber = (maxValue / unitValue);
        lineEndY = height - (xTextSize * 2) - xTextMarginTop;
        itemHeight = (lineEndY - centerMarginTop) / yLineNumber;
        itemValueHeight = height / maxValue;
        yLineStartX = yTextMarginRight + yTextWidth;
        if (values != null && values.size() > 0) {
            minMoveWidth = - (values.size() * xItemWidth - startAutoMovingMarginRight);
        }
        postInvalidate();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                autoMoving = false;
                touchStarX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - touchStarX) > 1) {
                    moveWidth += event.getX() - touchStarX;
                    touchStarX = event.getX();
                    if (moveWidth > 0f) {
                        moveWidth = 0f;
                    }
                    if (moveWidth >= minMoveWidth) {
                        Log.d("GridChartView","moveWidth:" + moveWidth);
                    }else{
                        moveWidth = minMoveWidth;
                    }
                    postInvalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                touchStarX = 0f;
                touchUpTime = System.currentTimeMillis();
                break;
        }
        //return super.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawXLine(canvas);
        drawYLine(canvas);
        drawGridLine(canvas);
        drawCenterLine(canvas);
        drawTitle(canvas);
        if (autoMoving && touchStarX == 0f && (values.size() * xItemWidth + yLineStartX + startAutoMovingMarginRight) + moveWidth > width) {
            postDelayed(autoMoveRunnable, 300);
        }


    }

    /**
     * 画网格线
     *
     * @param canvas
     */
    private void drawGridLine(Canvas canvas) {
        for (int i = 1; i <= yLineNumber; i++) {
            float y = lineEndY - (i * itemHeight);
            float startX = yTextMarginRight + yTextWidth;
            canvas.drawLine(startX, y, width, y, gridLinePaint);
        }

        // 纵线
        int length = Integer.MAX_VALUE;
        for (int i = 0; i < length; i++) {
            float x = i * (xItemWidth * unitTime) + yTextWidth + yTextMarginRight + moveWidth;
            if (x <= yLineStartX) {
                continue;
            }
            if (x > width) {
                break;
            }
            canvas.drawLine(x, lineEndY, x, centerMarginTop, gridLinePaint);
        }
    }

    /**
     * 画X轴
     *
     * @param canvas
     */
    private void drawXLine(Canvas canvas) {
        float startX = yTextMarginRight + yTextWidth;
        canvas.drawLine(startX, lineEndY, width, lineEndY, xLinePaint);
    }

    private void drawTitle(Canvas canvas) {
        // Y 轴标题
        for (int i = 0; i <= yLineNumber; i++) {
            float y = lineEndY - (i * itemHeight);
            String text = String.valueOf(i * unitValue);
            canvas.drawText(text, 0, y, yTextPaint);
        }

        int length = Integer.MAX_VALUE;
        for (int i = 0; i < length; i++) {
            String text = String.valueOf(i * unitTime);
            float x = (i * xItemWidth * unitTime) + yTextWidth + yTextMarginRight - (xTextPaint.measureText(text) / 2) + moveWidth;
            if(x > width ){
                break;
            }
            canvas.drawText(text, x, height - xTextSize + yLineSize, xTextPaint);
        }

        // X轴标题
       /* int size = values.size() / unitTime;
        if (values.size() % unitTime > 0) {
            size++;
        }
        if (size > 0) {
            for (int i = 0; i <= size; i++) {
                String text = String.valueOf(i * unitTime);
                float x = (i * xItemWidth * unitTime) + yTextWidth + yTextMarginRight - (xTextPaint.measureText(text) / 2) + moveWidth;
                canvas.drawText(text, x, height - xTextSize + yLineSize, xTextPaint);
            }
        }*/
    }

    /**
     * 画Y 轴
     *
     * @param canvas
     */
    private void drawYLine(Canvas canvas) {
        canvas.drawLine(yTextMarginRight + yTextWidth, 0, yTextMarginRight + yTextWidth, lineEndY, yLinePaint);
    }

    /**
     * 画内容中的连接线
     *
     * @param canvas
     */
    private void drawCenterLine(Canvas canvas) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (int i = 1; i < values.size(); i++) {
            int lastValue = values.get(i - 1);
            int value = values.get(i);
            float startX = xItemWidth * (i - 1) + yTextWidth + yTextMarginRight + moveWidth;
            float startY = lineEndY - (lastValue * itemValueHeight);
            float stopX = xItemWidth * i + yTextWidth + yTextMarginRight + moveWidth;
            float stopY = lineEndY - (value * itemValueHeight);

            //如果两个点都在Y轴左测则不用画
            if (startX < yTextMarginRight + yTextWidth && stopX < yTextMarginRight + yTextWidth) {
                continue;
            }
            if (startX < yTextMarginRight + yTextWidth) {
                startY = getFocusY(startX, startY, stopX, stopY, yTextMarginRight + yTextWidth);
                startX = yTextMarginRight + yTextWidth;
            }
            canvas.drawLine(startX, startY, stopX, stopY, centerLinePaint);
        }
    }

    /**
     * 求和与Y轴交点的Y座标
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @return
     */
    private float getFocusY(float x1, float y1, float x2, float y2, float x3) {
        return y2 - (((x2 - x3) / (x2 - x1)) * (y2 - y1));
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }

    public void addValue(int value) {
        if (this.values == null) {
            this.values = new ArrayList<>();
        }
        this.values.add(value);
        minMoveWidth = - (values.size() * xItemWidth - startAutoMovingMarginRight);

        if (touchStarX != 0f) {
            return;
        }

        if(touchUpTime != null && System.currentTimeMillis() - touchUpTime < 2000){
            return ;
        }

        if ((width - yLineStartX - startAutoMovingMarginRight - values.size() * xItemWidth) < moveWidth) {
            moveWidth = width - yLineStartX - startAutoMovingMarginRight - values.size() * xItemWidth;
        }
        autoMoving = true;
        postInvalidate();
    }

    Runnable autoMoveRunnable = new Runnable() {
        @Override
        public void run() {
            moveWidth -= xItemWidth;
            autoMoving = false;
            postInvalidate();
        }
    };

}
