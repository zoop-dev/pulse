package nodomain.freeyourgadget.gadgetbridge.activities.multipoint

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import nodomain.freeyourgadget.gadgetbridge.R

class MultipointDeviceAdapter(
    private val devices: List<MultipointDevice>,
    private val onAction: (MultipointDevice, Action) -> Unit
) : RecyclerView.Adapter<MultipointDeviceAdapter.DeviceViewHolder>() {

    var allowAction = false

    enum class Action {
        CONNECT,
        DISCONNECT
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceIcon: ImageView = itemView.findViewById(R.id.device_icon)
        val deviceName: TextView = itemView.findViewById(R.id.device_name)
        val deviceAddress: TextView = itemView.findViewById(R.id.device_address)
        val connectionButton: Button = itemView.findViewById(R.id.connection_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_multipoint_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        val context = holder.itemView.context

        holder.deviceName.text = device.name ?: context.getString(R.string.unknown)
        holder.deviceAddress.text = device.address

        val (icon, buttonText, action) = if (device.isConnected) {
            Triple(
                R.drawable.ic_bluetooth_connected,
                context.getString(R.string.controlcenter_disconnect),
                Action.DISCONNECT
            )
        } else {
            Triple(
                R.drawable.ic_bluetooth_disabled,
                context.getString(R.string.connect),
                Action.CONNECT
            )
        }

        val allowConnect = allowAction && devices.count { it.isConnected } < 2
        val allowDisconnect = allowAction

        holder.deviceIcon.setImageResource(icon)
        holder.connectionButton.text = buttonText
        holder.connectionButton.isEnabled = when (action) {
            Action.CONNECT -> allowConnect
            Action.DISCONNECT -> allowDisconnect
        }
        holder.connectionButton.setOnClickListener {
            onAction(device, action)
        }
    }

    override fun getItemCount(): Int = devices.size
}
