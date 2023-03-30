package com.example.gradleplugin;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

/**
 * @author sy007
 * @date 2023/03/29
 * @description
 */
public interface ClickAction extends View.OnClickListener {

    <V extends View> V findViewById(@IdRes int id);

    default void setOnClickListener(@IdRes int... ids) {
        setOnClickListener(this, ids);
    }

    default void setOnClickListener(View.OnClickListener listener, @IdRes int... ids) {
        for (int id : ids) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    default void setOnClickListener(View... views) {
        setOnClickListener(this, views);
    }

    default void setOnClickListener(View.OnClickListener listener, View... views) {
        for (View view : views) {
            view.setOnClickListener(listener);
        }
    }

    @Override
    default void onClick(@NonNull View view) {
        // 默认不实现，让子类实现
    }
}
