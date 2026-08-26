package org.gadgetbridge.kaitai_aux.main;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Takes a parsed .ksy file and generates auxiliary source files. The ksy files can contain "-*" keys in the
 * meta field, since they are ignored by the Kaitai compiler.
 * <p>
 * A generator might be called for ksy files it does not recognize (for example, no known top-level meta key). In such
 * cases, it is expected to no-op.
 */
public interface AuxGenerator {
    void generate(KsyFile file, Path outputDir) throws IOException;
}
