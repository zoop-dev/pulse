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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.filetransfer.ZeppOsFileTransferImpl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.filetransfer.ZeppOsFileTransferV2;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.filetransfer.ZeppOsFileTransferV3;

public class ZeppOsFileTransferService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFileTransferService.class);

    private static final short ENDPOINT = 0x000d;

    private ZeppOsFileTransferImpl impl;

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

        if (payload[0] != ZeppOsFileTransferImpl.CMD_CAPABILITIES_RESPONSE) {
            LOG.warn("Got file transfer command, but impl is not initialized");
            return;
        }
        final int version = payload[1] & 0xff;
        if (version == 1 || version == 2) {
            impl = new ZeppOsFileTransferV2(this, getSupport());
        } else if (version == 3) {
            impl = new ZeppOsFileTransferV3(this, getSupport());
        } else {
            LOG.error("Unsupported file transfer service version: {}", version);
            return;
        }

        impl.handlePayload(payload);
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        write(builder, new byte[]{ZeppOsFileTransferImpl.CMD_CAPABILITIES_REQUEST});
    }

    public void sendFile(final String url, final String filename, final byte[] bytes, final boolean compress, final Callback callback) {
        if (impl == null) {
            LOG.error("Service not initialized, refusing to send {}", url);
            callback.onFileUploadFinish(false);
            return;
        }

        impl.uploadFile(url, filename, bytes, compress, callback);
    }

    public void onCharacteristicChanged(final UUID characteristicUUID, final byte[] value) {
        if (impl == null) {
            LOG.error("Service not initialized, ignoring characteristic change for {}", characteristicUUID);
            return;
        }

        impl.onCharacteristicChanged(characteristicUUID, value);
    }

    public interface Callback {
        void onFileUploadFinish(final boolean success);

        void onFileUploadProgress(final int progress);

        void onFileDownloadFinish(final String url, final String filename, final byte[] data);
    }

    public interface UploadCallback extends Callback {
        @Override
        void onFileUploadFinish(final boolean success);

        @Override
        void onFileUploadProgress(final int progress);

        @Override
        default void onFileDownloadFinish(final String url, final String filename, final byte[] data) {
            LOG.error("Received unexpected file on upload callback for {}: url={} filename={} length={}", getClass(), url, filename, data.length);
        }
    }

    public interface DownloadCallback extends Callback {
        @Override
        default void onFileUploadFinish(final boolean success) {
            LOG.error("Received unexpected upload finish on download callback for {}: success={}", getClass(), success);
        }

        @Override
        default void onFileUploadProgress(final int progress) {
            LOG.error("Received unexpected upload progress on download callback for {}: progress={}", getClass(), progress);
        }

        @Override
        void onFileDownloadFinish(final String url, final String filename, final byte[] data);
    }
}
