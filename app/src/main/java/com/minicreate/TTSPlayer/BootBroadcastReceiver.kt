package com.minicreate.TTSPlayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

public class BootBroadcastReceiver : BroadcastReceiver() {

    /**
     * 可以实现开机自动打开软件并运行。
     */
    override fun onReceive(p0: Context?, p1: Intent?) {

        System.out.println("自启动程序即将执行");

        val intent = Intent()
        intent.setClass(WdTools.getContext(), MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        WdTools.getContext().startActivity(intent)
    }
}