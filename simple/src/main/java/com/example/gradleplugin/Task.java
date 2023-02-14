package com.example.gradleplugin;

import android.view.View;
import android.widget.Button;

public class Task {

    public static void init(View view, Button abc, View.OnClickListener click) {
        view.findViewById(R.id.btn_click_9).setOnClickListener(v -> {
            if (click != null) {
                System.out.println(abc.toString());
                //回调前干一些事情
                click.onClick(v);
                //回调后干一些事情
            }
        });
    }
}
