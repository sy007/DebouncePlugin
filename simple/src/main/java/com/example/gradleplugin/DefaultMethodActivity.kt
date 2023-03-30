package com.example.gradleplugin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity


class DefaultMethodActivity : AppCompatActivity(), ClickAction {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_interface)
        setOnClickListener(R.id.btn_test)
    }

    override fun onClick(view: View) {
        LogUtil.d("onClick=>${view.id}")
    }
}