/*  Copyright (C) 2025 Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.weightScale

import android.os.Parcelable
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import kotlinx.parcelize.Parcelize

import java.time.Instant

@Parcelize
class WeightScaleMeasurement(
    @field:FloatRange(from = 0.0) var weightKilogram: Double?,
    var time: Instant?,
    @field:IntRange(from = 0L, to = 255L) var userId: Int?,
    @field:FloatRange(from = 0.0) var heightMeter: Float?,
    @field:FloatRange(from = 0.0) var BMI: Float?
) : Parcelable {
    constructor() : this(null, null, null, null, null)

    override fun toString(): String {
        return "$weightKilogram kg, $time, $userId #, $BMI, $heightMeter m"
    }
}