/*  Copyright (C) 2022-2024 narektor, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_2_NOISE_CONTROL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOLUME;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_ANC_LEVEL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_BALANCE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_NOISE_CONTROL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_ANC;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_AMBIENT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_ADAPTIVE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_OFF;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_ANC;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_AMBIENT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_ADAPTIVE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_OFF;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_3_PRO_MEDIA_CONTROLS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_3_PRO_ANSWER_CALL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_3_PRO_DECLINE_CALL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_3_PRO_EARBUD_LIGHTS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_3_PRO_ANC_LEVEL;

import android.os.Parcel;

import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class GalaxyBudsSettingsCustomizer implements DeviceSpecificSettingsCustomizer {

    final GBDevice device;

    public GalaxyBudsSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, Prefs prefs, final String rootKey) {
        final Preference pref_galaxy_buds_pro_balance = handler.findPreference(PREF_GALAXY_BUDS_PRO_BALANCE);
        if (pref_galaxy_buds_pro_balance != null) {
            pref_galaxy_buds_pro_balance.setSummary(String.valueOf((prefs.getInt(PREF_GALAXY_BUDS_PRO_BALANCE, 16) - 16)));

            pref_galaxy_buds_pro_balance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    String newValue = String.valueOf((int) newVal - 16);
                    pref_galaxy_buds_pro_balance.setSummary(newValue);
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_PRO_BALANCE);
                    return true;
                }
            });
        }

        final Preference pref_galaxy_buds_pro_noise_control = handler.findPreference(PREF_GALAXY_BUDS_PRO_NOISE_CONTROL);
        String pref_galaxy_buds_pro_noise_control_value = prefs.getString(PREF_GALAXY_BUDS_PRO_NOISE_CONTROL, "0");
        final Preference pref_galaxy_buds_2_noise_control = handler.findPreference(PREF_GALAXY_BUDS_2_NOISE_CONTROL);
        String pref_galaxy_buds_2_noise_control_value = prefs.getString(PREF_GALAXY_BUDS_2_NOISE_CONTROL, "0");
        final Preference pref_galaxy_buds_pro_anc_level = handler.findPreference(PREF_GALAXY_BUDS_PRO_ANC_LEVEL);
        final Preference pref_galaxy_buds_ambient_volume = handler.findPreference(PREF_GALAXY_BUDS_AMBIENT_VOLUME);

        if (pref_galaxy_buds_pro_noise_control != null) {

            if (pref_galaxy_buds_pro_anc_level != null && pref_galaxy_buds_ambient_volume != null) {
                switch (pref_galaxy_buds_pro_noise_control_value) {
                    case "0":
                        pref_galaxy_buds_pro_anc_level.setEnabled(false);
                        pref_galaxy_buds_ambient_volume.setEnabled(false);
                        break;
                    case "1":
                        pref_galaxy_buds_pro_anc_level.setEnabled(true);
                        pref_galaxy_buds_ambient_volume.setEnabled(false);
                        break;
                    case "2":
                        pref_galaxy_buds_pro_anc_level.setEnabled(false);
                        pref_galaxy_buds_ambient_volume.setEnabled(true);
                        break;
                }
            }

            pref_galaxy_buds_pro_noise_control.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_PRO_NOISE_CONTROL);
                    switch (newVal.toString()) {
                        case "0":
                            if (pref_galaxy_buds_pro_anc_level != null)
                                pref_galaxy_buds_pro_anc_level.setEnabled(false);
                            if (pref_galaxy_buds_ambient_volume != null)
                                pref_galaxy_buds_ambient_volume.setEnabled(false);
                            break;
                        case "1":
                            if (pref_galaxy_buds_pro_anc_level != null)
                                pref_galaxy_buds_pro_anc_level.setEnabled(true);
                            if (pref_galaxy_buds_ambient_volume != null)
                                pref_galaxy_buds_ambient_volume.setEnabled(false);
                            break;
                        case "2":
                            if (pref_galaxy_buds_pro_anc_level != null)
                                pref_galaxy_buds_pro_anc_level.setEnabled(false);
                            if (pref_galaxy_buds_ambient_volume != null)
                                pref_galaxy_buds_ambient_volume.setEnabled(true);
                            break;
                    }

                    return true;
                }
            });
        }

        if (pref_galaxy_buds_2_noise_control != null) {

            switch (pref_galaxy_buds_2_noise_control_value) {
                case "0":
                case "1":
                    pref_galaxy_buds_ambient_volume.setEnabled(false);
                    break;
                case "2":
                    pref_galaxy_buds_ambient_volume.setEnabled(true);
                    break;
            }

            pref_galaxy_buds_2_noise_control.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_2_NOISE_CONTROL);
                    switch (newVal.toString()) {
                        case "0":
                        case "1":
                            pref_galaxy_buds_ambient_volume.setEnabled(false);
                            break;
                        case "2":
                            pref_galaxy_buds_ambient_volume.setEnabled(true);
                            break;
                    }

                    return true;
                }
            });
        }

        final Preference pref_galaxy_buds_touch_right = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_RIGHT);
        String pref_galaxy_buds_touch_right_value = prefs.getString(PREF_GALAXY_BUDS_TOUCH_RIGHT, "1");
        final Preference pref_galaxy_buds_touch_right_switch = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH);
        
        // For Buds3 Pro: checkbox-based switch controls (Buds2 Pro uses ListPreference)
        final Preference pref_galaxy_buds_touch_right_switch_anc = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_ANC);
        final Preference pref_galaxy_buds_touch_right_switch_ambient = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_AMBIENT);
        final Preference pref_galaxy_buds_touch_right_switch_adaptive = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_ADAPTIVE);
        final Preference pref_galaxy_buds_touch_right_switch_off = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_OFF);
        
        // Add change listeners for Buds3 Pro checkbox preferences to notify protocol handler
        if (pref_galaxy_buds_touch_right_switch_anc != null) {
            pref_galaxy_buds_touch_right_switch_anc.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_ANC);
                    return true;
                }
            });
        }
        if (pref_galaxy_buds_touch_right_switch_ambient != null) {
            pref_galaxy_buds_touch_right_switch_ambient.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_AMBIENT);
                    return true;
                }
            });
        }
        if (pref_galaxy_buds_touch_right_switch_adaptive != null) {
            pref_galaxy_buds_touch_right_switch_adaptive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_ADAPTIVE);
                    return true;
                }
            });
        }
        if (pref_galaxy_buds_touch_right_switch_off != null) {
            pref_galaxy_buds_touch_right_switch_off.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH_OFF);
                    return true;
                }
            });
        }

        if (pref_galaxy_buds_touch_right != null) {
            boolean isNoiseControl = pref_galaxy_buds_touch_right_value.equals("2");
            
            // Enable/disable old-style switch preference (for older models)
            if (pref_galaxy_buds_touch_right_switch != null) {
                pref_galaxy_buds_touch_right_switch.setEnabled(isNoiseControl);
            }
            
            // Enable/disable Buds3 Pro checkboxes (Buds2 Pro uses old ListPreference)
            if (pref_galaxy_buds_touch_right_switch_anc != null) {
                pref_galaxy_buds_touch_right_switch_anc.setEnabled(isNoiseControl);
            }
            if (pref_galaxy_buds_touch_right_switch_ambient != null) {
                pref_galaxy_buds_touch_right_switch_ambient.setEnabled(isNoiseControl);
            }
            if (pref_galaxy_buds_touch_right_switch_adaptive != null) {
                pref_galaxy_buds_touch_right_switch_adaptive.setEnabled(isNoiseControl);
            }
            if (pref_galaxy_buds_touch_right_switch_off != null) {
                pref_galaxy_buds_touch_right_switch_off.setEnabled(isNoiseControl);
            }

            pref_galaxy_buds_touch_right.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_TOUCH_RIGHT);
                    boolean enable = newVal.toString().equals("2");
                    
                    // Update old-style switch
                    if (pref_galaxy_buds_touch_right_switch != null) {
                        pref_galaxy_buds_touch_right_switch.setEnabled(enable);
                    }
                    
                    // Update Buds3 Pro checkboxes
                    if (pref_galaxy_buds_touch_right_switch_anc != null) {
                        pref_galaxy_buds_touch_right_switch_anc.setEnabled(enable);
                    }
                    if (pref_galaxy_buds_touch_right_switch_ambient != null) {
                        pref_galaxy_buds_touch_right_switch_ambient.setEnabled(enable);
                    }
                    if (pref_galaxy_buds_touch_right_switch_adaptive != null) {
                        pref_galaxy_buds_touch_right_switch_adaptive.setEnabled(enable);
                    }
                    if (pref_galaxy_buds_touch_right_switch_off != null) {
                        pref_galaxy_buds_touch_right_switch_off.setEnabled(enable);
                    }

                    return true;
                }
            });
        }

        final Preference pref_galaxy_buds_touch_left = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_LEFT);
        String pref_galaxy_buds_touch_left_value = prefs.getString(PREF_GALAXY_BUDS_TOUCH_LEFT, "1");
        final Preference pref_galaxy_buds_touch_left_switch = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH);
        
        // For Buds3 Pro: checkbox-based switch controls (Buds2 Pro uses ListPreference)
        final Preference pref_galaxy_buds_touch_left_switch_anc = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_ANC);
        final Preference pref_galaxy_buds_touch_left_switch_ambient = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_AMBIENT);
        final Preference pref_galaxy_buds_touch_left_switch_adaptive = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_ADAPTIVE);
        final Preference pref_galaxy_buds_touch_left_switch_off = handler.findPreference(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_OFF);
        
        // Add change listeners for Buds3 Pro checkbox preferences to notify protocol handler
        if (pref_galaxy_buds_touch_left_switch_anc != null) {
            pref_galaxy_buds_touch_left_switch_anc.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_ANC);
                    return true;
                }
            });
        }
        if (pref_galaxy_buds_touch_left_switch_ambient != null) {
            pref_galaxy_buds_touch_left_switch_ambient.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_AMBIENT);
                    return true;
                }
            });
        }
        if (pref_galaxy_buds_touch_left_switch_adaptive != null) {
            pref_galaxy_buds_touch_left_switch_adaptive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_ADAPTIVE);
                    return true;
                }
            });
        }
        if (pref_galaxy_buds_touch_left_switch_off != null) {
            pref_galaxy_buds_touch_left_switch_off.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH_OFF);
                    return true;
                }
            });
        }

        if (pref_galaxy_buds_touch_left != null) {
            boolean isNoiseControl = pref_galaxy_buds_touch_left_value.equals("2");
            
            // Enable/disable old-style switch preference (for older models)
            if (pref_galaxy_buds_touch_left_switch != null) {
                pref_galaxy_buds_touch_left_switch.setEnabled(isNoiseControl);
            }
            
            // Enable/disable Buds3 Pro checkboxes (Buds2 Pro uses old ListPreference)
            if (pref_galaxy_buds_touch_left_switch_anc != null) {
                pref_galaxy_buds_touch_left_switch_anc.setEnabled(isNoiseControl);
            }
            if (pref_galaxy_buds_touch_left_switch_ambient != null) {
                pref_galaxy_buds_touch_left_switch_ambient.setEnabled(isNoiseControl);
            }
            if (pref_galaxy_buds_touch_left_switch_adaptive != null) {
                pref_galaxy_buds_touch_left_switch_adaptive.setEnabled(isNoiseControl);
            }
            if (pref_galaxy_buds_touch_left_switch_off != null) {
                pref_galaxy_buds_touch_left_switch_off.setEnabled(isNoiseControl);
            }

            pref_galaxy_buds_touch_left.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_TOUCH_LEFT);
                    boolean enable = newVal.toString().equals("2");
                    
                    // Update old-style switch
                    if (pref_galaxy_buds_touch_left_switch != null) {
                        pref_galaxy_buds_touch_left_switch.setEnabled(enable);
                    }
                    
                    // Update Buds3 Pro checkboxes
                    if (pref_galaxy_buds_touch_left_switch_anc != null) {
                        pref_galaxy_buds_touch_left_switch_anc.setEnabled(enable);
                    }
                    if (pref_galaxy_buds_touch_left_switch_ambient != null) {
                        pref_galaxy_buds_touch_left_switch_ambient.setEnabled(enable);
                    }
                    if (pref_galaxy_buds_touch_left_switch_adaptive != null) {
                        pref_galaxy_buds_touch_left_switch_adaptive.setEnabled(enable);
                    }
                    if (pref_galaxy_buds_touch_left_switch_off != null) {
                        pref_galaxy_buds_touch_left_switch_off.setEnabled(enable);
                    }

                    return true;
                }
            });
        }

        // ANC level change listener
        if (pref_galaxy_buds_pro_anc_level != null) {
            pref_galaxy_buds_pro_anc_level.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_PRO_ANC_LEVEL);
                    return true;
                }
            });
        }

        // Galaxy Buds3 Pro specific preferences - add change listeners
        final Preference pref_galaxy_buds_3_pro_media_controls = handler.findPreference(PREF_GALAXY_BUDS_3_PRO_MEDIA_CONTROLS);
        if (pref_galaxy_buds_3_pro_media_controls != null) {
            pref_galaxy_buds_3_pro_media_controls.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_3_PRO_MEDIA_CONTROLS);
                    return true;
                }
            });
        }
        
        final Preference pref_galaxy_buds_3_pro_answer_call = handler.findPreference(PREF_GALAXY_BUDS_3_PRO_ANSWER_CALL);
        if (pref_galaxy_buds_3_pro_answer_call != null) {
            pref_galaxy_buds_3_pro_answer_call.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_3_PRO_ANSWER_CALL);
                    return true;
                }
            });
        }
        
        final Preference pref_galaxy_buds_3_pro_decline_call = handler.findPreference(PREF_GALAXY_BUDS_3_PRO_DECLINE_CALL);
        if (pref_galaxy_buds_3_pro_decline_call != null) {
            pref_galaxy_buds_3_pro_decline_call.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_3_PRO_DECLINE_CALL);
                    return true;
                }
            });
        }
        
        final Preference pref_galaxy_buds_3_pro_earbud_lights = handler.findPreference(PREF_GALAXY_BUDS_3_PRO_EARBUD_LIGHTS);
        if (pref_galaxy_buds_3_pro_earbud_lights != null) {
            pref_galaxy_buds_3_pro_earbud_lights.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_3_PRO_EARBUD_LIGHTS);
                    return true;
                }
            });
        }
        
        final Preference pref_galaxy_buds_3_pro_anc_level = handler.findPreference(PREF_GALAXY_BUDS_3_PRO_ANC_LEVEL);
        if (pref_galaxy_buds_3_pro_anc_level != null) {
            pref_galaxy_buds_3_pro_anc_level.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_3_PRO_ANC_LEVEL);
                    return true;
                }
            });
        }

/*
        final Preference pref_galaxy_buds_ambient_mode = handler.findPreference(PREF_GALAXY_BUDS_AMBIENT_SOUND);
        boolean is_pref_galaxy_buds_ambient_mode_enabled = prefs.getBoolean(PREF_GALAXY_BUDS_AMBIENT_SOUND, false);
        final Preference pref_galaxy_buds_ambient_voice_focus_preference = handler.findPreference(PREF_GALAXY_BUDS_AMBIENT_VOICE_FOCUS_PREFERENCE);

        if (pref_galaxy_buds_ambient_mode != null) {
            if (is_pref_galaxy_buds_ambient_mode_enabled) {
                pref_galaxy_buds_ambient_voice_focus_preference.setEnabled(true);
            } else {
                pref_galaxy_buds_ambient_voice_focus_preference.setEnabled(false);
            }


            pref_galaxy_buds_ambient_mode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    handler.notifyPreferenceChanged(PREF_GALAXY_BUDS_AMBIENT_SOUND);
                    if ((boolean) newVal) {
                        pref_galaxy_buds_ambient_voice_focus_preference.setEnabled(true);
                    } else {
                        pref_galaxy_buds_ambient_voice_focus_preference.setEnabled(false);
                    }

                    return true;
                }
            });
        }

 */
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }


    public static final Creator<GalaxyBudsSettingsCustomizer> CREATOR = new Creator<GalaxyBudsSettingsCustomizer>() {
        @Override
        public GalaxyBudsSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(GalaxyBudsSettingsCustomizer.class.getClassLoader());
            return new GalaxyBudsSettingsCustomizer(device);
        }

        @Override
        public GalaxyBudsSettingsCustomizer[] newArray(final int size) {
            return new GalaxyBudsSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(device, 0);
    }
}
