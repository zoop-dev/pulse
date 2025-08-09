package nodomain.freeyourgadget.gadgetbridge.activities.multipoint

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityMultipointPairingBinding
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import org.slf4j.LoggerFactory

class MultipointPairingActivity : AbstractGBActivity() {
    private lateinit var gbDevice: GBDevice
    private lateinit var binding: ActivityMultipointPairingBinding

    private lateinit var deviceAdapter: MultipointDeviceAdapter

    private var devices = mutableListOf<MultipointDevice>()
    private var isMultipointEnabled = false
    private var pairingNewDevice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultipointPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gbDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE)
            ?: throw IllegalArgumentException("GBDevice must not be null")

        initViews()
        setupRecyclerView()
        updateUI()
        registerBroadcastReceiver()
        requestStatus()
        requestDeviceList()
    }

    private fun initViews() {
        binding.multipointEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != isMultipointEnabled) {
                toggleMultipoint(isChecked)
            }
        }

        binding.buttonPairNewDevice.setOnClickListener {
            setPairingMode(!pairingNewDevice)
        }
    }

    private fun setupRecyclerView() {
        deviceAdapter = MultipointDeviceAdapter(devices) { device, action ->
            when (action) {
                MultipointDeviceAdapter.Action.CONNECT -> connectToDevice(device.address)
                MultipointDeviceAdapter.Action.DISCONNECT -> disconnectFromDevice(device.address)
            }
        }

        binding.devicesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.devicesRecyclerView.adapter = deviceAdapter
    }

    private fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_MULTIPOINT_DEVICE_LIST)
            addAction(ACTION_MULTIPOINT_STATUS_UPDATE)
            addAction(ACTION_MULTIPOINT_PAIRING_UPDATE)
            addAction(GBDevice.ACTION_DEVICE_CHANGED)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        setPairingMode(false)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action == null) {
                LOG.warn("Got null intent or action")
                return
            }

            val device = intent.getParcelableExtra<GBDevice>(GBDevice.EXTRA_DEVICE)
            if (device?.address != gbDevice.address) {
                LOG.warn("Got multipoint action {} for {}, but we're {}", intent.action, device, gbDevice)
                return // not for this device
            }

            when (intent.action) {
                ACTION_MULTIPOINT_DEVICE_LIST -> {
                    LOG.debug("Got multipoint device list")
                    val deviceList = intent.getParcelableArrayListExtra<MultipointDevice>(EXTRA_DEVICE_LIST)
                    if (deviceList != null) {
                        updateDeviceList(deviceList)
                    }
                }

                ACTION_MULTIPOINT_STATUS_UPDATE -> {
                    LOG.debug("Got multipoint status update")
                    isMultipointEnabled = intent.getBooleanExtra(EXTRA_MULTIPOINT_ENABLED, false)
                    updateUI()
                }

                ACTION_MULTIPOINT_PAIRING_UPDATE -> {
                    LOG.debug("Got multipoint pairing update")
                    pairingNewDevice = intent.getBooleanExtra(EXTRA_PAIRING_ENABLED, false)
                    updateUI()
                }

                GBDevice.ACTION_DEVICE_CHANGED -> {
                    LOG.debug("Got device update")
                    gbDevice = device
                    updateUI()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDeviceList(deviceList: List<MultipointDevice>) {
        devices.clear()
        devices.addAll(deviceList)
        deviceAdapter.notifyDataSetChanged()

        if (devices.isEmpty()) {
            binding.devicesRecyclerView.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.devicesRecyclerView.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }

        binding.buttonPairNewDevice.isEnabled =
            gbDevice.isInitialized && isMultipointEnabled && devices.count { it.isConnected } < 2
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateUI() {
        binding.multipointEnabled.setOnCheckedChangeListener(null)
        binding.multipointEnabled.isChecked = isMultipointEnabled
        binding.multipointEnabled.isEnabled = gbDevice.isInitialized
        binding.multipointEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != isMultipointEnabled) {
                toggleMultipoint(isChecked)
            }
        }

        binding.buttonPairNewDevice.isEnabled =
            gbDevice.isInitialized && isMultipointEnabled && devices.count { it.isConnected } < 2
        binding.buttonPairNewDevice.text = if (pairingNewDevice) {
            getString(R.string.bluetooth_multipoint_pair_stop)
        } else {
            getString(R.string.bluetooth_multipoint_pair_new)
        }

        deviceAdapter.allowAction = gbDevice.isInitialized && isMultipointEnabled
        deviceAdapter.notifyDataSetChanged()
    }

    private fun toggleMultipoint(enable: Boolean) {
        val action = if (enable) ACTION_MULTIPOINT_ENABLE else ACTION_MULTIPOINT_DISABLE
        sendDeviceIntent(Intent(action))
    }

    private fun requestStatus() {
        sendDeviceIntent(Intent(ACTION_MULTIPOINT_GET_STATUS))
    }

    private fun requestDeviceList() {
        sendDeviceIntent(Intent(ACTION_MULTIPOINT_GET_DEVICES))
    }

    private fun setPairingMode(enabled: Boolean) {
        val intent = Intent(ACTION_MULTIPOINT_START_PAIRING)
        intent.putExtra(EXTRA_PAIRING_ENABLED, enabled)
        sendDeviceIntent(intent)
    }

    private fun connectToDevice(deviceAddress: String) {
        val intent = Intent(ACTION_MULTIPOINT_CONNECT_DEVICE)
        intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress)
        sendDeviceIntent(intent)
    }

    private fun disconnectFromDevice(deviceAddress: String) {
        val intent = Intent(ACTION_MULTIPOINT_DISCONNECT_DEVICE)
        intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress)
        sendDeviceIntent(intent)
    }

    private fun sendDeviceIntent(intent: Intent) {
        intent.putExtra(GBDevice.EXTRA_DEVICE, gbDevice)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MultipointPairingActivity::class.java)

        const val ACTION_MULTIPOINT_DEVICE_LIST =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_MULTIPOINT_DEVICE_LIST"
        const val ACTION_MULTIPOINT_STATUS_UPDATE =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_MULTIPOINT_STATUS_UPDATE"
        const val ACTION_MULTIPOINT_PAIRING_UPDATE =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_MULTIPOINT_PAIRING_UPDATE"
        const val ACTION_MULTIPOINT_ENABLE =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_MULTIPOINT_ENABLE"
        const val ACTION_MULTIPOINT_DISABLE =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_MULTIPOINT_DISABLE"
        const val ACTION_MULTIPOINT_GET_STATUS =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_MULTIPOINT_GET_STATUS"
        const val ACTION_MULTIPOINT_GET_DEVICES =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_MULTIPOINT_GET_DEVICES"
        const val ACTION_MULTIPOINT_CONNECT_DEVICE =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_MULTIPOINT_CONNECT_DEVICE"
        const val ACTION_MULTIPOINT_DISCONNECT_DEVICE =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_MULTIPOINT_DISCONNECT_DEVICE"
        const val ACTION_MULTIPOINT_START_PAIRING =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_MULTIPOINT_START_PAIRING"

        const val EXTRA_DEVICE_LIST = "device_list"
        const val EXTRA_MULTIPOINT_ENABLED = "enabled"
        const val EXTRA_PAIRING_ENABLED = "enabled"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }
}
