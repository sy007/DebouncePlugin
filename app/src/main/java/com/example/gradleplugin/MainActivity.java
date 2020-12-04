package com.example.gradleplugin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = MainActivity.class.getSimpleName();
    private int instanceField = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_click_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.d("匿名内部类点击事件");
            }
        });
        findViewById(R.id.btn_click_2).setOnClickListener(v -> LogUtil.d("Lambda点击事件(non-instance-capturing lambdas)"));
        findViewById(R.id.btn_click_3).setOnClickListener(v -> LogUtil.d("Lambda点击事件(instance-capturing lambdas)" + instanceField));
        findViewById(R.id.btn_click_4).setOnClickListener(this);
        findViewById(R.id.btn_click_5).setOnClickListener(new InnearClickListener());
        findViewById(R.id.btn_click_6).setOnClickListener(new InnearStaticClickListener());
        Task.init(findViewById(R.id.ll_rootView), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.d("事件回调了");
            }
        });
        findViewById(R.id.btn_jump_to_list_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ListViewActivity.class);
                startActivity(intent);
            }
        });
    }


    private class InnearClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            LogUtil.d("内部类点击事件Hook");
        }
    }

    private static class InnearStaticClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            LogUtil.d("静态内部类点击事件");
        }
    }

    @Override
    public void onClick(View v) {
        LogUtil.d("implements View.OnClickListener点击事件");
    }
}