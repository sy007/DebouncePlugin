package com.example.gradleplugin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);
        ListView lv = findViewById(R.id.lv_list);


        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LogUtil.d("ListView.onItemClick click" + position);

            }
        });
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
}