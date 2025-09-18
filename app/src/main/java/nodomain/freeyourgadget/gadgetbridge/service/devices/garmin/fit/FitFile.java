/*  Copyright (C) 2024-2025 Daniele Gobbetti, José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.ChecksumCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.exception.FitParseException;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecordDataFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class FitFile {
    protected static final Logger LOG = LoggerFactory.getLogger(FitFile.class);
    private final Header header;
    private final List<RecordData> dataRecords;
    private final boolean canGenerateOutput;

    public FitFile(Header header, List<RecordData> dataRecords) {
        this.header = header;
        this.dataRecords = dataRecords;
        this.canGenerateOutput = false;
    }

    public FitFile(List<RecordData> dataRecords) {
        this.dataRecords = dataRecords;
        this.header = new Header(true, 16, 21117);
        this.canGenerateOutput = true;
    }

    private static byte[] readFileToByteArray(File file) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toByteArray();
        }
    }

    public static FitFile parseIncoming(File file) throws IOException, FitParseException {
        return parseIncoming(readFileToByteArray(file));
    }

    //TODO: process file in chunks??
    public static FitFile parseIncoming(byte[] fileContents) throws FitParseException {

        final GarminByteBufferReader garminByteBufferReader = new GarminByteBufferReader(fileContents);
        garminByteBufferReader.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        final Header header = Header.parseIncomingHeader(garminByteBufferReader);

        // needed because the headers can be redefined in the file. The last header for a local message number wins
        Map<Integer, RecordDefinition> recordDefinitionMap = new HashMap<>();
        List<RecordData> dataRecords = new ArrayList<>();
        Long referenceTimestamp = null;

        while (garminByteBufferReader.getPosition() < header.getHeaderSize() + header.getDataSize()) {
            byte rawRecordHeader = (byte) garminByteBufferReader.readByte();
            RecordHeader recordHeader = new RecordHeader(rawRecordHeader);
            final Integer timeOffset = recordHeader.getTimeOffset();
            if (timeOffset != null) {
                if (referenceTimestamp == null) {
                    throw new FitParseException("Got compressed timestamp without knowing current timestamp");
                }

                if (timeOffset >= (referenceTimestamp & 0x1FL)) {
                    referenceTimestamp = (referenceTimestamp & ~0x1FL) + timeOffset;
                } else if (timeOffset < (referenceTimestamp & 0x1FL)) {
                    referenceTimestamp = (referenceTimestamp & ~0x1FL) + timeOffset + 0x20;
                }
            }
            if (recordHeader.isDefinition()) {
                final RecordDefinition recordDefinition = RecordDefinition.parseIncoming(garminByteBufferReader, recordHeader);
                if (recordDefinition != null) {
                    if (recordHeader.isDeveloperData())
                        for (RecordData rd : dataRecords) {
                            if (GlobalFITMessage.FIELD_DESCRIPTION.equals(rd.getGlobalFITMessage()))
                                recordDefinition.populateDevFields(rd);
                        }
                    recordDefinitionMap.put(recordHeader.getLocalMessageType(), recordDefinition);
                }
            } else {
                final RecordDefinition referenceRecordDefinition = recordDefinitionMap.get(recordHeader.getLocalMessageType());
                if (referenceRecordDefinition != null) {
                    final RecordData runningData = FitRecordDataFactory.create(referenceRecordDefinition, recordHeader);
                    dataRecords.add(runningData);
                    Long newTimestamp = runningData.parseDataMessage(garminByteBufferReader, referenceTimestamp);
                    if (newTimestamp != null)
                        referenceTimestamp = newTimestamp;
                }
            }
        }
        garminByteBufferReader.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        final int fileCrc = garminByteBufferReader.readShort();
        final int actualCrc = ChecksumCalculator.computeCrc(fileContents, 0, garminByteBufferReader.getPosition() - 2);
        if (fileCrc != actualCrc) {
            throw new FitParseException("Wrong CRC for FIT file: got " + actualCrc + " expected " + fileCrc);
        }
        if (garminByteBufferReader.getPosition() < garminByteBufferReader.getLimit()) {
            LOG.warn("There are {} bytes after the fit file", garminByteBufferReader.getLimit() - garminByteBufferReader.getPosition());
            // TODO a fit file should actually be multiple fit files
        }
        return new FitFile(header, dataRecords);
    }

    public List<RecordData> getRecordsByGlobalMessage(GlobalFITMessage globalFITMessage) {
        final List<RecordData> filtered = new ArrayList<>();
        for (RecordData rd : dataRecords) {
            if (globalFITMessage.equals(rd.getGlobalFITMessage()))
                filtered.add(rd);
        }
        return filtered;
    }

    public List<RecordData> getRecords() {
        return dataRecords;
    }

    public void generateOutgoingDataPayload(MessageWriter writer) {
        if (!canGenerateOutput)
            throw new IllegalArgumentException("Generation of previously parsed FIT file not supported.");

        MessageWriter temporary = new MessageWriter(writer.getLimit());
        temporary.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        RecordDefinition prevDefinition = null;
        for (final RecordData rd : dataRecords) {
            if (!rd.getRecordDefinition().equals(prevDefinition)) {
                rd.getRecordDefinition().generateOutgoingPayload(temporary);
                prevDefinition = rd.getRecordDefinition();
            }

            rd.generateOutgoingDataPayload(temporary);
        }
        this.header.setDataSize(temporary.getSize());

        this.header.generateOutgoingDataPayload(writer);
        writer.writeBytes(temporary.getBytes());
        writer.writeShort(ChecksumCalculator.computeCrc(writer.getBytes(), this.header.getHeaderSize(), writer.getBytes().length - this.header.getHeaderSize()));
    }

    @Nullable
    public FileType.FILETYPE getFileType() {
        if (dataRecords == null || dataRecords.isEmpty()) {
            LOG.error("FIT file has no dataRecords");
            return null;
        }

        final Optional<FitFileId> fitFileIdOpt = dataRecords.stream()
                .filter(r -> r instanceof FitFileId)
                .map(r -> (FitFileId) r)
                .findFirst();

        if (!fitFileIdOpt.isPresent()) {
            LOG.error("FIT file has no FILE_ID message");
            return null;
        }

        final FitFileId fitFileId = fitFileIdOpt.get();
        FileType.FILETYPE type = fitFileId.getType();
        if (type == null) {
            LOG.error("FIT file FILE_ID message has 'type' value null");
        }
        return type;
    }

    public byte[] getOutgoingMessage() {
        // Compute the worst case scenario buffer size for the fit file
        // A ~1.6MB gpx file with ~16k points results in a ~320KB buffer, ~150KB of which get actually used
        final int dataRecordsSize = dataRecords.stream()
                .mapToInt(r -> {
                    // Worst case scenario, for each data record

                    // one distinct record definition: 5 bytes + (number of field definitions * 3 + 1)
                    final List<FieldDefinition> definitions = r.getRecordDefinition().getFieldDefinitions();
                    final int recordDefinitionOverhead = 5 + (definitions != null ? definitions.size() * 3 + 1 : 0);

                    // 1 + size of the value holder
                    final int dataRecordOverhead = 1 + r.valueHolder.limit();

                    return recordDefinitionOverhead + dataRecordOverhead;
                }).sum();

        // Final size = 14b header + data records + 2b crc
        final MessageWriter writer = new MessageWriter(14 + dataRecordsSize + 2);
        this.generateOutgoingDataPayload(writer);
        return writer.getBytes();
    }

    @NonNull
    @Override
    public String toString() {
        return dataRecords.toString();
    }

    public static class Header {
        public static final int MAGIC = 0x5449462E;

        private final int headerSize;
        private final int protocolVersion;
        private final int profileVersion;
        private final boolean hasCRC;
        private int dataSize;

        public Header(boolean hasCRC, int protocolVersion, int profileVersion) {
            this(hasCRC, protocolVersion, profileVersion, 0);
        }

        public Header(boolean hasCRC, int protocolVersion, int profileVersion, int dataSize) {
            this.hasCRC = hasCRC;
            headerSize = hasCRC ? 14 : 12;
            this.protocolVersion = protocolVersion;
            this.profileVersion = profileVersion;
            this.dataSize = dataSize;
        }

        static Header parseIncomingHeader(GarminByteBufferReader garminByteBufferReader) throws FitParseException {
            int headerSize = garminByteBufferReader.readByte();
            if (headerSize < 12) {
                throw new FitParseException("Too short header in FIT file.");
            }
            boolean hasCRC = headerSize == 14;
            int protocolVersion = garminByteBufferReader.readByte();
            int profileVersion = garminByteBufferReader.readShort();
            int dataSize = garminByteBufferReader.readInt();
            int magic = garminByteBufferReader.readInt();
            if (magic != MAGIC) {
                throw new FitParseException("Wrong magic header in FIT file");
            }
            if (hasCRC) {
                int incomingCrc = garminByteBufferReader.readShort();

                if (incomingCrc != 0 && incomingCrc != ChecksumCalculator.computeCrc(garminByteBufferReader.asReadOnlyBuffer(), 0, headerSize - 2)) {
                    throw new FitParseException("Wrong CRC for header in FIT file");
                }
                //            LOG.info("Fit File Header didn't have CRC, no check performed.");
            }
            return new Header(hasCRC, protocolVersion, profileVersion, dataSize);
        }

        public int getHeaderSize() {
            return headerSize;
        }

        public int getDataSize() {
            return dataSize;
        }

        public void setDataSize(int dataSize) {
            this.dataSize = dataSize;
        }

        public void generateOutgoingDataPayload(MessageWriter writer) {
            writer.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            writer.writeByte(headerSize);
            writer.writeByte(protocolVersion);
            writer.writeShort(profileVersion);
            writer.writeInt(dataSize);
            writer.writeInt(MAGIC);//magic
            if (hasCRC)
                writer.writeShort(ChecksumCalculator.computeCrc(writer.getBytes(), 0, writer.getBytes().length));
        }

    }
}
