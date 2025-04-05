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
package nodomain.freeyourgadget.gadgetbridge.activities.audiorecordings;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.concentus.OpusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.repository.AudioRecordingsRepository;
import nodomain.freeyourgadget.gadgetbridge.entities.AudioRecording;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.AudioUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class AudioRecordingsAdapter extends RecyclerView.Adapter<AudioRecordingsAdapter.AudioRecordingsViewHolder> {
    protected static final Logger LOG = LoggerFactory.getLogger(AudioRecordingsAdapter.class);

    private final List<AudioRecording> recordingsList;
    private final Context mContext;

    private final AudioRecordingFilter filter;

    public AudioRecordingsAdapter(final Context context, final List<AudioRecording> recordings) {
        mContext = context;
        recordingsList = recordings;
        filter = new AudioRecordingFilter(this, recordingsList);
    }

    @NonNull
    @Override
    public AudioRecordingsViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.item_audio_recording, parent, false);
        return new AudioRecordingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final AudioRecordingsViewHolder holder, final int position) {
        final AudioRecording recording = recordingsList.get(position);

        final String name;
        if (!StringUtils.isNullOrEmpty(recording.getLabel())) {
            name = recording.getLabel();
        } else {
            name = DateTimeUtils.formatDateTime(new Date(recording.getTimestamp()));
        }

        holder.itemView.setOnClickListener(v -> {
            try {
                final File wavFile = recordingAsWav(recording, name + ".wav");

                AndroidUtils.viewFile(wavFile.getPath(), "audio/wav", mContext);
            } catch (final IOException e) {
                GB.toast("Failed to share file", Toast.LENGTH_LONG, GB.ERROR, e);
            }
        });

        holder.date.setText(DateTimeUtils.formatDateTime(new Date(recording.getTimestamp())));
        holder.label.setText(recording.getLabel());
        //holder.seekBar.setProgress(0);
        //holder.playbackPosition.setText(formatProgress(0, recording.getDuration()));
        holder.menu.setOnClickListener(view -> {
            final PopupMenu menu = new PopupMenu(mContext, holder.menu);
            menu.inflate(R.menu.menu_audio_recording_item);
            menu.setOnMenuItemClickListener(item -> {
                final int itemId = item.getItemId();

                if (itemId == R.id.audio_recording_item_menu_rename) {
                    final EditText input = new EditText(mContext);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText((recording.getLabel() != null) ? recording.getLabel() : "");
                    final FrameLayout container = new FrameLayout(mContext);
                    final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = mContext.getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                    params.rightMargin = mContext.getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                    input.setLayoutParams(params);
                    container.addView(input);

                    new MaterialAlertDialogBuilder(mContext)
                            .setView(container)
                            .setCancelable(true)
                            .setTitle(mContext.getString(R.string.activity_summary_edit_name_title))
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                String newLabel = input.getText().toString();
                                if (newLabel.isEmpty()) newLabel = null;
                                recording.setLabel(newLabel);
                                AudioRecordingsRepository.insertOrReplace(null, recording);
                                notifyItemChanged(position);
                            })
                            .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                                // do nothing
                            })
                            .show();
                }

                if (itemId == R.id.audio_recording_item_menu_share) {
                    try {
                        final File wavFile = recordingAsWav(recording, name + ".wav");

                        AndroidUtils.shareFile(mContext, wavFile, "audio/wav");
                    } catch (final IOException e) {
                        GB.toast("Failed to share file", Toast.LENGTH_LONG, GB.ERROR, e);
                    }
                    return true;
                }

                if (itemId == R.id.audio_recording_item_menu_delete) {
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.Delete)
                            .setMessage(mContext.getString(R.string.music_delete_confirm_description, name))
                            .setIcon(R.drawable.ic_warning)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                AudioRecordingsRepository.delete(recording);
                                recordingsList.remove(position);
                                notifyItemRemoved(position);
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                    return true;
                }

                return false;
            });
            menu.show();
        });
    }

    private File recordingAsWav(final AudioRecording recording, final String filename) throws IOException {
        final File opusFile = new File(recording.getPath());
        if (!opusFile.getName().endsWith(".opus")) {
            throw new IOException("Unknown file type for " + opusFile.getName());
        }

        final File cacheDir = mContext.getCacheDir();
        final File rawCacheDir = new File(cacheDir, "audio");
        rawCacheDir.mkdir();
        final File wavFile = new File(rawCacheDir, filename);
        wavFile.deleteOnExit();

        try (FileInputStream fin = new FileInputStream(opusFile);
             FileOutputStream outputStream = new FileOutputStream(wavFile)) {
            // FIXME: This should be streamed
            final byte[] opusBytes = FileUtils.readAll(fin, 100 * 1024 * 1024);
            final byte[] pcmBytes = AudioUtils.opusToPcm(opusBytes);
            AudioUtils.writeWavHeader(pcmBytes.length, outputStream);
            outputStream.write(pcmBytes);
        } catch (final OpusException e) {
            throw new IOException("Failed to decode opus", e);
        }

        return wavFile;
    }


    private String formatProgress(final int millis) {
        final long totalSeconds = millis / 1000;
        final long hours = totalSeconds / 3600;
        final long minutes = (totalSeconds % 3600) / 60;
        final long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        }
    }

    private String formatProgress(final int posMillis, final int totalMillis) {
        return String.format(
                Locale.ROOT, "%s / %s",
                formatProgress(posMillis),
                formatProgress(totalMillis)
        );
    }

    @Override
    public int getItemCount() {
        return recordingsList.size();
    }

    public Filter getFilter() {
        return filter;
    }

    public static class AudioRecordingsViewHolder extends RecyclerView.ViewHolder {
        final TextView date;
        final TextView label;
        //final ImageView playbackToggle;
        final ImageView menu;
        //final SeekBar seekBar;
        //final TextView playbackPosition;
        final View divider;

        AudioRecordingsViewHolder(View itemView) {
            super(itemView);

            date = itemView.findViewById(R.id.recordingItemDate);
            label = itemView.findViewById(R.id.recordingItemLabel);
            //playbackToggle = itemView.findViewById(R.id.recordingItemPlaybackToggle);
            menu = itemView.findViewById(R.id.recordingItemMenu);
            //seekBar = itemView.findViewById(R.id.recordingItemSeekBar);
            //playbackPosition = itemView.findViewById(R.id.recordingItemPlaybackPosition);
            divider = itemView.findViewById(R.id.recordingItemDivider);
        }
    }

    private static class AudioRecordingFilter extends Filter {
        private final AudioRecordingsAdapter adapter;
        private final List<AudioRecording> originalList;
        private final List<AudioRecording> filteredList;

        private AudioRecordingFilter(final AudioRecordingsAdapter adapter, final List<AudioRecording> originalList) {
            super();
            this.originalList = new ArrayList<>(originalList);
            this.filteredList = new ArrayList<>();
            this.adapter = adapter;
        }

        @Override
        protected FilterResults performFiltering(final CharSequence filter) {
            filteredList.clear();
            final FilterResults results = new FilterResults();

            if (filter == null || filter.length() == 0)
                filteredList.addAll(originalList);
            else {
                final String filterPattern = filter.toString().toLowerCase().trim();

                for (AudioRecording a : originalList) {
                    if (a.getLabel().toLowerCase().contains(filterPattern)) {
                        filteredList.add(a);
                    }
                }
            }

            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(final CharSequence constraint, final FilterResults filterResults) {
            adapter.recordingsList.clear();
            adapter.recordingsList.addAll((List<AudioRecording>) filterResults.values);
            adapter.notifyDataSetChanged();
        }
    }
}
