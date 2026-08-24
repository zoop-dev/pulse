package org.gadgetbridge.kaitai.aux;

import org.gadgetbridge.kaitai.dji.DumlCmdRoutingGenerator;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs the auxiliary code generators for every .ksy file under app/src/main/ksy: parses each with
 * {@link KsyParser} and offers it to every registered {@link AuxGenerator}.
 * <p>
 * Add a plugin to {@link #GENERATORS}.
 */
public final class KaitaiAuxGen {
    private static final List<AuxGenerator> GENERATORS = List.of(
            new DumlCmdRoutingGenerator()
    );

    private KaitaiAuxGen() {
    }

    public static void main(final String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: KaitaiAuxGen <ksyDir> <outputDir>");
        }
        final Path ksyDir = Path.of(args[0]);
        final Path outputDir = Path.of(args[1]);
        Files.createDirectories(outputDir);

        try (DirectoryStream<Path> files = Files.newDirectoryStream(ksyDir, "*.ksy")) {
            for (final Path ksyFile : files) {
                final KsyFile parsed = KsyParser.parse(ksyFile);
                for (final AuxGenerator generator : GENERATORS) {
                    generator.generate(parsed, outputDir);
                }
            }
        }
    }
}
