package com.example.gradleplugin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.OnItemClick;

public class ListViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);
        ButterKnife.bind(this);
        ListView lv = findViewById(R.id.lv_list);
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("pos", i);
            map.put("item", "item" + i);
            list.add(map);
        }
        lv.setAdapter(new SimpleAdapter(this, list, android.R.layout.simple_list_item_2,
                new String[]{"pos", "text"}
                , new int[]{android.R.id.text1, android.R.id.text2}));
    }

    @OnItemClick(R.id.lv_list)
    void onItemClick(int position) {
        LogUtil.d("ListView.onItemClick click" + position);
    }
}