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

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceResponse
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.internethelper.aidl.http.HttpRequest
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class InternetUtils {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(InternetUtils::class.java)
        private val defaultClient = OkHttpClient()
        private const val USER_AGENT = "Gadgetbridge/${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_HASH_SHORT})"

        /**
         * Returns a new Map containing the User-Agent header.
         * If the provided Map already contains a User-Agent, it is kept intact.
         */
        fun headersWithUserAgent(
            requestHeaders: Map<String, String>
        ): Map<String, String> {
            val hasUserAgent = requestHeaders.keys.any { it.equals("User-Agent", ignoreCase = true) }

            return if (hasUserAgent) {
                requestHeaders
            } else {
                requestHeaders + ("User-Agent" to USER_AGENT)
            }
        }

        /**
         * Performs an HTTP request to the given URI, optionally allowing insecure connections.
         */
        fun doStringRequest(
            uri: Uri,
            method: String = "GET",
            requestHeaders: Map<String, String> = emptyMap(),
            body: String? = null,
            allowInsecure: Boolean = false
        ): String? {
            val response: WebResourceResponse? = if (GBApplication.hasDirectInternetAccess()) {
                directRequest(uri, method, requestHeaders, body, allowInsecure)
            } else {
                InternetHelperSingleton.send(
                    uri,
                    HttpRequest.Method.valueOf(method),
                    headersWithUserAgent(requestHeaders),
                    body?.toByteArray(),
                    allowInsecure,
                )
            }
            if (response == null) return null

            // Convert response InputStream to String
            return response.data.bufferedReader().use { it.readText() }
        }

        fun doJsonRequest(
            uri: Uri,
            method: String = "GET",
            requestHeaders: Map<String, String> = emptyMap(),
            body: String? = null,
            allowInsecure: Boolean = false
        ): JSONObject? {
            val text = doStringRequest(
                uri,
                method,
                headersWithUserAgent(requestHeaders),
                body,
                allowInsecure
            )
            try {
                return JSONObject(text)
            } catch (e: Exception) {
                LOG.error("Error while parsing JSON response", e)
                return null
            }
        }

        fun downloadBinaryFile(
            uri: Uri,
            targetFile: File,
            onComplete: (File) -> Unit
        ) {
            try {
                val response: WebResourceResponse? = if (GBApplication.hasDirectInternetAccess()) {
                    directRequest(
                        uri = uri,
                        method = "GET",
                        requestHeaders = mapOf("User-Agent" to USER_AGENT),
                        body = null,
                        allowInsecure = false
                    )
                } else {
                    InternetHelperSingleton.send(
                        uri,
                        HttpRequest.Method.GET,
                        requestHeaders = mapOf("User-Agent" to USER_AGENT),
                        null,
                        false
                    )
                }

                response?.data?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (response != null)
                    onComplete(targetFile)
            } catch (e: Exception) {
                LOG.error("Downloading $uri failed: ", e)
            }
        }

        /**
         * Direct HTTP request using OkHttp.
         */
        private fun directRequest(
            uri: Uri,
            method: String,
            requestHeaders: Map<String, String>,
            body: String?,
            allowInsecure: Boolean
        ): WebResourceResponse {

            val client = if (allowInsecure) createInsecureClient() else defaultClient

            val builder = Request.Builder().url(uri.toString())

            // Apply request headers
            for ((key, value) in headersWithUserAgent(requestHeaders)) {
                builder.addHeader(key, value)
            }

            // Convert body string to RequestBody if allowed
            val requestBody: RequestBody? =
                if (body != null && method.uppercase() !in listOf("GET", "HEAD")) {
                    body.toRequestBody("application/octet-stream".toMediaType())
                } else null

            // Configure HTTP method
            when (method.uppercase()) {
                "GET" -> builder.get()
                "HEAD" -> builder.head()
                "DELETE" -> if (requestBody != null) builder.delete(requestBody) else builder.delete()
                else -> builder.method(method.uppercase(), requestBody)
            }

            // Execute request
            client.newCall(builder.build()).execute().use { response ->
                val statusCode = response.code
                val message = if (!response.message.isEmpty()) response.message else "OK"
                val headers = response.headers.toMap()

                val contentType = response.header("content-type") ?: "application/octet-stream"
                val encoding = response.header("content-encoding") ?: "UTF-8"

                // HEAD: empty body
                if (method.equals("HEAD", true)) {
                    return WebResourceResponse(
                        contentType,
                        encoding,
                        statusCode,
                        message,
                        headers,
                        ByteArrayInputStream(ByteArray(0))
                    )
                }

                return WebResourceResponse(
                    contentType,
                    encoding,
                    statusCode,
                    message,
                    headers,
                    ByteArrayInputStream(response.body.bytes())
                )
            }
        }

        /**
         * Converts OkHttp Headers to a Map suitable for WebResourceResponse.
         */
        private fun Headers.toMap(): Map<String, String> {
            val result = LinkedHashMap<String, String>()
            for (name in this.names()) {
                result[name] = this.values(name).joinToString(",")
            }
            return result
        }

        /**
         * Creates an OkHttpClient that accepts all SSL certificates (insecure).
         */
        private fun createInsecureClient(): OkHttpClient {
            return try {
                val trustAllCerts = arrayOf<TrustManager>(
                    @SuppressLint("CustomX509TrustManager")
                    object : X509TrustManager {
                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                )

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                val sslSocketFactory = sslContext.socketFactory

                OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
                    .build()
            } catch (e: Exception) {
                LOG.error("Failed to create insecure OkHttp client", e)
                defaultClient
            }
        }
    }
}
