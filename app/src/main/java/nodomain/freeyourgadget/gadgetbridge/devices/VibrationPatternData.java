/*  Copyright (C) 2026 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.devices;

import java.util.List;

public class VibrationPatternData {

    public static class TypeEntry {
        public final int typeId;
        public final String typeName;
        public final int patternId;
        public final String patternName;

        public TypeEntry(final int typeId, final String typeName,
                         final int patternId, final String patternName) {
            this.typeId = typeId;
            this.typeName = typeName;
            this.patternId = patternId;
            this.patternName = patternName;
        }
    }

    public static class PatternEntry {
        public final int id;
        public final String name;
        public final int typeId;
        public final String typeName;
        public final List<Segment> segments;
        public final boolean canDelete;

        public PatternEntry(final int id, final String name,
                            final int typeId, final String typeName,
                            final List<Segment> segments, final boolean canDelete) {
            this.id = id;
            this.name = name;
            this.typeId = typeId;
            this.typeName = typeName;
            this.segments = segments;
            this.canDelete = canDelete;
        }
    }

    public static class Segment {
        public final boolean on;
        public final int durationMs;
        public final int strengthPercent;

        public Segment(final boolean on, final int durationMs, final int strengthPercent) {
            this.on = on;
            this.durationMs = durationMs;
            this.strengthPercent = strengthPercent;
        }
    }

    public final List<TypeEntry> typeMappings;
    public final List<PatternEntry> customPatterns;

    public VibrationPatternData(final List<TypeEntry> typeMappings,
                                final List<PatternEntry> customPatterns) {
        this.typeMappings = typeMappings;
        this.customPatterns = customPatterns;
    }
}
