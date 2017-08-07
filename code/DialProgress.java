package com.pcl.baierpad.weight;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.pcl.baierpad.BuildConfig;
import com.pcl.baierpad.R;
import com.pcl.baierpad.utils.Constant;
import com.pcl.baierpad.utils.MiscUtil;


/**
 * 带有刻度的圆形进度条
 * Created by littlejie on 2017/2/26.
 */

public class DialProgress extends View {

    private static final String TAG = DialProgress.class.getSimpleName();
    private Context mContext;

    //圆心坐标
    private Point mCenterPoint;
    private float mRadius;
    private float mTextOffsetPercentInRadius;

    private boolean antiAlias;
    //绘制提示
    private TextPaint mHintPaint;
    private CharSequence mHint;
    private int mHintColor;
    private float mHintSize;
    private float mHintOffset;

    //绘制数值
    private Paint mValuePaint;
    private int mValueColor;
    private float mMaxValue;
    private float mValue;
    private float mValueSize;
    private float mValueOffset;
    private String mPrecisionFormat;

    //绘制单位
    private Paint mUnitPaint;
    private float mUnitSize;
    private int mUnitColor;
    private float mUnitOffset;
    private CharSequence mUnit;
    //前景圆弧
    private Paint mArcPaint;
    private float mArcWidth;
    private int mDialIntervalDegree;
    private float mStartAngle, mSweepAngle;
    private RectF mRectF;
    //渐变
    private int[] mGradientColors = {Color.GREEN, Color.YELLOW, Color.RED};
    //当前进度，[0.0f,1.0f]
    private float mPercent;
    //动画时间
    private long mAnimTime;
    //属性动画
    private ValueAnimator mAnimator;

    //背景圆弧
    private Paint mBgArcPaint;
    private int mBgArcColor;

    //刻度线颜色
    private Paint mDialPaint;
    private float mDialWidth;
    private int mDialColor;

    private int mDefaultSize;

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public DialProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        mDefaultSize = MiscUtil.dipToPx(context, Constant.DEFAULT_SIZE);
        mRectF = new RectF();
        mCenterPoint = new Point();
        initConfig(context, attrs);
        initPaint();
        setValue(mValue);
    }

    private void initConfig(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DialProgress);

        antiAlias = typedArray.getBoolean(R.styleable.DialProgress_antiAlias, true);
        mMaxValue = typedArray.getFloat(R.styleable.DialProgress_maxValue, Constant.DEFAULT_MAX_VALUE);
        mValue = typedArray.getFloat(R.styleable.DialProgress_value, Constant.DEFAULT_VALUE);
        mValueSize = typedArray.getDimension(R.styleable.DialProgress_valueSize, Constant.DEFAULT_VALUE_SIZE);
        mValueColor = typedArray.getColor(R.styleable.DialProgress_valueColor, Color.BLACK);
        mDialIntervalDegree = typedArray.getInt(R.styleable.DialProgress_dialIntervalDegree, 10);
        int precision = typedArray.getInt(R.styleable.DialProgress_precision, 0);
        mPrecisionFormat = MiscUtil.getPrecisionFormat(precision);

        mUnit = typedArray.getString(R.styleable.DialProgress_unit);
        mUnitColor = typedArray.getColor(R.styleable.DialProgress_unitColor, Color.BLACK);
        mUnitSize = typedArray.getDimension(R.styleable.DialProgress_unitSize, Constant.DEFAULT_UNIT_SIZE);

        mHint = typedArray.getString(R.styleable.DialProgress_hint);
        mHintColor = typedArray.getColor(R.styleable.DialProgress_hintColor, Color.BLACK);
        mHintSize = typedArray.getDimension(R.styleable.DialProgress_hintSize, Constant.DEFAULT_HINT_SIZE);

        mArcWidth = typedArray.getDimension(R.styleable.DialProgress_arcWidth, Constant.DEFAULT_ARC_WIDTH);

        mStartAngle = typedArray.getFloat(R.styleable.DialProgress_startAngle, Constant.DEFAULT_START_ANGLE);
        mSweepAngle = typedArray.getFloat(R.styleable.DialProgress_sweepAngle, Constant.DEFAULT_SWEEP_ANGLE);

        mAnimTime = typedArray.getInt(R.styleable.DialProgress_animTime, Constant.DEFAULT_ANIM_TIME);

        mBgArcColor = typedArray.getColor(R.styleable.DialProgress_bgArcColor, Color.GRAY);
        mDialWidth = typedArray.getDimension(R.styleable.DialProgress_dialWidth, 2);
        mDialColor = typedArray.getColor(R.styleable.DialProgress_dialColor, Color.WHITE);

        mTextOffsetPercentInRadius = typedArray.getFloat(R.styleable.DialProgress_textOffsetPercentInRadius, 0.33f);

        int gradientArcColors = typedArray.getResourceId(R.styleable.DialProgress_arcColors, 0);
        if (gradientArcColors != 0) {
            try {
                int[] gradientColors = getResources().getIntArray(gradientArcColors);
                if (gradientColors.length == 0) {
                    int color = getResources().getColor(gradientArcColors);
                    mGradientColors = new int[2];
                    mGradientColors[0] = color;
                    mGradientColors[1] = color;
                } else if (gradientColors.length == 1) {
                    mGradientColors = new int[2];
                    mGradientColors[0] = gradientColors[0];
                    mGradientColors[1] = gradientColors[0];
                } else {
                    mGradientColors = gradientColors;
                }
            } catch (Resources.NotFoundException e) {
                throw new Resources.NotFoundException("the give resource not found.");
            }
        }
        typedArray.recycle();
    }

    private void initPaint() {
        mHintPaint = new TextPaint();
        // 设置抗锯齿,会消耗较大资源，绘制图形速度会变慢。
        mHintPaint.setAntiAlias(antiAlias);
        // 设置绘制文字大小
        mHintPaint.setTextSize(mHintSize);
        // 设置画笔颜色
        mHintPaint.setColor(mHintColor);
        // 从中间向两边绘制，不需要再次计算文字
        mHintPaint.setTextAlign(Paint.Align.CENTER);

        mValuePaint = new Paint();
        mValuePaint.setAntiAlias(antiAlias);
        mValuePaint.setTextSize(mValueSize);
        mValuePaint.setColor(mValueColor);
        mValuePaint.setTypeface(Typeface.DEFAULT_BOLD);
        mValuePaint.setTextAlign(Paint.Align.CENTER);

        mUnitPaint = new Paint();
        mUnitPaint.setAntiAlias(antiAlias);
        mUnitPaint.setTextSize(mUnitSize);
        mUnitPaint.setColor(mUnitColor);
        mUnitPaint.setTextAlign(Paint.Align.CENTER);

        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(antiAlias);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mArcWidth);
        mArcPaint.setStrokeCap(Paint.Cap.BUTT);

        mBgArcPaint = new Paint();
        mBgArcPaint.setAntiAlias(antiAlias);
        mBgArcPaint.setStyle(Paint.Style.STROKE);
        mBgArcPaint.setStrokeWidth(mArcWidth);
        mBgArcPaint.setStrokeCap(Paint.Cap.BUTT);
        mBgArcPaint.setColor(mBgArcColor);

        mDialPaint = new Paint();
        mDialPaint.setAntiAlias(antiAlias);
        mDialPaint.setColor(mDialColor);
        mDialPaint.setStrokeWidth(mDialWidth);
    }

    /**
     * 更新圆弧画笔
     */
    private void updateArcPaint() {
        // 设置渐变
        // 渐变的颜色是360度，如果只显示270，那么则会缺失部分颜色
        SweepGradient sweepGradient = new SweepGradient(mCenterPoint.x, mCenterPoint.y, mGradientColors, null);
        mArcPaint.setShader(sweepGradient);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MiscUtil.measure(widthMeasureSpec, mDefaultSize),
                MiscUtil.measure(heightMeasureSpec, mDefaultSize));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: w = " + w + "; h = " + h + "; oldw = " + oldw + "; oldh = " + oldh);
        int minSize = Math.min(getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - 2 * (int) mArcWidth,
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - 2 * (int) mArcWidth);
        mRadius = minSize / 2;
        mCenterPoint.x = getMeasuredWidth() / 2;
        mCenterPoint.y = getMeasuredHeight() / 2;
        //绘制圆弧的边界
        mRectF.left = mCenterPoint.x - mRadius - mArcWidth / 2;
        mRectF.top = mCenterPoint.y - mRadius - mArcWidth / 2;
        mRectF.right = mCenterPoint.x + mRadius + mArcWidth / 2;
        mRectF.bottom = mCenterPoint.y + mRadius + mArcWidth / 2;

        mValueOffset = mCenterPoint.y + getBaselineOffsetFromY(mValuePaint);
        mHintOffset = mCenterPoint.y - mRadius * mTextOffsetPercentInRadius + getBaselineOffsetFromY(mHintPaint);
        mUnitOffset = mCenterPoint.y + mRadius * mTextOffsetPercentInRadius + getBaselineOffsetFromY(mUnitPaint);

        updateArcPaint();
        Log.d(TAG, "onMeasure: 控件大小 = " + "(" + getMeasuredWidth() + ", " + getMeasuredHeight() + ")"
                + ";圆心坐标 = " + mCenterPoint.toString()
                + ";圆半径 = " + mRadius
                + ";圆的外接矩形 = " + mRectF.toString());
    }

    private float getBaselineOffsetFromY(Paint paint) {
        return MiscUtil.measureTextHeight(paint) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawArc(canvas);
        drawDial(canvas);
        drawText(canvas);
    }
    private boolean isMove;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:

                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("onTouchEvent","downX："+event.getX()+"downY:"+event.getY());
                oneRegion(event.getX(),event.getY());
                break;
            case MotionEvent.ACTION_UP:

                break;
        }
        return true;
    }
    private void oneRegion(float x,float y){
        //在第一扇形区域
        //对边
        float across=Math.abs(y-mCenterPoint.y);
        //直角边
        float  hypotenuse=mCenterPoint.x-x;
        //已知两边求第三边：a*a+b*b=c*c
        double xeibian= Math.sqrt(across*across+hypotenuse*hypotenuse);
        Log.d("mArcWidth",mArcWidth+"");
        if (xeibian>=mRadius-40&&xeibian<=mRadius+mArcWidth){
            //sin=对边除以斜边
            double resultAsin=across/xeibian;
            double resultAngle=0;
            if (x<mCenterPoint.x&&y>mCenterPoint.y){
                 resultAngle=45-Math.toDegrees(Math.asin(resultAsin));
            }else if(x<mCenterPoint.x&&y<mCenterPoint.y){
                resultAngle=45+Math.toDegrees(Math.asin(resultAsin));
            }else if(x>mCenterPoint.x&&y<mCenterPoint.y){
                resultAngle=225-Math.toDegrees(Math.asin(resultAsin));
            }else if(x>mCenterPoint.x&&y>mCenterPoint.y){
                resultAngle=225+Math.toDegrees(Math.asin(resultAsin));
            }
            Log.d("onTouchEvent","--扫过角度"+resultAngle+"toDegrees:"+Math.toDegrees(Math.asin(resultAsin)));
            if(resultAngle>=0&&resultAngle<=270){
                float temporaryPercent=(float) (resultAngle/mSweepAngle);
                Log.d("temporaryPercent",temporaryPercent+"--"+mPercent);
                if (mPercent>temporaryPercent){
                    if (mPercent-temporaryPercent<0.02){
                        mPercent= (float) (resultAngle/mSweepAngle);
                        mValue=Math.round(mPercent*100);
                        invalidate();
                    }
                }else{
                    if (temporaryPercent-mPercent<0.02){
                        mPercent= (float) (resultAngle/mSweepAngle);
                        mValue=Math.round(mPercent*100);
                        invalidate();
                    }
                }

            }
        }
    }

    private void drawArc(Canvas canvas) {
        // 绘制背景圆弧
        // 从进度圆弧结束的地方开始重新绘制，优化性能
        float currentAngle = mSweepAngle * mPercent;
        canvas.save();
        canvas.rotate(mStartAngle, mCenterPoint.x, mCenterPoint.y);
        canvas.drawArc(mRectF, currentAngle, mSweepAngle - currentAngle, false, mBgArcPaint);
        Log.d("drawArc","currentAngle:"+currentAngle+"mSweepAngle - currentAngle:"+(mSweepAngle - currentAngle));
        // 第一个参数 oval 为 RectF 类型，即圆弧显示区域
        // startAngle 和 sweepAngle  均为 float 类型，分别表示圆弧起始角度和圆弧度数
        // 3点钟方向为0度，顺时针递增
        // 如果 startAngle < 0 或者 > 360,则相当于 startAngle % 360
        // useCenter:如果为True时，在绘制圆弧时将圆心包括在内，通常用来绘制扇形
        canvas.drawArc(mRectF, 0, currentAngle, false, mArcPaint);
        canvas.restore();
    }

    private void drawDial(Canvas canvas) {
        int total = (int) (mSweepAngle / mDialIntervalDegree);
        Log.d("totaltotal",mSweepAngle+"--"+mDialIntervalDegree);
        canvas.save();
        canvas.rotate(mStartAngle, mCenterPoint.x, mCenterPoint.y);
        for (int i = 0; i <= total; i++) {
            Log.d("totaltotal",total+"");
            if(i==0||i==9||i==18||i==27||i==36||i==45||i==54||i==63||i==72||i==81||i==90){
                Paint test = new Paint();
                test.setAntiAlias(antiAlias);
                test.setColor(mBgArcColor);
                test.setStrokeWidth(5);
                Log.d("totaltotal",(mCenterPoint.x+mRadius+","+mCenterPoint.y+","+(mCenterPoint.x + mRadius + mArcWidth)+","+mCenterPoint.y));
                canvas.drawLine(mCenterPoint.x + mRadius, mCenterPoint.y, mCenterPoint.x + mRadius + mArcWidth, mCenterPoint.y, mDialPaint);
                if (i==0){
                    canvas.drawLine(mCenterPoint.x+mRadius , mCenterPoint.y+4, mCenterPoint.x + mRadius + mArcWidth-100, mCenterPoint.y+4, test);
                }else if(i==90){
                    canvas.drawLine(mCenterPoint.x+mRadius , mCenterPoint.y-4,mCenterPoint.x + mRadius + mArcWidth-100, mCenterPoint.y-4, test);
                }else{
                    canvas.drawLine(mCenterPoint.x + mRadius, mCenterPoint.y, mCenterPoint.x + mRadius + mArcWidth-100, mCenterPoint.y, test);
                }
                canvas.rotate(mDialIntervalDegree, mCenterPoint.x, mCenterPoint.y);
            }else{
                Log.d("totaltotal",(mCenterPoint.x+mRadius+","+mCenterPoint.y+","+(mCenterPoint.x + mRadius + mArcWidth)+","+mCenterPoint.y));
                canvas.drawLine(mCenterPoint.x + mRadius, mCenterPoint.y, mCenterPoint.x + mRadius + mArcWidth, mCenterPoint.y, mDialPaint);
                canvas.rotate(mDialIntervalDegree, mCenterPoint.x, mCenterPoint.y);
            }
        }
        canvas.restore();
    }
    Paint painttest = new Paint();
    Paint proportion = new Paint();
    private void drawText(Canvas canvas) {

        painttest.setAntiAlias(antiAlias);
        painttest.setTextSize(30);
        painttest.setColor(mBgArcColor);
        painttest.setTypeface(Typeface.DEFAULT_BOLD);
        painttest.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("0", mCenterPoint.x-mRadius+120, mValueOffset+140, painttest);
        canvas.drawText("10", mCenterPoint.x-mRadius+70, mValueOffset+50, painttest);
        canvas.drawText("20", mCenterPoint.x-mRadius+80, mValueOffset-50, painttest);
        canvas.drawText("30", mCenterPoint.x-mRadius+110, mValueOffset-130, painttest);
        canvas.drawText("40", mCenterPoint.x-mRadius+170, mValueOffset-190, painttest);
        canvas.drawText("50", mCenterPoint.x-mRadius+265, mValueOffset-210, painttest);
        canvas.drawText("60", mCenterPoint.x-mRadius+360, mValueOffset-190, painttest);
        canvas.drawText("70", mCenterPoint.x-mRadius+430, mValueOffset-140, painttest);
        canvas.drawText("80", mCenterPoint.x-mRadius+470, mValueOffset-50, painttest);
        canvas.drawText("90", mCenterPoint.x-mRadius+465, mValueOffset+50, painttest);
        canvas.drawText("100", mCenterPoint.x-mRadius+400, mValueOffset+140, painttest);
        proportion.setAntiAlias(antiAlias);
        proportion.setTextSize(110);
        proportion.setColor(ContextCompat.getColor(mContext,R.color.theme_body_color));
        proportion.setTypeface(Typeface.DEFAULT_BOLD);
        proportion.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.format(mPrecisionFormat, mValue), mCenterPoint.x, mValueOffset, proportion);
        proportion.setTextSize(60);
        canvas.drawText("%", mCenterPoint.x+110, mValueOffset, proportion);
        if (mUnit != null) {
            mUnitPaint.setColor(ContextCompat.getColor(mContext,R.color.theme_body_color));
            canvas.drawText(mUnit.toString(), mCenterPoint.x, mUnitOffset, mUnitPaint);
        }
        if (mHint != null) {
            canvas.drawText(mHint.toString(), mCenterPoint.x, mHintOffset, mHintPaint);
        }


    }

    public float getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(float maxValue) {
        mMaxValue = maxValue;
    }

    /**
     * 设置当前值
     *
     * @param value
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void setValue(float value) {
        if (value > mMaxValue) {
            value = mMaxValue;
        }
        float start = mPercent;
        float end = value / mMaxValue;
        startAnimator(start, end, mAnimTime);
    }
    public float getCurrentValue(){
        return mValue;
    }
    public void setmAnimTime(long time){
        this.mAnimTime=time;
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private void startAnimator(float start, float end, long animTime) {
        mAnimator = ValueAnimator.ofFloat(start, end);
        mAnimator.setDuration(animTime);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPercent = (float) animation.getAnimatedValue();
                mValue = mPercent * mMaxValue;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onAnimationUpdate: percent = " + mPercent
                            + ";currentAngle = " + (mSweepAngle * mPercent)
                            + ";value = " + mValue);
                }
                invalidate();
            }
        });
        mAnimator.start();
    }

    public int[] getGradientColors() {
        return mGradientColors;
    }

    public void setGradientColors(int[] gradientColors) {
        mGradientColors = gradientColors;
        updateArcPaint();
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void reset() {
        startAnimator(mPercent, 0.0f, 1000L);
    }
}
