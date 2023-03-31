package com.example.gradleplugin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class TestKotlinActivity : AppCompatActivity() {

    private var onClick: (view: View) -> Unit = {
        LogUtil.d("Kotlin高阶函数点击事件")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        findViewById<View>(R.id.btn_click_1).setOnClickListener {
            LogUtil.d("Kotlin点击事件1")
        }
        findViewById<View>(R.id.btn_click_2).setOnClickListener {
            LogUtil.d("Kotlin点击事件2")
        }
        findViewById<View>(R.id.btn_click_3).setOnClickListener(onClick)
        findViewById<View>(R.id.btn_click_4).setOnClickListener(this::instanceReferenceClick)
    }

    private fun instanceReferenceClick(view: View) {
        LogUtil.d("Kotlin Lambda方法引用")
    }
}