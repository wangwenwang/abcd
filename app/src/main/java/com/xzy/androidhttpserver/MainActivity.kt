package com.xzy.androidhttpserver

import android.content.*
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.AppFloatDefaultAnimator
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.interfaces.OnDisplayHeight
import com.lzf.easyfloat.interfaces.OnInvokeView
import com.lzf.easyfloat.utils.DisplayUtils
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


        EasyFloat.with(this)
            // 设置浮窗xml布局文件，并可设置详细信息
            .setLayout(R.layout.float_test, OnInvokeView {  })
            // 设置浮窗显示类型，默认只在当前Activity显示，可选一直显示、仅前台显示、仅后台显示
            .setShowPattern(ShowPattern.ALL_TIME)
            // 设置吸附方式，共15种模式，详情参考SidePattern
            .setSidePattern(SidePattern.RESULT_HORIZONTAL)
            // 设置浮窗的标签，用于区分多个浮窗
            .setTag("testFloat")
            // 设置浮窗是否可拖拽，默认可拖拽
            .setDragEnable(true)
            // 系统浮窗是否包含EditText，仅针对系统浮窗，默认不包含
            .hasEditText(false)
            // 设置浮窗固定坐标，ps：设置固定坐标，Gravity属性和offset属性将无效
            .setLocation(100, 1500)
            // 设置浮窗的对齐方式和坐标偏移量
            .setGravity(Gravity.END or Gravity.CENTER_VERTICAL, 0, 200)
            // 设置宽高是否充满父布局，直接在xml设置match_parent属性无效
            .setMatchParent(widthMatch = false, heightMatch = false)
            // 设置Activity浮窗的出入动画，可自定义，实现相应接口即可（策略模式），无需动画直接设置为null
            .setAnimator(DefaultAnimator())
            // 设置系统浮窗的出入动画，使用同上
            .setAppFloatAnimator(AppFloatDefaultAnimator())
            // 设置系统浮窗的不需要显示的页面
//            .setFilter(MainActivity::class.java)
            // 设置系统浮窗的有效显示高度（不包含虚拟导航栏的高度），基本用不到，除非有虚拟导航栏适配问题
            .setDisplayHeight(OnDisplayHeight { context -> DisplayUtils.rejectedNavHeight(context) })
            // 浮窗的一些状态回调，如：创建结果、显示、隐藏、销毁、touchEvent、拖拽过程、拖拽结束。
            // ps：通过Kotlin DSL实现的回调，可以按需复写方法，用到哪个写哪个
            .registerCallback {
                createResult { isCreated, msg, view ->  }
                show {  }
                hide {  }
                dismiss {  }
                touchEvent { view, motionEvent ->  }
                drag { view, motionEvent ->  }
                dragEnd {  }
            }
            // 创建浮窗（这是关键哦😂）
            .show()

        object : Thread() {
            override fun run() {

                sleep(3000)
                runOnUiThread {

//                    val home = Intent(Intent.ACTION_MAIN)
//                    home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                    home.addCategory(Intent.CATEGORY_HOME)
//                    startActivity(home)

                    val intent = Intent(Intent.ACTION_MAIN)
                    //前提：知道要跳转应用的包名、类名
                    val componentName = ComponentName("com.teamhd.gnamp", "");
                    intent.setComponent(componentName);
                    startActivity(intent);
                }
            }
        }.start()
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