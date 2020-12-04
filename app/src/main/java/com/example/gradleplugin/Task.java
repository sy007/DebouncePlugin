package com.example.gradleplugin;

import android.view.View;

public class Task {

    public static void init(View view, View.OnClickListener click) {
        view.findViewById(R.id.btn_click_7).setOnClickListener(v -> {
            if (click != null) {
                //回调前干一些事情
                click.onClick(v);
                //回调后干一些事情
            }
        });
    }
}
