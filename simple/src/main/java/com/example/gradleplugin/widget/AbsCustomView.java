package com.example.gradleplugin.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.example.gradleplugin.LogUtil;

/**
 * author : Six
 * date   : 2021/1/12 001222:01
 * desc   : 主要测试抽象类onClick方法插入
 * version: 1.0
 */
public abstract class AbsCustomView extends FrameLayout implements View.OnClickListener {
    public AbsCustomView(Context context) {
        super(context);
        init();
    }

    public AbsCustomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AbsCustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnClickListener(this);
        LogUtil.d("AsbCustomView->init()");
    }

    @Override
    public void onClick(View v) {
        LogUtil.d("AsbCustomView->onClick()");
        doAction();
    }

    protected abstract void doAction();
}
