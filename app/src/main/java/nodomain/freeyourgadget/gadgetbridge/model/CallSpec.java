/*  Copyright (C) 2016-2024 Andreas Shimokawa, Davis Mosenkovs, Dmitry
    Markin, mvn23

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

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.util.GBToStringBuilder;

public class CallSpec {
    // TODO: Migrate all usages to the enum..
    public static final int CALL_UNDEFINED = 0;
    public static final int CALL_ACCEPT = 1;
    public static final int CALL_INCOMING = 2;
    public static final int CALL_OUTGOING = 3;
    public static final int CALL_REJECT = 4;
    public static final int CALL_START = 5;
    public static final int CALL_END = 6;

    public String number;
    public String name;

    public String sourceName;

    /**
     * The application that generated the notification.
     */
    public String sourceAppId;

    public String key;
    public String channelId;
    public String category;

    public boolean isVoip = false;

    public int command;
    public int dndSuppressed;

    public enum Command {
        UNDEFINED,
        ACCEPT,
        INCOMING,
        OUTGOING,
        REJECT,
        START,
        END,
    }

    @NonNull
    @Override
    public String toString() {
        final GBToStringBuilder tsb = new GBToStringBuilder(this);
        tsb.append("command", Command.values()[command]);
        tsb.append("number", number);
        tsb.append("name", name);
        tsb.append("sourceName", sourceName);
        tsb.append("sourceAppId", sourceAppId);
        tsb.append("key", key);
        tsb.append("channelId", channelId);
        tsb.append("category", category);
        if (isVoip) {
            tsb.append("isVoip", isVoip);
        }
        if (dndSuppressed != 0) {
            tsb.append("dndSuppressed", dndSuppressed);
        }
        return tsb.toString();
    }
}
