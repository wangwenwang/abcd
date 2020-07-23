package com.minicreate.TTSPlayer

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.koushikdutta.async.http.Multimap
import com.koushikdutta.async.http.body.AsyncHttpRequestBody
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.HttpServerRequestCallback
import org.json.JSONException
import org.json.JSONObject


/**
 *
 * @author xzy
 * @date 2020/06/18
 */
class HttpServer : HttpServerRequestCallback  {
    var httpServer = AsyncHttpServer()

    fun start() {
        Log.d(TAG, "Starting http server...")
        httpServer["[\\d\\D]*", this]
        httpServer.post("[\\d\\D]*", this)
        httpServer.listen(PORT_DEFAULT)
    }

    fun stop() {
        Log.d(TAG, "Stopping http server...")
        httpServer.stop()
    }

    private fun sendResponse(response: AsyncHttpServerResponse, json: JSONObject) {
        // Enable CORS
        response.headers.add("Access-Control-Allow-Origin", "*")
        response.send(json)
    }

    override fun onRequest(
        request: AsyncHttpServerRequest,
        response: AsyncHttpServerResponse
    ) {
        val uri = request.path
        Log.d(TAG, "onRequest $uri")
        val params: Any?
        params = if (request.method == "GET") {
            request.query
        } else if (request.method == "POST") {
            val contentType = request.headers["Content-Type"]
            if (contentType == "application/json") {
                (request.body as AsyncHttpRequestBody<*>).get()
            } else {
                (request.body as AsyncHttpRequestBody<*>).get()
            }
        } else {
            Log.d(TAG, "Unsupported Method")
            return
        }
        if (params != null) {
            Log.d(TAG, "params = $params")
        }
        when (uri) {
            "/playTTS" -> handleDevicesRequest(params, response)
            else -> handleInvalidRequest(params, response)
        }
    }

    private fun handleDevicesRequest(
        params: Any?,
        response: AsyncHttpServerResponse
    ) {
        // Print request params
        val text: String
        when (params) {
            is Multimap -> {
                text = params.getString("text")

                var intent = Intent("actionName")
                intent.putExtra("msg", text)
                LocalBroadcastManager.getInstance(WdTools.getContext()).sendBroadcast(intent)

                Log.d(TAG, "[Multimap] text=$text")
            }
            is JSONObject -> {
                text = try {
                    Log.d(TAG, params.toString())
                    params.getString("text")
                } catch (e: JSONException) {
                    e.printStackTrace()
                    return
                }
                Log.d(TAG, "[JSONObject] text=$text")
            }
            else -> {
                Log.e(TAG, "Invalid request params")
                return
            }
        }

        // Send JSON format response
        try {
            val json = JSONObject()
            json.put("text", text)
            sendResponse(response, json)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun handleInvalidRequest(
        params: Any?,
        response: AsyncHttpServerResponse
    ) {
        Log.d("params", params.toString())
        val json = JSONObject()
        try {
            json.put("error", "Invalid API")
            sendResponse(response, json)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "HttpServer"
        private var mInstance: HttpServer? = null
        var PORT_DEFAULT = 7302
        val instance: HttpServer?
            get() {
                if (mInstance == null) {
                    synchronized(HttpServer::class.java) {
                        if (mInstance == null) {
                            mInstance = HttpServer()
                        }
                    }
                }
                return mInstance
            }
    }
}