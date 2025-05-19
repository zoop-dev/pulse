/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.annotation.SuppressLint;

import com.android.volley.toolbox.HurlStack;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class VolleyUtils {
    public static HurlStack createInsecureHurlStack() throws Exception {
        // Trust all certificates
        final TrustManager[] trustAllCerts = new TrustManager[]{new InsecureTrustManager()};
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        return new HurlStack(null, sslContext.getSocketFactory()) {
            @Override
            protected HttpURLConnection createConnection(final URL url) throws IOException {
                final HttpURLConnection httpURLConnection = super.createConnection(url);
                if (httpURLConnection instanceof HttpsURLConnection httpsURLConnection) {
                    // Trust all hostnames
                    httpsURLConnection.setHostnameVerifier((hostname, session) -> true);
                }
                return httpURLConnection;
            }
        };
    }

    @SuppressLint("CustomX509TrustManager")
    public static class InsecureTrustManager implements X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }
}
