/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Dmitry
    Markin, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.model;

import androidx.annotation.StringRes;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.R;

public interface Alarm extends Serializable {
    /**
     * The {@link android.os.Bundle} name for transferring parceled alarms.
     */
    String EXTRA_ALARM = "alarm";

    byte ALARM_ONCE = 0;
    byte ALARM_MON = 1;
    byte ALARM_TUE = 2;
    byte ALARM_WED = 4;
    byte ALARM_THU = 8;
    byte ALARM_FRI = 16;
    byte ALARM_SAT = 32;
    byte ALARM_SUN = 64;

    byte ALARM_DAILY = Alarm.ALARM_MON | Alarm.ALARM_TUE | Alarm.ALARM_WED | Alarm.ALARM_THU | Alarm.ALARM_FRI | Alarm.ALARM_SAT | Alarm.ALARM_SUN;

    enum ALARM_SOUND {
        UNSET(R.string.unset),
        OFF(R.string.off),
        TONE(R.string.pref_header_sound),
        VIBRATION(R.string.title_activity_vibration),
        TONE_AND_VIBRATION(R.string.sound_and_vibration),
        ;

        @StringRes
        private final int label;

        ALARM_SOUND(final int label) {
            this.label = label;
        }

        public int getLabel() {
            return label;
        }
    }

    /// Enum name will be persisted as the title
    enum ALARM_LABEL {
        NONE(R.string.none),
        WAKE_UP(R.string.wake_up_time),
        WORKOUT(R.string.pref_header_workout),
        REMINDER(R.string.reminder),
        APPOINTMENT(R.string.alarm_label_appointment),
        TRAINING(R.string.alarm_label_training),
        CLASS(R.string.alarm_label_class),
        MEDITATE(R.string.alarm_label_meditate),
        BEDTIME(R.string.bedtime),
        ;

        @StringRes
        private final int label;

        ALARM_LABEL(final int label) {
            this.label = label;
        }

        public int getLabel() {
            return label;
        }
    }

    int getPosition();

    boolean getEnabled();

    boolean getUnused();

    boolean getSmartWakeup();

    Integer getSmartWakeupInterval();

    boolean getSnooze();

    int getRepetition();

    boolean isRepetitive();

    boolean getRepetition(int dow);

    int getHour();

    int getMinute();

    String getTitle();

    String getDescription();

    int getSoundCode();

    boolean getBacklight();
}
