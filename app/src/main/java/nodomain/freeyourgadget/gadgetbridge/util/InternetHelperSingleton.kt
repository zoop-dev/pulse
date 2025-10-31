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
package nodomain.freeyourgadget.gadgetbridge.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.webkit.WebResourceResponse
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.internethelper.aidl.http.HttpGetRequest
import nodomain.freeyourgadget.internethelper.aidl.http.HttpHeaders
import nodomain.freeyourgadget.internethelper.aidl.http.HttpResponse
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpCallback
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpService
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.util.concurrent.CountDownLatch

object InternetHelperSingleton {
    private val LOG: Logger = LoggerFactory.getLogger(InternetHelperSingleton::class.java)
    private var internetHelperBound = false
    private var internetHelper: IHttpService? = null

    fun getHttpService(): IHttpService? {
        ensureInternetHelperBound()
        return internetHelper
    }

    //Internet helper outgoing connection
    private val internetHelperConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            LOG.info("internet helper service bound")
            internetHelperBound = true
            internetHelper = IHttpService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            LOG.info("internet helper service unbound")
            internetHelper = null
            internetHelperBound = false
        }
    }

    fun isInternetHelperBound(): Boolean {
        return internetHelperBound
    }

    fun ensureInternetHelperBound(): Boolean {
        val context = GBApplication.getContext()
        if (!internetHelperBound) {
            val internetHelperPkg = "nodomain.freeyourgadget.internethelper"
            val internetHelperCls = "$internetHelperPkg.HttpService"
            try {
                context.packageManager.getApplicationInfo(internetHelperPkg, 0)
                val intent = Intent()
                intent.component = ComponentName(internetHelperPkg, internetHelperCls)

                val intent1 = Intent("nodomain.freeyourgadget.internethelper.HttpService")
                intent1.setPackage("nodomain.freeyourgadget.internethelper")
                context.bindService(intent1, internetHelperConnection, Context.BIND_AUTO_CREATE)
                LOG.info("WEBVIEW: Internet helper bound successfully.")
            } catch (_: PackageManager.NameNotFoundException) {
                LOG.info("WEBVIEW: Internet helper not installed, only mimicked HTTP requests will work.")
            } catch (_: SecurityException) {
                LOG.info("WEBVIEW: Permission for internet helper not granted, only mimicked HTTP requests will work.")
            }
        }
        return internetHelperBound
    }

    @Throws(RemoteException::class, InterruptedException::class)
    fun send(webRequest: Uri): WebResourceResponse? {
        val httpHeaders = HttpHeaders()
        val httpGetRequest = HttpGetRequest(webRequest.toString(), httpHeaders)
        val latch = CountDownLatch(1)
        val internetResponseCapsule = Capsule<WebResourceResponse?>()
        LOG.debug("Forwarding GET request to {} to internet helper app", webRequest)
        try {
            internetHelper?.get(httpGetRequest, object : IHttpCallback.Stub() {
                @Throws(RemoteException::class)
                override fun onResponse(response: HttpResponse) {
                    // Extract headers
                    val contentType = response.headers["content-type"]?.split(";")?.get(0) ?: "text/html"
                    val contentEncoding = response.headers["content-encoding"] ?: "UTF-8"

                    // Retrieve original payload as InputStream
                    val inputStream = ParcelFileDescriptor.AutoCloseInputStream(response.body)

                    // Clean up and fix received malformed HTML
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
                    LOG.error("Error during GET request: $message")
                }
            })
        } catch (e: RemoteException) {
            LOG.error("Error during GET request", e)
        }
        try {
            latch.await()
        } catch (e: InterruptedException) {
            LOG.error("Error during GET request", e)
        }

        return internetResponseCapsule.get()
    }
}