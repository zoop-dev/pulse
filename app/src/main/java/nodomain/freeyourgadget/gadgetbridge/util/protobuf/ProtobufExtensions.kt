package nodomain.freeyourgadget.gadgetbridge.util.protobuf

import com.google.protobuf.GeneratedMessageLite

inline fun <T : GeneratedMessageLite<T, B>, B : GeneratedMessageLite.Builder<T, B>> B.buildWith(block: B.() -> Unit): T {
    this.apply(block)
    return this.build()
}
