package com.sun.hellolady.demo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.sun.hellolady.R;
import com.support.util.common.LogUtil;

/**
 * Created by sunjiamin on 2016/9/15.
 * 刮奖效果控件 ScratchView
 */

public class ScratchView extends View {
    private static final int DEFAULT_MASKER_COLOR = Color.GRAY;
    /**
     * 最小的橡皮擦尺寸大小
     */
    private final static float DEFAULT_ERASER_SIZE = 60f;
    /**
     * 默认擦除比例
     */
    private final static int DEFAULT_PERCENT = 70;

    private int mMaskColor;
    /**
     * 遮罩画笔
     */
    private Paint mMaskPaint;

    /**
     * 遮罩的效果 bitmap
     */
    private Bitmap mMaskBitmap;

    /**
     * 绘制遮罩的 Canvas
     */
    private Canvas mMaskCanvas;

    /**
     * 普通绘制 Bitmap 用的 Paint
     */
    private Paint mBitmapPaint;
    /**
     * 橡皮檫画笔
     */
    private Paint mErasePaint;
    /**
     *  擦除路劲
     */
    private Path mErasePath;

    /**
     * 最小滑动距离
     */
    private int mTouchSlop;

    /**
     * 擦除效果起始点的x坐标
     */
    private float mStartX;
    /**
     * 擦除效果起始点的y坐标
     */
    private float mStartY;

    /**
     * 完成擦除
     */
    private boolean mIsCompleted = false;
    /**
     * 最大擦除比例
     */
    private int mMaxPercent = DEFAULT_PERCENT;
    /**
     * 当前擦除比例
     */
    private int mPercent = 0;
    /**
     * 存放蒙层像素信息的数组
     */
    private int mPixels[];

    EraseStatusListener eraseStatusListener;

    /**
     * 水印
     */
    private BitmapDrawable mWatermark;


    public ScratchView(Context context) {
        super(context);
        TypedArray typedArray = context.obtainStyledAttributes(R.styleable.ScratchView);
        init(typedArray);
    }

    public ScratchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScratchView);
        init(typedArray);
    }

    public ScratchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScratchView, defStyleAttr, 0);
        init(typedArray);
    }

    private void init(TypedArray typedArray) {
        mMaskColor = typedArray.getColor(R.styleable.ScratchView_maskColor, DEFAULT_MASKER_COLOR);
        mMaskPaint = new Paint();
        mMaskPaint.setAntiAlias(true);//抗锯齿
        mMaskPaint.setDither(true);//防抖
        setMaskColor(mMaskColor);

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setDither(true);


        float eraseSize = typedArray.getFloat(R.styleable.ScratchView_eraseSize, DEFAULT_ERASER_SIZE);
        mErasePaint  = new Paint();
        mErasePaint.setAntiAlias(true);//抗锯齿
        mErasePaint.setDither(true);//抗逗
        mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));//设置擦除效果
        mErasePaint.setStyle(Paint.Style.STROKE);
        mErasePaint.setStrokeCap(Paint.Cap.ROUND);//设置笔尖形状，让绘制的边缘圆滑
        setEraserSize(eraseSize);

        mErasePath = new Path();
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        mTouchSlop = viewConfiguration.getScaledTouchSlop();

        int watermarkResId = typedArray.getResourceId(R.styleable.ScratchView_watermark, -1);

        setWatermark(watermarkResId);

    }

    /**
     * 设置橡皮檫尺寸大小（默认大小是 60）
     *
     * @param eraseSize 橡皮檫尺寸大小
     */
    private void setEraserSize(float eraseSize) {
        mErasePaint.setStrokeWidth(eraseSize);
    }

    /**
     * 设置蒙版颜色
     * @param mMaskColor  十六进制颜色值，如：0xffff0000（不透明的红色）
     */
    private void setMaskColor(int mMaskColor) {
        mMaskPaint.setColor(mMaskColor);
    }


    /**
     * 设置水印图标
     *
     * @param resId 图标资源id，-1表示去除水印
     */
    public void setWatermark(int resId) {
        if (resId == -1) {
            mWatermark = null;
        } else {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
            mWatermark = new BitmapDrawable(bitmap);
            mWatermark.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        }
    }

    /**
     * 设置擦除监听监听
     * @param l
     */
    public void seteRaseStatusListener(EraseStatusListener l){
        eraseStatusListener=l;
    }

    /**
     * 在 onDraw 中，利用 canvas.drawBitmap 将 onSizeChanged 中初始化生成 mMaskBitmap 绘制显示到界面，生成刮层
     * Canvas 来初始化生成 mMaskBitmap
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        LogUtil.e("sjm","ScratchView onDraw--->-->");
        super.onDraw(canvas);
        canvas.drawBitmap(mMaskBitmap, 0, 0, mBitmapPaint);//绘制图层遮罩
        mBitmapPaint.setColor(Color.RED);
        canvas.drawText("孙佳敏",200,200,mBitmapPaint);
    }

    /**
     * 在 onSizeChanged 中，利用 View 已经 Measure 完毕，可以获得 View 的宽高，并使用 Canvas 来初始化生成 mMaskBitmap 用于制作刮层
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        LogUtil.e("sjm","ScratchView onSizeChanged--->--> W:"+w+" h:"+h);
        createMasker(w, h);
    }

    @Override
    protected void onFinishInflate() {
        LogUtil.e("sjm","ScratchView onFinishInflate--->-->");
        super.onFinishInflate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LogUtil.e("sjm","ScratchView onMeasure--->-->");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        LogUtil.e("sjm","ScratchView onLayout--->--> "+" left："+left+" top:"+top+" right:"+right+" bottom："+bottom);
        super.onLayout(changed, left, top, right, bottom);
    }



    /**
     * 创建蒙版 并使用 Canvas 来初始化生成 mMaskBitmap 用于制作刮层
     * @param w 宽度
     * @param h 高度
     */
    private void createMasker(int w, int h) {
        mMaskBitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
        mMaskCanvas = new Canvas(mMaskBitmap);
        Rect rect = new Rect(0,0,w,h);
        mMaskCanvas.drawRect(rect,mMaskPaint);
        //绘制生成和控件大小一致的遮罩 Bitmap

        if (mWatermark != null) {
            Rect bounds = new Rect(rect);
            mWatermark.setBounds(bounds);
            mWatermark.draw(mMaskCanvas);
        }
        mPixels = new int[w * h];
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startErase(event.getX(), event.getY());
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                erase(event.getX(), event.getY());
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                stopErase();
                invalidate();
                return true;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }


    /**
     * 开始擦除
     * @param x
     * @param y
     */
    private void startErase(float x, float y) {
        mErasePath.reset();
        mErasePath.moveTo(x,y);
        this.mStartX = x;
        this.mStartY = y;
    }
    /**
     * 擦除
     * @param x
     * @param y
     */
    private void erase(float x, float y) {
        int dx = (int) Math.abs(x - mStartX);
        int dy = (int) Math.abs(y - mStartY);
        if (dx >= mTouchSlop || dy >= mTouchSlop) {
            this.mStartX = x;
            this.mStartY = y;

            mErasePath.lineTo(x, y);
            mMaskCanvas.drawPath(mErasePath, mErasePaint);

            mErasePath.reset();
            mErasePath.moveTo(mStartX, mStartY);
        }
    }
    /**
     * 停止擦除
     */
    private void stopErase() {

        this.mStartX = 0;
        this.mStartY = 0;
        mErasePath.reset();

        updateErasePercent();
    }

    private void updateErasePercent() {
        int width = getWidth();
        int height = getHeight();
        new AsyncTask<Integer, Integer, Boolean>(){

            @Override
            protected Boolean doInBackground(Integer... params) {
                int width = params[0];
                int height = params[1];
                mMaskBitmap.getPixels(mPixels,0,width,0,0,width,height);
                //获取覆盖图层中所有的像素信息，stride用于表示一行的像素个数有多少
                float erasePixelCount = 0;//擦除的像素个数
                float totalPixelCount = width * height;//总像素个数
                for (int pos = 0; pos < totalPixelCount; pos++) {
                    if (mPixels[pos] == 0) {//透明的像素值为0
                        erasePixelCount++;
                    }
                }
                int percent = 0;
                if (erasePixelCount >= 0 && totalPixelCount > 0) {
                    percent = Math.round(erasePixelCount * 100 / totalPixelCount);
                    publishProgress(percent);
                }

                return percent >= mMaxPercent;


            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                mPercent = values[0];
                onPercentUpdate();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (aBoolean && !mIsCompleted) {//标记擦除，并完成回调
                    mIsCompleted = true;
                    if (eraseStatusListener != null) {
                        eraseStatusListener.onCompleted(ScratchView.this);
                    }
                }
            }
        }.execute(width,height);
    }

    private void onPercentUpdate() {
        if (eraseStatusListener != null) {
            eraseStatusListener.onProgress(mPercent);
        }
    }

    /**
     * 重置为初始状态
     */
    public void reset() {
        mIsCompleted = false;

        int width = getWidth();
        int height = getHeight();
        createMasker(width, height);
        invalidate();

        updateErasePercent();
    }

    /**
     * 清除整个图层
     */
    public void clear() {
        int width = getWidth();
        int height = getHeight();
        mMaskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mMaskCanvas = new Canvas(mMaskBitmap);
        Rect rect = new Rect(0, 0, width, height);
        mMaskCanvas.drawRect(rect, mErasePaint);
        invalidate();

        updateErasePercent();
    }


    /**
     * 擦除状态监听器
     */
    public static interface EraseStatusListener {

        /**
         * 擦除进度
         *
         * @param percent 进度值，大于0，小于等于100；
         */
        public void onProgress(int percent);

        /**
         * 擦除完成回调函数
         *
         * @param view
         */
        public void onCompleted(View view);
    }
}
