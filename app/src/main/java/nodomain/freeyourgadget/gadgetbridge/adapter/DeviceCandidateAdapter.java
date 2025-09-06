/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniel
    Dakhno, José Rebelo, Petr Vaněk, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Adapter for displaying GBDeviceCandate instances.
 */
public class DeviceCandidateAdapter extends ArrayAdapter<GBDeviceCandidate> {
    public DeviceCandidateAdapter(final Context context, final List<GBDeviceCandidate> deviceCandidates) {
        super(context, 0, deviceCandidates);
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, View view, @NonNull final ViewGroup parent) {
        final GBDeviceCandidate device = getItem(position);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_with_details, parent, false);
        }
        final ImageView deviceImageView = view.findViewById(R.id.item_image);
        final TextView deviceNameLabel = view.findViewById(R.id.item_name);
        final TextView deviceDetailsLabel = view.findViewById(R.id.item_details);
        final TextView deviceStatus = view.findViewById(R.id.item_status);

        if (device == null) {
            // Should never happen?
            deviceNameLabel.setText("(null)");
            deviceDetailsLabel.setText("(null)");
            deviceImageView.setImageResource(R.drawable.ic_device_unknown);
            deviceStatus.setText("");
            return view;
        }

        final DeviceType deviceType = DeviceHelper.getInstance().resolveDeviceType(device);
        final DeviceCoordinator coordinator = deviceType.getDeviceCoordinator();

        final String name = formatDeviceName(device);
        deviceNameLabel.setText(name);
        deviceDetailsLabel.setText(getContext().getString(coordinator.getDeviceNameResource()) + "\n" + device.getMacAddress());
        deviceImageView.setImageResource(coordinator.getDefaultIconResource());

        final List<String> statusLines = new ArrayList<>();
        if (device.isBonded()) {
            statusLines.add(getContext().getString(R.string.device_is_currently_bonded));
        }

        if (!deviceType.isSupported()) {
            statusLines.add(getContext().getString(R.string.device_unsupported));
        }

        if (coordinator.isExperimental()) {
            statusLines.add(getContext().getString(R.string.device_experimental));
        }

        deviceStatus.setText(TextUtils.join("\n", statusLines));
        return view;
    }

    private String formatDeviceName(final GBDeviceCandidate device) {
        if (device.getRssi() > GBDevice.RSSI_UNKNOWN) {
            return getContext().getString(R.string.device_with_rssi, device.getName(), GB.formatRssi(device.getRssi()));
        }
        return device.getName();
    }
}
