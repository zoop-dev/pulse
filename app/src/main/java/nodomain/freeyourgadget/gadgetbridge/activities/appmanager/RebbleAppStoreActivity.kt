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
package nodomain.freeyourgadget.gadgetbridge.activities.appmanager

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.net.toUri
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.InternetHelperSingleton
import nodomain.freeyourgadget.gadgetbridge.webview.GBChromeClient
import nodomain.freeyourgadget.gadgetbridge.webview.GBWebClient
import nodomain.freeyourgadget.internethelper.aidl.http.HttpGetRequest
import nodomain.freeyourgadget.internethelper.aidl.http.HttpHeaders
import nodomain.freeyourgadget.internethelper.aidl.http.HttpResponse
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpCallback
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class RebbleAppStoreActivity : AbstractGBActivity()  {
    val LOG: Logger = LoggerFactory.getLogger(RebbleAppStoreActivity::class.java)
    private var mGBDevice: GBDevice? = null
    private var webView: WebView? = null
    private var url = "https://apps.rebble.io/en_US/watchfaces"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banglejs_apps_management)

        val extras = intent.extras
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE)
            url = extras.getString(DeviceService.EXTRA_URI, url)
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

    private fun isDownloadableWatchapp(url: String): Boolean {
        val downloadExtensions = listOf(".pbw", ".zip")
        return downloadExtensions.any { url.endsWith(it, ignoreCase = true) }
    }

    private fun downloadInstallWatchapp(url: Uri) {
        if (GBApplication.hasDirectInternetAccess()) {
            downloadBinaryFile(url) { file ->
                installFile(file)
            }
        } else {
            val httpHeaders = HttpHeaders()
            val httpGetRequest = HttpGetRequest(url.toString(), httpHeaders)
            InternetHelperSingleton.getHttpService()
                ?.get(httpGetRequest, object : IHttpCallback.Stub() {
                    override fun onResponse(response: HttpResponse) {
                        val contentType = response.headers["content-type"]?.split(";")?.get(0)
                        if (!contentType.equals("application/octet-stream") && !contentType.equals("application/zip")) {
                            GB.toast(
                                getString(
                                    R.string.rebble_appstore_download_failed_wrong_content_type,
                                    contentType
                                ),
                                Toast.LENGTH_LONG, GB.ERROR
                            )
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
                        installFile(cacheFile)
                    }

                    override fun onException(message: String?) {
                        GB.toast(
                            getString(R.string.rebble_appstore_download_failed, message),
                            Toast.LENGTH_LONG,
                            GB.ERROR
                        )
                    }
                })
        }
    }

    private fun downloadInstallWatchappById(storeId: String) {
        val appUrl = "https://appstore-api.rebble.io/api/v1/apps/id/$storeId"
        if (GBApplication.hasDirectInternetAccess()) {
            val requestQueue = Volley.newRequestQueue(this)
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.GET, appUrl, null,
                { response ->
                    val dataArray = response.getJSONArray("data")
                    val firstAppObject = dataArray.getJSONObject(0)
                    val latestRelease = firstAppObject.getJSONObject("latest_release")
                    val pbwFile = latestRelease.getString("pbw_file")
                    downloadInstallWatchapp(pbwFile.toUri())
                },
                { error ->
                    GB.toast(
                        getString(
                            R.string.rebble_appstore_fetching_download_file_failed,
                            error
                        ), Toast.LENGTH_LONG, GB.ERROR
                    )
                }
            )
            requestQueue.add(jsonObjectRequest)
        } else {
            val httpHeaders = HttpHeaders()
            val httpGetRequest = HttpGetRequest(appUrl, httpHeaders)
            InternetHelperSingleton.getHttpService()
                ?.get(httpGetRequest, object : IHttpCallback.Stub() {
                    override fun onResponse(response: HttpResponse) {
                        val contentType = response.headers["content-type"]?.split(";")?.get(0)
                        if (!contentType.equals("application/json")) {
                            GB.toast(
                                getString(
                                    R.string.rebble_appstore_fetch_app_info_failed_content_type,
                                    contentType
                                ),
                                Toast.LENGTH_LONG, GB.ERROR
                            )
                            return
                        }
                        val inputStream = ParcelFileDescriptor.AutoCloseInputStream(response.body)
                        val responseBody =
                            InputStreamReader(inputStream, StandardCharsets.UTF_8).readText()
                        val jsonObject = JSONObject(responseBody)
                        val dataArray = jsonObject.getJSONArray("data")
                        val firstAppObject = dataArray.getJSONObject(0)
                        val latestRelease = firstAppObject.getJSONObject("latest_release")
                        val pbwFile = latestRelease.getString("pbw_file")
                        downloadInstallWatchapp(pbwFile.toUri())
                    }

                    override fun onException(message: String?) {
                        GB.toast(
                            getString(
                                R.string.rebble_appstore_fetching_download_file_failed,
                                message
                            ), Toast.LENGTH_LONG, GB.ERROR
                        )
                    }
                })
        }
    }

    fun downloadBinaryFile(url: Uri, onComplete: (File) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = okhttp3.Request.Builder().url(url.toString()).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    LOG.error("Downloading $url failed: ${response.code}")
                    return@launch
                }

                val filename = url.lastPathSegment ?: "downloaded_file.pbw"
                val cacheDir = applicationContext.externalCacheDir
                val cacheFile = File(cacheDir, filename)
                val outputStream = FileOutputStream(cacheFile)

                val inputStream = response.body.byteStream()
                inputStream.copyTo(outputStream)
                outputStream.flush()
                outputStream.close()
                inputStream.close()

                onComplete(cacheFile)
            } catch (e: Exception) {
                LOG.error("Downloading $url failed: ", e)
            }
        }
    }

    fun installFile(file: File) {
        val installHandler: InstallHandler? = mGBDevice?.deviceCoordinator?.findInstallHandler(file.toUri(), applicationContext)
        if (installHandler == null) {
            GB.toast(getString(R.string.fwinstaller_file_not_compatible_to_device), Toast.LENGTH_LONG, GB.ERROR)
            return
        }
        val startIntent = Intent(applicationContext, installHandler.getInstallActivity())
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, mGBDevice)
        startIntent.action = Intent.ACTION_VIEW
        startIntent.setDataAndType(file.toUri(), null)
        startActivity(startIntent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initViews() {
        webView = findViewById(R.id.webview)
        webView!!.webViewClient = WebViewClient()
        val settings = webView!!.settings
        settings.javaScriptEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        webView!!.webViewClient = object : GBWebClient(GBWebClient.REQUEST_TYPE_PEBBLE_APP_STORE) {
            override fun shouldOverrideUrlLoading(
                wv: WebView,
                request: WebResourceRequest
            ): Boolean {
                val requestUrl = request.url
                // Handle pebble:// urls
                if (requestUrl.toString().startsWith("pebble://appstore/")) {
                    val appId = requestUrl.lastPathSegment
                    if (appId != null) {
                        downloadInstallWatchappById(appId)
                    }
                    return true
                }
                // Check if the URL is a downloadable watchface/watchapp
                if (isDownloadableWatchapp(requestUrl.toString())) {
                    downloadInstallWatchapp(requestUrl)
                    return true  // Indicate that the URL loading is handled
                }

                val intent = Intent(Intent.ACTION_VIEW, requestUrl)
                wv.context.startActivity(intent)
                return true
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

        webView!!.webChromeClient = object : GBChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }
    }
}
