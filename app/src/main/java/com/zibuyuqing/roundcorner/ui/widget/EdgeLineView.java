package com.zibuyuqing.roundcorner.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.zibuyuqing.roundcorner.model.bean.EdgeLineConfig;
import com.zibuyuqing.roundcorner.utils.SettingsDataKeeper;
import com.zibuyuqing.roundcorner.utils.ViewUtil;

/**
 * <pre>
 *     author : Xijun.Wang
 *     e-mail : zibuyuqing@gmail.com
 *     time   : 2018/03/15
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class EdgeLineView extends View {
    private static final String TAG = EdgeLineView.class.getSimpleName();
    public static final int STYLE_FADE_IN_OUT = 0; //渐隐渐显
    public static final int STYLE_OUTSPREAD_MIDDLE_OUT = 1;// 中间向外展开
    public static final int STYLE_WIND = 2;// 绕圈
    public static final int STYLE_LATTICE = 3;//点阵
    public static final int STYLE_OUTSPREAD_BOTTOM_TO_TOP = 4;//从下往上

    public static final int DEFAULT_REPEAT_COUNT = 4; // 动画重复次数
    private int[] mMixedColorArr; // 混合颜色
    private Context mContext;
    private Path mPath; // 屏幕边缘
    private Paint mPrimaryPaint; // 主画笔，用于画单个色调
    private Paint mMixedPaint; // 混合颜色画笔
    private int mCornerSize; // 屏幕圆角尺寸
    private float mStrokeWidth; // path 的线宽
    private int mPrimaryColor; // 主色调
    private int mDuration; // 动画时间
    private int mStyle; // 动画风格
    private float mProgress; // 动画执行进度
    private int mScreenHeight;
    private int mScreenWidth;
    private EdgeLineConfig mConfig; // 配置项
    private RectF mScreenRectF;
    private boolean needReverse = true;//是否需要倒放 用户自己打开吧
    private ValueAnimator.AnimatorUpdateListener mValueUpdateListener;
    private Animator.AnimatorListener mAnimatorListener;
    private ValueAnimator mValueAnimator;
    private PathMeasure mPathMeasure;
    private float mPathLength;
    private boolean isAnimatorRunning = false;
    private AnimationStateListener mStateListener;
    private LinearGradient mColorShader; // 渐变色shader
    private Matrix mGradientMatrix;
    private float mTranslationX; // 混合颜色不同颜色的比例
    private float mTranslationY;
    private boolean needChangeAlpha = false;
    private Path mDst; // 画动画路径
    private DashPathEffect mPathEffect; // 用来话虚线
    private int mPhase = 0;
    private float[] mPathIntervals;
    private int mCurrentRepeatCount = 0;
    private boolean isAlwaysOnAble = false;
    private OnScreenConfigurationChangeListener mConfigurationChangeListener;
    public EdgeLineView(Context context) {
        this(context, null);
    }

    public EdgeLineView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EdgeLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        mScreenWidth = ViewUtil.getScreenWidth(mContext);
        mScreenHeight = ViewUtil.getScreenHeight(mContext);
        Log.e(TAG, "init ;; mScreenHeight =：" + mScreenHeight);
        mPath = new Path();
        mDst = new Path();
        mPathMeasure = new PathMeasure();
        mPrimaryPaint = new Paint();
        mPrimaryPaint.setAntiAlias(true);
        mMixedPaint = new Paint();
        mMixedPaint.setAntiAlias(true);
        mGradientMatrix = new Matrix();
        mAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                Log.e(TAG,"onAnimationCancel ---------------- ");
                hide(true);
                mCurrentRepeatCount = 0;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                super.onAnimationRepeat(animation);
                mCurrentRepeatCount++;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                hide(false);
                mCurrentRepeatCount = 0;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
            }
        };
        mValueUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgress = (float) animation.getAnimatedValue();
                if (mStateListener != null) {
                    mStateListener.onAnimationRunning(mProgress);
                }
                flush();
            }
        };
        mValueAnimator = ValueAnimator.ofFloat(0, 1);
        mValueAnimator.addUpdateListener(mValueUpdateListener);
        mValueAnimator.addListener(mAnimatorListener);
    }

    public void setOnScreenConfigurationChangeListener(OnScreenConfigurationChangeListener listener){
        mConfigurationChangeListener = listener;
    }
    private void flush() {
        if (needChangeAlpha) {
            mMixedPaint.setAlpha((int) (mProgress * 255));
            mPrimaryPaint.setAlpha((int) (mProgress * 255));
        }
        invalidate();
    }

    public void setConfig(EdgeLineConfig config) {
        mConfig = config;
        mPrimaryColor = config.getPrimaryColor();
        mCornerSize = getCornerSize();
        mStrokeWidth = config.getStrokeSize();
        mMixedColorArr = config.getMixedColorArr();
        mDuration = config.getDuration();
        mStyle = config.getStyle();
        isAlwaysOnAble = config.isAlwaysOnAble();
        mPrimaryPaint.setStrokeWidth(mStrokeWidth);
        mPrimaryPaint.setColor(mPrimaryColor);
        mPrimaryPaint.setStyle(Paint.Style.STROKE);

        mColorShader = new LinearGradient(0, 0, mScreenWidth, mScreenHeight, mMixedColorArr, null, Shader.TileMode.MIRROR);
        mMixedPaint.setShader(mColorShader);
        mMixedPaint.setStyle(Paint.Style.STROKE);
        mMixedPaint.setStrokeWidth(mStrokeWidth);
        Log.e(TAG,"mStyle =:" + mStyle +",mStrokeWidth =:" + mStrokeWidth);
        switch (mStyle) {
            case STYLE_FADE_IN_OUT:
                needReverse = true;
                needChangeAlpha = true;
                break;
            case STYLE_WIND:
                needChangeAlpha = false;
                needReverse = false;
                break;
            case STYLE_OUTSPREAD_MIDDLE_OUT:
                needChangeAlpha = false;
                needReverse = false;
                break;
            case STYLE_LATTICE:
                needChangeAlpha = true;
                needReverse = false;
                mPathIntervals = new float[]{mStrokeWidth, mScreenHeight / 80};
                break;
            case STYLE_OUTSPREAD_BOTTOM_TO_TOP:
                needChangeAlpha = false;
                needReverse = false;
                break;
        }
        if (needReverse) {
            mValueAnimator.setRepeatMode(ValueAnimator.REVERSE);
        } else {
            mValueAnimator.setRepeatMode(ValueAnimator.RESTART);
        }
        int mixedColorNum = mMixedColorArr.length;
        if (mixedColorNum > 0) {
            int duration = mDuration / DEFAULT_REPEAT_COUNT;
            if(isAlwaysOnAble) {
                mValueAnimator.setRepeatCount(ValueAnimator.INFINITE);
            } else {
                mValueAnimator.setRepeatCount(DEFAULT_REPEAT_COUNT);
            }
            mValueAnimator.setDuration(duration);
        }

        configPath();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isAnimatorRunning) {
            drawEdgeLine(canvas);
        }
    }

    private void drawEdgeLine(Canvas canvas) {
        mScreenWidth = ViewUtil.getScreenWidth(mContext);
        mScreenHeight = ViewUtil.getScreenHeight(mContext);
        configPath();
        switch (mStyle) {
            case STYLE_WIND:
                drawWindStyle(canvas);
                break;
            case STYLE_FADE_IN_OUT:
                drawFadeInOutStyle(canvas);
                break;

            case STYLE_OUTSPREAD_MIDDLE_OUT:
                drawMiddleOutStyle(canvas);
                break;
            case STYLE_OUTSPREAD_BOTTOM_TO_TOP:
                drawBottomToTopStyle(canvas);
                break;
            case STYLE_LATTICE:
                drawLatticeStyle(canvas);
                break;

        }
    }


    private void drawBottomToTopStyle(Canvas canvas) {
        float bottomMiddleP = 0.5f;
        float distance = mScreenHeight;
        float leftStart = mPathLength * bottomMiddleP - mPathLength * mProgress;
        float leftEnd = leftStart + distance * mProgress;
        Path leftPath = new Path();
        mPathMeasure.getSegment(leftStart, leftEnd, leftPath, true);

        float rightStart = mPathLength * bottomMiddleP + mPathLength * mProgress - distance * mProgress;
        float rightEnd = rightStart + distance * mProgress;
        Path rightPath = new Path();

        mPathMeasure.getSegment(rightStart, rightEnd, rightPath, true);

        canvas.save();
        canvas.drawPath(leftPath, mMixedPaint);
        canvas.drawPath(rightPath, mMixedPaint);
        canvas.restore();
    }

    private void drawLatticeStyle(Canvas canvas) {
        int step = mCurrentRepeatCount % 3;
        mPhase = (int) (step * mPathIntervals[1] / 3);

        mPrimaryPaint.setColor(mMixedColorArr[step]);
        mPrimaryPaint.setAlpha((int) (mProgress * 255));
        mPathEffect = new DashPathEffect(mPathIntervals, mPhase);
        mPrimaryPaint.setPathEffect(mPathEffect);
        canvas.drawPath(mPath, mPrimaryPaint);
    }


    private void drawMiddleOutStyle(Canvas canvas) {
        Path leftMiddlePath = new Path();
        float leftReferencePro = 0.25f;

        Path rightMiddlePath = new Path();
        float rightReferencePro = 0.75f;
        float distance = mScreenHeight >> 2;
        float offsetProcess = distance / mPathLength;
        float range = offsetProcess;
        if (mProgress < range) {
            float leftStartPro = leftReferencePro - mProgress;
            float leftEndPro = leftReferencePro;
            float rightStartPro = rightReferencePro - mProgress;
            float rightEndPro = rightReferencePro;
            mPathMeasure.getSegment(leftStartPro * mPathLength, leftEndPro * mPathLength, leftMiddlePath, true);
            canvas.drawPath(leftMiddlePath, mMixedPaint);
            mPathMeasure.getSegment(rightStartPro * mPathLength, rightEndPro * mPathLength, rightMiddlePath, true);
            canvas.drawPath(rightMiddlePath, mMixedPaint);

        } else {

            Path rightCursorPath = new Path();
            float leftCursorStartPro = leftReferencePro - mProgress;


            float rightCursorStartPro = rightReferencePro - mProgress;
            float rightPosition = rightCursorStartPro * mPathLength;

            if (leftCursorStartPro >= -offsetProcess) {
                Path leftCursorPath = new Path();
                float leftPosition = leftCursorStartPro * mPathLength;
                mPathMeasure.getSegment(leftPosition, leftPosition + distance, leftCursorPath, true);
                canvas.drawPath(leftCursorPath, mMixedPaint);
            }
            if (leftCursorStartPro < 0) {
                Path replenishPath = new Path();
                float replenishPro = 1.0f - Math.abs(leftCursorStartPro);
                float repStartPosition;
                float repEndPosition;
                // 为什么要0.5呢，因为走到一半要缩小
                if (replenishPro > 0.5) {
                    repStartPosition = replenishPro * mPathLength;
                    repEndPosition = repStartPosition + distance;
                } else {
                    repStartPosition = 0.5f * mPathLength;
                    repEndPosition = repStartPosition + mPathLength * (offsetProcess + rightCursorStartPro);
                }
                mPathMeasure.getSegment(repStartPosition, repEndPosition, replenishPath, true);
                canvas.drawPath(replenishPath, mMixedPaint);
            }

            mPathMeasure.getSegment(rightPosition, rightPosition + distance, rightCursorPath, true);
            canvas.drawPath(rightCursorPath, mMixedPaint);

        }
        // 中间固定线

    }

    private void drawFadeInOutStyle(Canvas canvas) {
        mTranslationX = mProgress * mScreenWidth;
        mTranslationY = mProgress * mScreenHeight;
        mGradientMatrix.setTranslate(mTranslationX, mTranslationY);
        mColorShader.setLocalMatrix(mGradientMatrix);
        canvas.drawPath(mPath, mMixedPaint);
    }

    private void drawWindStyle(Canvas canvas) {
        mDst.reset();
        float distance = mScreenHeight / 3;
        float start = mPathLength * mProgress;
        float end = (float) (mPathLength * mProgress + distance * Math.pow((1.5f - Math.abs(mProgress - 0.5f)), 3.2));
        if (end >= mPathLength) {
            float offsetProgress = (end - mPathLength) / mPathLength;
            Path path = new Path();
            mPathMeasure.getSegment(0, mPathLength * offsetProgress, path, true);
            canvas.drawPath(path, mMixedPaint);
        }
        mPathMeasure.getSegment(start, end, mDst, true);
        canvas.drawPath(mDst, mMixedPaint);
    }

    private void configPath() {
        mPath.reset();
        mDst.reset();
        float offset = 2 * mStrokeWidth / 5;
        mScreenRectF = new RectF(offset, offset, mScreenWidth - offset, mScreenHeight - offset);
        Log.e(TAG, "config : " + isCornersShown() + "mCornerSize =:" + mCornerSize);
        float edge = mStrokeWidth / 2;
        if (isCornersShown()) {
            mPath.moveTo(mScreenWidth / 2 - edge, edge);
            mPath.lineTo(mCornerSize, edge);
            mPath.arcTo(new RectF(edge, edge, (mCornerSize * 2.0f + edge),
                    (mCornerSize * 2.0f + edge)), 270, -90.0f, false);
            mPath.lineTo(edge, mScreenHeight - mCornerSize - edge);
            mPath.arcTo(new RectF(edge, (mScreenHeight - 2 * mCornerSize - edge),
                    (mCornerSize * 2.0f + edge), (mScreenHeight - edge)), 180.0f, -90.0f, false);
            mPath.lineTo(mScreenWidth - mCornerSize - edge, mScreenHeight - edge);
            mPath.arcTo(new RectF((mScreenWidth - 2 * mCornerSize - edge), (mScreenHeight - 2 * mCornerSize - edge),
                    (mScreenWidth - edge), (mScreenHeight - edge)), 90.0f, -90.0f, false);
            mPath.lineTo(mScreenWidth - edge, mCornerSize + edge);
            mPath.arcTo(new RectF((mScreenWidth - 2 * mCornerSize - edge), edge,
                    (mScreenWidth - edge), (mCornerSize * 2.0f + edge)), 0.0f, -90.0f, false);
            mPath.lineTo(mScreenWidth / 2 - edge, edge);
            mPath.close();
        } else {
            mPath.moveTo(mScreenWidth / 2 - edge, edge);
            mPath.lineTo(edge, edge);
            mPath.lineTo(edge, mScreenHeight - edge);
            mPath.lineTo(mScreenWidth - edge, mScreenHeight - edge);
            mPath.lineTo(mScreenWidth - edge, edge);
            mPath.close();
        }
        mPathMeasure.setPath(mPath, false);
        mPathLength = mPathMeasure.getLength();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mScreenWidth, mScreenHeight);
        if(mConfigurationChangeListener != null){
            mConfigurationChangeListener.onScreenConfigurationChanged();
        }
    }

    public void setAnimationStateListener(AnimationStateListener stateListener) {
        mStateListener = stateListener;
    }

    public void cancelAnimator() {
        if (mValueAnimator.isRunning()) {
            mValueAnimator.cancel();
        }
    }

    public void startAnimator() {
        Log.e(TAG,"startAnimator :: startAnimator =:");
        postDelayed(new Runnable() {
            @Override
            public void run() {
                isAnimatorRunning = true;
                if (needChangeAlpha) {
                    mPrimaryPaint.setAlpha(0);
                    mMixedPaint.setAlpha(0);
                } else {
                    mPrimaryPaint.setAlpha(255);
                    mMixedPaint.setAlpha(255);
                }
                animateShow();
            }
        },500);

    }
    public boolean isAnimatorRunning(){
        return isAnimatorRunning;
    }
    private void animateShow() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "alpha", 0, 1.0f);
        animator.setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mValueAnimator != null && !mValueAnimator.isRunning()) {
                    mValueAnimator.start();
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                isAnimatorRunning = true;
                if (mStateListener != null) {
                    mStateListener.onAnimationStart();
                }
            }
        });
        animator.start();
    }
    private void hide(boolean immediately){
        Log.e(TAG,"hide immediately = :"  + immediately + ",isAnimatorRunning =:" + isAnimatorRunning);
        if(!isAnimatorRunning){
            return;
        }
        if(immediately){
            isAnimatorRunning = false;
            if (mStateListener != null && !isAlwaysOnAble) {
                mStateListener.onAnimationEnd();
            }
        } else {
            animateHide();
        }
    }
    private void animateHide() {

        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "alpha", 1.0f, 0.0f);
        animator.setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimatorRunning = false;
                if (mStateListener != null && !isAlwaysOnAble) {
                    mStateListener.onAnimationEnd();
                }
            }
        });
        animator.start();
    }

    private boolean isCornersShown() {
        return SettingsDataKeeper.getSettingsBoolean(mContext, SettingsDataKeeper.CORNER_ENABLE);
    }

    private int getCornerSize() {
        return SettingsDataKeeper.getSettingsInt(mContext, SettingsDataKeeper.CORNER_SIZE);
    }

    public void stopAnimator() {
        setVisibility(INVISIBLE);
    }

    public interface AnimationStateListener {
        void onAnimationStart();

        void onAnimationRunning(float progress);

        void onAnimationEnd();
    }
    public interface OnScreenConfigurationChangeListener{
        void onScreenConfigurationChanged();
    }
}
