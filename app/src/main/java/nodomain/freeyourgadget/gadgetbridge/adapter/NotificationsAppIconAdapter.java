/*  Copyright (C) 2025 Me7c7

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

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;

public class NotificationsAppIconAdapter extends RecyclerView.Adapter<NotificationsAppIconAdapter.NotificationsAppIconViewHolder> implements Filterable {

    public static int MAX_SELECT_COUNT = 30;

    private final Context context;

    private final List<String> appList;

    private final List<String> selectedItems;

    private ApplicationFilter applicationFilter;

    public NotificationsAppIconAdapter(Context context, List<String> appList, List<String> selectedItems) {
        this.context = context;
        this.appList = appList;
        this.selectedItems = selectedItems;
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }


    public List<String> getSelectedItems() {
        return selectedItems;
    }


    @Override
    public void onBindViewHolder(final NotificationsAppIconAdapter.NotificationsAppIconViewHolder holder, int position) {
        final String packageName = appList.get(position);

        holder.title.setText(NotificationUtils.getApplicationLabel(context, packageName));
        holder.icon.setImageDrawable(NotificationUtils.getAppIcon(context, packageName));

        holder.checkbox.setChecked(selectedItems.contains(packageName));

        holder.itemView.setOnClickListener(view -> toggleSelection(packageName));

    }

    @NonNull
    @Override
    public NotificationsAppIconAdapter.NotificationsAppIconViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notifications_app_icon, parent, false);
        return new NotificationsAppIconAdapter.NotificationsAppIconViewHolder(view);
    }

    @Override
    public Filter getFilter() {
        if (applicationFilter == null)
            applicationFilter = new ApplicationFilter(this, appList);
        return applicationFilter;
    }

    private void toggleSelection(String packageName) {
        if(selectedItems.size() >= MAX_SELECT_COUNT && !selectedItems.contains(packageName)) {
            GB.toast(this.context.getString(R.string.notifications_app_icon_uploading_limit_reached), Toast.LENGTH_LONG, GB.WARN);
            return;
        }
        if(selectedItems.contains(packageName)) {
            selectedItems.remove(packageName);
        } else {
            selectedItems.add(packageName);
        }
        int position = appList.indexOf(packageName);
        new Handler(context.getMainLooper()).post(() -> notifyItemChanged(position));
    }

    public static class NotificationsAppIconViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkbox;
        final ImageView icon;
        final TextView title;


        NotificationsAppIconViewHolder(View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.item_notifications_app_icon_checkbox);
            icon = itemView.findViewById(R.id.item_notifications_app_icon_image);
            title = itemView.findViewById(R.id.item_notifications_app_icon_title);
        }

    }

    private class ApplicationFilter extends Filter {

        private final NotificationsAppIconAdapter adapter;
        private final List<String> originalList;
        private final List<String> filteredList;

        private ApplicationFilter(NotificationsAppIconAdapter adapter, List<String> originalList) {
            super();
            this.originalList = new ArrayList<>(originalList);
            this.filteredList = new ArrayList<>();
            this.adapter = adapter;
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence filter) {
            filteredList.clear();
            final Filter.FilterResults results = new Filter.FilterResults();

            if (filter == null || filter.length() == 0)
                filteredList.addAll(originalList);
            else {
                final String filterPattern = filter.toString().toLowerCase().trim();

                for (String packageName : originalList) {
                    String name = NotificationUtils.getApplicationLabel(context, packageName);
                    if (TextUtils.isEmpty(name)) {
                        name = packageName;
                    }
                    if (name.toLowerCase().contains(filterPattern) ||
                            (packageName.contains(filterPattern))) {
                        filteredList.add(packageName);
                    }
                }
            }

            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            adapter.appList.clear();
            adapter.appList.addAll((List<String>) filterResults.values);
            adapter.notifyDataSetChanged();
        }
    }


}
