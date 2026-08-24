package org.gadgetbridge.kaitai.aux;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * A .ksy file's top-level `meta` block and its named types, each with their own meta block.
 */
public record KsyFile(Path path, Map<String, Object> meta, List<KsyType> types) {
    public String id() {
        return (String) meta.get("id");
    }
}
