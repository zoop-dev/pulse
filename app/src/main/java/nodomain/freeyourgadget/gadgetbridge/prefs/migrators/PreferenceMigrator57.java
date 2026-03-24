package nodomain.freeyourgadget.gadgetbridge.prefs.migrators;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.prefs.AbstractPreferenceMigrator;

public class PreferenceMigrator57 extends AbstractPreferenceMigrator {
    private static final Logger LOG = LoggerFactory.getLogger(PreferenceMigrator57.class);

    @Override
    public void migrate(final int oldVersion, final SharedPreferences sharedPrefs, final SharedPreferences.Editor editor) {
        String temperatureUnit = "";
        String weightUnit = "";

        // If any device has a weight or temperature unit already configured, migrate it to global
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession daoSession = db.getDaoSession();
            final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

            for (final Device dbDevice : activeDevices) {
                final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                final String deviceWeightUnit = deviceSharedPrefs.getString("pref_weight_scale_unit", "");
                if (!deviceWeightUnit.isEmpty()) {
                    weightUnit = deviceWeightUnit;
                } else {
                    final String miScaleWeightUnit = deviceSharedPrefs.getString("pref_miscale_weight_unit", "");
                    if (!miScaleWeightUnit.isEmpty()) {
                        weightUnit = switch (miScaleWeightUnit) {
                            case "0" -> "kilogram";
                            case "1" -> "pound";
                            case "2" -> "jin";
                            default -> "";
                        };
                    }
                }
                final String deviceTemperatureUnit = deviceSharedPrefs.getString("temperature_scale_cf", "");
                if (!deviceTemperatureUnit.isEmpty()) {
                    temperatureUnit = deviceTemperatureUnit;
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to migrate prefs to version 51", e);
        }

        final String measurementSystem = sharedPrefs.getString("measurement_system", "metric");
        if (temperatureUnit.isEmpty()) {
            temperatureUnit = "metric".equals(measurementSystem) ? "celsius" : "fahrenheit";
        } else {
            // We need to convert from c/f to Celsius/Fahrenheit
            //noinspection SwitchStatementWithTooFewBranches
            temperatureUnit = switch (temperatureUnit) {
                case "f" -> "fahrenheit";
                default -> "celsius";
            };
        }
        if (weightUnit.isEmpty()) {
            weightUnit = "metric".equals(measurementSystem) ? "kilogram" : "pound";
        }
        if ("metric".equals(measurementSystem)) {
            editor.putString("unit_distance", "metric");
        } else {
            editor.putString("unit_distance", "imperial");
        }
        editor.putString("unit_temperature", temperatureUnit);
        editor.putString("unit_weight", weightUnit);
    }
}
