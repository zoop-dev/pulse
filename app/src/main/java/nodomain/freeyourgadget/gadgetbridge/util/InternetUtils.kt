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
import nodomain.freeyourgadget.gadgetbridge.R
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class InternetUtils {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(InternetUtils::class.java)
        private val defaultClient = OkHttpClient()
        private const val USER_AGENT = "Gadgetbridge/${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_HASH_SHORT}${BuildConfig.GIT_DIRTY_STATUS})"

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
         * On failure returns null; [onError] (default no-op) is invoked with a localized,
         * user-facing reason (no internet / server unreachable / could not resolve host / ...)
         * so callers that surface errors can opt in without changing the return type.
         */
        @JvmOverloads
        fun doStringRequest(
            uri: Uri,
            method: String = "GET",
            requestHeaders: Map<String, String> = emptyMap(),
            body: String? = null,
            allowInsecure: Boolean = false,
            onError: (reason: String) -> Unit = {},
        ): String? {
            val response: WebResourceResponse? = try {
                if (GBApplication.hasDirectInternetAccess()) {
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
            } catch (e: Exception) {
                LOG.error("Request to $uri failed: ", e)
                onError(networkFailureReason(e))
                return null
            }
            if (response == null) {
                onError(networkFailureReason(null))
                return null
            }

            // Convert response InputStream to String
            return response.data.bufferedReader().use { it.readText() }
        }

        @JvmOverloads
        fun doJsonRequest(
            uri: Uri,
            method: String = "GET",
            requestHeaders: Map<String, String> = emptyMap(),
            body: String? = null,
            allowInsecure: Boolean = false,
            onError: (reason: String) -> Unit = {},
        ): JSONObject? {
            val text = doStringRequest(
                uri,
                method,
                headersWithUserAgent(requestHeaders),
                body,
                allowInsecure,
                onError,
            )
            if (text == null) {
                // No response at all (no connectivity / internet helper unavailable / server
                // unreachable). Not a parse error, onError already fired with the network reason.
                return null
            }
            try {
                return JSONObject(text)
            } catch (e: Exception) {
                LOG.error("Error while parsing JSON response", e)
                onError(GBApplication.getContext().getString(R.string.toast_error_unexpected_response))
                return null
            }
        }

        /**
         * Downloads [uri] into [targetFile]. On success [onComplete] is invoked with the file;
         * on failure [onError] (default no-op) is invoked with a localized, user-facing reason.
         */
        fun downloadBinaryFile(
            uri: Uri,
            targetFile: File,
            onError: (reason: String) -> Unit = {},
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

                if (response == null) {
                    onError(networkFailureReason(null))
                    return
                }

                response.data?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                onComplete(targetFile)
            } catch (e: Exception) {
                LOG.error("Downloading $uri failed: ", e)
                onError(networkFailureReason(e))
            }
        }

        /**
         * Uploads [file] as multipart/form-data. [onComplete] is invoked exactly once, always
         * *after* the network try/catch so that any exception thrown by the callback itself is
         * NOT mistaken for an upload failure (which previously double-fired the callback). On
         * failure [reason] carries a human-readable, localized explanation for the user
         * (no internet / server unreachable / …); it is null on success.
         */
        fun uploadBinaryFile(
            uri: Uri,
            file: File,
            requestHeaders: Map<String, String> = emptyMap(),
            method: String = "POST",
            allowInsecure: Boolean = false,
            onComplete: (success: Boolean, statusCode: Int?, response: String?, reason: String?) -> Unit
        ) {
            var success = false
            var statusCode: Int? = null
            var responseText: String? = null
            var reason: String? = null

            try {
                if (!file.exists() || !file.canRead()) {
                    LOG.error("File does not exist or cannot be read: ${file.path}")
                    reason = file.name
                } else {
                    val fileName = file.name
                    val mimeType = when (fileName.substringAfterLast('.', "").lowercase()) {
                        "gpx" -> "application/gpx+xml"
                        else -> "application/octet-stream"
                    }

                    val boundary = "----GadgetbridgeFormBoundary${System.currentTimeMillis()}"
                    val multipartBodyBytes = buildMultipartBody(file, fileName, mimeType, boundary)

                    val headers = requestHeaders.toMutableMap()
                    headers["Content-Type"] = "multipart/form-data; boundary=$boundary"

                    val response = if (GBApplication.hasDirectInternetAccess()) {
                        directBinaryRequest(
                            uri = uri,
                            method = method,
                            requestHeaders = headers,
                            body = multipartBodyBytes,
                            allowInsecure = allowInsecure
                        )
                    } else {
                        InternetHelperSingleton.send(
                            uri,
                            HttpRequest.Method.valueOf(method),
                            headers,
                            multipartBodyBytes,
                            allowInsecure
                        )
                    }

                    statusCode = response?.statusCode
                    responseText = response?.data?.bufferedReader()?.use { it.readText() }
                    success = response != null
                    if (!success) {
                        reason = networkFailureReason(null)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Uploading $uri failed: ", e)
                reason = networkFailureReason(e)
            }

            onComplete(success, statusCode, responseText, reason)
        }

        /**
         * Localized, user-facing reason for a failed request to an online tracker (Endurain,
         * Wanderer, …) when there was no usable response. Distinguishes "no internet access at
         * all" (no permission / internet helper unavailable / offline) from "server unreachable".
         * Shared by every online-tracker setup/connect path so the messaging stays consistent.
         */
        fun connectFailureReason(context: android.content.Context, serverUrl: String?): String {
            if (!GBApplication.hasInternetAccess()) {
                return noInternetAccessReason(context)
            }
            return if (serverUrl != null) {
                context.getString(R.string.toast_error_could_not_reach_server, serverUrl)
            } else {
                context.getString(R.string.toast_error_server_unreachable)
            }
        }

        /**
         * Why there is no internet access, made actionable: if the Internet Helper app is
         * installed but its (dangerous, runtime) permission was never granted, tell the user to
         * grant it — otherwise point them at the helper itself.
         */
        private fun noInternetAccessReason(context: android.content.Context): String {
            return when {
                !AndroidUtils.isPackageInstalled(PermissionsUtils.PACKAGE_INTERNET_HELPER) ->
                    context.getString(R.string.internet_helper_error_no_internet_helper)
                !PermissionsUtils.checkPermission(context, PermissionsUtils.CUSTOM_PERM_INTERNET_HELPER) ->
                    context.getString(R.string.internet_helper_error_permission)
                else ->
                    context.getString(R.string.internet_helper_error_no_internet_helper)
            }
        }

        /**
         * Builds a localized, user-facing reason for an upload/network failure. First distinguishes
         * "no internet connection" from a reachable-network failure, then maps the exception type to
         * an actionable message (host not resolved / connection refused / timed out / TLS error).
         */
        fun networkFailureReason(e: Throwable?): String {
            val context = GBApplication.getContext()
            if (!GBApplication.hasInternetAccess()) {
                // No direct permission and no usable internet helper (uninstalled, disabled, or
                // its runtime permission not granted) — give the actionable reason.
                return noInternetAccessReason(context)
            }
            return when (e) {
                is UnknownHostException ->
                    context.getString(R.string.toast_error_could_not_resolve_host)
                is ConnectException ->
                    context.getString(R.string.toast_error_connection_refused)
                is SocketTimeoutException ->
                    context.getString(R.string.toast_error_connection_timed_out)
                is SSLException ->
                    context.getString(R.string.toast_error_tls)
                else -> e?.localizedMessage
                    ?: context.getString(R.string.toast_error_server_unreachable)
            }
        }

        private fun buildMultipartBody(
            file: File,
            fileName: String,
            mimeType: String,
            boundary: String
        ): ByteArray {
            val fileBytes = file.readBytes()
            val output = ByteArrayOutputStream()

            val header = "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n" +
                "Content-Type: $mimeType\r\n" +
                "\r\n"
            output.write(header.toByteArray(Charsets.US_ASCII))
            output.write(fileBytes)
            output.write("\r\n--$boundary--\r\n".toByteArray(Charsets.US_ASCII))

            return output.toByteArray()
        }

        /**
         * Direct HTTP request using OkHttp with a binary body.
         */
        private fun directBinaryRequest(
            uri: Uri,
            method: String,
            requestHeaders: Map<String, String>,
            body: ByteArray,
            allowInsecure: Boolean
        ): WebResourceResponse {
            val client = if (allowInsecure) createInsecureClient() else defaultClient
            val builder = Request.Builder().url(uri.toString())

            for ((key, value) in headersWithUserAgent(requestHeaders)) {
                builder.addHeader(key, value)
            }

            val contentType = getHeader(requestHeaders, "content-type") ?: "application/octet-stream"
            val requestBody = body.toRequestBody(contentType.toMediaType())
            builder.method(method.uppercase(), requestBody)

            client.newCall(builder.build()).execute().use { response ->
                val statusCode = response.code
                val message = if (!response.message.isEmpty()) response.message else "OK"
                val headers = response.headers.toMap()
                val respContentType = response.header("content-type") ?: "application/octet-stream"
                val encoding = response.header("content-encoding") ?: "UTF-8"
                return WebResourceResponse(
                    respContentType,
                    encoding,
                    statusCode,
                    message,
                    headers,
                    ByteArrayInputStream(response.body.bytes())
                )
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
                    val contentType = getHeader(requestHeaders, "content-type") ?: "application/octet-stream"
                    body.toRequestBody(contentType.toMediaType())
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

        private fun getHeader(headers: Map<String, String>, key: String): String? {
            for (e in headers) {
                if (e.key.equals(key, ignoreCase = true)) {
                    return e.value
                }
            }

            return null
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
