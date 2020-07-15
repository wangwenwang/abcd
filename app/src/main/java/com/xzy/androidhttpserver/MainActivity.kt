package com.xzy.androidhttpserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*


@Suppress("unused")
open class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener  {
    public val tag = "MainActivity"

    public var textToSpeech // TTS对象
            : TextToSpeech? = null

    public var receiver :BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 启动服务
        val intent = Intent(this, HttpService::class.java)
        startService(intent)

        textToSpeech = TextToSpeech(this, this) // 参数Context,TextToSpeech.OnInitListener

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                var msg = intent?.getStringExtra("msg")

                if (textToSpeech != null && !textToSpeech!!.isSpeaking()) {
                    // 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
//                    textToSpeech!!.setPitch(0.5f);
                    //设定语速 ，默认1.0正常语速
//                    textToSpeech!!.setSpeechRate(1.5f);
                    //朗读，注意这里三个参数的added in API level 4   四个参数的added in API level 21
                    textToSpeech!!.speak(msg, TextToSpeech.QUEUE_FLUSH, null)
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver!!, IntentFilter("actionName"))

//        btn_clear.setOnClickListener { tv_result.text = "" }
        btn_test_local_get.setOnClickListener {
            runBlocking {
                val job = GlobalScope.async {
                    testDevicesAPIGet()
                }
                tv_result.text = job.await()
            }
        }
//        btn_test_local_post.setOnClickListener {
//            runBlocking {
//                val job = GlobalScope.async {
//                    testDevicesAPIPost(true)
//                }
//                tv_result.text = job.await()
//            }
//        }

        val home = Intent(Intent.ACTION_MAIN)
        home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        home.addCategory(Intent.CATEGORY_HOME)
        startActivity(home)
    }

    private fun testDevicesAPIGet(): String {

        var tv_prompt = findViewById(R.id.textView) as EditText

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

        Log.d("LM", "onInit: ")

//        // 判断是否转化成功
        if (status == TextToSpeech.SUCCESS){
            //默认设定语言为中文，原生的android貌似不支持中文。
            var result = textToSpeech!!.setLanguage(Locale.CHINESE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(this, "不支持中文", Toast.LENGTH_SHORT).show();
                //不支持中文就将语言设置为英文
                textToSpeech!!.setLanguage(Locale.ENGLISH);
            }else{
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