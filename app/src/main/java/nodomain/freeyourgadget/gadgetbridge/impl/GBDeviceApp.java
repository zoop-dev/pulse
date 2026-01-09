/*  Copyright (C) 2015-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.impl;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class GBDeviceApp implements Parcelable {
    private final String name;
    private final String creator;
    private final String version;
    private final UUID uuid;
    private final Type type;
    private final boolean inCache;
    private boolean isOnDevice;
    private boolean configurable;
    private boolean canBeStarted = true;
    private final Bitmap previewImage;
    private boolean isUpToDate = true;

    public GBDeviceApp(UUID uuid, String name, String creator, String version, Type type, Bitmap previewImage) {
        this.uuid = uuid;
        this.name = name;
        this.creator = creator;
        this.version = version;
        this.type = type;
        this.previewImage = previewImage;
        //FIXME: do not assume
        this.inCache = false;
        this.configurable = false;
        this.isOnDevice = false;
    }

    public GBDeviceApp(UUID uuid, String name, String creator, String version, Type type) {
        this.uuid = uuid;
        this.name = name;
        this.creator = creator;
        this.version = version;
        this.type = type;
        this.previewImage = null;
        //FIXME: do not assume
        this.inCache = false;
        this.configurable = false;
        this.isOnDevice = false;
    }

    public GBDeviceApp(JSONObject json, boolean configurable, Bitmap previewImage) {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String name = "";
        String creator = "";
        String version = "";
        Type type = Type.UNKNOWN;

        try {
            uuid = UUID.fromString(json.getString("uuid"));
            name = json.getString("name");
            creator = json.getString("creator");
            version = json.getString("version");
            type = Type.valueOf(json.getString("type"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.uuid = uuid;
        this.name = name;
        this.creator = creator;
        this.version = version;
        this.type = type;
        this.previewImage = previewImage;
        //FIXME: do not assume
        this.inCache = true;
        this.configurable = configurable;
    }

    private GBDeviceApp(final Parcel in) {
        this.name = in.readString();
        this.creator = in.readString();
        this.version = in.readString();
        final String uuidString = in.readString();
        this.uuid = uuidString != null ? UUID.fromString(uuidString) : null;
        final String typeString = in.readString();
        this.type = typeString != null ? Type.valueOf(typeString) : null;
        this.inCache = in.readInt() != 0;
        this.isOnDevice = in.readInt() != 0;
        this.configurable = in.readInt() != 0;
        this.canBeStarted = in.readInt() != 0;
        this.previewImage = in.readParcelable(GBDeviceApp.class.getClassLoader());
        this.isUpToDate = in.readInt() != 0;
    }

    public void setConfigurable(boolean configurable) {
        this.configurable = configurable;
    }

    public void setOnDevice(boolean isOnDevice) {
        this.isOnDevice = isOnDevice;
    }

    public boolean isInCache() {
        return inCache;
    }

    public boolean isOnDevice() {
        return isOnDevice;
    }

    public String getName() {
        return name;
    }

    public String getCreator() {
        return creator;
    }

    public String getVersion() {
        return version;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Type getType() {
        return type;
    }

    public Bitmap getPreviewImage() {
        return previewImage;
    }

    public enum Type {
        UNKNOWN,
        WATCHFACE,
        WATCHFACE_SYSTEM,
        APP_GENERIC,
        APP_ACTIVITYTRACKER,
        APP_SYSTEM,
    }

    public JSONObject getJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("uuid", uuid.toString());
            json.put("name", name);
            json.put("creator", creator);
            json.put("version", version);
            json.put("type", type.name());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public boolean isConfigurable() {
        return configurable;
    }

    public void setUpToDate(boolean isUpToDate) {
        this.isUpToDate = isUpToDate;
    }

    public boolean isUpToDate() {
        return isUpToDate;
    }

    public boolean isCanBeStarted() {
        return canBeStarted;
    }

    public void setCanBeStarted(boolean canBeStarted) {
        this.canBeStarted = canBeStarted;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeString(name);
        dest.writeString(creator);
        dest.writeString(version);
        dest.writeString(uuid != null ? uuid.toString() : null);
        dest.writeString(type != null ? type.name() : null);
        dest.writeInt(inCache ? 1 : 0);
        dest.writeInt(isOnDevice ? 1 : 0);
        dest.writeInt(configurable ? 1 : 0);
        dest.writeInt(canBeStarted ? 1 : 0);
        dest.writeParcelable(previewImage, 0);
        dest.writeInt(isUpToDate ? 1 : 0);
    }

    public static final Creator<GBDeviceApp> CREATOR = new Creator<>() {
        @Override
        public GBDeviceApp createFromParcel(Parcel in) {
            return new GBDeviceApp(in);
        }

        @Override
        public GBDeviceApp[] newArray(int size) {
            return new GBDeviceApp[size];
        }
    };
}
