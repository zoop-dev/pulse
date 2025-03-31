/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.datasync;

import java.util.List;

public class HuaweiDataSyncCommon {

    public static final int REPLY_OK = 100000;

    public static class ConfigData {
        public int configId = -1;
        public byte configAction = -1;
        public byte[] configData;
        public long configUnknown = -1; //??
    }

    public static class ConfigCommandData {
        private int code = REPLY_OK;
        private List<ConfigData> configDataList;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public List<ConfigData> getConfigDataList() {
            return configDataList;
        }

        public void setConfigDataList(List<ConfigData> configDataList) {
            this.configDataList = configDataList;
        }
    }

    public static class EventCommandData {
        private int code = REPLY_OK;
        private int eventId = 0;
        private int eventLevel = 0;
        private byte[] data = null;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public int getEventId() {
            return eventId;
        }

        public void setEventId(int eventId) {
            this.eventId = eventId;
        }

        public int getEventLevel() {
            return eventLevel;
        }

        public void setEventLevel(int eventLevel) {
            this.eventLevel = eventLevel;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }

    public static class DataCommandData {
        private int code = REPLY_OK;
        //TODO: discover and implement

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }

    public static class DictDataCommandData {
        private int code = REPLY_OK;
        //TODO: discover and implement

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }

    public interface DataCallback {
        void onConfigCommand(ConfigCommandData data);

        void onEventCommand(EventCommandData data);

        void onDataCommand(DataCommandData data);

        void onDictDataCommand(DictDataCommandData data);
    }

}
