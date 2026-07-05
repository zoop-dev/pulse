/*  Copyright (C) 2024-2026 José Rebelo, Daniele Gobbetti, Sebastian Dröge,
        punchdeerflyscorpion, Thomas Kuehne

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

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum FitCodeGen {
    ;

    // messages definitions directly accessed repeatedly
    // put everything else in functions to avoid "code to large" for the class constructor
    static final int[] DIRECT_ACCESS_MESSAGES = {0, 1, 206};

    private static final String NATIVE_HEADER =
            """                        
                    package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;
                    
                    import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType.*;
                    import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinitionFactory.FIELD.*;
                    import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.NativeFITMessage.FieldDefinitionPrimitive;
                    
                    import java.util.List;
                    import java.util.SortedMap;
                    import java.util.TreeMap;
                    
                    @SuppressWarnings("DuplicateStringLiteralInspection")
                    public enum NativeFITMessages {
                        DUMMY();""";

    private static final String COPYRIGHT_HEADER = """
            /*  Copyright (C) 2026 Freeyourgadget
            
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
            """;

    public static void main(final String[] args) throws Exception {
        final long start = System.currentTimeMillis();

        String pathToJson = (args.length > 0) ? args[0] : "FitCodeGenerator/src/main/resources/fit_profile.json";
        String pathToOutput = (args.length > 1) ? args[1] : "app/build/generated/sources/fit/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/fit/";
        String pathToManual = (args.length > 2) ? args[2] : "app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/fit/";

        File jsonFile = new File(pathToJson).getAbsoluteFile();
        File outputDir = new File(pathToOutput).getAbsoluteFile();

        final TreeSet<NativeFITMessage> messages;
        try (FileReader reader = new FileReader(jsonFile, StandardCharsets.UTF_8)) {
            messages = readJson(reader);
        }

        File messageDir = new File(outputDir, "messages").getAbsoluteFile();
        File manualMessageDir = new File(pathToManual, "messages").getAbsoluteFile();

        if (messageDir.exists()) {
            for (final File file : messageDir.listFiles()) {
                file.delete();
            }
        } else {
            messageDir.mkdirs();
        }

        generateNativeFile(messages, outputDir);
        generateFitRecordDataFactory(messages, messageDir);
        int numFiles = 0;
        for (final NativeFITMessage message : messages) {
            numFiles++;
            generateFitMessageClassFile(message, messageDir, manualMessageDir);
        }
        long time = System.currentTimeMillis() - start;
        System.out.println("Written " + numFiles + " files to " + messageDir + " in " + time + "ms");
    }

    private static void generateNativeFile(final TreeSet<NativeFITMessage> messages, final File outputDir) throws IOException {
        final File nativeFile = new File(outputDir, "NativeFITMessages.java");
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        generateNativeFile(messages, printer);
        printer.println("}");
        printer.flush();

        Files.writeString(nativeFile.toPath(), writer.toString(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        System.out.println("Written " + nativeFile.getCanonicalPath());
    }

    private static TreeSet<NativeFITMessage> readJson(final Reader reader) {
        final JSONTokener tokenizer = new JSONTokener(reader);
        final JSONObject root = new JSONObject(tokenizer);
        final JSONObject messages = root.getJSONObject("messages");
        final Set<Integer> messageKeys = new TreeSet<>();
        for (final String key : messages.keySet()) {
            messageKeys.add(Integer.decode(key));
        }

        final TreeSet<NativeFITMessage> fitMessages = new TreeSet<>();
        for (final Integer messageKey : messageKeys) {
            final JSONObject message = messages.getJSONObject(messageKey.toString());
            final NativeFITMessage fitMessage = new NativeFITMessage();
            fitMessage.num = message.getInt("num");
            fitMessage.name = message.getString("name");
            fitMessages.add(fitMessage);
            final JSONObject fields = message.getJSONObject("fields");
            final SortedSet<Integer> fieldKeys = new TreeSet<>();
            for (final String fieldKey : fields.keySet()) {
                fieldKeys.add(Integer.decode(fieldKey));
            }
            for (final Integer fieldKey : fieldKeys) {
                final JSONObject field = fields.getJSONObject(fieldKey.toString());
                final FieldDefinitionPrimitive fitField = new FieldDefinitionPrimitive();
                fitField.num = field.getInt("num");
                fitField.name = field.getString("name");

                fitField.type = field.has("type") ? field.getString("type") : null;
                fitField.scale = field.has("scale") ? field.getDouble("scale") : 1.0;
                fitField.offset = field.has("offset") ? field.getDouble("offset") : 0.0;
                fitField.arrayLen = field.has("arrayLen") ? field.getInt("arrayLen") : -1;
                fitField.stringLen = field.has("stringLen") ? field.getInt("stringLen") : -1;
                fitField.UOM = field.has("UOM") ? field.getString("UOM") : null;
                fitField.baseType = field.getString("baseType");
                if ("BYTE".contentEquals(fitField.baseType)) {
                    fitField.baseType = "BASE_TYPE_BYTE";
                }
                if (fitField.arrayLen < 0) {
                    fitField.arrayLen = -1;
                } else {
                    fitField.type = "ARRAY";
                }
                if (fitField.stringLen <= 0) {
                    fitField.stringLen = -1;
                }
                if (fitField.baseType != null && fitField.type != null && fitField.baseType.contentEquals(fitField.type)) {
                    fitField.type = null;
                }
                fitMessage.fields.add(fitField);
            }
        }
        return fitMessages;
    }

    private static void generateNativeFile(final TreeSet<NativeFITMessage> fitMessages, final PrintWriter printer) {
        printer.println(COPYRIGHT_HEADER);
        printer.println(NATIVE_HEADER);

        for (final NativeFITMessage messsage : fitMessages) {
            boolean directAccess = Arrays.binarySearch(DIRECT_ACCESS_MESSAGES, messsage.num) >= 0;
            if(directAccess) {
                printer.println("\tpublic static NativeFITMessage FIT_" + messsage.name + " = new NativeFITMessage(" + messsage.num + ", \"" + messsage.name + "\", List.of(");
            }else{
                printer.println("\tpublic static NativeFITMessage FIT_" + messsage.name + "() { return new NativeFITMessage(" + messsage.num + ", \"" + messsage.name + "\", List.of(");
            }

            final FieldDefinitionPrimitive[] fields = messsage.fields.toArray(new FieldDefinitionPrimitive[0]);
            for (int i = 0; i < fields.length; i++) {
                final FieldDefinitionPrimitive field = fields[i];
                // TODO: offset - add support for non-int in Java stage
                // TODO: scale - add support for non-int in Java stage
                // TODO: support - add support for string arrays in Java stage
                int size = field.stringLen > 0 ? field.stringLen : (field.arrayLen >= 0 ? field.arrayLen : 0);
                final Number scale;
                {
                    final long longScale = Math.round(field.scale);
                    if (longScale == field.scale) {
                        scale = longScale;
                    } else {
                        scale = field.scale;
                    }
                }
                final Number offset;
                {
                    final long longOffset = Math.round(field.offset);
                    if (longOffset == field.offset) {
                        offset = longOffset;
                    } else {
                        offset = field.offset;
                    }
                }

                if (size > 0) {
                    printer.print("\t\t\tnew FieldDefinitionPrimitive(" +
                            field.num + ", "
                            + field.baseType + ", "
                            + size + ", \""
                            + field.name + "\", "
                            + (field.type == null ? "null" : field.type) + ", "
                            + scale + ", "
                            + offset + ")"
                    );
                } else if (field.offset != 0 || field.scale != 1 || field.type != null || field.UOM != null) {
                    printer.print("\t\t\tnew FieldDefinitionPrimitive(" +
                            field.num + ", "
                            + field.baseType + ", \""
                            + field.name + "\", "
                            + (field.type == null ? "null" : field.type) + ", "
                            + scale + ", "
                            + offset + ")"
                    );
                } else {
                    printer.print("\t\t\tnew FieldDefinitionPrimitive(" +
                            field.num + ", " + field.baseType + ", \"" + field.name + "\")");
                }

                final boolean isLast = i + 1 >= fields.length;
                if (!isLast) {
                    printer.print(",");
                }

                if (null != field.UOM) {
                    printer.print(" // ");
                    printer.print(field.UOM);
                }

                printer.println();
            }
            if (directAccess) {
                printer.println("\t));");
            } else {
                printer.println("\t));}");
            }
        }

        printer.println("\tstatic final SortedMap<Integer, NativeFITMessage> mapKnownMessages() {");
        printer.println("\t\tSortedMap<Integer, NativeFITMessage> map = new TreeMap<>();");
        printer.println("\t\tNativeFITMessage mesg;");
        for (final NativeFITMessage message : fitMessages) {
            boolean directAccess = Arrays.binarySearch(DIRECT_ACCESS_MESSAGES, message.num) >= 0;
            if (directAccess) {
                printer.println("\t\tmap.put(" + message.num + ", FIT_" + message.name + ");");
            } else {
                printer.println("\t\tmap.put(" + message.num + ", FIT_" + message.name + "());");
            }
        }
        printer.println("\t\treturn map;");
        printer.println("\t}");
    }

    private static void generateFitRecordDataFactory(final TreeSet<NativeFITMessage> messages, final File outputDir) throws IOException {
        final File factoryFile = new File(outputDir, "FitRecordDataFactory.java");

        final List<String> switchCases = new ArrayList<>();
        final List<NativeFITMessage> nativeFITMessages = new ArrayList<>(messages);
        nativeFITMessages.sort(Comparator.comparingInt(NativeFITMessage::getNumber));

        for (final NativeFITMessage value : nativeFITMessages) {
            final String className = "Fit" + capitalize(toCamelCase(value.name()));
            switchCases.add("            case %d -> new %s(recordDefinition, recordHeader);".formatted(value.getNumber(), className));
        }

        final String template = """
                ${copyrightHeader}
                package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;
                
                import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
                import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
                import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
                
                /**
                 * WARNING: This class was auto-generated, please avoid modifying it directly.
                 * See {@link ${generatorClass}}
                 */
                public class FitRecordDataFactory {
                    private FitRecordDataFactory() {
                        // use create
                    }
                
                    public static RecordData create(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
                        return switch (recordDefinition.getNativeFITMessage().getNumber()) {
                ${switchCases}
                             default -> new RecordData(recordDefinition, recordHeader);
                        };
                    }
                }
                """;

        final String result = template
                .replace("${copyrightHeader}", COPYRIGHT_HEADER.strip())
                .replace("${generatorClass}", Objects.requireNonNull(FitCodeGen.class.getCanonicalName()))
                .replace("${switchCases}", String.join("\n", switchCases))
                .replaceAll("\\R", System.lineSeparator());

        Files.writeString(factoryFile.toPath(), result, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        System.out.println("Written " + factoryFile.getCanonicalPath());
    }

    private static void generateFitMessageClassFile(final NativeFITMessage nativeFITMessage, final File messagesDir, final File manualMessageDir) throws IOException {
        String className = "Fit" + capitalize(toCamelCase(nativeFITMessage.name));
        if (new File(manualMessageDir, className + ".java").exists()) {
            className = "Abstract" + className;
        }
        final File outputFile = new File(
                messagesDir,
                className + ".java"
        );

        //
        // Collect all imports
        //
        final List<String> imports = new ArrayList<>(List.of(
                "androidx.annotation.Nullable",
                "nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder",
                "nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData",
                "nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition",
                "nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader"
        ));
        imports.addAll(getImports(outputFile));

        //
        // Add field-specific imports
        //
        for (final FieldDefinitionPrimitive primitive : nativeFITMessage.getFieldDefinitionPrimitives()) {
            final FieldClass fieldType = getFieldType(primitive);
            if (!Objects.requireNonNull(fieldType.getCanonicalName()).startsWith("java.lang")) {
                if (fieldType.isArray()) {
                    imports.add(fieldType.getCanonicalName().replace("[]", ""));
                } else {
                    imports.add(fieldType.getCanonicalName());
                }
            }
        }

        //
        // Deduplicate + sort imports
        //
        final Set<String> uniqueImports = new TreeSet<>(imports);

        //
        // Copyright header + package
        //
        final StringBuilder sb = new StringBuilder();
        sb.append(COPYRIGHT_HEADER.strip()).append("\n");
        sb.append("package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;\n\n");

        //
        // Imports, grouped by prefix
        //
        final List<String> importOrder = List.of("androidx", "java.", "nodomain.freeyourgadget");
        for (final String prefix : importOrder) {
            boolean wroteAny = false;
            for (final String i : uniqueImports) {
                if (i.startsWith(prefix)) {
                    sb.append("import ").append(i).append(";\n");
                    wroteAny = true;
                }
            }
            if (wroteAny) {
                sb.append("\n"); // only add blank line if group was non-empty
            }
        }

        //
        // Imports - rest
        //
        boolean wroteOthers = false;
        for (final String i : uniqueImports) {
            if (importOrder.stream().noneMatch(i::startsWith)) {
                sb.append("import ").append(i).append(";\n");
                wroteOthers = true;
            }
        }
        if (wroteOthers) {
            sb.append("\n");
        }

        //
        // Class javadoc + constructor
        //
        sb.append("""
                /**
                 * WARNING: This class was auto-generated, please avoid modifying it directly.
                 * See {@link ${classCanonicalName}}
                 *
                 * @noinspection unused
                 */
                public class ${className} extends RecordData {
                    public ${className}(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
                        super(recordDefinition, recordHeader);
                
                        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
                        if (nativeNumber != ${nativeNumber}) {
                            throw new IllegalArgumentException("${className} expects native messages of " + ${nativeNumber} + ", got " + nativeNumber);
                        }
                    }
                """
                .replace("${classCanonicalName}", Objects.requireNonNull(FitCodeGen.class.getCanonicalName()))
                .replace("${className}", className)
                .replace("${nativeNumber}", String.valueOf(nativeFITMessage.getNumber()))
        );

        //
        // Field accessors
        //
        for (final FieldDefinitionPrimitive primitive : nativeFITMessage.getFieldDefinitionPrimitives()) {
            final FieldClass fieldType = getFieldType(primitive);
            final String fieldTypeName;
            final String accessorTemplate;
            if (fieldType.isArray()) {
                fieldTypeName = fieldType.getSimpleName().replace("[]", "");
                accessorTemplate = """
                        
                            @Nullable
                            public ${fieldType}[] ${getterName}() {
                                return getArrayFieldByNumber(${primitiveNumber}, ${fieldType}.class);
                            }
                        """;
            } else {
                fieldTypeName = fieldType.getSimpleName();
                accessorTemplate = """
                        
                            @Nullable
                            public ${fieldType} ${getterName}() {
                                return getFieldByNumber(${primitiveNumber}, ${fieldType}.class);
                            }
                        """;
            }

            sb.append(
                    accessorTemplate
                            .replace("${fieldType}", fieldTypeName)
                            .replace("${getterName}", method("get", primitive))
                            .replace("${primitiveNumber}", String.valueOf(primitive.getNumber()))
            );
        }

        //
        // Builder
        //
        sb.append("""
                
                    /**
                     * @noinspection unused
                     */
                    public static class Builder extends FitRecordDataBuilder {
                        public Builder() {
                            super(${nativeNumber});
                        }
                """
                .replace("${nativeNumber}", String.valueOf(nativeFITMessage.getNumber()))
        );

        for (final FieldDefinitionPrimitive primitive : nativeFITMessage.getFieldDefinitionPrimitives()) {
            final FieldClass fieldType = getFieldType(primitive);
            final String fieldTypeName = fieldType.getSimpleName();

            sb.append("""
                    
                            public Builder ${setterName}(final ${fieldType} value) {
                                setFieldByNumber(${primitiveNumber}, ${castIfArray}value);
                                return this;
                            }
                    """
                    .replace("${setterName}", method("set", primitive))
                    .replace("${fieldType}", fieldTypeName)
                    .replace("${primitiveNumber}", String.valueOf(primitive.getNumber()))
                    .replace("${castIfArray}", fieldType.isArray() ? "(Object[]) " : "")
            );
        }

        sb.append("""
                
                        @Override
                        public ${className} build() {
                            return (${className}) super.build();
                        }
                
                        @Override
                        public ${className} build(final int localMessageType) {
                            return (${className}) super.build(localMessageType);
                        }
                    }
                """
                .replace("${className}", className)
        );
        sb.append("}\n");

        final String output = sb.toString().replaceAll("\\R", System.lineSeparator());

        Files.writeString(outputFile.toPath(), output, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        if (Boolean.getBoolean("verbose")) {
            System.out.println("Written " + outputFile.getCanonicalPath());
        }
    }

    private static FieldClass getFieldType(final FieldDefinitionPrimitive primitive) {
        if (primitive.getType() != null) {
            return switch (primitive.getType()) {
                case "ALARM" -> new FieldClass(LocalTime.class);
                case "ARRAY" ->
                        primitive.getBaseType().contentEquals("STRING") ? new FieldClass(String[].class) : new FieldClass(Number[].class);
                case "BOOLEAN" -> new FieldClass(Boolean.class);
                case "DAY_OF_WEEK" -> new FieldClass(DayOfWeek.class);
                case "EXERCISE_CATEGORY" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionExerciseCategory.ExerciseCategory[]", true);
                case "ALARM_LABEL" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarmLabel.Label");
                case "FILE_TYPE" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType.FILETYPE");
                case "GOAL_SOURCE" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalSource.Source");
                case "GOAL_TYPE" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalType.Type");
                case "HRV_STATUS" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrvStatus.HrvStatus");
                case "HR_TIME_IN_ZONE" -> new FieldClass(Double[].class);
                case "HR_ZONE_HIGH_BOUNDARY" -> new FieldClass(Integer[].class);
                case "MEASUREMENT_SYSTEM" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem.Type");
                case "TEMPERATURE" -> new FieldClass(Integer.class);
                case "TIMESTAMP" -> new FieldClass(Long.class);
                case "WEATHER_CONDITION" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition.Condition");
                case "LANGUAGE" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLanguage.Language");
                case "SLEEP_STAGE" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage.SleepStage");
                case "WEATHER_AQI" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherAqi.AQI_LEVELS");
                case "COORDINATE" -> new FieldClass(Double.class);
                case "SWIM_STYLE" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSwimStyle.SwimStyle");
                case "LOCATION_SYMBOL" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLocationSymbol.LocationSymbol");
                case "COURSE_POINT" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionCoursePoint.CoursePoint");
                case "WEATHER_REPORT" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherReport.Type");
                case "BATTERY_STATUS" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionBatteryStatus.BatteryStatus");
                case "WATER_TYPE" ->
                        new FieldClass("nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWaterType.WaterType");
                default ->
                        throw new RuntimeException("getFieldType doesn't support: " + primitive.type);
            };
        }

        switch (primitive.getBaseType()) {
            case "ENUM":
            case "SINT8":
            case "UINT8":
            case "SINT16":
            case "UINT16":
            case "UINT8Z":
            case "UINT16Z":
            case "BASE_TYPE_BYTE":
                if (primitive.getScale() != 1) {
                    return new FieldClass(Float.class);
                } else {
                    return new FieldClass(Integer.class);
                }
            case "SINT32":
            case "UINT32":
            case "UINT32Z":
            case "SINT64":
            case "UINT64":
            case "UINT64Z":
                if (primitive.getScale() != 1) {
                    return new FieldClass(Double.class);
                } else {
                    return new FieldClass(Long.class);
                }
            case "STRING":
                return new FieldClass(String.class);
            case "FLOAT32":
                return new FieldClass(Float.class);
            case "FLOAT64":
                return new FieldClass(Double.class);
        }

        throw new RuntimeException("Unknown base type " + primitive.getBaseType());
    }

    private static String toCamelCase(final String str) {
        final StringBuilder sb = new StringBuilder(str.toLowerCase(Locale.ROOT));

        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '_') {
                sb.deleteCharAt(i);
                sb.replace(i, i + 1, String.valueOf(Character.toUpperCase(sb.charAt(i))));
            }
        }

        return sb.toString();
    }

    private static String method(final String methodName, final FieldDefinitionPrimitive primitive) {
        return methodName + capitalize(toCamelCase(primitive.getName()));
    }

    private static String capitalize(final String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Get all the imports from an existing java file.
     */
    private static List<String> getImports(final File file) throws IOException {
        if (file.exists()) {
            final String fileContents = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            final List<String> imports = new ArrayList<>();

            final Matcher m = Pattern.compile("import (.+);")
                    .matcher(fileContents);
            while (m.find()) {
                imports.add(m.group(1));
            }
            return imports;
        }

        return Collections.emptyList();
    }

    static class FieldDefinitionPrimitive implements Comparable<FieldDefinitionPrimitive> {
        int num;
        String name;
        String type;
        String baseType;
        double scale;
        double offset;
        int arrayLen;
        int stringLen;
        String UOM;

        @Override
        public int compareTo(final FieldDefinitionPrimitive o) {
            return Integer.compare(num, o.num);
        }

        String getName() {
            return name;
        }

        String getType() {
            return type;
        }

        String getBaseType() {
            return baseType;
        }

        int getNumber() {
            return num;
        }

        double getScale() {
            return scale;
        }
    }

    static final class NativeFITMessage implements Comparable<NativeFITMessage> {
        int num;
        String name;
        SortedSet<FieldDefinitionPrimitive> fields;

        NativeFITMessage() {
            num = -1;
            name = null;
            fields = new TreeSet<>();
        }

        @Override
        public int compareTo(final NativeFITMessage o) {
            return Integer.compare(num, o.num);
        }

        int getNumber() {
            return num;
        }

        String name() {
            return name;
        }

        Set<FieldDefinitionPrimitive> getFieldDefinitionPrimitives() {
            return fields;
        }
    }

    record FieldClass(String CanonicalName, boolean isArray) {
        FieldClass(final String canonicalName) {
            this(canonicalName, false);
        }

        FieldClass(final Class<?> clazz) {
            this(clazz.getCanonicalName(), clazz.isArray());
        }

        String getCanonicalName() {
            return CanonicalName;
        }

        String getSimpleName() {
            int index = CanonicalName.lastIndexOf('.');
            return CanonicalName.substring(index + 1);
        }
    }
}