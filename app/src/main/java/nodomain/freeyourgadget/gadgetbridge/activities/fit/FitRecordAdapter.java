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
package nodomain.freeyourgadget.gadgetbridge.activities.fit;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalFITMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;

public class FitRecordAdapter extends RecyclerView.Adapter<FitRecordAdapter.FitRecordViewHolder> {
    protected static final Logger LOG = LoggerFactory.getLogger(FitRecordAdapter.class);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private final List<RecordData> fitRecords;
    private final List<RecordData> filteredRecords;
    private final Set<GlobalFITMessage> filter = new HashSet<>();
    private final Context mContext;

    public FitRecordAdapter(final Context context, final FitFile fitFile) {
        mContext = context;
        fitRecords = new ArrayList<>(fitFile.getRecords());
        filteredRecords = new ArrayList<>(fitRecords.size());
        refreshFilter();
    }

    @NonNull
    @Override
    public FitRecordViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.item_fit_record, parent, false);
        return new FitRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final FitRecordViewHolder holder, int position) {
        final RecordData record = filteredRecords.get(position);

        holder.title.setText(record.getGlobalFITMessage().name());
        if (record.getComputedTimestamp() != null) {
            holder.description.setText(SDF.format(new Date(record.getComputedTimestamp() * 1000L)));
        } else {
            holder.description.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            final String recordInfo = record.getFieldDataList().stream()
                    .sorted(Comparator.comparingInt(RecordData.FieldData::getNumber))
                    .map(fieldData -> {
                        final String fieldName;
                        if (!StringUtils.isBlank(fieldData.getName())) {
                            fieldName = fieldData.getName();
                        } else {
                            fieldName = "unknown_" + fieldData.getNumber() + fieldData;
                        }
                        Object o = fieldData.decode();
                        final String fieldValueString;
                        if (o == null) {
                            fieldValueString = "null";
                        } else if (o instanceof Object[]) {
                            fieldValueString = "[" + StringUtils.join((Object[]) o, ",") + "]";
                        } else {
                            fieldValueString = o.toString();
                        }
                        return fieldName + " = " + fieldValueString;
                    }).collect(Collectors.joining("\n"));

            new MaterialAlertDialogBuilder(mContext)
                    .setCancelable(true)
                    .setTitle(record.getGlobalFITMessage().name())
                    .setMessage(recordInfo)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                    })
                    .setNeutralButton(android.R.string.copy, (dialog, which) -> {
                        final ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        final ClipData clip = ClipData.newPlainText(record.getGlobalFITMessage().name(), recordInfo);
                        clipboard.setPrimaryClip(clip);
                    })
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return filteredRecords.size();
    }

    public void updateFilter(final Set<GlobalFITMessage> filter) {
        this.filter.clear();
        this.filter.addAll(filter);
        refreshFilter();
    }

    private void refreshFilter() {
        filteredRecords.clear();
        if (filter.isEmpty()) {
            filteredRecords.addAll(fitRecords);
        } else {
            filteredRecords.addAll(fitRecords.stream().filter(r -> filter.contains(r.getGlobalFITMessage())).collect(Collectors.toList()));
        }
    }

    public static class FitRecordViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView description;

        FitRecordViewHolder(final View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.fit_record_title);
            description = itemView.findViewById(R.id.fit_record_description);
        }
    }
}
