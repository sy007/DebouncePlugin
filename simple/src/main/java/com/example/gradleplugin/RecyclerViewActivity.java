package com.example.gradleplugin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecyclerViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);
        List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            HashMap<String, String> map = new HashMap<>();
            map.put("pos", String.valueOf(i));
            map.put("item", "item" + i);
            list.add(map);
        }

        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        BaseQuickAdapter<Map<String, String>, BaseViewHolder> baseQuickAdapter = new BaseQuickAdapter<>(android.R.layout.simple_list_item_2, list) {
            @Override
            protected void convert(@NonNull BaseViewHolder baseViewHolder, Map<String, String> map) {
                baseViewHolder.setText(android.R.id.text1, map.get("pos"));
                baseViewHolder.setText(android.R.id.text2, map.get("item"));
            }
        };
        baseQuickAdapter.setOnItemClickListener((adapter, view, position) -> LogUtil.d("RecyclerView.onItemClick click" + position));
        rv.setAdapter(baseQuickAdapter);
    }
}