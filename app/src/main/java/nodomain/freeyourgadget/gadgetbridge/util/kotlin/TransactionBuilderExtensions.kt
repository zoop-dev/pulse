package nodomain.freeyourgadget.gadgetbridge.util.kotlin

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport

inline fun AbstractBTLESingleDeviceSupport.withTransaction(
    name: String,
    block: (nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder) -> Unit
) {
    val builder = createTransactionBuilder(name)
    block(builder)
    builder.queue()
}
