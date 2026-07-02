/*  Copyright (C) 2026 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.devices;

import java.util.List;

import androidx.annotation.NonNull;

public interface VibrationPatternDataSource {
    @NonNull
    VibrationPatternData loadData();

    @NonNull
    List<Integer> getSelectableNotificationTypeIds();

    @NonNull
    String getNotificationTypeName(int typeId);

    void requestRefresh();

    void addPattern(@NonNull String name, int notificationTypeId,
                    @NonNull List<VibrationPatternData.Segment> segments);

    void deletePattern(int id);
}
