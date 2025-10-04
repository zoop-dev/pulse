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
package nodomain.freeyourgadget.gadgetbridge.activities.heartratezones;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.databinding.FragmentHeartRateSettingsBinding;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusicPlaylist;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZones;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZonesConfig;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZonesSpec;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HeartRateSettingsFragment extends AbstractGBFragment implements MenuProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HeartRateSettingsFragment.class);

    static final String FRAGMENT_TAG = "HUAWEI_HEART_RATE_SETTINGS_FRAGMENT";


    private FragmentHeartRateSettingsBinding binding;

    private GBDevice device;

    private HeartRateZonesSpec spec;

    private List<HeartRateZonesConfig> config;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final Bundle arguments = getArguments();
        if (arguments == null) {
            return null;
        }
        device = arguments.getParcelable(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            return null;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();

        binding = FragmentHeartRateSettingsBinding.inflate(inflater);
        View view = binding.getRoot();

        spec = coordinator.getHeartRateZonesSpec(device);

        config = spec.getDeviceConfig();

        if (config == null) {
            return null;
        }

        for (HeartRateZonesConfig entry : config) {
            binding.hrSettingsTabLayout.addTab(binding.hrSettingsTabLayout.newTab().setText(spec.getNameByType(requireContext(), entry.getType())));
        }

        if (config.size() == 1) {
            binding.hrSettingsTabLayout.setVisibility(View.GONE);
        }

        binding.hrSettingsTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.hrSettingsZones.calculationMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateZones();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        binding.hrSettingsLimits.highHrWarning.setOnCheckedChangeListener((compoundButton, b) -> saveCurrentLimitWarning(b));
        binding.saveHrZonesSettings.setOnClickListener(view2 -> saveCurrentConfig());

        updateData();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onCreateMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.menu_heart_rate_zones, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.hr_zones_reset_all) {
            if (spec != null) {
                new MaterialAlertDialogBuilder(this.requireContext())
                        .setTitle(R.string.hr_settings_zones_remove_all)
                        .setMessage(this.getString(R.string.hr_settings_zones_remove_all_description))
                        .setIcon(R.drawable.ic_warning)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            spec.clearConfig();
                            config = spec.getDeviceConfig();
                            updateData();
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();

                return true;
            }
        }

        return false;
    }


    private static class CalculateMethodItem {
        private final String name;
        private final HeartRateZones.CalculationMethod method;

        public CalculateMethodItem(@NonNull String name, HeartRateZones.CalculationMethod method) {
            this.name = name;
            this.method = method;
        }

        public HeartRateZones.CalculationMethod getMethod() {
            return method;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    private HeartRateZonesConfig getSelectedHRConfig() {
        int pos = binding.hrSettingsTabLayout.getSelectedTabPosition();
        return config.get(pos);
    }

    private void saveCurrentConfig() {
        final HeartRateZonesConfig hrConfig = getSelectedHRConfig();
        spec.saveConfig(hrConfig);
    }

    private void saveCurrentLimitWarning(boolean checked) {
        HeartRateZonesConfig hrConfig = getSelectedHRConfig();
        hrConfig.setWarningEnable(checked);
    }

    private void updateData() {
        final HeartRateZonesConfig hrConfig = getSelectedHRConfig();
        if (hrConfig == null)
            return;

        List<CalculateMethodItem> types = new ArrayList<>();

        List<HeartRateZones> methods = hrConfig.getConfigByMethods();
        for (HeartRateZones zn : methods) {
            types.add(new CalculateMethodItem(HeartRateZones.methodToString(requireContext(), zn.getMethod()), zn.getMethod()));
        }

        CalculateMethodItem[] myStringArray = types.toArray(new CalculateMethodItem[0]);
        ArrayAdapter<CalculateMethodItem> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, myStringArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.hrSettingsZones.calculationMethodSpinner.setAdapter(adapter);

        int idx = 0;
        for (int i = 0; i < myStringArray.length; i++) {
            if (myStringArray[i].method == hrConfig.getCurrentCalculationMethod()) {
                idx = i;
            }
        }
        binding.hrSettingsZones.calculationMethodSpinner.setSelection(idx);

        binding.hrSettingsLimits.limitHighHr.setText(String.valueOf(hrConfig.getWarningHRLimit()));
        binding.hrSettingsLimits.highHrWarning.setChecked(hrConfig.getWarningEnable());

    }

    private void updateZones() {
        HeartRateZonesConfig hrConfig = getSelectedHRConfig();
        if (hrConfig == null)
            return;

        final CalculateMethodItem calcMethod = (CalculateMethodItem) binding.hrSettingsZones.calculationMethodSpinner.getSelectedItem();

        HeartRateZones selected = null;
        List<HeartRateZones> methods = hrConfig.getConfigByMethods();
        for (HeartRateZones zn : methods) {
            if (calcMethod.getMethod() == zn.getMethod()) {
                selected = zn;
                break;
            }
        }

        if (selected == null)
            return;

        hrConfig.setCurrentCalculationMethod(calcMethod.getMethod());

        if (calcMethod.getMethod() == HeartRateZones.CalculationMethod.LTHR) {
            binding.hrSettingsZones.zonesMaxHrTitle.setText(getString(R.string.hr_settings_zones_lactate_threshold_hr));
        } else {
            binding.hrSettingsZones.zonesMaxHrTitle.setText(getString(R.string.hr_settings_zones_max_hr));
        }
        binding.hrSettingsZones.zonesMaxHr.setText(String.valueOf(selected.getHRThreshold()));

        if (calcMethod.getMethod() == HeartRateZones.CalculationMethod.HRR) {
            binding.hrSettingsZones.layoutZonesRestingHr.setVisibility(View.VISIBLE);
            binding.hrSettingsZones.zonesRestingHr.setText(String.valueOf(selected.getHRResting()));
        } else {
            binding.hrSettingsZones.layoutZonesRestingHr.setVisibility(View.GONE);
        }

        if (calcMethod.getMethod() == HeartRateZones.CalculationMethod.LTHR) {
            binding.hrSettingsZones.heartRateZoneMaxValue.setText(getString(R.string.n_a));
            binding.hrSettingsZones.heartRateZoneMaxPercent.setText(getString(R.string.n_a));

        } else {
            binding.hrSettingsZones.heartRateZoneMaxValue.setText(String.valueOf(selected.getHRThreshold()));
            binding.hrSettingsZones.heartRateZoneMaxPercent.setText(String.valueOf(100));
        }

        binding.hrSettingsZones.heartRateZone5Value.setText(String.valueOf(selected.getZone5()));
        binding.hrSettingsZones.heartRateZone4Value.setText(String.valueOf(selected.getZone4()));
        binding.hrSettingsZones.heartRateZone3Value.setText(String.valueOf(selected.getZone3()));
        binding.hrSettingsZones.heartRateZone2Value.setText(String.valueOf(selected.getZone2()));
        binding.hrSettingsZones.heartRateZone1Value.setText(String.valueOf(selected.getZone1()));

        binding.hrSettingsZones.heartRateZone5Percent.setText(String.valueOf(selected.getPercentage(selected.getZone5())));
        binding.hrSettingsZones.heartRateZone4Percent.setText(String.valueOf(selected.getPercentage(selected.getZone4())));
        binding.hrSettingsZones.heartRateZone3Percent.setText(String.valueOf(selected.getPercentage(selected.getZone3())));
        binding.hrSettingsZones.heartRateZone2Percent.setText(String.valueOf(selected.getPercentage(selected.getZone2())));
        binding.hrSettingsZones.heartRateZone1Percent.setText(String.valueOf(selected.getPercentage(selected.getZone1())));
    }

    private void setDevice(final GBDevice device) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putParcelable("device", device);
        setArguments(args);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    static HeartRateSettingsFragment newInstance(final GBDevice device) {
        final HeartRateSettingsFragment fragment = new HeartRateSettingsFragment();
        fragment.setDevice(device);
        return fragment;
    }

    @Nullable
    @Override
    protected CharSequence getTitle() {
        return getString(R.string.heart_rate_settings);
    }

}
