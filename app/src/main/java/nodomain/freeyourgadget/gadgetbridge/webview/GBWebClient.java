/*  Copyright (C) 2017-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Pavel Elagin

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
package nodomain.freeyourgadget.gadgetbridge.webview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.RemoteException;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SunriseTransitSet;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.internet.InternetFirewall;
import nodomain.freeyourgadget.gadgetbridge.internet.InternetRequestType;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.weather.WeatherMapper;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.InternetHelperSingleton;
import nodomain.freeyourgadget.internethelper.aidl.http.HttpRequest;

public class GBWebClient extends WebViewClient {
    private static final Logger LOG = LoggerFactory.getLogger(GBWebClient.class);

    private final InternetFirewall firewall;

    private final String[] LocallySupportedDomains = new String[]{
            "openweathermap.org",   //for weather :)
            "rawgit.com",           //for trekvolle
    };

    private final Map<String, List<Entry>> postData = new HashMap<>();
    private record Entry(long timestamp, String body) {}

    public GBWebClient(final InternetRequestType type, @NotNull final GBDevice device) {
        super();
        this.firewall = new InternetFirewall(type, device);
    }

    /**
     * Stores HTTP POST body strings for later retrieval in mimicReply()
     */
    public synchronized void storePostBody(String url, String body) {
        long now = System.currentTimeMillis();

        // Cleanup expired entries (more than a minute old)
        Iterator<Map.Entry<String, List<Entry>>> mapIt = postData.entrySet().iterator();
        while (mapIt.hasNext()) {
            Map.Entry<String, List<Entry>> mapEntry = mapIt.next();
            List<Entry> list = mapEntry.getValue();
            list.removeIf(e -> now - e.timestamp > 60*1000);
            if (list.isEmpty()) {
                mapIt.remove();
            }
        }

        // Add new entry
        postData
                .computeIfAbsent(url, k -> new ArrayList<>())
                .add(new Entry(now, body));
    }

    /**
     * Returns the earliest known HTTP POST body for a certain URL
     */
    private synchronized String getFirstPostForUrl(String url) {
        List<Entry> list = postData.get(url);
        if (list == null || list.isEmpty()) {
            return null;
        }

        Entry first = list.remove(0);
        if (list.isEmpty()) {
            postData.remove(url);
        }

        return first.body;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        Uri parsedUri = request.getUrl();
        LOG.debug("WEBVIEW shouldInterceptRequest URL: {} (method {})", parsedUri.toString(), request.getMethod());
        WebResourceResponse mimickedReply = mimicReply(parsedUri, request.getMethod(), request.getRequestHeaders());
        if (mimickedReply != null)
            return mimickedReply;
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        LOG.debug("WEBVIEW shouldInterceptRequest URL (legacy): {}", url);
        Uri parsedUri = Uri.parse(url);
        WebResourceResponse mimickedReply = mimicReply(parsedUri, "GET", new HashMap<>());
        if (mimickedReply != null)
            return mimickedReply;
        return super.shouldInterceptRequest(view, url);
    }

    private WebResourceResponse mimicReply(Uri requestedUri, String method, Map<String, String> requestHeaders) {
        GBPrefs prefs = GBApplication.getPrefs();
        boolean locallySupported = StringUtils.indexOfAny(requestedUri.getHost(), LocallySupportedDomains) != -1;
        boolean urlIsAllowed = locallySupported;
        boolean matchFound = false;

        // Allow full access to internet when available
        boolean directInternetAccess = GBApplication.hasDirectInternetAccess();
        if (directInternetAccess && !locallySupported) {
            return null;
        }

        // Handle local schemes locally
        if (requestedUri.toString().startsWith("file://") || requestedUri.toString().startsWith("gadgetbridge://")) {
            return null;
        }

        // Handle predefined groups
        urlIsAllowed = firewall.isAllowed(requestedUri);

        // Handle OpenWeatherMap locally
        boolean forceLocal = false;
        if (locallySupported && prefs.getBoolean("pref_key_internethelper_force_local", true)) {
            urlIsAllowed = true;
            forceLocal = true;
        }

        // Handle request
        if (requestedUri.getHost() != null && urlIsAllowed) {
            if (!forceLocal && !directInternetAccess && InternetHelperSingleton.INSTANCE.ensureInternetHelperBound()) {
                LOG.debug("WEBVIEW forwarding request to the internet helper");
                try {
                    HttpRequest.Method requestMethod = HttpRequest.Method.valueOf(method);
                    String body = null;
                    if (requestMethod == HttpRequest.Method.POST) {
                        body = getFirstPostForUrl(requestedUri.toString());
                        LOG.debug("WEBVIEW POSTing with body: {}", body);
                    }
                    WebResourceResponse wrr = InternetHelperSingleton.INSTANCE.send(requestedUri, requestMethod, requestHeaders, body != null ? body.getBytes(StandardCharsets.UTF_8) : null, false);
                    if (wrr != null && wrr.getStatusCode() < 400)
                        return wrr;
                    else
                        return null;
                } catch (RemoteException | InterruptedException e) {
                    LOG.warn("Error downloading data from {}", requestedUri, e);
                }
            } else {
                if (StringUtils.endsWith(requestedUri.getHost(), "openweathermap.org")){
                    LOG.debug("WEBVIEW request to openweathermap.org detected of type: {} params: {}", requestedUri.getPath(), requestedUri.getQuery());
                    return mimicOpenWeatherMapResponse(requestedUri.getPath(), requestedUri.getQueryParameter("units"));
                } else if (StringUtils.endsWith(requestedUri.getHost(), "rawgit.com")) {
                    LOG.debug("WEBVIEW request to rawgit.com detected of type: {} params: {}", requestedUri.getPath(), requestedUri.getQuery());
                    return mimicRawGitResponse(requestedUri.getPath());
                } else {
                    LOG.debug("WEBVIEW request to allowed domain detected but not intercepted: {}", requestedUri);
                }
            }
        } else {
            LOG.debug("WEBVIEW request not intercepted: {}", requestedUri);
        }
        return null;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri parsedUri = Uri.parse(url);

        if (parsedUri.getScheme().startsWith("http")) {
            if (GBApplication.hasInternetAccess()) {
                view.loadUrl(url);
            } else {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                GBApplication.getContext().startActivity(i);
            }
        } else if (parsedUri.getScheme().startsWith("gadgetbridge")) {
            url = url.replaceFirst("^gadgetbridge://.*json=", "file:///android_asset/app_config/configure.html?config=true&json=");
            view.loadUrl(url);
        } else if (parsedUri.getScheme().startsWith("pebblejs")) {
            url = url.replaceFirst("^pebblejs://close#", "file:///android_asset/app_config/configure.html?config=true&json=");
            view.loadUrl(url);
        } else if (parsedUri.getScheme().equals("data")) { //clay
            view.loadUrl(url);
        } else {
            LOG.debug("WEBVIEW Ignoring unhandled scheme: {}", parsedUri.getScheme());
        }

        return true;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        injectPostInterceptor(view);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        injectPostInterceptor(view);
    }

    private void injectPostInterceptor(WebView view) {
        // Inject JavaScript for capturing POST body data, which we need when forwarding
        // requests to the internet helper
        view.evaluateJavascript(
            """
            (function () {
               if (window.__p) return; window.__p = 1;
               const sb = b => b instanceof FormData
                 ? [...b].map(e => e.map(encodeURIComponent).join('=')).join('&')
                 : b ? b.toString() : null;

               const send = (u, b) => GBReqInt.onPostBody(new URL(u, location.href).toString(), sb(b));

               const xo = XMLHttpRequest.prototype.open;
               const xs = XMLHttpRequest.prototype.send;
               XMLHttpRequest.prototype.open = function (m, u) {
                 this._m = m; this._u = u; return xo.apply(this, arguments);
               };
               XMLHttpRequest.prototype.send = function (b) {
                 if (this._m === 'POST') send(this._u, b);
                 return xs.apply(this, arguments);
               };

               const f = window.fetch;
               window.fetch = function (i, o = {}) {
                 if ((o.method || 'GET') === 'POST')
                   send(typeof i === 'string' ? i : i.url, o.body);
                 return f.apply(this, arguments);
               };

               document.addEventListener('submit', e => {
                 const f = e.target;
                 if (f.method && f.method.toUpperCase() === 'POST')
                   send(f.action || location.href, new FormData(f));
               }, true);
            })();
            """,
            null
        );
    }

    private WebResourceResponse mimicRawGitResponse(String path) {
        if("/aHcVolle/TrekVolle/master/online.html".equals(path)) { //TrekVolle online check
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");
            return new WebResourceResponse("text/html", "utf-8", 200, "OK",
                    headers,
                    new ByteArrayInputStream("1".getBytes())
            );
        }

        return null;
    }

    private WebResourceResponse mimicOpenWeatherMapResponse(String type, String units) {

        if (Weather.getWeatherSpecs().isEmpty()) {
            LOG.warn("WEBVIEW - WeatherSpecs is empty, cannot update weather");
            return null;
        }

        CurrentPosition currentPosition = new CurrentPosition();
        WeatherSpec current = Weather.getWeatherSpec();

        try {
            JSONObject resp = createReconstructedOWMWeatherReply(current);
            if ("/data/2.5/weather".equals(type) && resp != null) {
                JSONObject main = resp.getJSONObject("main");

                convertTemps(main, units); //caller might want different units
                JSONObject wind = resp.getJSONObject("wind");
                convertSpeeds(wind, units);

                resp.put("cod", 200);
                resp.put("coord", coordObject(currentPosition));
                resp.put("sys", sysObject(currentPosition));
//            } else if ("/data/2.5/forecast".equals(type) && Weather.getWeather2().reconstructedOWMForecast != null) { //this is wrong, as we only have daily data. Unfortunately it looks like daily forecasts cannot be reconstructed
//                resp = new JSONObject(Weather.getWeather2().reconstructedOWMForecast.toString());
//
//                JSONObject city = resp.getJSONObject("city");
//                city.put("coord", coordObject(currentPosition));
//
//                JSONArray list = resp.getJSONArray("list");
//                for (int i = 0, size = list.length(); i < size; i++) {
//                    JSONObject item = list.getJSONObject(i);
//                    JSONObject main = item.getJSONObject("main");
//                    convertTemps(main, units); //caller might want different units
//                }
//
//                resp.put("cod", 200);
            } else {
                LOG.warn("WEBVIEW - cannot mimick request of type {} (unsupported or lack of data)", type);
                return null;
            }

            LOG.info("WEBVIEW - mimic openweather response {}", resp.toString());
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");

            return new WebResourceResponse("application/json", "utf-8", 200, "OK",
                    headers,
                    new ByteArrayInputStream(resp.toString().getBytes())
            );
        } catch (JSONException e) {
            LOG.warn("Error building the JSON weather message.", e);
        }

        return null;

    }

    public JSONObject createReconstructedOWMWeatherReply(WeatherSpec weatherSpec) {
        if (weatherSpec == null) {
            return null;
        }
        JSONObject reconstructedOWMWeather = new JSONObject();
        JSONArray weather = new JSONArray();
        JSONObject condition = new JSONObject();
        JSONObject main = new JSONObject();
        JSONObject wind = new JSONObject();

        try {
            condition.put("id", weatherSpec.getCurrentConditionCode());
            condition.put("main", weatherSpec.getCurrentCondition());
            condition.put("description", weatherSpec.getCurrentCondition());
            condition.put("icon", WeatherMapper.mapToOpenWeatherMapIcon(weatherSpec.getCurrentConditionCode()));
            weather.put(condition);


            main.put("temp", weatherSpec.getCurrentTemp());
            main.put("humidity", weatherSpec.getCurrentHumidity());
            main.put("temp_min", weatherSpec.getTodayMinTemp());
            main.put("temp_max", weatherSpec.getTodayMaxTemp());

            wind.put("speed", (weatherSpec.getWindSpeed() / 3.6f)); //meter per second
            wind.put("deg", weatherSpec.getWindDirection());

            reconstructedOWMWeather.put("weather", weather);
            reconstructedOWMWeather.put("main", main);
            reconstructedOWMWeather.put("name", weatherSpec.getLocation());
            reconstructedOWMWeather.put("wind", wind);

        } catch (JSONException e) {
            LOG.error("Error while reconstructing OWM weather reply");
            return null;
        }
        LOG.debug("Weather JSON for WEBVIEW: {}", reconstructedOWMWeather);
        return reconstructedOWMWeather;
    }


    private static JSONObject sysObject(CurrentPosition currentPosition) throws JSONException {
        final SunriseTransitSet sunriseTransitSet = SPA.calculateSunriseTransitSet(
                ZonedDateTime.now(),
                currentPosition.getLatitude(),
                currentPosition.getLongitude(),
                DeltaT.estimate(LocalDate.now())
        );

        JSONObject sys = new JSONObject();
        sys.put("country", "World");
        sys.put("sunrise", sunriseTransitSet.getSunrise().toInstant().getEpochSecond());
        sys.put("sunset", sunriseTransitSet.getSunset().toInstant().getEpochSecond());

        return sys;
    }

    private static void convertSpeeds(JSONObject wind, String units) throws JSONException {
        if ("metric".equals(units)) {
            wind.put("speed", (wind.getDouble("speed") * 3.6f) );
        } else if ("imperial".equals(units)) { //it's 2018... this is so sad
            wind.put("speed", (wind.getDouble("speed") * 2.237f) );
        }
    }

    private static void convertTemps(JSONObject main, String units) throws JSONException {
        if ("metric".equals(units)) {
            main.put("temp", (int) main.get("temp") - 273);
            main.put("temp_min", (int) main.get("temp_min") - 273);
            main.put("temp_max", (int) main.get("temp_max") - 273);
        } else if ("imperial".equals(units)) { //it's 2017... this is so sad
            main.put("temp", ((int) (main.get("temp")) - 273.15f) * 1.8f + 32);         // lgtm [java/integer-multiplication-cast-to-long]
            main.put("temp_min", ((int) (main.get("temp_min")) - 273.15f) * 1.8f + 32); // lgtm [java/integer-multiplication-cast-to-long]
            main.put("temp_max", ((int) (main.get("temp_max")) - 273.15f) * 1.8f + 32); // lgtm [java/integer-multiplication-cast-to-long]
        }
    }

    private static JSONObject coordObject(CurrentPosition currentPosition) throws JSONException {
        JSONObject coord = new JSONObject();
        coord.put("lat", currentPosition.getLatitude());
        coord.put("lon", currentPosition.getLongitude());
        return coord;
    }
}
