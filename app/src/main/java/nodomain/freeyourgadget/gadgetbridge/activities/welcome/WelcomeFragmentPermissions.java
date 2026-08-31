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
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
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
    private PermissionAdapter requiredAdapter;
    private PermissionAdapter optionalAdapter;
    private List<String> requestingPermissions = new ArrayList<>();
    private boolean showDoNotAskAgain;
    private boolean requiredOnlyChosen;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentWelcomePermissionsBinding.inflate(getLayoutInflater(), container, false);

        final Bundle arguments = getArguments();
        showDoNotAskAgain = arguments != null && arguments.getBoolean(ARG_SHOW_DO_NOT_ASK_BUTTON, false);
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
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    })
                    .show();
        });

        binding.buttonRequestAll.setOnClickListener(v -> queueAndRequest(tierPermissions(null)));
        binding.buttonRequiredOnly.setOnClickListener(v -> {
            requiredOnlyChosen = true;
            queueAndRequest(tierPermissions(true));
        });

        final ActionBar supportActionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (supportActionBar != null && supportActionBar.isShowing()) {
            binding.permissionsTitle.setVisibility(View.GONE);
        }

        final List<PermissionsUtils.PermissionDetails> required = new ArrayList<>();
        final List<PermissionsUtils.PermissionDetails> optional = new ArrayList<>();
        for (final PermissionsUtils.PermissionDetails permission : PermissionsUtils.getRequiredPermissionsList(requireActivity())) {
            (permission.required() ? required : optional).add(permission);
        }
        sortByGrantThenName(required);
        sortByGrantThenName(optional);

        requiredAdapter = new PermissionAdapter(required, requireContext());
        optionalAdapter = new PermissionAdapter(optional, requireContext());
        binding.permissionsListRequired.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.permissionsListRequired.setAdapter(requiredAdapter);
        binding.permissionsListOptional.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.permissionsListOptional.setAdapter(optionalAdapter);

        if (required.isEmpty()) {
            binding.permissionsRequiredLabel.setVisibility(View.GONE);
        }
        if (optional.isEmpty()) {
            binding.permissionsOptionalLabel.setVisibility(View.GONE);
            binding.permissionsOptionalCaption.setVisibility(View.GONE);
        }

        updateCallout();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        requiredAdapter.resort();
        optionalAdapter.resort();
        updateCallout();
        if (PermissionsUtils.checkAllPermissions(requireActivity())) {
            binding.buttonRequestAll.setEnabled(false);
        }
        if (showDoNotAskAgain
                && (PermissionsUtils.checkAllPermissions(requireActivity())
                    || (requiredOnlyChosen && ungrantedRequiredCount() == 0))) {
            requireActivity().finish();
        }
        if (!requestingPermissions.isEmpty()) {
            requestAllPermissions();
        }
    }

    private void sortByGrantThenName(final List<PermissionsUtils.PermissionDetails> list) {
        list.sort((p1, p2) -> {
            final boolean p1Granted = PermissionsUtils.checkPermission(requireContext(), p1.permission());
            final boolean p2Granted = PermissionsUtils.checkPermission(requireContext(), p2.permission());
            if (p1Granted && !p2Granted) return 1;
            if (!p1Granted && p2Granted) return -1;
            return p1.title().compareToIgnoreCase(p2.title());
        });
    }

    private List<String> tierPermissions(final Boolean required) {
        final List<String> result = new ArrayList<>();
        for (final PermissionsUtils.PermissionDetails permission : PermissionsUtils.getRequiredPermissionsList(requireActivity())) {
            if (required == null || permission.required() == required) {
                result.add(permission.permission());
            }
        }
        return result;
    }

    private int ungrantedRequiredCount() {
        int count = 0;
        for (final PermissionsUtils.PermissionDetails permission : PermissionsUtils.getRequiredPermissionsList(requireActivity())) {
            if (permission.required() && !PermissionsUtils.checkPermission(requireContext(), permission.permission())) {
                count++;
            }
        }
        return count;
    }

    private void updateCallout() {
        final int pending = ungrantedRequiredCount();
        if (pending > 0) {
            binding.permissionsCallout.setVisibility(View.VISIBLE);
            binding.permissionsCalloutText.setText(getResources().getQuantityString(
                    R.plurals.pulse_permissions_still_needed, pending, pending));
            binding.buttonRequiredOnly.setVisibility(View.VISIBLE);
        } else {
            binding.permissionsCallout.setVisibility(View.GONE);
            binding.buttonRequiredOnly.setVisibility(View.GONE);
        }
    }

    private void queueAndRequest(final List<String> permissions) {
        requestingPermissions = new ArrayList<>(permissions);
        requestAllPermissions();
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
            if (combinedPermissions.length > 0) {
                ActivityCompat.requestPermissions(requireActivity(), combinedPermissions, 0);
            }
        }
    }

    private static int accentColor(final Context context) {
        final TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.pulseAccent, typedValue, true) && typedValue.data != 0) {
            return typedValue.data;
        }
        return ContextCompat.getColor(context, R.color.accent_blue);
    }

    private static int softTint(final int color) {
        return (color & 0x00FFFFFF) | 0x24000000;
    }

    private static class PermissionHolder extends RecyclerView.ViewHolder {
        final View chip;
        final ImageView icon;
        final TextView title;
        final TextView summary;
        final TextView requestPill;
        final TextView grantedPill;

        public PermissionHolder(View itemView) {
            super(itemView);
            chip = itemView.findViewById(R.id.permission_chip);
            icon = itemView.findViewById(R.id.permission_icon);
            title = itemView.findViewById(R.id.permission_title);
            summary = itemView.findViewById(R.id.permission_summary);
            requestPill = itemView.findViewById(R.id.permission_request);
            grantedPill = itemView.findViewById(R.id.permission_granted);
        }
    }

    private class PermissionAdapter extends RecyclerView.Adapter<PermissionHolder> {
        private final List<PermissionsUtils.PermissionDetails> permissionList;
        private final Context context;

        public PermissionAdapter(List<PermissionsUtils.PermissionDetails> permissionList, Context context) {
            this.permissionList = permissionList;
            this.context = context;
        }

        void resort() {
            sortByGrantThenName(permissionList);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PermissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.pulse_permission_row, parent, false);
            return new PermissionHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull PermissionHolder holder, int position) {
            final PermissionsUtils.PermissionDetails permission = permissionList.get(position);
            holder.title.setText(permission.title());
            holder.summary.setText(permission.summary());

            final int color = ContextCompat.getColor(context, permission.colorRes());
            holder.icon.setImageResource(permission.iconRes());
            ImageViewCompat.setImageTintList(holder.icon, ColorStateList.valueOf(color));
            holder.chip.setBackgroundTintList(ColorStateList.valueOf(softTint(color)));

            if (PermissionsUtils.checkPermission(requireContext(), permission.permission())) {
                holder.requestPill.setVisibility(View.GONE);
                holder.grantedPill.setVisibility(View.VISIBLE);
                final int mint = ContextCompat.getColor(context, R.color.pulse_mint);
                holder.grantedPill.setBackgroundTintList(ColorStateList.valueOf(softTint(mint)));
                holder.grantedPill.setTextColor(mint);
            } else {
                holder.grantedPill.setVisibility(View.GONE);
                holder.requestPill.setVisibility(View.VISIBLE);
                if (permission.required()) {
                    holder.requestPill.setBackgroundTintList(ColorStateList.valueOf(accentColor(context)));
                    holder.requestPill.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                } else {
                    holder.requestPill.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.pulse_card_alt)));
                    holder.requestPill.setTextColor(ContextCompat.getColor(context, R.color.pulse_text));
                }
                holder.requestPill.setOnClickListener(view ->
                        PermissionsUtils.requestPermission(requireActivity(), permission.permission()));
            }
        }

        @Override
        public int getItemCount() {
            return permissionList.size();
        }
    }
}
