package nodomain.freeyourgadget.gadgetbridge.util.protobuf

import com.google.protobuf.GeneratedMessageLite

inline fun <T, B : GeneratedMessageLite.Builder<T, B>> B.buildWith(block: B.() -> Unit): T {
    this.apply(block)
    @Suppress("UNCHECKED_CAST")
    return this.build() as T
}
