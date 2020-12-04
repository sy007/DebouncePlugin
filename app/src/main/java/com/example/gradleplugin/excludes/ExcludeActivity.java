package com.example.gradleplugin.excludes;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gradleplugin.LogUtil;
import com.example.gradleplugin.R;

public class ExcludeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exclude);
        findViewById(R.id.btn_exclude).setOnClickListener(v -> {
            LogUtil.d("click exclude btn");
        });
    }
}