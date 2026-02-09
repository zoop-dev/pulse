package nodomain.freeyourgadget.gadgetbridge.util;

import java.io.Serializable;
import java.util.function.Supplier;

public class CachedValue<V extends Serializable> implements Serializable {
    private volatile V value = null;

    public synchronized V get(final Supplier<V> supplier) {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }

    public synchronized void clear() {
        value = null;
    }

    public synchronized boolean hasValue() {
        return value != null;
    }

    public synchronized boolean isEmpty() {
        return value == null;
    }
}
