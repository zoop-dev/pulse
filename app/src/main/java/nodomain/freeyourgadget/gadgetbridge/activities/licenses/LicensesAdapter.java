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
package nodomain.freeyourgadget.gadgetbridge.activities.licenses;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class LicensesAdapter extends RecyclerView.Adapter<LicensesAdapter.LicenseViewHolder> {
    protected static final Logger LOG = LoggerFactory.getLogger(LicensesAdapter.class);

    private final List<License> mLicensesList;
    private final Context mContext;

    public LicensesAdapter(final Context context, final List<License> licenses) {
        mContext = context;
        mLicensesList = licenses;
    }

    @NonNull
    @Override
    public LicenseViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.item_license, parent, false);
        return new LicenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final LicenseViewHolder holder, int position) {
        final License license = mLicensesList.get(position);

        holder.name.setText(license.getName());
        holder.description.setText(mContext.getString(R.string.date_placeholders__date__time, license.getOwner(), license.getType()));
        holder.url.setText(license.getUrl());
        holder.itemView.setOnClickListener(v -> onLicenseClick(license));
    }

    @Override
    public int getItemCount() {
        return mLicensesList.size();
    }

    private void onLicenseClick(final License license) {
        if (license.getPath() == null) {
            mContext.startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(license.getUrl())
            ));
            return;
        }

        final String licenseText;

        try (InputStream in = mContext.getAssets().open(license.getPath())) {
            final byte[] licenseBytes = FileUtils.readAll(in, 100 * 1024);
            licenseText = new String(licenseBytes, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            GB.toast("Failed to read license text", Toast.LENGTH_LONG, GB.ERROR, e);
            return;
        }

        new MaterialAlertDialogBuilder(mContext)
                .setCancelable(true)
                .setTitle(license.getName())
                .setMessage(licenseText)
                .setNeutralButton(R.string.dialog_open_url, (dialog, which) -> {
                    mContext.startActivity(new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(license.getUrl())
                    ));
                })
                .setPositiveButton(R.string.dialog_close, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    public static class LicenseViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView description;
        final TextView url;

        LicenseViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.license_name);
            description = itemView.findViewById(R.id.license_description);
            url = itemView.findViewById(R.id.license_url);
        }
    }
}
