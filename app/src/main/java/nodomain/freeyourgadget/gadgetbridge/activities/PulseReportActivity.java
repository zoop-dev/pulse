/*  Copyright (C) 2026 Pulse

    This file is part of Pulse, a Garmin-only fork of Gadgetbridge.

    Pulse is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PulseLogReporter;

public class PulseReportActivity extends AbstractGBActivity {

    public static final String EXTRA_CRASH_TRACE = "pulse_crash_trace";

    private MaterialButton sendButton;
    private String crashTrace;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AbstractGBActivity.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_report);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.pulse_report_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        crashTrace = getIntent().getStringExtra(EXTRA_CRASH_TRACE);
        if (!TextUtils.isEmpty(crashTrace)) {
            findViewById(R.id.report_crash_banner).setVisibility(View.VISIBLE);
        }

        sendButton = findViewById(R.id.report_send);
        sendButton.setOnClickListener(v -> confirmAndSend());

        final MaterialButton enableLogging = findViewById(R.id.report_enable_logging);
        enableLogging.setOnClickListener(v -> {
            try {
                FileUtils.getExternalFilesDir();
                Logging.getInstance().setFileLoggingEnabled(true);
                nodomain.freeyourgadget.gadgetbridge.GBApplication.getPrefs().getPreferences()
                        .edit().putBoolean("log_to_file", true).apply();
            } catch (final IOException e) {
                GB.toast(this, getString(R.string.error_creating_directory_for_logfiles,
                        e.getLocalizedMessage()), Toast.LENGTH_LONG, GB.ERROR, e);
                return;
            }
            refreshLoggingState();
            GB.toast(this, getString(R.string.pulse_report_logging_on), Toast.LENGTH_LONG, GB.INFO);
        });

        refreshLoggingState();
    }

    private void refreshLoggingState() {
        final boolean active = PulseLogReporter.fileLoggingActive();
        findViewById(R.id.report_logging_hint).setVisibility(active ? View.GONE : View.VISIBLE);
        findViewById(R.id.report_enable_logging).setVisibility(active ? View.GONE : View.VISIBLE);
    }

    private void confirmAndSend() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pulse_report_confirm_title)
                .setMessage(R.string.pulse_report_confirm_msg)
                .setPositiveButton(R.string.pulse_report_send, (d, w) -> send())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void send() {
        final String note = ((TextView) findViewById(R.id.report_note)).getText().toString().trim();
        sendButton.setEnabled(false);
        sendButton.setText(R.string.pulse_report_sending);
        PulseLogReporter.submit(this, note, crashTrace, new PulseLogReporter.Callback() {
            @Override
            public void onSuccess(final String ref) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                sendButton.setText(R.string.pulse_report_send);
                sendButton.setEnabled(true);
                showResult(ref);
            }

            @Override
            public void onFailure(final String message) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                sendButton.setText(R.string.pulse_report_send);
                sendButton.setEnabled(true);
                new MaterialAlertDialogBuilder(PulseReportActivity.this)
                        .setTitle(R.string.pulse_report_title)
                        .setMessage(getString(R.string.pulse_report_failed, message))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });
    }

    private void showResult(final String ref) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pulse_report_sent_title)
                .setMessage(getString(R.string.pulse_report_sent_msg, ref))
                .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                .setNeutralButton(R.string.pulse_report_copy_ref, (d, w) -> {
                    final ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("Pulse report", ref));
                        GB.toast(this, getString(R.string.pulse_report_copied), Toast.LENGTH_SHORT, GB.INFO);
                    }
                })
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
