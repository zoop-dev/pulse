/*  Copyright (C) 2024 Damien Gaignon, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HuaweiUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiUtil.class);


    // NOTE: List can be incomplete
    private static final Map<Integer, String[]> regions = new HashMap<>();
    static {
        // China
        regions.put(1, new String[] {"CN"});
        // Asia, Africa, Latin America
        regions.put(5, new String[] {"AE","AG","AI","AM","AR","AW","AZ","BD","BF","BH","BI","BJ","BN","BO","BR","BW","BY","BZ","CD","CF","CG","CI","CK","CL","CM","CO","CR","CV","DJ","DO","DZ","EC","EG","ER","ET","FJ","GA","GD","GE","GF","GH","GM","GN","GP","GQ","GT","GW","GY","HK","HN","ID","IN","IQ","JM","JO","JP","KE","KG","KH","KM","KR","KW","KY","KZ","LA","LB","LC","LK","LR","LS","MA","MG","ML","MM","MN","MO","MQ","MR","MS","MU","MV","MW","MX","MY","MZ","NA","NE","NG","NI","NP","NR","OM","PA","PE","PF","PG","PH","PK","PR","PS","PY","QA","RE","SA","SB","SC","SG","SL","SN","SO","SR","ST","SV","SZ","TD","TG","TH","TJ","TN","TO","TT","TW","TZ","UG","UY","UZ","VE","VG","VN","YT","ZA","ZM","ZW"});
        // Europe
        regions.put(7, new String[] {"AD","AL","AT","AU","BA","BE","BG","CA","CH","CY","CZ","DE","DK","EE","ES","FI","FO","FR","GB","GL","GR","HR","HU","IE","IS","IT","LI","LT","LU","LV","MC","MD","ME","MK","MT","NL","NO","NZ","PL","PT","RO","RS","SE","SI","SK","SM","TR","UA","VA"});
        // ru
        regions.put(8, new String[] {"RU"});
    }

    public static int getSiteIdByCountryCode(final String countryCode) {
        if(TextUtils.isEmpty(countryCode)) {
            return 0;
        }
        for(Map.Entry<Integer, String[]> r: regions.entrySet()) {
            if(Arrays.asList(r.getValue()).contains(countryCode)) {
                return r.getKey();
            }
        }
        return 0;
    }

    @NonNull
    public static Map<String, String> getCountriesMap() {
        Map<String,String> countries = new TreeMap<>();
        Locale[] locales = Locale.getAvailableLocales();
        for (Locale locale : locales) {
            String country = locale.getDisplayCountry();
            String countryCode = locale.getCountry();
            if (!country.trim().isEmpty() && countryCode.matches("^[A-Z][A-Z]$") && !countries.containsKey(country)) {
                countries.put(country + " (" + countryCode + ")", countryCode);
            }
        }
        return countries;
    }

    public static byte[] timeToByte(String time) {
        Calendar calendar = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        try {
            Date t = df.parse(time);
            assert t != null;
            calendar.setTime(t);
        } catch (ParseException e) {
            LOG.error("Time to Byte conversion error", e);
            return null;
        }
        return new byte[]{
            (byte)calendar.get(Calendar.HOUR_OF_DAY),
            (byte)calendar.get(Calendar.MINUTE)};
    }

    public static byte[] getTimeAndZoneId(final Calendar now) {
        int zoneRawOffset = (now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET)) / 1000;
        byte[] id = now.getTimeZone().getID().getBytes();
        return ByteBuffer.allocate(6 + id.length)
            .putInt((int)(now.getTimeInMillis() / 1000))
            .put((byte)(zoneRawOffset < 0 ? (-zoneRawOffset / 3600 + 128) : zoneRawOffset / 3600) )
            .put((byte)(zoneRawOffset / 60 % 60))
            .put(id)
            .array();
    }

    public static double convBytes2Double(byte[] b) {
        return ByteBuffer.wrap(b).getDouble();
    }
}
