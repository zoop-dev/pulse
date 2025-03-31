/*  Copyright (C) 2024 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.impl;

import java.io.Serializable;
import java.util.ArrayList;

public class GBDeviceMusicPlaylist implements Serializable {
    private final int id;
    private String name;
    private ArrayList<Integer> musicIds;

    public GBDeviceMusicPlaylist(int id, String name, ArrayList<Integer> musicIds) {
        this.id = id;
        this.name = name;
        this.musicIds = musicIds;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Integer> getMusicIds() {
        return musicIds;
    }

    public void setMusicIds(ArrayList<Integer> musicIds) {
        this.musicIds = musicIds;
    }

    @Override
    public String toString() {
        return name;
    }
}
