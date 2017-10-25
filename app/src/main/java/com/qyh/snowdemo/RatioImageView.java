package com.qyh.snowdemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.vise.log.ViseLog;
import com.vise.snowdemo.R;

/**
 * @author 邱永恒
 * @time 2017/10/19  16:43
 * @desc ${TODD}
 */

public class RatioImageView extends ImageView {

    private float mRatio;

    public RatioImageView(Context context) {
        super(context);
        ViseLog.d("RatioImageView(Context context)");
    }

    public RatioImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ViseLog.d("RatioImageView(Context context, AttributeSet attrs)");
        init(context, attrs);
    }

    public RatioImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViseLog.d("RatioImageView(Context context, AttributeSet attrs, int defStyleAttr)");
        //init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RatioImageView);
        mRatio = typedArray.getFloat(R.styleable.RatioImageView_ratio, 0);
        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 宽模式
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        // 宽大小
        int widthSize = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        // 高大小
        int heightSize;
        // 只有宽的值是精确的才对高做精确的比例校对
        if (widthMode == MeasureSpec.EXACTLY && mRatio > 0) {
            heightSize = (int) (widthSize / mRatio + 0.0f);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
