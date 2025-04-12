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
package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.app_specific_notifications.AppSpecificNotificationSettingsDetailActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.packageNameToPebbleMsgSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppSpecificNotificationSettingsAppListAdapter extends RecyclerView.Adapter<AppSpecificNotificationSettingsAppListAdapter.AppNotificationSettingsViewHolder> implements Filterable {
    protected static final Logger LOG = LoggerFactory.getLogger(AppSpecificNotificationSettingsAppListAdapter.class);

    public static final String STRING_EXTRA_PACKAGE_NAME = "packageName";
    public static final String STRING_EXTRA_PACKAGE_TITLE = "packageTitle";

    private final List<String> applicationInfoList;
    private final int mLayoutId;
    private final Context mContext;
    private final GBDevice mDevice;
    private final IdentityHashMap<String, String> mNameMap;

    private ApplicationFilter applicationFilter;

    public AppSpecificNotificationSettingsAppListAdapter(int layoutId, Context context, GBDevice device) {
        mLayoutId = layoutId;
        mContext = context;
        mDevice = device;

        applicationInfoList = getAllApplications();


        // sort the package list by label and blacklist status
        mNameMap = new IdentityHashMap<>(applicationInfoList.size());
        for (String packageName : applicationInfoList) {
            String name = NotificationUtils.getApplicationLabel(mContext, packageName);
            if (name == null) {
                name = packageName;
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
    public AppNotificationSettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);
        return new AppNotificationSettingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final AppNotificationSettingsViewHolder holder, int position) {
        final String packageName = applicationInfoList.get(position);

        holder.deviceAppVersionAuthorLabel.setText(packageName);
        holder.deviceAppNameLabel.setText(mNameMap.get(packageName));
        holder.deviceImageView.setImageDrawable(NotificationUtils.getAppIcon(mContext, packageName));

        holder.itemView.setOnClickListener(v -> {
            Intent intentStartNotificationFilterActivity = new Intent(mContext, AppSpecificNotificationSettingsDetailActivity.class);
            intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_NAME, packageName);
            intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_TITLE, mNameMap.get(packageName));
            intentStartNotificationFilterActivity.putExtra(GBDevice.EXTRA_DEVICE, mDevice);
            mContext.startActivity(intentStartNotificationFilterActivity);
        });

        holder.btnConfigureApp.setOnClickListener(view -> {
            Intent intentStartNotificationFilterActivity = new Intent(mContext, AppSpecificNotificationSettingsDetailActivity.class);
            intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_NAME, packageName);
            intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_TITLE, mNameMap.get(packageName));
            intentStartNotificationFilterActivity.putExtra(GBDevice.EXTRA_DEVICE, mDevice);
            mContext.startActivity(intentStartNotificationFilterActivity);
        });
    }

    /**
     * Returns the applications for which the Gadgetbridge notifications are enabled.
     */
    public List<String> getAllApplications() {
        final List<String> apps = NotificationUtils.getAllApplications(GBApplication.getContext());
        boolean filterInverted = !GBApplication.getPrefs().getString("notification_list_is_blacklist", "true").equals("true");

        final List<String> ret = new LinkedList<>();

        for(String packageName: apps) {
            boolean blacklisted = GBApplication.appIsNotifBlacklisted(packageName) || GBApplication.appIsPebbleBlacklisted(packageNameToPebbleMsgSender(packageName));
            if((!filterInverted && !blacklisted) || (filterInverted && blacklisted)) {
                ret.add(packageName);
            }
        }

        return ret;
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

    public static class AppNotificationSettingsViewHolder extends RecyclerView.ViewHolder {

        final ImageView deviceImageView;
        final TextView deviceAppVersionAuthorLabel;
        final TextView deviceAppNameLabel;
        final ImageView btnConfigureApp;

        AppNotificationSettingsViewHolder(View itemView) {
            super(itemView);

            deviceImageView = itemView.findViewById(R.id.item_image);
            deviceAppVersionAuthorLabel = itemView.findViewById(R.id.item_details);
            deviceAppNameLabel = itemView.findViewById(R.id.item_name);
            btnConfigureApp = itemView.findViewById(R.id.btn_configureApp);
        }

    }

    private class ApplicationFilter extends Filter {

        private final AppSpecificNotificationSettingsAppListAdapter adapter;
        private final List<String> originalList;
        private final List<String> filteredList;

        private ApplicationFilter(AppSpecificNotificationSettingsAppListAdapter adapter, List<String> originalList) {
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
