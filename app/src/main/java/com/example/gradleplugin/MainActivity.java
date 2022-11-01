package com.example.gradleplugin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;

import androidx.appcompat.app.AppCompatActivity;

import com.sunyuan.debounce.lib.ClickDeBounce;
import com.sunyuan.debounce.lib.IgnoreClickDeBounce;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int instanceField = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        findViewById(R.id.btn_click_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.d("匿名内部类点击事件");
            }
        });
        findViewById(R.id.btn_click_2).setOnClickListener(v -> LogUtil.d("Lambda点击事件(non-instance-capturing lambdas)"));
        findViewById(R.id.btn_click_3).setOnClickListener(v -> LogUtil.d("Lambda点击事件(instance-capturing lambdas)" + instanceField));
        findViewById(R.id.btn_click_4).setOnClickListener(this::instanceReferenceClick);
        findViewById(R.id.btn_click_5).setOnClickListener(MainActivity::staticReferenceClick);
        findViewById(R.id.btn_click_6).setOnClickListener(this);
        InnerClickListener innerClickListener = new InnerClickListener();
        findViewById(R.id.btn_click_7).setOnClickListener(innerClickListener);
        findViewById(R.id.btn_click_8).setOnClickListener(new InnerStaticClickListener());
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

        findViewById(R.id.btn_jump_to_kotlin_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TestKotlinActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.btn_ignore_click_debounce).setOnClickListener(new View.OnClickListener() {
            @IgnoreClickDeBounce
            @Override
            public void onClick(View v) {
                LogUtil.d("忽略点击防抖");
            }
        });
    }


    private static void staticReferenceClick(View view) {
        LogUtil.d("Lambda点击事件hook(静态方法引用)");
    }

    private void instanceReferenceClick(View view) {
        LogUtil.d("Lambda点击事件hook(实例方法引用)");
    }

    @OnClick(R.id.btn_butter_knife)
    void onClick() {
        LogUtil.d("ButterKnife设置onClick事件");
    }

    //测试任意方法防抖功能
    @ClickDeBounce
    public void test() {

    }

    public void testAsmOnJDK9() {
        String str = "hello world at" + System.currentTimeMillis();
        LogUtil.d(str);
    }

    /**
     * xml中设置的点击事件
     */
    @ClickDeBounce
    public void reflectOnClick(View view) {
        LogUtil.d("xml设置onClick事件");
    }

    private class InnerClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            LogUtil.d("内部类点击事件");
        }
    }

    private static class InnerStaticClickListener implements View.OnClickListener {

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