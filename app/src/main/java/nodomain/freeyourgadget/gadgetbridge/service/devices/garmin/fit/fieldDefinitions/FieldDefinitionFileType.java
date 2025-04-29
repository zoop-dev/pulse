package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionFileType extends FieldDefinition {

    public FieldDefinitionFileType(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            final FileType.FILETYPE fileType = FileType.FILETYPE.fromDataTypeSubType(128, raw);
            return fileType == null ? raw : fileType;
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof FileType.FILETYPE) {
            baseType.encode(byteBuffer, (((FileType.FILETYPE) o).getSubType()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }
}
