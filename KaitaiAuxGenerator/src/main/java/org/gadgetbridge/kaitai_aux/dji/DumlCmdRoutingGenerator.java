package org.gadgetbridge.kaitai_aux.dji;

import org.gadgetbridge.kaitai_aux.main.AuxGenerator;
import org.gadgetbridge.kaitai_aux.main.KsyFile;
import org.gadgetbridge.kaitai_aux.main.KsyType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.gadgetbridge.kaitai_aux.main.KaitaiNames.pascalCase;
import static org.gadgetbridge.kaitai_aux.main.KaitaiNames.stripPrefix;
import static org.gadgetbridge.kaitai_aux.main.KaitaiNames.stripSuffix;

/**
 * Generates the DUML cmd routing (cmd constants + decoder table) for a .ksy file that contains
 * `-duml-cmdset` meta keys.
 * <p>
 * - `meta."-duml-cmdset"` marks a .ksy as a DUML command set; files without it are ignored.
 * - a type's own `meta."-duml-cmd"` marks it as a command payload; its direction is inferred from
 * the type id's _request/_response suffix (neither suffix means either direction).
 */
public final class DumlCmdRoutingGenerator implements AuxGenerator {
    private enum Direction {
        REQUEST,
        RESPONSE,
        BOTH,
    }

    private record CmdEntry(String constName,
                            String cmdLiteral,
                            int cmdValue,
                            String kaitaiType,
                            Direction direction) {
    }

    @Override
    public void generate(final KsyFile file, final Path outputDir) throws IOException {
        if (!file.meta().containsKey("-duml-cmdset")) {
            return; // not a DUML command set - nothing to generate
        }
        final String ksyId = file.id();
        if (ksyId == null) {
            throw new IllegalStateException(file.path() + ": meta.id is required");
        }
        final String kaitaiOuterClass = pascalCase(ksyId);

        final List<CmdEntry> entries = new ArrayList<>();
        for (final KsyType type : file.types()) {
            final Object cmd = type.meta().get("-duml-cmd");
            if (cmd == null) {
                continue;
            }
            if (!(cmd instanceof Number)) {
                throw new IllegalStateException(file.path() + ": " + type.id() + " has a non-numeric -duml-cmd: " + cmd);
            }
            final int cmdValue = ((Number) cmd).intValue();
            entries.add(new CmdEntry(
                    constName(type.id()),
                    String.format("0x%02X", cmdValue),
                    cmdValue,
                    kaitaiOuterClass + "." + pascalCase(type.id()),
                    direction(type.id())
            ));
        }
        if (entries.isEmpty()) {
            return;
        }

        final String shortName = pascalCase(stripPrefix(ksyId, "duml_"));
        final Path packageDir = outputDir.resolve("nodomain/freeyourgadget/gadgetbridge/service/devices/dji/duml/messages");
        Files.createDirectories(packageDir);
        writeFile(file.path(), packageDir.resolve("Duml" + shortName + "Cmds.kt"), shortName, entries);
    }

    @SuppressWarnings("ExtractMethodRecommender")
    private static void writeFile(
            final Path ksyFile,
            final Path outFile,
            final String shortName,
            final List<CmdEntry> entries
    ) throws IOException {
        final String objectName = shortName + "Cmd";
        final String mapName = shortName.toUpperCase(Locale.ROOT) + "_DECODERS";
        final String kaitaiOuterClass = entries.getFirst().kaitaiType().split("\\.")[0];

        // One constant per distinct command name - a request/response pair for the same cmd
        // shares one entry. Conflicting -duml-cmd values under the same name is an authoring
        // error in the .ksy, not something to silently paper over.
        final Map<String, CmdEntry> constants = new LinkedHashMap<>();
        for (final CmdEntry e : entries) {
            final CmdEntry existing = constants.get(e.constName());
            if (existing != null && existing.cmdValue() != e.cmdValue()) {
                throw new IllegalStateException(
                        ksyFile + ": " + e.constName() + " has conflicting -duml-cmd values ("
                                + existing.cmdLiteral() + " vs " + e.cmdLiteral() + ")"
                );
            }
            constants.putIfAbsent(e.constName(), e);
        }

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(outFile, StandardCharsets.UTF_8))) {
            w.println("// GENERATED CODE - DO NOT EDIT.");
            w.println("// Generated from " + ksyFile.getFileName() + " by :KaitaiAuxGenerator:genKaitaiAux");
            w.println("// Re-run that task (or a full build) after editing the .ksy file.");
            w.println("package nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.messages");
            w.println();
            w.println("import nodomain.freeyourgadget.gadgetbridge.kaitai." + kaitaiOuterClass);
            w.println("import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlPacketType");
            w.println();
            w.println("object " + objectName + " {");
            for (final CmdEntry c : constants.values()) {
                w.println("    const val " + c.constName() + " = " + c.cmdLiteral());
            }
            w.println("}");
            w.println();
            w.println("internal val " + mapName + ": Map<Pair<Int, DumlPacketType>, (ByteArray) -> Any> = mapOf(");
            for (final CmdEntry e : entries) {
                for (final Direction d : directionsFor(e.direction())) {
                    w.println("    (" + objectName + "." + e.constName() + " to DumlPacketType." + d + ") to " +
                            "decoderFor { " + e.kaitaiType() + "(it) },");
                }
            }
            w.println(")");
        }
    }

    private static List<Direction> directionsFor(final Direction d) {
        return d == Direction.BOTH ? List.of(Direction.REQUEST, Direction.RESPONSE) : List.of(d);
    }

    private static String constName(final String typeId) {
        return stripSuffix(stripSuffix(typeId, "_request"), "_response").toUpperCase(Locale.ROOT);
    }

    private static Direction direction(final String typeId) {
        if (typeId.endsWith("_request")) return Direction.REQUEST;
        if (typeId.endsWith("_response")) return Direction.RESPONSE;
        return Direction.BOTH;
    }
}
