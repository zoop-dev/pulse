/*  Copyright (C) 2020-2024 Petr Vaněk

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

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.R;

public class SpinnerWithIconAdapter extends ArrayAdapter<SpinnerWithIconItem> {
    int groupId;
    Activity context;
    ArrayList<SpinnerWithIconItem> list;
    LayoutInflater inflater;

    public SpinnerWithIconAdapter(Activity context, int groupId, int id, ArrayList<SpinnerWithIconItem>
            list) {
        super(context, id, list);
        this.list = list;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.groupId = groupId;
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(groupId, parent, false);
        }
        ImageView imageView = convertView.findViewById(R.id.spinner_item_icon);
        imageView.setImageResource(list.get(position).getImageId());
        TextView textView = convertView.findViewById(R.id.spinner_item_text);
        textView.setText(list.get(position).getText());

        return convertView;
    }

    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);

    }

    public int getItemPositionForSelection(SpinnerWithIconItem item) {
        if (item == null) return -1;
        int index = 0;
        for (SpinnerWithIconItem listItem : list) {
            if (listItem.id.equals(item.id)) {
                return index;
            }
            index++;
        }
        return -1;
    }
}

