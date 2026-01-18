/*  Copyright (C) 2025 Arjan Schrijver

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.internet.InternetRequestType
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.webview.GBChromeClient
import nodomain.freeyourgadget.gadgetbridge.webview.GBWebClient
import nodomain.freeyourgadget.gadgetbridge.webview.RequestInterceptorInterface
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PebbleJsService : Service() {

    companion object {
        @Volatile
        private var instance: PebbleJsService? = null

        fun getInstance(): PebbleJsService? = instance

        fun startService(context: Context) {
            val intent = Intent(context, PebbleJsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    val LOG: Logger = LoggerFactory.getLogger(PebbleJsService::class.java)
    private val webviews = ConcurrentHashMap<String, WebView>()
    private val currentRunningUUID = ConcurrentHashMap<String, UUID>()

    override fun onCreate() {
        super.onCreate()
        instance = this
        startForegroundServiceNotification()
    }

    override fun onDestroy() {
        instance = null
        destroyAllWebViews()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(GB.NOTIFICATION_CHANNEL_ID_PEBBLE_JS, getString(R.string.notification_channel_pebble_js_runner), NotificationManager.IMPORTANCE_LOW)
            )
        }

        val notification = NotificationCompat.Builder(this, GB.NOTIFICATION_CHANNEL_ID_PEBBLE_JS)
            .setContentTitle(getString(R.string.notification_pebble_js_service_title))
            .setContentText(getString(R.string.notification_pebble_js_service_text))
            .setSmallIcon(R.drawable.ic_play)
            .build()

        startForeground(1005, notification)
    }

    fun startJsForDevice(device: GBDevice, uuid: UUID) {
        Handler(Looper.getMainLooper()).post {
            if (!webviews.containsKey(device.address)) {
                LOG.info("WEBVIEW starting for device ${device.address}")
                WebView.setWebContentsDebuggingEnabled(true)
                val uiContext = applicationContext.createConfigurationContext(resources.configuration)
                val gbWebClient = GBWebClient(InternetRequestType.PEBBLE_BACKGROUND_JS, device)
                val wv = WebView(uiContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowContentAccess = true
                    settings.allowFileAccess = true
                    settings.allowFileAccessFromFileURLs = true
                    settings.allowUniversalAccessFromFileURLs = true
                    visibility = View.GONE
                    webViewClient = gbWebClient
                    webChromeClient = GBChromeClient()
                    addJavascriptInterface(RequestInterceptorInterface(gbWebClient), "GBReqInt")
                    setWillNotDraw(true)
                }
                wv.clearCache(true)
                wv.resumeTimers()
                webviews[device.address] = wv
            }

            if (uuid == currentRunningUUID) {
                LOG.debug("WEBVIEW uuid not changed keeping the old context")
            } else {
                val jsInterface = JSInterface(device, uuid)
                LOG.debug("WEBVIEW uuid changed, restarting")
                currentRunningUUID[device.address] = uuid
                webviews[device.address]?.onResume()
                webviews[device.address]?.removeJavascriptInterface("GBjs")
                webviews[device.address]?.addJavascriptInterface(jsInterface, "GBjs")
                webviews[device.address]?.loadUrl("file:///android_asset/app_config/configure.html?rand=" + Math.random() * 500)
            }
        }
    }

    fun evaluateJsForDevice(device: GBDevice, js: String, resultCallback: ValueCallback<String>?) {
        // The webviews can live in the service, but interaction with them must always be performed from the main thread
        Handler(Looper.getMainLooper()).post {
            val wv = webviews[device.address]
            wv?.evaluateJavascript(js, resultCallback)
        }
    }

    fun stopJsForDevice(device: GBDevice) {
        Handler(Looper.getMainLooper()).post {
            webviews.remove(device.address)?.destroy()
            if (webviews.isEmpty()) {
                LOG.info("Last webview stopped, stopping Pebble JS Service...")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    stopForeground(true)
                }
                stopSelf()
            }
        }
    }

    private fun destroyAllWebViews() {
        Handler(Looper.getMainLooper()).post {
            webviews.values.forEach { it.destroy() }
            webviews.clear()
        }
    }

    fun checkAppRunning(device: GBDevice, appUUID: UUID): Boolean {
        return currentRunningUUID[device.address]?.equals(appUUID) ?: false
    }
}
