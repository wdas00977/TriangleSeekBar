package com.view.triangleseekbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 *  seekbar 2018/12/2.
 *
 *  @author Sweeps
 */
public class TriangleSeekBar extends View {
    /**
     * seekbar 默认底色
     */
    private static final int DEFAULT_SEEK_BACKGROUND_COLOR = 0xFFE5E5E5;

    /**
     * seekbar 进度颜色
     */
    private static final int DEFAULT_SEEK_PROGRESS_COLOR = 0xFF2196F3;

    /**
     * 整体view的默认背景色
     */
    private static final int DEFAULT_VIEW_BACKGROUND_COLOR = 0xFF000000;

    /**
     * 默认最大seek长度(距离)
     */
    private static final int DEFAULT_SEEK_LENGTH = 300;

    /**
     * 绘制seekbar的paint
     */
    private Paint mSeekPaint;

    /**
     * 绘制矩形的paint
     */
    private Paint mRectPaint;

    /**
     * 移动距离
     */
    private int onMoveDistance;

    /**
     * 起始点
     */
    private Point mStartPoint = new Point();

    /**
     * thumb width
     */
    private int mThumbWidth;

    /**
     * thumb height
     */
    private int mThumbHeight;

    /**
     * thumb 起始y方向坐标
     */
    private float mThumbStartY;

    /**
     * seekbar 最宽位置宽度
     */
    private int seekBarMaxWidth = 20;

    /**
     * seek背景左侧最宽位置的 x点
     */
    private float bgLeftEndX;

    /**
     * seek背景左侧最宽位置的 x点
     */
    private float bgRightEndX;

    /**
     * thumb 的位置
     */
    private Rect mThumbLocation = new Rect();

    /**
     * 当前百分比
     */
    private int currentPercent;

    private int deltaY;
    private Drawable mThumbDrawable;

    /**
     * thumb move listener
     */
    private OnThumbTouchMoveListener mOnThumbTouchMoveListener;
    private Paint mProgressPaint;

    public TriangleSeekBar(Context context) {
        this(context, null);
    }

    public TriangleSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(DEFAULT_VIEW_BACKGROUND_COLOR);
        initThumb(context);
        initPaint();

    }

    private void initThumb(Context context) {
        mThumbDrawable = context.getResources().getDrawable(R.drawable.icon_triangle_seekbar);
        if (mThumbDrawable != null) {
            mThumbWidth = mThumbDrawable.getIntrinsicWidth() >> 2;
            mThumbHeight = mThumbDrawable.getIntrinsicHeight() >> 2;
        }
    }
    private void initPaint() {
        mSeekPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSeekPaint.setStyle(Style.FILL);
        mSeekPaint.setColor(DEFAULT_SEEK_BACKGROUND_COLOR);
        mSeekPaint.setDither(true);

        mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressPaint.setStyle(Style.FILL);
        mProgressPaint.setColor(DEFAULT_SEEK_PROGRESS_COLOR);
        mProgressPaint.setDither(true);

        mRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRectPaint.setDither(true);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        // seek背景起始点
        mStartPoint.x = width >> 1;
        mStartPoint.y = 0;
        // 背景最宽位置左侧x点
        bgLeftEndX = mStartPoint.x - seekBarMaxWidth;
        // 背景最宽位置右侧x点
        bgRightEndX = mStartPoint.x + seekBarMaxWidth;

        // thumb y方向起始点
        mThumbStartY = mStartPoint.y + mThumbHeight;
        mThumbLocation.left = mStartPoint.x - mThumbWidth;
        mThumbLocation.top = mStartPoint.y;
        mThumbLocation.right = mStartPoint.x + mThumbWidth;
        mThumbLocation.bottom = mStartPoint.y + mThumbHeight << 1;

        // thumb的范围和rect一致
        mThumbDrawable.setBounds(mThumbLocation);
        if (currentPercent > 0) {
            handleThumbLocation(convertPercent2Distance(currentPercent));
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制背景
        drawSeekBackground(canvas);
        // 绘制前景(progress覆盖区域)
        drawSeekForeground(canvas);

        if (mThumbStartY < mStartPoint.y) {
            mThumbStartY = mStartPoint.y;
        } else if (mThumbStartY > DEFAULT_SEEK_LENGTH) {
            mThumbStartY = DEFAULT_SEEK_LENGTH;
        }

        // 绘制thumb
        canvas.drawRect(mThumbLocation, mRectPaint);
        mThumbDrawable.draw(canvas);
    }


    /**
     * 绘制前景(progress覆盖区域)
     * @param canvas Canvas
     */
    private void drawSeekForeground(Canvas canvas) {
        Path path = new Path();
        // -1以及后面lineTo中的+1， 是为了不让顶端看起来那么尖
        path.moveTo(mStartPoint.x - 1, mStartPoint.y);
        // 计算progress方向移动距离, +1是为了让覆盖区域略大于背景
        int move = (int)((float)currentPercent / 100 * seekBarMaxWidth + 1);
        int currLeftX = mStartPoint.x - move;
        int currRight = mStartPoint.x + move;
        // 限定边界
        if (currLeftX < bgLeftEndX) {
            currLeftX = (int)bgLeftEndX;
        }

        if (currRight > bgRightEndX) {
            currRight = (int)bgRightEndX;
        }

        path.lineTo(currLeftX, mStartPoint.y + onMoveDistance);
        path.lineTo(currRight, mStartPoint.y + onMoveDistance);
        path.lineTo(mStartPoint.x + 1, mStartPoint.y);
        path.close();

        canvas.drawPath(path, mProgressPaint);
    }

    /**
     * 绘制seek背景
     * @param canvas Canvas
     */
    private void drawSeekBackground(Canvas canvas) {
        Path path = new Path();
        // -1以及后面lineTo中的+1， 是为了不让顶端看起来那么尖
        path.moveTo(mStartPoint.x - 1, mStartPoint.y);
        path.lineTo(bgLeftEndX, DEFAULT_SEEK_LENGTH);
        path.lineTo(bgRightEndX, DEFAULT_SEEK_LENGTH);
        path.lineTo(mStartPoint.x + 1, mStartPoint.y);

        path.close();
        canvas.drawPath(path, mSeekPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int)event.getX();
        int y = (int)event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 检测点击区域是否在thumb的矩形范围
                if (!mThumbLocation.contains(x, y)) {
                    return false;
                }
                deltaY = y - mThumbLocation.top;
                break;

            case MotionEvent.ACTION_MOVE:
                // 更新矩形位置
                Rect dirtyRect = handleThumbLocation(y);
                computeMoveDistance(y);
                // 求出脏区域
                dirtyRect.union(mThumbLocation);
                invalidate(dirtyRect);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mOnThumbTouchMoveListener != null) {
                    mOnThumbTouchMoveListener.onMoveEnd(currentPercent);
                }
                break;
        }
        return true;
    }

    /**
     * 计算滑动距离
     * @param move 滑动距离
     */
    private void computeMoveDistance(int move) {
        onMoveDistance = move - mStartPoint.y;
        // 限定边界
        if (onMoveDistance > DEFAULT_SEEK_LENGTH) {
            onMoveDistance = DEFAULT_SEEK_LENGTH;
        }

        if (onMoveDistance < mStartPoint.y) {
            onMoveDistance = mStartPoint.y;
        }

        currentPercent = convertDistance2Percent(onMoveDistance, DEFAULT_SEEK_LENGTH);
    }

    /**
     * 将移动距离转换为百分比
     * @param moveDistance 移动距离
     * @param maxDistance 最大移动距离
     * @return 移动的百分比
     */
    private int convertDistance2Percent(int moveDistance, int maxDistance) {
        return (int)((float)moveDistance / (float)maxDistance * 100);
    }

    /**
     * 将移动的百分比转换为距离
     * @param seekPercent 移动的百分比
     * @return 移动的距离
     */
    private int convertPercent2Distance(int seekPercent) {
        return (int)((float)seekPercent / 100 * DEFAULT_SEEK_LENGTH);
    }

    /**
     * 根据移动距离， 计算thumb位置
     * @param move 移动距离
     * @return thumb bound location
     */
    private Rect handleThumbLocation(int move) {
        Rect rect = new Rect(mThumbLocation);
        if (mOnThumbTouchMoveListener != null) {
            mOnThumbTouchMoveListener.onMovePercent(currentPercent);
        }

        mThumbLocation.top = move - deltaY;
        if (mThumbLocation.top < mStartPoint.y) {
            mThumbLocation.top = mStartPoint.y;
        }

        if (mThumbLocation.top + mThumbHeight > DEFAULT_SEEK_LENGTH) {
            mThumbLocation.top = DEFAULT_SEEK_LENGTH - (mThumbHeight << 1);
        }

        mThumbLocation.bottom = mThumbLocation.top + mThumbWidth;
        if (mThumbLocation.bottom > DEFAULT_SEEK_LENGTH) {
            mThumbLocation.bottom = DEFAULT_SEEK_LENGTH;
        }
        mThumbDrawable.setBounds(mThumbLocation);
        return rect;
    }

    /**
     * @return 获取当前进度 [0, 100]
     */
    public int getProgressPercent() {
        return currentPercent;
    }

    /**
     * 设置进度百分比
     * @param currentPercent 当前进度百分比
     */
    public void setProgressPercent(int currentPercent) {
        this.currentPercent = currentPercent;
        computeMoveDistance(convertPercent2Distance(currentPercent));
    }

    /**
     * 设置seekbar底色
     * @param color 16进制颜色值
     */
    public void setSeekBarBackgroundColor(int color) {
        mSeekPaint.setColor(color);
    }

    /**
     * 设置seekbar进度值颜色
     * @param color 16进制颜色值
     */
    public void setSeekBarProgressColor(int color) {
        mProgressPaint.setColor(color);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = mThumbWidth << 1 + getPaddingLeft() + getPaddingRight();
        }
        if (heightMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED) {
            heightSize = DEFAULT_SEEK_LENGTH + getPaddingTop() + getPaddingBottom();
        }

        setMeasuredDimension(MeasureSpec.makeMeasureSpec(widthSize, widthMode), MeasureSpec.makeMeasureSpec(heightSize, heightMode));
    }

    public interface OnThumbTouchMoveListener {
        /**
         * seek移动
         * @param percent 移动的百分比
         */
        void onMovePercent(int percent);

        /**
         * seek结束移动
         * @param percent 结束时的百分比
         */
        void onMoveEnd(int percent);
    }

    /**
     * 设置thumb移动listener
     * @param listener OnThumbTouchMoveListener
     */
    public void setOnThumbTouchMoveListener(OnThumbTouchMoveListener listener) {
        this.mOnThumbTouchMoveListener = listener;
    }
}
