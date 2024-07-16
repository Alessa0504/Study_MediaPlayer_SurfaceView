package com.example.study_mediaplayer_surfaceview

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.study_mediaplayer_surfaceview.databinding.ActivityMainBinding
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import utils.Utils
import java.io.File
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        // 使用 ViewBinding 设置内容视图
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initPermission()
        initView()
    }

    private fun initView() {
        binding.btnPlay.setOnClickListener {
            btn04(it)
        }
    }

    private fun initPermission() {
        XXPermissions.with(this)
            // Request single permission
            .permission(Permission.MANAGE_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (!allGranted) {
                        Toast.makeText(
                            this@MainActivity,
                            "Some permissions were obtained successfully, but some permissions were not granted normally",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Acquired storage permission successfully", Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        Toast.makeText(
                            this@MainActivity,
                            "Authorization denied permanently, please grant storage permission manually",
                            Toast.LENGTH_SHORT
                        ).show()
                        // If it is permanently denied, jump to the application permission system settings page
                        XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to get storage permission", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

    private fun btn04(view: View?) {
        val name = "local_video.mp4"
        val path = externalCacheDir?.path
        val url_local = "$path/$name"
        //判断本地有没有这个文件
        val file = File(url_local)
        if (file.exists()) {
            binding.mnVideoplayer.playVideo(url_local, "本地视频播放")
        } else {
            Toast.makeText(this, "本地文件不存在,正在创建", Toast.LENGTH_SHORT).show()
            Utils.copy(this, name, path!!, name)
            binding.mnVideoplayer.playVideo(url_local, "本地视频播放")
        }
    }
}