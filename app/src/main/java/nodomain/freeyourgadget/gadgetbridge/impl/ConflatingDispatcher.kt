package nodomain.freeyourgadget.gadgetbridge.impl

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class ConflatingDispatcher<T : Any>(
    private val executor: Executor,
    private val forward: Consumer<T>
) {
    private val pending = AtomicReference<T?>(null)
    private val dispatchScheduled = AtomicBoolean(false)

    fun offer(value: T) {
        pending.set(value)
        if (dispatchScheduled.compareAndSet(false, true)) {
            executor.execute(::dispatchLatest)
        }
    }

    private fun dispatchLatest() {
        dispatchScheduled.set(false)
        val latest = pending.getAndSet(null)
        if (latest != null) {
            forward.accept(latest)
        }
    }
}