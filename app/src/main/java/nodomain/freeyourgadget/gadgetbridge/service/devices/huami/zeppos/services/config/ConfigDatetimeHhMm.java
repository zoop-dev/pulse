package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

@SuppressWarnings("ClassCanBeRecord")
public class ConfigDatetimeHhMm {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigDatetimeHhMm.class);

    final String value;

    public ConfigDatetimeHhMm(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConfigDatetimeHhMm consume(final ByteBuffer buf) {
        final DateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
        final String hh_mm = String.format(Locale.ROOT, "%02d:%02d", buf.get(), buf.get());
        try {
            df.parse(hh_mm);
        } catch (final ParseException e) {
            LOG.error("Failed to parse HH:mm from {}", hh_mm);
            return null;
        }
        return new ConfigDatetimeHhMm(hh_mm);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("ConfigDatetimeHhMm{value=%s}", value);
    }
}
