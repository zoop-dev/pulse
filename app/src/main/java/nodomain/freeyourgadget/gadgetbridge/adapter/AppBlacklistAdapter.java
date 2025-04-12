/*  Copyright (C) 2017-2024 abettenburg, AndrewBedscastle, Carsten Pfeiffer,
    Daniele Gobbetti, José Rebelo, Petr Vaněk

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
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.packageNameToPebbleMsgSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppBlacklistAdapter extends RecyclerView.Adapter<AppBlacklistAdapter.AppBLViewHolder> implements Filterable {
    protected static final Logger LOG = LoggerFactory.getLogger(AppBlacklistAdapter.class);

    public static final String STRING_EXTRA_PACKAGE_NAME = "packageName";

    private final List<String> applicationInfoList;
    private final int mLayoutId;
    private final Context mContext;
    private final IdentityHashMap<String, String> mNameMap;

    private ApplicationFilter applicationFilter;

    public AppBlacklistAdapter(int layoutId, Context context) {
        mLayoutId = layoutId;
        mContext = context;

        applicationInfoList = NotificationUtils.getAllApplications(mContext);

        // sort the package list by label and blacklist status
        mNameMap = new IdentityHashMap<>(applicationInfoList.size());
        for (String packageName : applicationInfoList) {
            String name = NotificationUtils.getApplicationLabel(mContext, packageName);
            if (name == null) {
                name = packageName;
            }
            if (GBApplication.appIsNotifBlacklisted(packageName) || GBApplication.appIsPebbleBlacklisted(packageNameToPebbleMsgSender(packageName))) {
                // sort blacklisted first by prefixing with a '!'
                name = "!" + name;
            }
            mNameMap.put(packageName, name);
        }

        Collections.sort(applicationInfoList, (ai1, ai2) -> {
            final String s1 = mNameMap.get(ai1);
            final String s2 = mNameMap.get(ai2);
            return s1.compareToIgnoreCase(s2);
        });

    }

    @NonNull
    @Override
    public AppBlacklistAdapter.AppBLViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);
        return new AppBLViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final AppBlacklistAdapter.AppBLViewHolder holder, int position) {
        final String packageName = applicationInfoList.get(position);

        holder.deviceAppVersionAuthorLabel.setText(packageName);
        holder.deviceAppNameLabel.setText(mNameMap.get(packageName));
        holder.deviceImageView.setImageDrawable(NotificationUtils.getAppIcon(mContext, packageName));

        holder.blacklist_checkbox.setChecked(GBApplication.appIsNotifBlacklisted(packageName));
        holder.blacklist_pebble_checkbox.setChecked(GBApplication.appIsPebbleBlacklisted(packageNameToPebbleMsgSender(packageName)));

        holder.blacklist_pebble_checkbox.setOnClickListener(view -> {
            ((CheckedTextView) view).toggle();
            if (((CheckedTextView) view).isChecked()) {
                GBApplication.addAppToPebbleBlacklist(packageName);
            } else {
                GBApplication.removeFromAppsPebbleBlacklist(packageName);
            }

        });
        holder.itemView.setOnClickListener(v -> {
            CheckedTextView checkBox = (v.findViewById(R.id.item_checkbox));
            checkBox.toggle();
            if (checkBox.isChecked()) {
                GBApplication.addAppToNotifBlacklist(packageName);
            } else {
                GBApplication.removeFromAppsNotifBlacklist(packageName);
            }
        });

        holder.btnConfigureApp.setOnClickListener(view -> {

            if (GBApplication.getPrefs().getString("notification_list_is_blacklist", "true").equals("true")) {
                if (holder.blacklist_checkbox.isChecked()) {
                    GB.toast(mContext, mContext.getString(R.string.toast_app_must_not_be_selected), Toast.LENGTH_SHORT, GB.INFO);
                } else {
                    Intent intentStartNotificationFilterActivity = new Intent(mContext, NotificationFilterActivity.class);
                    intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_NAME, packageName);
                    mContext.startActivity(intentStartNotificationFilterActivity);
                }
            } else {
                if (holder.blacklist_checkbox.isChecked()) {
                    Intent intentStartNotificationFilterActivity = new Intent(mContext, NotificationFilterActivity.class);
                    intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_NAME, packageName);
                    mContext.startActivity(intentStartNotificationFilterActivity);
                } else {
                    GB.toast(mContext, mContext.getString(R.string.toast_app_must_be_selected), Toast.LENGTH_SHORT, GB.INFO);
                }
            }
        });
    }

    public void checkAllApplications() {
        List<String> allApps = NotificationUtils.getAllApplications(mContext);
        Set<String> apps_blacklist = new HashSet<>(allApps);
        GBApplication.setAppsNotifBlackList(apps_blacklist);
        notifyDataSetChanged();
    }

    public void uncheckAllApplications() {
        Set<String> apps_blacklist = new HashSet<>();
        GBApplication.setAppsNotifBlackList(apps_blacklist);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return applicationInfoList.size();
    }

    @Override
    public Filter getFilter() {
        if (applicationFilter == null)
            applicationFilter = new ApplicationFilter(this, applicationInfoList);
        return applicationFilter;
    }

    public static class AppBLViewHolder extends RecyclerView.ViewHolder {

        final CheckedTextView blacklist_checkbox;
        final CheckedTextView blacklist_pebble_checkbox;
        final ImageView deviceImageView;
        final TextView deviceAppVersionAuthorLabel;
        final TextView deviceAppNameLabel;
        final ImageView btnConfigureApp;

        AppBLViewHolder(View itemView) {
            super(itemView);

            blacklist_checkbox = itemView.findViewById(R.id.item_checkbox);
            blacklist_pebble_checkbox = itemView.findViewById(R.id.item_pebble_checkbox);
            deviceImageView = itemView.findViewById(R.id.item_image);
            deviceAppVersionAuthorLabel = itemView.findViewById(R.id.item_details);
            deviceAppNameLabel = itemView.findViewById(R.id.item_name);
            btnConfigureApp = itemView.findViewById(R.id.btn_configureApp);
        }

    }

    private class ApplicationFilter extends Filter {

        private final AppBlacklistAdapter adapter;
        private final List<String> originalList;
        private final List<String> filteredList;

        private ApplicationFilter(AppBlacklistAdapter adapter, List<String> originalList) {
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
                    String name = NotificationUtils.getApplicationLabel(mContext, packageName);
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
            adapter.applicationInfoList.clear();
            adapter.applicationInfoList.addAll((List<String>) filterResults.values);
            adapter.notifyDataSetChanged();
        }
    }

}
