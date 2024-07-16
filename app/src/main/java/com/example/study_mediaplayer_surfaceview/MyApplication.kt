package com.example.study_mediaplayer_surfaceview

import android.app.Application
import com.didichuxing.doraemonkit.DoKit

/**
 * @Description:
 * @author zouji
 * @date 2024/7/7
 */
class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        DoKit.Builder(this)
            .productId("需要使用平台功能的话，需要到dokit.cn平台申请id")
            .build()
    }
}