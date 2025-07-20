/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import org.apache.commons.lang3.builder.EqualsBuilder;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

class GBDeviceDiffUtil extends DiffUtil.ItemCallback<GBDevice> {
    @Override
    public boolean areItemsTheSame(@NonNull final GBDevice oldItem,
                                   @NonNull final GBDevice newItem) {
        return new EqualsBuilder()
                .append(oldItem.getAddress(), newItem.getAddress())
                .append(oldItem.getName(), newItem.getName())
                .isEquals();
    }

    @Override
    public boolean areContentsTheSame(@NonNull final GBDevice oldItem,
                                      @NonNull final GBDevice newItem) {
        return EqualsBuilder.reflectionEquals(oldItem, newItem);
    }
}
