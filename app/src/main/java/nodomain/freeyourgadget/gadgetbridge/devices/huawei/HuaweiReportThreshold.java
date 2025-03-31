/*  Copyright (C) 2024 Me7c7

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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HuaweiReportThreshold {
    private int dataType;
    private int valueType;
    private int value;
    private int action;

    public HuaweiReportThreshold(int dataType, int valueType, int value, int action) {
        this.dataType = dataType;
        this.valueType = valueType;
        this.value = value;
        this.action = action;
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put((byte) dataType);
        buffer.put((byte) valueType);
        buffer.putShort((short) value);
        buffer.put((byte) action);
        return buffer.array();
    }

    public static List<HuaweiReportThreshold> getReportThresholds() {
        List<HuaweiReportThreshold> thresholds = new ArrayList<>();
        thresholds.add(new HuaweiReportThreshold(1,3, 500, 2));
        thresholds.add(new HuaweiReportThreshold(3,3, 100, 2));

        int current_hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int value = (current_hour < 6)?21600:3600;
        thresholds.add(new HuaweiReportThreshold(4,3, value, 2));

        return thresholds;
    }
}
