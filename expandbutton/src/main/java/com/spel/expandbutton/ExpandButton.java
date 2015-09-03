package com.spel.expandbutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;


public class ExpandButton extends View {

    private static final long STEP_DURATION = 5;

    private static float SELF_VERTICAL_PADDING = 0.35f;
    private static float SELF_HORIZONTAL_PADDING = 0.2f;

    public static final int ANIMATION_TYPE_ROTATE = 0;
    public static final int ANIMATION_TYPE_MORPH = 1;

    public static final int ROTATE_TYPE_CLOCKWISE = 1;
    public static final int ROTATE_TYPE_ANTICLOCKWISE = -1;


    private int mAnimationType = ANIMATION_TYPE_MORPH;
    private int mRotateType = ROTATE_TYPE_CLOCKWISE;

    private float mArrowLineWidth = 8f;
    private int mArrowColor = Color.WHITE;

    private float mArrowStateProgress = 1f;//progress of arrow spinning 0 - down, 1 - top.
    private float mProgressStep = 0.01f;
    private long mAnimationDuration = 150;//ms
    private boolean mExpanded = false;

    private OnStateChangedListener mListener;

    private Paint mArrowPaint;
    private RectF mDrawableRect;
    private Path mArrowPath;
    private Animator mAnimator;

    private PointF mLeftPoint;
    private PointF mCenterPoint;
    private PointF mRightPoint;

    public ExpandButton(Context context) {
        super(context);
        init();
    }

    public ExpandButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ExpandButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(){
        mDrawableRect = new RectF();
        mArrowPath = new Path();

        mLeftPoint = new PointF();
        mCenterPoint = new PointF();
        mRightPoint = new PointF();

        mArrowPaint = new Paint();
        mArrowPaint.setStyle(Paint.Style.STROKE);
        mArrowPaint.setAntiAlias(true);

        mAnimator = new Animator();

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeState(true);
            }
        });

        setBackgroundResource(R.drawable.button_background);
    }

    private void init(AttributeSet attrs){
        init();

        TypedArray a = getContext().obtainStyledAttributes(attrs,R.styleable.ExpandCollapseButton);

        mAnimationType = a.getInt(R.styleable.ExpandCollapseButton_animation_style, ANIMATION_TYPE_MORPH);
        mAnimationDuration = a.getInt(R.styleable.ExpandCollapseButton_animation_duration, 150);
        mArrowLineWidth = a.getDimensionPixelOffset(R.styleable.ExpandCollapseButton_arrow_width, 8);
        mArrowColor = a.getColor(R.styleable.ExpandCollapseButton_arrow_color, Color.WHITE);
        mRotateType = a.getInt(R.styleable.ExpandCollapseButton_rotate_type, ROTATE_TYPE_CLOCKWISE);

        a.recycle();
    }

    public void setAnimationType(int type){
        mAnimationType = type;
    }

    public void setAnimationDuration(long duration){
        mAnimationDuration = duration;
    }

    public void setArrowColor(int color) {
        mArrowColor = color;
    }

    public void setArrowWidth(float width){
        mArrowLineWidth = width;
    }

    public boolean isExpanded(){
        return mExpanded;
    }

    public void setOnStateChangedListener(OnStateChangedListener listener){
        mListener = listener;
    }

    public void setRotateType(int type){
        if(type == 1 || type == -1){
            mRotateType = type;
        } else {
            throw new IllegalArgumentException("Unexpected rotate type: " + type);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //init rect available for draw.
        mDrawableRect.left = getPaddingLeft();
        mDrawableRect.top = getPaddingTop();
        mDrawableRect.right = canvas.getWidth() - getPaddingRight();
        mDrawableRect.bottom = canvas.getHeight() - getPaddingBottom();

        //add self padding
        float width = mDrawableRect.width();
        float height = mDrawableRect.height();
        mDrawableRect.left += width * SELF_HORIZONTAL_PADDING;
        mDrawableRect.top += height * SELF_VERTICAL_PADDING;
        mDrawableRect.right -= width * SELF_HORIZONTAL_PADDING;
        mDrawableRect.bottom -= height * SELF_VERTICAL_PADDING;

        //init arrow path.
        preparePoints();
        mArrowPath = new Path();
        mArrowPath.moveTo(mLeftPoint.x, mLeftPoint.y);
        mArrowPath.lineTo(mCenterPoint.x, mCenterPoint.y);
        mArrowPath.lineTo(mRightPoint.x, mRightPoint.y);

        //init arrow paint.
        mArrowPaint.setColor(mArrowColor);
        mArrowPaint.setStrokeWidth(mArrowLineWidth);

        //draw.
        canvas.drawPath(mArrowPath, mArrowPaint);

    }

    public void changeState(boolean animated){
        mExpanded = !mExpanded;
        if(mListener != null)
            mListener.onStateChanged(mExpanded);

        if(animated){
            mProgressStep = (float) STEP_DURATION / (float) mAnimationDuration;
            mProgressStep *= mExpanded ? -1 : 1;

            mAnimator.run();
        } else {
            mArrowStateProgress = mExpanded ? 0 : 1;
            invalidate();
        }
    }

    private void preparePoints(){
        switch (mAnimationType){
            case ANIMATION_TYPE_MORPH:
                preparePointsMorph();
                break;
            case ANIMATION_TYPE_ROTATE:
                preparePointsRotate();
                break;
            default:
                throw new IllegalArgumentException("Unexpected animation type: " + mAnimationType);
        }
    }

    private void preparePointsMorph(){
        float extremeY = mDrawableRect.bottom - (mDrawableRect.bottom - mDrawableRect.top)* mArrowStateProgress;

        mLeftPoint.x = mDrawableRect.left;
        mLeftPoint.y = extremeY;

        mCenterPoint.x = mDrawableRect.left + (mDrawableRect.right - mDrawableRect.left) / 2;
        mCenterPoint.y = mDrawableRect.top + (mDrawableRect.bottom - mDrawableRect.top) * mArrowStateProgress;

        mRightPoint.x = mDrawableRect.right;
        mRightPoint.y = extremeY;
    }

    private void preparePointsRotate(){
        mLeftPoint.x = mDrawableRect.left;
        mLeftPoint.y = mDrawableRect.bottom;

        mCenterPoint.x = mDrawableRect.left + mDrawableRect.width() / 2;
        mCenterPoint.y = mDrawableRect.top;

        mRightPoint.x = mDrawableRect.right;
        mRightPoint.y = mDrawableRect.bottom;

        PointF center = new PointF(mDrawableRect.left + mDrawableRect.width() / 2, mDrawableRect.top + mDrawableRect.height() / 2);

        float angle = (mExpanded ? -180 : 180) * mArrowStateProgress * mRotateType;

        rotatePoint(mLeftPoint, center, angle);
        rotatePoint(mCenterPoint, center, angle);
        rotatePoint(mRightPoint, center, angle);
    }

    private void rotatePoint(PointF point, PointF center, float angle){

        float x = point.x - center.x;
        float y = point.y - center.y;

        float cos = (float) Math.cos(Math.toRadians(angle));
        float sin = (float) Math.sin(Math.toRadians(angle));

        float resultX = x * cos - y * sin;
        float resultY = x * sin + y * cos;

        point.x = resultX + center.x;
        point.y = resultY + center.y;

    }


    class Animator implements Runnable{

        @Override
        public void run() {
            mArrowStateProgress += mProgressStep;
            invalidate();
            if(mArrowStateProgress < 0){
                mArrowStateProgress = 0;
            } else if(mArrowStateProgress > 1){
                mArrowStateProgress = 1;
            } else{
                postDelayed(this, STEP_DURATION);
            }
        }
    }

    public interface OnStateChangedListener{
        void onStateChanged(boolean expanded);
    }

}
