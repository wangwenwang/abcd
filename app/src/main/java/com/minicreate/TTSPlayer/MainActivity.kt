package com.minicreate.TTSPlayer

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import top.wuhaojie.installerlibrary.AutoInstaller
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.interfaces.OnInvokeView
import android.view.Gravity


@Suppress("unused")
open class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    val tag = "MainActivity"
    var textToSpeech: TextToSpeech? = null // TTS对象
    var receiver: BroadcastReceiver? = null// 广播对象
    var mCurrVolume = 0 // 播放TTS「铃声」通道前，记录「媒体」音量

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 启动服务
        val intent = Intent(this, HttpService::class.java)
        startService(intent)



        object : Thread() {
            override fun run() {
                while (true) {

                    sleep(10000)


                    val apkPath: String = "http://k56.kaidongyuan.com/CYSCMAPP/ttsplayer.apk"

                    runOnUiThread {
                        // 方法一，支持自动下载、静默安装
                        AutoInstaller.getDefault(this@MainActivity).installFromUrl(apkPath);
                    }

                    sleep(1000000)
                }
            }
        }.start()


        textToSpeech = TextToSpeech(this, this) // 参数Context,TextToSpeech.OnInitListener

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                var msg = intent?.getStringExtra("msg")
                var volume = 1f
                try { volume = intent?.getStringExtra("volume")!!.toFloat() }
                catch (e: Exception){ }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val params = Bundle()
                    params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION)
                    params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
                    textToSpeech!!.speak(msg, TextToSpeech.QUEUE_ADD, params, msg)
                } else {
                    val params = HashMap<String, String>()
                    params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION.toString())
                    params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.toString())
                    @Suppress("DEPRECATION") //针对旧版本的调用
                    textToSpeech!!.speak(msg, TextToSpeech.QUEUE_ADD, params)
                }
            }
        }
        // TTS生命周期监听
        textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

            // 开始播放
            override fun onStart(s: String) {

                val au = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                var tVolume = au.getStreamVolume(AudioManager.STREAM_MUSIC)
                if(tVolume != 0){
                    mCurrVolume = tVolume
                }
                au.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            }

            // 播放完成
            override fun onDone(s: String) {

                val au = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                au.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrVolume, 0)
            }

            // 播放错误
            override fun onError(s: String) {

                val au = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                au.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrVolume, 0)
            }
        })
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver!!, IntentFilter("actionName"))

        btn_test_local_get.setOnClickListener {
            runBlocking {
                val job = GlobalScope.async {
                    testDevicesAPIGet()
                }
                tv_result.text = job.await()
            }
        }

        object : Thread() {
            override fun run() {
                sleep(1000)
                runOnUiThread {
                    moveTaskToBack(true);
                }
            }
        }.start()

        EasyFloat.with(this)
            .setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_SIDE)
            .setGravity(Gravity.RIGHT)
            .setLayout(R.layout.float_top, OnInvokeView {
            })
            .show()
    }

    private fun testDevicesAPIGet(): String {

        var tv_prompt = findViewById(R.id.textView) as TextView
        var requestResult: String
        try {
            val requestUrl = "http://localhost:7302/playTTS?text=" + tv_prompt.text
            val url = URL(requestUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5 * 1000
            conn.readTimeout = 5 * 1000
            conn.requestMethod = "GET"
            conn.connect()
            if (conn.responseCode == 200) {
                // Receive response
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                var line: String?
                val result = StringBuffer("")
                while (reader.readLine().also { line = it } != null) {
                    line = URLDecoder.decode(line, "utf-8")
                    result.append(line)
                }
                reader.close()
                Log.e(tag, "Request result--->$result")
                requestResult = result.toString()
            } else {
                val str =
                    "Request failed ,responseCode = ${conn.responseCode},responseMsg = ${conn.responseMessage}"
                Log.e(tag, str)
                requestResult = str
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(tag, e.toString())
            requestResult = e.toString()
        }
        return requestResult
    }

    private fun testDevicesAPIPost(useJson: Boolean): String {

        var requestResult: String
        try {
            val requestUrl = "http://localhost:7302/playTTS?text=12"
            val url = URL(requestUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5 * 1000
            conn.readTimeout = 5 * 1000
            conn.requestMethod = "GET"
            conn.connect()
            if (conn.responseCode == 200) {
                // Receive response
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                var line: String?
                val result = StringBuffer("")
                while (reader.readLine().also { line = it } != null) {
                    line = URLDecoder.decode(line, "utf-8")
                    result.append(line)
                }
                reader.close()
                Log.e(tag, "Request result--->$result")
                requestResult = result.toString()
            } else {
                val str =
                    "Request failed ,responseCode = ${conn.responseCode},responseMsg = ${conn.responseMessage}"
                Log.e(tag, str)
                requestResult = str
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(tag, e.toString())
            requestResult = e.toString()
        }
        return requestResult
    }

    override fun onInit(status: Int) {

        // 判断是否转化成功
        if (status == TextToSpeech.SUCCESS) {
            //默认设定语言为中文，原生的android貌似不支持中文。
            var result = textToSpeech!!.setLanguage(Locale.CHINESE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "不支持中文", Toast.LENGTH_SHORT).show();
                //不支持中文就将语言设置为英文
                textToSpeech!!.setLanguage(Locale.ENGLISH);
            } else {
                Toast.makeText(this, "支持中文", Toast.LENGTH_SHORT).show();
            }
        }
    }

    class BootCompleteReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, arg1: Intent) {

            //启动app代码
            val autoStart = Intent(context, MainActivity::class.java)
            autoStart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(autoStart)
        }
    }
}