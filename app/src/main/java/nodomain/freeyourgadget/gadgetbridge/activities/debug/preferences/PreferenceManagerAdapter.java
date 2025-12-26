/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.activities.debug.preferences;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;

public class PreferenceManagerAdapter extends RecyclerView.Adapter<PreferenceManagerAdapter.DebugPreferenceViewHolder> {
    protected static final Logger LOG = LoggerFactory.getLogger(PreferenceManagerAdapter.class);

    private final List<DebugPreference> preferenceList;
    private final Context mContext;

    private final PreferenceFilter preferenceFilter;

    public PreferenceManagerAdapter(final Context context, final List<DebugPreference> preferenceList) {
        mContext = context;

        this.preferenceList = preferenceList;
        this.preferenceFilter = new PreferenceFilter(this, preferenceList);
    }

    @NonNull
    @Override
    public DebugPreferenceViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.item_debug_preference, parent, false);
        return new DebugPreferenceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final DebugPreferenceViewHolder holder, final int position) {
        final DebugPreference preference = preferenceList.get(position);

        //holder.icon.setVisibility(View.GONE);
        holder.name.setText(preference.key());
        holder.description.setText(preference.value());
        holder.description.setMaxLines(10);
        holder.menu.setVisibility(View.INVISIBLE);
        //holder.menu.setOnClickListener(view -> {
        //    final PopupMenu menu = new PopupMenu(mContext, holder.menu);
        //    menu.inflate(R.menu.file_manager_file);
        //    menu.getMenu().findItem(R.id.file_manager_preference_menu_view).setVisible(file.getPath().toLowerCase(Locale.ROOT).endsWith(".fit"));
        //    menu.setOnMenuItemClickListener(item -> {
        //        final int itemId = item.getItemId();
        //        if (itemId == R.id.file_manager_preference_menu_share) {
        //            try {
        //                AndroidUtils.shareFile(mContext, file, "*/*");
        //            } catch (final IOException e) {
        //                GB.toast("Failed to share file", Toast.LENGTH_LONG, GB.ERROR, e);
        //            }
        //            return true;
        //        }
        //        if (itemId == R.id.file_manager_preference_menu_view) {
        //            final Intent inspectFileIntent = new Intent(mContext, FitViewerActivity.class);
        //            inspectFileIntent.putExtra(FitViewerActivity.EXTRA_PATH, file.getPath());
        //            mContext.startActivity(inspectFileIntent);
        //            return true;
        //        }
        //        return false;
        //    });
        //    menu.show();
        //});
    }

    @Override
    public int getItemCount() {
        return preferenceList.size();
    }

    public Filter getFilter() {
        return preferenceFilter;
    }

    public static class DebugPreferenceViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView description;
        final ImageView menu;

        DebugPreferenceViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.preference_key);
            description = itemView.findViewById(R.id.preference_description);
            menu = itemView.findViewById(R.id.preference_menu);
        }
    }

    private static class PreferenceFilter extends Filter {
        private final PreferenceManagerAdapter adapter;
        private final List<DebugPreference> originalList;
        private final List<DebugPreference> filteredList;

        private PreferenceFilter(final PreferenceManagerAdapter adapter, final List<DebugPreference> originalList) {
            super();
            this.originalList = new ArrayList<>(originalList);
            this.filteredList = new ArrayList<>();
            this.adapter = adapter;
        }

        @Override
        protected FilterResults performFiltering(final CharSequence filter) {
            if (originalList.isEmpty()) {
                originalList.addAll(adapter.preferenceList);
            }

            filteredList.clear();
            final FilterResults results = new FilterResults();

            if (filter == null || filter.length() == 0)
                filteredList.addAll(originalList);
            else {
                final String filterPattern = filter.toString().toLowerCase().trim();

                for (DebugPreference p : originalList) {
                    if (p.key().toLowerCase().contains(filterPattern) || p.value().toLowerCase().contains(filterPattern)) {
                        filteredList.add(p);
                    }
                }
            }

            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(final CharSequence constraint, final FilterResults filterResults) {
            adapter.preferenceList.clear();
            adapter.preferenceList.addAll((List<DebugPreference>) filterResults.values);
            adapter.notifyDataSetChanged();
        }
    }
}
