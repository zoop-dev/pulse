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
package nodomain.freeyourgadget.gadgetbridge.model;

import androidx.annotation.DrawableRes;

public class RunnableListIconItem {
    private final String title;
    private final int iconResId;
    private final Runnable action;

    public RunnableListIconItem(final String title,
                                @DrawableRes final int iconResId,
                                final Runnable action) {
        this.title = title;
        this.iconResId = iconResId;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    @DrawableRes
    public int getIconResId() {
        return iconResId;
    }

    public Runnable getAction() {
        return action;
    }
}
