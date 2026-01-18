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
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.Toast
import androidx.core.net.toUri
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleAppstoreIdEntry
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.internet.InternetRequestType
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.InternetUtils
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils
import nodomain.freeyourgadget.gadgetbridge.webview.GBChromeClient
import nodomain.freeyourgadget.gadgetbridge.webview.GBWebClient
import nodomain.freeyourgadget.gadgetbridge.webview.RequestInterceptorInterface
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class RebbleAppStoreActivity : AbstractGBActivity()  {
    val LOG: Logger = LoggerFactory.getLogger(RebbleAppStoreActivity::class.java)
    private lateinit var mGBDevice: GBDevice
    private var webView: WebView? = null
    private var url = "https://apps.rebble.io/en_US/watchfaces"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banglejs_apps_management)

        val gbDevice: GBDevice? = intent?.extras?.getParcelable(GBDevice.EXTRA_DEVICE)
        requireNotNull(gbDevice) { "Must provide a device when invoking this activity" }
        mGBDevice = gbDevice
        url = intent?.extras?.getString(DeviceService.EXTRA_URI, url)!!
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
        val filename = url.lastPathSegment ?: "downloaded_file.bin"
        val cacheDir = applicationContext.externalCacheDir
        val targetFile = File(cacheDir, filename)

        InternetUtils.downloadBinaryFile(url, targetFile) { file ->
            installFile(file)
        }
    }

    private fun downloadInstallWatchappById(storeId: String) {
        val appUrl = "https://appstore-api.rebble.io/api/v1/apps/id/$storeId"
        val response: JSONObject? = InternetUtils.doJsonRequest(appUrl.toUri())
        if (response != null) {
            val dataArray = response.getJSONArray("data")
            val firstAppObject = dataArray.getJSONObject(0)
            val appUUID = firstAppObject.getString("uuid")
            // Cache appstore ID for app update checks
            DBHelper.store(PebbleAppstoreIdEntry(appUUID, storeId, System.currentTimeMillis(), false));
            // Find and install pbw file
            val latestRelease = firstAppObject.getJSONObject("latest_release")
            val pbwFile = latestRelease.getString("pbw_file")
            downloadInstallWatchapp(pbwFile.toUri())
            // Download and cache preview image
            val previewUrl = firstAppObject.getJSONObject("list_image").getString("144x144")
            val cacheDir: File? = PebbleUtils.getPbwCacheDir()
            val previewImgFile = File(cacheDir, appUUID + "_preview.png")
            InternetUtils.downloadBinaryFile(previewUrl.toUri(), previewImgFile) { }
        }
    }

    fun installFile(file: File) {
        val installHandler: InstallHandler? = mGBDevice?.deviceCoordinator?.findInstallHandler(file.toUri(), applicationContext)
        if (installHandler == null) {
            GB.toast(getString(R.string.fwinstaller_file_not_compatible_to_device), Toast.LENGTH_LONG, GB.ERROR)
            LOG.error("Installable file not compatible with device")
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
        val settings = webView!!.settings
        settings.javaScriptEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        val gbWebClient = object : GBWebClient(InternetRequestType.PEBBLE_APP_STORE, mGBDevice) {
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
                LOG.error(description)
                view.loadUrl("about:blank")
            }
        }
        webView!!.webViewClient = gbWebClient

        webView!!.webChromeClient = object : GBChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        webView!!.addJavascriptInterface(RequestInterceptorInterface(gbWebClient), "GBReqInt")
        webView!!.loadUrl(url)
    }
}
