package com.example.gradleplugin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gradleplugin.excludes.ExcludeActivity;
import com.sunyuan.debounce.lib.ClickDeBounce;
import com.sunyuan.debounce.lib.IgnoreClickDeBounce;
import com.sunyuan.simple.lib.LibraryActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int instanceField = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        findViewById(R.id.cb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogUtil.d("CheckBox点击事件=>isCheck:" + ((CheckBox) view).isChecked());
            }
        });
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
        Task.init(findViewById(R.id.ll_rootView), findViewById(R.id.btn_click_8), new View.OnClickListener() {
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

        findViewById(R.id.btn_rv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RecyclerViewActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_jump_to_lib).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LibraryActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_jump_to_default_method).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DefaultMethodActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_jump_to_excludes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExcludeActivity.class);
                startActivity(intent);
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