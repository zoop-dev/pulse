/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.welcome;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.databinding.FragmentWelcomePermissionsBinding;
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils;

public class WelcomeFragmentPermissions extends Fragment {
    public static final String ARG_SHOW_DO_NOT_ASK_BUTTON = "show_do_not_ask";

    private FragmentWelcomePermissionsBinding binding;
    private PermissionAdapter permissionAdapter;
    private List<String> requestingPermissions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentWelcomePermissionsBinding.inflate(getLayoutInflater(), container, false);

        final Bundle arguments = getArguments();
        final boolean showDoNotAskAgain = arguments != null && arguments.getBoolean(ARG_SHOW_DO_NOT_ASK_BUTTON, false);
        if (!showDoNotAskAgain) {
            binding.buttonDoNotAskAgain.setVisibility(View.GONE);
        }
        binding.buttonDoNotAskAgain.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setCancelable(true)
                    .setTitle(R.string.first_start_permissions_do_not_ask_again)
                    .setMessage(R.string.first_start_permissions_do_not_ask_warning_summary)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        GBApplication.getPrefs().getPreferences().edit()
                                .putBoolean("permission_pestering", false)
                                .apply();
                        requireActivity().finish();
                    })
                    .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                        // do nothing
                    })
                    .show();
        });

        binding.buttonRequestAll.setOnClickListener(v -> {
            List<PermissionsUtils.PermissionDetails> wantedPermissions = PermissionsUtils.getRequiredPermissionsList(requireActivity());
            requestingPermissions = new ArrayList<>();
            for (PermissionsUtils.PermissionDetails wantedPermission : wantedPermissions) {
                requestingPermissions.add(wantedPermission.permission());
            }
            requestAllPermissions();
        });

        final ActionBar supportActionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (supportActionBar!= null && supportActionBar.isShowing()) {
            // Hide title when the Action Bar is visible (i.e. when not in the first run flow)
            binding.permissionsTitle.setVisibility(View.GONE);
        }

        // Set up RecyclerView
        permissionAdapter = new PermissionAdapter(PermissionsUtils.getRequiredPermissionsList(requireActivity()), requireContext());
        binding.permissionsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.permissionsList.setAdapter(permissionAdapter);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        permissionAdapter.notifyDataSetChanged();
        if (PermissionsUtils.checkAllPermissions(requireActivity())) {
            binding.buttonRequestAll.setEnabled(false);
        }
        if (!requestingPermissions.isEmpty()) {
            requestAllPermissions();
        }
    }

    public void requestAllPermissions() {
        if (!requestingPermissions.isEmpty()) {
            Iterator<String> it = requestingPermissions.iterator();
            while (it.hasNext()) {
                String currentPermission = it.next();
                if (PermissionsUtils.specialPermissions.contains(currentPermission)) {
                    it.remove();
                    if (!PermissionsUtils.checkPermission(requireActivity(), currentPermission)) {
                        PermissionsUtils.requestPermission(requireActivity(), currentPermission);
                        return;
                    }
                }
            }
            String[] combinedPermissions = requestingPermissions.toArray(new String[0]);
            requestingPermissions.clear();
            ActivityCompat.requestPermissions(requireActivity(), combinedPermissions, 0);
        }
    }

    private static class PermissionHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView summaryTextView;
        ImageView checkmarkImageView;
        Button requestButton;

        public PermissionHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.permission_title);
            summaryTextView = itemView.findViewById(R.id.permission_summary);
            checkmarkImageView = itemView.findViewById(R.id.permission_check);
            requestButton = itemView.findViewById(R.id.permission_request);
        }
    }

    private class PermissionAdapter extends RecyclerView.Adapter<PermissionHolder> {
        private final List<PermissionsUtils.PermissionDetails> permissionList;
        private final Context context;

        public PermissionAdapter(List<PermissionsUtils.PermissionDetails> permissionList, Context context) {
            this.permissionList = permissionList;
            this.context = context;
        }

        @NonNull
        @Override
        public PermissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_welcome_permission_row, parent, false);
            return new PermissionHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull PermissionHolder holder, int position) {
            PermissionsUtils.PermissionDetails permissionData = permissionList.get(position);
            holder.titleTextView.setText(permissionData.title());
            holder.summaryTextView.setText(permissionData.summary());
            if (PermissionsUtils.checkPermission(requireContext(), permissionData.permission())) {
                holder.requestButton.setVisibility(View.INVISIBLE);
                holder.requestButton.setEnabled(false);
                holder.checkmarkImageView.setVisibility(View.VISIBLE);
            } else {
                holder.requestButton.setVisibility(View.VISIBLE);
                holder.requestButton.setEnabled(true);
                holder.checkmarkImageView.setVisibility(View.GONE);
                holder.requestButton.setOnClickListener(view -> {
                    PermissionsUtils.requestPermission(requireActivity(), permissionData.permission());
                });
            }
        }

        @Override
        public int getItemCount() {
            return permissionList.size();
        }
    }
}
