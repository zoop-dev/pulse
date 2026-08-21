/*  Copyright (C) 2026 Toby Murray

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.una

/** Reassembles one windowed file read and decides when to ask for the next window. */
internal class UnaReadWindow(private val windowSize: Int) {
    enum class Next { WAIT, REQUEST_NEXT_WINDOW, COMPLETE }

    private val chunks = mutableMapOf<Int, ByteArray>()
    private var windowEnd = windowSize
    private var total = 0

    var firstByteNotHeld = 0
        private set

    var notifications = 0
        private set

    var requests = 1
        private set

    var sawShortDelivery = false
        private set

    fun accept(chunk: UnaFtsReadChunk): Next {
        total = chunk.total
        notifications++
        sawShortDelivery = sawShortDelivery || chunk.deliveredLessThanAdvertised
        chunks[chunk.offset] = chunk.payload
        firstByteNotHeld = firstGapAfter(firstByteNotHeld)

        if (firstByteNotHeld >= total) return Next.COMPLETE
        // Firmware that overstates a chunk length sends no more of the window, so stop waiting for
        // it: https://github.com/UNAWatch/una-sdk/issues/272
        if (chunk.deliveredLessThanAdvertised || firstByteNotHeld >= windowEnd) {
            windowEnd = firstByteNotHeld + windowSize
            requests++
            return Next.REQUEST_NEXT_WINDOW
        }
        return Next.WAIT
    }

    private fun firstGapAfter(offset: Int): Int {
        var end = offset
        while (true) {
            end += (chunks[end] ?: return end).size
        }
    }

    /** Valid once [accept] has returned [Next.COMPLETE]. */
    fun assemble(): ByteArray {
        val bytes = ByteArray(total)
        for ((offset, payload) in chunks) {
            val bytesWithinFile = minOf(payload.size, total - offset)
            if (bytesWithinFile > 0) System.arraycopy(payload, 0, bytes, offset, bytesWithinFile)
        }
        return bytes
    }
}
