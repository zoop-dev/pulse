/*  Copyright (C) 2023-2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.filetransfer.AbstractFileTransferImpl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.filetransfer.FileTransferImplV2;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.filetransfer.FileTransferImplV3;

public class ZeppOsFileTransferService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFileTransferService.class);

    private static final short ENDPOINT = 0x000d;

    private AbstractFileTransferImpl impl;

    public ZeppOsFileTransferService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    /**
     * HACK: Expose the method to the impl.
     */
    @Override
    public void write(final String taskName, final byte[] data) {
        super.write(taskName, data);
    }

    @Override
    public void handlePayload(final byte[] payload) {
        if (impl != null) {
            impl.handlePayload(payload);
            return;
        }

        if (payload[0] != AbstractFileTransferImpl.CMD_CAPABILITIES_RESPONSE) {
            LOG.warn("Got file transfer command, but impl is not initialized");
            return;
        }
        final int version = payload[1] & 0xff;
        if (version == 1 || version == 2) {
            impl = new FileTransferImplV2(this, getSupport());
        } else if (version == 3) {
            impl = new FileTransferImplV3(this, getSupport());
        } else {
            LOG.error("Unsupported file transfer service version: {}", version);
            return;
        }

        impl.handlePayload(payload);
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        write(builder, new byte[]{AbstractFileTransferImpl.CMD_CAPABILITIES_REQUEST});
    }

    public void sendFile(final String url, final String filename, final byte[] bytes, final boolean compress, final Callback callback) {
        if (impl == null) {
            LOG.error("Service not initialized, refusing to send {}", url);
            callback.onFileUploadFinish(false);
            return;
        }

        impl.uploadFile(url, filename, bytes, compress, callback);
    }

    public void onCharacteristicChanged(final BluetoothGattCharacteristic characteristic) {
        if (impl == null) {
            LOG.error("Service not initialized, ignoring characteristic change for {}", characteristic.getUuid());
            return;
        }

        impl.onCharacteristicChanged(characteristic);
    }

    public interface Callback {
        void onFileUploadFinish(boolean success);

        void onFileUploadProgress(int progress);

        void onFileDownloadFinish(String url, String filename, byte[] data);
    }
}
