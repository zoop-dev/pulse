package nodomain.freeyourgadget.gadgetbridge.adapter

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nodomain.freeyourgadget.gadgetbridge.databinding.ListItemDeviceInstallBinding
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice

class DeviceInstallAdapter(
    private val onItemClicked: (GBDevice) -> Unit
) : ListAdapter<GBDevice, DeviceInstallAdapter.DeviceViewHolder>(GBDeviceDiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ListItemDeviceInstallBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class DeviceViewHolder(private val binding: ListItemDeviceInstallBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClicked(getItem(position))
                }
            }
        }

        fun bind(device: GBDevice) {
            binding.textViewDeviceName.text = device.aliasOrName
            binding.imageViewDeviceIcon.setImageResource(device.deviceCoordinator.defaultIconResource)
            if (!device.isConnected) {
                val colorMatrix = ColorMatrix()
                colorMatrix.setSaturation(0f)
                binding.imageViewDeviceIcon.colorFilter = ColorMatrixColorFilter(colorMatrix)
            } else {
                binding.imageViewDeviceIcon.colorFilter = null
            }
        }
    }
}
