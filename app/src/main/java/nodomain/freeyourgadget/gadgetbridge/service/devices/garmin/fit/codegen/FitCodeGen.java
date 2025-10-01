package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen;

import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalFITMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarmLabel;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionExerciseCategory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalSource;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrvStatus;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLanguage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLocationSymbol;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSwimStyle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherAqi;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

/**
 * This class is only used to generate code, and will not be packaged in the final apk
 *
 * @noinspection ReadWriteStringCanBeUsed
 */
@RequiresApi(api = Build.VERSION_CODES.CUR_DEVELOPMENT)
public class FitCodeGen {
    private static final String COPYRIGHT_HEADER = """
            /*  Copyright (C) 2025 Freeyourgadget
            
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
        // To run this in Android Studio, right click and select "Run 'FitCodeGen.main()' with Coverage"
        // for some reason, the classpath is broken otherwise.
        new FitCodeGen().generate();
    }

    public void generate() throws IOException {
        generateFitRecordDataFactory();

        for (final GlobalFITMessage value : GlobalFITMessage.KNOWN_MESSAGES.values()) {
            generateFitMessageClassFile(value);
        }
    }

    private void generateFitRecordDataFactory() throws IOException {
        final File factoryFile = new File("app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/fit/messages/FitRecordDataFactory.java");

        final List<String> switchCases = new ArrayList<>();
        final List<GlobalFITMessage> globalFITMessages = new ArrayList<>(GlobalFITMessage.KNOWN_MESSAGES.values());
        globalFITMessages.sort(Comparator.comparingInt(GlobalFITMessage::getNumber));

        for (final GlobalFITMessage value : globalFITMessages) {
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
                        return switch (recordDefinition.getGlobalFITMessage().getNumber()) {
                ${switchCases}
                             default -> new RecordData(recordDefinition, recordHeader);
                        };
                    }
                }
                """;
        final String existingHeader = getHeader(factoryFile);

        final String result = template
                .replace("${copyrightHeader}", (existingHeader.isEmpty() ? COPYRIGHT_HEADER : existingHeader).strip())
                .replace("${generatorClass}", Objects.requireNonNull(getClass().getCanonicalName()))
                .replace("${switchCases}", String.join("\n", switchCases));

        FileUtils.copyStringToFile(result, factoryFile, "replace");
    }

    public void generateFitMessageClassFile(final GlobalFITMessage globalFITMessage) throws IOException {
        final String className = "Fit" + capitalize(toCamelCase(globalFITMessage.name()));
        final File outputFile = new File(
                "app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/fit/messages/",
                className + ".java"
        );

        //
        // Collect all imports
        //
        final List<String> imports = new ArrayList<>(List.of(
                Nullable.class.getCanonicalName(),
                FitRecordDataBuilder.class.getCanonicalName(),
                RecordData.class.getCanonicalName(),
                RecordDefinition.class.getCanonicalName(),
                RecordHeader.class.getCanonicalName()
        ));
        imports.addAll(getImports(outputFile));

        //
        // Add field-specific imports
        //
        for (final GlobalFITMessage.FieldDefinitionPrimitive primitive : globalFITMessage.getFieldDefinitionPrimitives()) {
            final Class<?> fieldType = getFieldType(primitive);
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
        String header = getHeader(outputFile);
        sb.append((header.isEmpty() ? COPYRIGHT_HEADER : header).strip()).append("\n");
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
        for (String i : uniqueImports) {
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
                
                        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
                        if (globalNumber != ${globalNumber}) {
                            throw new IllegalArgumentException("${className} expects global messages of " + ${globalNumber} + ", got " + globalNumber);
                        }
                    }
                """
                .replace("${classCanonicalName}", Objects.requireNonNull(getClass().getCanonicalName()))
                .replace("${className}", className)
                .replace("${globalNumber}", String.valueOf(globalFITMessage.getNumber()))
        );

        //
        // Field accessors
        //
        for (final GlobalFITMessage.FieldDefinitionPrimitive primitive : globalFITMessage.getFieldDefinitionPrimitives()) {
            final Class<?> fieldType = getFieldType(primitive);
            final String fieldTypeName;
            final String accessorTemplate;
            if (fieldType.isArray()) {
                fieldTypeName = fieldType.getSimpleName().replace("[]", "");
                accessorTemplate = """
                        
                            @Nullable
                            public ${fieldType}[] ${getterName}() {
                                final Object object = getFieldByNumber(${primitiveNumber});
                                if (object == null)
                                    return null;
                                if (!object.getClass().isArray()) {
                                    return new ${fieldType}[]{(${fieldType}) object};
                                }
                                final Object[] objectsArray = (Object[]) object;
                                final ${fieldType}[] ret = new ${fieldType}[objectsArray.length];
                                for (int i = 0; i < objectsArray.length; i++) {
                                    ret[i] = (${fieldType}) objectsArray[i];
                                }
                                return ret;
                            }
                        """;
            } else {
                fieldTypeName = fieldType.getSimpleName();
                accessorTemplate = """
                        
                            @Nullable
                            public ${fieldType} ${getterName}() {
                                return (${fieldType}) getFieldByNumber(${primitiveNumber});
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
                            super(${globalNumber});
                        }
                """
                .replace("${globalNumber}", String.valueOf(globalFITMessage.getNumber()))
        );

        for (final GlobalFITMessage.FieldDefinitionPrimitive primitive : globalFITMessage.getFieldDefinitionPrimitives()) {
            final Class<?> fieldType = getFieldType(primitive);
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
                    }
                """
                .replace("${className}", className)
        );

        //
        // Preserve manual changes if any
        //
        if (outputFile.exists()) {
            final String fileContents = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
            final int manualChangesIndex = fileContents.indexOf("// manual changes below");
            if (manualChangesIndex > 0) {
                sb.append("\n    ").append(fileContents.substring(manualChangesIndex));
            } else {
                sb.append("}\n");
            }
        } else {
            sb.append("}\n");
        }

        FileUtils.copyStringToFile(sb.toString(), outputFile, "replace");
    }

    public Class<?> getFieldType(final GlobalFITMessage.FieldDefinitionPrimitive primitive) {
        if (primitive.getType() != null) {
            return switch (primitive.getType()) {
                case ALARM -> LocalTime.class;
                case ARRAY -> primitive.getBaseType() == BaseType.STRING ? String[].class : Number[].class;
                case BOOLEAN -> Boolean.class;
                case DAY_OF_WEEK -> DayOfWeek.class;
                case EXERCISE_CATEGORY -> FieldDefinitionExerciseCategory.ExerciseCategory[].class;
                case ALARM_LABEL -> FieldDefinitionAlarmLabel.Label.class;
                case FILE_TYPE -> FileType.FILETYPE.class;
                case GOAL_SOURCE -> FieldDefinitionGoalSource.Source.class;
                case GOAL_TYPE -> FieldDefinitionGoalType.Type.class;
                case HRV_STATUS -> FieldDefinitionHrvStatus.HrvStatus.class;
                case HR_TIME_IN_ZONE -> Double[].class;
                case HR_ZONE_HIGH_BOUNDARY -> Integer[].class;
                case MEASUREMENT_SYSTEM -> FieldDefinitionMeasurementSystem.Type.class;
                case TEMPERATURE -> Integer.class;
                case TIMESTAMP -> Long.class;
                case WEATHER_CONDITION -> FieldDefinitionWeatherCondition.Condition.class;
                case LANGUAGE -> FieldDefinitionLanguage.Language.class;
                case SLEEP_STAGE -> FieldDefinitionSleepStage.SleepStage.class;
                case WEATHER_AQI -> FieldDefinitionWeatherAqi.AQI_LEVELS.class;
                case COORDINATE -> Double.class;
                case SWIM_STYLE -> FieldDefinitionSwimStyle.SwimStyle.class;
                case LOCATION_SYMBOL -> FieldDefinitionLocationSymbol.LocationSymbol.class;
            };
        }

        switch (primitive.getBaseType()) {
            case ENUM:
            case SINT8:
            case UINT8:
            case SINT16:
            case UINT16:
            case UINT8Z:
            case UINT16Z:
            case BASE_TYPE_BYTE:
                if (primitive.getScale() != 1) {
                    return Float.class;
                } else {
                    return Integer.class;
                }
            case SINT32:
            case UINT32:
            case UINT32Z:
            case SINT64:
            case UINT64:
            case UINT64Z:
                if (primitive.getScale() != 1) {
                    return Double.class;
                } else {
                    return Long.class;
                }
            case STRING:
                return String.class;
            case FLOAT32:
                return Float.class;
            case FLOAT64:
                return Double.class;
        }

        throw new RuntimeException("Unknown base type " + primitive.getBaseType());
    }

    private String toCamelCase(final String str) {
        final StringBuilder sb = new StringBuilder(str.toLowerCase());

        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '_') {
                sb.deleteCharAt(i);
                sb.replace(i, i + 1, String.valueOf(Character.toUpperCase(sb.charAt(i))));
            }
        }

        return sb.toString();
    }

    public String method(final String methodName, final GlobalFITMessage.FieldDefinitionPrimitive primitive) {
        return methodName + capitalize(toCamelCase(primitive.getName()));
    }

    private String capitalize(final String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Get a java file header, before the package, if any.
     */
    private static String getHeader(final File file) throws IOException {
        if (file.exists()) {
            final String fileContents = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            final int packageIndex = fileContents.indexOf("package") - 1;
            if (packageIndex > 0) {
                return fileContents.substring(0, packageIndex);
            }
        }

        return "";
    }

    /**
     * Get all the imports from an existing java file.
     */
    private static List<String> getImports(final File file) throws IOException {
        if (file.exists()) {
            final String fileContents = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
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
}
