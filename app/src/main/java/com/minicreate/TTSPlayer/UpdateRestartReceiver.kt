package com.minicreate.TTSPlayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.PACKAGE_REPLACED") {
            //Toast.makeText(context,"已升级到新版本",Toast.LENGTH_SHORT).show();
            val intent2 = Intent(context, MainActivity::class.java)
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent2)
        }
    }
}
