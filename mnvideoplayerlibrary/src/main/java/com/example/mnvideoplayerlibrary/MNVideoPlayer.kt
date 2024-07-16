package com.example.mnvideoplayerlibrary

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnBufferingUpdateListener
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.example.mnvideoplayerlibrary.databinding.MnPlayerViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import utils.PlayerUtils

/**
 * @Description:
 * @author Jillian
 * @date 2024/7/2
 */
class MNVideoPlayer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    SurfaceHolder.Callback, OnCompletionListener,
    OnPreparedListener, MediaPlayer.OnErrorListener, OnBufferingUpdateListener {

    private val url1 =
        "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"

    private var videoTitle: String? = null
    private var videoPath: String? = null
    private var playerViewW = 0
    private var playerViewH = 0
    private var isPrepare: Boolean? = false
    private var videoPosition: Int? = 0

    //标记暂停和播放状态
    private var isPlaying = false
    private var binding: MnPlayerViewBinding? = null
    private var surfaceHolder: SurfaceHolder? = null

    private val updateInterval: Long = 1000 // 1秒
    private var updateJob: Job? = null

    init {
        binding = MnPlayerViewBinding.inflate(LayoutInflater.from(context), this, true)
        initAttrs(context, attrs)
        initSurfaceView()  //初始化SurfaceView
        //todo 注册监听网络、电量信息的广播
    }

    //自动播放
    private var autoPlay = true
    private var mediaPlayer: MediaPlayer? = null

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        // 获取自定义属性
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MNViderPlayer)
        // 遍历拿到自定义属性
        for (i in 0 until typedArray.indexCount) {
            val index = typedArray.getIndex(i)
            if (index == R.styleable.MNViderPlayer_mnVideo_autoPlay) {
                autoPlay = typedArray.getBoolean(R.styleable.MNViderPlayer_mnVideo_autoPlay, true)
            }
        }
        // 销毁
        typedArray.recycle()
    }

    private fun initSurfaceView() {
        // 得到SurfaceView容器，播放的内容就是显示在这个容器里面
        surfaceHolder = binding?.mnPlayerSurfaceView?.holder
        surfaceHolder?.setKeepScreenOn(true)
        // 设置SurfaceView的回调
        surfaceHolder?.addCallback(this)
    }

    // === SurfaceView ===
    override fun surfaceCreated(holder: SurfaceHolder) {
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC) // 设置 MediaPlayer 的音频流类型为音乐流类型
        mediaPlayer?.setDisplay(holder) // 添加到容器中
        //播放完成的监听
        mediaPlayer?.setOnCompletionListener(this)
        // 异步准备的一个监听函数，准备好了就调用里面的方法
        mediaPlayer?.setOnPreparedListener(this)
        //播放错误的监听
        mediaPlayer?.setOnErrorListener(this)
        mediaPlayer?.setOnBufferingUpdateListener(this)
        if (autoPlay) {   //自动播放网络视频
            playVideo(url1 ?: "", videoTitle ?: "")
        }
    }

    // 处理表面尺寸变化，更新绘图参数
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    // 当 SurfaceView 所在的窗口被销毁时, 可释放资源
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        //保存播放位置
        if (mediaPlayer != null) {
            mediaPlayer?.pause()
            binding?.mnIvPlayPause?.setImageResource(R.drawable.mn_player_play)
            videoPosition = mediaPlayer?.currentPosition
        }
    }
    // === SurfaceView ===


    // === MediaPlayer ===
    override fun onCompletion(mp: MediaPlayer?) {
        binding?.mnIvPlayPause?.setImageResource(R.drawable.mn_player_play)
        isPlaying = false
        videoPosition = 0
    }

    // 当 MediaPlayer 准备好进行播放时会调用这个方法
    // 这通常在调用 prepareAsync() 方法之后被触发，用于通知应用程序 MediaPlayer 已经完成了初始化和数据准备，可以开始播放了
    // 在此方法中，你可以调用 start() 方法开始播放音频或视频
    override fun onPrepared(mp: MediaPlayer?) {
        mediaPlayer?.start()
        binding?.mnIvPlayPause?.setImageResource(R.drawable.mn_player_pause)
        isPrepare = true
        isPlaying = true
        // 把得到的总长度和进度条的匹配
        binding?.mnSeekBar?.max = mediaPlayer?.duration ?: 0
        binding?.mnTvTime?.text = "${PlayerUtils.convertLongTimeToStr(mediaPlayer?.currentPosition?.toLong() ?: 0L)} / " +
                "${PlayerUtils.convertLongTimeToStr(mediaPlayer?.duration?.toLong() ?: 0L)}"
        startUpdating()
        //适配大小
        fitVideoSize()
        binding?.mnIvPlayPause?.setOnClickListener {
            mediaPlayer?.apply {
                if (this.isPlaying) {
                    pauseVideo()
                } else {
                    playVideo()
                }
            }
        }
    }

    private fun startUpdating() {
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                binding?.mnSeekBar?.progress = mediaPlayer?.currentPosition ?: 0
                binding?.mnTvTime?.text = "${PlayerUtils.convertLongTimeToStr(mediaPlayer?.currentPosition?.toLong() ?: 0L)} / " +
                        "${PlayerUtils.convertLongTimeToStr(mediaPlayer?.duration?.toLong() ?: 0L)}"
                // 延迟一段时间
                delay(updateInterval)
            }
        }
    }

    private fun fitVideoSize() {
        if (mediaPlayer == null) {
            return
        }
        if (playerViewW == 0) {
            playerViewW = width
            playerViewH = height
        }
        //适配视频的高度
        val videoWidth = mediaPlayer?.videoWidth
        val videoHeight = mediaPlayer?.videoHeight
        //父布局宽高
        val parentWidth = playerViewW
        val parentHeight = playerViewH
        //判断视频宽高和父布局的宽高
        val surfaceViewW: Int
        val surfaceViewH: Int
        if (videoWidth!!.toFloat() / videoHeight!!.toFloat() > parentWidth.toFloat() / parentHeight.toFloat()) {
            surfaceViewW = parentWidth
            surfaceViewH = videoHeight * surfaceViewW / videoWidth
        } else {
            surfaceViewH = parentHeight
            surfaceViewW = videoWidth * parentHeight / videoHeight
        }
        //改变surfaceView的大小,实际操作的是surfaceView的父布局LinearLayout
        val params: ViewGroup.LayoutParams = binding!!.mnPlayerSurfaceBg.layoutParams
        params.height = surfaceViewH
        params.width = surfaceViewW
        Log.e(">>>>>>>>", "surfaceViewW:$surfaceViewW,surfaceViewH:$surfaceViewH")
        binding?.mnPlayerSurfaceBg?.layoutParams = params
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
//        Log.i(TAG, "发生错误error:" + what);
        if (what != -38) {  //这个错误不管
            Toast.makeText(context, "加载异常！", Toast.LENGTH_SHORT).show();
        }
        return true
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
        if (mp == null) return
        //  缓冲更新seekBar
//        Log.i(TAG, "二级缓存onBufferingUpdate: " + percent);
        if (percent in 0..100) {
            val secondProgress = mp.duration * percent / 100
            binding?.mnSeekBar?.secondaryProgress = secondProgress
        }
    }
    // === MediaPlayer ===

    fun playVideo(url: String, title: String) {
        playVideo(url, title, videoPosition ?: 0)
    }

    private fun playVideo(url: String, title: String, position: Int) {
        //地址判空处理
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(
                context,
                context.getString(R.string.mnPlayerUrlEmptyHint),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        //todo 销毁ControllerView
        //赋值
        videoPath = url
        videoTitle = title
        videoPosition = position
        isPrepare = false
        //todo 权限&网络
        //重置MediaPlayer
        resetMediaPlayer()
    }

    private fun resetMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                mediaPlayer?.stop()
            }
            //重置mediaPlayer
            mediaPlayer?.reset()
            //添加播放路径
            mediaPlayer?.setDataSource(videoPath)
            //准备开始,异步准备，自动在子线程中
            mediaPlayer?.prepareAsync()   // !!会触发回调MediaPlayer的onPrepared方法
            //视频缩放模式
            mediaPlayer?.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
        } else {
            Toast.makeText(context, "播放器初始化失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseVideo() {
        mediaPlayer?.apply {
            this.pause()
            binding?.mnIvPlayPause?.setImageResource(R.drawable.mn_player_play)
            videoPosition = this.currentPosition
        }
    }

    private fun playVideo() {
        mediaPlayer?.apply {
            this.start()
            binding?.mnIvPlayPause?.setImageResource(R.drawable.mn_player_pause)
        }
    }
}