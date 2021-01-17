package com.example.gradleplugin.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.gradleplugin.LogUtil;

/**
 * author : Six
 * date   : 2021/1/12 001222:02
 * desc   :
 * version: 1.0
 */
public class CustomViewImpl extends AbsCustomView {

    public CustomViewImpl(Context context) {
        super(context);
        init();
    }

    public CustomViewImpl(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomViewImpl(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LogUtil.d("CustomViewImpl->init()");
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        LogUtil.d("CustomViewImpl->onClick()");
    }

    @Override
    protected void doAction() {
        LogUtil.d("CustomViewImpl->doAction()");
    }
}
