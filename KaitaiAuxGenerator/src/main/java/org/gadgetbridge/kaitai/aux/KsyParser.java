package org.gadgetbridge.kaitai.aux;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a .ksy file's top-level `meta` block and each type's own `meta` block via SnakeYAML, for
 * {@link AuxGenerator}s to interpret.
 * <p>
 * Only meta blocks are read - seq/instances/enums content is ignored.
 */
public final class KsyParser {
    private KsyParser() {
    }

    public static KsyFile parse(final Path ksyFile) throws IOException {
        final Map<String, Object> root;
        try (InputStream in = Files.newInputStream(ksyFile)) {
            root = new Yaml().load(in);
        }

        final Map<String, Object> topMeta = asMap(root.get("meta"));

        final List<KsyType> types = new ArrayList<>();
        for (final Map.Entry<String, Object> entry : asMap(root.get("types")).entrySet()) {
            final Map<String, Object> typeBody = asMap(entry.getValue());
            types.add(new KsyType(entry.getKey(), asMap(typeBody.get("meta"))));
        }

        return new KsyFile(ksyFile, topMeta, types);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(final Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }
}
