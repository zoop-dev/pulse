package org.gadgetbridge.kaitai_aux.main;

import java.util.Map;

/**
 * One type definition from inside a .ksy file's `types` section, with its own meta block (if any).
 */
public record KsyType(String id, Map<String, Object> meta) {
}
