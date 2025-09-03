package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionLanguage extends FieldDefinition {

    public FieldDefinitionLanguage(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            return Language.fromId(raw);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof Language) {
            baseType.encode(byteBuffer, (((Language) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum Language {
        english(0),
        french(1),
        italian(2),
        german(3),
        spanish(4),
        croatian(5),
        czech(6),
        danish(7),
        dutch(8),
        finnish(9),
        greek(10),
        hungarian(11),
        norwegian(12),
        polish(13),
        portuguese(14),
        slovakian(15),
        slovenian(16),
        swedish(17),
        russian(18),
        turkish(19),
        latvian(20),
        ukrainian(21),
        arabic(22),
        farsi(23),
        bulgarian(24),
        romanian(25),
        chinese(26),
        japanese(27),
        korean(28),
        taiwanese(29),
        thai(30),
        hebrew(31),
        brazilian_portuguese(32),
        indonesian(33),
        malaysian(34),
        vietnamese(35),
        burmese(36),
        mongolian(37)
        ;

        private final int id;

        Language(int i) {
            id = i;
        }

        @Nullable
        public static Language fromId(int id) {
            for (Language language :
                    Language.values()) {
                if (id == language.getId()) {
                    return language;
                }
            }
            return null;
        }

        public int getId() {
            return id;
        }
    }
}
