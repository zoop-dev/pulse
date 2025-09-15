/*  Copyright (C) 2022-2024 Andreas Shimokawa, Daniel Dakhno, Gordon Williams, Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.appmanager

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.net.toUri
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.Capsule
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.internethelper.aidl.http.HttpGetRequest
import nodomain.freeyourgadget.internethelper.aidl.http.HttpHeaders
import nodomain.freeyourgadget.internethelper.aidl.http.HttpResponse
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpCallback
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpService
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.Volatile

class RebbleAppStoreActivity : AbstractGBActivity()  {
    val LOG: Logger = LoggerFactory.getLogger(RebbleAppStoreActivity::class.java)
    private var mGBDevice: GBDevice? = null
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banglejs_apps_management)

        val intent1 = Intent("nodomain.freeyourgadget.internethelper.HttpService")
        intent1.setPackage("nodomain.freeyourgadget.internethelper")
        val res = applicationContext.bindService(intent1, mHttpConnection, BIND_AUTO_CREATE)
        if (res) {
            LOG.info("Bound to HttpService")
        } else {
            LOG.warn("Could not bind to HttpService")
        }

        val extras = intent.extras
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE)
        }
        requireNotNull(mGBDevice) { "Must provide a device when invoking this activity" }
        initViews()
    }

    override fun onResume() {
        super.onResume()
        if (webView != null) return  // already set up
        val commandFilter = IntentFilter()
        commandFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED)
        initViews()
    }

    override fun onDestroy() {
        webView!!.destroy()
        webView = null
        super.onDestroy()
        finish()
    }

    @Volatile
    private var iHttpService: IHttpService? = null

    private val mHttpConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            LOG.info("onServiceConnected: {}", className)
            iHttpService = IHttpService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            LOG.error("Service has unexpectedly disconnected: {}", className)
            iHttpService = null
        }
    }

    private fun isDownloadableWatchapp(url: String): Boolean {
        val downloadExtensions = listOf(".pbw", ".zip")
        return downloadExtensions.any { url.endsWith(it, ignoreCase = true) }
    }

    private fun downloadInstallWatchapp(url: Uri) {
        val httpHeaders = HttpHeaders()
        val httpGetRequest = HttpGetRequest(url.toString(), httpHeaders)
        iHttpService!!.get(httpGetRequest, object : IHttpCallback.Stub() {
            override fun onResponse(response: HttpResponse) {
                val contentType = response.headers["content-type"]?.split(";")?.get(0)
                if (!contentType.equals("application/octet-stream") && !contentType.equals("application/zip")) {
                    GB.toast("Download failed, wrong content-type: $contentType", Toast.LENGTH_LONG, GB.ERROR)
                    return
                }
                val inputStream = ParcelFileDescriptor.AutoCloseInputStream(response.body)
                val filename = url.lastPathSegment ?: "downloaded_file.pbw"
                val cacheDir = applicationContext.externalCacheDir ?: return
                val cacheFile = File(cacheDir, filename)
                val outputStream: OutputStream = FileOutputStream(cacheFile)
                val buffer = ByteArray(1024)
                var len: Int
                while (inputStream.read(buffer).also { len = it } > 0) {
                    outputStream.write(buffer, 0, len)
                }
                outputStream.flush()
                outputStream.close()
                val installHandler: InstallHandler? = mGBDevice?.deviceCoordinator?.findInstallHandler(cacheFile.toUri(), applicationContext)
                if (installHandler == null) {
                    GB.toast(getString(R.string.fwinstaller_file_not_compatible_to_device), Toast.LENGTH_LONG, GB.ERROR)
                    return
                }
                val startIntent = Intent(applicationContext, installHandler.getInstallActivity())
                startIntent.putExtra(GBDevice.EXTRA_DEVICE, mGBDevice)
                startIntent.action = Intent.ACTION_VIEW
                startIntent.setDataAndType(cacheFile.toUri(), null)
                startActivity(startIntent)
            }
            override fun onException(message: String?) {
                GB.toast("Download failed: $message", Toast.LENGTH_LONG, GB.ERROR)
            }
        })
    }

    // TODO: handle links like pebble://appstore/52b231c2b70e1c159500009b#
    //       so we don't have to use ?dev_settings=true

    @SuppressLint("SetJavaScriptEnabled")
    private fun initViews() {
        webView = findViewById(R.id.webview)
        webView!!.webViewClient = WebViewClient()
        val settings = webView!!.settings
        settings.javaScriptEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        val url = "https://apps.rebble.io/en_US/watchfaces?dev_settings=true"

        webView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                wv: WebView,
                request: WebResourceRequest
            ): Boolean {
                val requestUrl = request.url
                // Check if the URL is a downloadable watchface/watchapp
                if (isDownloadableWatchapp(requestUrl.toString())) {
                    downloadInstallWatchapp(requestUrl)
                    return true  // Indicate that the URL loading is handled
                }

                val intent = Intent(Intent.ACTION_VIEW, requestUrl)
                wv.context.startActivity(intent)
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                LOG.info("shouldIntercept {} {} {}", request.method, request.url, iHttpService != null)

                if (!request.method.equals("get", ignoreCase = true)) {
                    return super.shouldInterceptRequest(view, request)
                }
                if (iHttpService == null) {
                    return super.shouldInterceptRequest(view, request)
                }

                val httpHeaders = HttpHeaders()
                for (header in request.requestHeaders.entries) {
                    httpHeaders.addHeader(header.key, header.value)
                }

                val httpGetRequest = HttpGetRequest(request.url.toString(), httpHeaders)
                val latch = CountDownLatch(1)
                val internetResponseCapsule = Capsule<WebResourceResponse?>()

                try {
                    iHttpService!!.get(httpGetRequest, object : IHttpCallback.Stub() {
                        @Throws(RemoteException::class)
                        override fun onResponse(response: HttpResponse) {
                            // Extract headers
                            val contentType = response.headers["content-type"]?.split(";")?.get(0) ?: "text/html"
                            val contentEncoding = response.headers["content-encoding"] ?: "UTF-8"

                            // Retrieve original payload as InputStream
                            val inputStream = ParcelFileDescriptor.AutoCloseInputStream(response.body)

                            // Clean up malformed HTML from Rebble
                            if (contentType.equals("text/html", ignoreCase = true)) {
                                val rawHtml = inputStream.bufferedReader(Charset.forName(contentEncoding)).use { it.readText() }
                                val cleanedHtml = Jsoup.parse(rawHtml).html()
                                val internetResponse = WebResourceResponse(
                                    contentType,
                                    contentEncoding,
                                    response.status,
                                    "OK",
                                    response.headers.toMap(),
                                    cleanedHtml.byteInputStream()
                                )
                                internetResponseCapsule.set(internetResponse)
                            } else {
                                // If not text/html, return original response
                                val internetResponse = WebResourceResponse(
                                    contentType,
                                    contentEncoding,
                                    response.status,
                                    "OK",
                                    response.headers.toMap(),
                                    inputStream
                                )
                                internetResponseCapsule.set(internetResponse)
                            }
                            latch.countDown()
                        }

                        @Throws(RemoteException::class)
                        override fun onException(message: String?) {
                            throw RuntimeException(message)
                        }
                    })
                } catch (e: RemoteException) {
                    throw RuntimeException(e)
                }

                try {
                    latch.await()
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }

                return internetResponseCapsule.get()
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                GB.toast(
                    "Error: $description",
                    Toast.LENGTH_SHORT,
                    GB.ERROR
                )
                view.loadUrl("about:blank")
            }
        }

        val mainLooper = Looper.getMainLooper()
        Handler(mainLooper).postDelayed({
            webView!!.loadUrl(url)
        }, 100)

        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }
    }
}