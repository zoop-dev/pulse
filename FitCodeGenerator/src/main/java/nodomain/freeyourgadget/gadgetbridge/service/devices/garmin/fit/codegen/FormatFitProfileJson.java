/*  Copyright (C) 2026 Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.SortedSet;
import java.util.TreeSet;

public enum FormatFitProfileJson {
    ;

    public static void main(final String[] args) throws Exception {
        final String pathToJson = (args.length > 0) ? args[0] : FitCodeGen.DEFAULT_INPUT_PATH;

        final File jsonFile = new File(pathToJson).getAbsoluteFile();

        final SortedSet<FitCodeGen.FitMessage> messages = new TreeSet<>();
        final SortedSet<FitCodeGen.FitEnum> enumerations = new TreeSet<>();
        final SortedSet<FitCodeGen.FitDevice> devices = new TreeSet<>();

        System.out.println("Reading " + jsonFile.getAbsolutePath());
        try (final FileReader reader = new FileReader(jsonFile, StandardCharsets.UTF_8)) {
            FitCodeGen.readJson(reader, messages, enumerations, devices);
        }

        edit(messages, enumerations, devices);

        System.out.println("Writing " + jsonFile.getAbsolutePath());
        try (final Writer writer = new FileWriter(jsonFile, StandardCharsets.UTF_8)) {
            FitCodeGen.writeJSon(writer, messages, enumerations, devices);
        }

        System.out.println( "Done");
    }

    private static void edit(final SortedSet<FitCodeGen.FitMessage> messages,
                             final SortedSet<FitCodeGen.FitEnum> enumerations,
                             final SortedSet<FitCodeGen.FitDevice> devices) throws Exception {
        // optionally: add modification logic here
    }
}
